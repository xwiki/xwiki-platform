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
import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import net.sf.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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
    public void provideMapping() throws Exception
    {
        assertEquals("{\"sinceDays\":{\"type\":\"long\"},\"firstPingDate\":{\"type\":\"date\"}}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideMapping()).toString());
    }

    @Test
    public void provideData() throws Exception
    {
        InstanceId id = new InstanceId(UUID.randomUUID().toString());
        InstanceIdManager idManager = this.mocker.getInstance(InstanceIdManager.class);
        when(idManager.getInstanceId()).thenReturn(id);

        JestClient client = mock(JestClient.class);
        JestResult searchResult = new JestResult(new Gson());
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

        assertEquals("{\"sinceDays\":4,\"firstPingDate\":1392854400000}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideData()).toString());
    }
}
