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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.activeinstalls2.DataManager;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.component.annotation.Component;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
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
    /**
     * The field holding the id uniquely identifying an XWiki instance across its pings.
     */
    private static final String INSTANCE_ID_FIELD = "distribution.instanceId";

    /**
     * The field holding the installed extensions, mapped as a nested type so that each entry's id, version and
     * features stay indexed together.
     */
    private static final String EXTENSIONS_FIELD = "extensions";

    private static final String EXTENSION_ID_FIELD = "extensions.id";

    /**
     * The number of extensions a single {@link #countDistinctInstallsByExtension(String)} call can report. A terms
     * aggregation silently drops the extensions that don't fit in its size, so going over this is reported as an
     * error rather than as an under-count.
     */
    private static final int EXTENSION_LIMIT = 10000;

    private static final String DISTINCT_INSTALLS_AGGREGATION = "distinctInstalls";

    private static final String NESTED_EXTENSIONS_AGGREGATION = "nestedExtensions";

    private static final String EXTENSION_IDS_AGGREGATION = "extensionIds";

    private static final String ROOT_AGGREGATION = "root";

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
        SearchRequest request = SearchRequest.of(s -> {
            s.index(ElasticsearchClientManager.INDEX);
            return applyQuery(s, jsonQuery);
        });
        SearchResponse<Ping> search = this.clientManager.getClient().search(request, Ping.class);

        List<Ping> results = new ArrayList<>();
        if (!search.hits().hits().isEmpty()) {
            for (Hit<Ping> hit : search.hits().hits()) {
                results.add(hit.source());
            }
        }
        return results;
    }

    @Override
    public long countDistinctInstalls(String jsonQuery) throws Exception
    {
        SearchRequest request = SearchRequest.of(s -> {
            s.index(ElasticsearchClientManager.INDEX)
                // Only the aggregation result is needed, so don't pay for fetching any hit.
                .size(0)
                .aggregations(DISTINCT_INSTALLS_AGGREGATION, b0 -> b0
                    .cardinality(b1 -> b1.field(INSTANCE_ID_FIELD)));
            return applyQuery(s, jsonQuery);
        });
        SearchResponse<Ping> search = this.clientManager.getClient().search(request, Ping.class);

        return search.aggregations().get(DISTINCT_INSTALLS_AGGREGATION).cardinality().value();
    }

    @Override
    public Map<String, Long> countDistinctInstallsByExtension(String jsonQuery) throws Exception
    {
        SearchRequest request = SearchRequest.of(s -> {
            s.index(ElasticsearchClientManager.INDEX)
                .size(0)
                .aggregations(NESTED_EXTENSIONS_AGGREGATION, b0 -> b0
                    .nested(b1 -> b1.path(EXTENSIONS_FIELD))
                    .aggregations(EXTENSION_IDS_AGGREGATION, b1 -> b1
                        .terms(b2 -> b2.field(EXTENSION_ID_FIELD).size(EXTENSION_LIMIT))
                        // The instance id is on the root document and not on the nested extension, so step back out
                        // of the nested context before counting the distinct instances.
                        .aggregations(ROOT_AGGREGATION, b2 -> b2
                            .reverseNested(b3 -> b3)
                            .aggregations(DISTINCT_INSTALLS_AGGREGATION, b3 -> b3
                                .cardinality(b4 -> b4.field(INSTANCE_ID_FIELD))))));
            return applyQuery(s, jsonQuery);
        });
        SearchResponse<Ping> search = this.clientManager.getClient().search(request, Ping.class);

        StringTermsAggregate extensionIds = search.aggregations().get(NESTED_EXTENSIONS_AGGREGATION).nested()
            .aggregations().get(EXTENSION_IDS_AGGREGATION).sterms();
        Long droppedExtensions = extensionIds.sumOtherDocCount();
        if (droppedExtensions != null && droppedExtensions > 0) {
            throw new Exception(String.format("Found more than the [%s] extensions that can be counted at once",
                EXTENSION_LIMIT));
        }

        Map<String, Long> counts = new HashMap<>();
        for (StringTermsBucket bucket : extensionIds.buckets().array()) {
            counts.put(bucket.key().stringValue(), bucket.aggregations().get(ROOT_AGGREGATION).reverseNested()
                .aggregations().get(DISTINCT_INSTALLS_AGGREGATION).cardinality().value());
        }
        return counts;
    }

    /**
     * Restricts a search to the pings matching the passed query. Note that the query is the only part of the request
     * the caller controls: it cannot alter the aggregations computed on top of it.
     */
    private SearchRequest.Builder applyQuery(SearchRequest.Builder builder, String jsonQuery)
    {
        if (StringUtils.isEmpty(jsonQuery)) {
            return builder;
        }
        return builder.query(b0 -> b0.wrapper(b1 -> b1.query(encodeJSON(jsonQuery))));
    }

    private String encodeJSON(String json)
    {
        return Base64.getEncoder().encodeToString(json.getBytes());
    }
}
