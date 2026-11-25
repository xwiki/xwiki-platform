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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.JdbcSettings;

/**
 * The Hibernate settings of the JDBC connection, which can each be configured with several names: the Jakarta
 * Persistence standard one ({@code jakarta.persistence.jdbc.*}), the Hibernate specific one
 * ({@code hibernate.connection.*}, used by most existing {@code hibernate.cfg.xml} files), the Java Persistence 2 one
 * ({@code javax.persistence.jdbc.*}) and the short form accepted in {@code hibernate.cfg.xml}
 * ({@code connection.*}).
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
// The deprecated names are still supported by Hibernate and used by existing configurations
@SuppressWarnings("deprecation")
public enum HibernateConnectionSetting
{
    /**
     * The JDBC URL.
     */
    URL(JdbcSettings.JAKARTA_JDBC_URL, JdbcSettings.URL, JdbcSettings.JPA_JDBC_URL),

    /**
     * The JDBC driver class.
     */
    DRIVER(JdbcSettings.JAKARTA_JDBC_DRIVER, JdbcSettings.DRIVER, JdbcSettings.JPA_JDBC_DRIVER),

    /**
     * The database user.
     */
    USER(JdbcSettings.JAKARTA_JDBC_USER, JdbcSettings.USER, JdbcSettings.JPA_JDBC_USER),

    /**
     * The database user password.
     */
    PASSWORD(JdbcSettings.JAKARTA_JDBC_PASSWORD, JdbcSettings.PASS, JdbcSettings.JPA_JDBC_PASSWORD);

    private static final String HIBERNATE_PREFIX = "hibernate.";

    private final List<String> names;

    HibernateConnectionSetting(String standardName, String hibernateName, String javaxName)
    {
        this.names =
            List.of(standardName, hibernateName, javaxName, hibernateName.substring(HIBERNATE_PREFIX.length()));
    }

    /**
     * @return the names of the setting, in the order of precedence used by Hibernate
     */
    public List<String> getNames()
    {
        return this.names;
    }

    /**
     * Returns the value of the setting, whichever of its names is used: for example the JDBC URL configured with
     * {@code hibernate.connection.url} or {@code jakarta.persistence.jdbc.url}.
     *
     * @param settings the Hibernate settings (for example {@link Configuration#getProperties()},
     *            {@link LoadedConfig#getConfigurationValues()}, the settings passed to a connection provider or
     *            {@link System#getProperties()}); values which are not strings are converted with
     *            {@link Object#toString()}
     * @return the value of the first name with a value, in the order of precedence used by Hibernate, or {@code null}
     *         if the setting is not set
     */
    public String getValue(Map<?, ?> settings)
    {
        for (String name : this.names) {
            Object value = settings.get(name);
            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    /**
     * Returns the value of the setting in the passed configuration, whichever of its names is used.
     *
     * @param configuration the Hibernate configuration
     * @return the value of the first name with a value, in the order of precedence used by Hibernate, or {@code null}
     *         if the setting is not set
     */
    public String getValue(Configuration configuration)
    {
        return getValue(configuration.getProperties());
    }

    /**
     * Replaces the value of each name of the setting which is set (several names can be set at the same time, and the
     * one Hibernate actually uses depends on its precedence rules, so all of them are updated).
     *
     * @param settings the Hibernate settings to modify (for example {@link Configuration#getProperties()} or
     *            {@link LoadedConfig#getConfigurationValues()})
     * @param resolver the function returning the new value for a current value (for example a JDBC URL with its
     *            variables resolved), or {@code null} to keep it unchanged
     * @return the names whose value was replaced, associated with their new value (empty when nothing changed), in
     *         the order of precedence used by Hibernate
     */
    public Map<String, String> replaceValues(Map<? super String, Object> settings, UnaryOperator<String> resolver)
    {
        Map<String, String> replaced = new LinkedHashMap<>();
        for (String name : this.names) {
            Object value = settings.get(name);
            if (value != null) {
                String newValue = resolver.apply(value.toString());
                if (newValue != null && !newValue.equals(value)) {
                    settings.put(name, newValue);
                    replaced.put(name, newValue);
                }
            }
        }

        return replaced;
    }
}
