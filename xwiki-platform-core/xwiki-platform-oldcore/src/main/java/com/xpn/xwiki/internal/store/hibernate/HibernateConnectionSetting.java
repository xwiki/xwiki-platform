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

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

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
     * @param getter the function returning the value associated with a setting name
     * @return the value of the first name with a value, in the order of precedence used by Hibernate, or {@code null}
     *         if the setting is not set
     */
    public String get(Function<String, ?> getter)
    {
        for (String name : this.names) {
            Object value = getter.apply(name);
            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    /**
     * Transform the value of each name of the setting which is set.
     *
     * @param getter the function returning the value associated with a setting name
     * @param setter the function setting a new value for a setting name
     * @param transformer the transformation to apply, returning {@code null} to keep the value unchanged
     */
    public void transform(Function<String, ?> getter, BiConsumer<String, String> setter,
        UnaryOperator<String> transformer)
    {
        for (String name : this.names) {
            Object value = getter.apply(name);
            if (value != null) {
                String newValue = transformer.apply(value.toString());
                if (newValue != null && !Objects.equals(newValue, value)) {
                    setter.accept(name, newValue);
                }
            }
        }
    }
}
