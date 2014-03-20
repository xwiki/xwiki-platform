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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.searchbox.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;

import java.util.Collections;
import java.util.UUID;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.google.gson.Gson;

/**
 * Unit tests for {@link DefaultPingSender}.
 *
 * @version $Id$
 * @since 5.2M2
 */
public class DefaultPingSenderTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultPingSender> mocker =
        new MockitoComponentMockingRule<DefaultPingSender>(DefaultPingSender.class);

    @Test
    public void sendPing() throws Exception
    {
        InstanceId id = new InstanceId(UUID.randomUUID().toString());
        InstanceIdManager idManager = this.mocker.getInstance(InstanceIdManager.class);
        when(idManager.getInstanceId()).thenReturn(id);

        ExtensionId extensionId = new ExtensionId("extensionid", "1.0");
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(extensionId);

        InstalledExtensionRepository repository = this.mocker.getInstance(InstalledExtensionRepository.class);
        when(repository.getInstalledExtensions()).thenReturn(Collections.singletonList(extension));

        JestClient client = mock(JestClient.class);
        JestResult result = new JestResult(new Gson());
        result.setSucceeded(true);
        when(client.execute(any(Action.class))).thenReturn(result);

        JestClientManager jestManager = this.mocker.getInstance(JestClientManager.class);
        when(jestManager.getClient()).thenReturn(client);

        ExtensionId environmentExtensionId = new ExtensionId("environmentextensionid", "2.0");
        CoreExtension environmentExtension = mock(CoreExtension.class);
        when(environmentExtension.getId()).thenReturn(environmentExtensionId);
        CoreExtensionRepository CoreExtensionRepository = this.mocker.getInstance(CoreExtensionRepository.class);
        when(CoreExtensionRepository.getEnvironmentExtension()).thenReturn(environmentExtension);

        this.mocker.getComponentUnderTest().sendPing();

        // Real test is here, we verify the data sent to the server.
        ArgumentCaptor<Index> index = ArgumentCaptor.forClass(Index.class);
        verify(client, times(3)).execute(index.capture());
        String jsonString = index.getValue().getData(new Gson()).toString();
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        assertEquals("1.0", json.getString("formatVersion"));
        assertNotNull(json.getString("date"));
        assertEquals("2.0", json.getString("distributionVersion"));
        assertEquals(1, json.getJSONArray("extensions").size());
        assertEquals("{\"id\":\"extensionid\",\"version\":\"1.0\"}", json.getJSONArray("extensions").get(0).toString());
    }
}
