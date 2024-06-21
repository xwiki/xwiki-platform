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
package org.xwiki.livedata.internal;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Test of {@link LiveDataRendererConfiguration}.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@ComponentTest
class LiveDataRendererConfigurationTest
{
    @InjectMockComponents
    private LiveDataRendererConfiguration configuration;

    @MockComponent
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.stringLiveDataConfigResolver.resolve("{}")).thenReturn(new LiveDataConfiguration());
    }

    @Test
    void getLiveDataConfigurationDescriptionNotDefined() throws Exception
    {
        LiveDataConfiguration liveDataConfiguration =
            this.configuration.getLiveDataConfiguration("{}", new LiveDataRendererParameters());
        assertNull(liveDataConfiguration.getMeta().getDescription());
    }

    @Test
    void getLiveDataConfigurationDescriptionIsDefined() throws Exception
    {
        LiveDataRendererParameters parameters = new LiveDataRendererParameters();
        String description = "A description";
        parameters.setDescription(description);
        LiveDataConfiguration liveDataConfiguration = this.configuration.getLiveDataConfiguration("{}", parameters);
        assertEquals(description, liveDataConfiguration.getMeta().getDescription());
    }

    @Test
    void getLiveDataConfigurationLimitIsDefined() throws Exception
    {
        LiveDataRendererParameters parameters = new LiveDataRendererParameters();
        parameters.setLimit(2);
        LiveDataConfiguration liveDataConfiguration = this.configuration.getLiveDataConfiguration("{}", parameters);
        assertEquals(List.of(2), liveDataConfiguration.getMeta().getPagination().getPageSizes());
    }
}
