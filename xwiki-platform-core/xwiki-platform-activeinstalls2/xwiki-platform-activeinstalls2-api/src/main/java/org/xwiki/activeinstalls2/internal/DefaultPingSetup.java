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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.IndexSettings;

/**
 * Prepare the ElasticSearch server to be ready to accept pings (e.g. create the ingest pipelines, create the
 * index and mapping, etc). Note that this is not done inside {@link PingSender#sendPing()} since it's only needed
 * to be done once, and it requires appropriate permissions on the ES instance side.
 *
 * @version $Id$
 * @since 14.6
 */
@Component
@Singleton
public class DefaultPingSetup implements PingSetup
{
    @Inject
    private ElasticsearchClientManager clientManager;

    @Inject
    private Provider<List<PingDataProvider>> pingDataProviderProvider;

    /**
     * @throws Exception in case an error happened during the preparation
     */
    @Override
    public void setup() throws Exception
    {
        // Only send a ping if a client is available. Note that an empty ping URL disables the feature.
        ElasticsearchClient client = this.clientManager.getClient();

        if (client != null) {
            // Step 1: Perform setups (if need be)
            setup(client);

            // Step 2: Create index (if already exists then it'll just be ignored)
            createIndex(client);

            // Step 3: Create a mapping. If such a mapping already exists then it'll just be ignored or overwritten.
            client.indices().putMapping(builder -> builder
                .index(ElasticsearchClientManager.INDEX)
                .properties(provideMapping()));
        }
    }

    private void setup(ElasticsearchClient client) throws Exception
    {
        for (PingDataProvider pingDataProvider : this.pingDataProviderProvider.get()) {
            pingDataProvider.setup(client);
        }
    }

    private void createIndex(ElasticsearchClient client) throws Exception
    {
        try {
            client.indices().create(builder -> builder
                .index(ElasticsearchClientManager.INDEX)
                .settings(provideIndexSettings()));
        } catch (ElasticsearchException e) {
            // Don't fail if the index already exists, just ignore it.
            if (!e.response().error().type().equals("resource_already_exists_exception")) {
                throw e;
            }
        }
    }

    private IndexSettings provideIndexSettings()
    {
        IndexSettings.Builder builder = new IndexSettings.Builder();
        for (PingDataProvider pingDataProvider : this.pingDataProviderProvider.get()) {
            pingDataProvider.provideIndexSettings().apply(builder);
        }
        return builder.build();
    }

    private Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        for (PingDataProvider pingDataProvider : this.pingDataProviderProvider.get()) {
            propertiesMap.putAll(pingDataProvider.provideMapping());
        }
        return propertiesMap;
    }
}
