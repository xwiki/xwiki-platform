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
package org.xwiki.test.docker.junit5;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Create and execute the Docker database container for the tests.
 *
 * @version $Id$
 * @since 10.9
 */
public class DatabaseContainerExecutor extends AbstractContainerExecutor
{
    private static final String DBNAME = "xwiki";

    private static final String DBUSERNAME = DBNAME;

    private static final String DBPASSWORD = DBUSERNAME;

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     */
    public void start(TestConfiguration testConfiguration)
    {
        switch (testConfiguration.getDatabase()) {
            case MYSQL:
                startMySQLContainer(testConfiguration);
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
                testConfiguration.getDatabase().setIpAddress("localhost");
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

    private void startMySQLContainer(TestConfiguration testConfiguration)
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
        databaseContainer
            .withDatabaseName(DBNAME)
            .withUsername(DBUSERNAME)
            .withPassword(DBPASSWORD);

        if (testConfiguration.isDatabaseDataSaved()) {
            // This allows re-running the test with the database already provisioned without having to redo
            // the provisioning. Running "mvn clean" will remove the database data.
            databaseContainer.withFileSystemBind("./target/mysql", "/var/lib/mysql");
        }

        databaseContainer.addParameter("character-set-server", "utf8");
        databaseContainer.addParameter("collation-server", "utf8_bin");
        databaseContainer.addParameter("explicit-defaults-for-timestamp", "1");

        startDatabaseContainer(databaseContainer, 3306, testConfiguration);
    }

    private void startPostgreSQLContainer(TestConfiguration testConfiguration)
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

        if (testConfiguration.isDatabaseDataSaved()) {
            // This allows re-running the test with the database already provisioned without having to redo
            // the provisioning. Running "mvn clean" will remove the database data.
            databaseContainer.withFileSystemBind("./target/postgres", "/var/lib/postgresql/data");
        }

        databaseContainer.addEnv("POSTGRES_ROOT_PASSWORD", DBPASSWORD);
        databaseContainer.addEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8");

        startDatabaseContainer(databaseContainer, 5432, testConfiguration);
    }

    private void startOracleContainer(TestConfiguration testConfiguration)
    {
        JdbcDatabaseContainer databaseContainer;
        if (testConfiguration.getDatabaseTag() != null) {
            databaseContainer = new OracleContainer(testConfiguration.getDatabaseTag());
        } else {
            databaseContainer = new OracleContainer();
        }
        databaseContainer
            .withDatabaseName(DBNAME)
            .withUsername(DBUSERNAME)
            .withPassword(DBPASSWORD);

        startDatabaseContainer(databaseContainer, -1, testConfiguration);
    }

    private void startDatabaseContainer(JdbcDatabaseContainer databaseContainer, int port,
        TestConfiguration testConfiguration)
    {
        databaseContainer
            .withExposedPorts(port)
            .withNetwork(Network.SHARED)
            .withNetworkAliases("xwikidb");

        start(databaseContainer, testConfiguration);

        if (testConfiguration.getServletEngine().isOutsideDocker()) {
            testConfiguration.getDatabase().setIpAddress(databaseContainer.getContainerIpAddress());
            testConfiguration.getDatabase().setPort(databaseContainer.getMappedPort(port));
        } else {
            testConfiguration.getDatabase().setIpAddress((String) databaseContainer.getNetworkAliases().get(0));
            testConfiguration.getDatabase().setPort(port);
        }
    }
}
