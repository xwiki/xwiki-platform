package com.xpn.xwiki.store;

import java.io.File;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.jdbc.BorrowedConnectionProxy;
import org.hibernate.jdbc.ConnectionManager;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiRequest;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class XWikiHibernateBaseStore
{
    private static final Log log = LogFactory.getLog(XWikiHibernateBaseStore.class);

    private Map connections = new ConcurrentHashMap();

    private int nbConnections = 0;

    private SessionFactory sessionFactory;

    private Configuration configuration;

    private String hibpath;

    private URL hiburl;

    /**
     * THis allows to initialize our storage engine. The hibernate config file path is taken from
     * xwiki.cfg or directly in the WEB-INF directory.
     * 
     * @param xwiki
     * @param context
     */
    public XWikiHibernateBaseStore(XWiki xwiki, XWikiContext context)
    {
        String path = xwiki.Param("xwiki.store.hibernate.path", "/WEB-INF/hibernate.cfg.xml");
        log.debug("Hibernate configuration file: [" + path + "]");
        try {
            if ((path != null) && ((new File(path).exists() || context.getEngineContext() == null))) {
                setPath(path);
                return;
            }
        } catch (Exception ex) {
            // Probably running under -security, which prevents calling File.exists()
            log.info("Failed setting the Hibernate configuration path using a path string");
        }
        try {
            setHibUrl(context.getEngineContext().getResource(path));
        } catch (Exception ex) {
            log.info("Failed setting the Hibernate configuration path using getResource");
            try {
                setHibUrl(XWiki.class.getClassLoader().getResource(path));
            } catch (Exception ex2) {
                log.error("Failed setting the Hibernate configuration file with any method, storage cannot be configured", ex2);
            }
        }
    }

    /**
     * Initialize the storage engine with a specific path This is used for tests.
     * 
     * @param hibpath
     */
    public XWikiHibernateBaseStore(String hibpath)
    {
        setPath(hibpath);
    }

    /**
     * Allows to get the current hibernate config file path
     * 
     * @return
     */
    public String getPath()
    {
        return hibpath;
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
     * Get's the hibernate config path as an URL
     * 
     * @return
     */
    public URL getHibUrl()
    {
        return hiburl;
    }

    /**
     * Set the hibernate config path as an URL
     * 
     * @param hiburl
     */
    public void setHibUrl(URL hiburl)
    {
        this.hiburl = hiburl;
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
    private void initHibernate() throws HibernateException
    {
        // Load Configuration and build SessionFactory
        String path = getPath();
        if (path != null)
            setConfiguration((new Configuration()).configure(new File(path)));
        else {
            URL hiburl = getHibUrl();
            if (hiburl != null)
                setConfiguration(new Configuration().configure(hiburl));
            else
                setConfiguration(new Configuration().configure());
        }

        setSessionFactory(getConfiguration().buildSessionFactory());
    }

    /**
     * This get's the current session. This is set in beginTransaction
     * 
     * @param context
     * @return
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
        if (session == null)
            context.remove("hibsession");
        else
            context.put("hibsession", session);
    }

    /**
     * Allows to get the current transaction from the context This is set in beginTransaction
     * 
     * @param context
     * @return
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
        if (transaction == null)
            context.remove("hibtransaction");
        else
            context.put("hibtransaction", transaction);
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
    public synchronized void updateSchema(XWikiContext context, boolean force)
        throws HibernateException
    {

        // We don't update the schema if the XWiki hibernate config parameter says not to update
        if ((!force) && (context.getWiki() != null)
            && ("0".equals(context.getWiki().Param("xwiki.store.hibernate.updateschema")))) {
            if (log.isDebugEnabled())
                log.debug("Schema update deactivated for wiki " + context.getDatabase());
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Updating schema update for wiki " + context.getDatabase() + " ...");
        }

        try {
            String fullName =
                ((context != null) && (context.getWiki() != null) && (context.getWiki().isMySQL()))
                    ? "concat(xwd_web,'.',xwd_name)" : "xwd_fullname";
            String[] schemaSQL = getSchemaUpdateScript(getConfiguration(), context);
            String[] addSQL =
                {
                // Make sure we have no null valued in integer fields
                "update xwikidoc set xwd_translation=0 where xwd_translation is null",
                "update xwikidoc set xwd_language='' where xwd_language is null",
                "update xwikidoc set xwd_default_language='' where xwd_default_language is null",
                "update xwikidoc set xwd_fullname=" + fullName + " where xwd_fullname is null",
                "update xwikidoc set xwd_elements=3 where xwd_elements is null",
                "delete from xwikiproperties where xwp_name like 'editbox_%' and xwp_classtype='com.xpn.xwiki.objects.LongProperty'",
                "delete from xwikilongs where xwl_name like 'editbox_%'"};

            int inb = (schemaSQL == null) ? 0 : schemaSQL.length;
            int nb = inb + addSQL.length;
            String[] sql = new String[nb];
            if (schemaSQL != null) {
                for (int i = 0; i < inb; i++)
                    sql[i] = schemaSQL[i];
            }
            for (int i = 0; i < addSQL.length; i++)
                sql[i + inb] = addSQL[i];

            updateSchema(sql, context);
        } finally {
            if (log.isInfoEnabled()) {
                log.info("Schema update for wiki " + context.getDatabase() + " done");
            }
        }
    }

    /**
     * Convert wiki name in database/schema name.
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

        if (databaseProduct == DatabaseProduct.DERBY) {
            return context.isMainWiki(wikiName) ? "APP" : wikiName.replace(
                '-', '_');
        } else if (databaseProduct == DatabaseProduct.HSQLDB) {
            return context.isMainWiki(wikiName) ? "PUBLIC" : wikiName
                .replace('-', '_');
        } else
            return wikiName.replace('-', '_');
    }

    /**
     * Convert context's database in real database/schema name.
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
     * This function gets the schema update scripts generated by comparing the current database woth
     * the current hibernate mapping config.
     * 
     * @param config
     * @param context
     * @return
     * @throws HibernateException
     */
    public String[] getSchemaUpdateScript(Configuration config, XWikiContext context)
        throws HibernateException
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
            if (databaseProduct == DatabaseProduct.ORACLE
                || databaseProduct == DatabaseProduct.HSQLDB
                || databaseProduct == DatabaseProduct.DERBY) {
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
            if (log.isErrorEnabled())
                log.error("Failed creating schema update script", e);
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
            if (monitor != null)
                monitor.startTimer("sqlupgrade");
            for (int j = 0; j < createSQL.length; j++) {
                sql = createSQL[j];
                if (log.isDebugEnabled())
                    log.debug("Update Schema sql: " + sql);
                stmt.executeUpdate(sql);
            }
            connection.commit();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed updating schema while executing query \"" + sql + "\"", e);
            }
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
            }
            ;
            try {
                if (bTransaction)
                    endTransaction(context, true);
            } catch (Exception e) {
            }

            // End monitoring timer
            if (monitor != null)
                monitor.endTimer("sqlupgrade");
        }
    }

    /**
     * Custom Mapping This function update the schema based on the dynamic custom mapping provided
     * by the class
     * 
     * @param bclass
     * @param context
     * @throws com.xpn.xwiki.XWikiException
     */
    public void updateSchema(BaseClass bclass, XWikiContext context) throws XWikiException
    {
        String custommapping = bclass.getCustomMapping();
        if (!bclass.hasExternalCustomMapping())
            return;

        Configuration config = makeMapping(bclass.getName(), custommapping);
        /*
         * if (isValidCustomMapping(bclass.getName(), config, bclass)==false) { throw new
         * XWikiException( XWikiException.MODULE_XWIKI_STORE,
         * XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING, "Cannot update schema for
         * class " + bclass.getName() + " because of an invalid mapping"); }
         */

        String[] sql = getSchemaUpdateScript(config, context);
        updateSchema(sql, context);
    }

    /**
     * Initializes hibernate and calls updateSchema if necessary
     * 
     * @param context
     * @throws HibernateException
     */
    public void checkHibernate(XWikiContext context) throws HibernateException
    {

        if (getSessionFactory() == null) {
            initHibernate();

            /* Check Schema */
            if (getSessionFactory() != null) {
                updateSchema(context);
            }
        }
    }

    /**
     * Checks if this xwiki setup is virtual meaning if multiple wikis can be accessed using the
     * same database pool
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
        if (isVirtual(context)) {
            try {
                if (log.isDebugEnabled())
                    log.debug("Switch database to: " + context.getDatabase());

                if (context.getDatabase() != null) {
                    String schemaName = getSchemaFromWikiName(context);

                    DatabaseProduct databaseProduct = getDatabaseProductName(context);
                    if (DatabaseProduct.ORACLE == databaseProduct) {
                        Statement stmt = null;
                        try {
                            stmt = session.connection().createStatement();
                            stmt.execute("alter session set current_schema = " + schemaName);
                        } finally {
                            try {
                                if (stmt != null)
                                    stmt.close();
                            } catch (Exception e) {
                            }
                        }
                    } else if (DatabaseProduct.DERBY == databaseProduct) {
                        Statement stmt = null;
                        try {
                            stmt = session.connection().createStatement();
                            stmt.execute("SET SCHEMA " + schemaName);
                        } finally {
                            try {
                                if (stmt != null)
                                    stmt.close();
                            } catch (Exception e) {
                            }
                        }
                    } else if (DatabaseProduct.HSQLDB == databaseProduct) {
                        Statement stmt = null;
                        try {
                            stmt = session.connection().createStatement();
                            stmt.execute("SET SCHEMA " + schemaName);
                        } finally {
                            try {
                                if (stmt != null)
                                    stmt.close();
                            } catch (Exception e) {
                            }
                        }
                    } else {
                        String catalog = session.connection().getCatalog();
                        catalog = (catalog == null) ? null : catalog.replace('_', '-');
                        if (!schemaName.equals(catalog))
                            session.connection().setCatalog(schemaName);
                    }
                }
            } catch (Exception e) {
                Object[] args = {context.getDatabase()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE,
                    "Exception while switching to database {0}",
                    e,
                    args);
            }
        }
    }

    /**
     * Begins a transaction
     * 
     * @param context
     * @return
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
     * @return
     * @throws XWikiException
     */
    public boolean beginTransaction(boolean withTransaction, XWikiContext context)
        throws XWikiException
    {
        return beginTransaction(null, withTransaction, context);
    }

    /**
     * Begins a transaction with a specific SessionFactory
     * 
     * @param sfactory
     * @param context
     * @return
     * @throws XWikiException
     */
    public boolean beginTransaction(SessionFactory sfactory, XWikiContext context)
        throws XWikiException
    {
        return beginTransaction(sfactory, true, context);
    }

    /**
     * Begins a transaction with a specific SessionFactory
     * 
     * @param sfactory
     * @param withTransaction
     * @param context
     * @return
     * @throws HibernateException
     * @throws XWikiException
     */
    public boolean beginTransaction(SessionFactory sfactory, boolean withTransaction,
        XWikiContext context) throws HibernateException, XWikiException
    {

        Transaction transaction = getTransaction(context);
        Session session = getSession(context);

        if (((session == null) && (transaction != null))
            || ((transaction == null) && (session != null))) {
            if (log.isWarnEnabled())
                log.warn("Incompatible session (" + session + ") and transaction (" + transaction
                    + ") status");
            // TODO: Fix this problem, don't ignore it!
            return false;
        }

        if (session != null) {
            if (log.isDebugEnabled())
                log.debug("Taking session from context " + session);
            if (log.isDebugEnabled())
                log.debug("Taking transaction from context " + transaction);
            return false;
        }

        if (session == null) {
            if (log.isDebugEnabled())
                log.debug("Trying to get session from pool");
            if (sfactory == null)
                session = (SessionImpl) getSessionFactory().openSession();
            else
                session = sfactory.openSession();

            if (log.isDebugEnabled())
                log.debug("Taken session from pool " + session);

            // Keep some statistics about session and connections
            nbConnections++;
            addConnection(getRealConnection(session), context);

            setSession(session, context);
            setDatabase(session, context);

            if (log.isDebugEnabled()) {
                log.debug("Trying to open transaction");
            }
            transaction = session.beginTransaction();
            if (log.isDebugEnabled()) {
                log.debug("Opened transaction " + transaction);
            }
            setTransaction(transaction, context);
        }
        return true;
    }

    /**
     * Adding a connection to the Monitor module
     * 
     * @param connection
     * @param context
     * @todo This function is temporarily deactivated because of an error that causes memory leaks.
     */
    private void addConnection(Connection connection, XWikiContext context)
    {
        /*
         * if (connection!=null) connections.put(connection, new ConnectionMonitor(connection,
         * context));
         */}

    /**
     * Remove a connection to the Monitor module
     * 
     * @param connection
     * @todo This function is temporarily deactivated because of an error that causes memory leaks.
     */
    private void removeConnection(Connection connection)
    {
        // connection.equals(connection) = false for some strange reasons.
        /*
         * try { if (connection!=null) connections.remove(connection); } catch (Exception e) { }
         */}

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
    public void endTransaction(XWikiContext context, boolean commit, boolean withTransaction)
        throws HibernateException
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

                if (log.isDebugEnabled())
                    log.debug("Releasing hibernate transaction " + transaction);
                if (commit) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                }
            }
        } finally {
            closeSession(session);
        }
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
            if (log.isDebugEnabled())
                log.debug("Releasing hibernate session " + session);
            Connection connection = getRealConnection(session);
            if ((connection != null)) {
                nbConnections--;
                try {
                    removeConnection(connection);
                } catch (Throwable e) {
                    // This should not happen
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Hack to get the real JDBC connection because hibernate 3.1 wraps the connection in a proxy
     * and this creates a memory leak
     */
    private Connection getRealConnection(Session session)
    {
        Connection conn = session.connection();
        if (conn instanceof Proxy) {
            Object bcp = Proxy.getInvocationHandler(conn);
            if (bcp instanceof BorrowedConnectionProxy) {
                ConnectionManager cm =
                    (ConnectionManager) XWiki.getPrivateField(bcp, "connectionManager");
                if (cm != null)
                    return cm.getConnection();
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
                if (log.isWarnEnabled())
                    log.warn("Cleanup of session was needed: " + session);
                endTransaction(context, false);
            }
        } catch (HibernateException e) {
        }
    }

    public SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public Collection getConnections()
    {
        return connections.values();
    }

    public int getNbConnections()
    {
        return nbConnections;
    }

    public void setNbConnections(int nbConnections)
    {
        this.nbConnections = nbConnections;
    }

    public class ConnectionMonitor
    {
        private Exception exception;

        private Connection connection;

        private Date date;

        private URL url = null;

        public ConnectionMonitor(Connection connection, XWikiContext context)
        {
            this.setConnection(connection);

            try {
                setDate(new Date());
                setException(new XWikiException());
                XWikiRequest request = context.getRequest();
                if (request != null)
                    setURL(XWiki.getRequestURL(context.getRequest()));
            } catch (Throwable e) {
            }

        }

        public Connection getConnection()
        {
            return connection;
        }

        public void setConnection(Connection connection)
        {
            this.connection = connection;
        }

        public Date getDate()
        {
            return date;
        }

        public void setDate(Date date)
        {
            this.date = date;
        }

        public Exception getException()
        {
            return exception;
        }

        public void setException(Exception exception)
        {
            this.exception = exception;
        }

        public URL getURL()
        {
            return url;
        }

        public void setURL(URL url)
        {
            this.url = url;
        }

    }

    protected Configuration makeMapping(String className, String custommapping1)
    {
        Configuration hibconfig = new Configuration();
        {
            hibconfig.addXML(makeMapping(className, "xwikicustom_"
                + className.replaceAll("\\.", "_"), custommapping1));
        }
        hibconfig.buildMappings();
        return hibconfig;
    }

    protected String makeMapping(String entityName, String tableName, String custommapping1)
    {
        String custommapping =
            "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE hibernate-mapping PUBLIC\n"
                + "\t\"-//Hibernate/Hibernate Mapping DTD//EN\"\n"
                + "\t\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n"
                + "<hibernate-mapping>" + "<class entity-name=\"" + entityName + "\" table=\""
                + tableName + "\">\n"
                + " <id name=\"id\" type=\"integer\" unsaved-value=\"any\">\n"
                + "   <column name=\"XWO_ID\" not-null=\"true\" />\n"
                + "   <generator class=\"assigned\" />\n" + " </id>\n" + custommapping1
                + "</class>\n" + "</hibernate-mapping>";
        return custommapping;
    }

    /**
     * Callback (closure) interface for operations in hibernate. spring like.
     */
    public interface HibernateCallback
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
        Object doInHibernate(Session session) throws HibernateException, XWikiException;
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
    public Object execute(XWikiContext context, boolean bTransaction, boolean doCommit,
        HibernateCallback cb) throws XWikiException
    {
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor != null)
                monitor.startTimer("hibernate");

            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }

            return cb.doInHibernate(getSession(context));
        } catch (Exception e) {
            if (e instanceof XWikiException)
                throw (XWikiException) e;
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Exception while hibernate execute",
                e);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, doCommit);
                if (monitor != null)
                    monitor.endTimer("hibernate");
            } catch (Exception e) {
                if (log.isErrorEnabled())
                    log.error("Exeption while close transaction", e);
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
     * @see #execute(XWikiContext, boolean, boolean,
     *      com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback)
     */
    public Object executeRead(XWikiContext context, boolean bTransaction, HibernateCallback cb)
        throws XWikiException
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
     * @see #execute(XWikiContext, boolean, boolean,
     *      com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback)
     */
    public Object executeWrite(XWikiContext context, boolean bTransaction, HibernateCallback cb)
        throws XWikiException
    {
        return execute(context, bTransaction, true, cb);
    }
}
