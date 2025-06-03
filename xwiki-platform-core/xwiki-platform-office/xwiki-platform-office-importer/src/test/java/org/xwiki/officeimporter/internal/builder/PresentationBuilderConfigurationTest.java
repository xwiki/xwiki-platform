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

package org.xwiki.officeimporter.internal.builder;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PresentationBuilderConfiguration}.
 *
 * @version $Id$
 * @since 16.8.0
 * @since 16.4.4
 * @since 15.10.13
 */
@ComponentTest
class PresentationBuilderConfigurationTest
{
    @InjectMockComponents
    private PresentationBuilderConfiguration presentationBuilderConfiguration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @BeforeEach
    void setup()
    {
        when(this.configurationSource.getProperty(anyString(), any(Object.class)))
            .thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[1]);
    }

    @Test
    void getQuality()
    {
        assertEquals(0.56f, this.presentationBuilderConfiguration.getQuality());
        when(this.configurationSource.getProperty("officeimporter.presentation.quality", 56f)).thenReturn(66f);
        assertEquals(0.66f, this.presentationBuilderConfiguration.getQuality());
        when(this.configurationSource.getProperty("officeimporter.presentation.imageFormat", "jpg")).thenReturn("png");
        assertEquals(0f, this.presentationBuilderConfiguration.getQuality());
    }

    @Test
    void getSlideWidth()
    {
        assertEquals(1920, this.presentationBuilderConfiguration.getSlideWidth());
        when(this.configurationSource.getProperty("officeimporter.presentation.slideWidth", 1920)).thenReturn(800);
        assertEquals(800, this.presentationBuilderConfiguration.getSlideWidth());
    }

    @Test
    void getImageFormat()
    {
        assertEquals("jpg", this.presentationBuilderConfiguration.getImageFormat());
        when(this.configurationSource.getProperty("officeimporter.presentation.imageFormat", "jpg")).thenReturn("png");
        assertEquals("png", this.presentationBuilderConfiguration.getImageFormat());
    }
}