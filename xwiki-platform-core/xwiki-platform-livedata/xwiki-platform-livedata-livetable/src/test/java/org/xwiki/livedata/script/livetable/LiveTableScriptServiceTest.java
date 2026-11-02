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
import java.util.stream.Stream;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.livetable.LiveTableConfiguration;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private LiveTableScriptService service;

    @MockComponent
    @Named("liveTable")
    private LiveDataConfigurationResolver<LiveTableConfiguration> liveTableLiveDataConfigResolver;

    @MockComponent
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigurationResolver;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

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
    void getNewRowNamingStrategies() throws Exception
    {
        LiveTableNewRowNamingStrategy title = mock();
        when(title.getMandatoryProperties(Map.of())).thenReturn(Set.of("doc.title"));
        givenStrategies("uuid", "broken", "title");
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "uuid"))
            .thenReturn(mock(LiveTableNewRowNamingStrategy.class));
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "broken"))
            .thenThrow(new ComponentLookupException("broken"));
        when(this.contextComponentManager.getInstance(LiveTableNewRowNamingStrategy.class, "title")).thenReturn(title);

        Map<String, Set<String>> strategies = this.service.getNewRowNamingStrategies();

        assertEquals(List.of("title", "uuid"), List.copyOf(strategies.keySet()));
        assertEquals(Set.of("doc.title"), strategies.get("title"));
        assertEquals(Set.of(), strategies.get("uuid"));
        assertEquals("Failed to load the new row naming strategy [broken]. Root cause is "
            + "[ComponentLookupException: broken].", this.logCapture.getMessage(0));
    }

    private void givenStrategies(String... hints)
    {
        List<ComponentDescriptor<LiveTableNewRowNamingStrategy>> descriptors = Stream.of(hints).map(hint -> {
            DefaultComponentDescriptor<LiveTableNewRowNamingStrategy> descriptor = new DefaultComponentDescriptor<>();
            descriptor.setRoleType(LiveTableNewRowNamingStrategy.class);
            descriptor.setRoleHint(hint);
            return (ComponentDescriptor<LiveTableNewRowNamingStrategy>) descriptor;
        }).toList();
        when(this.contextComponentManager.getComponentDescriptorList(LiveTableNewRowNamingStrategy.class))
            .thenReturn(descriptors);
    }
}
