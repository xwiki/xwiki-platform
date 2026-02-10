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
package org.xwiki.store.hibernate.internal.datasource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.store.hibernate.HibernateDataSourceProvider;
import org.xwiki.store.hibernate.internal.HibernateCfgXmlLoader;

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
public class DBCPHibernateDataSourceProvider implements HibernateDataSourceProvider, Disposable
{
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
    private HibernateCfgXmlLoader cfgXmlLoader;

    private BasicDataSource dataSource;

    private synchronized void maybeInitialize() throws SQLException
    {
        // Don't initialize more than once (in case multiple SessionFactory instances are created).
        // We check this again in the synchronized method here to avoid race conditions.
        if (this.dataSource != null) {
            return;
        }

        URL configurationURL = this.cfgXmlLoader.getConfigurationURL();
        if (configurationURL == null) {
            String errorMessage = "No hibernate configuration found, cannot create datasource";
            this.logger.error(errorMessage);
            throw new SQLException(errorMessage);
        }

        try (var bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build()) {
            LoadedConfig loadedConfig =
                this.cfgXmlLoader.loadConfig(bootstrapServiceRegistry, configurationURL);

            @SuppressWarnings("unchecked")
            Map<String, String> values = loadedConfig.getConfigurationValues();

            this.dataSource = createBasicDataSource(values);

            // The BasicDataSource has lazy initialization.
            // Borrowing a connection will start the DataSource and make sure it is configured correctly.
            this.dataSource.getConnection().close();
        } catch (Exception e) {
            if (this.dataSource != null) {
                try {
                    this.dataSource.close();
                } catch (Exception ignored) {
                    // Ignore.
                }
                this.dataSource = null;
            }

            if (e instanceof SQLException sqlException) {
                throw sqlException;
            }

            throw new SQLException("Failed to create shared datasource", e);
        }
    }

    @Override
    public DataSource getDataSource() throws SQLException
    {
        // First, check without any lock to avoid calling the synchronized method too frequently.
        if (this.dataSource == null) {
            // Initialize the data source lazily only when it is called to avoid too early initialization, e.g., in
            // unit tests that include all components.
            maybeInitialize();
        }

        return this.dataSource;
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
    }

    private BasicDataSource createBasicDataSource(Map<String, String> props) throws SQLException
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

    private void applyDriverClass(Map<String, String> props, Properties dbcpProperties)
    {
        String jdbcDriverClass = props.get(AvailableSettings.DRIVER);
        // Some drivers register themselves automatically using the Service Loader mechanism and thus we don't need
        // to set the "driverClassName" property.
        if (jdbcDriverClass != null) {
            dbcpProperties.put("driverClassName", jdbcDriverClass);
        }
    }

    private void applyJdbcURL(Map<String, String> props, Properties dbcpProperties)
    {
        String jdbcUrl = System.getProperty(AvailableSettings.URL);
        if (jdbcUrl == null) {
            jdbcUrl = props.get(AvailableSettings.URL);
        }
        dbcpProperties.put("url", jdbcUrl);
    }

    private void applyCredentials(Map<String, String> props, Properties dbcpProperties)
    {
        // Username / password. Only put username and password if they're not null. This allows
        // external authentication support (OS authenticated). It'll thus work if the hibernate
        // config does not specify a username and/or password.
        String username = props.get(AvailableSettings.USER);
        if (username != null) {
            dbcpProperties.put("username", username);
        }

        String password = props.get(AvailableSettings.PASS);
        if (password != null) {
            dbcpProperties.put("password", password);
        }
    }

    private void applyIsolation(Map<String, String> props, Properties dbcpProperties)
    {
        String isolationLevel = props.get(AvailableSettings.ISOLATION);
        if (StringUtils.isNotBlank(isolationLevel)) {
            dbcpProperties.put("defaultTransactionIsolation", isolationLevel);
        }
    }

    private void applyAutoCommit(Map<String, String> props, Properties dbcpProperties)
    {
        // Turn off autocommit (unless autocommit property is set)
        // Note that this property can be overwritten below if the DBCP "defaultAutoCommit" property is defined.
        String autocommit = props.get(AUTOCOMMIT);
        if (StringUtils.isNotBlank(autocommit)) {
            dbcpProperties.put(PROPERTY_DEFAULT_AUTOCOMMIT, autocommit);
        } else {
            dbcpProperties.put(PROPERTY_DEFAULT_AUTOCOMMIT, String.valueOf(Boolean.FALSE));
        }
    }

    private void applyPoolSize(Map<String, String> props, Properties dbcpProperties)
    {
        String poolSize = props.get(AvailableSettings.POOL_SIZE);
        if (StringUtils.isNotBlank(poolSize) && Integer.parseInt(poolSize) > 0) {
            dbcpProperties.put(PROPERTY_MAX_TOTAL, poolSize);
        }
    }

    private void applyConnectionProperties(Map<String, String> props, Properties dbcpProperties)
    {
        // Copy all "driver" properties into "connectionProperties"
        Properties driverProps = ConnectionProviderInitiator.getConnectionProperties(props);
        if (!driverProps.isEmpty()) {
            StringBuilder connectionProperties = new StringBuilder();
            for (Iterator<Object> iter = driverProps.keySet().iterator(); iter.hasNext();) {
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

    private void applyDBCPProperties(Map<String, String> props, Properties dbcpProperties)
    {
        // Copy all DBCP properties removing the prefix
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(DBCP_PREFIX)) {
                String property = key.substring(DBCP_PREFIX.length());
                String value = entry.getValue();

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
