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
package com.xpn.xwiki.internal.store.hibernate;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.DisposePriority;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.environment.Environment;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationManager;
import com.xpn.xwiki.util.Util;

/**
 * Instance shared by all hibernate based stores.
 * 
 * @version $Id$
 * @since 9.10RC1
 */
@Component(roles = HibernateStore.class)
@Singleton
@DisposePriority(10000)
public class HibernateStore implements Disposable
{
    /**
     * @see #isInSchemaMode()
     */
    private static final String VIRTUAL_MODE_SCHEMA = "schema";

    private static final String CONTEXT_SESSION = "hibsession";

    private static final String CONTEXT_TRANSACTION = "hibtransaction";

    /**
     * The name of the property for configuring the environment permanent directory.
     */
    private static final String PROPERTY_PERMANENTDIRECTORY = "environment.permanentDirectory";

    @Inject
    private Logger logger;

    /**
     * Initialize migration manager lazily to avoid cross dependency issue.
     */
    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private Provider<DataMigrationManager> dataMigrationManagerProvider;

    @Inject
    private Execution execution;

    @Inject
    private WikiDescriptorManager wikis;

    @Inject
    private HibernateConfiguration hibernateConfiguration;

    @Inject
    private Environment environment;

    private DataMigrationManager dataMigrationManager;

    private final MetadataSources metadataSources = new MetadataSources();

    private final Configuration configuration = new Configuration(this.metadataSources);

    private Dialect dialect;

    private DatabaseProduct databaseProductCache = DatabaseProduct.UNKNOWN;

    private SessionFactory sessionFactory;

    private Metadata metadata;

    private DataMigrationManager getDataMigrationManager()
    {
        if (this.dataMigrationManager == null) {
            this.dataMigrationManager = this.dataMigrationManagerProvider.get();
        }

        return this.dataMigrationManager;
    }

    /**
     * @since 11.2RC1
     */
    public void initHibernate()
    {
        String path = this.hibernateConfiguration.getPath();
        URL url = Util.getResource(path);
        if (url == null) {
            this.logger.error("Failed to find hibernate configuration file corresponding to path [{}]", path);
        } else {
            this.configuration.configure(url);

            // Resolve some variables
            replaceVariables(this.configuration);
        }

        build();
    }

    /**
     * Replace variables defined in Hibernate properties using the <code>${variable}</code> notation. Note that right
     * now the only variable being replaced is {@link #PROPERTY_PERMANENTDIRECTORY} and replaced with the value coming
     * from the XWiki configuration.
     *
     * @param hibernateConfiguration the Hibernate Configuration object that we're evaluating
     */
    private void replaceVariables(Configuration hibernateConfiguration)
    {
        String url = hibernateConfiguration.getProperty(org.hibernate.cfg.Environment.URL);
        if (StringUtils.isEmpty(url)) {
            return;
        }

        // Replace variables
        if (url.matches(".*\\$\\{.*\\}.*")) {
            String newURL = StringUtils.replace(url, String.format("${%s}", PROPERTY_PERMANENTDIRECTORY),
                this.environment.getPermanentDirectory().getAbsolutePath());

            // Set the new URL
            hibernateConfiguration.setProperty(org.hibernate.cfg.Environment.URL, newURL);
            this.logger.debug("Resolved Hibernate URL [{}] to [{}]", url, newURL);
        }
    }

    /**
     * @since 11.1RC1
     */
    public void build()
    {
        // Get rid of existing session factory
        disposeInternal();

        // Create a new session factory
        this.sessionFactory = this.configuration.buildSessionFactory();
        this.metadata = this.metadataSources.buildMetadata();
    }

