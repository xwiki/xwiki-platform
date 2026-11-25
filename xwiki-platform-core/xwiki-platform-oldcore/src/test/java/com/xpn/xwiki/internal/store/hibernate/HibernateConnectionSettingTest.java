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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Validate {@link HibernateConnectionSetting}.
 *
 * @version $Id$
 */
class HibernateConnectionSettingTest
{
    @Test
    void getNames()
    {
        assertEquals(List.of("jakarta.persistence.jdbc.url", "hibernate.connection.url", "javax.persistence.jdbc.url",
            "connection.url"), HibernateConnectionSetting.URL.getNames());
        assertEquals(List.of("jakarta.persistence.jdbc.driver", "hibernate.connection.driver_class",
            "javax.persistence.jdbc.driver", "connection.driver_class"), HibernateConnectionSetting.DRIVER.getNames());
        assertEquals(List.of("jakarta.persistence.jdbc.user", "hibernate.connection.username",
            "javax.persistence.jdbc.user", "connection.username"), HibernateConnectionSetting.USER.getNames());
        assertEquals(List.of("jakarta.persistence.jdbc.password", "hibernate.connection.password",
            "javax.persistence.jdbc.password", "connection.password"), HibernateConnectionSetting.PASSWORD.getNames());
    }

    @Test
    void getValueFollowsHibernatePrecedence()
    {
        Map<String, Object> settings = new HashMap<>();
        assertNull(HibernateConnectionSetting.URL.getValue(settings));

        settings.put("connection.url", "short");
        assertEquals("short", HibernateConnectionSetting.URL.getValue(settings));

        settings.put("javax.persistence.jdbc.url", "javax");
        assertEquals("javax", HibernateConnectionSetting.URL.getValue(settings));

        settings.put("hibernate.connection.url", "hibernate");
        assertEquals("hibernate", HibernateConnectionSetting.URL.getValue(settings));

        settings.put("jakarta.persistence.jdbc.url", "jakarta");
        assertEquals("jakarta", HibernateConnectionSetting.URL.getValue(settings));
    }

    @Test
    void getValueKeepsEmptyValue()
    {
        Map<String, Object> settings = Map.of("hibernate.connection.password", "");

        assertEquals("", HibernateConnectionSetting.PASSWORD.getValue(settings));
    }

    @Test
    void getValueFromConfiguration()
    {
        Configuration configuration = new Configuration();
        assertNull(HibernateConnectionSetting.DRIVER.getValue(configuration));

        configuration.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        assertEquals("org.hsqldb.jdbcDriver", HibernateConnectionSetting.DRIVER.getValue(configuration));
    }

    @Test
    void replaceValuesOfAllNames()
    {
        Map<String, Object> settings = new HashMap<>();
        settings.put("jakarta.persistence.jdbc.url", "jdbc:${dir}/jakarta");
        settings.put("hibernate.connection.url", "jdbc:${dir}/hibernate");
        settings.put("connection.url", "jdbc:/unchanged");

        Map<String, String> replaced = HibernateConnectionSetting.URL.replaceValues(settings,
            value -> value.contains("${dir}") ? value.replace("${dir}", "/data") : null);

        assertEquals(List.of("jakarta.persistence.jdbc.url", "hibernate.connection.url"),
            List.copyOf(replaced.keySet()));
        assertEquals(Map.of("jakarta.persistence.jdbc.url", "jdbc:/data/jakarta", "hibernate.connection.url",
            "jdbc:/data/hibernate"), replaced);
        assertEquals(Map.of("jakarta.persistence.jdbc.url", "jdbc:/data/jakarta", "hibernate.connection.url",
            "jdbc:/data/hibernate", "connection.url", "jdbc:/unchanged"), settings);
    }

    @Test
    void replaceValuesInProperties()
    {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.url", "jdbc:same");

        assertEquals(Map.of(), HibernateConnectionSetting.URL.replaceValues(properties, value -> value));
        assertEquals(Map.of("hibernate.connection.url", "jdbc:new"),
            HibernateConnectionSetting.URL.replaceValues(properties, value -> "jdbc:new"));
        assertEquals("jdbc:new", properties.getProperty("hibernate.connection.url"));
    }
}
