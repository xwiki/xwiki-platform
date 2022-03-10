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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.livetable.LiveTableConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
}
