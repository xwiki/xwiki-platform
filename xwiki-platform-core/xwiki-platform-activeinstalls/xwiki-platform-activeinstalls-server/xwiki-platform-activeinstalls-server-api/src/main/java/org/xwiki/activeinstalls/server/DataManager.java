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
package org.xwiki.activeinstalls.server;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import com.google.gson.JsonObject;

/**
 * Provides access to stored ping data.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Role
public interface DataManager
{
    /**
     * Executes a Search query for Active Installs.
     *
     * @param indexType the Elastic Search index type (e.g. "installs" or "installs2". Some XWiki instances may have
     *        sent pings stored under a given index type while other instances may have used another index type (and
     *        thus another data format). We use the index type to match a given data model. The Active Installs Client
     *        module should be checked to understand the data model used.
     * @param fullQuery the full Elastic Search query used to search for installs. For example:
     *        <pre>{@code
     *            {
     *              "query" : {
     *                "term": { "distributionVersion" : "5.2" }
     *              }
     *            }
     *        }</pre>
     * @return the parsed JSON result coming from Elastic Search, for example:
     *         <pre>{@code
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
     *         }</pre>
     * @param parameters the ElasticSearch search parameters to use (see for example
     *        <a href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-uri-request.html">
     *        Search URI parameters</a>)
     * @throws Exception when an error happens while retrieving the data
     * @since 6.1M1
     */
    JsonObject searchInstalls(String indexType, String fullQuery, Map<String, Object> parameters) throws Exception;

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
     * @return the parsed JSON result coming from Elastic Search
     * @throws Exception when an error happens while retrieving the data
     * @since 6.1M1
     */
    JsonObject countInstalls(String indexType, String fullQuery, Map<String, Object> parameters) throws Exception;
}
