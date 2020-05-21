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
package org.xwiki.test.docker.internal.junit5.database;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.xwiki.test.docker.internal.junit5.AbstractContainerExecutor;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.text.StringUtils;

/**
 * Create and execute the Docker database container for the tests.
 *
 * @version $Id$
 * @since 10.9
 */
public class DatabaseContainerExecutor extends AbstractContainerExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseContainerExecutor.class);

    private static final String DBNAME = "xwiki";

    private static final String DBUSERNAME = DBNAME;

    private static final String DBPASSWORD = DBUSERNAME;

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @throws Exception if the container fails to start
     */
    public void start(TestConfiguration testConfiguration) throws Exception
    {
        switch (testConfiguration.getDatabase()) {
            case MYSQL:
                startMySQLContainer(testConfiguration);
                break;
            case MARIADB:
                startMariaDBContainer(testConfiguration);
                break;
            case POSTGRESQL:
                startPostgreSQLContainer(testConfiguration);
                break;
            case ORACLE:
                startOracleContainer(testConfiguration);
                break;
            case HSQLDB_EMBEDDED:
                // We don't need a Docker image/container since HSQLDB can work in embedded mode.
                // It's configured automatically in the custom XWiki WAR.
                // Thus, nothing to do here!
                testConfiguration.getDatabase().setIP("localhost");
                break;
            default:
                throw new RuntimeException(String.format("Database [%s] is not yet supported!",
                    testConfiguration.getDatabase()));
        }
    }

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     */
    public void stop(TestConfiguration testConfiguration)
    {
        // Note that we don't need to stop the container as this is taken care of by TestContainers
    }

    private void startMySQLContainer(TestConfiguration testConfiguration) throws Exception
    {
        // docker run --net=xwiki-nw --name mysql-xwiki -v /my/own/mysql:/var/lib/mysql
        //     -e MYSQL_ROOT_PASSWORD=xwiki -e MYSQL_USER=xwiki -e MYSQL_PASSWORD=xwiki
        //     -e MYSQL_DATABASE=xwiki -d mysql:5.7 --character-set-server=utf8 --collation-server=utf8_bin
        //     --explicit-defaults-for-timestamp=1
        JdbcDatabaseContainer databaseContainer;
        if (testConfiguration.getDatabaseTag() != null) {
            databaseContainer = new MySQLContainer<>(String.format("mysql:%s", testConfiguration.getDatabaseTag()));
        } else {
            databaseContainer = new MySQLContainer<>();
        }
        startMySQLContainer(databaseContainer, testConfiguration);
    }

    private void startMySQLContainer(JdbcDatabaseContainer databaseContainer, TestConfiguration testConfiguration)
        throws Exception
    {
        databaseContainer
            .withDatabaseName(DBNAME)
            .withUsername(DBUSERNAME)
            .withPassword(DBPASSWORD);

        mountDatabaseDataIfNeeded(databaseContainer, "./target/mysql", "/var/lib/mysql", testConfiguration);

        // Note: the "explicit-defaults-for-timestamp" parameter has been introduced in MySQL 5.6.6+ only and using it
        // in older versions make MySQL fail to start.
        Properties commands = new Properties();
        commands.setProperty("character-set-server", "utf8mb4");
        commands.setProperty("collation-server", "utf8mb4_bin");

        if (!isMySQL55x(testConfiguration)) {
            commands.setProperty("explicit-defaults-for-timestamp", "1");
        }
        // MySQL 8.x has changed the default authentication plugin value so we need to explicitly configure it to get
        // the native password mechanism.
        // The reason we don't include when the tag is null is because with the TC version we use, MySQLContainer
        // defaults to
        if (isMySQL8xPlus(testConfiguration)) {
            commands.setProperty("default-authentication-plugin", "mysql_native_password");
        }
        databaseContainer.withCommand(mergeCommands(commands, testConfiguration.getDatabaseCommands()));

        startDatabaseContainer(databaseContainer, 3306, testConfiguration);

        // Allow the XWiki user to create databases (ie create subwikis)
        grantMySQLPrivileges(databaseContainer);
    }

    private void grantMySQLPrivileges(JdbcDatabaseContainer databaseContainer) throws Exception
    {
        // Retry 3 times, as we're getting some flickering from time to time with the message:
        //   ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: YES)
        LOGGER.info("Setting MySQL permissions to create subwikis");
        for (int i = 0; i < 3; i++) {
            Container.ExecResult result = databaseContainer.execInContainer("mysql", "-u", "root", "-p" + DBPASSWORD,
                "-e", String.format("grant all privileges on *.* to '%s'@'%%' identified by '%s'", DBUSERNAME, DBNAME));
            if (result.getExitCode() == 0) {
                break;
            } else {
                String errorMessage = result.getStderr().isEmpty() ? result.getStdout() : result.getStderr();
                if (i == 2) {
                    throw new RuntimeException(String.format("Failed to grant all privileges to user [%s] on MySQL "
                        + "with return code [%d] and console logs [%s]", DBUSERNAME, result.getExitCode(),
                        errorMessage));
                } else {
                    LOGGER.info("Failed to set MySQL permissions, retrying ({}/2)... Error: [{}]", i + 1, errorMessage);
                    Thread.sleep(1000L);
                }
            }
        }
    }

    private boolean isMySQL55x(TestConfiguration testConfiguration)
    {
        return testConfiguration.getDatabaseTag() != null && testConfiguration.getDatabaseTag().startsWith("5.5");
    }

    private boolean isMySQL8xPlus(TestConfiguration testConfiguration)
    {
        return (testConfiguration.getDatabaseTag() != null && extractMajor(testConfiguration.getDatabaseTag()) >= 8)
            || (extractMajor(MySQLContainer.DEFAULT_TAG) >= 8 && testConfiguration.getDatabaseTag() == null);
    }

    private int extractMajor(String version)
    {
        return Integer.valueOf(StringUtils.substringBefore(version, "."));
    }

    private void startMariaDBContainer(TestConfiguration testConfiguration) throws Exception
    {
        JdbcDatabaseContainer databaseContainer;
        if (testConfiguration.getDatabaseTag() != null) {
            databaseContainer = new MariaDBContainer<>(String.format("mariadb:%s", testConfiguration.getDatabaseTag()));
        } else {
            databaseContainer = new MariaDBContainer<>();
        }
        startMySQLContainer(databaseContainer, testConfiguration);
    }

    private void startPostgreSQLContainer(TestConfiguration testConfiguration) throws Exception
    {
        // docker run --net=xwiki-nw --name postgres-xwiki -v /my/own/postgres:/var/lib/postgresql/data
        //     -e POSTGRES_ROOT_PASSWORD=xwiki -e POSTGRES_USER=xwiki -e POSTGRES_PASSWORD=xwiki
        //     -e POSTGRES_DB=xwiki -e POSTGRES_INITDB_ARGS="--encoding=UTF8" -d postgres:9.5
        JdbcDatabaseContainer databaseContainer;
        if (testConfiguration.getDatabaseTag() != null) {
            databaseContainer =
                new PostgreSQLContainer<>(String.format("postgres:%s", testConfiguration.getDatabaseTag()));
        } else {
            databaseContainer = new PostgreSQLContainer<>();
        }
        databaseContainer
            .withDatabaseName(DBNAME)
            .withUsername(DBUSERNAME)
            .withPassword(DBPASSWORD);

        mountDatabaseDataIfNeeded(databaseContainer, "./target/postgres", "/var/lib/postgresql/data",
            testConfiguration);

        databaseContainer.addEnv("POSTGRES_ROOT_PASSWORD", DBPASSWORD);
        databaseContainer.addEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8");

        startDatabaseContainer(databaseContainer, 5432, testConfiguration);
    }

    private void mountDatabaseDataIfNeeded(JdbcDatabaseContainer databaseContainer, String hostPath,
        String containerPath, TestConfiguration testConfiguration)
    {
        if (testConfiguration.isDatabaseDataSaved()) {
            // Note 1: This allows re-running the test with the database already provisioned without having to redo
            // the provisioning. Running "mvn clean" will remove the database data.
            // Note 2: This won't work in the DOOD use case. For that to work we would need to copy the data instead
            // of mounting the volume but the time to do that would be counter productive and cost in execution time
            // when the goal is to win time...
            databaseContainer.withFileSystemBind(hostPath, containerPath);
        }
    }

    private void startOracleContainer(TestConfiguration testConfiguration) throws Exception
    {
        JdbcDatabaseContainer databaseContainer;
        if (testConfiguration.getDatabaseTag() != null) {
            databaseContainer = new OracleContainer(String.format("xwiki/oracle-database:%s",
                testConfiguration.getDatabaseTag()));
        } else {
            databaseContainer = new OracleContainer("xwiki/oracle-database");
        }
        databaseContainer
            .withUsername("system")
            .withPassword("oracle");

        startDatabaseContainer(databaseContainer, 1521, testConfiguration);
    }

    private void startDatabaseContainer(JdbcDatabaseContainer databaseContainer, int port,
        TestConfiguration testConfiguration) throws Exception
    {
        databaseContainer
            .withExposedPorts(port)
            .withNetwork(Network.SHARED)
            .withNetworkAliases("xwikidb");

        start(databaseContainer, testConfiguration);

        if (testConfiguration.getServletEngine().isOutsideDocker()) {
            testConfiguration.getDatabase().setIP(databaseContainer.getContainerIpAddress());
            testConfiguration.getDatabase().setPort(databaseContainer.getMappedPort(port));
        } else {
            testConfiguration.getDatabase().setIP((String) databaseContainer.getNetworkAliases().get(0));
            testConfiguration.getDatabase().setPort(port);
        }
    }
}
