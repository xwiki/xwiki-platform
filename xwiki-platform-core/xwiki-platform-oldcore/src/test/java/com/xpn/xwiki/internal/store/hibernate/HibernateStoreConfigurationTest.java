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
package com.xpn.xwiki.internal.store.hibernate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link HibernateStoreConfiguration}.
 *
 * @version $Id$
 */
class HibernateStoreConfigurationTest
{
    @Test
    void getPropertyOfConnectionSettingWithAnotherName()
    {
        HibernateStoreConfiguration configuration = new HibernateStoreConfiguration(null);
        configuration.setProperty("jakarta.persistence.jdbc.driver", "org.hsqldb.jdbc.JDBCDriver");

        // The names used by the code written for Hibernate 5
        assertEquals("org.hsqldb.jdbc.JDBCDriver", configuration.getProperty("hibernate.connection.driver_class"));
        assertEquals("org.hsqldb.jdbc.JDBCDriver", configuration.getProperty("connection.driver_class"));
        assertEquals("org.hsqldb.jdbc.JDBCDriver", configuration.getProperty("javax.persistence.jdbc.driver"));
        assertNull(configuration.getProperty("hibernate.connection.url"));
    }

    @Test
    void getPropertyPrefersTheRequestedName()
    {
        HibernateStoreConfiguration configuration = new HibernateStoreConfiguration(null);
        configuration.setProperty("jakarta.persistence.jdbc.user", "jakarta");
        configuration.setProperty("hibernate.connection.username", "hibernate");

        assertEquals("hibernate", configuration.getProperty("hibernate.connection.username"));
        assertEquals("jakarta", configuration.getProperty("jakarta.persistence.jdbc.user"));
        assertNull(configuration.getProperty("hibernate.dialect"));
    }
}
