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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.store.hibernate.DatabaseProductNameResolved;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.store.hibernate.HibernateAdapterFactory;

import com.xpn.xwiki.store.DatabaseProduct;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultHibernateAdapterFactory implements HibernateAdapterFactory
{
    private static final Version DEFAULT_VERSION = new DefaultVersion("0");

    @Inject
    private ComponentManager componentManager;

    @Inject
    private List<DatabaseProductNameResolved> resolvers;

    @Inject
    private Logger logger;

    @Override
    public Optional<HibernateAdapter> createHibernateAdapter(DatabaseMetaData metaData,
        Configuration hibernateConfiguration) throws HibernateException
    {
        HibernateAdapter adaparter = null;

        // Index all adapters
        Map<String, Map<Version, String>> mapping = getAdpaterMapping();

        // Gather adapter(s) associated to the specific database product id
        Map<Version, String> versions;
        try {
            // Resolved the database product identifier
            String databaseProductId = getDatabaseId(metaData.getDatabaseProductName());

            versions = mapping.get(databaseProductId);
        } catch (SQLException e) {
            throw new HibernateException("Failed to access the database product name", e);
        }

        if (versions != null) {
            // Resolve the current database version
            Version currentVersion;
            try {
                currentVersion = new DefaultVersion(metaData.getDatabaseProductVersion());
            } catch (SQLException e) {
                throw new HibernateException("Failed to access the database product version", e);
            }

            // Search if a registered adapter matched the current version
            String roleHint = null;
            for (Map.Entry<Version, String> entry : versions.entrySet()) {
                if (entry.getKey().compareTo(currentVersion) <= 0) {
                    roleHint = entry.getValue();
                } else {
                    break;
                }
            }

            // Load the found adapter
            if (roleHint != null) {
                try {
                    adaparter = this.componentManager.getInstance(HibernateAdapter.class, roleHint);
                } catch (ComponentLookupException e) {
                    throw new HibernateException("Failed to initialize the adapater", e);
                }
            }
        }

        return Optional.ofNullable(adaparter);
    }

    private String getDatabaseId(String databaseProductName)
    {
        for (DatabaseProductNameResolved resolver : this.resolvers) {
            Optional<String> id = resolver.resolve(databaseProductName);

            if (id.isPresent()) {
                return id.get();
            }
        }

        // Default resolution based on old DatabaseProduct resolution, except for MariaDB (since it's identified as
        // MySQL)
        if (databaseProductName.equalsIgnoreCase(MariaDBHibernateAdapter.HINT)) {
            return MariaDBHibernateAdapter.HINT;
        } else {
            DatabaseProduct product = DatabaseProduct.toProduct(databaseProductName);

            return product != DatabaseProduct.UNKNOWN ? product.getJDBCScheme() : databaseProductName.toLowerCase();
        }
    }

    private Map<String, Map<Version, String>> getAdpaterMapping()
    {
        List<ComponentDescriptor<HibernateAdapter>> descriptors =
            this.componentManager.getComponentDescriptorList(HibernateAdapter.class);

        Map<String, Map<Version, String>> databases = new HashMap<>();
        for (ComponentDescriptor<HibernateAdapter> descriptor : descriptors) {
            String roleHint = descriptor.getRoleHint();
            if (roleHint != null && !"default".equalsIgnoreCase(roleHint)) {
                String databaseName = roleHint;
                Version databaseVersion = DEFAULT_VERSION;
                int index = roleHint.indexOf("/");
                if (index > 0) {
                    databaseName = roleHint.substring(0, index);
                    if (roleHint.length() > index) {
                        databaseVersion = new DefaultVersion(roleHint.substring(index + 1));
                    }
                }

                Map<Version, String> databaseVersions =
                    databases.computeIfAbsent(databaseName.toLowerCase(), n -> new TreeMap<>());

                databaseVersions.put(databaseVersion, roleHint);
            }
        }

        return databases;
    }
}