    private void disposeInternal()
    {
        Session session = getCurrentSession();
        closeSession(session);

        if (this.sessionFactory != null) {
            this.sessionFactory.close();
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        disposeInternal();
    }

    /**
     * @return the Hibernate configuration
     */
    public Configuration getConfiguration()
    {
        return this.configuration;
    }

    /**
     * @return true if the user has configured Hibernate to use XWiki in schema mode (vs database mode)
     */
    public boolean isInSchemaMode()
    {
        String virtualModePropertyValue = getConfiguration().getProperty("xwiki.virtual_mode");
        if (virtualModePropertyValue == null) {
            virtualModePropertyValue = VIRTUAL_MODE_SCHEMA;
        }
        return StringUtils.equals(virtualModePropertyValue, VIRTUAL_MODE_SCHEMA);
    }

    /**
     * Convert wiki name in database/schema name.
     *
     * @param wikiId the wiki name to convert.
     * @param product the database engine type.
     * @return the database/schema name.
     */
    public String getSchemaFromWikiName(String wikiId, DatabaseProduct product)
    {
        if (wikiId == null) {
            return null;
        }

        String mainWikiId = this.wikis.getMainWikiId();

        String schema;
        if (StringUtils.equalsIgnoreCase(wikiId, mainWikiId)) {
            schema = this.hibernateConfiguration.getDB();
            if (schema == null) {
                if (product == DatabaseProduct.DERBY) {
                    schema = "APP";
                } else if (product == DatabaseProduct.HSQLDB || product == DatabaseProduct.H2) {
                    schema = "PUBLIC";
                } else if (product == DatabaseProduct.POSTGRESQL && isInSchemaMode()) {
                    schema = "public";
                } else {
                    schema = wikiId.replace('-', '_');
                }
            }
        } else {
            // virtual
            schema = wikiId.replace('-', '_');

            // For HSQLDB/H2 we only support uppercase schema names. This is because Hibernate doesn't properly generate
            // quotes around schema names when it qualifies the table name when it generates the update script.
            if (DatabaseProduct.HSQLDB == product || DatabaseProduct.H2 == product) {
                schema = StringUtils.upperCase(schema);
            }
        }

        // Apply prefix
        String prefix = this.hibernateConfiguration.getDBPrefix();
        schema = prefix + schema;

        return schema;
    }

    /**
     * Convert wiki name in database/schema name.
     * <p>
     * Need hibernate to be initialized.
     *
     * @param wikiId the wiki name to convert.
     * @return the database/schema name.
     */
    public String getSchemaFromWikiName(String wikiId)
    {
        return getSchemaFromWikiName(wikiId, getDatabaseProductName());
    }

    /**
     * Convert wiki name in database/schema name.
     * <p>
     * Need hibernate to be initialized.
     *
     * @return the database/schema name.
     */
    public String getSchemaFromWikiName()
    {
        return getSchemaFromWikiName(this.wikis.getCurrentWikiId());
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
        DatabaseProduct product = this.databaseProductCache;

        if (product == DatabaseProduct.UNKNOWN) {
            DatabaseMetaData metaData = getDatabaseMetaData();
            if (metaData != null) {
                try {
                    product = DatabaseProduct.toProduct(metaData.getDatabaseProductName());
                } catch (SQLException ignored) {
                    // do not care, return UNKNOWN
                }
            } else {
                // do not care, return UNKNOWN
            }
        }

        return product;
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
        try (SessionImplementor session = (SessionImplementor) getSessionFactory().openSession()) {
            JdbcConnectionAccess jdbcConnectionAccess = session.getJdbcConnectionAccess();

            try (Connection connection = jdbcConnectionAccess.obtainConnection()) {
                return connection.getMetaData();
            } catch (SQLException e) {
                // Log something ?
            }
        }

        return null;
    }

    /**
     * @since 11.1RC1
     */
    public Metadata getMetadata()
    {
        return this.metadata;
    }

    public Metadata getMetadata(String className, String customMapping)
    {
        MetadataSources builder = new MetadataSources();

        builder.addInputStream(
            new ByteArrayInputStream(makeMapping(className, customMapping).getBytes(StandardCharsets.UTF_8)));

        return builder.buildMetadata();
    }

    /**
     * Build a new XML string to define the provided mapping. Since 4.0M1, the ids are longs, and a conditional mapping
     * is made for Oracle.
     *
     * @param className the name of the class to map.
     * @param customMapping the custom mapping
     * @return a XML definition for the given mapping, using XWO_ID column for the object id.
     * @since 11.1RC1
     */
    public String makeMapping(String className, String customMapping)
    {
        DatabaseProduct databaseProduct = getDatabaseProductName();

        return new StringBuilder(2000).append("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n")
            .append("<!DOCTYPE hibernate-mapping PUBLIC\n").append("\t\"-//Hibernate/Hibernate Mapping DTD//EN\"\n")
            .append("\t\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n").append("<hibernate-mapping>")
            .append("<class entity-name=\"").append(className).append("\" table=\"")
            .append(toDynamicMappingTableName(className)).append("\">\n")
            .append(" <id name=\"id\" type=\"long\" unsaved-value=\"any\">\n")
            .append("   <column name=\"XWO_ID\" not-null=\"true\" ")
            .append((databaseProduct == DatabaseProduct.ORACLE) ? "sql-type=\"integer\" " : "")
            .append("/>\n   <generator class=\"assigned\" />\n").append(" </id>\n").append(customMapping)
            .append("</class>\n</hibernate-mapping>").toString();
    }

    /**
     * Return the name generated for a dynamic mapped object.
     *
     * @param className the classname of the object.
     * @return a name in the form xwikicustom_space_class
     * @since 11.1RC1
     */
    public String toDynamicMappingTableName(String className)
    {
        return "xwikicustom_" + className.replaceAll("\\.", "_");
    }

    /**
     * Escape schema name depending of the database engine.
     *
     * @param schema the schema name to escape
     * @return the escaped version
     */
    public String escapeSchema(String schema)
    {
        String escapedSchema;

        // - Oracle converts user names in uppercase when no quotes is used.
        // For example: "create user xwiki identified by xwiki;" creates a user named XWIKI (uppercase)
        // - In Hibernate.cfg.xml we just specify: <property name="hibernate.connection.username">xwiki</property> and Hibernate
        // seems to be passing this username as is to Oracle which converts it to uppercase.
        //
        // Thus for Oracle we don't escape the schema.
        if (DatabaseProduct.ORACLE == getDatabaseProductName()) {
            escapedSchema = schema;
        } else {
            String closeQuote = String.valueOf(getDialect().closeQuote());
            escapedSchema = getDialect().openQuote() + schema.replace(closeQuote, closeQuote + closeQuote) + closeQuote;
        }

        return escapedSchema;
    }

    /**
     * @return a singleton instance of the configured {@link Dialect}
     */
    public Dialect getDialect()
    {
        if (this.dialect == null) {
            this.dialect = Dialect.getDialect(getConfiguration().getProperties());
        }

        return this.dialect;
    }

    /**
     * Set the current wiki in the passed session.
     * 
     * @param session the hibernate session
     * @throws XWikiException when failing to switch wiki
     */
    public void setWiki(Session session) throws XWikiException
    {
        setWiki(session, this.wikis.getCurrentWikiId());
    }

    /**
     * Set the passed wiki in the passed session
     *
     * @param session the hibernate session
     * @param wikiId the id of the wiki to switch to
     * @throws XWikiException when failing to switch wiki
     */
    public void setWiki(Session session, String wikiId) throws XWikiException
    {
        try {
            this.logger.debug("Set the right catalog/schema in teh session [{}]", wikiId);

            // Switch the database only if we did not switched on it last time
            if (wikiId != null) {
                String schemaName = getSchemaFromWikiName(wikiId);
                String escapedSchemaName = escapeSchema(schemaName);

                DatabaseProduct product = getDatabaseProductName();
                if (DatabaseProduct.ORACLE == product) {
                    executeSQL("alter session set current_schema = " + escapedSchemaName, session);
                } else if (DatabaseProduct.DERBY == product || DatabaseProduct.HSQLDB == product
                    || DatabaseProduct.DB2 == product || DatabaseProduct.H2 == product) {
                    executeSQL("SET SCHEMA " + escapedSchemaName, session);
                } else if (DatabaseProduct.POSTGRESQL == product && isInSchemaMode()) {
                    executeSQL("SET search_path TO " + escapedSchemaName, session);
                } else {
                    session.doWork(connection -> {
                        String catalog = connection.getCatalog();
                        catalog = (catalog == null) ? null : catalog.replace('_', '-');
                        if (!schemaName.equals(catalog)) {
                            connection.setCatalog(schemaName);
                        }
                    });
                }
            }

            getDataMigrationManager().checkDatabase();
        } catch (Exception e) {
            // close session with rollback to avoid further usage
            endTransaction(false);

            Object[] args = { wikiId };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE, "Exception while switching to database {0}",
                e, args);
        }
    }

