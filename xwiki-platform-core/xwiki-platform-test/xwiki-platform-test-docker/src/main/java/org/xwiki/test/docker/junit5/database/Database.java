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
package org.xwiki.test.docker.junit5.database;

/**
 * The database to use for the UI tests.
 *
 * @version $Id$
 * @since 10.9
 */
public enum Database
{
    /**
     * Represents the MySQL database.
     */
    MYSQL("mysql"),

    /**
     * Represents the MariaDB database.
     */
    MARIADB("mariadb"),

    /**
     * Represents the HyperSQL database, running outside of a Docker contaier.
     */
    HSQLDB_EMBEDDED("hsqldb"),

    /**
     * Represents the PostgreSQL database.
     */
    POSTGRESQL("pgsql"),

    /**
     * Represents the Oracle database.
     */
    ORACLE("oracle");

    private String ip;

    private int port;

    private String pomPropertyPrefix;

    /**
     * @param pomPropertyPrefix see {@link #getPomPropertyPrefix()}
     * @since 15.2RC1
     */
    Database(String pomPropertyPrefix)
    {
        this.pomPropertyPrefix = pomPropertyPrefix;
    }

    /**
     * @param ip see {@link #getIP()}
     */
    public void setIP(String ip)
    {
        this.ip = ip;
    }

    /**
     * @return the IP address to use to connect to the database (it can be different if the DB runs locally or in a
     * Docker container)
     */
    public String getIP()
    {
        return this.ip;
    }

    /**
     * @param port see {@link #getPort()}
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * @return the port to use to connect to the database (it can be different if the DB runs locally or in a Docker
     * container)
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * @return the prefix for defining the JDBC artifact properties in XWiki's {@code pom.xml}
     *         (e.g {@code mariadb} for {@code mariadb.groupId}, {@code mariadb.artifactId} and {@code mariadb.version})
     * @since 15.2RC1
     */
    public String getPomPropertyPrefix()
    {
        return this.pomPropertyPrefix;
    }
}
