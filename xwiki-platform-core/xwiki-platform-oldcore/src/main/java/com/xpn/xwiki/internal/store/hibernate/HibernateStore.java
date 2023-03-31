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
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.query.NativeQuery;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.DisposePriority;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.environment.Environment;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.legacy.LegacySessionImplementor;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationManager;

/**
 * Instance shared by all hibernate based stores.
 * 
 * @version $Id$
 * @since 9.10RC1
 */
@Component(roles = HibernateStore.class)
@Singleton
// Make sure the Hibernate store is disposed at the end in case some components needs it for their own dispose
@DisposePriority(10000)
public class HibernateStore implements Disposable, Initializable
{
    /**
     * @see #isConfiguredInSchemaMode()
     */
    private static final String VIRTUAL_MODE_SCHEMA = "schema";

    private static final String CONTEXT_SESSION = "hibsession";

    private static final String CONTEXT_TRANSACTION = "hibtransaction";

    /**
     * The name of the property for configuring the environment permanent directory.
     */
    private static final String PROPERTY_PERMANENTDIRECTORY = "environment.permanentDirectory";

    /**
     * The name of the property for configuring the current timezone.
     */
    private static final String PROPERTY_TIMEZONE_VARIABLE = "${timezone}";

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

    @Inject
    private LoggerConfiguration loggerConfiguration;

    private DataMigrationManager dataMigrationManager;

    private MetadataSources metadataSources;

    private HibernateStoreConfiguration configuration;

    private BootstrapServiceRegistry bootstrapServiceRegistry;

    private StandardServiceRegistry standardRegistry;

    private Dialect dialect;

    private DatabaseProduct databaseProductCache = DatabaseProduct.UNKNOWN;

    private SessionFactory sessionFactory;

    private Metadata configuredMetadata;

    private String configurationCatalog;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private URL configurationURL;

    private DataMigrationManager getDataMigrationManager()
    {
        if (this.dataMigrationManager == null) {
            this.dataMigrationManager = this.dataMigrationManagerProvider.get();
        }

        return this.dataMigrationManager;
    }

    private URL getHibernateConfigurationURL()
    {
        String path = this.hibernateConfiguration.getPath();

        if (path == null) {
            return null;
        }

        File file = new File(path);
        try {
            if (file.exists()) {
                return file.toURI().toURL();
            }
        } catch (Exception e) {
            // Probably running under -security, which prevents calling File.exists()
            this.logger.debug("Failed load resource [{}] using a file path", path);
        }

        try {
            URL res = this.environment.getResource(path);
            if (res != null) {
                return res;
            }
        } catch (Exception e) {
            this.logger.debug("Failed to load resource [{}] using the application context", path);
        }

        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            this.logger.error("Failed to find hibernate configuration file corresponding to path [{}]",
                this.hibernateConfiguration.getPath());
        }

        return url;
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Search for the base configuration file
        this.configurationURL = getHibernateConfigurationURL();