    /**
     * @return the current {@link Session} or null
     */
    public Session getCurrentSession()
    {
        ExecutionContext context = this.execution.getContext();

        if (context != null) {
            Session session = (Session) context.getProperty(CONTEXT_SESSION);

            // Make sure we are in this mode
            try {
                if (session != null) {
                    session.setHibernateFlushMode(FlushMode.COMMIT);
                }
            } catch (org.hibernate.SessionException ex) {
                session = null;
            }

            return session;
        }

        return null;
    }

    /**
     * @param session the new current {@link Session}
     */
    public void setCurrentSession(Session session)
    {
        ExecutionContext context = this.execution.getContext();

        if (session == null) {
            context.removeProperty(CONTEXT_SESSION);
        } else {
            context.setProperty(CONTEXT_SESSION, session);
        }
    }

    /**
     * @return the current {@link Transaction} or null
     */
    public Transaction getCurrentTransaction()
    {
        ExecutionContext context = this.execution.getContext();

        return (Transaction) context.getProperty(CONTEXT_TRANSACTION);
    }

    /**
     * @param transaction the new current {@link Transaction}
     */
    public void setCurrentTransaction(Transaction transaction)
    {
        ExecutionContext context = this.execution.getContext();

        if (transaction == null) {
            context.removeProperty(CONTEXT_TRANSACTION);
        } else {
            context.setProperty(CONTEXT_TRANSACTION, transaction);
        }
    }

