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
package org.xwiki.activeinstalls.internal.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.component.annotation.Component;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.mapping.PutMapping;
import net.sf.json.JSONObject;

/**
 * Default implementation using the Jest API to connect to a remote Elastic Search instance.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultPingSender implements PingSender
{
    @Inject
    private JestClientManager jestClientManager;

    @Inject
    private Provider<List<PingDataProvider>> pingDataProviderProvider;

    @Override
    public void sendPing() throws Exception
    {
        // Only send a ping if an ES client is available. Note that an empty ping URL disables the feature.
        JestClient client = this.jestClientManager.getClient();

        if (client != null) {
            // Step 1: Create index (if already exists then it'll just be ignored)
            client.execute(new CreateIndex.Builder(JestClientManager.INDEX).build());

            // Step 2: Create a mapping so that we can search distribution versions containing hyphens (otherwise they
            // are removed by the default tokenizer/analyzer). If mapping already exists then it'll just be ignored.
            PutMapping putMapping =
                new PutMapping.Builder(JestClientManager.INDEX, JestClientManager.TYPE, constructJSONMapping()).build();
            client.execute(putMapping);

            // Step 3: Index the data
            Index index = new Index.Builder(constructIndexJSON())
                .index(JestClientManager.INDEX)
                .type(JestClientManager.TYPE)
                .build();
            JestResult result = client.execute(index);

            if (!result.isSucceeded()) {
                throw new Exception(result.getErrorMessage());
            }
        }
    }

    private String constructJSONMapping()
    {
        Map<String, Object> jsonMap = new HashMap<>();

        Map<String, Object> timestampMap = new HashMap<>();
        timestampMap.put("enabled", true);
        timestampMap.put("store", true);

        Map<String, Object> propertiesMap = new HashMap<>();
        for (PingDataProvider pingDataProvider : this.pingDataProviderProvider.get()) {
            propertiesMap.putAll(pingDataProvider.provideMapping());
        }

        jsonMap.put("_timestamp", timestampMap);
        jsonMap.put("properties", propertiesMap);

        return JSONObject.fromObject(Collections.singletonMap(JestClientManager.TYPE, jsonMap)).toString();
    }

    private String constructIndexJSON()
    {
        Map<String, Object> jsonMap = new HashMap<>();

        for (PingDataProvider pingDataProvider : this.pingDataProviderProvider.get()) {
            jsonMap.putAll(pingDataProvider.provideData());
        }

        return JSONObject.fromObject(jsonMap).toString();
    }
}
