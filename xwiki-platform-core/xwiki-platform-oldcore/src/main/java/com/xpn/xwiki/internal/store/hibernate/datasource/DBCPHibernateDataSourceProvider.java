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
package com.xpn.xwiki.internal.store.hibernate.datasource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;

/**
 * Creates a single shared {@link BasicDataSource} based on the same properties as the standard
 * {@code com.xpn.xwiki.store.DBCPConnectionProvider}.
 * <p>
 * This is used to ensure that multiple Hibernate {@code SessionFactory} instances can share the same pool.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component
@Singleton
public class DBCPHibernateDataSourceProvider
    implements HibernateDataSourceProvider, Initializable, Disposable
{
    private static final String PROPERTY_PERMANENTDIRECTORY = "environment.permanentDirectory";

    private static final String PROPERTY_TIMEZONE_VARIABLE = "${timezone}";

    private static final String DBCP_PREFIX = "hibernate.dbcp.";

    private static final String COMPATIBILITY_PS_MAXACTIVE = "ps.maxActive";

    private static final String COMPATIBILITY_MAXACTIVE = "maxActive";

    private static final String COMPATIBILITY_MAXWAIT = "maxWait";

    private static final String AUTOCOMMIT = "hibernate.connection.autocommit";

    private static final String PROPERTY_DEFAULT_AUTOCOMMIT = "defaultAutoCommit";

    private static final String PROPERTY_MAX_TOTAL = "maxTotal";

    @Inject
    private Logger logger;

    @Inject
    private HibernateConfiguration hibernateConfiguration;

    @Inject
    private Environment environment;

    private BasicDataSource dataSource;

    private BootstrapServiceRegistry bootstrapServiceRegistry;

    @Override
    public void initialize() throws InitializationException
    {
        URL configurationURL = getHibernateConfigurationURL();
        if (configurationURL == null) {
            this.logger.debug("No hibernate configuration found, cannot create shared datasource");
            return;
        }

        this.bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();

        ConfigLoader configLoader = new ConfigLoader(this.bootstrapServiceRegistry);
        LoadedConfig loadedConfig = configLoader.loadConfigXmlUrl(configurationURL);

        Map values = loadedConfig.getConfigurationValues();
        replaceVariables(values);

        // Only create the shared pool if XWiki's DBCP provider is configured.
        // Otherwise we keep empty and let callers fallback to standard Hibernate bootstrap.
        Object provider = values.get("hibernate.connection.provider_class");

        if (provider == null || !StringUtils.contains(String.valueOf(provider), "DBCPConnectionProvider")) {
            this.logger.debug("Hibernate is not configured to use XWiki DBCPConnectionProvider (found [{}]); "
                + "shared pool will not be created", provider);
            return;
        }

        try {
            this.dataSource = createBasicDataSource(values);

            // Force early init (mirrors DBCPConnectionProvider behavior)
            this.dataSource.getConnection().close();
        } catch (Exception e) {
            throw new InitializationException("Failed to create the shared DBCP datasource", e);
        }
    }

    @Override
    public Optional<DataSource> getDataSource()
    {
        return Optional.ofNullable(this.dataSource);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.dataSource != null) {
            try {
                this.dataSource.close();
            } catch (Exception e) {
                this.logger.debug("Failed to close shared datasource", e);
            }
            this.dataSource = null;
        }

        if (this.bootstrapServiceRegistry != null) {
            this.bootstrapServiceRegistry.close();
            this.bootstrapServiceRegistry = null;
        }
    }

    private URL getHibernateConfigurationURL()
    {
        String path = this.hibernateConfiguration.getPath();

        if (StringUtils.isEmpty(path)) {
            return null;
        }

        try {
            URL res = this.environment.getResource(path);
            if (res != null) {
                return res;
            }
        } catch (Exception e) {
            // ignore
        }

        return Thread.currentThread().getContextClassLoader().getResource(path);
    }

    private void replaceVariables(Map values)
    {
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

    private BasicDataSource createBasicDataSource(Map props) throws Exception
    {
        Properties dbcpProperties = new Properties();

        applyDriverClass(props, dbcpProperties);
        applyJdbcURL(props, dbcpProperties);
        applyCredentials(props, dbcpProperties);
        applyIsolation(props, dbcpProperties);
        applyAutoCommit(props, dbcpProperties);
        applyPoolSize(props, dbcpProperties);
        applyConnectionProperties(props, dbcpProperties);
        applyDBCPProperties(props, dbcpProperties);

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Creating a shared DBCP BasicDataSource with the following properties:");
            StringWriter sw = new StringWriter();
            dbcpProperties.list(new PrintWriter(sw, true));
            this.logger.debug(sw.toString());
        }

        return BasicDataSourceFactory.createDataSource(dbcpProperties);
    }

    private void applyDriverClass(Map props, Properties dbcpProperties)
    {
        String jdbcDriverClass = (String) props.get(org.hibernate.cfg.Environment.DRIVER);
        // Some drivers register themselves automatically using the Service Loader mechanism and thus we don't need
        // to set the "driverClassName" property.
        if (jdbcDriverClass != null) {
            dbcpProperties.put("driverClassName", jdbcDriverClass);
        }
    }

    private void applyJdbcURL(Map props, Properties dbcpProperties)
    {
        String jdbcUrl = System.getProperty(org.hibernate.cfg.Environment.URL);
        if (jdbcUrl == null) {
            jdbcUrl = (String) props.get(org.hibernate.cfg.Environment.URL);
        }
        dbcpProperties.put("url", jdbcUrl);
    }

    private void applyCredentials(Map props, Properties dbcpProperties)
    {
        String username = (String) props.get(org.hibernate.cfg.Environment.USER);
        if (username != null) {
            dbcpProperties.put("username", username);
        }

        String password = (String) props.get(org.hibernate.cfg.Environment.PASS);
        if (password != null) {
            dbcpProperties.put("password", password);
        }
    }

    private void applyIsolation(Map props, Properties dbcpProperties)
    {
        String isolationLevel = (String) props.get(org.hibernate.cfg.Environment.ISOLATION);
        if (StringUtils.isNotBlank(isolationLevel)) {
            dbcpProperties.put("defaultTransactionIsolation", isolationLevel);
        }
    }

    private void applyAutoCommit(Map props, Properties dbcpProperties)
    {
        // Turn off autocommit (unless autocommit property is set)
        // Note that this property can be overwritten below if the DBCP "defaultAutoCommit" property is defined.
        String autocommit = (String) props.get(AUTOCOMMIT);
        if (StringUtils.isNotBlank(autocommit)) {
            dbcpProperties.put(PROPERTY_DEFAULT_AUTOCOMMIT, autocommit);
        } else {
            dbcpProperties.put(PROPERTY_DEFAULT_AUTOCOMMIT, String.valueOf(Boolean.FALSE));
        }
    }

    private void applyPoolSize(Map props, Properties dbcpProperties)
    {
        String poolSize = (String) props.get(org.hibernate.cfg.Environment.POOL_SIZE);
        if (StringUtils.isNotBlank(poolSize) && Integer.parseInt(poolSize) > 0) {
            dbcpProperties.put(PROPERTY_MAX_TOTAL, poolSize);
        }
    }

    private void applyConnectionProperties(Map props, Properties dbcpProperties)
    {
        Properties driverProps = ConnectionProviderInitiator.getConnectionProperties(props);
        if (!driverProps.isEmpty()) {
            StringBuilder connectionProperties = new StringBuilder();
            for (Iterator iter = driverProps.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String value = driverProps.getProperty(key);
                connectionProperties.append(key).append('=').append(value);
                if (iter.hasNext()) {
                    connectionProperties.append(';');
                }
            }
            dbcpProperties.put("connectionProperties", connectionProperties.toString());
        }
    }

    private void applyDBCPProperties(Map props, Properties dbcpProperties)
    {
        for (Object element : props.keySet()) {
            String key = String.valueOf(element);
            if (key.startsWith(DBCP_PREFIX)) {
                String property = key.substring(DBCP_PREFIX.length());
                String value = (String) props.get(key);

                applyDBCPProperty(dbcpProperties, property, value);
            }
        }
    }

    private void applyDBCPProperty(Properties dbcpProperties, String property, String value)
    {
        // Handle backward compatibility
        switch (property) {
            case COMPATIBILITY_PS_MAXACTIVE:
                dbcpProperties.put("poolPreparedStatements", String.valueOf(Boolean.TRUE));
                dbcpProperties.put("maxOpenPreparedStatements", value);
                break;
            case COMPATIBILITY_MAXACTIVE:
                dbcpProperties.put(PROPERTY_MAX_TOTAL, value);
                break;
            case COMPATIBILITY_MAXWAIT:
                dbcpProperties.put("maxWaitMillis", value);
                break;
            default:
                dbcpProperties.put(property, value);
        }
    }
}

