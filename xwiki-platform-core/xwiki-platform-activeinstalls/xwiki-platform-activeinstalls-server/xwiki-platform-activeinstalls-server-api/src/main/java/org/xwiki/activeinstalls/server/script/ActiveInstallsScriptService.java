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
package org.xwiki.activeinstalls.server.script;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.server.DataManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;
import com.google.gson.JsonObject;

/**
 * Provides Scripting APIs for the Active Installs module.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Named("activeinstalls")
@Singleton
public class ActiveInstallsScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String ACTIVEINSTALLS_ERROR_KEY = "scriptservice.activeinstalls.error";

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to retrieve the data.
     */
    @Inject
    private DataManager dataManager;

    /**
     * Executes a Count query for Active Installs.
     *
     * @param indexType the Elastic Search index type (e.g. "installs" or "installs2". Some XWiki instances may have
     *        sent pings stored under a given index type while other instances may have used another index type (and
     *        thus another data format). We use the index type to match a given data model. The Active Installs Client
     *        module should be checked to understand the data model used.
     * @param fullQuery the full Elastic Search query used to search for installs.
     * @param parameters the ElasticSearch count parameters to use (see for example
     *        <a href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-count.html">
     *        Count API</a>)
     * @return the parsed JSON result coming from Elastic Search or null if an error happened, in which case the error
     *         can be retrieved with {@link #getLastError()}.
     * @since 6.1M1
     */
    public JsonObject countInstalls(String indexType, String fullQuery, Map<String, Object> parameters)
    {
        setError(null);

        JsonObject result;

        try {
            result = this.dataManager.countInstalls(indexType, fullQuery, parameters);
        } catch (Exception e) {
            setError(e);
            result = null;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * @see #countInstalls(String, String, java.util.Map)
     */
    public JsonObject countInstalls(String indexType, String fullQuery)
    {
        return countInstalls(indexType, fullQuery, Collections.<String, Object>emptyMap());
    }

    /**
     * Executes a Search query for Active Installs.
     *
     * @param indexType the Elastic Search index type (e.g. "installs" or "installs2". Some XWiki instances may have
     *        sent pings stored under a given index type while other instances may have used another index type (and
     *        thus another data format). We use the index type to match a given data model. The Active Installs Client
     *        module should be checked to understand the data model used.
     * @param fullQuery the full Elastic Search query used to search for installs. For example:
     *        <p><pre><code>
     *            {
     *              "query" : {
     *                "term": { "distributionVersion" : "5.2" }
     *              }
     *            }
     *        </code></pre></p>
     * @return the parsed JSON result coming from Elastic Search or null if an error happened, in which case the error
     *         can be retrieved with {@link #getLastError()}. For example:
     *        <p><pre><code>
     *            {
     *              "took": 97,
     *              "timed_out": false,
     *              "_shards": {
     *                "total": 1,
     *                "successful": 1,
     *                "failed": 0
     *              },
     *              "hits": {
     *                "total": 2,
     *                "max_score": 1,
     *                "hits": [
     *                  {
     *                    "_index": "installs",
     *                    "_type": "install",
     *                    "_id": "id1",
     *                    "_score": 1,
     *                    "_source": {
     *                      "distributionVersion": "5.2",
     *                      ...
     *                    }
     *                  },
     *                  {
     *                    "_index": "installs",
     *                    "_type": "install",
     *                    "_id": "id2",
     *                    "_score": 0.625,
     *                    "_source": {
     *                      "distributionVersion": "5.2-SNAPSHOT",
     *                      ...
     *                    }
     *                  }
     *                ]
     *              }
     *            }
     *        </code></pre></p>
     * @param parameters the ElasticSearch search parameters to use (see for example
     *        <a href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-uri-request.html">
     *        Search URI parameters</a>)
     * @since 6.1M1
     */
    public JsonObject searchInstalls(String indexType, String fullQuery, Map<String, Object> parameters)
    {
        setError(null);

        JsonObject result;

        try {
            result = this.dataManager.searchInstalls(indexType, fullQuery, parameters);
        } catch (Exception e) {
            setError(e);
            result = null;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * @see #searchInstalls(String, String, java.util.Map)
     */
    public JsonObject searchInstalls(String indexType, String fullQuery)
    {
        return searchInstalls(indexType, fullQuery, Collections.<String, Object>emptyMap());
    }

    // Error management

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ACTIVEINSTALLS_ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(ACTIVEINSTALLS_ERROR_KEY, e);
    }
}
