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

import java.io.File;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.MountableFile;

/**
 * A container running HSQLDB in server mode, so that several XWiki instances can use the same database at the same
 * time.
 * <p>
 * There's no HSQLDB Docker image, so the HSQLDB server is started in a JRE image, from the HSQLDB JAR (which contains
 * both the server and the JDBC driver).
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
public class HSQLDBContainer extends JdbcDatabaseContainer<HSQLDBContainer>
{
    /**
     * The port on which the HSQLDB server listens (i.e. the default HSQLDB port).
     */
    public static final int PORT = 9001;

    /**
     * The directory in the container where the database files are stored.
     */
    public static final String DATA_DIRECTORY = "/var/hsqldb";

    private static final String JAR_PATH = "/opt/hsqldb/hsqldb.jar";

    private String databaseName = "xwiki";

    private String username = "sa";

    private String password = "";

    /**
     * @param dockerImageName the name of the JRE image in which to execute the HSQLDB server
     * @param hsqldbJARFile the HSQLDB JAR from which the server is started
     */
    public HSQLDBContainer(String dockerImageName, File hsqldbJARFile)
    {
        super(dockerImageName);

        withCopyFileToContainer(MountableFile.forHostPath(hsqldbJARFile.getAbsolutePath()), JAR_PATH);
        withExposedPorts(PORT);
    }

    @Override
    protected void configure()
    {
        // Note: the database name is also used as the name of the database files since a single HSQLDB database is
        // enough for XWiki: subwikis are created as schemas of the main database.
        withCommand("java", "-cp", JAR_PATH, "org.hsqldb.server.Server",
            "--database.0", String.format("file:%s/%s", DATA_DIRECTORY, this.databaseName),
            "--dbname.0", this.databaseName,
            "--port", String.valueOf(PORT));
    }

    @Override
    public String getDriverClassName()
    {
        return "org.hsqldb.jdbcDriver";
    }

    @Override
    public String getJdbcUrl()
    {
        return String.format("jdbc:hsqldb:hsql://%s:%s/%s", getHost(), getMappedPort(PORT), this.databaseName);
    }

    @Override
    public String getDatabaseName()
    {
        return this.databaseName;
    }

    @Override
    public String getUsername()
    {
        return this.username;
    }

    @Override
    public String getPassword()
    {
        return this.password;
    }

    @Override
    protected String getTestQueryString()
    {
        return "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS";
    }

    @Override
    public HSQLDBContainer withDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public HSQLDBContainer withUsername(String username)
    {
        this.username = username;
        return self();
    }

    @Override
    public HSQLDBContainer withPassword(String password)
    {
        this.password = password;
        return self();
    }
}
