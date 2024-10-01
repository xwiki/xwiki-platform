/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.store;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageAttachmentReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.store.UnexpectedException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocument.XWikiAttachmentToRemove;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.doc.XWikiSpace;
import com.xpn.xwiki.internal.store.hibernate.legacy.LegacySessionImplementor;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseElement;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.stats.impl.XWikiStats;
import com.xpn.xwiki.util.Util;

/**
 * The XWiki Hibernate database driver.
 *
 * @version $Id$
 */
@Component
@Named(XWikiHibernateBaseStore.HINT)
@Singleton
public class XWikiHibernateStore extends XWikiHibernateBaseStore implements XWikiStoreInterface, Initializable
{
    @Inject
    private Logger logger;

    /**
     * QueryManager for this store.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Needed so we can register an event to trap logout and delete held locks.
     */
    @Inject
    private ObservationManager observationManager;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @Inject
    private DocumentReferenceResolver<String> defaultDocumentReferenceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<PageReference> currentPageReferenceDocumentReferenceResolver;

    /**
     * Used to convert a proper Document Reference to string (standard form).
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part).
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private ComponentManager componentManager;

    @Inject
    @Named(HINT)
    private XWikiAttachmentStoreInterface attachmentContentStore;

    @Inject
    @Named(HINT)
    private AttachmentVersioningStore attachmentArchiveStore;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    private Map<String, String[]> validTypesMap = new HashMap<>();

    /**
     * Stores locks for saving documents in a map with soft references for values to ensure that they can be cleared
     * under memory pressure but that under no circumstances a lock that is currently in use is removed from the map (as
     * this would endanger the purpose of the lock). These locks ensure that no two threads can save the same document
     * at the same time, see also <a href="https://jira.xwiki.org/browse/XWIKI-13473">XWIKI-13473</a>.
     */
    private final Map<Long, ReentrantLock> documentSavingLockMap = Collections.synchronizedMap(new ReferenceMap<>());

    /**
     * Same mechanism used for saving spaces.
     */
    private final Map<Long, ReentrantLock> spaceSavingLockMap = Collections.synchronizedMap(new ReferenceMap<>());

