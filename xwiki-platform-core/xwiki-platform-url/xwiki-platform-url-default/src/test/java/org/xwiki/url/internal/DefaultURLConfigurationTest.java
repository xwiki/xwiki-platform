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
package org.xwiki.url.internal;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.FrontendURLCheckPolicy;
import org.xwiki.url.URLConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultURLConfigurationTest
{
    @InjectMockComponents
    private DefaultURLConfiguration defaultURLConfiguration;

    @MockComponent
    private Provider<ConfigurationSource> configuration;

    @MockComponent
    private ConfigurationSource configurationSource;

    @BeforeEach
    void setup()
    {
        when(configuration.get()).thenReturn(configurationSource);
    }

    @Test
    void useResourceLastModificationDate()
    {
        URLConfiguration urlConfiguration = () -> null;

        assertTrue(urlConfiguration.useResourceLastModificationDate());
        String property = "url.useResourceLastModificationDate";

        when(configurationSource.getProperty(property, true)).thenReturn(false);
        assertFalse(defaultURLConfiguration.useResourceLastModificationDate());
        verify(configurationSource, atLeastOnce()).getProperty(property, true);
    }

    @Test
    void getFrontendUrlCheckPolicy()
    {
        String deprecatedProperty = "url.frontendUrlCheckEnabled";
        String newProperty = "url.frontendUrlCheckPolicy";
        when(configurationSource.getProperty(deprecatedProperty, Boolean.class)).thenReturn(false);
        when(configurationSource.getProperty(newProperty)).thenReturn("enabled");

        when(configurationSource.containsKey(newProperty)).thenReturn(true);
        when(configurationSource.containsKey(deprecatedProperty)).thenReturn(true);
        assertEquals(FrontendURLCheckPolicy.ENABLED, defaultURLConfiguration.getFrontendUrlCheckPolicy());

        when(configurationSource.getProperty(newProperty)).thenReturn("comments");
        assertEquals(FrontendURLCheckPolicy.COMMENTS, defaultURLConfiguration.getFrontendUrlCheckPolicy());

        when(configurationSource.containsKey(newProperty)).thenReturn(false);
        assertEquals(FrontendURLCheckPolicy.DISABLED, defaultURLConfiguration.getFrontendUrlCheckPolicy());

        when(configurationSource.getProperty(deprecatedProperty, Boolean.class)).thenReturn(true);
        assertEquals(FrontendURLCheckPolicy.ENABLED, defaultURLConfiguration.getFrontendUrlCheckPolicy());

        when(configurationSource.containsKey(deprecatedProperty)).thenReturn(false);
        assertEquals(FrontendURLCheckPolicy.COMMENTS, defaultURLConfiguration.getFrontendUrlCheckPolicy());

        when(configurationSource.containsKey(newProperty)).thenReturn(true);
        when(configurationSource.getProperty(newProperty)).thenReturn("foo");
        assertEquals(FrontendURLCheckPolicy.COMMENTS, defaultURLConfiguration.getFrontendUrlCheckPolicy());
    }
}