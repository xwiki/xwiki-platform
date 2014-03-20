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
package org.xwiki.activeinstalls.internal.server;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.activeinstalls.server.DataManager;
import org.xwiki.component.annotation.Component;

import io.searchbox.client.JestResult;
import io.searchbox.core.Count;
import net.sf.json.JSONObject;

/**
 * Get stored ping data from a remote Elastic Search instance.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultDataManager implements DataManager
{
    /**
     * JSON property name for a query.
     */
    private static final String JSON_QUERY_NAME = "query";

    @Inject
    private JestClientManager jestClientManager;

    @Override
    public long getInstallCount(String query) throws Exception
    {
        Map<String, Object> queryMap = new HashMap();
        queryMap.put(JSON_QUERY_NAME, query);

        // This allows to write queries such as: -distributionVersion:*SNAPSHOT
        queryMap.put("lowercase_expanded_terms", false);

        Map<String, Object> queryStringMap = new HashMap();
        queryStringMap.put("query_string", JSONObject.fromObject(queryMap));

        Map<String, Object> jsonMap = new HashMap();
        jsonMap.put(JSON_QUERY_NAME, JSONObject.fromObject(queryStringMap));

        return executeCount(JSONObject.fromObject(jsonMap).toString());
    }

    private long executeCount(String query) throws Exception
    {
        Count count = new Count.Builder()
            .query(query)
            .addIndex("installs")
            .addType("install")
            .build();
        JestResult result = this.jestClientManager.getClient().execute(count);

        if (!result.isSucceeded()) {
            throw new Exception(String.format("Error while executing Count query [%s]: [%s], Reason: [%s]", query,
                result.getErrorMessage(), result.getJsonString()));
        }

        return ((Double) result.getValue("count")).longValue();
    }
}
