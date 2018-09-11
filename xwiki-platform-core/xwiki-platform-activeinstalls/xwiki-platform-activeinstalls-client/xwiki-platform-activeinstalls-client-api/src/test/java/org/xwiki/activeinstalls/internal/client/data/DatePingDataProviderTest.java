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
package org.xwiki.activeinstalls.internal.client.data;

import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.activeinstalls.internal.client.data.DatePingDataProvider;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.activeinstalls.internal.client.data.DatePingDataProvider}.
 *
 * @version $Id$
 * @since 6.1M1
 */
public class DatePingDataProviderTest
{
    @Rule
    public MockitoComponentMockingRule<DatePingDataProvider> mocker =
        new MockitoComponentMockingRule<>(DatePingDataProvider.class);

    @Test
    public void constructSearchJson() {
        String instanceId = "c5471b1d-bc03-4e35-b11e-ac89db67a3a1";
        String searchJson = DatePingDataProvider.constructSearchJSON(instanceId);
        String expectedResult = "{\"query\":{\"term\":{\"instanceId\":\"c5471b1d-bc03-4e35-b11e-ac89db67a3a1\"}},\"aggs\":{\"firstPingDate\":{\"min\":{\"field\":\"_timestamp\"}},\"serverTime\":{\"min\":{\"script\":\"time()\"}}}}";
        assertEquals(expectedResult, searchJson);
    }

    @Test
    public void provideMapping() throws Exception
    {
        Map<String, Object> mapping = this.mocker.getComponentUnderTest().provideMapping();
        assertEquals(2, mapping.size());

        Map<String, Object> propertiesMapping = (Map<String, Object>) mapping.get("sinceDays");
        assertEquals(1, propertiesMapping.size());
        assertEquals("long", propertiesMapping.get("type"));

        propertiesMapping = (Map<String, Object>) mapping.get("firstPingDate");
        assertEquals(1, propertiesMapping.size());
        assertEquals("date", propertiesMapping.get("type"));
    }

    @Test
    public void provideData() throws Exception
    {
        InstanceId id = new InstanceId(UUID.randomUUID().toString());
        InstanceIdManager idManager = this.mocker.getInstance(InstanceIdManager.class);
        when(idManager.getInstanceId()).thenReturn(id);

        JestClient client = mock(JestClient.class);
        SearchResult searchResult = new SearchResult(new Gson());
        String resultString = "{\n" +
            "   \"took\": 4,\n" +
            "   \"timed_out\": false,\n" +
            "   \"_shards\": {\n" +
            "      \"total\": 5,\n" +
            "      \"successful\": 5,\n" +
            "      \"failed\": 0\n" +
            "   },\n" +
            "   \"hits\": {\n" +
            "      \"total\": 2,\n" +
            "      \"max_score\": 0,\n" +
            "      \"hits\": []\n" +
            "   },\n" +
            "   \"aggregations\": {\n" +
            "      \"firstPingDate\": {\n" +
            "         \"value\": 1392854400000\n" +
            "      },\n" +
            "      \"serverTime\": {\n" +
            "         \"value\": 1393200000000\n" +
            "      }\n" +
            "   }\n" +
            "}";
        searchResult.setJsonString(resultString);
        searchResult.setJsonObject(new JsonParser().parse(resultString).getAsJsonObject());
        searchResult.setSucceeded(true);
        when(client.execute(any(Search.class))).thenReturn(searchResult);

        JestClientManager jestManager = this.mocker.getInstance(JestClientManager.class);
        when(jestManager.getClient()).thenReturn(client);

        Map<String, Object> data = this.mocker.getComponentUnderTest().provideData();
        assertEquals(2, data.size());
        assertEquals(4L, data.get("sinceDays"));
        assertEquals(1392854400000L, data.get("firstPingDate"));
    }
}
