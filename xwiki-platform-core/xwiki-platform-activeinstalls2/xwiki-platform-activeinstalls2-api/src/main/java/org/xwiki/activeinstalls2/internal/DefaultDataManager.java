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
package org.xwiki.activeinstalls2.internal;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.activeinstalls2.DataManager;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.component.annotation.Component;

import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

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
    private ElasticsearchClientManager clientManager;

    @Override
    public long countInstalls(String jsonQuery) throws Exception
    {
        CountRequest request;
        if (StringUtils.isEmpty(jsonQuery)) {
            request = CountRequest.of(s -> s
                .index(ElasticsearchClientManager.INDEX));
        } else {
            request = CountRequest.of(s -> s
                .index(ElasticsearchClientManager.INDEX)
                .query(b0 -> b0.wrapper(b1 -> b1
                    .query(encodeJSON(jsonQuery)))));
        }
        CountResponse count = this.clientManager.getClient().count(request);
        return count.count();
    }

    @Override
    public List<Ping> searchInstalls(String jsonQuery) throws Exception
    {
        SearchRequest request;
        if (StringUtils.isEmpty(jsonQuery)) {
            request = SearchRequest.of(s -> s
                .index(ElasticsearchClientManager.INDEX));
        } else {
            request = SearchRequest.of(s -> s
                .index(ElasticsearchClientManager.INDEX)
                .query(b0 -> b0.wrapper(b1 -> b1
                    .query(encodeJSON(jsonQuery)))));
        }
        SearchResponse<Ping> search = this.clientManager.getClient().search(request, Ping.class);

        List<Ping> results = new ArrayList<>();
        if (!search.hits().hits().isEmpty()) {
            for (Hit<Ping> hit : search.hits().hits()) {
                results.add(hit.source());
            }
        }
        return results;
    }

    private String encodeJSON(String json)
    {
        return Base64.getEncoder().encodeToString(json.getBytes());
    }
}
