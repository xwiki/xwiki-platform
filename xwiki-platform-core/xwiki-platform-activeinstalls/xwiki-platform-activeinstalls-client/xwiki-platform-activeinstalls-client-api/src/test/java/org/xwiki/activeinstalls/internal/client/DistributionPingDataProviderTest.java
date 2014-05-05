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
import org.xwiki.activeinstalls.internal.client.data.DistributionPingDataProvider;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import net.sf.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.activeinstalls.internal.client.data.DistributionPingDataProvider}.
 *
 * @version $Id$
 * @since 6.1M1
 */
public class DistributionPingDataProviderTest
{
    @Rule
    public MockitoComponentMockingRule<DistributionPingDataProvider> mocker =
        new MockitoComponentMockingRule<>(DistributionPingDataProvider.class);

    @Test
    public void provideMapping() throws Exception
    {
        assertEquals("{\"distributionId\":{\"index\":\"not_analyzed\",\"type\":\"string\"},"
                + "\"distributionVersion\":{\"index\":\"not_analyzed\",\"type\":\"string\"},"
                + "\"instanceId\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideMapping()).toString()
        );
    }

    @Test
    public void provideData() throws Exception
    {
        InstanceId id = new InstanceId(UUID.randomUUID().toString());
        InstanceIdManager idManager = this.mocker.getInstance(InstanceIdManager.class);
        when(idManager.getInstanceId()).thenReturn(id);

        ExtensionId environmentExtensionId = new ExtensionId("environmentextensionid", "2.0");
        CoreExtension environmentExtension = mock(CoreExtension.class);
        when(environmentExtension.getId()).thenReturn(environmentExtensionId);
        CoreExtensionRepository CoreExtensionRepository = this.mocker.getInstance(CoreExtensionRepository.class);
        when(CoreExtensionRepository.getEnvironmentExtension()).thenReturn(environmentExtension);

        assertEquals("{\"distributionId\":\"environmentextensionid\","
                + "\"distributionVersion\":\"2.0\","
                + "\"instanceId\":\"" + id.getInstanceId() + "\"}",
            JSONObject.fromObject(this.mocker.getComponentUnderTest().provideData()).toString()
        );
    }
}