    /**
     * Begins a transaction with a specific SessionFactory.
     *
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     */
    public boolean beginTransaction() throws XWikiException
    {
        return beginTransaction(null);
    }

    /**
     * Begins a transaction with a specific SessionFactory.
     *
     * @param sfactory the session factory used to begin a new session if none are available
     * @return true if a new transaction has been created, false otherwise.
     * @throws XWikiException if an error occurs while retrieving or creating a new session and transaction.
     */
    public boolean beginTransaction(SessionFactory sfactory) throws XWikiException
    {
        Transaction transaction = getCurrentTransaction();
        Session session = getCurrentSession();

        // XWiki uses a new Session for a new Transaction so we need to keep both in sync and thus we check if that's
        // the case. If it isn't it means some code is faulty somewhere.
        if (((session == null) && (transaction != null)) || ((transaction == null) && (session != null))) {
            this.logger.warn("Incompatible session ({}) and transaction ({}) status", session, transaction);

            // TODO: Fix this problem, don't ignore it!
            return false;
        }

        if (session != null) {
            this.logger.debug("Taking session from context [{}]", session);
            this.logger.debug("Taking transaction from context [{}]", transaction);

            return false;
        }

        // session is obviously null here
        this.logger.debug("Trying to get session from pool");
        if (sfactory == null) {
            session = getSessionFactory().openSession();
        } else {
            session = sfactory.openSession();
        }

        this.logger.debug("Taken session from pool [{}]", session);

        setCurrentSession(session);

        this.logger.debug("Trying to open transaction");
        transaction = session.beginTransaction();
        this.logger.debug("Opened transaction [{}]", transaction);
        setCurrentTransaction(transaction);

        // during #setDatabase, the transaction and the session will be closed if the database could not be
        // safely accessed due to version mismatch
        setWiki(session);

        return true;
    }

    /**
     * @since 11.1RC1
     */
    public SessionFactory getSessionFactory()
    {
        return this.sessionFactory;
    }

    /**
     * Ends a transaction and close the session.
     *
     * @param commit should we commit or not
     */
    public void endTransaction(boolean commit)
    {
        Session session = null;
        try {
            session = getCurrentSession();
            Transaction transaction = getCurrentTransaction();
            setCurrentSession(null);
            setCurrentTransaction(null);

            if (transaction != null) {
                this.logger.debug("Releasing hibernate transaction [{}]", transaction);

                if (commit) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                }
            }
        } catch (HibernateException e) {
            // Ensure the original cause will get printed.
            throw new HibernateException(
                "Failed to commit or rollback transaction. Root cause [" + getExceptionMessage(e) + "]", e);
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
     * Allows to shut down the hibernate configuration Closing all pools and connections.
     * 
     * @deprecated automatically done when the component is disposed
     */
    @Deprecated
    public void shutdownHibernate()
    {
        // Close all connections
        disposeInternal();
    }

    /**
     * Closes the hibernate session.
     *
     * @param session the sessions to close
     * @throws HibernateException
     */
    private void closeSession(Session session)
    {
        if (session != null) {
            session.close();
        }
    }

    /**
     * Execute an SQL statement using Hibernate.
     *
     * @param sql the SQL statement to execute
     * @param session the Hibernate Session in which to execute the statement
     */
    private void executeSQL(final String sql, Session session)
    {
        session.doWork(new Work()
        {
            @Override
            public void execute(Connection connection) throws SQLException
            {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                }
            }
        });
    }

    /**
     * Automatically update the current database schema to contains what's defined in standard metadata.
     * 
     * @since 11.2RC1
     */
    public void updateSchema(String wikiId)
    {
        updateSchema(getMetadata(), wikiId);
    }

    /**
     * Automatically update the current database schema to contains what's defined in standard metadata.
     * 
     * @param metadata the metadata we want the current database to follow
     * @since 11.2RC1
     */
    public void updateSchema(Metadata metadata, String wikiId)
    {
        new SchemaUpdate().execute(EnumSet.of(TargetType.DATABASE), metadata);
    }

    /**
     * Allows to update the schema to match the hibernate mapping
     *
     * @param wikiId the identifier of the wiki to update
     * @param force defines whether or not to force the update despite the xwiki.cfg settings
     * @since 11.2RC1
     */
    public synchronized void updateSchema(String wikiId, boolean force)
    {
        // We don't update the schema if the XWiki hibernate config parameter says not to update
        if (!force && !this.hibernateConfiguration.isUpdateSchema()) {
            this.logger.debug("Schema update deactivated for wiki [{}]", wikiId);
            return;
        }

        this.logger.info("Updating schema for wiki [{}]...", wikiId);

        try {
            updateSchema(wikiId);
        } finally {
            this.logger.info("Schema update for wiki [{}] done", wikiId);
        }
    }
}
