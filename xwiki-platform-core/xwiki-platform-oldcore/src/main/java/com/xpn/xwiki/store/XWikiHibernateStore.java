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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.store.UnexpectedException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocument.XWikiAttachmentToRemove;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseElement;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.stats.impl.XWikiStats;
import com.xpn.xwiki.store.migration.MigrationRequiredException;
import com.xpn.xwiki.util.Util;

/**
 * The XWiki Hibernate database driver.
 *
 * @version $Id$
 */
@Component
@Named("hibernate")
@Singleton
public class XWikiHibernateStore extends XWikiHibernateBaseStore implements XWikiStoreInterface
{
    @Inject
    private Logger logger;

    /**
     * QueryManager for this store.
     */
    @Inject
    private QueryManager queryManager;

    /** Needed so we can register an event to trap logout and delete held locks. */
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

    private Map<String, String[]> validTypesMap = new HashMap<String, String[]>();

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
        super.initialize();
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
        this.validTypesMap = new HashMap<String, String[]>();
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.StringClass", string_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.TextAreaClass", string_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.PasswordClass", string_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.NumberClass", number_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.DateClass", date_types);
        this.validTypesMap.put("com.xpn.xwiki.objects.classes.BooleanClass", boolean_types);
    }

    @Override
    public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException
    {
        boolean available;

        boolean bTransaction = true;
        String database = context.getWikiId();

        try {
            bTransaction = beginTransaction(context);
            Session session = getSession(context);

            // Capture Logs since we voluntarily generate storage errors to check if the wiki already exists and
            // we don't want to pollute application logs with "normal errors"...
            if (!this.logger.isDebugEnabled()) {
                this.loggerManager.pushLogListener(null);
            }

            context.setWikiId(wikiName);
            try {
                setDatabase(session, context);
                available = false;
            } catch (XWikiException e) {
                // Failed to switch to database. Assume it means database does not exists.
                available = !(e.getCause() instanceof MigrationRequiredException);
            }
        } catch (Exception e) {
            Object[] args = { wikiName };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DATABASE,
                "Exception while listing databases to search for {0}", e, args);
        } finally {
            context.setWikiId(database);
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }

            // Restore proper logging
            if (!this.logger.isDebugEnabled()) {
                this.loggerManager.popLogListener();
            }
        }

        return available;
    }

    @Override
    public void createWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        boolean bTransaction = true;
        String database = context.getWikiId();
        Statement stmt = null;
        try {
            bTransaction = beginTransaction(context);
            Session session = getSession(context);
            Connection connection = session.connection();
            stmt = connection.createStatement();

            String schema = getSchemaFromWikiName(wikiName, context);
            String escapedSchema = escapeSchema(schema, context);

            DatabaseProduct databaseProduct = getDatabaseProductName();
            if (DatabaseProduct.ORACLE == databaseProduct) {
                stmt.execute("create user " + escapedSchema + " identified by " + escapedSchema);
                stmt.execute("grant resource to " + escapedSchema);
            } else if (DatabaseProduct.DERBY == databaseProduct || DatabaseProduct.DB2 == databaseProduct
                || DatabaseProduct.H2 == databaseProduct)
            {
                stmt.execute("CREATE SCHEMA " + escapedSchema);
            } else if (DatabaseProduct.HSQLDB == databaseProduct) {
                stmt.execute("CREATE SCHEMA " + escapedSchema + " AUTHORIZATION DBA");
            } else if (DatabaseProduct.MYSQL == databaseProduct) {
                // TODO: find a proper java lib to convert from java encoding to mysql charset name and collation
                if (context.getWiki().getEncoding().equals("UTF-8")) {
                    stmt.execute("create database " + escapedSchema + " CHARACTER SET utf8 COLLATE utf8_bin");
                } else {
                    stmt.execute("create database " + escapedSchema);
                }
            } else if (DatabaseProduct.POSTGRESQL == databaseProduct) {
                if (isInSchemaMode()) {
                    stmt.execute("CREATE SCHEMA " + escapedSchema);
                } else {
                    this.logger.error("Creation of a new database is currently only supported in the schema mode, "
                        + "see http://jira.xwiki.org/browse/XWIKI-8753");
                }
            } else {
                stmt.execute("create database " + escapedSchema);
            }

            endTransaction(context, true);
        } catch (Exception e) {
            Object[] args = { wikiName };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CREATE_DATABASE, "Exception while create wiki database {0}",
                e, args);
        } finally {
            context.setWikiId(database);
            try {
                if (stmt != null) {
                    stmt.close();
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

    @Override
    public void deleteWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        boolean bTransaction = true;
        String database = context.getWikiId();
        Statement stmt = null;
        try {
            bTransaction = beginTransaction(context);
            Session session = getSession(context);
            Connection connection = session.connection();
            stmt = connection.createStatement();

            String schema = getSchemaFromWikiName(wikiName, context);
            String escapedSchema = escapeSchema(schema, context);

            executeDeleteWikiStatement(stmt, getDatabaseProductName(), escapedSchema);

            endTransaction(context, true);
        } catch (Exception e) {
            Object[] args = { wikiName };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETE_DATABASE, "Exception while delete wiki database {0}",
                e, args);
        } finally {
            context.setWikiId(database);
            try {
                if (stmt != null) {
                    stmt.close();
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
     *
     * @param doc
     * @param context
     * @throws XWikiException
     */
    @Override
    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        boolean bTransaction = true;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {

            doc.setStore(this);
            checkHibernate(context);

            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate");
            }

            bTransaction = bTransaction && beginTransaction(false, context);
            Session session = getSession(context);
            String fullName = doc.getFullName();

            String sql = "select doc.fullName from XWikiDocument as doc where doc.fullName=:fullName";
            if (!doc.getLocale().equals(Locale.ROOT)) {
                sql += " and doc.language=:language";
            }
            if (monitor != null) {
                monitor.setTimerDesc("hibernate", sql);
            }
            Query query = session.createQuery(sql);
            query.setString("fullName", fullName);
            if (!doc.getLocale().equals(Locale.ROOT)) {
                query.setString("language", doc.getLocale().toString());
            }
            Iterator<String> it = query.list().iterator();
            while (it.hasNext()) {
                if (fullName.equals(it.next())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DOC, "Exception while reading document {0}", e,
                args);
        } finally {
            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }

            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate");
            }
            doc.setStore(this);
            // Make sure the database name is stored
            doc.setDatabase(context.getWikiId());

            // If the comment is larger than the max size supported by the Storage, then abbreviate it
            String comment = doc.getComment();
            if (comment != null && comment.length() > 1023) {
                doc.setComment(StringUtils.abbreviate(comment, 1023));
            }

            if (bTransaction) {
                checkHibernate(context);
                SessionFactory sfactory = injectCustomMappingsInSessionFactory(doc, context);
                bTransaction = beginTransaction(sfactory, context);
            }
            Session session = getSession(context);
            session.setFlushMode(FlushMode.COMMIT);

            // These informations will allow to not look for attachments and objects on loading
            doc.setElement(XWikiDocument.HAS_ATTACHMENTS, (doc.getAttachmentList().size() != 0));
            doc.setElement(XWikiDocument.HAS_OBJECTS, (doc.getXObjects().size() != 0));

            // Let's update the class XML since this is the new way to store it
            // TODO If all the properties are removed, the old xml stays?
            BaseClass bclass = doc.getXClass();
            if (bclass != null) {
                if (bclass.getFieldList().size() > 0) {
                    doc.setXClassXML(bclass.toXMLString());
                } else {
                    doc.setXClassXML("");
                }
                bclass.setDirty(false);
            }

            if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
                saveAttachmentList(doc, context, false);
            }
            // Remove attachments planned for removal
            if (doc.getAttachmentsToRemove().size() > 0) {
                for (XWikiAttachmentToRemove attachment : doc.getAttachmentsToRemove()) {
                    context.getWiki().getAttachmentStore()
                        .deleteXWikiAttachment(attachment.getAttachment(), false, context, false);
                }
                doc.clearAttachmentsToRemove();
            }

            // Handle the latest text file
            if (doc.isContentDirty() || doc.isMetaDataDirty()) {
                Date ndate = new Date();
                doc.setDate(ndate);
                if (doc.isContentDirty()) {
                    doc.setContentUpdateDate(ndate);
                    doc.setContentAuthorReference(doc.getAuthorReference());
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
                        context.getWiki().getVersioningStore()
                            .saveXWikiDocArchive(doc.getDocumentArchive(), false, context);

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

                            // If the version does not exist it means it's a new version so register it in the history
                            if (!containsVersion(doc, doc.getRCSVersion(), context)) {
                                context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, false, context);
                            }
                        }
                    } catch (XWikiException e) {
                        // this is a non critical error
                    }
                }
            }

            // Verify if the document already exists
            Query query =
                session.createQuery("select xwikidoc.id from XWikiDocument as xwikidoc where xwikidoc.id = :id");
            query.setLong("id", doc.getId());
            if (query.uniqueResult() == null) {
                if (doc.isContentDirty() || doc.isMetaDataDirty()) {
                    // Reset the creationDate to reflect the date of the first save, not the date of the object creation
                    doc.setCreationDate(new Date());
                }
                session.save(doc);
            } else {
                session.update(doc);
                // TODO: this is slower!! How can it be improved?
                // session.saveOrUpdate(doc);
            }

            // Remove objects planned for removal
            if (doc.getXObjectsToRemove().size() > 0) {
                for (BaseObject removedObject : doc.getXObjectsToRemove()) {
                    deleteXWikiCollection(removedObject, context, false, false);
                }
                doc.setXObjectsToRemove(new ArrayList<BaseObject>());
            }

            if (bclass != null) {
                bclass.setDocumentReference(doc.getDocumentReference());
                // Store this XWikiClass in the context so that we can use it in case of recursive usage of classes
                context.addBaseClass(bclass);
                // Update instances of the class, in case some properties changed their storage type

                // In case the current document has both a class and instances of that class, we have to take care
                // not to insert duplicate entities in the session
                Map<Long, BaseObject> localClassObjects = new HashMap<Long, BaseObject>();
                if (doc.hasElement(XWikiDocument.HAS_OBJECTS) && doc.getXObjects(doc.getDocumentReference()) != null) {
                    for (BaseObject obj : doc.getXObjects(doc.getDocumentReference())) {
                        if (obj != null) {
                            localClassObjects.put(obj.getId(), obj);
                        }
                    }
                }
                for (PropertyClass prop : (Collection<PropertyClass>) bclass.getFieldList()) {
                    // migrate values of list properties
                    if (prop instanceof StaticListClass || prop instanceof DBListClass) {
                        ListClass lc = (ListClass) prop;
                        String[] classes =
                            { DBStringListProperty.class.getName(), StringListProperty.class.getName(),
                            StringProperty.class.getName() }; // @see ListClass#newProperty()
                        for (String oldclass : classes) {
                            if (!oldclass.equals(lc.newProperty().getClass().getName())) {
                                Query q = session.createQuery("select p from " + oldclass + " as p, BaseObject as o"
                                    + " where o.className=? and p.id=o.id and p.name=?");
                                q.setString(0, bclass.getName()).setString(1, lc.getName());
                                for (Iterator it = q.list().iterator(); it.hasNext();) {
                                    BaseProperty lp = (BaseProperty) it.next();
                                    BaseProperty lp1 = lc.newProperty();
                                    lp1.setId(lp.getId());
                                    lp1.setName(lp.getName());
                                    if (lc.isMultiSelect()) {
                                        List tmp;
                                        if (lp.getValue() instanceof List) {
                                            tmp = (List) lp.getValue();
                                        } else {
                                            tmp = new ArrayList<String>(1);
                                            tmp.add(lp.getValue());
                                        }
                                        lp1.setValue(tmp);
                                    } else {
                                        Object tmp = lp.getValue();
                                        if (tmp instanceof List && ((List) tmp).size() > 0) {
                                            tmp = ((List) tmp).get(0);
                                        }
                                        lp1.setValue(tmp);
                                    }
                                    session.delete(lp);
                                    session.save(lp1);
                                }
                            }
                        }
                    }
                    // migrate values of list properties
                    else if (prop instanceof NumberClass) {
                        NumberClass numberClass = (NumberClass) prop;
                        // @see NumberClass#newProperty()
                        String[] numberPropertyTypes =
                            { IntegerProperty.class.getName(), LongProperty.class.getName(),
                            FloatProperty.class.getName(), DoubleProperty.class.getName() };
                        for (String numberPropertyType : numberPropertyTypes) {
                            if (!numberPropertyType.equals(numberClass.newProperty().getClass().getName())) {
                                Query q = session.createQuery("select p from " + numberPropertyType
                                    + " as p, BaseObject as o where o.className=?  and p.id=o.id and p.name=?");
                                q.setString(0, bclass.getName()).setString(1, numberClass.getName());
                                for (BaseProperty oldProperty : (List<BaseProperty>) q.list()) {
                                    BaseProperty newProperty = numberClass.newProperty();
                                    newProperty.setId(oldProperty.getId());
                                    newProperty.setName(oldProperty.getName());
                                    Number oldValue = (Number) oldProperty.getValue();
                                    if (oldValue != null) {
                                        // Convert the old value to the new number type.
                                        if (numberClass.getNumberType().equals("integer")) {
                                            newProperty.setValue(Integer.valueOf(oldValue.intValue()));
                                        } else if (numberClass.getNumberType().equals("float")) {
                                            newProperty.setValue(Float.valueOf(oldValue.floatValue()));
                                        } else if (numberClass.getNumberType().equals("double")) {
                                            newProperty.setValue(Double.valueOf(oldValue.doubleValue()));
                                        } else if (numberClass.getNumberType().equals("long")) {
                                            newProperty.setValue(Long.valueOf(oldValue.longValue()));
                                        }
                                    }
                                    session.delete(oldProperty);
                                    session.save(newProperty);
                                }
                            }
                        }
                    } else {
                        // General migration of properties
                        Query q = session.createQuery("select p from BaseProperty as p, BaseObject as o"
                            + " where o.className=? and p.id=o.id and p.name=? and p.classType <> ?");
                        q.setString(0, bclass.getName());
                        q.setString(1, prop.getName());
                        q.setString(2, prop.newProperty().getClassType());
                        @SuppressWarnings("unchecked")
                        List<BaseProperty> brokenProperties = q.list();
                        for (BaseProperty brokenProperty : brokenProperties) {
                            BaseProperty newProperty = prop.fromString(brokenProperty.toText());
                            BaseObject localObject = localClassObjects.get(brokenProperty.getId());
                            if (localObject != null) {
                                BaseProperty currentProperty = (BaseProperty) localObject.get(prop.getName());
                                if (currentProperty != null) {
                                    newProperty = prop.fromString(currentProperty.toText());
                                    if (newProperty != null) {
                                        localObject.put(prop.getName(), newProperty);
                                    } else {
                                        localObject.put(prop.getName(), brokenProperty);
                                    }
                                }
                            }
                            if (newProperty == null) {
                                this.logger.warn("Incompatible data migration when changing field {} of class {}",
                                    prop.getName(), prop.getClassName());
                                continue;
                            }
                            newProperty.setId(brokenProperty.getId());
                            session.delete(brokenProperty);
                            session.save(newProperty);
                        }
                    }
                }
            }

            if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
                // TODO: Delete all objects for which we don't have a name in the Map
                for (List<BaseObject> objects : doc.getXObjects().values()) {
                    for (BaseObject obj : objects) {
                        if (obj != null) {
                            obj.setDocumentReference(doc.getDocumentReference());
                            /* If the object doesn't have a GUID, create it before saving */
                            if (StringUtils.isEmpty(obj.getGuid())) {
                                obj.setGuid(UUID.randomUUID().toString());
                            }
                            saveXWikiCollection(obj, context, false);
                        }
                    }
                }
            }

            if (context.getWiki().hasBacklinks(context)) {
                try {
                    saveLinks(doc, context, true);
                } catch (Exception e) {
                    this.logger
                        .error("Failed to save links for document [{}]", doc.getDocumentReferenceWithLocale(), e);
                }
            }

            if (bTransaction) {
                endTransaction(context, true);
            }

            doc.setNew(false);

            // We need to ensure that the saved document becomes the original document
            doc.setOriginalDocument(doc.clone());

        } catch (Exception e) {
            Object[] args = { this.defaultEntityReferenceSerializer.serialize(doc.getDocumentReference()) };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "Exception while saving document {0}", e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }
        }
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

    @Override
    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        // To change body of implemented methods use Options | File Templates.
        boolean bTransaction = true;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate");
            }
            doc.setStore(this);
            checkHibernate(context);

            SessionFactory sfactory = injectCustomMappingsInSessionFactory(doc, context);
            bTransaction = bTransaction && beginTransaction(sfactory, false, context);
            Session session = getSession(context);
            session.setFlushMode(FlushMode.MANUAL);

            try {
                session.load(doc, new Long(doc.getId()));
                doc.setDatabase(context.getWikiId());
                doc.setNew(false);
                doc.setMostRecent(true);
                // Fix for XWIKI-1651
                doc.setDate(new Date(doc.getDate().getTime()));
                doc.setCreationDate(new Date(doc.getCreationDate().getTime()));
                doc.setContentUpdateDate(new Date(doc.getContentUpdateDate().getTime()));
            } catch (ObjectNotFoundException e) { // No document
                doc.setNew(true);

                // Make sure to always return a document with an original version, even for one that does not exist.
                // Allow writing more generic code.
                doc.setOriginalDocument(new XWikiDocument(doc.getDocumentReference()));

                return doc;
            }

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
                Query query = session.createQuery("from BaseObject as bobject where bobject.name = :name order by "
                    + "bobject.number");
                query.setText("name", doc.getFullName());
                @SuppressWarnings("unchecked")
                Iterator<BaseObject> it = query.list().iterator();

                EntityReference localGroupEntityReference = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE));
                DocumentReference groupsDocumentReference = new DocumentReference(context.getWikiId(),
                    localGroupEntityReference.getParent().getName(), localGroupEntityReference.getName());

                boolean hasGroups = false;
                while (it.hasNext()) {
                    BaseObject object = it.next();
                    DocumentReference classReference = object.getXClassReference();

                    if (classReference == null) {
                        continue;
                    }

                    // It seems to search before is case insensitive. And this would break the loading if we get an
                    // object which doesn't really belong to this document
                    if (!object.getDocumentReference().equals(doc.getDocumentReference())) {
                        continue;
                    }

                    BaseObject newobject;
                    if (classReference.equals(doc.getDocumentReference())) {
                        newobject = bclass.newCustomClassInstance(context);
                    } else {
                        newobject = BaseClass.newCustomClassInstance(classReference, context);
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
                    Query query2 = session.createQuery("select bobject.number, prop.value from StringProperty as prop,"
                        + "BaseObject as bobject where bobject.name = :name and bobject.className='XWiki.XWikiGroups' "
                        + "and bobject.id=prop.id.id and prop.id.name='member' order by bobject.number");
                    query2.setText("name", doc.getFullName());
                    @SuppressWarnings("unchecked")
                    Iterator<Object[]> it2 = query2.list().iterator();
                    while (it2.hasNext()) {
                        Object[] result = it2.next();
                        Integer number = (Integer) result[0];
                        String member = (String) result[1];
                        BaseObject obj = BaseClass.newCustomClassInstance(groupsDocumentReference, context);
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
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            Object[] args = { doc.getDocumentReference() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC, "Exception while reading document [{0}]", e,
                args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }
        }

        this.logger.debug("Loaded XWikiDocument: [{}]", doc.getDocumentReference());

        return doc;
    }

    @Override
    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        boolean bTransaction = true;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate");
            }
            checkHibernate(context);
            SessionFactory sfactory = injectCustomMappingsInSessionFactory(doc, context);
            bTransaction = bTransaction && beginTransaction(sfactory, context);
            Session session = getSession(context);
            session.setFlushMode(FlushMode.COMMIT);

            if (doc.getStore() == null) {
                Object[] args = { doc.getDocumentReference() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
                    "Impossible to delete document {0} if it is not loaded", null, args);
            }

            // Let's delete any attachment this document might have
            for (XWikiAttachment attachment : doc.getAttachmentList()) {
                context.getWiki().getAttachmentStore().deleteXWikiAttachment(attachment, false, context, false);
            }

            // deleting XWikiLinks
            if (context.getWiki().hasBacklinks(context)) {
                deleteLinks(doc.getId(), context, true);
            }

            // Find the list of classes for which we have an object
            // Remove properties planned for removal
            if (doc.getXObjectsToRemove().size() > 0) {
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

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            Object[] args = { doc.getDocumentReference() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC, "Exception while deleting document {0}", e,
                args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }
        }
    }

    private void checkObjectClassIsLocal(BaseCollection object, XWikiContext context) throws XWikiException
    {
        DocumentReference xclass = object.getXClassReference();
        WikiReference wikiReference = xclass.getWikiReference();
        String db = context.getWikiId();
        if (!wikiReference.getName().equals(db)) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
                "XObject [{0}] is an instance of an external XClass and cannot be persisted in this wiki [{1}].",
                null, new Object[] { this.localEntityReferenceSerializer.serialize(object.getReference()), db });
        }
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void saveXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
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
            Session session = getSession(context);

            // Verify if the property already exists
            Query query;
            if (stats) {
                query = session.createQuery("select obj.id from " + object.getClass().getName()
                    + " as obj where obj.id = :id");
            } else {
                query = session.createQuery("select obj.id from BaseObject as obj where obj.id = :id");
            }
            query.setLong("id", object.getId());
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
            List<String> handledProps = new ArrayList<String>();
            if ((bclass != null) && (bclass.hasCustomMapping()) && context.getWiki().hasCustomMappings()) {
                // save object using the custom mapping
                Map<String, Object> objmap = object.getCustomMappingMap();
                handledProps = bclass.getCustomMappingPropertyList(context);
                Session dynamicSession = session.getSession(EntityMode.MAP);
                query = session.createQuery("select obj.id from " + bclass.getName() + " as obj where obj.id = :id");
                query.setLong("id", object.getId());
                if (query.uniqueResult() == null) {
                    dynamicSession.save(bclass.getName(), objmap);
                } else {
                    dynamicSession.update(bclass.getName(), objmap);
                }

                // dynamicSession.saveOrUpdate((String) bclass.getName(), objmap);
            }

            if (object.getXClassReference() != null) {
                // Remove all existing properties
                if (object.getFieldsToRemove().size() > 0) {
                    for (int i = 0; i < object.getFieldsToRemove().size(); i++) {
                        BaseProperty prop = (BaseProperty) object.getFieldsToRemove().get(i);
                        if (!handledProps.contains(prop.getName())) {
                            session.delete(prop);
                        }
                    }
                    object.setFieldsToRemove(new ArrayList<BaseProperty>());
                }

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
        } catch (XWikiException xe) {
            throw xe;
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT, "Exception while saving object {0}", e, args);

        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, true);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void loadXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        loadXWikiCollectionInternal(object, null, context, bTransaction, false);
    }

    private void loadXWikiCollectionInternal(BaseCollection object, XWikiContext context, boolean bTransaction,
        boolean alreadyLoaded) throws XWikiException
    {
        loadXWikiCollectionInternal(object, null, context, bTransaction, alreadyLoaded);
    }

    private void loadXWikiCollectionInternal(BaseCollection object1, XWikiDocument doc, XWikiContext context,
        boolean bTransaction, boolean alreadyLoaded) throws XWikiException
    {
        BaseCollection object = object1;
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
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

                List<String> handledProps = new ArrayList<String>();
                try {
                    if ((bclass != null) && (bclass.hasCustomMapping()) && context.getWiki().hasCustomMappings()) {
                        Session dynamicSession = session.getSession(EntityMode.MAP);
                        Object map = dynamicSession.load(bclass.getName(), object.getId());
                        // Let's make sure to look for null fields in the dynamic mapping
                        bclass.fromValueMap((Map) map, object);
                        handledProps = bclass.getCustomMappingPropertyList(context);
                        for (String prop : handledProps) {
                            if (((Map) map).get(prop) == null) {
                                handledProps.remove(prop);
                            }
                        }
                    }
                } catch (Exception e) {
                }

                // Load strings, integers, dates all at once

                Query query = session.createQuery(
                    "select prop.name, prop.classType from BaseProperty as prop where prop.id.id = :id");
                query.setLong("id", object.getId());
                for (Object[] result : (List<Object[]>) query.list()) {
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
                            Object[] args =
                                { object.getName(), object.getClass(), Integer.valueOf(object.getNumber() + ""), name };
                            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                                "Exception while loading object '{0}' of class '{1}', number '{2}' and property '{3}'",
                                e, args);
                        }
                    }

                    object.addField(name, property);
                }
            }

            if (bTransaction) {
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            Object[] args = { object.getName(), object.getClass(), Integer.valueOf(object.getNumber() + "") };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                "Exception while loading object '{0}' of class '{1}' and number '{2}'", e, args);

        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }

    }

    /**
     * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
     */
    @Deprecated
    public void deleteXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction, boolean evict)
        throws XWikiException
    {
        if (object == null) {
            return;
        }
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Session session = getSession(context);

            // Let's check if the class has a custom mapping
            BaseClass bclass = object.getXClass(context);
            List<String> handledProps = new ArrayList<String>();
            if ((bclass != null) && (bclass.hasCustomMapping()) && context.getWiki().hasCustomMappings()) {
                handledProps = bclass.getCustomMappingPropertyList(context);
                Session dynamicSession = session.getSession(EntityMode.MAP);
                Object map = dynamicSession.get(bclass.getName(), object.getId());
                if (map != null) {
                    if (evict) {
                        dynamicSession.evict(map);
                    }
                    dynamicSession.delete(map);
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
        } catch (Exception e) {
            Object[] args = { object.getName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_OBJECT, "Exception while deleting object {0}", e,
                args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    private void loadXWikiProperty(PropertyInterface property, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            try {
                session.load(property, (Serializable) property);
                // In Oracle, empty string are converted to NULL. Since an undefined property is not found at all, it is
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
                this.logger.error("No data for property [{}] of object id [{}]", property.getName(), property.getId());
            }

            // TODO: understand why collections are lazy loaded
            // Let's force reading lists if there is a list
            // This seems to be an issue since Hibernate 3.0
            // Without this test ViewEditTest.testUpdateAdvanceObjectProp fails
            if (property instanceof ListProperty) {
                ((ListProperty) property).getList();
            }

            if (bTransaction) {
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            BaseCollection obj = property.getObject();
            Object[] args = { (obj != null) ? obj.getName() : "unknown", property.getName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                "Exception while loading property {1} of object {0}", e, args);

        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }
    }

    private void saveXWikiPropertyInternal(final PropertyInterface property,
        final XWikiContext context, final boolean runInOwnTransaction) throws XWikiException
    {
        // Clone runInOwnTransaction so the value passed is not altered.
        boolean bTransaction = runInOwnTransaction;
        try {
            if (bTransaction) {
                this.checkHibernate(context);
                bTransaction = this.beginTransaction(context);
            }

            final Session session = this.getSession(context);

            Query query = session.createQuery("select prop.classType from BaseProperty as prop "
                + "where prop.id.id = :id and prop.id.name= :name");
            query.setLong("id", property.getId());
            query.setString("name", property.getName());

            String oldClassType = (String) query.uniqueResult();
            String newClassType = ((BaseProperty) property).getClassType();
            if (oldClassType == null) {
                session.save(property);
            } else if (oldClassType.equals(newClassType)) {
                session.update(property);
            } else {
                // The property type has changed. We cannot simply update its value because the new value and the old
                // value are stored in different tables (we're using joined-subclass to map different property types).
                // We must delete the old property value before saving the new one and for this we must load the old
                // property from the table that corresponds to the old property type (we cannot delete and save the new
                // property or delete a clone of the new property; loading the old property from the BaseProperty table
                // doesn't work either).
                query = session.createQuery("select prop from " + oldClassType
                    + " as prop where prop.id.id = :id and prop.id.name= :name");
                query.setLong("id", property.getId());
                query.setString("name", property.getName());
                session.delete(query.uniqueResult());
                session.save(property);
            }

            ((BaseProperty) property).setValueDirty(false);

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            // Something went wrong, collect some information.
            final BaseCollection obj = property.getObject();
            final Object[] args = { (obj != null) ? obj.getName() : "unknown", property.getName() };

            // Try to roll back the transaction if this is in it's own transaction.
            try {
                if (bTransaction) {
                    this.endTransaction(context, false);
                }
            } catch (Exception ee) {
                // Not a lot we can do here if there was an exception committing and an exception rolling back.
            }

            // Throw the exception.
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                "Exception while saving property {1} of object {0}", e, args);
        }
    }

    private void loadAttachmentList(XWikiDocument doc, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            Query query = session.createQuery("from XWikiAttachment as attach where attach.docId=:docid");
            query.setLong("docid", doc.getId());
            @SuppressWarnings("unchecked")
            List<XWikiAttachment> list = query.list();
            for (XWikiAttachment attachment : list) {
                attachment.setDoc(doc);
                attachment.setMetaDataDirty(false);
            }
            doc.setAttachmentList(list);
            if (bTransaction) {
                endTransaction(context, false, false);
                bTransaction = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Object[] args = { doc.getDocumentReference() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCHING_ATTACHMENT,
                "Exception while searching attachments for documents {0}", e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }
    }

    private void saveAttachmentList(XWikiDocument doc, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            getSession(context);

            List<XWikiAttachment> list = doc.getAttachmentList();
            for (XWikiAttachment attachment : list) {
                attachment.setDoc(doc);
                saveAttachment(attachment, false, context, false);
            }

            if (bTransaction) {
                // The session is closed here, too.
                endTransaction(context, true);
            }
        } catch (Exception e) {
            Object[] args = { doc.getDocumentReference() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT_LIST,
                "Exception while saving attachments attachment list of document {0}", e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    private void saveAttachment(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        try {
            // If the comment is larger than the max size supported by the Storage, then abbreviate it
            String comment = attachment.getComment();
            if (comment != null && comment.length() > 1023) {
                attachment.setComment(StringUtils.abbreviate(comment, 1023));
            }

            // The version number must be increased and the date must be set before the attachment meta data is saved.
            // Changing the version and date after calling session.save()/session.update() "worked" (the altered version
            // was what Hibernate saved) but only if everything is done in the same transaction and as far as I know it
            // depended on undefined behavior.
            // Note that the second condition is required because there are cases when we want the attachment content to
            // be saved (see below) but we don't want the version to be increased (e.g. restore a document from recycle
            // bin, copy or import a document).
            // See XWIKI-9421: Attachment version is incremented when a document is restored from recycle bin
            if (attachment.isContentDirty() && !attachment.getDoc().isNew()) {
                attachment.updateContentArchive(context);
            }

            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Session session = getSession(context);

            Query query = session.createQuery("select attach.id from XWikiAttachment as attach where attach.id = :id");
            query.setLong("id", attachment.getId());
            if (query.uniqueResult() == null) {
                session.save(attachment);
            } else {
                session.update(attachment);
            }

            // Save the attachment content if it's marked as "dirty" (out of sync with the database).
            if (attachment.isContentDirty()) {
                // updateParent and bTransaction must be false because the content should be saved in the same
                // transaction as the attachment and if the parent doc needs to be updated, this function will do it.
                context.getWiki().getAttachmentStore().saveAttachmentContent(attachment, false, context, false);
            }

            if (parentUpdate) {
                context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, false);
            }

            if (bTransaction) {
                endTransaction(context, true);
            }

            // Mark the attachment content and metadata as not dirty.
            // Ideally this would only happen if the transaction is committed successfully but since an unsuccessful
            // transaction will most likely be accompanied by an exception, the cache will not have a chance to save
            // the copy of the document with erronious information. If this is not set here, the cache will return
            // a copy of the attachment which claims to be dirty although it isn't.
            attachment.setMetaDataDirty(false);
            if (attachment.isContentDirty()) {
                attachment.getAttachment_content().setContentDirty(false);
            }

        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getDocumentReference() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                "Exception while saving attachments for attachment {0} of document {1}", e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    // ---------------------------------------
    // Locks
    // ---------------------------------------

    @Override
    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        XWikiLock lock = null;
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            Query query = session.createQuery("select lock.docId from XWikiLock as lock where lock.docId = :docId");
            query.setLong("docId", docId);
            if (query.uniqueResult() != null) {
                lock = new XWikiLock();
                session.load(lock, new Long(docId));
            }

            if (bTransaction) {
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_LOCK, "Exception while loading lock", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }
        return lock;
    }

    @Override
    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Session session = getSession(context);

            Query query = session.createQuery("select lock.docId from XWikiLock as lock where lock.docId = :docId");
            query.setLong("docId", lock.getDocId());
            if (query.uniqueResult() == null) {
                session.save(lock);
            } else {
                session.update(lock);
            }

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_LOCK, "Exception while locking document", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Session session = getSession(context);

            session.delete(lock);

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_LOCK, "Exception while deleting lock", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
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
            this.beginTransaction(ctx);
            Session session = this.getSession(ctx);
            final Query query =
                session.createQuery("delete from XWikiLock as lock where lock.userName=:userName");
            // Using deprecated getUser() because this is how locks are created.
            // It would be a maintainibility disaster to use different code paths
            // for calculating names when creating and removing.
            query.setString("userName", ctx.getUser());
            query.executeUpdate();
            this.endTransaction(ctx, true);
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
        if (!ctx.isMainWiki()
            && ctx.isMainWiki(ctx.getUserReference().getWikiReference().getName()))
        {
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
    public List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        List<XWikiLink> links = new ArrayList<XWikiLink>();
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            Query query = session.createQuery(" from XWikiLink as link where link.id.docId = :docId");
            query.setLong("docId", docId);

            links = query.list();

            if (bTransaction) {
                endTransaction(context, false, false);
                bTransaction = false;
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_LINKS, "Exception while loading links", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }
        return links;
    }

    @Override
    public List<DocumentReference> loadBacklinks(DocumentReference documentReference, boolean bTransaction,
        XWikiContext context) throws XWikiException
    {
        // Note: Ideally the method should return a Set but it would break the current API.

        // TODO: We use a Set here so that we don't get duplicates. In the future, when we can reference a page in
        // another language using a syntax, we should modify this code to return one DocumentReference per language
        // found. To implement this we need to be able to either serialize the reference with the language information
        // or add some new column for the XWikiLink table in the database.
        Set<DocumentReference> backlinkReferences = new HashSet<DocumentReference>();

        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            // the select clause is compulsory to reach the fullName i.e. the page pointed
            Query query = session.createQuery(
                "select backlink.fullName from XWikiLink as backlink where backlink.id.link = :backlink");
            query.setString("backlink", this.localEntityReferenceSerializer.serialize(documentReference));

            @SuppressWarnings("unchecked")
            List<String> backlinkNames = query.list();

            // Convert strings into references
            for (String backlinkName : backlinkNames) {
                backlinkReferences.add(this.currentMixedDocumentReferenceResolver.resolve(backlinkName));
            }

            if (bTransaction) {
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_BACKLINKS, "Exception while loading backlinks", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }
        return new ArrayList<DocumentReference>(backlinkReferences);
    }

    /**
     * @deprecated since 2.2M2 use {@link #loadBacklinks(DocumentReference, boolean, XWikiContext)}
     */
    @Deprecated
    @Override
    public List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        List<String> backlinkNames = new ArrayList<String>();
        List<DocumentReference> backlinkReferences =
            loadBacklinks(this.currentMixedDocumentReferenceResolver.resolve(fullName), bTransaction, context);
        for (DocumentReference backlinkReference : backlinkReferences) {
            backlinkNames.add(this.localEntityReferenceSerializer.serialize(backlinkReference));
        }
        return backlinkNames;
    }

    @Override
    public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Session session = getSession(context);

            // need to delete existing links before saving the page's one
            deleteLinks(doc.getId(), context, bTransaction);

            // necessary to blank links from doc
            context.remove("links");

            if (doc.getSyntax().equals(Syntax.XWIKI_1_0)) {
                saveLinks10(doc, context, session);
            } else {
                // When not in 1.0 content get WikiLinks directly from XDOM
                Set<XWikiLink> links = doc.getUniqueWikiLinkedPages(context);
                for (XWikiLink wikiLink : links) {
                    session.save(wikiLink);
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_LINKS, "Exception while saving links", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    private void saveLinks10(XWikiDocument doc, XWikiContext context, Session session) throws XWikiException
    {
        // call to RenderEngine and converting the list of links into a list of backlinks
        // Note: We need to set the passed document as the current document as the "wiki"
        // renderer uses context.getDoc().getSpace() to find out the space name if no
        // space is specified in the link. A better implementation would be to pass
        // explicitely the current space to the render() method.
        ExecutionContext econtext = getExecution().getContext();

        List<String> links;
        try {
            // Create new clean context to avoid wiki manager plugin requests in same session
            XWikiContext renderContext = context.clone();

            renderContext.setDoc(doc);
            econtext.setProperty("xwikicontext", renderContext);

            setSession(null, renderContext);
            setTransaction(null, renderContext);

            XWikiRenderer renderer = renderContext.getWiki().getRenderingEngine().getRenderer("wiki");
            renderer.render(doc.getContent(), doc, doc, renderContext);

            links = (List<String>) renderContext.get("links");
        } catch (Exception e) {
            // If the rendering fails lets forget backlinks without errors
            links = Collections.emptyList();
        } finally {
            econtext.setProperty("xwikicontext", context);
        }

        if (links != null) {
            for (String reference : links) {
                // XWikiLink is the object declared in the Hibernate mapping
                XWikiLink link = new XWikiLink();
                link.setDocId(doc.getId());
                link.setFullName(doc.getFullName());
                link.setLink(reference);

                session.save(link);
            }
        }
    }

    @Override
    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Session session = getSession(context);

            Query query = session.createQuery("delete from XWikiLink as link where link.id.docId = :docId");
            query.setLong("docId", docId);
            query.executeUpdate();

            if (bTransaction) {
                endTransaction(context, true);
                bTransaction = false;
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_LINKS, "Exception while deleting links", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }
    }

    public void getContent(XWikiDocument doc, StringBuffer buf)
    {
        buf.append(doc.getContent());
    }

    @Override
    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        boolean bTransaction = true;
        try {
            checkHibernate(context);
            bTransaction = beginTransaction(false, context);
            Session session = getSession(context);

            Query query = session.createQuery("select doc.fullName from XWikiDocument as doc "
                + "where (doc.xWikiClassXML is not null and doc.xWikiClassXML like '<%')");
            List<String> list = new ArrayList<String>();
            list.addAll(query.list());

            if (bTransaction) {
                endTransaction(context, false, false);
            }
            return list;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH, "Exception while searching class list", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Add values into named query.
     *
     * @param parameterId the parameter id to increment.
     * @param query the query to fill.
     * @param parameterValues the values to add to query.
     * @return the id of the next parameter to add.
     */
    private int injectParameterListToQuery(int parameterId, Query query, Collection<?> parameterValues)
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
    private void injectParameterToQuery(int parameterId, Query query, Object parameterValue)
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
        String sql = createSQLQuery("select distinct doc.space, doc.name", parametrizedSqlClause);
        return searchDocumentReferencesInternal(sql, nb, start, parameterValues, context);
    }

    @Override
    public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start,
        List<?> parameterValues, XWikiContext context) throws XWikiException
    {
        String sql = createSQLQuery("select distinct doc.space, doc.name", parametrizedSqlClause);
        return searchDocumentsNamesInternal(sql, nb, start, parameterValues, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context)
        throws XWikiException
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
        String sql = createSQLQuery("select distinct doc.space, doc.name", wheresql);
        return searchDocumentReferencesInternal(sql, nb, start, Collections.EMPTY_LIST, context);
    }

    @Override
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        String sql = createSQLQuery("select distinct doc.space, doc.name", wheresql);
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
    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        boolean bTransaction = true;

        if (sql == null) {
            return null;
        }

        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate");
            }
            checkHibernate(context);
            bTransaction = beginTransaction(false, context);
            Session session = getSession(context);

            if (whereParams != null) {
                sql += generateWhereStatement(whereParams);
            }

            Query query = session.createQuery(filterSQL(sql));

            // Add values for provided HQL request containing "?" characters where to insert real
            // values.
            int parameterId = injectParameterListToQuery(0, query, parameterValues);

            if (whereParams != null) {
                for (Object[] whereParam : whereParams) {
                    query.setString(parameterId++, (String) whereParam[1]);
                }
            }

            if (start != 0) {
                query.setFirstResult(start);
            }
            if (nb != 0) {
                query.setMaxResults(nb);
            }
            List<T> list = new ArrayList<T>();
            list.addAll(query.list());
            return list;
        } catch (Exception e) {
            Object[] args = { sql };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH, "Exception while searching documents with sql {0}",
                e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }
        }
    }

    private String generateWhereStatement(Object[][] whereParams)
    {
        StringBuffer str = new StringBuffer();

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
        }
        return str.toString();
    }

    public List search(Query query, int nb, int start, XWikiContext context) throws XWikiException
    {
        boolean bTransaction = true;

        if (query == null) {
            return null;
        }

        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate", query.getQueryString());
            }
            checkHibernate(context);
            bTransaction = beginTransaction(false, context);
            if (start != 0) {
                query.setFirstResult(start);
            }
            if (nb != 0) {
                query.setMaxResults(nb);
            }
            Iterator it = query.list().iterator();
            List list = new ArrayList();
            while (it.hasNext()) {
                list.add(it.next());
            }
            if (bTransaction) {
                // The session is closed here, too.
                endTransaction(context, false, false);
                bTransaction = false;
            }
            return list;
        } catch (Exception e) {
            Object[] args = { query.toString() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH, "Exception while searching documents with sql {0}",
                e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }
        }
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
        List<String> documentNames = new ArrayList<String>();
        for (DocumentReference reference : searchDocumentReferencesInternal(sql, nb, start, parameterValues, context)) {
            documentNames.add(this.compactWikiEntityReferenceSerializer.serialize(reference));
        }
        return documentNames;
    }

    /**
     * @since 2.2M1
     */
    private List<DocumentReference> searchDocumentReferencesInternal(String sql, int nb, int start,
        List<?> parameterValues, XWikiContext context) throws XWikiException
    {
        List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
        for (Object[] result : searchGenericInternal(sql, nb, start, parameterValues, context)) {
            // Construct a reference, using the current wiki as the wiki reference name. This is because the wiki
            // name is not stored in the database for document references.
            DocumentReference reference = new DocumentReference((String) result[1],
                new SpaceReference((String) result[0], new WikiReference(context.getWikiId())));
            documentReferences.add(reference);
        }
        return documentReferences;
    }

    /**
     * @since 2.2M1
     */
    private List<Object[]> searchGenericInternal(String sql, int nb, int start, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        boolean bTransaction = false;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate", sql);
            }

            checkHibernate(context);
            bTransaction = beginTransaction(false, context);
            Session session = getSession(context);
            Query query = session.createQuery(filterSQL(sql));

            injectParameterListToQuery(0, query, parameterValues);

            if (start != 0) {
                query.setFirstResult(start);
            }
            if (nb != 0) {
                query.setMaxResults(nb);
            }
            Iterator it = query.list().iterator();
            List<Object[]> list = new ArrayList<Object[]>();
            while (it.hasNext()) {
                list.add((Object[]) it.next());
            }
            return list;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                "Exception while searching documents with SQL [{0}]", e, new Object[] { sql });
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }
        }
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException
    {
        return searchDocuments(wheresql, distinctbylanguage, customMapping, checkRight, nb, start, null, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List<?> parameterValues, XWikiContext context) throws XWikiException
    {
        // Search documents
        List<Object[]> documentDatas = new ArrayList<Object[]>();
        boolean bTransaction = true;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            String sql;
            if (distinctbylanguage) {
                sql = createSQLQuery("select distinct doc.space, doc.name, doc.language", wheresql);
            } else {
                sql = createSQLQuery("select distinct doc.space, doc.name", wheresql);
            }

            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate", sql);
            }

            checkHibernate(context);
            if (bTransaction) {
                // Inject everything until we know what's needed
                SessionFactory sfactory =
                    customMapping ? injectCustomMappingsInSessionFactory(context) : getSessionFactory();
                bTransaction = beginTransaction(sfactory, false, context);
            }
            Session session = getSession(context);

            Query query = session.createQuery(filterSQL(sql));

            injectParameterListToQuery(0, query, parameterValues);

            if (start != 0) {
                query.setFirstResult(start);
            }
            if (nb != 0) {
                query.setMaxResults(nb);
            }
            documentDatas.addAll(query.list());
            if (bTransaction) {
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                "Exception while searching documents with SQL [{0}]", e, new Object[] { wheresql });
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("hibernate");
            }
        }

        // Resolve documents. We use two separated sessions because rights service could need to switch database to
        // check rights
        List<XWikiDocument> documents = new ArrayList<XWikiDocument>();
        for (Object[] result : documentDatas) {
            XWikiDocument doc =
                new XWikiDocument(new DocumentReference(context.getWikiId(), (String) result[0], (String) result[1]));
            if (checkRight) {
                if (!context.getWiki().getRightService()
                    .hasAccessLevel("view", context.getUser(), doc.getFullName(), context)) {
                    continue;
                }
            }

            DocumentReference documentReference = doc.getDocumentReference();
            if (distinctbylanguage) {
                String language = (String) result[2];
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                if ((language == null) || (language.equals(""))) {
                    documents.add(document);
                } else {
                    documents.add(document.getTranslatedDocument(language, context));
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
     *         required for example for HSQLDB).
     */
    protected String createSQLQuery(String queryPrefix, String whereSQL)
    {
        StringBuffer sql = new StringBuffer(queryPrefix);

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
     *         an empty string otherwise. The returned columns are extracted from the where clause. One reason for doing
     *         so is because HSQLDB only support SELECT DISTINCT SQL statements where the columns operated on are
     *         returned from the query.
     */
    protected String getColumnsForSelectStatement(String whereSQL)
    {
        StringBuffer columns = new StringBuffer();

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
    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
    {
        try {
            Configuration hibconfig = getMapping(bclass.getName(), custommapping1);
            return isValidCustomMapping(bclass, hibconfig);
        } catch (Exception e) {
            return false;
        }
    }

    private SessionFactory injectCustomMappingsInSessionFactory(XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        // If we haven't turned of dynamic custom mappings we should not inject them
        if (context.getWiki().hasDynamicCustomMappings() == false) {
            return getSessionFactory();
        }

        boolean result = injectCustomMappings(doc, context);
        if (result == false) {
            return getSessionFactory();
        }

        Configuration config = getConfiguration();
        SessionFactoryImpl sfactory = (SessionFactoryImpl) config.buildSessionFactory();
        Settings settings = sfactory.getSettings();
        ConnectionProvider provider = ((SessionFactoryImpl) getSessionFactory()).getSettings().getConnectionProvider();
        Field field = null;
        try {
            field = settings.getClass().getDeclaredField("connectionProvider");
            field.setAccessible(true);
            field.set(settings, provider);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_MAPPING_INJECTION_FAILED, "Mapping injection failed", e);
        }
        return sfactory;
    }

    @Override
    public void injectCustomMappings(XWikiContext context) throws XWikiException
    {
        SessionFactory sfactory = injectCustomMappingsInSessionFactory(context);
        setSessionFactory(sfactory);
    }

    @Override
    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
        Configuration config = getConfiguration();
        setSessionFactory(injectInSessionFactory(config));
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

    private SessionFactory injectInSessionFactory(Configuration config) throws XWikiException
    {
        SessionFactoryImpl sfactory = (SessionFactoryImpl) config.buildSessionFactory();
        Settings settings = sfactory.getSettings();
        ConnectionProvider provider = ((SessionFactoryImpl) getSessionFactory()).getSettings().getConnectionProvider();
        Field field = null;
        try {
            field = settings.getClass().getDeclaredField("connectionProvider");
            field.setAccessible(true);
            field.set(settings, provider);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_MAPPING_INJECTION_FAILED, "Mapping injection failed", e);
        }
        return sfactory;
    }

    public SessionFactory injectCustomMappingsInSessionFactory(XWikiContext context) throws XWikiException
    {
        // If we haven't turned of dynamic custom mappings we should not inject them
        if (context.getWiki().hasDynamicCustomMappings() == false) {
            return getSessionFactory();
        }

        List<XWikiDocument> list;
        list = searchDocuments(" where (doc.xWikiClassXML is not null and doc.xWikiClassXML like '<%')", true, false,
            false, 0, 0, context);
        boolean result = false;

        for (XWikiDocument doc : list) {
            if (doc.getXClass().getFieldList().size() > 0) {
                result |= injectCustomMapping(doc.getXClass(), context);
            }
        }

        if (result == false) {
            return getSessionFactory();
        }

        Configuration config = getConfiguration();
        return injectInSessionFactory(config);
    }

    @Override
    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
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
    }

    /**
     * @param className the name of the class to map
     * @param custommapping the custom mapping to inject for this class
     * @param context the current XWikiContext
     * @return a boolean indicating if the mapping has been added to the current hibernate configuration, and a reload
     *         of the factory is required.
     * @throws XWikiException if an error occurs
     * @since 4.0M1
     */
    public boolean injectCustomMapping(String className, String custommapping, XWikiContext context)
        throws XWikiException
    {
        // If we haven't turned of dynamic custom mappings we should not inject them
        if (!context.getWiki().hasDynamicCustomMappings()) {
            return false;
        }

        Configuration config = getConfiguration();

        // don't add a mapping that's already there
        if (config.getClassMapping(className) != null) {
            return false;
        }

        config.addXML(makeMapping(className, custommapping));
        config.buildMappings();
        return true;
    }

    @Override
    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context) throws XWikiException
    {
        if (!doc1class.hasExternalCustomMapping()) {
            return false;
        }

        if (injectCustomMapping(doc1class.getName(), doc1class.getCustomMapping(), context)) {
            if (!isValidCustomMapping(doc1class, getConfiguration())) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING, "Invalid Custom Mapping");
            }
            return true;
        }

        return false;
    }

    private boolean isValidCustomMapping(BaseClass bclass, Configuration config)
    {
        PersistentClass mapping = config.getClassMapping(bclass.getName());
        if (mapping == null) {
            return true;
        }

        Iterator it = mapping.getPropertyIterator();
        while (it.hasNext()) {
            Property hibprop = (Property) it.next();
            String propname = hibprop.getName();
            PropertyClass propclass = (PropertyClass) bclass.getField(propname);
            if (propclass == null) {
                this.logger.warn("Mapping contains invalid field name [{}]", propname);
                return false;
            }

            boolean result = isValidColumnType(hibprop.getValue().getType().getName(), propclass.getClassName());
            if (result == false) {
                this.logger.warn("Mapping contains invalid type in field [{}]", propname);
                return false;
            }
        }

        return true;
    }

    @Override
    public List<String> getCustomMappingPropertyList(BaseClass bclass)
    {
        List<String> list = new ArrayList<String>();
        Configuration hibconfig;
        if (bclass.hasExternalCustomMapping()) {
            hibconfig = getMapping(bclass.getName(), bclass.getCustomMapping());
        } else {
            hibconfig = getConfiguration();
        }
        PersistentClass mapping = hibconfig.getClassMapping(bclass.getName());
        if (mapping == null) {
            return null;
        }

        Iterator it = mapping.getPropertyIterator();
        while (it.hasNext()) {
            Property hibprop = (Property) it.next();
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
        // Note that the query is made to work with Oracle which treats empty strings as null.
        String hql = "select doc.language from XWikiDocument as doc where doc.space = ? and doc.name = ? "
            + "and (doc.language <> '' or (doc.language is not null and '' is null))";
        ArrayList<String> params = new ArrayList<String>();
        params.add(doc.getSpace());
        params.add(doc.getName());
        List<String> list = search(hql, 0, 0, params, context);
        return (list == null) ? new ArrayList<String>() : list;
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
}