        // For retro compatibility reasons we have to create an old Configuration object since it's exposed in the API
        this.configuration = new HibernateStoreConfiguration(this.configurationURL);
        replaceVariables(this.configuration);
    }

    private void disposeSessionFactory()
    {
        Session session = getCurrentSession();
        closeSession(session);

        if (this.sessionFactory != null) {
            this.sessionFactory.close();
        }
        if (this.standardRegistry != null) {
            this.standardRegistry.close();
        }
        if (this.bootstrapServiceRegistry != null) {
            this.bootstrapServiceRegistry.close();
        }
    }

    private void createSessionFactory()
    {
        this.bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();

        // Load the base configuration file
        ConfigLoader configLoader = new ConfigLoader(this.bootstrapServiceRegistry);
        LoadedConfig baseConfiguration = configLoader.loadConfigXmlUrl(this.configurationURL);
        // Resolve some variables
        replaceVariables(baseConfiguration);

        StandardServiceRegistryBuilder standardRegistryBuilder =
            new StandardServiceRegistryBuilder(this.bootstrapServiceRegistry);
        standardRegistryBuilder.configure(baseConfiguration);
        this.standardRegistry = standardRegistryBuilder.build();

        this.metadataSources = new MetadataSources(this.standardRegistry);

        // Copy the extended configuration
        this.configuration.copy(this.metadataSources);

        MetadataBuilder metadataBuilder = this.metadataSources.getMetadataBuilder();
        this.configuredMetadata = metadataBuilder.build();

        Identifier catalog = this.configuredMetadata.getDatabase().getJdbcEnvironment().getCurrentCatalog();
        if (catalog != null) {
            this.configurationCatalog = catalog.getCanonicalName();
        }

        this.sessionFactory = this.configuredMetadata.getSessionFactoryBuilder().build();
    }

    /**
     * @since 11.5RC1
     */
    public void initHibernate()
    {
        build();
    }

    private String resolveURL(String url)
    {
        // Replace variables
        if (StringUtils.isNotEmpty(url) && url.matches(".*\\$\\{.*\\}.*")) {
            String newURL = StringUtils.replace(url, String.format("${%s}", PROPERTY_PERMANENTDIRECTORY),
                this.environment.getPermanentDirectory().getAbsolutePath());

            try {
                return StringUtils.replace(newURL, PROPERTY_TIMEZONE_VARIABLE,
                    URLEncoder.encode(TimeZone.getDefault().getID(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                this.logger.error("Failedd to encode the current timezone id", e);
            }
        }

        return null;
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
        String newURL = resolveURL(hibernateConfiguration.getProperty(org.hibernate.cfg.AvailableSettings.URL));
        if (newURL != null) {
            // Set the new URL
            hibernateConfiguration.setProperty(org.hibernate.cfg.AvailableSettings.URL, newURL);
            this.logger.debug("Resolved Hibernate URL [{}] to [{}]", newURL, newURL);
        }
    }

    /**
     * Replace variables defined in Hibernate properties using the <code>${variable}</code> notation. Note that right
     * now the only variable being replaced is {@link #PROPERTY_PERMANENTDIRECTORY} and replaced with the value coming
     * from the XWiki configuration.
     *
     * @param hibernateConfiguration the Hibernate Configuration object that we're evaluating
     */
    private void replaceVariables(LoadedConfig hibernateConfiguration)
    {
        Map values = hibernateConfiguration.getConfigurationValues();
        String newURL = resolveURL((String) values.get(org.hibernate.cfg.AvailableSettings.URL));
        if (newURL != null) {
            // Set the new URL
            values.put(org.hibernate.cfg.AvailableSettings.URL, newURL);
            this.logger.debug("Resolved Hibernate URL [{}] to [{}]", newURL, newURL);
        }
    }

    /**
     * Reload the Hibernate setup.
     * <p>
     * This method is synchronized to make sure that it's only executed once at a time.
     * 
     * @since 11.5RC1
     */
    public void build()
    {
        this.lock.writeLock().lock();

        try {
            // Check if it's a reload
            if (this.sessionFactory != null) {
                disposeSessionFactory();
            }

            createSessionFactory();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        disposeSessionFactory();
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
     * @since 11.6RC1
     */
    public boolean isConfiguredInSchemaMode()
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
    public String getDatabaseFromWikiName(String wikiId, DatabaseProduct product)
    {
        if (wikiId == null) {
            return null;
        }

        String database = wikiId;

        // Some databases have special database for main wiki
        // It's also possible to configure the name of the main wiki database
        String mainWikiId = this.wikis.getMainWikiId();
        if (StringUtils.equalsIgnoreCase(wikiId, mainWikiId)) {
            database = this.hibernateConfiguration.getDB();
            if (database == null) {
                if (product == DatabaseProduct.DERBY) {
                    database = "APP";
                } else if (product == DatabaseProduct.HSQLDB || product == DatabaseProduct.H2) {
                    database = "PUBLIC";
                } else if (product == DatabaseProduct.POSTGRESQL && isConfiguredInSchemaMode()) {
                    database = "public";
                } else {
                    database = wikiId;
                }
            }
        }

        // Minus (-) is not supported by many databases
        database = database.replace('-', '_');

        // In various places we need the canonical database name (which is upper case for HSQLDB, Oracle and H2) because
        // the translation is not properly done by the Dialect
        if (DatabaseProduct.HSQLDB == product || DatabaseProduct.ORACLE == product || DatabaseProduct.H2 == product) {
            database = StringUtils.upperCase(database);
        }

        // Apply prefix
        String prefix = this.hibernateConfiguration.getDBPrefix();
        database = prefix + database;

        return database;
    }

    /**
     * Convert wiki name in database name.
     * <p>
     * Need Hibernate to be initialized.
     *
     * @param wikiId the wiki name to convert.
     * @return the database name.
     * @since 11.6RC1
     */
    public String getDatabaseFromWikiName(String wikiId)
    {
        return getDatabaseFromWikiName(wikiId, getDatabaseProductName());
    }

    /**
     * Convert wiki name in database name.
     * <p>
     * Need hibernate to be initialized.
     *
     * @return the database name.
     * @since 11.6RC1
     */
    public String getDatabaseFromWikiName()
    {
        return getDatabaseFromWikiName(this.wikis.getCurrentWikiId());
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
        if (this.databaseProductCache == DatabaseProduct.UNKNOWN) {
            if (getSessionFactory() != null) {
                DatabaseMetaData metaData = getDatabaseMetaData();
                if (metaData != null) {
                    try {
                        this.databaseProductCache = DatabaseProduct.toProduct(metaData.getDatabaseProductName());
                    } catch (SQLException ignored) {
                        // do not care, return UNKNOWN
                    }
                } else {
                    // do not care, return UNKNOWN
                }
            } else {
                // Not initialized yet so we can't use the actual database product, try to deduce it from the configured
                // driver
                String connectionURL = this.configuration.getProperty("hibernate.connection.url");
                if (connectionURL == null) {
                    connectionURL = this.configuration.getProperty("connection.url");
                }
                this.databaseProductCache = DatabaseProduct.toProduct(extractJDBCConnectionURLScheme(connectionURL));
            }
        }

        return this.databaseProductCache;
    }

    private String extractJDBCConnectionURLScheme(String fullConnectionURL)
    {
        // Format of a JDBC URL is always: "jdbc:<db scheme>:..."
        return StringUtils.split(fullConnectionURL, ':')[1];
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
     * @return the {@link Metadata} corresponding to the configuration
     * @since 11.5RC1
     */
    public Metadata getConfigurationMetadata()
    {
        return this.configuredMetadata;
    }

    /**
     * @return true if the current database product is catalog based, false for a schema based databases
     * @since 11.6RC1
     */
    public boolean isCatalog()
    {
        DatabaseProduct product = getDatabaseProductName();
        if (DatabaseProduct.ORACLE == product
            || (DatabaseProduct.POSTGRESQL == product && isConfiguredInSchemaMode())) {
            return false;
        } else {
            return getDialect().canCreateCatalog();
        }

    }

    /**
     * @since 11.5RC1
     */
    public void setWiki(MetadataBuilder builder, String wikiId)
    {
        String databaseName = getDatabaseFromWikiName(wikiId);

        if (isCatalog()) {
            builder.applyImplicitCatalogName(databaseName);
        } else {
            builder.applyImplicitSchemaName(databaseName);
        }
    }

    /**
     * @since 11.5RC1
     */
    public Metadata getMetadata(String className, String customMapping, String wikiId)
    {
        MetadataSources builder = new MetadataSources();

        builder.addInputStream(
            new ByteArrayInputStream(makeMapping(className, customMapping).getBytes(StandardCharsets.UTF_8)));

        MetadataBuilder metadataBuilder = builder.getMetadataBuilder();

        if (wikiId != null) {
            setWiki(metadataBuilder, wikiId);
        }

        return metadataBuilder.build();
    }

    /**
     * Build a new XML string to define the provided mapping. Since 4.0M1, the ids are longs, and a conditional mapping
     * is made for Oracle.
     *
     * @param className the name of the class to map.
     * @param customMapping the custom mapping
     * @return a XML definition for the given mapping, using XWO_ID column for the object id.
     * @since 11.5RC1
     */
    public String makeMapping(String className, String customMapping)
    {
        DatabaseProduct databaseProduct = getDatabaseProductName();

        return new StringBuilder(2000).append("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n")
            .append("<!DOCTYPE hibernate-mapping PUBLIC\n").append("\t\"-//Hibernate/Hibernate Mapping DTD//EN\"\n")
            .append("\t\"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">\n").append("<hibernate-mapping>")
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
     * @since 11.5RC1
     */
    public String toDynamicMappingTableName(String className)
    {
        return "xwikicustom_" + className.replaceAll("\\.", "_");
    }

    /**
     * Escape database name depending of the database engine.
     *
     * @param databaseName the schema name to escape
     * @return the escaped version
     * @since 11.6RC1
     */
    public String escapeDatabaseName(String databaseName)
    {
        String escapedDatabaseName;

        // - Oracle converts user names in uppercase when no quotes is used.
        // For example: "create user xwiki identified by xwiki;" creates a user named XWIKI (uppercase)
        // - In Hibernate.cfg.xml we just specify: <property name="hibernate.connection.username">xwiki</property> and
        // Hibernate
        // seems to be passing this username as is to Oracle which converts it to uppercase.
        //
        // Thus for Oracle we don't escape the schema.
        if (DatabaseProduct.ORACLE == getDatabaseProductName()) {
            escapedDatabaseName = databaseName;
        } else {
            String closeQuote = String.valueOf(getDialect().closeQuote());
            escapedDatabaseName =
                getDialect().openQuote() + databaseName.replace(closeQuote, closeQuote + closeQuote) + closeQuote;
        }

        return escapedDatabaseName;
    }

    /**
     * @return a singleton instance of the configured {@link Dialect}
     */
    public Dialect getDialect()
    {
        if (this.dialect == null) {
            JdbcServices jdbcServices = this.standardRegistry.getService(JdbcServices.class);
            this.dialect = jdbcServices.getDialect();
        }

        return this.dialect;
    }

    /**
     * Set the current wiki in the passed session.
     * 
     * @param session the Hibernate session
     * @throws XWikiException when failing to switch wiki
     */
    public void setWiki(Session session) throws XWikiException
    {
        setWiki(session, this.wikis.getCurrentWikiId());
    }

    /**
     * Set the passed wiki in the passed session
     *
     * @param session the Hibernate session
     * @param wikiId the id of the wiki to switch to
     * @throws XWikiException when failing to switch wiki
     */
    public void setWiki(Session session, String wikiId) throws XWikiException
    {
        try {
            this.logger.debug("Set the right catalog in the session [{}]", wikiId);

            // Switch the database only if we did not switched on it last time
            if (wikiId != null) {
                String databaseName = getDatabaseFromWikiName(wikiId);
                String escapedDatabaseName = escapeDatabaseName(databaseName);

                DatabaseProduct product = getDatabaseProductName();
                if (DatabaseProduct.ORACLE == product) {
                    executeStatement("alter session set current_schema = " + escapedDatabaseName, session);
                } else if (DatabaseProduct.DERBY == product || DatabaseProduct.HSQLDB == product
                    || DatabaseProduct.DB2 == product || DatabaseProduct.H2 == product) {
                    executeStatement("SET SCHEMA " + escapedDatabaseName, session);
                } else if (DatabaseProduct.POSTGRESQL == product && isConfiguredInSchemaMode()) {
                    executeStatement("SET search_path TO " + escapedDatabaseName, session);
                } else {
                    session.doWork(connection -> {
                        String catalog = connection.getCatalog();
                        catalog = (catalog == null) ? null : catalog.replace('_', '-');
                        if (!databaseName.equals(catalog)) {
                            connection.setCatalog(databaseName);
                        }
                    });
                }

                session.setProperty("xwiki.database", databaseName);
            }
        } catch (Exception e) {
            // close session with rollback to avoid further usage
            endTransaction(false);

            Object[] args = {wikiId};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE, "Exception while switching to wiki {0}", e,
                args);
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

        String contextWikiId = this.wikis.getCurrentWikiId();

        if (session != null) {
            String sessionDatabase = (String) session.getProperties().get("xwiki.database");
            String contextDatabase = getDatabaseFromWikiName(contextWikiId);

            // The current context is trying to manipulate a database different from the one in the current session
            if (!Objects.equals(sessionDatabase, contextDatabase)) {
                Object[] args = {contextWikiId};
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE,
                    "Cannot switch to database {0} in an existing session", null, args);
            }

            this.logger.debug("Taking session from context [{}]", session);
            this.logger.debug("Taking transaction from context [{}]", transaction);

            return false;
        }

        // We should not try to access the schema/database which is not a registered wiki
        try {
            if (!this.wikis.isMainWiki(contextWikiId) && this.wikis.getById(contextWikiId) == null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                    "No wiki with id [" + contextWikiId + "] could be found");
            }
        } catch (WikiManagerException e) {
            throw new XWikiException("Failed to load the wiki descriptor", e);
        }

        // Makes sure the database is initialized/migrated
        // Doing it before creating a new session because:
        // * we don't need one for that
        // * it seems MySQL does not like having changes in the tables structure during a session (even if those change
        // are not done as part of this session, just at the same time)
        try {
            getDataMigrationManager().checkDatabase();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE, "Exception while initializing the database",
                e);
        }

        // session is obviously null here
        this.logger.debug("Trying to get session from pool");
        if (sfactory == null) {
            session = getSessionFactory().openSession();
        } else {
            session = sfactory.openSession();
        }

        this.logger.debug("Taken session from pool [{}]", session);

        // Put back legacy feature to the Hibernate session
        if (session instanceof SessionImplementor) {
            session = new LegacySessionImplementor((SessionImplementor) session, this.loggerConfiguration);
        }

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
     * @since 11.5RC1
     */
    public SessionFactory getSessionFactory()
    {
        this.lock.readLock().lock();

        try {
            return this.sessionFactory;
        } finally {
            this.lock.readLock().unlock();
        }
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
                    if (transaction.getRollbackOnly()) {
                        throw new HibernateException(
                            "The transaction [" + transaction + "] has been unexpectedly marked as rollback only");
                    }

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
        disposeSessionFactory();
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
    private void executeStatement(final String sql, Session session)
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
     * @since 11.6RC1
     */
    public void updateDatabase(String wikiId)
    {
        MetadataBuilder metadataBuilder = this.metadataSources.getMetadataBuilder();

        // Associate the metadata with a specific wiki
        setWiki(metadataBuilder, wikiId);

        updateDatabase(metadataBuilder.build());

        // Workaround Hibernate bug which does not create the required sequence in some databases
        createSequenceIfMissing(wikiId);
    }

    private <T, E> T executeNative(String sqlString, Function<NativeQuery<E>, T> function)
    {
        try (Session session = getSessionFactory().openSession()) {
            NativeQuery<E> query = session.createNativeQuery(sqlString);

            return function.apply(query);
        }
    }

    private Iterable<SequenceInformation> getSchemaSequences(String schemaName) throws SQLException
    {
        try (SessionImplementor session = (SessionImplementor) getSessionFactory().openSession()) {
            JdbcConnectionAccess jdbcConnectionAccess = session.getJdbcConnectionAccess();

            try (Connection connection = jdbcConnectionAccess.obtainConnection()) {
                JdbcEnvironment jdbcEnvironment = this.standardRegistry.getService(JdbcEnvironment.class);

                ExtractionContext extractionContext = new ExtractionContext.EmptyExtractionContext()
                {
                    @Override
                    public Connection getJdbcConnection()
                    {
                        return connection;
                    }

                    @Override
                    public JdbcEnvironment getJdbcEnvironment()
                    {
                        return jdbcEnvironment;
                    }

                    @Override
                    public Identifier getDefaultSchema()
                    {
                        return Identifier.toIdentifier(schemaName);
                    }
                };

                return getDialect().getSequenceInformationExtractor().extractMetadata(extractionContext);
            }
        }
    }

    private List<String> getOracleSequences(String schemaName)
    {
        return this.<List<String>, String>executeNative(String
            .format("select SEQUENCE_NAME from all_sequences where SEQUENCE_OWNER = '%s'", schemaName.toUpperCase()),
            query -> query.getResultList());
    }

    /**
     * In the Hibernate mapping file for XWiki we use a "native" generator for some tables (deleted document and deleted
     * attachments for example - The reason we use generated ids and not custom computed ones is because we don't need
     * to address rows from these tables). For a lot of databases engines the Dialect uses an Identity Generator (when
     * the DB supports it). PostgreSQL and Oracle don't support it and Hibernate defaults to a Sequence Generator which
     * uses a sequence named "hibernate_sequence" by default. Hibernate will normally create such a sequence
     * automatically when updating the schema (see #getSchemaUpdateScript) but because of
     * https://hibernate.atlassian.net/browse/HHH-13464 if the sequence exist on any schema it won't create it.
     * 
     * @param wikiId the identifier of the wiki
     */
    private void createSequenceIfMissing(String wikiId)
    {
        // There's no issue with catalog based databases, only with schemas.
        if (!isCatalog() && getDialect().getNativeIdentifierGeneratorStrategy().equals("sequence")) {
            String schemaName = getDatabaseFromWikiName(wikiId);

            boolean ignoreError = false;

            // Check if the sequence already exist
            try {
                DatabaseProduct product = getDatabaseProductName();
                if (product == DatabaseProduct.ORACLE) {
                    // Oracle does not provide any information about the sequence schema when using standard Hibernate
                    // API
                    List<String> sequences = getOracleSequences(schemaName);

                    if (sequences.contains("HIBERNATE_SEQUENCE")) {
                        // The sequence already exist, no need to create it
                        return;
                    }
                } else {
                    Iterable<SequenceInformation> sequences = getSchemaSequences(schemaName);

                    for (SequenceInformation sequence : sequences) {
                        QualifiedSequenceName sequenceName = sequence.getSequenceName();
                        if (sequenceName.getSequenceName().getCanonicalName().equalsIgnoreCase("hibernate_sequence")
                            && sequenceName.getSchemaName() != null
                            && sequenceName.getSchemaName().getCanonicalName().equals(schemaName)) {
                            // The sequence already exist, no need to create it
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                this.logger.warn(
                    "Failed to get the sequences of the schema [{}] ({}). Trying to create hibernate_sequence anyway.",
                    schemaName, ExceptionUtils.getRootCauseMessage(e));

                // Ignore errors in the log during the creation of the sequence since we know it can fail and we
                // don't want to show false positives to the user.
                ignoreError = true;
            }

            // Try to create the Hibernate sequence
            try (Session session = getSessionFactory().openSession()) {
                Transaction transaction = session.beginTransaction();
                session.createNativeQuery(String.format("create sequence %s.hibernate_sequence", schemaName))
                    .executeUpdate();
                transaction.commit();
            } catch (Exception e) {
                if (!ignoreError) {
                    this.logger.error("Failed to create the hibernate_sequence", e);
                }
            }
        }
    }

    /**
     * Automatically update the current database schema to contains what's defined in standard metadata.
     * 
     * @param metadata the metadata we want the current database to follow
     * @since 11.6RC1
     */
    public void updateDatabase(Metadata metadata)
    {
        SchemaUpdate updater = new SchemaUpdate();
        updater.execute(EnumSet.of(TargetType.DATABASE), metadata);

        List<Exception> exceptions = updater.getExceptions();

        if (exceptions.isEmpty()) {
            return;
        }

        // Print the errors
        for (Exception exception : exceptions) {
            this.logger.error(exception.getMessage(), exception);
        }

        throw new HibernateException("Failed to update the database. See the log for all errors", exceptions.get(0));
    }

    /**
     * Allows to update the schema to match the Hibernate mapping
     *
     * @param wikiId the identifier of the wiki to update
     * @param force defines whether or not to force the update despite the xwiki.cfg settings
     * @since 11.6RC1
     */
    public synchronized void updateDatabase(String wikiId, boolean force)
    {
        // We don't update the database if the XWiki hibernate config parameter says not to update
        if (!force && !this.hibernateConfiguration.isUpdateSchema()) {
            this.logger.debug("Database update deactivated for wiki [{}]", wikiId);
            return;
        }

        this.logger.info("Updating database for wiki [{}]...", wikiId);

        try {
            updateDatabase(wikiId);
        } finally {
            this.logger.info("Database update for wiki [{}] done", wikiId);
        }
    }

    /**
     * @since 11.6RC1
     */
    public String getConfiguredColumnName(PersistentClass persistentClass, String propertyName)
    {
        String columnName = null;

        if (propertyName != null) {
            // FIXME: remove when https://hibernate.atlassian.net/browse/HHH-14627
            // (org.hibernate.mapping.PersistentClass#getProperty does not support composite ids) is fixed
            KeyValue identifier = persistentClass.getIdentifier();
            if (identifier instanceof org.hibernate.mapping.Component) {
                Iterator<Property> it = ((org.hibernate.mapping.Component) identifier).getPropertyIterator();

                while (it.hasNext()) {
                    Property property = it.next();

                    if (property.getName().equals(propertyName)) {
                        return getConfiguredColumnName(property);
                    }
                }
            }

            return getConfiguredColumnName(persistentClass.getProperty(propertyName));
        }

        return columnName;
    }

    /**
     * @since 13.4
     * @since 12.10.8
     */
    public String getConfiguredColumnName(Property property)
    {
        Column column = (Column) property.getColumnIterator().next();

        return getConfiguredColumnName(column);
    }

    /**
     * @since 13.2RC1
     */
    public String getConfiguredColumnName(Column column)
    {
        String columnName = column.getName();

        if (getDatabaseProductName() == DatabaseProduct.POSTGRESQL) {
            columnName = columnName.toLowerCase();
        }

        return columnName;
    }

    /**
     * @since 11.6RC1
     */
    public String getConfiguredTableName(PersistentClass persistentClass)
    {
        return getConfiguredTableName(persistentClass.getTable());
    }

    /**
     * @since 13.2
     */
    public String getConfiguredTableName(Table table)
    {
        String tableName = table.getName();
        // HSQLDB and Oracle needs to use uppercase table name to retrieve the value.
        if (getDatabaseProductName() == DatabaseProduct.HSQLDB || getDatabaseProductName() == DatabaseProduct.ORACLE) {
            tableName = tableName.toUpperCase();
        }

        return tableName;
    }

    /**
     * Execute the passed function with the {@link DatabaseMetaData} {@link ResultSet} corresponding to the passed
     * entity and property.
     * 
     * @param <R> the type of the return
     * @param entityType the mapping
     * @param propertyName the name of the property in the class (can be null to get a {@link ResultSet} of the table)
     * @param def the default value to return
     * @param function the function to execute
     * @return the result of the function execution
     * @since 11.6RC1
     */
    public <R> R metadataTableOrColumn(Class<?> entityType, String propertyName, R def, ResultSetFunction<R> function)
    {
        // retrieve the database name from the context
        final String databaseName = getDatabaseFromWikiName();

        PersistentClass persistentClass = getConfigurationMetadata().getEntityBinding(entityType.getName());

        final String tableName = getConfiguredTableName(persistentClass);

        final String columnName = getConfiguredColumnName(persistentClass, propertyName);

        return metadata(def, (databaseMetaData, session) -> {
            ResultSet resultSet;

            String name = databaseName;

            if (columnName != null) {
                if (isCatalog()) {
                    resultSet = databaseMetaData.getColumns(name, null, tableName, columnName);
                } else {
                    resultSet = databaseMetaData.getColumns(null, name, tableName, columnName);
                }
            } else {
                if (isCatalog()) {
                    resultSet = databaseMetaData.getTables(name, null, tableName, null);
                } else {
                    resultSet = databaseMetaData.getTables(null, name, tableName, null);
                }
            }

            try {
                // next will return false if the resultSet is empty.
                if (resultSet.next()) {
                    return function.apply(resultSet);
                }
            } finally {
                resultSet.close();
            }

            return def;
        });
    }

    /**
     * Execute the passed function with the {@link DatabaseMetaData}.
     * 
     * @param <R> the type of the return
     * @param def the default value to return
     * @param function the function to execute
     * @return the result of the function execution
     * @since 11.6RC1
     */
    public <R> R metadata(R def, DatabaseMetaDataFunction<R> function)
    {
        R result = def;

        try (SessionImplementor session = (SessionImplementor) getSessionFactory().openSession()) {
            JdbcConnectionAccess jdbcConnectionAccess = session.getJdbcConnectionAccess();

            try (Connection connection = jdbcConnectionAccess.obtainConnection()) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();

                return function.apply(databaseMetaData, session);
            }
        } catch (SQLException e) {
            this.logger.error("Error while extracting metadata", e);
        }

        return result;
    }

    public int getLimitSize(Class<?> entityType, String propertyName)
    {
        int result = metadataTableOrColumn(entityType, propertyName, -1, resultSet -> resultSet.getInt("COLUMN_SIZE"));

        if (result == -1) {
            PersistentClass persistentClass = getConfigurationMetadata().getEntityBinding(entityType.getName());
            Column column2 = (Column) persistentClass.getProperty(propertyName).getColumnIterator().next();
            result = column2.getLength();
            this.logger.warn(
                "Error while getting the size limit for entity [{}] and propertyName [{}]. "
                    + "The length value set by hibernate [{}] will be used.",
                entityType.getName(), propertyName, result);
        }

        return result;
    }

    /**
     * @since 11.5RC1
     */
    public boolean tableExists(Class<?> entityClass)
    {
        return metadataTableOrColumn(entityClass, null, false, resultSet -> true);
    }

    /**
     * @since 11.6RC1
     */
    public boolean isWikiDatabaseExist(String wikiName)
    {
        final String databaseName = getDatabaseFromWikiName(wikiName);

        if (isCatalog()) {
            return isCatalogExist(databaseName);
        } else {
            return isSchemaExist(databaseName);
        }
    }

    /**
     * @since 11.6RC1
     */
    public boolean isCatalogExist(String catalogName)
    {
        return metadata(false, (metadata, session) -> {
            try (ResultSet catalogs = metadata.getCatalogs()) {
                while (catalogs.next()) {
                    if (catalogName.equalsIgnoreCase(catalogs.getString("TABLE_CAT"))) {
                        return true;
                    }
                }
            }

            return false;
        });
    }

    /**
     * @since 11.6RC1
     */
    public boolean isSchemaExist(String schemaName)
    {
        return metadata(false, (metadata, session) -> {
            try (ResultSet schemas = this.configurationCatalog != null
                ? metadata.getSchemas(this.configurationCatalog, null) : metadata.getSchemas()) {
                while (schemas.next()) {
                    if (schemaName.equalsIgnoreCase(schemas.getString("TABLE_SCHEM"))) {
                        return true;
                    }
                }
            }

            return false;
        });
    }
}
