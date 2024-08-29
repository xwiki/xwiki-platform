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
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.Execution;
import org.xwiki.logging.LoggerManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.AbstractXWikiStore;
import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.DataMigrationManager;
import com.xpn.xwiki.util.Util;

public class XWikiHibernateBaseStore extends AbstractXWikiStore
{
    /**
     * The role hint of this component.
     * 
     * @since 9.0RC1
     */
    public static final String HINT = "hibernate";

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiHibernateBaseStore.class);

    /** LoggerManager to suspend logging during normal faulty SQL operation. */
    @Inject
    protected LoggerManager loggerManager;

    @Inject
    protected HibernateStore store;

    @Inject
    protected HibernateConfiguration hibernateConfiguration;

    @Inject
    @Named(HINT)
    private DataMigrationManager dataMigrationManager;

    /** Need to get the xcontext to get the path to the hibernate.cfg.xml. */
    @Inject
    private Execution execution;

    /**
     * THis allows to initialize our storage engine. The hibernate config file path is taken from xwiki.cfg or directly
     * in the WEB-INF directory.
     *
     * @param xwiki
     * @param context
     * @deprecated 1.6M1. Use ComponentManager.lookup(String) instead.
     */
    @Deprecated
    public XWikiHibernateBaseStore(XWiki xwiki, XWikiContext context)
    {
        String path = xwiki.Param("xwiki.store.hibernate.path", "/WEB-INF/hibernate.cfg.xml");
        LOGGER.debug("Hibernate configuration file: [{}]", path);

        this.hibernateConfiguration = new HibernateConfiguration();

        setPath(path);
    }

    /**
     * Initialize the storage engine with a specific path This is used for tests.
     *
     * @param hibpath
     * @deprecated 1.6M1. Use ComponentManager.lookup(String) instead.
     */
    @Deprecated
    public XWikiHibernateBaseStore(String hibpath)
    {
        this.hibernateConfiguration = new HibernateConfiguration();

        setPath(hibpath);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public XWikiHibernateBaseStore()
    {
    }

    public String getHint()
    {
        return HINT;
    }

    /**
     * Allows to get the current hibernate config file path
     */
    public String getPath()
    {
        return this.hibernateConfiguration.getPath();
    }

    /**
     * Allows to set the current hibernate config file path
     *
     * @param hibpath
     */
    public void setPath(String hibpath)
    {
        this.hibernateConfiguration.setPath(hibpath);
    }

    /**
     * Retrieve metadata about the database used (name, version, etc).
     * <p>
     * Note that the database metadata is not cached and it's retrieved at each call. If all you need is the database
     * product name you should use {@link #getDatabaseProductName()} instead, which is cached.
     * </p>
     *
     * @return the database meta data or null if an error occurred
     * @since 6.1M1
     */
    public DatabaseMetaData getDatabaseMetaData()
    {
        return this.store.getDatabaseMetaData();
    }

    /**
     * Retrieve the current database product name.
     * <p>
     * Note that the database product name is cached for improved performances.
     * </p>
     *
     * @return the database product name, see {@link DatabaseProduct}
     * @since 4.0M1
     */
    public DatabaseProduct getDatabaseProductName()
    {
        return this.store.getDatabaseProductName();
    }

    /**
     * @return the database product name
     * @deprecated since 4.0M1 use {@link #getDatabaseProductName()}
     */
    @Deprecated
    public DatabaseProduct getDatabaseProductName(XWikiContext context)
    {
        return getDatabaseProductName();
    }

    /**
     * This get's the current session. This is set in beginTransaction
     *
     * @param inputxcontext
     */
    public Session getSession(XWikiContext inputxcontext)
    {
        return this.store.getCurrentSession();
    }

    /**
     * Allows to set the current session in the context This is set in beginTransaction
     *
     * @param session
     * @param inputxcontext
     */
    public void setSession(Session session, XWikiContext inputxcontext)
    {
        this.store.setCurrentSession(session);
    }

    /**
     * Allows to get the current transaction from the context This is set in beginTransaction
     *
     * @param inputxcontext
     */
    public Transaction getTransaction(XWikiContext inputxcontext)
    {
        return this.store.getCurrentTransaction();
    }

    /**
     * Allows to set the current transaction This is set in beginTransaction
     *
     * @param transaction
     * @param inputxcontext
     */
    public void setTransaction(Transaction transaction, XWikiContext inputxcontext)
    {
        this.store.setCurrentTransaction(transaction);
    }

    /**
     * Allows to shut down the hibernate configuration Closing all pools and connections
     *
     * @param inputxcontext
     * @throws HibernateException
     * @deprecated automatically done when the {@link HibernateSessionFactory} component is disposed
     */
    @Deprecated
    public void shutdownHibernate(XWikiContext inputxcontext) throws HibernateException
    {
        this.store.shutdownHibernate();
    }

    /**
     * Allows to update the schema to match the hibernate mapping
     *
     * @param inputxcontext
     * @throws HibernateException
     */
    public void updateSchema(XWikiContext inputxcontext) throws HibernateException
    {
        updateSchema(inputxcontext, false);
    }

    /**
     * Allows to update the schema to match the hibernate mapping
     *
     * @param inputxcontext
     * @param force defines wether or not to force the update despite the xwiki.cfg settings
     * @throws HibernateException
     */
    public void updateSchema(XWikiContext inputxcontext, boolean force) throws HibernateException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            this.store.updateDatabase(context.getWikiId(), force);
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * Convert wiki name in database/schema name.
     *
     * @param wikiName the wiki name to convert.
     * @param databaseProduct the database engine type.
     * @param inputxcontext the XWiki context.
     * @return the database/schema name.
     * @since 1.1.2
     * @since 1.2M2
     */
    protected String getSchemaFromWikiName(String wikiName, DatabaseProduct databaseProduct, XWikiContext inputxcontext)
    {
        return this.store.getDatabaseFromWikiName(wikiName, databaseProduct);
    }

    /**
     * Convert wiki name in database/schema name.
     * <p>
     * Need hibernate to be initialized.
     *
     * @param wikiId the wiki name to convert.
     * @param inputxcontext the XWiki context.
     * @return the database/schema name.
     * @since 1.1.2
     * @since 1.2M2
     */
    protected String getSchemaFromWikiName(String wikiId, XWikiContext inputxcontext)
    {
        return this.store.getDatabaseFromWikiName(wikiId);
    }

    /**
     * Convert context's database in real database/schema name.
     * <p>
     * Need hibernate to be initialized.
     *
     * @param context the XWiki context.
     * @return the database/schema name.
     * @since 1.1.2
     * @since 1.2M2
     */
    public String getSchemaFromWikiName(XWikiContext context)
    {
        return this.store.getDatabaseFromWikiName();
    }

    /**
     * This function gets the schema update scripts generated by comparing the current database with the current
     * hibernate mapping config.
     *
     * @param config
     * @param inputxcontext
     * @throws HibernateException
     * @deprecated do nothing since 11.5RC1
     */
    @Deprecated
    public String[] getSchemaUpdateScript(Configuration config, XWikiContext inputxcontext) throws HibernateException
    {
        return null;
    }

    /**
     * In the Hibernate mapping file for XWiki we use a "native" generator for some tables (deleted document and deleted
     * attachments for example - The reason we use generated ids and not custom computed ones is because we don't need
     * to address rows from these tables). For a lot of database the Dialect uses an Identity Generator (when the DB
     * supports it). PostgreSQL and Oracle don't support it and Hibernate defaults to a Sequence Generator which uses a
     * sequence named "hibernate_sequence" by default. Hibernate will normally create such a sequence automatically when
     * updating the schema (see #getSchemaUpdateScript). However the problem is that Hibernate maintains a cache of
     * sequence names per catalog and will only generate the sequence creation SQL if the sequence is not in this cache.
     * Since the main wiki is updated first the sequence named "hibernate_sequence" will be put in this cache, thus
     * preventing subwikis to automatically create sequence with the same name (see also
     * https://hibernate.atlassian.net/browse/HHH-1672). As a workaround, we create the required sequence here.
     *
     * @param schemaSQL the list of SQL commands to execute to update the schema, possibly containing the
     *            "hibernate_sequence" sequence creation
     * @param schemaName the schema name corresponding to the subwiki being updated
     * @param session the Hibernate session, used to get the Dialect object
     * @since 5.2RC1
     * @deprecated since 11.5RC1
     */
    @Deprecated
    protected void createHibernateSequenceIfRequired(String[] schemaSQL, String schemaName, Session session)
    {
        // There's no issue when in database mode, only in schema mode.
        if (isInSchemaMode()) {
            Dialect dialect = this.store.getDialect();
            if (dialect.getNativeIdentifierGeneratorStrategy().equals("sequence")) {
                // We create the sequence only if it's not already in the SQL to execute as otherwise we would get an
                // error that the sequence already exists ("relation "hibernate_sequence" already exists").
                boolean hasSequence = false;
                String sequenceSQL = String.format("create sequence %s.hibernate_sequence", schemaName);
                for (String sql : schemaSQL) {
                    if (sequenceSQL.equals(sql)) {
                        hasSequence = true;
                        break;
                    }
                }
                if (!hasSequence) {
                    // Ideally we would need to check if the sequence exists for the current schema.
                    // Since there's no way to do that in a generic way that would work on all DBs and since calling
                    // dialect.getQuerySequencesString() will get the native SQL query to find out all sequences BUT
                    // only for the default schema, we need to find another way. The solution we're implementing is to
                    // try to create the sequence and if it fails then we consider it already exists.
                    try {
                        // Ignore errors in the log during the creation of the sequence since we know it can fail and we
                        // don't want to show false positives to the user.
                        this.loggerManager.pushLogListener(null);
                        session.createSQLQuery(sequenceSQL).executeUpdate();
                    } catch (HibernateException e) {
                        // Sequence failed to be created, we assume it already exists and that's why an exception was
                        // raised!
                    } finally {
                        this.loggerManager.popLogListener();
                    }
                }
            }
        }
    }

    /**
     * Runs the update script on the current database
     *
     * @param createSQL
     * @param inputxcontext
     * @deprecated since 11.5RC1
     */
    @Deprecated
    public void updateSchema(String[] createSQL, XWikiContext inputxcontext) throws HibernateException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        // Updating the schema for custom mappings
        Session session;
        boolean bTransaction = true;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        String sql = "";

        try {
            bTransaction = beginTransaction(context);
            session = getSession(context);
            setDatabase(session, context);
            if (monitor != null) {
                monitor.startTimer("sqlupgrade");
            }
            for (String element : createSQL) {
                sql = element;
                LOGGER.debug("Update Schema sql: [{}]", sql);
                session.createSQLQuery(sql).executeUpdate();
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            throw new HibernateException("Failed updating schema while executing query [" + sql + "]", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, true);
                }
            } catch (Exception e) {
            }

            restoreExecutionXContext();

            // End monitoring timer
            if (monitor != null) {
                monitor.endTimer("sqlupgrade");
            }
        }
    }

    /**
     * Custom Mapping This function update the schema based on the dynamic custom mapping provided by the class
     *
     * @param bclass
     * @param inputxcontext
     * @throws com.xpn.xwiki.XWikiException
     */
    public void updateSchema(BaseClass bclass, XWikiContext inputxcontext) throws XWikiException, HibernateException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            String custommapping = bclass.getCustomMapping();
            if (!bclass.hasExternalCustomMapping()) {
                return;
            }

            Metadata metadata = this.store.getMetadata(bclass.getName(), custommapping, context.getWikiId());
            this.store.updateDatabase(metadata);
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * Initializes Hibernate.
     *
     * @param context
     * @throws HibernateException
     */
    public void checkHibernate(XWikiContext context) throws HibernateException
    {
        if (getSessionFactory() == null) {
            // If not already initialized make sure to check/init but protected by a lock this time to make sure it's
            // not done by several threads
            checkHibernateSynchronized();
        }
    }

    private synchronized void checkHibernateSynchronized() throws HibernateException
    {
        if (getSessionFactory() == null) {
            this.store.initHibernate();
        }
    }

    /**
     * Checks if this xwiki setup is virtual meaning if multiple wikis can be accessed using the same database pool
     *
     * @deprecated Virtual mode is on by default, starting with XWiki 5.0M2.
     * @param context the XWiki context.
     * @return true if multi-wiki, false otherwise.
     */
    @Deprecated
    protected boolean isVirtual(XWikiContext context)
    {
        return true;
    }

    /**
     * Virtual Wikis Allows to switch database connection
     *
     * @param session
     * @param inputxcontext
     * @throws XWikiException
     */
    public void setDatabase(Session session, XWikiContext inputxcontext) throws XWikiException
    {
        this.store.setWiki(session);
    }

    /**
     * Escape schema name depending of the database engine.
     *
     * @param schema the schema name to escape
     * @param context the XWiki context to get database engine identifier
     * @return the escaped version
     */
    protected String escapeSchema(String schema, XWikiContext context)
    {
        return this.store.escapeDatabaseName(schema);
    }

    /**
     * Begins a transaction if the context does not contains any.
     *
     * @param context the current XWikiContext
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     */
    public boolean beginTransaction(XWikiContext context) throws XWikiException
    {
        return this.store.beginTransaction();
    }

    /**
     * Begins a transaction
     *
     * @param withTransaction this argument is unused
     * @param context the current XWikiContext
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     * @deprecated since 4.0M1, use {@link #beginTransaction(SessionFactory, XWikiContext)}
     */
    @Deprecated
    public boolean beginTransaction(boolean withTransaction, XWikiContext context) throws XWikiException
    {
        return this.store.beginTransaction();
    }

    /**
     * Begins a transaction with a specific SessionFactory.
     *
     * @param sfactory the session factory used to begin a new session if none are available
     * @param withTransaction this argument is unused
     * @param context the current XWikiContext
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     * @deprecated since 4.0M1, use {@link #beginTransaction(SessionFactory, XWikiContext)}
     */
    @Deprecated
    public boolean beginTransaction(SessionFactory sfactory, boolean withTransaction, XWikiContext context)
        throws XWikiException
    {
        return beginTransaction(sfactory, context);
    }

    /**
     * Begins a transaction with a specific SessionFactory.
     *
     * @param sfactory the session factory used to begin a new session if none are available
     * @param inputxcontext the current XWikiContext
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     */
    public boolean beginTransaction(SessionFactory sfactory, XWikiContext inputxcontext) throws XWikiException
    {
        return this.store.beginTransaction(sfactory);
    }

    /**
     * Ends a transaction and close the session.
     *
     * @param context the current XWikiContext
     * @param commit should we commit or not
     * @param withTransaction
     * @throws HibernateException
     * @deprecated since 4.0M1, use {@link #endTransaction(XWikiContext, boolean)}
     */
    @Deprecated
    public void endTransaction(XWikiContext context, boolean commit, boolean withTransaction) throws HibernateException
    {
        this.store.endTransaction(commit);
    }

    /**
     * Ends a transaction and close the session.
     *
     * @param inputxcontext the current XWikiContext
     * @param commit should we commit or not
     */
    public void endTransaction(XWikiContext inputxcontext, boolean commit)
    {
        this.store.endTransaction(commit);
    }

    /**
     * Cleanup all sessions Used at the shutdown time
     *
     * @param inputxcontext
     */
    public void cleanUp(XWikiContext inputxcontext)
    {
        try {
            Session session = this.store.getCurrentSession();
            if (session != null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Cleanup of session was needed: {}", session.toString());
                }

                this.store.endTransaction(false);
            }
        } catch (HibernateException e) {
        }
    }

    public SessionFactory getSessionFactory()
    {
        return this.store.getSessionFactory();
    }

    /**
     * @deprecated does not do anything since 11.5RC1
     */
    @Deprecated
    public void setSessionFactory(SessionFactory sessionFactory)
    {
        // Do nothing
    }

    public Configuration getConfiguration()
    {
        return this.store.getConfiguration();
    }

    /**
     * @since 11.5RC1
     */
    public Metadata getMetadata()
    {
        return this.store.getConfigurationMetadata();
    }

    /**
     * @deprecated since 9.9RC1
     */
    @Deprecated
    public Map<String, String> getConnections()
    {
        return Collections.emptyMap();
    }

    /**
     * @deprecated since 9.9RC1
     */
    @Deprecated
    public int getNbConnections()
    {
        return -1;
    }

    /**
     * @deprecated since 9.9RC1
     */
    @Deprecated
    public void setNbConnections(int nbConnections)
    {
        // Don't do anything anymore
    }

    /**
     * Return the name generated for a dynamic mapped object.
     *
     * @param className the classname of the object.
     * @return a name in the form xwikicustom_space_class
     * @since 4.0M1
     */
    public String dynamicMappingTableName(String className)
    {
        return this.store.toDynamicMappingTableName(className);
    }

    /**
     * Build a {@link Configuration} containing the provide mapping. Before 4.0M1, this function was called makeMapping.
     * In 4.0M1, it enter in conflict with {@link #makeMapping(String, String)}
     *
     * @param className the classname of the class to map.
     * @param customMapping the custom mapping
     * @return a new {@link Configuration} containing this mapping alone.
     * @since 4.0M1
     * @deprecated since 11.5RC1
     */
    @Deprecated
    protected Configuration getMapping(String className, String customMapping)
    {
        Configuration hibconfig = new Configuration();

        hibconfig.addInputStream(
            new ByteArrayInputStream(makeMapping(className, customMapping).getBytes(StandardCharsets.UTF_8)));

        return hibconfig;
    }

    /**
     * Build a new XML string to define the provided mapping. Since 4.0M1, the ids are longs, and a conditional mapping
     * is made for Oracle.
     *
     * @param className the name of the class to map.
     * @param customMapping the custom mapping
     * @return a XML definition for the given mapping, using XWO_ID column for the object id.
     */
    protected String makeMapping(String className, String customMapping)
    {
        return this.store.makeMapping(className, customMapping);
    }

    /**
     * Callback (closure) interface for operations in Hibernate.
     */
    @FunctionalInterface
    public interface HibernateCallback<T>
    {
        /**
         * method executed by {@link XWikiHibernateBaseStore} and pass open session to it.
         *
         * @param session - open hibernate session
         * @return any you need be returned by
         *         {@link XWikiHibernateBaseStore#execute(XWikiContext, boolean, boolean, HibernateCallback)}
         * @throws HibernateException if any store specific exception
         * @throws XWikiException if exception in xwiki.
         */
        T doInHibernate(Session session) throws HibernateException, XWikiException;
    }

    /**
     * Execute method for operations in hibernate. spring like.
     *
     * @param context - used everywhere.
     * @param bTransaction - should store use old transaction(false) or create new (true)
     * @param doCommit - should store commit changes(if any), or rollback it.
     * @param cb - callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @throws XWikiException if any error
     * @deprecated since 4.0M1, use {@link #execute(XWikiContext, boolean, HibernateCallback)} or
     *             {@link #failSafeExecute(XWikiContext, boolean, HibernateCallback)}
     */
    @Deprecated
    public <T> T execute(XWikiContext context, boolean bTransaction, boolean doCommit, HibernateCallback<T> cb)
        throws XWikiException
    {
        return execute(context, doCommit, cb);
    }

    /**
     * Execute method for operations in hibernate in an independent session (but not closing the current one if any).
     * Never throw any error, but there is no warranty that the operation has been completed successfully.
     *
     * @param inputxcontext - used everywhere.
     * @param doCommit - should store commit changes(if any), or rollback it.
     * @param cb - callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}, returns null if the callback throw an error.
     */
    public <T> T failSafeExecute(XWikiContext inputxcontext, boolean doCommit, HibernateCallback<T> cb)
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            final Session originalSession = this.store.getCurrentSession();
            final Transaction originalTransaction = getTransaction(context);
            this.store.setCurrentSession(null);
            this.store.setCurrentTransaction(null);

            this.loggerManager.pushLogListener(null);
            try {
                return execute(context, doCommit, cb);
            } catch (Exception ignored) {
                return null;
            } finally {
                this.loggerManager.popLogListener();
                this.store.setCurrentSession(originalSession);
                this.store.setCurrentTransaction(originalTransaction);
            }
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * Execute method for operations in Hibernate.
     *
     * @param inputxcontext - used everywhere.
     * @param doCommit - should store commit changes(if any), or rollback it.
     * @param cb - callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @throws XWikiException if any error
     */
    public <T> T execute(XWikiContext inputxcontext, boolean doCommit, HibernateCallback<T> cb) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            MonitorPlugin monitor = Util.getMonitorPlugin(context);
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer(XWikiHibernateBaseStore.HINT);
            }

            try {
                // Make sure Hibernate is initialized
                checkHibernate(context);

                // Create a new transaction if not already the case
                boolean bTransaction = beginTransaction(context);

                boolean commit = false;
                try {
                    // Execute the callback
                    T result = cb.doInHibernate(this.store.getCurrentSession());

                    // Commit the result only if successful, otherwise rollback in the finally
                    commit = doCommit;

                    return result;
                } catch (XWikiException e) {
                    throw e;
                } catch (Exception e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Exception while hibernate execute", e);
                } finally {
                    if (bTransaction) {
                        try {
                            this.store.endTransaction(commit);
                        } catch (Exception e) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("Exception while close transaction", e);
                            }
                        }
                    }
                }
            } finally {
                if (monitor != null) {
                    // Stop the monitor
                    monitor.endTimer(XWikiHibernateBaseStore.HINT);
                }
            }
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * Execute method for read-only operations in hibernate. spring like.
     *
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @param context the current XWikiContext
     * @param bTransaction this argument is unused
     * @param cb the callback to execute
     * @throws XWikiException if any error
     * @see #execute(XWikiContext, boolean, HibernateCallback)
     * @deprecated since 4.0M1, use {@link #executeRead(XWikiContext, HibernateCallback)} or
     *             {@link #failSafeExecuteRead(XWikiContext, HibernateCallback)}
     */
    @Deprecated
    public <T> T executeRead(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb) throws XWikiException
    {
        return execute(context, false, cb);
    }

    /**
     * Execute hibernate read-only operation in a independent session (but not closing the current one if any). Never
     * throw any error, but there is no warranty that the operation has been completed successfully.
     *
     * @param context the current XWikiContext
     * @param cb the callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}, returns null if the callback throw an error.
     * @see #failSafeExecute(XWikiContext, boolean, HibernateCallback)
     */
    public <T> T failSafeExecuteRead(XWikiContext context, HibernateCallback<T> cb)
    {
        return failSafeExecute(context, false, cb);
    }

    /**
     * Execute method for read-only operations in hibernate. spring like.
     *
     * @param context - used everywhere.
     * @param cb - callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @throws XWikiException if any error
     * @see #execute(XWikiContext, boolean, HibernateCallback)
     */
    public <T> T executeRead(XWikiContext context, HibernateCallback<T> cb) throws XWikiException
    {
        return execute(context, false, cb);
    }

    /**
     * Execute method for read-write operations in hibernate. spring like.
     *
     * @param context the current XWikiContext
     * @param bTransaction this argument is unused
     * @param cb the callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @throws XWikiException if any error
     * @see #execute(XWikiContext, boolean, HibernateCallback)
     * @deprecated since 4.0M1, use {@link #executeWrite(XWikiContext, HibernateCallback)} or
     *             {@link #failSafeExecuteWrite(XWikiContext, HibernateCallback)}
     */
    @Deprecated
    public <T> T executeWrite(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb) throws XWikiException
    {
        return execute(context, true, cb);
    }

    /**
     * Execute hibernate read-only operation in a independent session (but not closing the current one if any). Never
     * throw any error, but there is no warranty that the operation has been completed successfully.
     *
     * @param context the current XWikiContext
     * @param cb the callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @see #execute(XWikiContext, boolean, HibernateCallback)
     */
    public <T> T failSafeExecuteWrite(XWikiContext context, HibernateCallback<T> cb)
    {
        return failSafeExecute(context, true, cb);
    }

    /**
     * Execute method for read-write operations in Hibernate.
     *
     * @param context the current XWikiContext
     * @param cb the callback to execute
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @throws XWikiException if any error
     * @see #execute(XWikiContext, boolean, HibernateCallback)
     */
    public <T> T executeWrite(XWikiContext context, HibernateCallback<T> cb) throws XWikiException
    {
        return execute(context, true, cb);
    }

    /**
     * @return true if the user has configured Hibernate to use XWiki in schema mode (vs database mode)
     * @since 4.5M1
     */
    protected boolean isInSchemaMode()
    {
        return this.store.isConfiguredInSchemaMode();
    }

    /**
     * We had to add this method because the Component Manager doesn't inject a field in the base class if a derived
     * class defines a field with the same name.
     *
     * @return the execution
     * @since 5.1M1
     */
    protected Execution getExecution()
    {
        return this.execution;
    }

    /**
     * @return a singleton instance of the configured {@link Dialect}
     * @since 8.4RC1
     */
    public Dialect getDialect()
    {
        return this.store.getDialect();
    }
}
