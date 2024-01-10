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
package org.xwiki.activeinstalls2.internal.data;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * Provide database name and version.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("database")
@Singleton
public class DatabasePingDataProvider extends AbstractPingDataProvider
{
    private static final String PROPERTY_DB_NAME = "name";

    private static final String PROPERTY_DB_VERSION = "version";

    private static final String PROPERTY_DB = "database";

    @Inject
    private Execution execution;

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_DB_NAME, Property.of(b1 -> b1.keyword(b2 -> b2)));
        propertiesMap.put(PROPERTY_DB_VERSION, Property.of(b1 -> b1.keyword(b2 -> b2)));

        return Collections.singletonMap(PROPERTY_DB, Property.of(b0 -> b0.object(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        DatabaseMetaData metaData;
        try {
            metaData = getDatabaseMetaData();
        } catch (Exception e) {
            // Ignore, we just don't save DB information...
            // However we log a warning since it's a problem that needs to be seen and looked at.
            logWarning("Failed to retrieve database metadata", e);
            metaData = null;
        }

        if (metaData != null) {
            DatabasePing databasePing = new DatabasePing();
            try {
                databasePing.setName(metaData.getDatabaseProductName());
            } catch (SQLException e) {
                // Ignore, we just don't save that information...
                // However we log a warning since it's a problem that needs to be seen and looked at.
                logWarning("Failed to compute the database product name", e);
            }
            try {
                databasePing.setVersion(metaData.getDatabaseProductVersion());
            } catch (SQLException e) {
                // Ignore, we just don't save that information...
                // However we log a warning since it's a problem that needs to be seen and looked at.
                logWarning("Failed to compute the database product version", e);
            }
            ping.setDatabase(databasePing);
        }
    }

    private DatabaseMetaData getDatabaseMetaData()
    {
        DatabaseMetaData metaData = null;
        ExecutionContext ec = this.execution.getContext();
        if (ec != null) {
            XWikiContext xcontext = (XWikiContext) ec.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
            if (xcontext != null) {
                XWikiStoreInterface storeInterface = getStoreInterface(xcontext);
                if (XWikiHibernateBaseStore.class.isAssignableFrom(storeInterface.getClass())) {
                    XWikiHibernateBaseStore baseStore = (XWikiHibernateBaseStore) storeInterface;
                    metaData = baseStore.getDatabaseMetaData();
                }
            }
        }
        return metaData;
    }

    private XWikiStoreInterface getStoreInterface(XWikiContext xcontext)
    {
        XWikiStoreInterface storeInterface = xcontext.getWiki().getStore();
        if (storeInterface instanceof XWikiCacheStoreInterface xwikiCacheStoreInterface) {
            storeInterface = xwikiCacheStoreInterface.getStore();
        }
        return storeInterface;
    }
}
