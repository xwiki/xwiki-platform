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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.client.data.ExtensionPingDataProvider;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import net.sf.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.activeinstalls.internal.client.data.ExtensionPingDataProvider}.
 *
 * @version $Id$
 * @since 6.1M1
 */
public class ExtensionPingDataProviderTest
{
    @Rule
    public MockitoComponentMockingRule<ExtensionPingDataProvider> mocker =
        new MockitoComponentMockingRule<>(ExtensionPingDataProvider.class);

    @Test
    public void provideMapping() throws Exception
    {
        assertEquals("{\"extensions\":{\"properties\":{"
                + "\"features\":{\"index\":\"not_analyzed\",\"type\":\"string\"},"
                + "\"id\":{\"index\":\"not_analyzed\","
                + "\"type\":\"string\"},\"version\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}}}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideMapping()).toString()
        );
    }

    @Test
    public void provideData() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("extensionid", "1.0");
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(extensionId);

        InstalledExtensionRepository repository = this.mocker.getInstance(InstalledExtensionRepository.class);
        when(repository.getInstalledExtensions()).thenReturn(Collections.singletonList(extension));

        assertEquals("{\"extensions\":[{\"id\":\"extensionid\",\"version\":\"1.0\"}]}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideData()).toString());
    }
}
