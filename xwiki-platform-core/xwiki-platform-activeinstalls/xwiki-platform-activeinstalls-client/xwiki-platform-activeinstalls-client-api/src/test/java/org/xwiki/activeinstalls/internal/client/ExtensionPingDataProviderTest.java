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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.internal.client.data.ExtensionPingDataProvider;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import net.sf.json.JSONArray;
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
        Map<String, Object> mapping = this.mocker.getComponentUnderTest().provideMapping();
        assertEquals(1, mapping.size());

        Map<String, Object> extensionsMapping = (Map<String, Object>) mapping.get("extensions");
        assertEquals(1, extensionsMapping.size());

        Map<String, Object> propertiesMapping = (Map<String, Object>) extensionsMapping.get("properties");
        assertEquals(3, propertiesMapping.size());

        Map<String, Object> propertyMapping = (Map<String, Object>) propertiesMapping.get("id");
        assertEquals(2, propertyMapping.size());
        assertEquals("not_analyzed", propertyMapping.get("index"));
        assertEquals("string", propertyMapping.get("type"));

        propertyMapping = (Map<String, Object>) propertiesMapping.get("version");
        assertEquals(2, propertyMapping.size());
        assertEquals("not_analyzed", propertyMapping.get("index"));
        assertEquals("string", propertyMapping.get("type"));

        propertyMapping = (Map<String, Object>) propertiesMapping.get("features");
        assertEquals(2, propertyMapping.size());
        assertEquals("not_analyzed", propertyMapping.get("index"));
        assertEquals("string", propertyMapping.get("type"));
    }

    @Test
    public void provideData() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("extensionid", "1.0");
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(extensionId);
        when(extension.getFeatures()).thenReturn(Arrays.asList("feature1", "feature2"));

        InstalledExtensionRepository repository = this.mocker.getInstance(InstalledExtensionRepository.class);
        when(repository.getInstalledExtensions()).thenReturn(Collections.singletonList(extension));

        Map<String, Object> data = this.mocker.getComponentUnderTest().provideData();
        assertEquals(1, data.size());
        JSONObject[] extensions = (JSONObject[]) data.get("extensions");
        assertEquals(1, extensions.length);
        JSONObject propertiesData = extensions[0];
        assertEquals(3, propertiesData.size());
        assertEquals("extensionid", propertiesData.get("id"));
        assertEquals("1.0", propertiesData.get("version"));
        JSONArray features = (JSONArray) propertiesData.get("features");
        assertEquals(2, features.size());
        assertEquals("feature1", features.get(0));
        assertEquals("feature2", features.get(1));
    }
}
