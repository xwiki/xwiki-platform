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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.activeinstalls2.DataManager;
import org.xwiki.activeinstalls2.TooManyExtensionsException;
import org.xwiki.activeinstalls2.internal.data.DistributionPingDataProvider;
import org.xwiki.activeinstalls2.internal.data.ExtensionPingDataProvider;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.component.annotation.Component;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
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
     * The maximum number of extensions {@link #countDistinctInstallsByExtension(String)} reports, above which it
     * raises a {@link TooManyExtensionsException} rather than under-reporting. Counting one extension costs 2
     * Elasticsearch aggregation buckets, so this keeps a query well within Elasticsearch's own search.max_buckets
     * (65536 by default).
     */
    static final int MAX_EXTENSION_COUNT_PER_QUERY = 10000;

    /**
     * The number of distinct instances up to which a count is exact, above which it becomes a HyperLogLog++ estimate.
     * This is the highest precision threshold Elasticsearch supports, and the whole instance population is well
     * below it, so the counts are exact in practice. HyperLogLog++ keeps its sparse representation until the observed
     * count approaches the threshold, so the memory a count actually uses follows that count rather than this bound.
     */
    static final int PRECISION_THRESHOLD = 40000;

    private static final String DISTINCT_INSTALLS_AGGREGATION = "distinctInstalls";

    private static final String NESTED_EXTENSIONS_AGGREGATION = "nestedExtensions";

    private static final String EXTENSION_IDS_AGGREGATION = "extensionIds";

    private static final String ROOT_AGGREGATION = "root";

    @Inject
    private ElasticsearchClientManager clientManager;

    @Override
    public long countInstalls(String jsonQuery) throws Exception
    {
        CountRequest request = CountRequest.of(s -> {
            s.index(ElasticsearchClientManager.INDEX);
            if (!StringUtils.isEmpty(jsonQuery)) {
                s.query(toQuery(jsonQuery));
            }
            return s;
        });
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
                    .cardinality(b1 -> b1
                        .field(DistributionPingDataProvider.FIELD_INSTANCE_ID)
                        .precisionThreshold(PRECISION_THRESHOLD)));
            return applyQuery(s, jsonQuery);
        });
        SearchResponse<Ping> search = this.clientManager.getClient().search(request, Ping.class);

        return search.aggregations().get(DISTINCT_INSTALLS_AGGREGATION).cardinality().value();
    }

    @Override
    public SequencedMap<String, Long> countDistinctInstallsByExtension(String jsonQuery) throws Exception
    {
        // Ask the terms aggregation for one extension more than can be reported, so that getting that extra one back
        // is what tells that there are more extensions than the maximum. This is exact whatever the number of shards
        // of the index: when the index holds no more extensions than the maximum, every shard holds fewer of them
        // than the shard_size Elasticsearch derives from the requested size, and thus reports all of the ones it
        // holds, so nothing can be missing from the merged result.
        int requestedExtensionCount = MAX_EXTENSION_COUNT_PER_QUERY + 1;
        SearchRequest request = SearchRequest.of(s -> {
            s.index(ElasticsearchClientManager.INDEX)
                .size(0)
                .aggregations(NESTED_EXTENSIONS_AGGREGATION, b0 -> b0
                    .nested(b1 -> b1.path(ExtensionPingDataProvider.PROPERTY_EXTENSIONS))
                    .aggregations(EXTENSION_IDS_AGGREGATION, b1 -> b1
                        .terms(b2 -> b2
                            .field(ExtensionPingDataProvider.FIELD_EXTENSION_ID)
                            .size(requestedExtensionCount))
                        // The instance id is on the root document and not on the nested extension, so step back out
                        // of the nested context before counting the distinct instances.
                        .aggregations(ROOT_AGGREGATION, b2 -> b2
                            .reverseNested(b3 -> b3)
                            .aggregations(DISTINCT_INSTALLS_AGGREGATION, b3 -> b3
                                .cardinality(b4 -> b4
                                    .field(DistributionPingDataProvider.FIELD_INSTANCE_ID)
                                    .precisionThreshold(PRECISION_THRESHOLD))))));
            return applyQuery(s, jsonQuery);
        });
        SearchResponse<Ping> search = this.clientManager.getClient().search(request, Ping.class);

        StringTermsAggregate extensionIds = search.aggregations().get(NESTED_EXTENSIONS_AGGREGATION).nested()
            .aggregations().get(EXTENSION_IDS_AGGREGATION).sterms();
        List<StringTermsBucket> buckets = extensionIds.buckets().array();
        // Report the failure rather than only some of the extensions, which the caller has no way of telling apart
        // from a complete answer.
        if (buckets.size() > MAX_EXTENSION_COUNT_PER_QUERY) {
            throw new TooManyExtensionsException(MAX_EXTENSION_COUNT_PER_QUERY);
        }

        // The terms aggregation returns its buckets ordered by descending number of matching pings, so use a map
        // preserving that order rather than losing it. Note that this is not exactly the order of the counts below,
        // since a ping count is not a distinct instance count.
        SequencedMap<String, Long> counts = new LinkedHashMap<>();
        for (StringTermsBucket bucket : buckets) {
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
        return builder.query(toQuery(jsonQuery));
    }

    /**
     * Wraps a caller-provided JSON query into an Elasticsearch query. The JSON is base64-encoded whole into a
     * {@code wrapper} query rather than concatenated into a larger document, so that it cannot escape the slot it is
     * given and reshape the rest of the request.
     */
    private Query toQuery(String jsonQuery)
    {
        return Query.of(b0 -> b0.wrapper(b1 -> b1.query(encodeJSON(jsonQuery))));
    }

    private String encodeJSON(String json)
    {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
