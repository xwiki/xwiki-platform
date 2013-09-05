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
import java.util.UUID;

import org.junit.*;
import org.mockito.ArgumentCaptor;
import org.xwiki.activeinstalls.ActiveInstallsConfiguration;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.instance.InstanceId;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import io.searchbox.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActiveInstallsPingThread}.
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsPingThreadTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @Test
    public void sendPing() throws Exception
    {
        InstanceId id = new InstanceId(UUID.randomUUID().toString());
        ActiveInstallsConfiguration configuration = mock(ActiveInstallsConfiguration.class);

        ExtensionId extensionId = new ExtensionId("extensionid", "1.0");
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(extensionId);

        InstalledExtensionRepository repository = mock(InstalledExtensionRepository.class);
        when(repository.getInstalledExtensions()).thenReturn(Collections.singletonList(extension));

        JestClient client = mock(JestClient.class);
        JestResult result = new JestResult();
        result.setSucceeded(true);
        when(client.execute(any(Action.class))).thenReturn(result);

        JestClientManager jestManager = mock(JestClientManager.class);
        when(jestManager.getClient()).thenReturn(client);

        ActiveInstallsPingThread thread = new ActiveInstallsPingThread(id, configuration, repository, jestManager);
        thread.sendPing();

        // Real test is here, we verify the data sent to the server.
        ArgumentCaptor<Index> index = ArgumentCaptor.forClass(Index.class);
        verify(client).execute(index.capture());
        String jsonString = index.getValue().getData().toString();
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        assertEquals("1.0", json.getString("formatVersion"));
        assertNotNull(json.getString("date"));
        assertEquals(1, json.getJSONArray("extensions").size());
        assertEquals("{\"id\":\"extensionid\",\"version\":\"1.0\"}", json.getJSONArray("extensions").get(0).toString());
    }
}
