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
package org.xwiki.livedata.script.livetable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.livetable.LiveTableConfiguration;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategyManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableScriptService}.
 * 
 * @version $Id$
 */
@ComponentTest
class LiveTableScriptServiceTest
{
    @InjectMockComponents
    private LiveTableScriptService service;

    @MockComponent
    @Named("liveTable")
    private LiveDataConfigurationResolver<LiveTableConfiguration> liveTableLiveDataConfigResolver;

    @MockComponent
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigurationResolver;

    @MockComponent
    private LiveTableNewRowNamingStrategyManager namingStrategyManager;

    @Test
    void getConfig() throws Exception
    {
        List<String> columns = Arrays.asList("doc.title", "doc.author");
        Map<String, Object> columnProperties = Collections.singletonMap("doc.title", "{...}");
        Map<String, Object> options = Collections.singletonMap("className", "Path.To.Class");

        ArgumentCaptor<LiveTableConfiguration> liveTableConfigCaptor =
            ArgumentCaptor.forClass(LiveTableConfiguration.class);
        LiveDataConfiguration partialConfig = new LiveDataConfiguration();
        when(this.liveTableLiveDataConfigResolver.resolve(liveTableConfigCaptor.capture())).thenReturn(partialConfig);

        LiveDataConfiguration config = new LiveDataConfiguration();
        when(this.defaultLiveDataConfigurationResolver.resolve(partialConfig)).thenReturn(config);

        assertSame(config, this.service.getConfig("test", columns, columnProperties, options));

        LiveTableConfiguration liveTableConfig = liveTableConfigCaptor.getValue();
        assertEquals("test", liveTableConfig.getId());
        assertSame(columns, liveTableConfig.getColumns());
        assertSame(columnProperties, liveTableConfig.getColumnProperties());
        assertSame(options, liveTableConfig.getOptions());
    }

    @Test
    void getNewRowNamingStrategies()
    {
        LiveTableNewRowNamingStrategy title = mock();
        when(title.getMandatoryProperties(Map.of())).thenReturn(Set.of("doc.title"));
        when(title.getDescription()).thenReturn("Use the title");
        Map<String, LiveTableNewRowNamingStrategy> installed = new TreeMap<>();
        installed.put("title", title);
        installed.put("uuid", mock());
        when(this.namingStrategyManager.getAll()).thenReturn(installed);

        Map<String, NewRowNamingStrategyDescriptor> strategies = this.service.getNewRowNamingStrategies();

        assertEquals(List.of("title", "uuid"), List.copyOf(strategies.keySet()));
        assertEquals("Use the title", strategies.get("title").description());
        assertEquals(Set.of("doc.title"), strategies.get("title").mandatoryProperties());
        assertNull(strategies.get("uuid").description());
        assertEquals(Set.of(), strategies.get("uuid").mandatoryProperties());
    }
}
