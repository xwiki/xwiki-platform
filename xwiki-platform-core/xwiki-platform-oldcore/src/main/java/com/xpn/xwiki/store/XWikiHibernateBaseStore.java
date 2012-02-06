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

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.jdbc.BorrowedConnectionProxy;
import org.hibernate.jdbc.ConnectionManager;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.DataMigrationManager;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

public class XWikiHibernateBaseStore implements Initializable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiHibernateBaseStore.class);

    private Map<String, String> connections = new ConcurrentHashMap<String, String>();

    private int nbConnections = 0;

    @Inject
    private HibernateSessionFactory sessionFactory;

    @Inject
    @Named("hibernate")
    private DataMigrationManager dataMigrationManager;

    private String hibpath = "/WEB-INF/hibernate.cfg.xml";

    /**
     * Key in XWikiContext for access to current hibernate database name.
     */
    private static String currentDatabaseKey = "hibcurrentdatabase";

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
        LOGGER.debug("Hibernate configuration file: [" + path + "]");
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
        setPath(hibpath);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public XWikiHibernateBaseStore()
    {
    }

    @Override
    public void initialize() throws InitializationException
    {
        Execution execution = (Execution) Utils.getComponent(Execution.class);
        XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");
        setPath(context.getWiki().getConfig().getProperty("xwiki.store.hibernate.path", getPath()));
    }

    /**
     * Allows to get the current hibernate config file path
     */
    public String getPath()
    {
        return this.hibpath;
    }

    /**
     * Allows to set the current hibernate config file path
     * 
     * @param hibpath
     */
    public void setPath(String hibpath)
    {
        this.hibpath = hibpath;
    }

    /**
     * @return the database product name
     */
    public DatabaseProduct getDatabaseProductName(XWikiContext context)
    {
        String productName;
        try {
            productName = getSession(context).connection().getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            productName = "Unknown";
        }

        return DatabaseProduct.toProduct(productName);
    }

    /**
     * Allows to init the hibernate configuration
     * 
     * @throws org.hibernate.HibernateException
     */
    private synchronized void initHibernate(XWikiContext context) throws HibernateException
    {
        getConfiguration().configure(getPath());

        XWiki wiki = context.getWiki();
        if (wiki != null && wiki.Param("xwiki.db") != null && !wiki.isVirtualMode()) {
            // substitute default db name to configured.
            // note, that we can't call getSchemaFromWikiName() here,
            // because it ask getDatabaseProduct() which use connection
            // which must be opened. But here (before connection init)
            // we have no opened connections yet.
            String schemaName = getSchemaFromWikiName(context.getDatabase(), null, context);

            String dialect = getConfiguration().getProperty(Environment.DIALECT);
            if ("org.hibernate.dialect.MySQLDialect".equals(dialect)) {
                getConfiguration().setProperty(Environment.DEFAULT_CATALOG, schemaName);
            } else {
                getConfiguration().setProperty(Environment.DEFAULT_SCHEMA, schemaName);
            }
        }
        if (this.sessionFactory == null) {
            this.sessionFactory = Utils.getComponent(HibernateSessionFactory.class);
        }

        setSessionFactory(getConfiguration().buildSessionFactory());
    }

    /**
     * This get's the current session. This is set in beginTransaction
     * 
     * @param context
     */
    public Session getSession(XWikiContext context)
    {
        Session session = (Session) context.get("hibsession");
        // Make sure we are in this mode
        try {
            if (session != null) {
                session.setFlushMode(FlushMode.COMMIT);
            }
        } catch (org.hibernate.SessionException ex) {
            session = null;
        }

        return session;
    }

    /**
     * Allows to set the current session in the context This is set in beginTransaction
     * 
     * @param session
     * @param context
     */
    public void setSession(Session session, XWikiContext context)
    {
        if (session == null) {
            context.remove("hibsession");
        } else {
            context.put("hibsession", session);
        }
    }

    /**
     * Allows to get the current transaction from the context This is set in beginTransaction
     * 
     * @param context
     */
    public Transaction getTransaction(XWikiContext context)
    {
        Transaction transaction = (Transaction) context.get("hibtransaction");
        return transaction;
    }

    /**
     * Allows to set the current transaction This is set in beginTransaction
     * 
     * @param transaction
     * @param context
     */
    public void setTransaction(Transaction transaction, XWikiContext context)
    {
        if (transaction == null) {
            context.remove("hibtransaction");
        } else {
            context.put("hibtransaction", transaction);
        }
    }

    /**
     * Allows to shut down the hibernate configuration Closing all pools and connections
     * 
     * @param context
     * @throws HibernateException
     */
    public void shutdownHibernate(XWikiContext context) throws HibernateException
    {
        Session session = getSession(context);
        preCloseSession(session);
        closeSession(session);
        if (getSessionFactory() != null) {
            ((SessionFactoryImpl) getSessionFactory()).getConnectionProvider().close();
        }
    }

    /**
     * Allows to update the schema to match the hibernate mapping
     * 
     * @param context
     * @throws HibernateException
     */
    public void updateSchema(XWikiContext context) throws HibernateException
    {
        updateSchema(context, false);
    }

    /**
     * Allows to update the schema to match the hibernate mapping
     * 
     * @param context
     * @param force defines wether or not to force the update despite the xwiki.cfg settings
     * @throws HibernateException
     */
    public synchronized void updateSchema(XWikiContext context, boolean force) throws HibernateException
    {

        // We don't update the schema if the XWiki hibernate config parameter says not to update
        if ((!force) && (context.getWiki() != null)
            && ("0".equals(context.getWiki().Param("xwiki.store.hibernate.updateschema")))) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Schema update deactivated for wiki [" + context.getDatabase() + "]");
            }
            return;
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Updating schema update for wiki [" + context.getDatabase() + "]...");
        }

        try {
            String[] sql = getSchemaUpdateScript(getConfiguration(), context);
            updateSchema(sql, context);
        } finally {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Schema update for wiki [" + context.getDatabase() + "] done");
            }
        }
    }

    /**
     * Convert wiki name in database/schema name.
     * 
     * @param wikiName the wiki name to convert.
     * @param databaseProduct the database engine type.
     * @param context the XWiki context.
     * @return the database/schema name.
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    protected String getSchemaFromWikiName(String wikiName, DatabaseProduct databaseProduct, XWikiContext context)
    {
        if (wikiName == null) {
            return null;
        }

        XWiki wiki = context.getWiki();

        String schema;
        if (context.isMainWiki(wikiName)) {
            schema = wiki.Param("xwiki.db");
            if (schema == null) {
                if (databaseProduct == DatabaseProduct.DERBY) {
                    schema = "APP";
                } else if (databaseProduct == DatabaseProduct.HSQLDB) {
                    schema = "PUBLIC";
                } else {
                    schema = wikiName.replace('-', '_');
                }
            }
        } else {
            // virtual
            schema = wikiName.replace('-', '_');
        }

        // Apply prefix
        String prefix = wiki.Param("xwiki.db.prefix", "");
        schema = prefix + schema;

        return schema;
    }

    /**
     * Convert wiki name in database/schema name.
     * <p>
     * Need hibernate to be initialized.
     * 
     * @param wikiName the wiki name to convert.
     * @param context the XWiki context.
     * @return the database/schema name.
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    protected String getSchemaFromWikiName(String wikiName, XWikiContext context)
    {
        if (wikiName == null) {
            return null;
        }

        DatabaseProduct databaseProduct = getDatabaseProductName(context);

        String schema = getSchemaFromWikiName(wikiName, databaseProduct, context);

        return schema;
    }

    /**
     * Convert context's database in real database/schema name.
     * <p>
     * Need hibernate to be initialized.
     * 
     * @param context the XWiki context.
     * @return the database/schema name.
     * @since XWiki Core 1.1.2, XWiki Core 1.2M2
     */
    protected String getSchemaFromWikiName(XWikiContext context)
    {
        return getSchemaFromWikiName(context.getDatabase(), context);
    }

    /**
     * This function gets the schema update scripts generated by comparing the current database with the current
     * hibernate mapping config.
     * 
     * @param config
     * @param context
     * @throws HibernateException
     */
    public String[] getSchemaUpdateScript(Configuration config, XWikiContext context) throws HibernateException
    {
        String[] schemaSQL = null;

        Session session;
        Connection connection;
        DatabaseMetadata meta;
        Statement stmt = null;
        Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());
        boolean bTransaction = true;
        String dschema = null;

        try {
            bTransaction = beginTransaction(false, context);
            session = getSession(context);
            connection = session.connection();
            setDatabase(session, context);

            String contextSchema = getSchemaFromWikiName(context);

            DatabaseProduct databaseProduct = getDatabaseProductName(context);
            if (databaseProduct == DatabaseProduct.ORACLE || databaseProduct == DatabaseProduct.HSQLDB
                || databaseProduct == DatabaseProduct.DERBY || databaseProduct == DatabaseProduct.DB2) {
                dschema = config.getProperty(Environment.DEFAULT_SCHEMA);
                config.setProperty(Environment.DEFAULT_SCHEMA, contextSchema);
                Iterator iter = config.getTableMappings();
                while (iter.hasNext()) {
                    Table table = (Table) iter.next();
                    table.setSchema(contextSchema);
                }
            }

            meta = new DatabaseMetadata(connection, dialect);
            stmt = connection.createStatement();
            schemaSQL = config.generateSchemaUpdateScript(dialect, meta);
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed creating schema update script", e);
            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
                if (dschema != null) {
                    config.setProperty(Environment.DEFAULT_SCHEMA, dschema);
                }
            } catch (Exception e) {
            }
        }

        return schemaSQL;
    }

    /**
     * Runs the update script on the current database
     * 
     * @param createSQL
     * @param context
     */
    public void updateSchema(String[] createSQL, XWikiContext context)
    {
        // Updating the schema for custom mappings
        Session session;
        Connection connection;
        Statement stmt = null;
        boolean bTransaction = true;
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        String sql = "";

        try {
            bTransaction = beginTransaction(context);
            session = getSession(context);
            connection = session.connection();
            setDatabase(session, context);
            stmt = connection.createStatement();

            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("sqlupgrade");
            }
            for (int j = 0; j < createSQL.length; j++) {
                sql = createSQL[j];
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Update Schema sql: [" + sql + "]");
                }
                stmt.executeUpdate(sql);
            }
            connection.commit();
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed updating schema while executing query [" + sql + "]", e);
            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
            }
            try {
                if (bTransaction) {
                    endTransaction(context, true);
                }
            } catch (Exception e) {
            }

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
     * @param context
     * @throws com.xpn.xwiki.XWikiException
     */
    public void updateSchema(BaseClass bclass, XWikiContext context) throws XWikiException
    {
        String custommapping = bclass.getCustomMapping();
        if (!bclass.hasExternalCustomMapping()) {
            return;
        }

        Configuration config = makeMapping(bclass.getName(), custommapping);
        /*
         * if (isValidCustomMapping(bclass.getName(), config, bclass)==false) { throw new XWikiException(
         * XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING, "Cannot update
         * schema for class " + bclass.getName() + " because of an invalid mapping"); }
         */

        String[] sql = getSchemaUpdateScript(config, context);
        updateSchema(sql, context);
    }

    /**
     * Initializes hibernate
     * 
     * @param context
     * @throws HibernateException
     */
    public void checkHibernate(XWikiContext context) throws HibernateException
    {
        if (getSessionFactory() == null) {
            initHibernate(context);
        }
    }

    /**
     * Checks if this xwiki setup is virtual meaning if multiple wikis can be accessed using the same database pool
     * 
     * @param context the XWiki context.
     * @return true if multi-wiki, false otherwise.
     */
    protected boolean isVirtual(XWikiContext context)
    {
        if ((context == null) || (context.getWiki() == null)) {
            return true;
        }

        return context.getWiki().isVirtualMode();
    }

    /**
     * Virtual Wikis Allows to switch database connection
     * 
     * @param session
     * @param context
     * @throws XWikiException
     */
    public void setDatabase(Session session, XWikiContext context) throws XWikiException
    {
        try {
            if (isVirtual(context)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Switch database to: [" + context.getDatabase() + "]");
                }

                if (context.getDatabase() != null) {
                    String schemaName = getSchemaFromWikiName(context);
                    String escapedSchemaName = escapeSchema(schemaName, context);

                    DatabaseProduct databaseProduct = getDatabaseProductName(context);
                    if (DatabaseProduct.ORACLE == databaseProduct) {
                        Statement stmt = null;
                        try {
                            stmt = session.connection().createStatement();
                            stmt.execute("alter session set current_schema = " + escapedSchemaName);
                        } finally {
                            try {
                                if (stmt != null) {
                                    stmt.close();
                                }
                            } catch (Exception e) {
                            }
                        }
                    } else if (DatabaseProduct.DERBY == databaseProduct || DatabaseProduct.HSQLDB == databaseProduct
                        || DatabaseProduct.DB2 == databaseProduct) {
                        Statement stmt = null;
                        try {
                            stmt = session.connection().createStatement();
                            stmt.execute("SET SCHEMA " + escapedSchemaName);
                        } finally {
                            try {
                                if (stmt != null) {
                                    stmt.close();
                                }
                            } catch (Exception e) {
                            }
                        }
                    } else {
                        String catalog = session.connection().getCatalog();
                        catalog = (catalog == null) ? null : catalog.replace('_', '-');
                        if (!schemaName.equals(catalog)) {
                            session.connection().setCatalog(schemaName);
                        }
                    }
                    setCurrentDatabase(context, context.getDatabase());
                }
            }

            dataMigrationManager.checkDatabase();
        } catch (Exception e) {
            endTransaction(context, false); // close session with rollback to avoid further usage
            Object[] args = {context.getDatabase()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE,
                "Exception while switching to database {0}", e, args);
        }
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
        String escapedSchema;
        Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());

        // - Oracle converts user names in uppercase when no quotes is used.
        // For example: "create user xwiki identified by xwiki;" creates a user named XWIKI (uppercase)
        // - In Hibernate.cfg.xml we just specify: <property name="connection.username">xwiki</property> and Hibernate
        // seems to be passing this username as is to Oracle which converts it to uppercase.
        //
        // Thus for Oracle we don't escape the schema.
        // TODO: Remove this when https://hibernate.onjira.com/browse/HHH-6888 is fixed
        DatabaseProduct databaseProduct = getDatabaseProductName(context);
        if (DatabaseProduct.ORACLE == databaseProduct) {
            escapedSchema = schema;
        } else {
            String closeQuote = String.valueOf(dialect.closeQuote());
            escapedSchema = dialect.openQuote() + schema.replace(closeQuote, closeQuote + closeQuote) + closeQuote;
        }

        return escapedSchema;
    }

    /**
     * Begins a transaction
     * 
     * @param context
     * @throws XWikiException
     */
    public boolean beginTransaction(XWikiContext context) throws XWikiException
    {
        return beginTransaction(null, true, context);
    }

    /**
     * Begins a transaction
     * 
     * @param withTransaction
     * @param context
     * @throws XWikiException
     */
    public boolean beginTransaction(boolean withTransaction, XWikiContext context) throws XWikiException
    {
        return beginTransaction(null, withTransaction, context);
    }

    /**
     * Begins a transaction with a specific SessionFactory
     * 
     * @param sfactory
     * @param context
     * @throws XWikiException
     */
    public boolean beginTransaction(SessionFactory sfactory, XWikiContext context) throws XWikiException
    {
        return beginTransaction(sfactory, true, context);
    }

    /**
     * Begins a transaction with a specific SessionFactory
     * 
     * @param sfactory
     * @param withTransaction
     * @param context
     * @throws HibernateException
     * @throws XWikiException
     */
    public boolean beginTransaction(SessionFactory sfactory, boolean withTransaction, XWikiContext context)
        throws HibernateException, XWikiException
    {

        Transaction transaction = getTransaction(context);
        Session session = getSession(context);

        if (((session == null) && (transaction != null)) || ((transaction == null) && (session != null))) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Incompatible session (" + session + ") and transaction (" + transaction + ") status");
            }
            // TODO: Fix this problem, don't ignore it!
            return false;
        }

        if (session != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Taking session from context " + session);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Taking transaction from context " + transaction);
            }
            return false;
        }

        // session is obviously null here
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Trying to get session from pool");
        }
        if (sfactory == null) {
            session = getSessionFactory().openSession();
        } else {
            session = sfactory.openSession();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Taken session from pool " + session);
        }

        if (LOGGER.isDebugEnabled()) {
            addConnection(getRealConnection(session), context);
        }

        setSession(session, context);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Trying to open transaction");
        }
        transaction = session.beginTransaction();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Opened transaction " + transaction);
        }
        setTransaction(transaction, context);

        // during #setDatabase, the data version may now be checked using this new transaction.
        // In case of any failure during database switch, the transaction and the session will be closed.
        setDatabase(session, context);

        return true;
    }

    /**
     * Adding a connection to the Monitor module
     * 
     * @param connection
     * @param context
     * @todo This function is temporarily deactivated because of an error that causes memory leaks.
     */
    private synchronized void addConnection(Connection connection, XWikiContext context)
    {
        // connection.equals(connection) = false for some strange reasons, so we're remembering the
        // toString representation of each active connection. We also remember the stack trace (if
        // debug logging is enabled) to help spotting what code causes connections to leak.
        if (connection != null) {
            try {
                // Keep some statistics about session and connections
                if (this.connections.containsKey(connection.toString())) {
                    LOGGER.info("Connection [" + connection.toString() + "] already in connection map for store "
                        + this.toString());
                } else {
                    String value = "";
                    if (LOGGER.isDebugEnabled()) {
                        // No need to fill in the logging stack trace if debug is not enabled.
                        XWikiException stackException = new XWikiException();
                        stackException.fillInStackTrace();
                        value = stackException.getStackTraceAsString();
                    }
                    this.connections.put(connection.toString(), value);
                    this.nbConnections++;
                }
            } catch (Throwable e) {
                // This should not happen
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * Remove a connection to the Monitor module
     * 
     * @param connection
     * @todo This function is temporarily deactivated because of an error that causes memory leaks.
     */
    private synchronized void removeConnection(Connection connection)
    {
        if (connection != null) {
            try {
                // Keep some statistics about session and connections
                if (this.connections.containsKey(connection.toString())) {
                    this.connections.remove(connection.toString());
                    this.nbConnections--;
                } else {
                    LOGGER.info("Connection [" + connection.toString() + "] not in connection map");
                }
            } catch (Throwable e) {
                // This should not happen
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * Ends a transaction
     * 
     * @param context
     * @param commit should we commit or not
     */
    public void endTransaction(XWikiContext context, boolean commit)
    {
        endTransaction(context, commit, false);
    }

    /**
     * Ends a transaction
     * 
     * @param context
     * @param commit should we commit or not
     * @param withTransaction
     * @throws HibernateException
     */
    public void endTransaction(XWikiContext context, boolean commit, boolean withTransaction) throws HibernateException
    {
        Session session = null;
        try {
            session = getSession(context);
            Transaction transaction = getTransaction(context);
            setSession(null, context);
            setTransaction(null, context);

            if (transaction != null) {
                // We need to clean up our connection map first because the connection will
                // be aggressively closed by hibernate 3.1 and more
                preCloseSession(session);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Releasing hibernate transaction " + transaction);
                }
                if (commit) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                }
            }
        } catch (HibernateException e) {
            // Ensure the original cause will get printed.
            throw new HibernateException("Failed to commit or rollback transaction. Root cause ["
                + getExceptionMessage(e) + "]", e);
        } finally {
            closeSession(session);
        }
    }

    /**
     * Hibernate and JDBC will wrap the exception thrown by the trigger in another exception (the
     * java.sql.BatchUpdateException) and this exception is sometimes wrapped again. Also the
     * java.sql.BatchUpdateException stores the underlying trigger exception in the nextException and not in the cause
     * property. The following method helps you to get to the underlying trigger message.
     */
    private String getExceptionMessage(Throwable t)
    {
        StringBuilder sb = new StringBuilder();
        Throwable next = null;
        for (Throwable current = t; current != null; current = next) {
            next = current.getCause();
            if (next == current) {
                next = null;
            }
            if (current instanceof SQLException) {
                SQLException sx = (SQLException) current;
                while (sx.getNextException() != null) {
                    sx = sx.getNextException();
                    sb.append("\nSQL next exception = [" + sx + "]");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Closes the hibernate session
     * 
     * @param session
     * @throws HibernateException
     */
    private void closeSession(Session session) throws HibernateException
    {
        if (session != null) {
            session.close();
        }
    }

    /**
     * Closes the hibernate session
     * 
     * @param session
     * @throws HibernateException
     */
    private void preCloseSession(Session session) throws HibernateException
    {
        if (session != null) {
            if (LOGGER.isDebugEnabled()) {
                // Remove the connection from the list of active connections, used for debugging.
                LOGGER.debug("Releasing hibernate session " + session);
                Connection connection = getRealConnection(session);
                if ((connection != null)) {
                    removeConnection(connection);
                }
            }
        }
    }

    /**
     * Hack to get the real JDBC connection because hibernate 3.1 wraps the connection in a proxy and this creates a
     * memory leak
     */
    private Connection getRealConnection(Session session)
    {
        Connection conn = session.connection();
        if (conn instanceof Proxy) {
            Object bcp = Proxy.getInvocationHandler(conn);
            if (bcp instanceof BorrowedConnectionProxy) {
                ConnectionManager cm = (ConnectionManager) XWiki.getPrivateField(bcp, "connectionManager");
                if (cm != null) {
                    return cm.getConnection();
                }
            }
        }
        return conn;
    }

    /**
     * Cleanup all sessions Used at the shutdown time
     * 
     * @param context
     */
    public void cleanUp(XWikiContext context)
    {
        try {
            Session session = getSession(context);
            if (session != null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Cleanup of session was needed: " + session);
                }
                endTransaction(context, false);
            }
        } catch (HibernateException e) {
        }
    }

    public SessionFactory getSessionFactory()
    {
        return this.sessionFactory.getSessionFactory();
    }

    public void setSessionFactory(SessionFactory sessionFactory)
    {
        this.sessionFactory.setSessionFactory(sessionFactory);
    }

    public Configuration getConfiguration()
    {
        return this.sessionFactory.getConfiguration();
    }

    public Map<String, String> getConnections()
    {
        return this.connections;
    }

    public int getNbConnections()
    {
        return this.nbConnections;
    }

    public void setNbConnections(int nbConnections)
    {
        this.nbConnections = nbConnections;
    }

    protected Configuration makeMapping(String className, String custommapping1)
    {
        Configuration hibconfig = new Configuration();
        {
            hibconfig.addXML(makeMapping(className, "xwikicustom_" + className.replaceAll("\\.", "_"), custommapping1));
        }
        hibconfig.buildMappings();
        return hibconfig;
    }

    protected String makeMapping(String entityName, String tableName, String custommapping1)
    {
        String custommapping =
            "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE hibernate-mapping PUBLIC\n"
                + "\t\"-//Hibernate/Hibernate Mapping DTD//EN\"\n"
                + "\t\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n" + "<hibernate-mapping>"
                + "<class entity-name=\"" + entityName + "\" table=\"" + tableName + "\">\n"
                + " <id name=\"id\" type=\"integer\" unsaved-value=\"any\">\n"
                + "   <column name=\"XWO_ID\" not-null=\"true\" />\n" + "   <generator class=\"assigned\" />\n"
                + " </id>\n" + custommapping1 + "</class>\n" + "</hibernate-mapping>";
        return custommapping;
    }

    /**
     * Callback (closure) interface for operations in hibernate. spring like.
     */
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
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @param context - used everywhere.
     * @param bTransaction - should store use old transaction(false) or create new (true)
     * @param doCommit - should store commit changes(if any), or rollback it.
     * @param cb - callback to execute
     * @throws XWikiException if any error
     */
    public <T> T execute(XWikiContext context, boolean bTransaction, boolean doCommit, HibernateCallback<T> cb)
        throws XWikiException
    {
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null) {
                monitor.startTimer("hibernate");
            }

            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }

            if (context.getDatabase() != null && !context.getDatabase().equals(getCurrentDatabase(context))) {
                setDatabase(getSession(context), context);
            }

            return cb.doInHibernate(getSession(context));
        } catch (Exception e) {
            doCommit = false;
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Exception while hibernate execute", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, doCommit);
                }
                if (monitor != null) {
                    monitor.endTimer("hibernate");
                }
            } catch (Exception e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Exeption while close transaction", e);
                }
            }
        }
    }

    /**
     * Execute method for read-only operations in hibernate. spring like.
     * 
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @param context - used everywhere.
     * @param bTransaction - should store to use old transaction(false) or create new (true)
     * @param cb - callback to execute
     * @throws XWikiException if any error
     * @see #execute(XWikiContext, boolean, boolean, com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback)
     */
    public <T> T executeRead(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb) throws XWikiException
    {
        return execute(context, bTransaction, false, cb);
    }

    /**
     * Execute method for read-write operations in hibernate. spring like.
     * 
     * @return {@link HibernateCallback#doInHibernate(Session)}
     * @param context - used everywhere.
     * @param bTransaction - should store to use old transaction(false) or create new (true)
     * @param cb - callback to execute
     * @throws XWikiException if any error
     * @see #execute(XWikiContext, boolean, boolean, com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback)
     */
    public <T> T executeWrite(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb)
        throws XWikiException
    {
        return execute(context, bTransaction, true, cb);
    }

    /**
     * @param context XWikiContext
     * @return current hibernate database name
     */
    private String getCurrentDatabase(XWikiContext context)
    {
        return (String) context.get(currentDatabaseKey);
    }

    /**
     * @param context XWikiContext
     * @param database current hibernate database name to set
     */
    private void setCurrentDatabase(XWikiContext context, String database)
    {
        context.put(currentDatabaseKey, database);
    }
}
