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
package org.xwiki.store.hibernate.internal;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Create the session factories used by the tests, on an in-memory HSQLDB database mapping {@link LegacyEntity}.
 *
 * @version $Id$
 */
final class TestSessionFactories
{
    private TestSessionFactories()
    {
        // Utility class
    }

    /**
     * @param database the name of the in-memory database
     * @param logCapture the extension capturing the warnings logged by Hibernate
     * @return the session factory
     */
    static SessionFactory create(String database, LogCaptureExtension logCapture)
    {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbc.JDBCDriver");
        configuration.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:" + database + ";shutdown=true");
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.connection.password", "");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        // HSQLDB reports a warning when an update does not match any row
        configuration.setProperty("hibernate.jdbc.log.warnings", "false");
        configuration.addResource("legacy.hbm.xml");

        SessionFactory sessionFactory = configuration.buildSessionFactory();

        assertEquals("HHH10001002: Using built-in connection pool (not intended for production use)",
            logCapture.getMessage(0));
        assertTrue(
            logCapture.getMessage(1).startsWith("HHH90000028: Support for `<hibernate-mappings/>` is deprecated"));

        return sessionFactory;
    }
}
