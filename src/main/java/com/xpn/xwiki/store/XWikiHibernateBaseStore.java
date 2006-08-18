package com.xpn.xwiki.store;

import org.hibernate.*;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.cfg.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Date;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.monitor.api.MonitorPlugin;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 10 mars 2006
 * Time: 14:04:13
 * To change this template use File | Settings | File Templates.
 */
public class XWikiHibernateBaseStore {
    private static final Log log = LogFactory.getLog(XWikiHibernateBaseStore.class);

    private Map connections = new HashMap();
    private int nbConnections = 0;
    private SessionFactory sessionFactory;
    private Configuration configuration;

    private String hibpath;
    private URL hiburl;

    /**
     * THis allows to initialize our storage engine.
     * The hibernate config file path is taken from xwiki.cfg
     * or directly in the WEB-INF directory.
     * @param xwiki
     * @param context
     */
    public XWikiHibernateBaseStore(XWiki xwiki, XWikiContext context) {
        String path = xwiki.Param("xwiki.store.hibernate.path", "hibernate.cfg.xml");
        if ((path!=null)&&((new File(path).exists() || context.getEngineContext() == null))) {
            setPath (path);
        } else {
            try {
                setHibUrl(context.getEngineContext().getResource(path));
            } catch (MalformedURLException e) {
            }
        }
    }

    /**
     * Initialize the storage engine with a specific path
     * This is used for tests.
     * @param hibpath
     */
    public XWikiHibernateBaseStore(String hibpath) {
        setPath(hibpath);
    }

    /**
     * Allows to get the current hibernate config file path
     * @return
     */
    public String getPath() {
        return hibpath;
    }

    /**
     * Allows to set the current hibernate config file path
     * @param hibpath
     */
    public void setPath(String hibpath) {
        this.hibpath = hibpath;
    }

    /**
     * Get's the hibernate config path as an URL
     * @return
     */
    public URL getHibUrl() {
        return hiburl;
    }

    /**
     * Set the hibernate config path as an URL
     * @param hiburl
     */
    public void setHibUrl(URL hiburl) {
        this.hiburl = hiburl;
    }

    /**
     * Allows to init the hibernate configuration
     * @throws org.hibernate.HibernateException
     */
    private void initHibernate() throws HibernateException {
        // Load Configuration and build SessionFactory
        String path = getPath();
        if (path!=null)
            setConfiguration((new Configuration()).configure(new File(path)));
        else {
            URL hiburl = getHibUrl();
            if (hiburl!=null)
                setConfiguration(new Configuration().configure(hiburl));
            else
                setConfiguration(new Configuration().configure());
        }

        setSessionFactory(getConfiguration().buildSessionFactory());
    }

    /**
     * This get's the current session.
     * This is set in beginTransaction
     * @param context
     * @return
     */
    public Session getSession(XWikiContext context) {
        Session session = (Session) context.get("hibsession");
        // Make sure we are in this mode
        if (session!=null)
         session.setFlushMode(FlushMode.COMMIT);
        return session;
    }

    /**
     * Allows to set the current session in the context
     * This is set in beginTransaction
     * @param session
     * @param context
     */
    public void setSession(Session session, XWikiContext context) {
        if (session==null)
            context.remove("hibsession");
        else
            context.put("hibsession", session);
    }


    /**
     * Allows to get the current transaction from the context
     * This is set in beginTransaction
     * @param context
     * @return
     */
    public Transaction getTransaction(XWikiContext context) {
        Transaction transaction = (Transaction) context.get("hibtransaction");
        return transaction;
    }

    /**
     * Allows to set the current transaction
     * This is set in beginTransaction
     * @param transaction
     * @param context
     */
    public void setTransaction(Transaction transaction, XWikiContext context) {
        if (transaction==null)
            context.remove("hibtransaction");
        else
            context.put("hibtransaction", transaction);
    }


    /**
     * Allows to shut down the hibernate configuration
     * Closing all pools and connections
     * @param context
     * @throws HibernateException
     */
    public void shutdownHibernate(XWikiContext context) throws HibernateException {
        closeSession(getSession(context));
        if (getSessionFactory()!=null) {
            ((SessionFactoryImpl)getSessionFactory()).getConnectionProvider().close();
        }
    }

    /**
     * Allows to update the schema to match the hibernate mapping
     * @param context
     * @throws HibernateException
     */
    public void updateSchema(XWikiContext context) throws HibernateException {
        updateSchema(context, false);
    }


