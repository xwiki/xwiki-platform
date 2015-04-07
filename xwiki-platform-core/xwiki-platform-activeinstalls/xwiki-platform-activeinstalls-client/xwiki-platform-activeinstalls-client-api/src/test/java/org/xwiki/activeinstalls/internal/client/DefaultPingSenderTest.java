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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.JestClientManager;
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
        new MockitoComponentMockingRule<>(DefaultPingSender.class);

    @Test
    public void sendPing() throws Exception
    {
        JestClient client = mock(JestClient.class);
        JestResult indexResult = new JestResult(new Gson());
        indexResult.setSucceeded(true);
        when(client.execute(any(Index.class))).thenReturn(indexResult);

        JestClientManager jestManager = this.mocker.getInstance(JestClientManager.class);
        when(jestManager.getClient()).thenReturn(client);

        PingDataProvider pingDataProvider = this.mocker.registerMockComponent(PingDataProvider.class, "test");

        this.mocker.getComponentUnderTest().sendPing();

        // Verify that provideMapping() and provideData() are called
        verify(pingDataProvider).provideMapping();
        verify(pingDataProvider).provideData();
    }
}
