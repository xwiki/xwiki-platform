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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A connection provider that uses an Apache commons DBCP connection pool.
 * </p>
 * <p>
 * To use this connection provider set:<br>
 * <code>hibernate.connection.provider_class&nbsp;org.hibernate.connection.DBCPConnectionProvider</code>
 * </p>
 *
 * <pre>
 * Supported Hibernate properties:
 *   hibernate.connection.driver_class
 *   hibernate.connection.url
 *   hibernate.connection.username
 *   hibernate.connection.password
 *   hibernate.connection.isolation
 *   hibernate.connection.autocommit
 *   hibernate.connection.pool_size
 *   hibernate.connection (JDBC driver properties)
 * </pre>
 *
 * <br>
 * All DBCP properties are also supported by using the hibernate.dbcp prefix. A complete list can be found on the DBCP
 * configuration page: <a
 * href="http://jakarta.apache.org/commons/dbcp/configuration.html">http://jakarta.apache.org/commons
 * /dbcp/configuration.html</a>. <br>
 *
 * <pre>
 * Example:
 *   hibernate.connection.provider_class org.hibernate.connection.DBCPConnectionProvider
 *   hibernate.connection.driver_class org.hsqldb.jdbcDriver
 *   hibernate.connection.username sa
 *   hibernate.connection.password
 *   hibernate.connection.url jdbc:hsqldb:test
 *   hibernate.connection.pool_size 20
 *   hibernate.dbcp.initialSize 10
 *   hibernate.dbcp.maxWait 3000
 *   hibernate.dbcp.validationQuery select 1 from dual
 * </pre>
 * <p>
 * More information about configuring/using DBCP can be found on the <a
 * href="http://jakarta.apache.org/commons/dbcp/">DBCP website</a>. There you will also find the DBCP wiki, mailing
 * lists, issue tracking and other support facilities
 * </p>
 *
 * @see org.hibernate.connection.ConnectionProvider
 * @author Dirk Verbeeck
 */
public class DBCPConnectionProvider implements ConnectionProvider
{
    /**
     * Logger to use to log shutdown information (opposite of initialization).
     */
    private static final Logger SHUTDOWN_LOGGER = LoggerFactory.getLogger("org.xwiki.shutdown");

    private static final Logger LOGGER = LoggerFactory.getLogger(DBCPConnectionProvider.class);

    private static final String PREFIX = "hibernate.dbcp.";

    private BasicDataSource ds;

    // Old Environment property for backward-compatibility (property removed in Hibernate3)
    private static final String COMPATIBILITY_PS_MAXACTIVE = "ps.maxActive";

    // Properties removed from DBCP 2.0
    private static final String COMPATIBILITY_MAXACTIVE = "maxActive";
    private static final String COMPATIBILITY_MAXWAIT = "maxWait";

    // Property doesn't exists in Hibernate2
    private static final String AUTOCOMMIT = "hibernate.connection.autocommit";