    /**
     * This allows to initialize our storage engine. The hibernate config file path is taken from xwiki.cfg or directly
     * in the WEB-INF directory.
     *
     * @param xwiki
     * @param context
     * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateStore(XWiki xwiki, XWikiContext context)
    {
        super(xwiki, context);
        initValidColumTypes();
    }

    /**
     * Initialize the storage engine with a specific path. This is used for tests.
     *
     * @param hibpath
     * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateStore(String hibpath)
    {
        super(hibpath);
        initValidColumTypes();
    }

    /**
     * @see #XWikiHibernateStore(XWiki, XWikiContext)
     * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateStore(XWikiContext context)
    {
        this(context.getWiki(), context);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public XWikiHibernateStore()
    {
        initValidColumTypes();
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.registerLogoutListener();
    }

    /**
     * This initializes the valid custom types Used for Custom Mapping
     */
    private void initValidColumTypes()
    {
        String[] string_types = { "string", "text", "clob" };
        String[] number_types =
            { "integer", "long", "float", "double", "big_decimal", "big_integer", "yes_no", "true_false" };
        String[] date_types = { "date", "time", "timestamp" };
        String[] boolean_types = { "boolean", "yes_no", "true_false", "integer" };
        this.validTypesMap = new HashMap<>();
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.StringClass", string_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.TextAreaClass", string_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.PasswordClass", string_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.NumberClass", number_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.DateClass", date_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.BooleanClass", boolean_types);
    }

    @Override
    public boolean isWikiNameAvailable(String wikiName, XWikiContext inputxcontext) throws XWikiException
    {
        try {
            return !this.store.isWikiDatabaseExist(wikiName);
        } catch (Exception e) {
            Object[] args = { wikiName };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DATABASE,
                "Exception while listing databases to search for {0}", e, args);
        }
    }

    @Override
    public void createWiki(String wikiName, XWikiContext inputxcontext) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, false);

        boolean bTransaction = true;
        String database = context.getWikiId();
        AtomicReference<Statement> stmt = new AtomicReference<>(null);

        bTransaction = beginTransaction(context);
        try {
            Session session = getSession(context);

            session.doWork(connection -> {
                stmt.set(connection.createStatement());
                Statement statement = stmt.get();

                String schema = getSchemaFromWikiName(wikiName, context);
                String escapedSchema = escapeSchema(schema, context);

                DatabaseProduct databaseProduct = getDatabaseProductName();
                if (DatabaseProduct.ORACLE == databaseProduct) {
                    // Notes:
                    // - We use default tablespaces (which mean the USERS and TEMP tablespaces) to make it simple.
                    // We also don't know which tablespace was used to create the main wiki and creating a new one
                    // here would make things more complex (we would need to check if it exists already for example).
                    // - We must specify a quota on the USERS tablespace so that the user can create objects (like
                    // tables). Note that tables are created deferred by default so you'd think the user can create
                    // them without quotas set but that's because tables are created deferred by default and thus
                    // they'll fail when the first data is written in them.
                    // See https://dba.stackexchange.com/a/254950
                    // - Depending on how it's configured, the default users tablespace size might be too small. Thus
                    // it's up to a DBA to make sure it's large enough.
                    statement.execute(String.format("CREATE USER %s IDENTIFIED BY %s QUOTA UNLIMITED ON USERS",
                        escapedSchema, escapedSchema));
                } else if (DatabaseProduct.DERBY == databaseProduct || DatabaseProduct.DB2 == databaseProduct
                    || DatabaseProduct.H2 == databaseProduct)
                {
                    statement.execute("CREATE SCHEMA " + escapedSchema);
                } else if (DatabaseProduct.HSQLDB == databaseProduct) {
                    statement.execute("CREATE SCHEMA " + escapedSchema + " AUTHORIZATION DBA");
                } else if (DatabaseProduct.MYSQL == databaseProduct || DatabaseProduct.MARIADB == databaseProduct) {
                    StringBuilder statementBuilder = new StringBuilder("create database " + escapedSchema);
                    String[] charsetAndCollation = getCharsetAndCollation(wikiName, session, context);
                    statementBuilder.append(" CHARACTER SET ");
                    statementBuilder.append(charsetAndCollation[0]);
                    statementBuilder.append(" COLLATE ");
                    statementBuilder.append(charsetAndCollation[1]);
                    statement.execute(statementBuilder.toString());
                } else if (DatabaseProduct.POSTGRESQL == databaseProduct) {
                    if (isInSchemaMode()) {
                        statement.execute("CREATE SCHEMA " + escapedSchema);
                    } else {
                        this.logger.error("Creation of a new database is currently only supported in the schema mode, "
                            + "see https://jira.xwiki.org/browse/XWIKI-8753");
                    }
                } else {
                    statement.execute("create database " + escapedSchema);
                }
            });

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            Object[] args = { wikiName };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CREATE_DATABASE, "Exception while create wiki database {0}",
                e, args);
        } finally {
            context.setWikiId(database);
            try {
                Statement statement = stmt.get();
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
            }
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * @return the MySQL charset and collation to use when creating a new database. They are retrieved by finding the
     *     ones used for the main wiki and if that fails, the {@code utf8mb4} charset and {@code utf8mb4_bin} collation
     *     are used (We use {@code utf8mb4} and not {@code utf8} so that by default, users can insert emojis in
     *     content).
     */
    private String[] getCharsetAndCollation(String wikiName, Session session, XWikiContext context)
    {
        String[] result = new String[2];
        String charset = "utf8mb4";
        String collation = "utf8mb4_bin";

        // Get main wiki encoding
        if (!context.isMainWiki(wikiName)) {
            NativeQuery<Object[]> selectQuery = session.createNativeQuery(
                "select DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME from INFORMATION_SCHEMA.SCHEMATA"
                    + " where SCHEMA_NAME='" + getSchemaFromWikiName(context.getMainXWiki(), context) + "'");
            Object[] queryResult = selectQuery.uniqueResult();
            if (queryResult != null) {
                charset = (String) queryResult[0];
                collation = (String) queryResult[1];
            }
        }

        result[0] = charset;
        result[1] = collation;
        return result;
    }

    @Override
    public void deleteWiki(String wikiName, XWikiContext inputxcontext) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, false);

        String database = context.getWikiId();
        AtomicReference<Statement> stmt = new AtomicReference<>(null);

        boolean bTransaction = beginTransaction(context);
        try {
            Session session = getSession(context);
            session.doWork(connection -> {
                stmt.set(connection.createStatement());

                String schema = getSchemaFromWikiName(wikiName, context);
                String escapedSchema = escapeSchema(schema, context);

                executeDeleteWikiStatement(stmt.get(), getDatabaseProductName(), escapedSchema);
            });

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            Object[] args = { wikiName };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETE_DATABASE, "Exception while delete wiki database {0}",
                e, args);
        } finally {
            context.setWikiId(database);
            try {
                Statement statement = stmt.get();
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
            }
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Execute the SQL statement on the database to remove a wiki.
     *
     * @param statement the statement object on which to execute the wiki deletion
     * @param databaseProduct the database type
     * @param escapedSchemaName the subwiki schema name being deleted
     * @throws SQLException in case of an error while deleting the sub wiki
     */
    protected void executeDeleteWikiStatement(Statement statement, DatabaseProduct databaseProduct,
        String escapedSchemaName) throws SQLException
    {
        if (DatabaseProduct.ORACLE == databaseProduct) {
            statement.execute("DROP USER " + escapedSchemaName + " CASCADE");
        } else if (DatabaseProduct.DERBY == databaseProduct || DatabaseProduct.MYSQL == databaseProduct
            || DatabaseProduct.MARIADB == databaseProduct
            || DatabaseProduct.H2 == databaseProduct)
        {
            statement.execute("DROP SCHEMA " + escapedSchemaName);
        } else if (DatabaseProduct.HSQLDB == databaseProduct) {
            statement.execute("DROP SCHEMA " + escapedSchemaName + " CASCADE");
        } else if (DatabaseProduct.DB2 == databaseProduct) {
            statement.execute("DROP SCHEMA " + escapedSchemaName + " RESTRICT");
        } else if (DatabaseProduct.POSTGRESQL == databaseProduct) {
            if (isInSchemaMode()) {
                statement.execute("DROP SCHEMA " + escapedSchemaName + " CASCADE");
            } else {
                this.logger.warn("Subwiki deletion not yet supported in Database mode for PostgreSQL");
            }
        }
    }

    /**
     * Verifies if a wiki document exists
     */
    @Override
    public boolean exists(XWikiDocument doc, XWikiContext inputxcontext) throws XWikiException
    {
        // In order to avoid trying to issue any SQL query to the DB, we first check if the wiki containing the
        // doc exists. If not, then the doc cannot exist for sure.
        try {
            if (!this.wikiDescriptorManager.exists(this.wikiDescriptorManager.getCurrentWikiId())) {
                return false;
            }
        } catch (WikiManagerException e) {
            // An error occurred while retrieving the wiki descriptors. This is an important problem and we shouldn't
            // swallow it and instead we mist let it bubble up.
            Object[] args = { this.wikiDescriptorManager.getCurrentWikiId() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DOC,
                "Error while checking for existence of the [{0}] wiki", e, args);
        }

        return executeRead(inputxcontext, session -> {
            try {
                String fullName = doc.getFullName();

                String sql = "select doc.fullName from XWikiDocument as doc where doc.fullName=:fullName";
                if (!doc.getLocale().equals(Locale.ROOT)) {
                    sql += " and doc.language=:language";
                }

                Query<String> query = session.createQuery(sql);
                query.setParameter("fullName", fullName);
                if (!doc.getLocale().equals(Locale.ROOT)) {
                    query.setParameter("language", doc.getLocale().toString());
                }
                Iterator<String> it = query.list().iterator();
                while (it.hasNext()) {
                    if (fullName.equals(it.next())) {
                        return true;
                    }
                }

                return false;
            } catch (Exception e) {
                Object[] args = { doc.getDocumentReferenceWithLocale() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DOC, "Exception while reading document {0}",
                    e, args);
            }
        });
    }

    @Override
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        Lock lock = this.documentSavingLockMap.computeIfAbsent(doc.getId(), id -> new ReentrantLock(true));
        lock.lock();

        try {
            XWikiContext context = getExecutionXContext(inputxcontext, true);

            MonitorPlugin monitor = Util.getMonitorPlugin(context);

            try {
                // Start monitoring timer
                if (monitor != null) {
                    monitor.startTimer(HINT);
                }
                doc.setStore(this);
                // Make sure the database name is stored
                doc.setDatabase(context.getWikiId());

                // If the comment is larger than the max size supported by the Storage, then abbreviate it
                String comment = doc.getComment();
                if (comment != null && comment.length() > 1023) {
                    doc.setComment(StringUtils.abbreviate(comment, 1023));
                }

                // Before starting the transaction, make sure any document metadata which might rely on configuration is
                // initialized
                doc.initialize();

                if (bTransaction) {
                    checkHibernate(context);
                    SessionFactory sfactory = injectCustomMappingsInSessionFactory(doc, context);
                    bTransaction = beginTransaction(sfactory, context);
                }

                try {
                    Session session = getSession(context);
                    session.setHibernateFlushMode(FlushMode.COMMIT);

                    // These informations will allow to not look for attachments and objects on loading
                    doc.setElement(XWikiDocument.HAS_ATTACHMENTS, !doc.getAttachmentList().isEmpty());
                    doc.setElement(XWikiDocument.HAS_OBJECTS, !doc.getXObjects().isEmpty());

                    // Let's update the class XML since this is the new way to store it
                    // TODO If all the properties are removed, the old xml stays?
                    BaseClass bclass = doc.getXClass();
                    if (bclass != null) {
                        if (bclass.getFieldList().isEmpty()) {
                            doc.setXClassXML("");
                        } else {
                            // Don't format the XML to reduce the size of the stored data as much as possible
                            doc.setXClassXML(bclass.toXMLString(false));
                        }
                        bclass.setDirty(false);
                    }

                    // Remove attachments planned for removal
                    if (!doc.getAttachmentsToRemove().isEmpty()) {
                        for (XWikiAttachmentToRemove attachmentToRemove : doc.getAttachmentsToRemove()) {
                            XWikiAttachment attachment = attachmentToRemove.getAttachment();

                            XWikiAttachment attachmentToAdd = doc.getAttachment(attachment.getFilename());
                            if (attachmentToAdd != null && attachmentToAdd.getId() == attachment.getId()) {
                                // Hibernate does not like when the "same" database entity (from identifier point of
                                // view)
                                // is manipulated through two different Java objects in the same session. But it also
                                // refuse
                                // to delete and insert the "same" entity (still from id point of view) in the same
                                // sessions. So when we hit such a case we only remove the attachment history and let
                                // the
                                // saveAttachmentList code below update the current attachment content.
                                AttachmentVersioningStore store = getAttachmentVersioningStore(attachment);
                                store.deleteArchive(attachment, context, bTransaction);
                                // Keep the same content store since we need to overwrite existing data
                                attachmentToAdd.setContentStore(attachment.getContentStore());
                            } else {
                                XWikiAttachmentStoreInterface store = getXWikiAttachmentStoreInterface(attachment);
                                store.deleteXWikiAttachment(attachment, false, context, false);
                            }
                        }
                    }
                    // Update/add new attachments
                    if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
                        saveAttachmentList(doc, context);
                    }
                    // Reset the list of attachments to remove
                    doc.clearAttachmentsToRemove();

                    // Handle the latest text file
                    if (doc.isContentDirty() || doc.isMetaDataDirty()) {
                        Date ndate = new Date();
                        doc.setDate(ndate);
                        if (doc.isContentDirty()) {
                            doc.setContentUpdateDate(ndate);
                            DocumentAuthors authors = doc.getAuthors();
                            authors.setContentAuthor(authors.getEffectiveMetadataAuthor());
                        }
                        doc.incrementVersion();
                        if (context.getWiki().hasVersioning(context)) {
                            context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, false, context);
                        }

                        doc.setContentDirty(false);
                        doc.setMetaDataDirty(false);
                    } else {
                        if (doc.getDocumentArchive() != null) {
                            // A custom document archive has been provided, we assume it's right
                            // (we also assume it's custom but that's another matter...)
                            // Let's make sure we save the archive if we have one
                            // This is especially needed if we load a document from XML
                            if (context.getWiki().hasVersioning(context)) {
                                context.getWiki().getVersioningStore().saveXWikiDocArchive(doc.getDocumentArchive(),
                                    false, context);

                                // If the version does not exist it means it's a new version so add it to the history
                                if (!containsVersion(doc, doc.getRCSVersion(), context)) {
                                    context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, false, context);
                                }
                            }
                        } else {
                            // Make sure the getArchive call has been made once
                            // with a valid context
                            try {
                                if (context.getWiki().hasVersioning(context)) {
                                    doc.getDocumentArchive(context);

                                    // If the version does not exist it means it's a new version so register it in the
                                    // history
                                    if (!containsVersion(doc, doc.getRCSVersion(), context)) {
                                        context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, false,
                                            context);
                                    }
                                }
                            } catch (XWikiException e) {
                                // this is a non critical error
                            }
                        }
                    }

                    // Verify if the document already exists
                    Query query = session
                        .createQuery("select xwikidoc.id from XWikiDocument as xwikidoc where xwikidoc.id = :id");
                    query.setParameter("id", doc.getId());
                    if (query.uniqueResult() == null) {
                        doc.setNew(true);
                    }

                    // Note: we don't use session.saveOrUpdate(doc) because it used to be slower in Hibernate than
                    // calling
                    // session.save() and session.update() separately.
                    if (doc.isNew()) {
                        if (doc.isContentDirty() || doc.isMetaDataDirty()) {
                            // Reset the creationDate to reflect the date of the first save, not the date of the object
                            // creation
                            doc.setCreationDate(new Date());
                        }
                        session.save(doc);
                    } else {
                        session.update(doc);
                    }

                    // Remove objects planned for removal
                    if (!doc.getXObjectsToRemove().isEmpty()) {
                        for (BaseObject removedObject : doc.getXObjectsToRemove()) {
                            deleteXWikiCollection(removedObject, context, false, false);
                        }
                        doc.setXObjectsToRemove(new ArrayList<BaseObject>());
                    }

                    if (bclass != null) {
                        bclass.setDocumentReference(doc.getDocumentReference());
                        // Store this XWikiClass in the context so that we can use it in case of recursive usage of
                        // classes
                        context.addBaseClass(bclass);
                    }

                    if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
                        // TODO: Delete all objects for which we don't have a name in the Map
                        for (List<BaseObject> objects : doc.getXObjects().values()) {
                            for (BaseObject obj : objects) {
                                if (obj != null) {
                                    obj.setDocumentReference(doc.getDocumentReference());
                                    /* If the object doesn't have a GUID, create it before saving */
                                    if (StringUtils.isEmpty(obj.getGuid())) {
                                        obj.setGuid(null);
                                    }
                                    saveXWikiCollection(obj, context, false);
                                }
                            }
                        }
                    }

                    // Update space table
                    updateXWikiSpaceTable(doc, session);

                    if (bTransaction) {
                        endTransaction(context, true);
                    }

                    doc.setNew(false);

                    // Make sure that properly saved documents aren't restricted.
                    doc.setRestricted(false);

                    // We need to ensure that the saved document becomes the original document
                    doc.setOriginalDocument(doc.clone());
                } finally {
                    if (bTransaction) {
                        try {
                            endTransaction(context, false);
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                Object[] args = { this.defaultEntityReferenceSerializer.serialize(doc.getDocumentReference()) };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "Exception while saving document {0}", e,
                    args);
            } finally {
                // End monitoring timer
                if (monitor != null) {
                    monitor.endTimer(HINT);
                }
            }
        } finally {
            lock.unlock();
            restoreExecutionXContext();
        }
    }

    private void updateXWikiSpaceTable(XWikiDocument document, Session session)
    {
        if (document.getLocale().equals(Locale.ROOT)) {
            // It's possible the space does not yet exist yet
            maybeCreateSpace(document.getDocumentReference().getLastSpaceReference(), document.isHidden(), session);

            if (!document.isNew()) {
                // If the hidden state of an existing document did not changed there is nothing to do
                if (document.isHidden() != document.getOriginalDocument().isHidden()) {
                    if (document.isHidden()) {
                        // If the document became hidden it's possible the space did too
                        maybeMakeSpaceHidden(document.getDocumentReference().getLastSpaceReference(),
                            document.getFullName(), session);
                    } else {
                        // If the document became visible then all its parents should be visible as well
                        makeSpaceVisible(document.getDocumentReference().getLastSpaceReference(), session);
                    }
                }
            }
        }
    }

    private void insertXWikiSpace(XWikiSpace space, Session session)
    {
        Lock lock = this.spaceSavingLockMap.computeIfAbsent(space.getId(), id -> new ReentrantLock(true));
        lock.lock();
        try {
            // Insert the space
            session.save(space);

            // Update parent space
            if (space.getSpaceReference().getParent() instanceof SpaceReference) {
                maybeCreateSpace((SpaceReference) space.getSpaceReference().getParent(), space.isHidden(), session);
            }
        } finally {
            lock.unlock();
        }
    }

    private void makeSpaceVisible(SpaceReference spaceReference, Session session)
    {
        XWikiSpace space = loadXWikiSpace(spaceReference, session);

        makeSpaceVisible(space, session);
    }

    private void makeSpaceVisible(XWikiSpace space, Session session)
    {
        if (space.isHidden()) {
            space.setHidden(false);

            session.update(space);

            // Update parent
            if (space.getSpaceReference().getParent() instanceof SpaceReference) {
                makeSpaceVisible((SpaceReference) space.getSpaceReference().getParent(), session);
            }
        }
    }

    private void maybeMakeSpaceHidden(SpaceReference spaceReference, String modifiedDocument, Session session)
    {
        XWikiSpace space = loadXWikiSpace(spaceReference, session);

        // The space is supposed to exist
        if (space == null) {
            this.logger.warn(
                "Space [{}] does not exist. Usually means the spaces table is not in sync with the documents table.",
                spaceReference);

            return;
        }

        // If the space is already hidden return
        if (space.isHidden()) {
            return;
        }

        if (calculateHiddenStatus(spaceReference, modifiedDocument, session)) {
            // Make the space hidden
            space.setHidden(true);
            session.update(space);

            // Update space parent
            if (spaceReference.getParent() instanceof SpaceReference) {
                maybeMakeSpaceHidden((SpaceReference) spaceReference.getParent(), modifiedDocument, session);
            }
        }
    }

    private void maybeCreateSpace(SpaceReference spaceReference, boolean hidden, Session session)
    {
        XWikiSpace space = loadXWikiSpace(spaceReference, session);

        if (space != null) {
            if (space.isHidden() && !hidden) {
                makeSpaceVisible(space, session);
            }
        } else {
            insertXWikiSpace(new XWikiSpace(spaceReference, hidden), session);
        }
    }

    private boolean hasDocuments(SpaceReference spaceReference, Session session, String extraWhere,
        Map<String, ?> parameters)
    {
        StringBuilder builder = new StringBuilder(
            "select distinct xwikidoc.space from XWikiDocument as xwikidoc where (space = :space OR space LIKE :like)");

        if (StringUtils.isNotEmpty(extraWhere)) {
            builder.append(" AND ");
            builder.append('(');
            builder.append(extraWhere);
            builder.append(')');
        }

        Query<String> query = session.createQuery(builder.toString(), String.class);

        String localSpaceReference = this.localEntityReferenceSerializer.serialize(spaceReference);
        String localSpaceReferencePrefix = localSpaceReference + '.';

        query.setParameter("space", localSpaceReference);
        query.setParameter("like", localSpaceReferencePrefix + "%");

        if (parameters != null) {
            parameters.forEach(query::setParameter);
        }

        // Leading and trailing white spaces are not taken into account in SQL comparisons so we have to make sure the
        // matched spaces really are the expected ones
        for (String result : query.getResultList()) {
            if (result.equals(localSpaceReference) || result.startsWith(localSpaceReferencePrefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find hidden status of a space from its children.
     */
    private boolean calculateHiddenStatus(SpaceReference spaceReference, String documentToIngore, Session session)
    {
        // If there is at least one visible document then the space is visible
        StringBuilder builder = new StringBuilder("(hidden = false OR hidden IS NULL)");

        Map<String, ?> parameters;
        if (documentToIngore != null) {
            builder.append(" AND fullName <> :documentToIngore");
            parameters = Collections.singletonMap("documentToIngore", documentToIngore);
        } else {
            parameters = null;
        }

        return !hasDocuments(spaceReference, session, builder.toString(), parameters);
    }

    private boolean containsVersion(XWikiDocument doc, Version targetversion, XWikiContext context)
        throws XWikiException
    {
        for (Version version : doc.getRevisions(context)) {
            if (version.equals(targetversion)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        saveXWikiDoc(doc, context, true);
    }

    /**
     * {@inheritDoc} This implementation of rename relies on {@link #saveXWikiDoc(XWikiDocument, XWikiContext, boolean)}
     * and {@link #deleteXWikiDoc(XWikiDocument, XWikiContext, boolean)}. The idea here is that the document reference
     * has many impacts everywhere and it's actually safer to keep relying on existing save method. Now all the benefit
     * of this rename, is to call those methods in the same transaction when both old and new reference belong to the
     * same wiki (same database). If the references belong to different databases we are force to use two transactions.
     */
    @Override
    public void renameXWikiDoc(XWikiDocument doc, DocumentReference newReference, XWikiContext inputxcontext)
        throws XWikiException
    {
        WikiReference sourceWikiReference = doc.getDocumentReference().getWikiReference();
        WikiReference targetWikiReference = newReference.getWikiReference();

        // perform the change in same session only if the new and old reference belongs to same wiki (same database)
        boolean sameSession = sourceWikiReference.equals(targetWikiReference);

        XWikiContext context = getExecutionXContext(inputxcontext, true);
        XWikiDocument newDocument = doc.cloneRename(newReference, context);
        newDocument.setNew(true);
        newDocument.setStore(this);
        newDocument
            .setComment("Renamed from " + this.defaultEntityReferenceSerializer.serialize(doc.getDocumentReference()));

        boolean copyPerformed = false;

        try {
            if (sameSession) {
                // We execute the whole call with a commit at the end,
                // but we ensure to not commit at each step (save and delete)
                executeWrite(context, session -> {
                    saveXWikiDoc(newDocument, context, false);

                    // Since the save documment is called without a commit, the information are not flushed
                    // in the session either. However we need the new information in the session for the delete
                    // in particular to know the possible changes made in the spaces.
                    session.flush();
                    deleteXWikiDoc(doc, context, false);

                    return true;
                });
            } else {
                // Execute the save on the right DB with a commit at the end
                context.setWikiReference(targetWikiReference);
                executeWrite(context, session -> {
                    saveXWikiDoc(newDocument, context, false);

                    return true;
                });

                // to be able to rollback in case of problem during delete
                copyPerformed = true;

                // Execute the delete on the right DB with a commit at the end
                context.setWikiReference(sourceWikiReference);
                executeWrite(context, session -> {
                    this.deleteXWikiDoc(doc, context, false);

                    return true;
                });
            }
        } catch (Exception e) {
            // We only need to perform special actions in case of different sessions,
            // and if the first step has been executed. In all other cases nothing should have been committed.
            if (!sameSession && copyPerformed) {
                // Ensure to delete the doc that has been copied already.
                // Note that in case of problem there, the exception is directly thrown.
                executeWrite(context, session -> {
                    this.deleteXWikiDoc(newDocument, context, false);

                    return true;
                });
            }

            Object[] args = { doc.getDocumentReference(), newReference };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_RENAMING_DOC,
                "Exception while renaming document [{0}] to [{1}]", e, args);
        }
    }

    @Override
    public XWikiDocument loadXWikiDoc(XWikiDocument defaultDocument, XWikiContext inputxcontext) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        XWikiDocument doc = defaultDocument;
        try {
            boolean bTransaction = true;
            MonitorPlugin monitor = Util.getMonitorPlugin(context);
            try {
                // Start monitoring timer
                if (monitor != null) {
                    monitor.startTimer(HINT);
                }
                checkHibernate(context);

                SessionFactory sfactory = injectCustomMappingsInSessionFactory(defaultDocument, context);
                bTransaction = bTransaction && beginTransaction(sfactory, context);
                try {
                    Session session = getSession(context);
                    session.setHibernateFlushMode(FlushMode.MANUAL);

                    doc = session.get(XWikiDocument.class, doc.getId());
                    if (doc == null) {
                        defaultDocument.setNew(true);

                        // Make sure to always return a document with an original version, even for one that does not
                        // exist.
                        // Allow writing more generic code.
                        defaultDocument.setOriginalDocument(
                            new XWikiDocument(defaultDocument.getDocumentReference(), defaultDocument.getLocale()));

                        return defaultDocument;
                    }

                    doc.setStore(this);
                    doc.setNew(false);
                    doc.setMostRecent(true);
                    // Fix for XWIKI-1651
                    doc.setDate(new Date(doc.getDate().getTime()));
                    doc.setCreationDate(new Date(doc.getCreationDate().getTime()));
                    doc.setContentUpdateDate(new Date(doc.getContentUpdateDate().getTime()));

                    // Loading the attachment list
                    if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
                        loadAttachmentList(doc, context, false);
                    }

                    // TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
                    BaseClass bclass = new BaseClass();
                    String cxml = doc.getXClassXML();
                    if (cxml != null) {
                        bclass.fromXML(cxml);
                        doc.setXClass(bclass);
                        bclass.setDirty(false);
                    }

                    // Store this XWikiClass in the context so that we can use it in case of recursive usage
                    // of classes
                    context.addBaseClass(bclass);

                    if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
                        Query<BaseObject> query = session.createQuery(
                            "from BaseObject as bobject where bobject.name = :name order by bobject.number",
                            BaseObject.class);
                        query.setParameter("name", doc.getFullName());

                        Iterator<BaseObject> it = query.list().iterator();

                        EntityReference localGroupEntityReference = new EntityReference("XWikiGroups",
                            EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));
                        DocumentReference groupsDocumentReference = new DocumentReference(context.getWikiId(),
                            localGroupEntityReference.getParent().getName(), localGroupEntityReference.getName());

                        boolean hasGroups = false;
                        while (it.hasNext()) {
                            BaseObject object = it.next();
                            DocumentReference classReference = object.getXClassReference();

                            if (classReference == null) {
                                continue;
                            }

                            // It seems to search before is case insensitive. And this would break the loading if we get
                            // an
                            // object which doesn't really belong to this document
                            if (!object.getDocumentReference().equals(doc.getDocumentReference())) {
                                continue;
                            }

                            BaseObject newobject;
                            if (classReference.equals(doc.getDocumentReference())) {
                                newobject = bclass.newCustomClassInstance(true);
                            } else {
                                newobject = BaseClass.newCustomClassInstance(classReference, true, context);
                            }
                            if (newobject != null) {
                                newobject.setId(object.getId());
                                newobject.setXClassReference(object.getRelativeXClassReference());
                                newobject.setDocumentReference(object.getDocumentReference());
                                newobject.setNumber(object.getNumber());
                                newobject.setGuid(object.getGuid());
                                object = newobject;
                            }

                            if (classReference.equals(groupsDocumentReference)) {
                                // Groups objects are handled differently.
                                hasGroups = true;
                            } else {
                                loadXWikiCollectionInternal(object, doc, context, false, true);
                            }
                            doc.setXObject(object.getNumber(), object);
                        }

                        // AFAICT this was added as an emergency patch because loading of objects has proven
                        // too slow and the objects which cause the most overhead are the XWikiGroups objects
                        // as each group object (each group member) would otherwise cost 2 database queries.
                        // This will do every group member in a single query.
                        if (hasGroups) {
                            Query<Object[]> query2 = session.createQuery(
                                "select bobject.number, prop.value from StringProperty as prop,"
                                    + "BaseObject as bobject where bobject.name = :name and bobject.className='XWiki.XWikiGroups' "
                                    + "and bobject.id=prop.id.id and prop.id.name='member' order by bobject.number",
                                Object[].class);
                            query2.setParameter("name", doc.getFullName());

                            Iterator<Object[]> it2 = query2.list().iterator();
                            while (it2.hasNext()) {
                                Object[] result = it2.next();
                                Integer number = (Integer) result[0];
                                String member = (String) result[1];
                                BaseObject obj =
                                    BaseClass.newCustomClassInstance(groupsDocumentReference, true, context);
                                obj.setDocumentReference(doc.getDocumentReference());
                                obj.setXClassReference(localGroupEntityReference);
                                obj.setNumber(number.intValue());
                                obj.setStringValue("member", member);
                                doc.setXObject(obj.getNumber(), obj);
                            }
                        }
                    }

                    doc.setContentDirty(false);
                    doc.setMetaDataDirty(false);

                    // We need to ensure that the loaded document becomes the original document
                    doc.setOriginalDocument(doc.clone());

                    if (bTransaction) {
                        endTransaction(context, false);
                    }
                } finally {
                    if (bTransaction) {
                        try {
                            endTransaction(context, false);
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                Object[] args = { defaultDocument.getDocumentReferenceWithLocale() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC, "Exception while reading document [{0}]", e,
                    args);
            } finally {
                // End monitoring timer
                if (monitor != null) {
                    monitor.endTimer(HINT);
                }
            }

            this.logger.debug("Loaded XWikiDocument: [{}]", doc.getDocumentReferenceWithLocale());

            return doc;
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext inputxcontext) throws XWikiException
    {
        deleteXWikiDoc(doc, inputxcontext, true);
    }

    private void deleteXWikiDoc(XWikiDocument doc, XWikiContext inputxcontext, boolean bTransaction)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            MonitorPlugin monitor = Util.getMonitorPlugin(context);
            try {
                // Start monitoring timer
                if (monitor != null) {
                    monitor.startTimer(HINT);
                }
                checkHibernate(context);
                SessionFactory sfactory = injectCustomMappingsInSessionFactory(doc, context);
                if (bTransaction) {
                    bTransaction = beginTransaction(sfactory, context);
                }
                try {
                    Session session = getSession(context);
                    session.setHibernateFlushMode(FlushMode.COMMIT);

                    if (doc.getStore() == null) {
                        Object[] args = { doc.getDocumentReference() };
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
                            "Impossible to delete document {0} if it is not loaded", null, args);
                    }

                    // Let's delete any attachment this document might have
                    for (XWikiAttachment attachment : doc.getAttachmentList()) {
                        XWikiAttachmentStoreInterface store = getXWikiAttachmentStoreInterface(attachment);
                        store.deleteXWikiAttachment(attachment, false, context, false);
                    }

                    // deleting XWikiLinks
                    if (context.getWiki().hasBacklinks(context)) {
                        deleteLinks(doc.getId(), context, true);
                    }

                    // Find the list of classes for which we have an object
                    // Remove properties planned for removal
                    if (!doc.getXObjectsToRemove().isEmpty()) {
                        for (BaseObject bobj : doc.getXObjectsToRemove()) {
                            if (bobj != null) {
                                deleteXWikiCollection(bobj, context, false, false);
                            }
                        }
                        doc.setXObjectsToRemove(new ArrayList<BaseObject>());
                    }
                    for (List<BaseObject> objects : doc.getXObjects().values()) {
                        for (BaseObject obj : objects) {
                            if (obj != null) {
                                deleteXWikiCollection(obj, context, false, false);
                            }
                        }
                    }
                    context.getWiki().getVersioningStore().deleteArchive(doc, false, context);

                    session.delete(doc);

                    // We need to ensure that the deleted document becomes the original document
                    doc.setOriginalDocument(doc.clone());

                    // Update space table if needed
                    maybeDeleteXWikiSpace(doc, session);

                    if (bTransaction) {
                        endTransaction(context, true);
                    }
                } finally {
                    if (bTransaction) {
                        try {
                            endTransaction(context, false);
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                Object[] args = { doc.getDocumentReference() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC, "Exception while deleting document {0}", e,
                    args);
            } finally {
                // End monitoring timer
                if (monitor != null) {
                    monitor.endTimer(HINT);
                }
            }
        } finally {
            restoreExecutionXContext();
        }
    }

    private void maybeDeleteXWikiSpace(XWikiDocument deletedDocument, Session session)
    {
        if (deletedDocument.getLocale().equals(Locale.ROOT)) {
            DocumentReference documentReference = deletedDocument.getDocumentReference();
            maybeDeleteXWikiSpace(documentReference.getLastSpaceReference(),
                this.localEntityReferenceSerializer.serialize(documentReference), session);
        }
    }

    private void maybeDeleteXWikiSpace(SpaceReference spaceReference, String deletedDocument, Session session)
    {
        if (!hasDocuments(spaceReference, session,
            "fullName <> :deletedDocument AND (language IS NULL OR language = '')",
            Collections.singletonMap("deletedDocument", deletedDocument)))
        {
            // The document was the last document in the space
            XWikiSpace space = new XWikiSpace(spaceReference, this);

            session.delete(space);

            // Update parent
            if (spaceReference.getParent() instanceof SpaceReference) {
                maybeDeleteXWikiSpace((SpaceReference) spaceReference.getParent(), deletedDocument, session);
            }
        } else {
            // Update space hidden property if needed
            maybeMakeSpaceHidden(spaceReference, deletedDocument, session);
        }
    }

    private XWikiSpace loadXWikiSpace(SpaceReference spaceReference, Session session)
    {
        XWikiSpace space = session.get(XWikiSpace.class, XWikiSpace.getId(spaceReference));

        if (space != null) {
            space.setStore(this);
        }
        return space;
    }

    private void checkObjectClassIsLocal(BaseCollection object, XWikiContext context) throws XWikiException
    {
        DocumentReference xclass = object.getXClassReference();
        WikiReference wikiReference = xclass.getWikiReference();
        String db = context.getWikiId();
        if (!wikiReference.getName().equals(db)) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
                "XObject [{0}] is an instance of an external XClass and cannot be persisted in this wiki [{1}].", null,
                new Object[] { this.localEntityReferenceSerializer.serialize(object.getReference()), db });
        }
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void saveXWikiCollection(BaseCollection object, XWikiContext inputxcontext, boolean bTransaction)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            if (object == null) {
                return;
            }
            // We need a slightly different behavior here
            boolean stats = (object instanceof XWikiStats);
            if (!stats) {
                checkObjectClassIsLocal(object, context);
            }

            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            try {
                Session session = getSession(context);

                // Verify if the property already exists
                Query<Long> query;
                if (stats) {
                    query = session.createQuery(
                        "select obj.id from " + object.getClass().getName() + " as obj where obj.id = :id", Long.class);
                } else {
                    query = session.createQuery("select obj.id from BaseObject as obj where obj.id = :id", Long.class);
                }
                query.setParameter("id", object.getId());
                if (query.uniqueResult() == null) {
                    if (stats) {
                        session.save(object);
                    } else {
                        session.save("com.xpn.xwiki.objects.BaseObject", object);
                    }
                } else {
                    if (stats) {
                        session.update(object);
                    } else {
                        session.update("com.xpn.xwiki.objects.BaseObject", object);
                    }
                }
                /*
                 * if (stats) session.saveOrUpdate(object); else
                 * session.saveOrUpdate((String)"com.xpn.xwiki.objects.BaseObject", (Object)object);
                 */
                BaseClass bclass = object.getXClass(context);
                List<String> handledProps = new ArrayList<>();
                if ((bclass != null) && (bclass.hasCustomMapping()) && context.getWiki().hasCustomMappings()) {
                    // save object using the custom mapping
                    Map<String, Object> objmap = object.getCustomMappingMap();
                    handledProps = bclass.getCustomMappingPropertyList(context);
                    query = session.createQuery("select obj.id from " + bclass.getName() + " as obj where obj.id = :id",
                        Long.class);
                    query.setParameter("id", object.getId());
                    if (query.uniqueResult() == null) {
                        session.save(bclass.getName(), objmap);
                    } else {
                        session.update(bclass.getName(), objmap);
                    }

                    // dynamicSession.saveOrUpdate((String) bclass.getName(), objmap);
                }

                if (object.getXClassReference() != null) {
                    // Remove properties to remove
                    if (!object.getFieldsToRemove().isEmpty()) {
                        for (int i = 0; i < object.getFieldsToRemove().size(); i++) {
                            BaseProperty prop = (BaseProperty) object.getFieldsToRemove().get(i);
                            if (!handledProps.contains(prop.getName())) {
                                session.delete(prop);
                            }
                        }
                        object.setFieldsToRemove(new ArrayList<>());
                    }

                    // Add missing properties to the object
                    BaseClass xclass = object.getXClass(context);
                    if (xclass != null) {
                        for (String key : xclass.getPropertyList()) {
                            if (object.safeget(key) == null) {
                                PropertyClass classProperty = (PropertyClass) xclass.getField(key);
                                BaseProperty property = classProperty.newProperty();
                                if (property != null) {
                                    object.safeput(key, property);
                                }
                            }
                        }
                    }

                    // Save properties
                    Iterator<String> it = object.getPropertyList().iterator();
                    while (it.hasNext()) {
                        String key = it.next();
                        BaseProperty prop = (BaseProperty) object.getField(key);
                        if (!prop.getName().equals(key)) {
                            Object[] args = { key, object.getName() };
                            throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES,
                                XWikiException.ERROR_XWIKI_CLASSES_FIELD_INVALID,
                                "Field {0} in object {1} has an invalid name", null, args);
                        }

                        String pname = prop.getName();
                        if (pname != null && !pname.trim().equals("") && !handledProps.contains(pname)) {
                            saveXWikiPropertyInternal(prop, context, false);
                        }
                    }
                }

                if (bTransaction) {
                    endTransaction(context, true);
                }
            } finally {
                if (bTransaction) {
                    try {
                        endTransaction(context, true);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (XWikiException xe) {
            throw xe;
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT, "Exception while saving object {0}", e, args);
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void loadXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        loadXWikiCollectionInternal(object, context, bTransaction, false);
    }

    private void loadXWikiCollectionInternal(BaseCollection object, XWikiContext context, boolean bTransaction,
        boolean alreadyLoaded) throws XWikiException
    {
        loadXWikiCollectionInternal(object, null, context, bTransaction, alreadyLoaded);
    }

    private void loadXWikiCollectionInternal(BaseCollection object1, XWikiDocument doc, XWikiContext inputxcontext,
        boolean bTransaction, boolean alreadyLoaded) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        BaseCollection object = object1;
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            try {
                Session session = getSession(context);

                if (!alreadyLoaded) {
                    try {
                        session.load(object, object1.getId());
                    } catch (ObjectNotFoundException e) {
                        // There is no object data saved
                        object = null;
                        return;
                    }
                }

                DocumentReference classReference = object.getXClassReference();

                // If the class reference is null in the loaded object then skip loading properties
                if (classReference != null) {

                    BaseClass bclass = null;
                    if (!classReference.equals(object.getDocumentReference())) {
                        // Let's check if the class has a custom mapping
                        bclass = object.getXClass(context);
                    } else {
                        // We need to get it from the document otherwise
                        // we will go in an endless loop
                        if (doc != null) {
                            bclass = doc.getXClass();
                        }
                    }

                    List<String> handledProps = new ArrayList<>();
                    try {
                        if ((bclass != null) && (bclass.hasCustomMapping()) && context.getWiki().hasCustomMappings()) {
                            String className =
                                this.localEntityReferenceSerializer.serialize(bclass.getDocumentReference());
                            @SuppressWarnings("unchecked")
                            Map<String, ?> map = (Map<String, ?>) session.load(className, object.getId());
                            // Let's make sure to look for null fields in the dynamic mapping
                            bclass.fromValueMap(map, object);
                            for (String prop : bclass.getCustomMappingPropertyList(context)) {
                                if (map.get(prop) != null) {
                                    handledProps.add(prop);
                                }
                            }
                        }
                    } catch (HibernateException e) {
                        this.logger.error("Failed loading custom mapping for doc [{}], class [{}], nb [{}]",
                            object.getDocumentReference(), object.getXClassReference(), object.getNumber(), e);
                    }

                    // Load strings, integers, dates all at once

                    Query<Object[]> query = session.createQuery(
                        "select prop.name, prop.classType from BaseProperty as prop where prop.id.id = :id",
                        Object[].class);
                    query.setParameter("id", object.getId());
                    for (Object[] result : query.list()) {
                        String name = (String) result[0];
                        // No need to load fields already loaded from
                        // custom mapping
                        if (handledProps.contains(name)) {
                            continue;
                        }
                        String classType = (String) result[1];
                        BaseProperty property = null;

                        try {
                            property = (BaseProperty) Class.forName(classType).newInstance();
                            property.setObject(object);
                            property.setName(name);
                            loadXWikiProperty(property, context, false);
                        } catch (Exception e) {
                            // WORKAROUND IN CASE OF MIXMATCH BETWEEN STRING AND LARGESTRING
                            try {
                                if (property instanceof StringProperty) {
                                    LargeStringProperty property2 = new LargeStringProperty();
                                    property2.setObject(object);
                                    property2.setName(name);
                                    loadXWikiProperty(property2, context, false);
                                    property.setValue(property2.getValue());

                                    if (bclass != null) {
                                        if (bclass.get(name) instanceof TextAreaClass) {
                                            property = property2;
                                        }
                                    }
                                } else if (property instanceof LargeStringProperty) {
                                    StringProperty property2 = new StringProperty();
                                    property2.setObject(object);
                                    property2.setName(name);
                                    loadXWikiProperty(property2, context, false);
                                    property.setValue(property2.getValue());

                                    if (bclass != null) {
                                        if (bclass.get(name) instanceof StringClass) {
                                            property = property2;
                                        }
                                    }
                                } else {
                                    throw e;
                                }
                            } catch (Throwable e2) {
                                Object[] args = { object.getName(), object.getClass(),
                                    Integer.valueOf(object.getNumber() + ""), name };
                                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                                    "Exception while loading object [{0}] of class [{1}], number [{2}] and property [{3}]",
                                    e, args);
                            }
                        }

                        object.addField(name, property);
                    }
                }

                if (bTransaction) {
                    endTransaction(context, false);
                }
            } finally {
                if (bTransaction) {
                    try {
                        endTransaction(context, false);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            Object[] args = { object.getName(), object.getClass(), object.getNumber() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                "Exception while loading object [{0}] of class [{1}] and number [{2}]", e, args);
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void deleteXWikiCollection(BaseCollection object, XWikiContext inputxcontext, boolean bTransaction,
        boolean evict) throws XWikiException
    {
        if (object == null) {
            return;
        }

        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            try {
                Session session = getSession(context);

                // Let's check if the class has a custom mapping
                BaseClass bclass = object.getXClass(context);
                List<String> handledProps = new ArrayList<>();
                if ((bclass != null) && (bclass.hasCustomMapping()) && context.getWiki().hasCustomMappings()) {
                    handledProps = bclass.getCustomMappingPropertyList(context);
                    Object map = session.get(bclass.getName(), object.getId());
                    if (map != null) {
                        if (evict) {
                            session.evict(map);
                        }
                        session.delete(map);
                    }
                }

                if (object.getXClassReference() != null) {
                    for (BaseElement property : (Collection<BaseElement>) object.getFieldList()) {
                        if (!handledProps.contains(property.getName())) {
                            if (evict) {
                                session.evict(property);
                            }
                            if (session.get(property.getClass(), property) != null) {
                                session.delete(property);
                            }
                        }
                    }
                }

                // In case of custom class we need to force it as BaseObject to delete the xwikiobject row
                if (!"".equals(bclass.getCustomClass())) {
                    BaseObject cobject = new BaseObject();
                    cobject.setDocumentReference(object.getDocumentReference());
                    cobject.setClassName(object.getClassName());
                    cobject.setNumber(object.getNumber());
                    if (object instanceof BaseObject) {
                        cobject.setGuid(((BaseObject) object).getGuid());
                    }
                    cobject.setId(object.getId());
                    if (evict) {
                        session.evict(cobject);
                    }
                    session.delete(cobject);
                } else {
                    if (evict) {
                        session.evict(object);
                    }
                    session.delete(object);
                }

                if (bTransaction) {
                    endTransaction(context, true);
                }
            } finally {
                if (bTransaction) {
                    try {
                        endTransaction(context, false);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_OBJECT, "Exception while deleting object {0}", e,
                args);
        } finally {
            restoreExecutionXContext();
        }
    }

    private void loadXWikiProperty(PropertyInterface property, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        executeRead(context, session -> {
            try {
                try {
                    session.load(property, (Serializable) property);
                    // In Oracle, empty string are converted to NULL. Since an undefined property is not found at all,
                    // it is
                    // safe to assume that a retrieved NULL value should actually be an empty string.
                    if (property instanceof BaseStringProperty) {
                        BaseStringProperty stringProperty = (BaseStringProperty) property;
                        if (stringProperty.getValue() == null) {
                            stringProperty.setValue("");
                        }
                    }
                    ((BaseProperty) property).setValueDirty(false);
                } catch (ObjectNotFoundException e) {
                    // Let's accept that there is no data in property tables but log it
                    this.logger.error("No data for property [{}] of object id [{}]", property.getName(),
                        property.getId());
                }

                // TODO: understand why collections are lazy loaded
                // Let's force reading lists if there is a list
                // This seems to be an issue since Hibernate 3.0
                // Without this test ViewEditTest.testUpdateAdvanceObjectProp fails
                if (property instanceof ListProperty) {
                    ((ListProperty) property).getList();
                }
            } catch (Exception e) {
                BaseCollection obj = property.getObject();
                Object[] args = { (obj != null) ? obj.getName() : "unknown", property.getName() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while loading property {1} of object {0}", e, args);
            }

            return null;
        });
    }

    private void saveXWikiPropertyInternal(final PropertyInterface property, final XWikiContext context,
        final boolean runInOwnTransaction) throws XWikiException
    {
        // Clone runInOwnTransaction so the value passed is not altered.
        boolean bTransaction = runInOwnTransaction;
        try {
            if (bTransaction) {
                this.checkHibernate(context);
                bTransaction = this.beginTransaction(context);
            }
            try {
                final Session session = this.getSession(context);

                Query<String> query = session.createQuery(
                    "select prop.classType from BaseProperty as prop where prop.id.id = :id and prop.id.name= :name",
                    String.class);
                query.setParameter("id", property.getId());
                query.setParameter("name", property.getName());

                String oldClassType = query.uniqueResult();
                String newClassType = ((BaseProperty) property).getClassType();
                if (oldClassType == null) {
                    session.save(property);
                } else if (oldClassType.equals(newClassType)) {
                    session.update(property);
                } else {
                    // The property type has changed. We cannot simply update its value because the new value and the
                    // old
                    // value are stored in different tables (we're using joined-subclass to map different property
                    // types).
                    // We must delete the old property value before saving the new one and for this we must load the old
                    // property from the table that corresponds to the old property type (we cannot delete and save the
                    // new
                    // property or delete a clone of the new property; loading the old property from the BaseProperty
                    // table
                    // doesn't work either).
                    Query propQuery = session.createQuery(
                        "select prop from " + oldClassType + " as prop where prop.id.id = :id and prop.id.name= :name");
                    propQuery.setParameter("id", property.getId());
                    propQuery.setParameter("name", property.getName());
                    session.delete(propQuery.uniqueResult());
                    session.save(property);
                }

                ((BaseProperty) property).setValueDirty(false);

                if (bTransaction) {
                    endTransaction(context, true);
                }
            } finally {
                if (bTransaction) {
                    try {
                        this.endTransaction(context, false);
                    } catch (Exception ee) {
                        // Not a lot we can do here if there was an exception committing and an exception rolling back.
                    }
                }
            }
        } catch (Exception e) {
            // Something went wrong, collect some information.
            final BaseCollection obj = property.getObject();
            final Object[] args = { (obj != null) ? obj.getName() : "unknown", property.getName() };

            // Throw the exception.
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                "Exception while saving property {1} of object {0}", e, args);
        }
    }

    private void loadAttachmentList(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        executeRead(context, session -> {
            try {
                Query<XWikiAttachment> query = session
                    .createQuery("from XWikiAttachment as attach where attach.docId=:docid", XWikiAttachment.class);
                query.setParameter("docid", doc.getId());

                List<XWikiAttachment> list = query.list();
                for (XWikiAttachment attachment : list) {
                    doc.setAttachment(attachment);
                }

                return null;
            } catch (Exception e) {
                this.logger.error("Failed to load attachments of document [{}]", doc.getDocumentReference(), e);

                Object[] args = { doc.getDocumentReference() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCHING_ATTACHMENT,
                    "Exception while searching attachments for documents {0}", e, args);
            }
        });
    }

    private boolean isDeleted(XWikiAttachment attachment, XWikiDocument doc)
    {
        for (XWikiAttachmentToRemove attachmentToRemove : doc.getAttachmentsToRemove()) {
            if (attachmentToRemove.getAttachment().getFilename().equals(attachment.getFilename())) {
                return true;
            }
        }

        return false;
    }

    private void saveAttachmentList(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            List<XWikiAttachment> list = doc.getAttachmentList();
            for (XWikiAttachment attachment : list) {
                saveAttachment(attachment, isDeleted(attachment, doc), context);
            }
        } catch (Exception e) {
            Object[] args = { doc.getDocumentReference() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT_LIST,
                "Exception while saving attachments attachment list of document {0}", e, args);
        }
    }

    private void saveAttachment(XWikiAttachment attachment, boolean deleted, XWikiContext context) throws XWikiException
    {
        try {
            // If the comment is larger than the max size supported by the Storage, then abbreviate it
            String comment = attachment.getComment();
            if (comment != null && comment.length() > 1023) {
                attachment.setComment(StringUtils.abbreviate(comment, 1023));
            }

            Session session = getSession(context);

            Query<Object[]> query = session.createQuery(
                "select attach.contentStore, attach.archiveStore from XWikiAttachment as attach where attach.id = :id",
                Object[].class);
            query.setParameter("id", attachment.getId());
            Object[] existingAttachment = query.uniqueResult();
            boolean exist = existingAttachment != null;

            boolean saveContent;
            if (exist) {
                // Make sure the attachment content and archive stores stay the same
                attachment.setContentStore((String) existingAttachment[0]);
                attachment.setArchiveStore((String) existingAttachment[1]);

                // Don't update the history if the attachment was actually not supposed to exist
                // Don't update the attachment version if document metadata dirty is forced false (any modification to
                // the attachment automatically set document metadata dirty to true)
                if (!deleted && attachment.isContentDirty() && attachment.getDoc().isMetaDataDirty()) {
                    attachment.updateContentArchive(context);
                }

                session.update(attachment);

                // Save the attachment content if it's marked as "dirty" (out of sync with the database).
                saveContent = attachment.isContentDirty();
            } else {
                if (attachment.getContentStore() == null) {
                    // Set content store
                    attachment.setContentStore(getDefaultAttachmentContentStore(context));
                }

                if (attachment.getArchiveStore() == null) {
                    // Set archive store
                    attachment.setArchiveStore(getDefaultAttachmentArchiveStore(context));
                }

                session.save(attachment);

                // Always save the content since it does not exist
                saveContent = true;
            }

            if (saveContent) {
                // updateParent and bTransaction must be false because the content should be saved in the same
                // transaction as the attachment and if the parent doc needs to be updated, this function will do it.
                XWikiAttachmentStoreInterface store = getXWikiAttachmentStoreInterface(attachment);
                store.saveAttachmentContent(attachment, false, context, false);
            }

            // Mark the attachment content and metadata as not dirty.
            // Ideally this would only happen if the transaction is committed successfully but since an unsuccessful
            // transaction will most likely be accompanied by an exception, the cache will not have a chance to save
            // the copy of the document with erroneous information. If this is not set here, the cache will return
            // a copy of the attachment which claims to be dirty although it isn't.
            attachment.setMetaDataDirty(false);
            if (attachment.isContentDirty()) {
                attachment.getAttachment_content().setContentDirty(false);
            }
        } catch (Exception e) {
            Object[] args = { attachment.getReference() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT, "Exception while saving attachment [{0}]",
                e, args);
        }
    }

    // ---------------------------------------
    // Locks
    // ---------------------------------------

    @Override
    public XWikiLock loadLock(long docId, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        return executeRead(inputxcontext, session -> {
            try {
                XWikiLock lock = null;

                Query<Long> query = session
                    .createQuery("select lock.docId from XWikiLock as lock where lock.docId = :docId", Long.class);
                query.setParameter("docId", docId);
                if (query.uniqueResult() != null) {
                    lock = new XWikiLock();
                    session.load(lock, Long.valueOf(docId));
                }

                return lock;
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_LOCK, "Exception while loading lock", e);
            }
        });
    }

    @Override
    public void saveLock(XWikiLock lock, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        executeWrite(inputxcontext, session -> {
            try {
                Query<Long> query = session
                    .createQuery("select lock.docId from XWikiLock as lock where lock.docId = :docId", Long.class);
                query.setParameter("docId", lock.getDocId());
                if (query.uniqueResult() == null) {
                    session.save(lock);
                } else {
                    session.update(lock);
                }
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_LOCK,
                    String.format("Exception while locking document for lock [%s]", lock.toString()), e);
            }

            return null;
        });
    }

    @Override
    public void deleteLock(XWikiLock lock, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        executeWrite(inputxcontext, session -> {
            try {
                session.delete(lock);
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_LOCK, "Exception while deleting lock", e);
            }

            return null;
        });
    }

    private void registerLogoutListener()
    {
        this.observationManager.addListener(new EventListener()
        {
            private final Event ev = new ActionExecutingEvent();

            @Override
            public String getName()
            {
                return "deleteLocksOnLogoutListener";
            }

            @Override
            public List<Event> getEvents()
            {
                return Collections.<Event>singletonList(this.ev);
            }

            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                if ("logout".equals(((ActionExecutingEvent) event).getActionName())) {
                    final XWikiContext ctx = (XWikiContext) data;
                    if (ctx.getUserReference() != null) {
                        releaseAllLocksForCurrentUser(ctx);
                    }
                }
            }
        });
    }

    /**
     * Release all of the locks held by the currently logged in user.
     *
     * @param ctx the XWikiContext, used to start the connection and get the user name.
     */
    private void releaseAllLocksForCurrentUser(final XWikiContext ctx)
    {
        try {
            executeWrite(ctx, session -> {
                final Query query = session.createQuery("delete from XWikiLock as lock where lock.userName=:userName");
                // Using deprecated getUser() because this is how locks are created.
                // It would be a maintainibility disaster to use different code paths
                // for calculating names when creating and removing.
                query.setParameter("userName", ctx.getUser());
                query.executeUpdate();

                return null;
            });
        } catch (Exception e) {
            String msg = "Error while deleting active locks held by user.";
            try {
                this.endTransaction(ctx, false);
            } catch (Exception utoh) {
                msg += " Failed to commit OR rollback [" + utoh.getMessage() + "]";
            }
            throw new UnexpectedException(msg, e);
        }

        // If we're in a non-main wiki & the user is global,
        // switch to the global wiki and delete locks held there.
        if (!ctx.isMainWiki() && ctx.isMainWiki(ctx.getUserReference().getWikiReference().getName())) {
            final String cdb = ctx.getWikiId();
            try {
                ctx.setWikiId(ctx.getMainXWiki());
                this.releaseAllLocksForCurrentUser(ctx);
            } finally {
                ctx.setWikiId(cdb);
            }
        }
    }

    // ---------------------------------------
    // Links
    // ---------------------------------------

    @Override
    @Deprecated(since = "14.8RC1")
    public List<XWikiLink> loadLinks(long docId, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        return executeRead(inputxcontext, session -> {
            try {
                Query<XWikiLink> query =
                    session.createQuery(" from XWikiLink as link where link.id.docId = :docId", XWikiLink.class);
                query.setParameter("docId", docId);

                return query.list();
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_LINKS, "Exception while loading links", e);
            }
        });
    }

    @Override
    @Deprecated(since = "14.8RC1")
    public List<DocumentReference> loadBacklinks(DocumentReference documentReference, boolean bTransaction,
        XWikiContext inputxcontext) throws XWikiException
    {
        return innerLoadBacklinks(inputxcontext, (Session session) -> {
            // the select clause is compulsory to reach the fullName i.e. the page pointed
            Query<String> query = session.createQuery(
                "select distinct backlink.fullName from XWikiLink as backlink where backlink.id.link = :backlink",
                String.class);

            // if we are in the same wiki context, we should only get the local reference
            // but if we are not, then we have to check the full reference, containing the wiki part since
            // it's how the link are recorded.
            // This should be changed once the refactoring to support backlinks properly has been done.
            // See: XWIKI-16192
            query.setParameter("backlink", this.compactWikiEntityReferenceSerializer.serialize(documentReference));
            return query;
        });
    }

    @Override
    @Deprecated(since = "14.8RC1")
    public List<DocumentReference> loadBacklinks(AttachmentReference attachmentReference, boolean bTransaction,
        XWikiContext inputxcontext) throws XWikiException
    {
        return innerLoadBacklinks(inputxcontext, (Session session) -> {
            // the select clause is compulsory to reach the fullName i.e. the page pointed
            Query<String> query = session.createQuery(
                "select distinct backlink.fullName from XWikiLink as backlink " + "where backlink.id.link = :backlink "
                    + "and backlink.id.type = :type " + "and backlink.attachmentName = :attachmentName",
                String.class);

            // if we are in the same wiki context, we should only get the local reference
            // but if we are not, then we have to check the full reference, containing the wiki part since
            // it's how the link are recorded.
            // This should be changed once the refactoring to support backlinks properly has been done.
            // See: XWIKI-16192
            query.setParameter("backlink",
                this.compactWikiEntityReferenceSerializer.serialize(attachmentReference.getDocumentReference()));
            query.setParameter("type", attachmentReference.getType().getLowerCase());
            query.setParameter("attachmentName", attachmentReference.getName());
            return query;
        });
    }

    private List<DocumentReference> innerLoadBacklinks(XWikiContext inputxcontext,
        Function<Session, Query<String>> queryBuilder) throws XWikiException
    {
        return executeRead(inputxcontext, session -> {
            try {
                // Note: Ideally the method should return a Set but it would break the current API.

                // TODO: We use a Set here so that we don't get duplicates. In the future, when we can reference a page
                // in
                // another language using a syntax, we should modify this code to return one DocumentReference per
                // language
                // found. To implement this we need to be able to either serialize the reference with the language
                // information
                // or add some new column for the XWikiLink table in the database.
                Set<DocumentReference> backlinkReferences = new HashSet<>();

                Query<String> apply = queryBuilder.apply(session);
                List<String> backlinkNames = apply.list();

                // Convert strings into references
                for (String backlinkName : backlinkNames) {
                    DocumentReference backlinkreference =
                        this.currentMixedDocumentReferenceResolver.resolve(backlinkName);
                    backlinkReferences.add(backlinkreference);
                }

                return new ArrayList<>(backlinkReferences);
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_BACKLINKS, "Exception while loading backlinks",
                    e);
            }
        });
    }

    /**
     * @deprecated since 2.2M2 use {@link #loadBacklinks(DocumentReference, boolean, XWikiContext)}
     */
    @Deprecated
    @Override
    public List<String> loadBacklinks(String fullName, XWikiContext inputxcontext, boolean bTransaction)
        throws XWikiException
    {
        List<String> backlinkNames = new ArrayList<>();
        List<DocumentReference> backlinkReferences =
            loadBacklinks(this.currentMixedDocumentReferenceResolver.resolve(fullName), bTransaction, inputxcontext);
        for (DocumentReference backlinkReference : backlinkReferences) {
            backlinkNames.add(this.localEntityReferenceSerializer.serialize(backlinkReference));
        }
        return backlinkNames;
    }

    private Set<XWikiLink> extractLinks(XWikiDocument doc, XWikiContext context)
    {
        Set<XWikiLink> links = new LinkedHashSet<>();

        String fullName = this.localEntityReferenceSerializer.serialize(doc.getDocumentReference());

        // Add entity references.
        for (EntityReference entityReference : doc.getUniqueLinkedEntities(context)) {
            XWikiLink wikiLink = new XWikiLink();

            wikiLink.setDocId(doc.getId());
            wikiLink.setFullName(fullName);

            // getUniqueLinkedEntities() returns both DOCUMENT and PAGE references (and ATTACHMENT and
            // PAGE_ATTACHMENT references). If the reference is a PageReference (or a PageAttachmentReference) then
            // we can't know if it points to a terminal page or a non-terminal one, and thus we need to get the
            // document to check if it exists, starting with the non-terminal one since "[[page:test]]" points
            // first to the non-terminal page when it exists.
            EntityReference documentReferenceToSerialize = convertToDocumentReference(entityReference);
            wikiLink.setLink(this.compactWikiEntityReferenceSerializer.serialize(documentReferenceToSerialize));
            boolean isAttachmentReference = false;
            if (Objects.equals(entityReference.getType(), EntityType.ATTACHMENT)
                || Objects.equals(entityReference.getType(), EntityType.PAGE_ATTACHMENT))
            {
                wikiLink.setAttachmentName(entityReference.getName());
                isAttachmentReference = true;
            }
            wikiLink.setType(
                isAttachmentReference ? EntityType.ATTACHMENT.getLowerCase() : EntityType.DOCUMENT.getLowerCase());

            links.add(wikiLink);
        }

        // Add included pages.
        List<String> includedPages = doc.getIncludedPages(context);
        for (String includedPage : includedPages) {
            XWikiLink wikiLink = new XWikiLink();

            wikiLink.setDocId(doc.getId());
            wikiLink.setFullName(fullName);
            wikiLink.setLink(includedPage);
            wikiLink.setType(EntityType.DOCUMENT.getLowerCase());
            links.add(wikiLink);
        }

        return links;
    }

    @Override
    @Deprecated(since = "14.8RC1")
    public void saveLinks(XWikiDocument doc, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        // Extract the links
        Set<XWikiLink> links = extractLinks(doc, context);

        // Save the links
        executeWrite(context, session -> {
            // We delete the existing links before saving the newly analyzed ones. Unless non exists yet.
            if (countLinks(doc.getId(), context, false) > 0) {
                deleteLinks(doc.getId(), context, false);
            }

            // necessary to blank links from doc
            context.remove("links");

            if (!links.isEmpty()) {
                // Get link size limit
                int linkMaxSize = getLimitSize(context, XWikiLink.class, "link");

                // Save the links.
                for (XWikiLink wikiLink : links) {
                    // Verify that the link reference isn't larger than the maximum size of the field since otherwise
                    // that would lead to a DB error that would result in a fatal error, and the user would have a hard
                    // time understanding why his page failed to be saved.
                    if (wikiLink.getLink().length() <= linkMaxSize) {
                        session.save(wikiLink);
                    } else {
                        this.logger.warn("Could not store backlink [{}] because the link reference [{}] is too big",
                            wikiLink, wikiLink.getLink());
                    }
                }
            }

            return null;
        });
    }

    private EntityReference convertToDocumentReference(EntityReference entityReference)
    {
        // The passed entityReference can of type DOCUMENT, ATTACHMENT, PAGE or PAGE_ATTACHMENT.
        EntityReference documentReference = entityReference;
        if (documentReference instanceof PageAttachmentReference) {
            documentReference = documentReference.extractReference(EntityType.PAGE);
        }
        if (documentReference instanceof PageReference) {
            // If the reference is a PageReference then we can't know if it points to a terminal page or a
            // non-terminal one, and thus we need to resolve it.
            documentReference =
                this.currentPageReferenceDocumentReferenceResolver.resolve((PageReference) documentReference);
        } else {
            documentReference = documentReference.extractReference(EntityType.DOCUMENT);
        }
        return documentReference;
    }

    @Override
    @Deprecated(since = "14.8RC1")
    public void deleteLinks(long docId, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        executeWrite(inputxcontext, session -> {
            try {
                Query<?> query = session.createQuery("delete from XWikiLink as link where link.id.docId = :docId");
                query.setParameter("docId", docId);
                query.executeUpdate();
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_LINKS, "Exception while deleting links", e);
            }

            return null;
        });
    }

    public void getContent(XWikiDocument doc, StringBuffer buf)
    {
        buf.append(doc.getContent());
    }

    @Override
    public List<String> getClassList(XWikiContext inputxcontext) throws XWikiException
    {
        return executeRead(inputxcontext, session -> {
            try {
                Query<String> query = session.createQuery("select doc.fullName from XWikiDocument as doc "
                    + "where (doc.xWikiClassXML is not null and doc.xWikiClassXML like '<%')", String.class);
                List<String> list = new ArrayList<>();
                list.addAll(query.list());

                return list;
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH, "Exception while searching class list", e);
            }
        });
    }

    private <T> Query<T> createQuery(Session session, String statement, Collection<?> parameterValues)
    {
        Query<T> query = session.createQuery(statement);

        injectParameterListToQuery(LegacySessionImplementor.containsLegacyOrdinalStatement(statement) ? 0 : 1, query,
            parameterValues);

        return query;
    }

    /**
     * Add values into named query.
     *
     * @param parameterId the parameter id to increment.
     * @param query the query to fill.
     * @param parameterValues the values to add to query.
     * @return the id of the next parameter to add.
     */
    private int injectParameterListToQuery(int parameterId, Query<?> query, Collection<?> parameterValues)
    {
        int index = parameterId;

        if (parameterValues != null) {
            for (Iterator<?> valueIt = parameterValues.iterator(); valueIt.hasNext(); ++index) {
                injectParameterToQuery(index, query, valueIt.next());
            }
        }

        return index;
    }

    /**
     * Add value into named query.
     *
     * @param parameterId the parameter id to increment.
     * @param query the query to fill.
     * @param parameterValue the values to add to query.
     */
    private void injectParameterToQuery(int parameterId, Query<?> query, Object parameterValue)
    {
        query.setParameter(parameterId, parameterValue);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        return searchDocumentReferences(parametrizedSqlClause, 0, 0, parameterValues, context);
    }

    @Override
    public List<String> searchDocumentsNames(String parametrizedSqlClause, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        return searchDocumentsNames(parametrizedSqlClause, 0, 0, parameterValues, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb, int start,
        List<?> parameterValues, XWikiContext context) throws XWikiException
    {
        String sql = createSQLQuery("select distinct doc.fullName", parametrizedSqlClause);
        return searchDocumentReferencesInternal(sql, nb, start, parameterValues, context);
    }

    @Override
    public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        String sql = createSQLQuery("select distinct doc.fullName", parametrizedSqlClause);
        return searchDocumentsNamesInternal(sql, nb, start, parameterValues, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context) throws XWikiException
    {
        return searchDocumentReferences(wheresql, 0, 0, "", context);
    }

    @Override
    public List<String> searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException
    {
        return searchDocumentsNames(wheresql, 0, 0, "", context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return searchDocumentReferences(wheresql, nb, start, "", context);
    }

    @Override
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return searchDocumentsNames(wheresql, nb, start, "", context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        String sql = createSQLQuery("select distinct doc.fullName", wheresql);
        return searchDocumentReferencesInternal(sql, nb, start, Collections.EMPTY_LIST, context);
    }

    @Override
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        String sql = createSQLQuery("select distinct doc.fullName", wheresql);
        return searchDocumentsNamesInternal(sql, nb, start, Collections.EMPTY_LIST, context);
    }

    @Override
    public <T> List<T> search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return search(sql, nb, start, (List<?>) null, context);
    }

    @Override
    public <T> List<T> search(String sql, int nb, int start, List<?> parameterValues, XWikiContext context)
        throws XWikiException
    {
        return search(sql, nb, start, null, parameterValues, context);
    }

    @Override
    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return search(sql, nb, start, whereParams, null, context);
    }

    @Override
    public <T> List<T> search(final String sql, int nb, int start, Object[][] whereParams, List<?> parameterValues,
        XWikiContext inputxcontext) throws XWikiException
    {
        if (sql == null) {
            return null;
        }

        return executeRead(inputxcontext, session -> {
            try {
                boolean legacyOrdinal = LegacySessionImplementor.containsLegacyOrdinalStatement(sql);

                String statement = sql;

                if (whereParams != null) {
                    statement +=
                        generateWhereStatement(whereParams, legacyOrdinal ? -1 : CollectionUtils.size(parameterValues));
                }

                statement = filterSQL(statement);
                Query<T> query = session.createQuery(statement);

                injectParameterListToQuery(legacyOrdinal ? 0 : 1, query, parameterValues);

                if (whereParams != null) {
                    int parameterIndex = CollectionUtils.size(parameterValues);
                    if (!legacyOrdinal) {
                        ++parameterIndex;
                    }
                    for (Object[] whereParam : whereParams) {
                        query.setParameter(parameterIndex++, whereParam[1]);
                    }
                }

                if (start > 0) {
                    query.setFirstResult(start);
                }
                if (nb > 0) {
                    query.setMaxResults(nb);
                }
                List<T> list = new ArrayList<>();
                list.addAll(query.list());

                return list;
            } catch (Exception e) {
                Object[] args = { sql };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching documents with sql {0}", e, args);
            }
        });
    }

    private String generateWhereStatement(Object[][] whereParams, int previousIndex)
    {
        StringBuilder str = new StringBuilder();

        int index = previousIndex;

        str.append(" where ");
        for (int i = 0; i < whereParams.length; i++) {
            if (i > 0) {
                if (whereParams[i - 1].length >= 4 && whereParams[i - 1][3] != "" && whereParams[i - 1][3] != null) {
                    str.append(" ");
                    str.append(whereParams[i - 1][3]);
                    str.append(" ");
                } else {
                    str.append(" and ");
                }
            }
            str.append(whereParams[i][0]);
            if (whereParams[i].length >= 3 && whereParams[i][2] != "" && whereParams[i][2] != null) {
                str.append(" ");
                str.append(whereParams[i][2]);
                str.append(" ");
            } else {
                str.append(" = ");
            }
            str.append(" ?");
            if (index > -1) {
                str.append(++index);
            }
        }

        return str.toString();
    }

    public List search(Query query, int nb, int start, XWikiContext inputxcontext) throws XWikiException
    {
        if (query == null) {
            return null;
        }

        return executeRead(inputxcontext, session -> {
            try {
                if (start > 0) {
                    query.setFirstResult(start);
                }
                if (nb > 0) {
                    query.setMaxResults(nb);
                }
                Iterator it = query.list().iterator();
                List list = new ArrayList<>();
                while (it.hasNext()) {
                    list.add(it.next());
                }

                return list;
            } catch (Exception e) {
                Object[] args = { query.toString() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching documents with sql {0}", e, args);
            }
        });
    }

    @Override
    public int countDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        String sql = createSQLQuery("select count(distinct doc.fullName)", wheresql);
        List<Number> l = search(sql, 0, 0, context);
        return l.get(0).intValue();
    }

    @Override
    public int countDocuments(String parametrizedSqlClause, List<?> parameterValues, XWikiContext context)
        throws XWikiException
    {
        String sql = createSQLQuery("select count(distinct doc.fullName)", parametrizedSqlClause);
        List l = search(sql, 0, 0, parameterValues, context);
        return ((Number) l.get(0)).intValue();
    }

    /**
     * @deprecated since 2.2M1 used {@link #searchDocumentReferencesInternal(String, int, int, List, XWikiContext)}
     */
    @Deprecated
    private List<String> searchDocumentsNamesInternal(String sql, int nb, int start, List parameterValues,
        XWikiContext context) throws XWikiException
    {
        List<String> documentNames = new ArrayList<>();
        for (DocumentReference reference : searchDocumentReferencesInternal(sql, nb, start, parameterValues, context)) {
            documentNames.add(this.compactWikiEntityReferenceSerializer.serialize(reference));
        }
        return documentNames;
    }

    /**
     * @since 2.2M1
     */
    private List<DocumentReference> searchDocumentReferencesInternal(String sql, int nb, int start,
        List<?> parameterValues, XWikiContext inputxcontext) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            List<DocumentReference> documentReferences = new ArrayList<>();

            // Construct a reference, using the current wiki as the wiki reference name. This is because the wiki
            // name is not stored in the database for document references.
            WikiReference wikiReference = new WikiReference(context.getWikiId());
            for (Object result : this.searchGenericInternal(sql, nb, start, parameterValues, context)) {
                // The select always contains several elements in case of order by so we have to support both Object[]
                // and
                // String
                String referenceString;
                if (result instanceof String) {
                    referenceString = (String) result;
                } else {
                    referenceString = (String) ((Object[]) result)[0];
                }

                DocumentReference reference =
                    this.defaultDocumentReferenceResolver.resolve(referenceString, wikiReference);

                documentReferences.add(reference);
            }

            return documentReferences;
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * @since 2.2M1
     */
    private <T> List<T> searchGenericInternal(String sql, int nb, int start, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        return executeRead(context, session -> {
            try {
                Query query = createQuery(session, filterSQL(sql), parameterValues);

                if (start > 0) {
                    query.setFirstResult(start);
                }
                if (nb > 0) {
                    query.setMaxResults(nb);
                }
                Iterator<T> it = query.list().iterator();
                List<T> list = new ArrayList<>();
                while (it.hasNext()) {
                    list.add(it.next());
                }

                return list;
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching documents with SQL [{0}]", e, new Object[] { sql });
            }
        });
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, customMapping, checkRight, nb, start, null, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List<?> parameterValues, XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        // Search documents
        List documentDatas = new ArrayList<>();
        boolean bTransaction = true;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            String sql;
            if (distinctbylanguage) {
                sql = createSQLQuery("select distinct doc.fullName, doc.language", wheresql);
            } else {
                sql = createSQLQuery("select distinct doc.fullName", wheresql);
            }

            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer(HINT, sql);
            }

            checkHibernate(context);
            if (bTransaction) {
                // Inject everything until we know what's needed
                SessionFactory sfactory =
                    customMapping ? injectCustomMappingsInSessionFactory(context) : getSessionFactory();
                bTransaction = beginTransaction(sfactory, context);
            }
            try {
                Session session = getSession(context);

                Query query = createQuery(session, filterSQL(sql), parameterValues);

                if (start > 0) {
                    query.setFirstResult(start);
                }
                if (nb > 0) {
                    query.setMaxResults(nb);
                }
                documentDatas.addAll(query.list());
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } finally {
                if (bTransaction) {
                    try {
                        endTransaction(context, false);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH, "Exception while searching documents with SQL [{0}]",
                e, new Object[] { wheresql });
        } finally {
            restoreExecutionXContext();

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer(HINT);
            }
        }

        // Resolve documents. We use two separated sessions because rights service could need to switch database to
        // check rights
        List<XWikiDocument> documents = new ArrayList<>();
        WikiReference currentWikiReference = new WikiReference(context.getWikiId());
        for (Object result : documentDatas) {
            String fullName;
            String locale = null;
            if (result instanceof String) {
                fullName = (String) result;
            } else {
                fullName = (String) ((Object[]) result)[0];
                if (distinctbylanguage) {
                    locale = (String) ((Object[]) result)[1];
                }
            }

            XWikiDocument doc =
                new XWikiDocument(this.defaultDocumentReferenceResolver.resolve(fullName, currentWikiReference));
            if (checkRight) {
                if (!context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), doc.getFullName(),
                    context))
                {
                    continue;
                }
            }

            DocumentReference documentReference = doc.getDocumentReference();
            if (distinctbylanguage) {
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                if (StringUtils.isEmpty(locale)) {
                    documents.add(document);
                } else {
                    documents.add(document.getTranslatedDocument(locale, context));
                }
            } else {
                documents.add(context.getWiki().getDocument(documentReference, context));
            }
        }

        return documents;
    }

    /**
     * @param queryPrefix the start of the SQL query (for example "select distinct doc.space, doc.name")
     * @param whereSQL the where clause to append
     * @return the full formed SQL query, to which the order by columns have been added as returned columns (this is
     *     required for example for HSQLDB).
     */
    protected String createSQLQuery(String queryPrefix, String whereSQL)
    {
        StringBuilder sql = new StringBuilder(queryPrefix);

        String normalizedWhereSQL;
        if (StringUtils.isBlank(whereSQL)) {
            normalizedWhereSQL = "";
        } else {
            normalizedWhereSQL = whereSQL.trim();
        }

        sql.append(getColumnsForSelectStatement(normalizedWhereSQL));
        sql.append(" from XWikiDocument as doc");

        if (!normalizedWhereSQL.equals("")) {
            if ((!normalizedWhereSQL.startsWith("where")) && (!normalizedWhereSQL.startsWith(","))) {
                sql.append(" where ");
            } else {
                sql.append(" ");
            }
            sql.append(normalizedWhereSQL);
        }

        return sql.toString();
    }

    /**
     * @param whereSQL the SQL where clause
     * @return the list of columns to return in the select clause as a string starting with ", " if there are columns or
     *     an empty string otherwise. The returned columns are extracted from the where clause. One reason for doing so
     *     is because HSQLDB only support SELECT DISTINCT SQL statements where the columns operated on are returned from
     *     the query.
     */
    protected String getColumnsForSelectStatement(String whereSQL)
    {
        StringBuilder columns = new StringBuilder();

        int orderByPos = whereSQL.toLowerCase().indexOf("order by");
        if (orderByPos >= 0) {
            String orderByStatement = whereSQL.substring(orderByPos + "order by".length() + 1);
            StringTokenizer tokenizer = new StringTokenizer(orderByStatement, ",");
            while (tokenizer.hasMoreTokens()) {
                String column = tokenizer.nextToken().trim();
                // Remove "desc" or "asc" from the column found
                column = StringUtils.removeEndIgnoreCase(column, " desc");
                column = StringUtils.removeEndIgnoreCase(column, " asc");
                columns.append(", ").append(column.trim());
            }
        }

        return columns.toString();
    }

    @Override
    @Deprecated
    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
    {
        return isCustomMappingValid(bclass, custommapping1);
    }

    @Override
    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1)
    {
        try {
            Metadata metadata = this.store.getMetadata(bclass.getName(), custommapping1, null);

            return isValidCustomMapping(bclass, metadata);
        } catch (Exception e) {
            return false;
        }
    }

    private SessionFactory injectCustomMappingsInSessionFactory(XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        // If we haven't turned of dynamic custom mappings we should not inject them
        if (!context.getWiki().hasDynamicCustomMappings()) {
            return getSessionFactory();
        }

        boolean result = injectCustomMappings(doc, context);
        if (!result) {
            return getSessionFactory();
        }

        return getConfiguration().buildSessionFactory();
    }

    @Override
    public void injectCustomMappings(XWikiContext context) throws XWikiException
    {
        injectCustomMappingsInSessionFactory(context);
    }

    @Override
    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
        Configuration config = getConfiguration();
        injectInSessionFactory(config);
    }

    public SessionFactory injectCustomMappingsInSessionFactory(BaseClass bclass, XWikiContext context)
        throws XWikiException
    {
        boolean result = injectCustomMapping(bclass, context);
        if (result == false) {
            return getSessionFactory();
        }

        Configuration config = getConfiguration();
        return injectInSessionFactory(config);
    }

    private SessionFactory injectInSessionFactory(Configuration config)
    {
        return config.buildSessionFactory();
    }

    public SessionFactory injectCustomMappingsInSessionFactory(XWikiContext inputxcontext) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            // If we haven't turned of dynamic custom mappings we should not inject them
            if (context.getWiki().hasDynamicCustomMappings() == false) {
                return getSessionFactory();
            }

            List<XWikiDocument> list;
            list = searchDocuments(" where (doc.xWikiClassXML is not null and doc.xWikiClassXML like '<%')", true,
                false, false, 0, 0, context);
            boolean result = false;

            for (XWikiDocument doc : list) {
                if (!doc.getXClass().getFieldList().isEmpty()) {
                    result |= injectCustomMapping(doc.getXClass(), context);
                }
            }

            if (!result) {
                return getSessionFactory();
            }

            Configuration config = getConfiguration();
            return injectInSessionFactory(config);
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext inputxcontext) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            // If we haven't turned of dynamic custom mappings we should not inject them
            if (context.getWiki().hasDynamicCustomMappings() == false) {
                return false;
            }

            boolean result = false;
            for (List<BaseObject> objectsOfType : doc.getXObjects().values()) {
                for (BaseObject object : objectsOfType) {
                    if (object != null) {
                        result |= injectCustomMapping(object.getXClass(context), context);
                        // Each class must be mapped only once
                        break;
                    }
                }
            }
            return result;
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * @param className the name of the class to map
     * @param customMapping the custom mapping to inject for this class
     * @param inputxcontext the current XWikiContext
     * @return a boolean indicating if the mapping has been added to the current hibernate configuration, and a reload
     *     of the factory is required.
     * @throws XWikiException if an error occurs
     * @since 4.0M1
     */
    public boolean injectCustomMapping(String className, String customMapping, XWikiContext inputxcontext)
        throws XWikiException
    {
        // If we haven't turned of dynamic custom mappings we should not inject them
        if (!this.hibernateConfiguration.hasDynamicCustomMappings()) {
            return false;
        }

        // Don't add a mapping that's already there
        if (this.store.getConfigurationMetadata() != null
            && this.store.getConfigurationMetadata().getEntityBinding(className) != null)
        {
            return false;
        }

        this.store.getConfiguration().addInputStream(
            new ByteArrayInputStream(makeMapping(className, customMapping).getBytes(StandardCharsets.UTF_8)));

        // Rebuild to take into account the new mapping
        this.store.build();

        return true;
    }

    @Override
    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext inputxcontext) throws XWikiException
    {
        return injectCustomMapping(doc1class);
    }

    @Override
    public boolean injectCustomMapping(BaseClass doc1class) throws XWikiException
    {
        if (!doc1class.hasExternalCustomMapping()) {
            return false;
        }

        if (!isCustomMappingValid(doc1class, doc1class.getCustomMapping())) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING, "Invalid Custom Mapping");
        }

        return injectCustomMapping(doc1class.getName(), doc1class.getCustomMapping(), null);
    }

    private boolean isValidCustomMapping(BaseClass bclass, Metadata metadata)
    {
        PersistentClass mapping = metadata.getEntityBinding(bclass.getName());
        if (mapping == null) {
            return true;
        }

        Iterator<Property> it = mapping.getPropertyIterator();
        while (it.hasNext()) {
            Property hibprop = it.next();
            String propname = hibprop.getName();
            PropertyClass propclass = (PropertyClass) bclass.getField(propname);
            if (propclass == null) {
                this.logger.warn("Mapping contains invalid field name [{}]", propname);
                return false;
            }

            boolean result = isValidColumnType(hibprop.getValue().getType().getName(), propclass.getClassName());
            if (!result) {
                this.logger.warn("Mapping contains invalid type in field [{}]", propname);

                return false;
            }
        }

        return true;
    }

    @Override
    public List<String> getCustomMappingPropertyList(BaseClass bclass)
    {
        List<String> list = new ArrayList<>();
        Metadata metadata;
        if (bclass.hasExternalCustomMapping()) {
            metadata = this.store.getMetadata(bclass.getName(), bclass.getCustomMapping(), null);
        } else {
            metadata = this.store.getConfigurationMetadata();
        }
        PersistentClass mapping = metadata.getEntityBinding(bclass.getName());
        if (mapping == null) {
            return null;
        }

        Iterator<Property> it = mapping.getPropertyIterator();
        while (it.hasNext()) {
            Property hibprop = it.next();
            String propname = hibprop.getName();
            list.add(propname);
        }
        return list;
    }

    private boolean isValidColumnType(String name, String className)
    {
        String[] validtypes = this.validTypesMap.get(className);
        if (validtypes == null) {
            return true;
        } else {
            return ArrayUtils.contains(validtypes, name);
        }
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, null, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, List<?> parameterValues, XWikiContext context)
        throws XWikiException
    {
        return searchDocuments(wheresql, 0, 0, parameterValues, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, XWikiContext context)
        throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, 0, 0, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, customMapping, 0, 0, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return searchDocuments(wheresql, nb, start, null, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, true, nb, start, parameterValues, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        List<?> parameterValues, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, false, nb, start, parameterValues, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, nb, start, null, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, customMapping, nb, start, null, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, List<?> parameterValues, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, customMapping, true, nb, start, parameterValues, context);
    }

    @Override
    public List<String> getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            return getTranslationList(doc.getDocumentReference());
        } catch (QueryException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                "Failed to retrieve the list of translations for [{0}]", e,
                new Object[] { doc.getDocumentReference() });
        }
    }

    private List<String> getTranslationList(DocumentReference documentReference) throws QueryException
    {
        // Note that the query is made to work with Oracle which treats empty strings as null.
        String hql = "select doc.language from XWikiDocument as doc where doc.space = :space and doc.name = :name "
            + "and (doc.language <> '' or (doc.language is not null and '' is null))";
        org.xwiki.query.Query query = getQueryManager().createQuery(hql, org.xwiki.query.Query.HQL);
        query.setWiki(documentReference.getWikiReference().getName());
        query.bindValue("space", this.localEntityReferenceSerializer.serialize(documentReference.getParent()));
        query.bindValue("name", documentReference.getName());
        return query.execute();
    }

    @Override
    public QueryManager getQueryManager()
    {
        return this.queryManager;
    }

    /**
     * This is in response to the fact that Hibernate interprets backslashes differently from the database. Our solution
     * is to simply replace all instances of \ with \\ which makes the first backslash escape the second.
     *
     * @param sql the uncleaned sql.
     * @return same as sql except it is guarenteed not to contain groups of odd numbers of backslashes.
     * @since 2.4M1
     */
    private String filterSQL(String sql)
    {
        return StringUtils.replace(sql, "\\", "\\\\");
    }

    private String getDefaultAttachmentContentStore(XWikiContext xcontext)
    {
        XWikiAttachmentStoreInterface store = xcontext.getWiki().getDefaultAttachmentContentStore();

        if (store != null && store != this.attachmentContentStore) {
            return store.getHint();
        }

        return null;
    }

    private String getDefaultAttachmentArchiveStore(XWikiContext xcontext)
    {
        AttachmentVersioningStore store = xcontext.getWiki().getDefaultAttachmentArchiveStore();

        if (store != null && store != this.attachmentArchiveStore) {
            return store.getHint();
        }

        return null;
    }

    private XWikiAttachmentStoreInterface getXWikiAttachmentStoreInterface(XWikiAttachment attachment)
        throws ComponentLookupException
    {
        String storeHint = attachment.getContentStore();

        if (storeHint != null && !storeHint.equals(HINT)) {
            return this.componentManager.getInstance(XWikiAttachmentStoreInterface.class, storeHint);
        }

        return this.attachmentContentStore;
    }

    private AttachmentVersioningStore getAttachmentVersioningStore(XWikiAttachment attachment)
        throws ComponentLookupException
    {
        String storeHint = attachment.getArchiveStore();

        if (storeHint != null && !storeHint.equals(HINT)) {
            return this.componentManager.getInstance(AttachmentVersioningStore.class, storeHint);
        }

        return this.attachmentArchiveStore;
    }

    @Override
    public int getLimitSize(XWikiContext context, Class<?> entityType, String propertyName)
    {
        return this.store.getLimitSize(entityType, propertyName);
    }

    private long countLinks(long docId, XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        return executeRead(inputxcontext, session -> {
            try {
                Query<Long> query =
                    session.createQuery("select count(*) from XWikiLink as link where link.id.docId = :docId")
                        .setParameter("docId", docId);
                return query.getSingleResult();
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_BACKLINKS, "Exception while count backlinks", e);
            }
        });
    }
}
