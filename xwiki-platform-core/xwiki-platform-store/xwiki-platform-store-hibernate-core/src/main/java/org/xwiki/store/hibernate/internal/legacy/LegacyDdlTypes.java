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
package org.xwiki.store.hibernate.internal.legacy;

import java.util.Map;
import java.util.WeakHashMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Implementation of the Hibernate 5 {@link Dialect} APIs giving the database type of a JDBC type code, which Hibernate
 * 7 moved to the {@link DdlTypeRegistry} of the {@link TypeConfiguration}.
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
public final class LegacyDdlTypes
{
    /**
     * The registries built for each dialect, since a dialect does not have access to the one of the session factory.
     */
    private static final Map<Dialect, DdlTypeRegistry> REGISTRIES = new WeakHashMap<>();

    private LegacyDdlTypes()
    {
    }

    /**
     * @param dialect the dialect
     * @param code the JDBC type code
     * @return the database type associated with the type code, whatever its size
     */
    @SuppressWarnings("deprecation")
    public static String getTypeName(Dialect dialect, int code)
    {
        return getDescriptor(dialect, code).getRawTypeName();
    }

    /**
     * @param dialect the dialect
     * @param code the JDBC type code
     * @param length the length of the column
     * @param precision the precision of the column
     * @param scale the scale of the column
     * @return the database type associated with the type code and the size
     */
    @SuppressWarnings("deprecation")
    public static String getTypeName(Dialect dialect, int code, long length, int precision, int scale)
    {
        return getDescriptor(dialect, code).getTypeName(length, precision, scale);
    }

    private static DdlType getDescriptor(Dialect dialect, int code)
    {
        DdlType descriptor = getRegistry(dialect).getDescriptor(code);
        if (descriptor == null) {
            throw new HibernateException("No type mapping for java.sql.Types code: " + code);
        }

        return descriptor;
    }

    private static DdlTypeRegistry getRegistry(Dialect dialect)
    {
        synchronized (REGISTRIES) {
            return REGISTRIES.computeIfAbsent(dialect, LegacyDdlTypes::createRegistry);
        }
    }

    private static DdlTypeRegistry createRegistry(Dialect dialect)
    {
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
        try {
            // Contribute the types the same way Hibernate does when building the metadata
            TypeConfiguration typeConfiguration = new TypeConfiguration();
            TypeContributions contributions = () -> typeConfiguration;
            dialect.contribute(contributions, serviceRegistry);
            for (TypeContributor contributor : serviceRegistry.requireService(ClassLoaderService.class)
                .loadJavaServices(TypeContributor.class)) {
                contributor.contribute(contributions, serviceRegistry);
            }

            return typeConfiguration.getDdlTypeRegistry();
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }
}
