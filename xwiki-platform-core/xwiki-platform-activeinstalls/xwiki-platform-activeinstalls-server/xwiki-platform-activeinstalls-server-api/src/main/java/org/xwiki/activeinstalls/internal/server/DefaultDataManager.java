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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.activeinstalls.server.DataManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.properties.ConverterManager;

import com.google.gson.JsonObject;

import io.searchbox.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.Count;
import io.searchbox.core.Search;
import io.searchbox.params.Parameters;
import io.searchbox.params.SearchType;

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
    @Inject
    private JestClientManager jestClientManager;

    @Inject
    private ConverterManager converterManager;

    @Override
    public JsonObject countInstalls(String indexType, String fullQuery, Map<String, Object> parameters)
        throws Exception
    {
        Count.Builder countBuilder = new Count.Builder()
            .query(fullQuery)
            .addIndex(JestClientManager.INDEX)
            .addType(indexType);

        // Add parameters.
        for (Map.Entry<String, Object> parameterEntry : parameters.entrySet()) {
            countBuilder.setParameter(parameterEntry.getKey(), parameterEntry.getValue());
        }

        return executeActionQuery(countBuilder.build(), fullQuery).getJsonObject();
    }

    @Override
    public JsonObject searchInstalls(String indexType, String fullQuery, Map<String, Object> parameters)
        throws Exception
    {
        Search.Builder searchBuilder = new Search.Builder(fullQuery)
            .addIndex(JestClientManager.INDEX)
            .addType(indexType);

        // Add parameters and handle specifically the Search Type.
        if (parameters.containsKey(Parameters.SEARCH_TYPE)) {
            SearchType searchType = this.converterManager.convert(SearchType.class,
                parameters.get(Parameters.SEARCH_TYPE));
            searchBuilder.setSearchType(searchType);
        }
        for (Map.Entry<String, Object> parameterEntry : parameters.entrySet()) {
            if (!parameterEntry.getKey().equals(Parameters.SEARCH_TYPE)) {
                searchBuilder.setParameter(parameterEntry.getKey(), parameterEntry.getValue());
            }
        }

        return executeActionQuery(searchBuilder.build(), fullQuery).getJsonObject();
    }

    private JestResult executeActionQuery(Action action, String fullQuery) throws Exception
    {
        JestResult result = this.jestClientManager.getClient().execute(action);

        if (!result.isSucceeded()) {
            throw new Exception(String.format("Error while executing [%s] query [%s]: [%s], Reason: [%s]",
                action.getClass().getSimpleName(), fullQuery, result.getErrorMessage(), result.getJsonString()));
        }

        return result;
    }
}