    @Override
    public void configure(Properties props) throws HibernateException
    {
        try {
            LOGGER.debug("Configure DBCPConnectionProvider");

            // DBCP properties used to create the BasicDataSource
            Properties dbcpProperties = new Properties();

            // DriverClass & url
            String jdbcDriverClass = props.getProperty(Environment.DRIVER);
            dbcpProperties.put("driverClassName", jdbcDriverClass);

            String jdbcUrl = System.getProperty(Environment.URL);
            if (jdbcUrl == null) {
                jdbcUrl = props.getProperty(Environment.URL);
            }
            dbcpProperties.put("url", jdbcUrl);

            // Username / password. Only put username and password if they're not null. This allows
            // external authentication support (OS authenticated). It'll thus work if the hibernate
            // config does not specify a username and/or password.
            String username = props.getProperty(Environment.USER);
            if (username != null) {
                dbcpProperties.put("username", username);
            }
            String password = props.getProperty(Environment.PASS);
            if (password != null) {
                dbcpProperties.put("password", password);
            }

            // Isolation level
            String isolationLevel = props.getProperty(Environment.ISOLATION);
            if ((isolationLevel != null) && (isolationLevel.trim().length() > 0)) {
                dbcpProperties.put("defaultTransactionIsolation", isolationLevel);
            }

            // Turn off autocommit (unless autocommit property is set)
            // Note that this property will be overwritten below if the DBCP "defaultAutoCommit" property is defined.
            String autocommit = props.getProperty(AUTOCOMMIT);
            if ((autocommit != null) && (autocommit.trim().length() > 0)) {
                dbcpProperties.put("defaultAutoCommit", autocommit);
            } else {
                dbcpProperties.put("defaultAutoCommit", String.valueOf(Boolean.FALSE));
            }

            // Pool size
            String poolSize = props.getProperty(Environment.POOL_SIZE);
            if ((poolSize != null) && (poolSize.trim().length() > 0) && (Integer.parseInt(poolSize) > 0)) {
                dbcpProperties.put("maxTotal", poolSize);
            }

            // Copy all "driver" properties into "connectionProperties"
            Properties driverProps = ConnectionProviderFactory.getConnectionProperties(props);
            if (driverProps.size() > 0) {
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

            // Copy all DBCP properties removing the prefix
            for (Object element : props.keySet()) {
                String key = String.valueOf(element);
                if (key.startsWith(PREFIX)) {
                    String property = key.substring(PREFIX.length());
                    String value = props.getProperty(key);

                    // Handle backward compatibility
                    switch (property) {
                        case COMPATIBILITY_PS_MAXACTIVE:
                            dbcpProperties.put("poolPreparedStatements", String.valueOf(Boolean.TRUE));
                            dbcpProperties.put("maxOpenPreparedStatements", value);
                            break;
                        case COMPATIBILITY_MAXACTIVE:
                            dbcpProperties.put("maxTotal", value);
                            break;
                        case COMPATIBILITY_MAXWAIT:
                            dbcpProperties.put("maxWaitMillis", value);
                            break;
                        default:
                            dbcpProperties.put(property, value);
                    }
                }
            }

            // Some debug info
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating a DBCP BasicDataSource with the following DBCP factory properties:");
                StringWriter sw = new StringWriter();
                dbcpProperties.list(new PrintWriter(sw, true));
                LOGGER.debug(sw.toString());
            }

            // Let the factory create the pool
            this.ds = BasicDataSourceFactory.createDataSource(dbcpProperties);

            // The BasicDataSource has lazy initialization
            // borrowing a connection will start the DataSource
            // and make sure it is configured correctly.
            Connection conn = this.ds.getConnection();
            conn.close();

            // Log pool statistics before continuing.
            logStatistics();
        } catch (Exception e) {
            String message = "Could not create a DBCP pool. There is an error in the Hibernate configuration file, "
                + "please review it.";
            LOGGER.error(message, e);
            if (this.ds != null) {
                try {
                    this.ds.close();
                } catch (Exception e2) {
                    // ignore
                }
                this.ds = null;
            }
            throw new HibernateException(message, e);
        }
        LOGGER.debug("Configure DBCPConnectionProvider complete");
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        // Check if the database has not been already stopped to avoid NPE. This could also possibly happen if the init
        // has not been already executed and some code calls getConnection().
        if (this.ds == null) {
            throw new SQLException("Database Connection Pool has not been started or is already stopped!");
        }

        Connection conn = null;
        try {
            conn = this.ds.getConnection();
        } finally {
            logStatistics();
        }
        return conn;
    }

    @Override
    public void closeConnection(Connection conn) throws SQLException
    {
        try {
            conn.close();
        } finally {
            logStatistics();
        }
    }

    @Override
    public void close() throws HibernateException
    {
        SHUTDOWN_LOGGER.debug("Stopping Database Connection Pool...");
        logStatistics();
        try {
            if (this.ds != null) {
                this.ds.close();
                this.ds = null;
            } else {
                LOGGER.warn("Cannot close Database Connection Pool (not initialized)");
            }
        } catch (Exception e) {
            throw new HibernateException("Could not close DBCP pool", e);
        }
        SHUTDOWN_LOGGER.debug("Database Connection Pool has been stopped");
    }

    /**
     * Does this connection provider support aggressive release of JDBC connections and re-acquistion of those
     * connections (if need be) later?
     * <p>
     * This is used in conjunction with {@link org.hibernate.cfg.Environment#RELEASE_CONNECTIONS} to aggressively
     * release JDBC connections. However, the configured ConnectionProvider must support re-acquisition of the same
     * underlying connection for that semantic to work.
     * <p>
     * Typically, this is only true in managed environments where a container tracks connections by transaction or
     * thread.
     */
    @Override
    public boolean supportsAggressiveRelease()
    {
        return false;
    }

    protected void logStatistics()
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("active: [{}] (max: [{}]), idle: [{}] (max: [{}])", this.ds.getNumActive(),
                this.ds.getMaxTotal(), this.ds.getNumIdle(), this.ds.getMaxIdle());
        }
    }
}