    /**
     * Allows to update the schema to match the hibernate mapping
     * @param context
     * @param force defines wether or not to force the update despite the xwiki.cfg settings
     * @throws HibernateException
     */
    public synchronized void updateSchema(XWikiContext context, boolean force) throws HibernateException {
        try {
            // No updating of schema if we have a config parameter saying so
            try {
                if ((!force)&&(context.getWiki()!=null)&&("0".equals(context.getWiki().Param("xwiki.store.hibernate.updateschema")))) {
                    if (log.isInfoEnabled())
                        log.info("Schema update deactivated for wiki " + context.getDatabase());
                    return;
                }

                if (log.isInfoEnabled())
                    log.info("Schema update for wiki " + context.getDatabase());

            } catch (Exception e) {}

            String fullName = ((context!=null)&&(context.getWiki()!=null)&&(context.getWiki().isMySQL())) ?  "concat('xwd_web','.','xwd_name)" : "xwd_fullname";
            String[] schemaSQL = getSchemaUpdateScript(getConfiguration(), context);
            String[] addSQL = {
                // Make sure we have no null valued in integer fields
                "update xwikidoc set xwd_translation=0 where xwd_translation is null",
                "update xwikidoc set xwd_language='' where xwd_language is null",
                "update xwikidoc set xwd_default_language='' where xwd_default_language is null",
                "update xwikidoc set xwd_fullname=" + fullName + " where xwd_fullname is null",
                "update xwikidoc set xwd_elements=3 where xwd_elements is null",
                "delete from xwikiproperties where xwp_name like 'editbox_%' and xwp_classtype='com.xpn.xwiki.objects.LongProperty'",
                "delete from xwikilongs where xwl_name like 'editbox_%'"
                };

            String[] sql = new String[schemaSQL.length+addSQL.length];
            for (int i=0;i<schemaSQL.length;i++)
                sql[i] = schemaSQL[i];
            for (int i=0;i<addSQL.length;i++)
                sql[i + schemaSQL.length] = addSQL[i];

            updateSchema(sql, context);
        } finally {

            if (log.isInfoEnabled())
                log.info("Schema update for wiki " + context.getDatabase() + " done");
        }
    }

    /**
     * This function gets the schema update scripts generated by comparing the current database
     * woth the current hibernate mapping config.
     * @param config
     * @param context
     * @return
     * @throws HibernateException
     */
    public String[] getSchemaUpdateScript(Configuration config, XWikiContext context) throws HibernateException {
        String[] schemaSQL = null;

        Session session;
        Connection connection;
        DatabaseMetadata meta;
        Statement stmt=null;
        Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());
        boolean bTransaction = true;

