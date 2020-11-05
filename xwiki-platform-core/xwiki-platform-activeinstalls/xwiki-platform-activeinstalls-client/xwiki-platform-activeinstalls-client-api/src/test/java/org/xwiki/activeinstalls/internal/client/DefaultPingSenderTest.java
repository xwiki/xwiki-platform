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

import org.junit.jupiter.api.Test;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.google.gson.Gson;

import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPingSender}.
 *
 * @version $Id$
 * @since 5.2M2
 */
@ComponentTest
class DefaultPingSenderTest
{
    @InjectMockComponents
    private DefaultPingSender pingSender;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private JestClientManager jestManager;

    @Test
    void sendPing() throws Exception
    {
        JestClient client = mock(JestClient.class);
        DocumentResult indexResult = new DocumentResult(new Gson());
        indexResult.setSucceeded(true);
        when(client.execute(any(Index.class))).thenReturn(indexResult);

        when(this.jestManager.getClient()).thenReturn(client);

        PingDataProvider pingDataProvider = this.componentManager.registerMockComponent(PingDataProvider.class, "test");

        this.pingSender.sendPing();

        // Verify that provideMapping() and provideData() are called
        verify(pingDataProvider).provideMapping();
        verify(pingDataProvider).provideData();
    }

    @Test
    void sendPingWhenClientNull() throws Exception
    {
        when(jestManager.getClient()).thenReturn(null);

        // Register a data provider to make it available and to make sure it's not called, i.e. that a null client
        // will prevent a ping from being sent.
        PingDataProvider pingDataProvider = this.componentManager.registerMockComponent(PingDataProvider.class, "test");

        this.pingSender.sendPing();

        // Verify that provideMapping() and provideData() are not called
        verify(pingDataProvider, never()).provideMapping();
        verify(pingDataProvider, never()).provideData();
    }
}
