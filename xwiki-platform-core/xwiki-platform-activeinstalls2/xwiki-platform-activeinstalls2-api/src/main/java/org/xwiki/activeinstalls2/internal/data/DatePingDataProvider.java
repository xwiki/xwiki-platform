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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls2.internal.ElasticsearchClientManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.instance.InstanceIdManager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.ingest.Processor;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;

/**
 * Provide the ping date, the date of the first ping and the elapsed days since the first ping. We do that to make
 * it simpler to perform complex queries on the ping data later on (for example to be able to query the average
 * duration an instance is used: < 1 day, 2-7 days, 7-30 days, 30-365 days, > 365 days).
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("date")
@Singleton
public class DatePingDataProvider extends AbstractPingDataProvider
{
    private static final String PIPELINE_NAME = "set-timestamp";

    private static final String PROPERTY_PING_DATE = "current";

    private static final String PROPERTY_FIRST_PING_DATE = "first";

    private static final String PROPERTY_SINCE_DAYS = "since";

    private static final String PROPERTY_SERVER_TIME = "serverTime";

    private static final String PROPERTY_DATE = "date";

    @Inject
    private ElasticsearchClientManager clientManager;

    @Inject
    private InstanceIdManager instanceIdManager;

    @Override
    public void setup(ElasticsearchClient client) throws Exception
    {
        // Note: if the pipeline already exists, it'll be overwritten or ignored.
        Processor processor1 = Processor.of(b0 -> b0
            .set(b1 -> b1
                .field(getDateKey(PROPERTY_PING_DATE))
                .value(JsonData.of("{{{_ingest.timestamp}}}"))));
        Processor processor2 = Processor.of(b0 -> b0
            .set(b3 -> b3
                // Don't overwrite if a first ping date is set
                .override(false)
                .field(getDateKey(PROPERTY_FIRST_PING_DATE))
                .value(JsonData.of(String.format("{{{%s}}}", getDateKey(PROPERTY_PING_DATE))))));
        client.ingest().putPipeline(b0 -> b0
            .id(PIPELINE_NAME)
            .description("Set current date to be the server date/time and fill first ping date if empty")
            .processors(processor1, processor2));
    }

    @Override
    public Function<IndexSettings.Builder, ObjectBuilder<IndexSettings>> provideIndexSettings()
    {
        // Make the PIPELINE_NAME pipeline active by default for the Ping Index.
        return b0 -> b0
            .index(b1 -> b1
                .defaultPipeline(PIPELINE_NAME));
    }

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_PING_DATE, Property.of(b1 -> b1.date(b2 -> b2)));
        propertiesMap.put(PROPERTY_FIRST_PING_DATE, Property.of(b1 -> b1.date(b2 -> b2)));
        propertiesMap.put(PROPERTY_SINCE_DAYS, Property.of(b1 -> b1.long_(b2 -> b2)));

        return Collections.singletonMap(PROPERTY_DATE, Property.of(b0 -> b0.object(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        // Notes:
        // 1) The "pingDate" field value is always set by an ingest pipeline processor, see #provideMapping. We do
        //    this to always use the ES server's date and not rely on the client date which could be off for various
        //    reasons (badly set, bad timezones, etc).
        // 2) There's also another ingest pipeline processor to set the "firstPingDate" when it's empty. In this case
        //    the value from the "pingDate" field is copied.

        try {
            // Find the first Ping entry for the current instance id, and extract the firstPingDate and server time
            // to set the sinceDays and firstPingDate for the new ping.
            SearchResponse<Ping> search = this.clientManager.getClient().search(s -> s
                .index(ElasticsearchClientManager.INDEX)
                .query(q -> q
                    .term(t -> t
                        .field(getDistributionKey(DistributionPingDataProvider.PROPERTY_INSTANCE_ID))
                        .value(v -> v.stringValue(this.instanceIdManager.getInstanceId().toString()))
                    ))
                .aggregations(getDateKey(PROPERTY_SERVER_TIME), a1 -> a1
                    .min(m -> m.script(s2 -> s2.inline(i -> i.source("new Date().getTime()")))))
                .aggregations(getDateKey(PROPERTY_FIRST_PING_DATE), a2 -> a2
                    .min(m2 -> m2.field(getDateKey(PROPERTY_FIRST_PING_DATE)))),
                Ping.class);

            DatePing datePing = new DatePing();
            if (!search.hits().hits().isEmpty()) {
                Aggregate serverTimeAggregate = search.aggregations().get(getDateKey(PROPERTY_SERVER_TIME));
                Aggregate firstPingDateAggregate = search.aggregations().get(getDateKey(PROPERTY_FIRST_PING_DATE));
                // If firstPingDateAggregate min value is 0 then we consider it means there's no hit found.
                if (firstPingDateAggregate != null && serverTimeAggregate != null) {
                    double serverTime = serverTimeAggregate.min().value();
                    double firstPingDate = firstPingDateAggregate.min().value();
                    long sinceDays = Math.round((serverTime - firstPingDate) / 86400000D);
                    datePing.setSince(sinceDays);
                    datePing.setFirst(new Date(Math.round(firstPingDate)));
                } else {
                    // This means it's the first ping and thus there was no previous _timestamp. Thus, we set the since
                    // Days to 0. Note that in this case the firstPingDate is set on the server by Elasticsearch
                    // using an ingest pipeline.
                    datePing.setSince(0);
                }
            } else {
                // This is the first ping, see above for details.
                datePing.setSince(0);
            }
            ping.setDate(datePing);
        } catch (Exception e) {
            // If this fails we just don't send this information, but we still send the other piece of information.
            // However, we log a warning since it's a problem that needs to be seen and looked at.
            logWarning("Failed to compute the first ping date and the number of elapsed days since the first ping", e);
        }
    }

    private String getDateKey(String suffix)
    {
        return getKey(PROPERTY_DATE, suffix);
    }

    private String getDistributionKey(String suffix)
    {
        return getKey(DistributionPingDataProvider.PROPERTY_DISTRIBUTION, suffix);
    }

    private String getKey(String prefix, String suffix)
    {
        return String.format("%s.%s", prefix, suffix);
    }
}