        try {
            bTransaction = beginTransaction(false, context);
            session = getSession(context);
            connection = session.connection();
            setDatabase(session, context);

            meta = new DatabaseMetadata(connection, dialect);
            stmt = connection.createStatement();

            schemaSQL = config.generateSchemaUpdateScript(dialect, meta);
        }
        catch (Exception e) {
            if ( log.isErrorEnabled() ) log.error("Failed updating schema: " + e.getMessage());
        }
        finally {
            try {
                if (stmt!=null) stmt.close();
                if (bTransaction)
                    endTransaction(context, false, false);
            }
            catch (Exception e) {
            }
        }
        return schemaSQL;
    }

    /**
     * Runs the update script on the current database
     * @param createSQL
     * @param context
     */
    public void updateSchema(String[] createSQL, XWikiContext context) {
        // Updating the schema for custom mappings
        Session session;
        Connection connection;
        Statement stmt=null;
        boolean bTransaction = true;
        MonitorPlugin monitor  = Util.getMonitorPlugin(context);

        try {
            bTransaction = beginTransaction(context);
            session = getSession(context);
            connection = session.connection();
            setDatabase(session, context);
            stmt = connection.createStatement();

            // Start monitoring timer
            if (monitor!=null)
                monitor.startTimer("sqlupgrade");
            for (int j = 0; j < createSQL.length; j++) {
                final String sql = createSQL[j];
                if ( log.isDebugEnabled() ) log.debug("Update Schema sql: " + sql);
                stmt.executeUpdate(sql);
            }
            connection.commit();
        }
        catch (Exception e) {
            if ( log.isErrorEnabled() ) log.error("Failed updating schema: " + e.getMessage());
        }
        finally {
            try {
                if (stmt!=null) stmt.close();
            } catch (Exception e)  {};
            try {
                if (bTransaction)
                    endTransaction(context, true);
            } catch (Exception e) {}

            // End monitoring timer
            if (monitor!=null)
                monitor.endTimer("sqlupgrade");
        }
    }


    /**
     * Custom Mapping
     * This function update the schema based on the dynamic custom mapping
     * provided by the class
     * @param bclass
     * @param context
     * @throws com.xpn.xwiki.XWikiException
     */
    public void updateSchema(BaseClass bclass, XWikiContext context) throws XWikiException {
        String custommapping = bclass.getCustomMapping();
        if (!bclass.hasExternalCustomMapping())
         return;

        Configuration config = makeMapping(bclass.getName(), custommapping);
        /* if (isValidCustomMapping(bclass.getName(), config, bclass)==false) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING,
                    "Cannot update schema for class " + bclass.getName() + " because of an invalid mapping");
        } */

        String[] sql = getSchemaUpdateScript(config, context);
        updateSchema(sql, context);
    }

    /**
     * Initializes hibernate and calls updateSchema if necessary
     * @param context
     * @throws HibernateException
     */
    public void checkHibernate(XWikiContext context) throws HibernateException {

        if (getSessionFactory()==null) {
            initHibernate();

            /* Check Schema */
            if (getSessionFactory()!=null) {
                updateSchema(context);
            }
        }
    }

    /**
     * Checks if this xwiki setup is virtual
     * meaning if multiple wikis can be accessed using the same database pool
     * @param context
     * @return
     */
    protected boolean isVirtual(XWikiContext context) {
        if ((context==null)||(context.getWiki()==null))
            return true;
        return context.getWiki().isVirtual();
    }

    /**
     * Virtual Wikis
     * Allows to switch database connection
     * @param session
     * @param context
     * @throws XWikiException
     */
    public void setDatabase(Session session, XWikiContext context) throws XWikiException {
        if (isVirtual(context)) {
            String database = context.getDatabase();
            try {
                if ( log.isDebugEnabled() ) log.debug("Switch database to: " + database);
                if (database!=null) {
                    if (!database.equals(session.connection().getCatalog()))
                          session.connection().setCatalog(database);
                }
            } catch (Exception e) {
                Object[] args = { database };
                throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE,
                        "Exception while switching to database {0}", e, args);
            }
        }
    }

    /**
     * Begins a transaction
     * @param context
     * @return
     * @throws XWikiException
     */
    public boolean beginTransaction(XWikiContext context) throws XWikiException {
            return beginTransaction(null, true, context);
    }

    /**
     * Begins a transaction
     * @param withTransaction
     * @param context
     * @return
     * @throws XWikiException
     */
    public boolean beginTransaction(boolean withTransaction, XWikiContext context) throws XWikiException {
            return beginTransaction(null, withTransaction, context);
    }

    /**
     * Begins a transaction with a specific SessionFactory
     * @param sfactory
     * @param context
     * @return
     * @throws XWikiException
     */
    public boolean beginTransaction(SessionFactory sfactory, XWikiContext context) throws XWikiException {
        return beginTransaction(sfactory, true, context);
    }

    /**
     * Begins a transaction with a specific SessionFactory
     * @param sfactory
     * @param withTransaction
     * @param context
     * @return
     * @throws HibernateException
     * @throws XWikiException
     */
    public boolean beginTransaction(SessionFactory sfactory, boolean withTransaction, XWikiContext context)
            throws HibernateException, XWikiException {

        Transaction transaction = getTransaction(context);
        Session session = getSession(context);

        if (((session==null)&&(transaction!=null))
                ||((transaction==null)&&(session!=null))) {
            if ( log.isWarnEnabled() ) log.warn("Incompatible session (" + session + ") and transaction (" + transaction + ") status");
            return false;
        }

        if (session!=null) {
            if ( log.isDebugEnabled() ) log.debug("Taking session from context " + session);
            if ( log.isDebugEnabled() ) log.debug("Taking transaction from context " + transaction);
            return false;
        }

        if (session==null) {
            if ( log.isDebugEnabled() ) log.debug("Trying to get session from pool");
            if (sfactory==null)
                session = (SessionImpl)getSessionFactory().openSession();
            else
                session = sfactory.openSession();

            if ( log.isDebugEnabled() ) log.debug("Taken session from pool " + session);

            // Keep some statistics about session and connections
            nbConnections++;
            addConnection(session.connection(), context);

            setSession(session, context);
            setDatabase(session, context);

            if ( log.isDebugEnabled() )
                log.debug("Trying to open transaction");
            transaction = session.beginTransaction();
            if ( log.isDebugEnabled() )
                log.debug("Opened transaction " + transaction);
            setTransaction(transaction, context);
        }
        return true;
    }

    /**
     * Adding a connection to the Monitor module
     * @param connection
     * @param context
     */
    private void addConnection(Connection connection, XWikiContext context) {
        if (connection!=null)
            connections.put(connection, new ConnectionMonitor(connection, context));
    }

    /**
     * Remove a connection to the Monitor module
     * @param connection
     */
    private void removeConnection(Connection connection) {
        try {
            if (connection!=null)
                connections.remove(connection);
        } catch (Exception e) {
        }
    }

    /**
     * Ends a transaction
     * @param context
     * @param commit should we commit or not
     */
    public void endTransaction(XWikiContext context, boolean commit) {
        endTransaction(context, commit, false);
    }

    /**
     * Ends a transaction
     * @param context
     * @param commit should we commit or not
     * @param withTransaction
     * @throws HibernateException
     */
    public void endTransaction(XWikiContext context, boolean commit, boolean withTransaction)
            throws HibernateException {
        Session session = null;
        try {
            session = getSession(context);
            Transaction transaction = getTransaction(context);
            setSession(null, context);
            setTransaction(null, context);

            if (transaction!=null) {
                if ( log.isDebugEnabled() ) log.debug("Releasing hibernate transaction " + transaction);
                if (commit) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                }
            }
        } finally {
            if (session!=null) {
                closeSession(session);
            }
        }
    }

    /**
     * Closes the hibernate session
     * @param session
     * @throws HibernateException
     */
    private void closeSession(Session session) throws HibernateException {
        if (session!=null) {
            try {
                if ( log.isDebugEnabled() ) log.debug("Releasing hibernate session " + session);
                Connection connection = session.connection();
                if ((connection!=null)) {
                    nbConnections--;
                    try {
                        removeConnection(connection);
                    } catch (Throwable e) {
                        // This should not happen
                        e.printStackTrace();
                    }
                }
            } finally {
                session.close();
            }
        }
    }

    /**
     * Cleanup all sessions
     * Used at the shutdown time
     * @param context
     */
    public void cleanUp(XWikiContext context) {
        try {
            Session session = getSession(context);
            if (session!=null) {
                if ( log.isWarnEnabled() ) log.warn("Cleanup of session was needed: " + session);
                endTransaction(context, false);
            }
        } catch (HibernateException e) {
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Collection getConnections() {
        return connections.values();
    }

    public int getNbConnections() {
        return nbConnections;
    }

    public void setNbConnections(int nbConnections) {
        this.nbConnections = nbConnections;
    }


    public class ConnectionMonitor {
        private Exception exception;
        private Connection connection;
        private Date date;
        private URL url = null;

        public ConnectionMonitor(Connection connection, XWikiContext context) {
            this.setConnection(connection);

            try {
                setDate(new Date());
                setException(new XWikiException());
                XWikiRequest request = context.getRequest();
                if (request!=null)
                    setURL(XWiki.getRequestURL(context.getRequest()));
            } catch (Throwable e) {
            }

        }

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public URL getURL() {
            return url;
        }

        public void setURL(URL url) {
            this.url = url;
        }

    }

    protected Configuration makeMapping(String className, String custommapping1) {
        Configuration hibconfig = new Configuration();
        {
            hibconfig.addXML(makeMapping(className , "xwikicustom_" + className.replace('.','_'), custommapping1));
        }
        hibconfig.buildMappings();
        return hibconfig;
    }

    protected String makeMapping(String entityName, String tableName, String custommapping1) {
        String custommapping = "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE hibernate-mapping PUBLIC\n" +
                "\t\"-//Hibernate/Hibernate Mapping DTD//EN\"\n" +
                "\t\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n" +
                "<hibernate-mapping>" +
                "<class entity-name=\"" + entityName + "\" table=\"" + tableName+ "\">\n" +
                " <id name=\"id\" type=\"integer\" unsaved-value=\"any\">\n" +
                "   <column name=\"XWO_ID\" not-null=\"true\" />\n" +
                "   <generator class=\"assigned\" />\n" +
                " </id>\n" +
                custommapping1 +
                "</class>\n" +
                "</hibernate-mapping>";
        return custommapping;
    }
}
