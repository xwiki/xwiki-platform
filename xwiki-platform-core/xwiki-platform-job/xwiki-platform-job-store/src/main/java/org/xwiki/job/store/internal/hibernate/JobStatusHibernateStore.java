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
package org.xwiki.job.store.internal.hibernate;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.datasource.HibernateDataSourceProvider;
import com.xpn.xwiki.store.DatabaseProduct;

/**
 * Minimal Hibernate bootstrap dedicated to job status / logs.
 * <p>
 * It builds its own {@link SessionFactory} with only the {@code jobstatus*.hbm.xml} mappings and ensures that the
 * required tables exist as early as possible (before the main XWiki Hibernate store is initialized).
 * <p>
 * It also explicitly targets the main wiki database/schema (main wiki id is always {@code xwiki}).
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@Component(roles = JobStatusHibernateStore.class)
@Singleton
public class JobStatusHibernateStore implements Initializable, Disposable
{
    private static final String PROPERTY_PERMANENTDIRECTORY = "environment.permanentDirectory";

    private static final String PROPERTY_TIMEZONE_VARIABLE = "${timezone}";

    private static final String VIRTUAL_MODE_SCHEMA = "schema";

    private static final String PROPERTY_VIRTUAL_MODE = "xwiki.virtual_mode";

    private static final String MAIN_WIKI_ID = "xwiki";

    private static final String HBM_JOBSTATUS_DEFAULT = "jobstatus.hbm.xml";

    private static final String HBM_JOBSTATUS_ORACLE = "jobstatus.oracle.hbm.xml";

    private static final String UPDATE_SCHEMA_FAILED_MESSAGE = "Failed to update job status database schema";

    @Inject
    private Logger logger;

    @Inject
    private HibernateConfiguration hibernateConfiguration;

    @Inject
    private Environment environment;

    @Inject
    private HibernateDataSourceProvider dataSourceProvider;

    private BootstrapServiceRegistry bootstrapServiceRegistry;

    private StandardServiceRegistry standardServiceRegistry;

    private SessionFactory sessionFactory;

    private Dialect dialect;

    private DatabaseProduct databaseProduct;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            buildSessionFactory();
            updateDatabase();
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize JobStatus Hibernate store", e);
        }
    }

    /**
     * @return the dedicated Hibernate {@link SessionFactory} for job status / logs
     */
    public SessionFactory getSessionFactory()
    {
        return this.sessionFactory;
    }

    /**
     * @return the configured Hibernate {@link Dialect}
     */
    public Dialect getDialect()
    {
        return this.dialect;
    }

    /**
     * @return the database product used by the configured connection
     */
    public DatabaseProduct getDatabaseProduct()
    {
        return this.databaseProduct;
    }

    /**
     * @return the explicit database/schema name for the main wiki
     */
    public String getMainWikiDatabaseName()
    {
        String database = this.hibernateConfiguration.getDB();
        if (database == null) {
            database = getDefaultMainWikiDatabaseName();
        }

        database = this.hibernateConfiguration.getDBPrefix() + database;

        // Minus (-) is not supported by many databases
        database = database.replace('-', '_');

        // Some databases are case sensitive when quoting identifiers (and we quote them below).
        // Align with the main XWiki Hibernate adapters behavior.
        if (this.databaseProduct == DatabaseProduct.HSQLDB || this.databaseProduct == DatabaseProduct.H2) {
            database = database.toUpperCase();
        }

        return database;
    }

    private String getDefaultMainWikiDatabaseName()
    {
        // Keep the logic aligned with org.xwiki.store.hibernate.AbstractHibernateAdapter#getDatabaseFromWikiName().
        // For the main wiki, some databases rely on a default schema.
        if (this.databaseProduct == DatabaseProduct.HSQLDB || this.databaseProduct == DatabaseProduct.H2) {
            return "PUBLIC";
        }

        if (this.databaseProduct == DatabaseProduct.DERBY) {
            return "APP";
        }

        if (this.databaseProduct == DatabaseProduct.POSTGRESQL && isSchemaMode()) {
            return "public";
        }

        return MAIN_WIKI_ID;
    }

    /**
     * @return true if configured in schema mode (e.g., PostgreSQL subwikis as schemas)
     */
    public boolean isSchemaMode()
    {
        // This is stored as a custom property in hibernate.cfg.xml.
        // Default to schema mode to match XWiki's adapter default.
        org.hibernate.engine.config.spi.ConfigurationService configurationService =
            this.standardServiceRegistry.getService(org.hibernate.engine.config.spi.ConfigurationService.class);

        Object virtualModeValue = configurationService.getSettings().get(PROPERTY_VIRTUAL_MODE);
        String virtualMode = virtualModeValue != null ? String.valueOf(virtualModeValue) : VIRTUAL_MODE_SCHEMA;

        return StringUtils.equals(virtualMode, VIRTUAL_MODE_SCHEMA);
    }

    /**
     * Explicitly switch the passed {@link Session} to the main wiki database/schema/catalog.
     *
     * @param session the Hibernate session
     */
    public void setMainWiki(Session session)
    {
        // Switch to the configured main wiki database/schema explicitly (no relying on defaults).
        String databaseName = getMainWikiDatabaseName();
        String escapedDatabaseName = escapeDatabaseName(databaseName);

        DatabaseProduct product = getDatabaseProduct();

        if (DatabaseProduct.ORACLE == product) {
            executeStatement("alter session set current_schema = " + escapedDatabaseName, session);
        } else if (DatabaseProduct.DERBY == product || DatabaseProduct.HSQLDB == product
            || DatabaseProduct.DB2 == product || DatabaseProduct.H2 == product) {
            executeStatement("SET SCHEMA " + escapedDatabaseName, session);
        } else if (DatabaseProduct.POSTGRESQL == product && isSchemaMode()) {
            executeStatement("SET search_path TO " + escapedDatabaseName, session);
        } else {
            session.doWork(connection -> {
                // Catalog switching (e.g., MySQL)
                String catalog = connection.getCatalog();
                catalog = (catalog == null) ? null : catalog.replace('_', '-');
                if (!databaseName.equals(catalog)) {
                    connection.setCatalog(databaseName);
                }
            });
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.sessionFactory != null) {
            this.sessionFactory.close();
            this.sessionFactory = null;
        }

        if (this.standardServiceRegistry != null) {
            this.standardServiceRegistry.close();
            this.standardServiceRegistry = null;
        }

        if (this.bootstrapServiceRegistry != null) {
            this.bootstrapServiceRegistry.close();
            this.bootstrapServiceRegistry = null;
        }
    }

    private void buildSessionFactory() throws Exception
    {
        URL configurationURL = getHibernateConfigurationURL();
        if (configurationURL == null) {
            throw new IllegalStateException("Failed to locate hibernate configuration file");
        }

        this.bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();

        ConfigLoader configLoader = new ConfigLoader(this.bootstrapServiceRegistry);
        LoadedConfig baseConfiguration = configLoader.loadConfigXmlUrl(configurationURL);
        replaceVariables(baseConfiguration);

        // Always reuse the shared pool when available.
        this.dataSourceProvider.getDataSource().ifPresent(dataSource -> {
            Map values = baseConfiguration.getConfigurationValues();
            values.put(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);
            values.remove("hibernate.connection.provider_class");
            values.remove("connection.provider_class");

            // Prevent Hibernate from calling DataSource#getConnection(username, password) which is not supported by
            // Apache DBCP's BasicDataSource (it throws UnsupportedOperationException).
            values.remove(org.hibernate.cfg.AvailableSettings.USER);
            values.remove(org.hibernate.cfg.AvailableSettings.PASS);
            values.remove("connection.username");
            values.remove("connection.password");
        });

        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder(this.bootstrapServiceRegistry);
        registryBuilder.configure(baseConfiguration);

        // Explicitly avoid creating/updating the main mappings; we add only jobstatus mapping resources below.
        this.standardServiceRegistry = registryBuilder.build();

        JdbcServices jdbcServices = this.standardServiceRegistry.getService(JdbcServices.class);
        this.dialect = jdbcServices.getDialect();

        this.databaseProduct = detectDatabaseProduct();

        MetadataSources metadataSources = new MetadataSources(this.standardServiceRegistry);
        metadataSources.addResource(selectJobStatusMapping());

        MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
        applyMainWikiDatabase(metadataBuilder);

        Metadata metadata = metadataBuilder.build();
        this.sessionFactory = metadata.getSessionFactoryBuilder().build();
    }

    private void updateDatabase()
    {
        MetadataSources metadataSources = new MetadataSources(this.standardServiceRegistry);
        metadataSources.addResource(selectJobStatusMapping());
        MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
        applyMainWikiDatabase(metadataBuilder);
        Metadata metadata = metadataBuilder.build();

        // Ensure statements are executed on the right schema/catalog.
        try (Session session = this.sessionFactory.openSession()) {
            setMainWiki(session);
        }

        SchemaUpdate updater = new SchemaUpdate();
        updater.execute(EnumSet.of(TargetType.DATABASE), metadata);

        if (!updater.getExceptions().isEmpty()) {
            for (Object exception : updater.getExceptions()) {
                if (exception instanceof Exception e) {
                    this.logger.error(e.getMessage(), e);
                } else {
                    this.logger.error("{}", exception);
                }
            }

            Object first = updater.getExceptions().get(0);
            if (first instanceof Throwable throwable) {
                throw new IllegalStateException(UPDATE_SCHEMA_FAILED_MESSAGE, throwable);
            }

            throw new IllegalStateException(UPDATE_SCHEMA_FAILED_MESSAGE);
        }
    }

    private void applyMainWikiDatabase(MetadataBuilder builder)
    {
        String databaseName = getMainWikiDatabaseName();
        if (this.dialect != null && this.dialect.canCreateCatalog()) {
            builder.applyImplicitCatalogName(databaseName);
        } else {
            builder.applyImplicitSchemaName(databaseName);
        }
    }

    private DatabaseProduct detectDatabaseProduct()
    {
        Optional<DataSource> ds = this.dataSourceProvider.getDataSource();
        if (ds.isPresent()) {
            try (Connection connection = ds.get().getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                return DatabaseProduct.toProduct(metaData.getDatabaseProductName());
            } catch (Exception e) {
                this.logger.debug("Failed to detect database product from datasource", e);
            }
        }

        // Fallback: try to deduce it from URL scheme
        org.hibernate.engine.config.spi.ConfigurationService configurationService =
            this.standardServiceRegistry.getService(org.hibernate.engine.config.spi.ConfigurationService.class);
        Map<?, ?> settings = configurationService.getSettings();
        Object url = settings.get("hibernate.connection.url");
        if (url == null) {
            url = settings.get("connection.url");
        }

        if (url != null) {
            return DatabaseProduct.toProduct(extractJDBCConnectionURLScheme(String.valueOf(url)));
        }

        return DatabaseProduct.UNKNOWN;
    }

    private String selectJobStatusMapping()
    {
        return this.databaseProduct == DatabaseProduct.ORACLE ? HBM_JOBSTATUS_ORACLE : HBM_JOBSTATUS_DEFAULT;
    }

    private String extractJDBCConnectionURLScheme(String connectionURL)
    {
        if (connectionURL == null) {
            return null;
        }

        int startIndex = connectionURL.indexOf(':');
        if (startIndex >= 0) {
            int endIndex = connectionURL.indexOf(':', startIndex + 1);
            if (endIndex > startIndex) {
                return connectionURL.substring(startIndex + 1, endIndex);
            }
        }

        return connectionURL;
    }

    private URL getHibernateConfigurationURL()
    {
        String path = this.hibernateConfiguration.getPath();

        if (StringUtils.isEmpty(path)) {
            return null;
        }

        File file = new File(path);
        try {
            if (file.exists()) {
                return file.toURI().toURL();
            }
        } catch (Exception e) {
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

        return Thread.currentThread().getContextClassLoader().getResource(path);
    }

    private void replaceVariables(LoadedConfig hibernateConfiguration)
    {
        Map values = hibernateConfiguration.getConfigurationValues();
        Object urlValue = values.get(org.hibernate.cfg.AvailableSettings.URL);
        if (urlValue instanceof String url) {
            String resolved = resolveURL(url);
            if (resolved != null) {
                values.put(org.hibernate.cfg.AvailableSettings.URL, resolved);
            }
        }
    }

    private String resolveURL(String url)
    {
        if (StringUtils.isNotEmpty(url) && url.matches(".*\\$\\{.*\\}.*")) {
            String newURL = StringUtils.replace(url, String.format("${%s}", PROPERTY_PERMANENTDIRECTORY),
                this.environment.getPermanentDirectory().getAbsolutePath());

            try {
                return StringUtils.replace(newURL, PROPERTY_TIMEZONE_VARIABLE,
                    URLEncoder.encode(TimeZone.getDefault().getID(), "UTF-8"));
            } catch (Exception e) {
                this.logger.debug("Failed to encode the current timezone id", e);
            }
        }

        return null;
    }

    private String escapeDatabaseName(String databaseName)
    {
        String closeQuote = String.valueOf(this.dialect.closeQuote());
        return this.dialect.openQuote() + databaseName.replace(closeQuote, closeQuote + closeQuote) + closeQuote;
    }

    private void executeStatement(String statement, Session session)
    {
        session.doWork(connection -> {
            try (java.sql.Statement st = connection.createStatement()) {
                st.execute(statement);
            }
        });
    }
}
