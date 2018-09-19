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

/**
 * The database to use for the UI tests.
 *
 * @version $Id$
 * @since 10.9RC1
 */
public enum Database
{
    /**
     * Represents the MySQL database.
     */
    MYSQL,

    /**
     * Represents the HyperSQL database.
     */
    HSQLDB,

    /**
     * Represents the PostgreSQL database.
     */
    POSTGRESQL,

    /**
     * Represents the Oracle database.
     */
    ORACLE
}
