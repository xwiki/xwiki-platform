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

import java.util.Map;
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
        Map<String, Object> mapping = this.mocker.getComponentUnderTest().provideMapping();
        assertEquals(3, mapping.size());

        Map<String, Object> propertiesMapping = (Map<String, Object>) mapping.get("distributionId");
        assertEquals(2, propertiesMapping.size());
        assertEquals("not_analyzed", propertiesMapping.get("index"));
        assertEquals("string", propertiesMapping.get("type"));

        propertiesMapping = (Map<String, Object>) mapping.get("distributionVersion");
        assertEquals(2, propertiesMapping.size());
        assertEquals("not_analyzed", propertiesMapping.get("index"));
        assertEquals("string", propertiesMapping.get("type"));

        propertiesMapping = (Map<String, Object>) mapping.get("instanceId");
        assertEquals(2, propertiesMapping.size());
        assertEquals("not_analyzed", propertiesMapping.get("index"));
        assertEquals("string", propertiesMapping.get("type"));
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

        Map<String, Object> data = this.mocker.getComponentUnderTest().provideData();
        assertEquals(3, data.size());
        assertEquals("environmentextensionid", data.get("distributionId"));
        assertEquals("2.0", data.get("distributionVersion"));
        assertEquals(id.getInstanceId(), data.get("instanceId"));
    }
}
