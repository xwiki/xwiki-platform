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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.activeinstalls.internal.client.PingDataProvider;
import org.xwiki.component.annotation.Component;
import org.xwiki.instance.InstanceIdManager;

import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import io.searchbox.params.SearchType;
import net.sf.json.JSONObject;

/**
 * Provide the date of the first ping and the elapsed days since the first ping. We do that to make it simpler to
 * perform complex queries on the ping data later on (for example to be able to query the average duration an instance
 * is used: < 1 day, 2-7 days, 7-30 days, 30-365 days, > 365 days).
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("date")
@Singleton
public class DatePingDataProvider implements PingDataProvider
{
    private static final String PROPERTY_FIRST_PING_DATE = "firstPingDate";

    private static final String PROPERTY_SINCE_DAYS = "sinceDays";

    private static final String PROPERTY_SERVER_TIME = "serverTime";

    private static final String PROPERTY_VALUE = "value";

    private static final String PROPERTY_TYPE = "type";

    private static final String PROPERTY_MIN = "min";

    private static final String ERROR_MESSAGE = "Failed to compute the first ping date and the number of elapsed days "
        + "since the first ping. This information has not been added to the Active Installs ping data. Reason [{}]";

    @Inject
    private JestClientManager jestClientManager;

    @Inject
    private InstanceIdManager instanceIdManager;

    @Inject
    private Logger logger;

    @Override
    public Map<String, Object> provideMapping()
    {
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_FIRST_PING_DATE, Collections.singletonMap(PROPERTY_TYPE, "date"));
        propertiesMap.put(PROPERTY_SINCE_DAYS, Collections.singletonMap(PROPERTY_TYPE, "long"));
        return propertiesMap;
    }

    @Override
    public Map<String, Object> provideData()
    {
        Map<String, Object> jsonMap = new HashMap<>();
        try {
            String instanceId = this.instanceIdManager.getInstanceId().toString();
            Search search = new Search.Builder(constructSearchJSON(instanceId))
                .addIndex(JestClientManager.INDEX)
                .addType(JestClientManager.TYPE)
                .setSearchType(SearchType.COUNT)
                .build();
            JestResult result = this.jestClientManager.getClient().execute(search);

            if (!result.isSucceeded()) {
                this.logger.warn(ERROR_MESSAGE, result.getErrorMessage());
                return jsonMap;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> aggregationsMap = (Map<String, Object>) result.getValue("aggregations");

            // Get the current server time and the first timestamp of the ping for this instance id and compute the
            // since days from them.
            @SuppressWarnings("unchecked")
            Map<String, Object> serverTimeMap = (Map<String, Object>) aggregationsMap.get(PROPERTY_SERVER_TIME);
            Object serverTimeObject = serverTimeMap.get(PROPERTY_VALUE);
            @SuppressWarnings("unchecked")
            Map<String, Object> firstPingDateMap = (Map<String, Object>) aggregationsMap.get(PROPERTY_FIRST_PING_DATE);
            Object firstPingDateObject = firstPingDateMap.get(PROPERTY_VALUE);

            if (serverTimeObject != null && firstPingDateObject != null) {
                long sinceDays = Math.round(((double) serverTimeObject - (double) firstPingDateObject) / 86400000D);
                jsonMap.put(PROPERTY_SINCE_DAYS, sinceDays);
                long firstPingDate = Math.round((double) firstPingDateObject);
                jsonMap.put(PROPERTY_FIRST_PING_DATE, firstPingDate);
            } else {
                // This means it's the first ping and thus there was no previous _timestamp. Thus we set the since Days
                // to 0.
                jsonMap.put(PROPERTY_SINCE_DAYS, 0);
            }
        } catch (Exception e) {
            // If this fails we just don't send this information but we still send the other piece of information.
            // However we log a warning since it's a problem that needs to be seen and looked at.
            this.logger.warn(ERROR_MESSAGE, ExceptionUtils.getRootCauseMessage(e));
        }
        return jsonMap;
    }

    protected static String constructSearchJSON(String instanceId)
    {
        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("query", Collections.singletonMap("term", Collections.singletonMap("instanceId", instanceId)));

        Map<String, Object> aggsMap = new HashMap<>();
        aggsMap.put(PROPERTY_SERVER_TIME, Collections.singletonMap(PROPERTY_MIN,
            Collections.singletonMap("script", "time()")));
        aggsMap.put(PROPERTY_FIRST_PING_DATE, Collections.singletonMap(PROPERTY_MIN,
            Collections.singletonMap("field", "_timestamp")));

        jsonMap.put("aggs", aggsMap);

        return JSONObject.fromObject(jsonMap).toString();
    }
}
