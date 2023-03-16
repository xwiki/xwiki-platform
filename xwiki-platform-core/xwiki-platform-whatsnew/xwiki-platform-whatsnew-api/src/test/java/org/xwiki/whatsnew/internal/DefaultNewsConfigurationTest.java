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
package org.xwiki.whatsnew.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.whatsnew.NewsSourceDescriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNewsConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultNewsConfigurationTest
{
    @InjectMockComponents
    private DefaultNewsConfiguration configuration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Test
    void getNewsSourceDescriptorsWhenNoConfiguration()
    {
        when(this.configurationSource.containsKey("whatsnew.sources")).thenReturn(false);

        List< NewsSourceDescriptor> descriptors = this.configuration.getNewsSourceDescriptors();

        assertEquals(2, descriptors.size());
        assertTrue(descriptors.contains(new NewsSourceDescriptor("xwikiorg", "xwikiblog",
            Collections.singletonMap("rssURL", "https://extensions.xwiki.org/news"))));
        assertTrue(descriptors.contains(new NewsSourceDescriptor("xwikisas", "xwikiblog",
            Collections.singletonMap("rssURL", "https://xwiki.com/news"))));
    }

    @Test
    void getNewsSourceDescriptorsWhenEmptyConfiguration()
    {
        when(this.configurationSource.containsKey("whatsnew.sources")).thenReturn(true);
        when(this.configurationSource.getProperty("whatsnew.sources", Properties.class))
            .thenReturn(new Properties());

        List< NewsSourceDescriptor> descriptors = this.configuration.getNewsSourceDescriptors();

        assertEquals(0, descriptors.size());
    }

    @Test
    void getNewsSourceDescriptorsWithNoParameters()
    {
        Properties data = new Properties();
        data.setProperty("sourceid1", "sourcetype1");
        data.setProperty("sourceid2", "sourcetype2");
        when(this.configurationSource.containsKey("whatsnew.sources")).thenReturn(true);
        when(this.configurationSource.getProperty("whatsnew.sources", Properties.class)).thenReturn(data);
        when(this.configurationSource.getKeys()).thenReturn(List.of("whatever", "whatsnew.sources"));

        List< NewsSourceDescriptor> descriptors = this.configuration.getNewsSourceDescriptors();

        assertEquals(2, descriptors.size());
        assertTrue(descriptors.contains(new NewsSourceDescriptor("sourceid1", "sourcetype1", Collections.emptyMap())));
        assertTrue(descriptors.contains(new NewsSourceDescriptor("sourceid2", "sourcetype2", Collections.emptyMap())));
    }

    @Test
    void getNewsSourceDescriptorsWithParameters()
    {
        Properties data = new Properties();
        data.setProperty("sourceid1", "sourcetype1");
        data.setProperty("sourceid2", "sourcetype2");
        when(this.configurationSource.containsKey("whatsnew.sources")).thenReturn(true);
        when(this.configurationSource.getProperty("whatsnew.sources", Properties.class)).thenReturn(data);
        when(this.configurationSource.getKeys()).thenReturn(List.of("whatever", "whatsnew.sources",
            "whatsnew.source.sourceid1.a", "whatsnew.source.sourceid1.aa", "whatsnew.source.sourceid2.c"));
        when(this.configurationSource.getProperty("whatsnew.source.sourceid1.a", String.class)).thenReturn("b");
        when(this.configurationSource.getProperty("whatsnew.source.sourceid1.aa", String.class)).thenReturn("bb");
        when(this.configurationSource.getProperty("whatsnew.source.sourceid2.c", String.class)).thenReturn("d");

        List< NewsSourceDescriptor> descriptors = this.configuration.getNewsSourceDescriptors();

        assertEquals(2, descriptors.size());
        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "b");
        parameters.put("aa", "bb");
        assertTrue(descriptors.contains(new NewsSourceDescriptor("sourceid1", "sourcetype1", parameters)));
        assertTrue(descriptors.contains(new NewsSourceDescriptor("sourceid2", "sourcetype2",
            Collections.singletonMap("c", "d"))));
    }

    @Test
    void getNewsDisplayCount()
    {
        when(this.configurationSource.getProperty("whatsnew.displayCount", 10)).thenReturn(5);

        assertEquals(5, this.configuration.getNewsDisplayCount());
    }

    @Test
    void getNewsRefreshRate()
    {
        when(this.configurationSource.getProperty("whatsnew.refreshRate", 1 * 60L * 60L * 24)).thenReturn(100L);

        assertEquals(100, this.configuration.getNewsRefreshRate());
    }

    @Test
    void isActiveWhenNoSourceConfiguration()
    {
        when(this.configurationSource.containsKey("whatsnew.sources")).thenReturn(false);

        assertTrue(this.configuration.isActive());
    }

    @Test
    void isActiveWhenEmptySourceConfiguration()
    {
        when(this.configurationSource.containsKey("whatsnew.sources")).thenReturn(true);
        when(this.configurationSource.getProperty("whatsnew.sources", Properties.class))
            .thenReturn(new Properties());

        assertFalse(this.configuration.isActive());
    }

    @Test
    void isActiveWhenNotEmptySourceConfiguration()
    {
        Properties data = new Properties();
        data.setProperty("a", "b");
        when(this.configurationSource.getProperty("whatsnew.sources", Properties.class))
            .thenReturn(data);

        assertTrue(this.configuration.isActive());
    }
}
