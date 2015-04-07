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
package org.xwiki.activeinstalls.internal.client.data;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.activeinstalls.internal.client.PingDataProvider;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Provide database name and version.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("database")
@Singleton
public class DatabasePingDataProvider implements PingDataProvider
{
    private static final String PROPERTY_DB_NAME = "dbName";

    private static final String PROPERTY_DB_VERSION = "dbVersion";

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    @Override
    public Map<String, Object> provideMapping()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "string");
        map.put("index", "not_analyzed");

        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_DB_NAME, map);
        propertiesMap.put(PROPERTY_DB_VERSION, map);

        return propertiesMap;
    }

    @Override
    public Map<String, Object> provideData()
    {
        Map<String, Object> jsonMap = new HashMap<>();
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
            try {
                jsonMap.put(PROPERTY_DB_NAME, metaData.getDatabaseProductName());
            } catch (SQLException e) {
                // Ignore, we just don't save that information...
                // However we log a warning since it's a problem that needs to be seen and looked at.
                logWarning("Failed to compute the database product name", e);
            }
            try {
                jsonMap.put(PROPERTY_DB_VERSION, metaData.getDatabaseProductVersion());
            } catch (SQLException e) {
                // Ignore, we just don't save that information...
                // However we log a warning since it's a problem that needs to be seen and looked at.
                logWarning("Failed to compute the database product version", e);
            }
        }
        return jsonMap;
    }

    private void logWarning(String explanation, Throwable e)
    {
        this.logger.warn("{}. This information has not been added to the Active Installs ping data. Reason [{}]",
            explanation, ExceptionUtils.getRootCauseMessage(e));
    }

    private DatabaseMetaData getDatabaseMetaData()
    {
        DatabaseMetaData metaData = null;
        XWikiContext xcontext =
            (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        if (xcontext != null) {
            XWikiStoreInterface storeInterface = xcontext.getWiki().getStore();
            if (storeInterface instanceof XWikiCacheStoreInterface) {
                storeInterface = ((XWikiCacheStoreInterface) storeInterface).getStore();
            }
            if (XWikiHibernateBaseStore.class.isAssignableFrom(storeInterface.getClass())) {
                XWikiHibernateBaseStore baseStore = (XWikiHibernateBaseStore) storeInterface;
                metaData = baseStore.getDatabaseMetaData();
            }
        }
        return metaData;
    }
}
