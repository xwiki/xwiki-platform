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
package org.xwiki.configuration.internal;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Validate {@link AllConfigurationSource}.
 * 
 * @version $Id$
 */
@ComponentTest
class AllConfigurationSourceTest
{
    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @MockComponent
    @Named("documents")
    private ConfigurationSource documentsPreferencesSource;

    @MockComponent
    @Named("user")
    private ConfigurationSource userPreferencesSource;

    @InjectMockComponents
    private AllConfigurationSource configuration;

    @Test
    void getProperty()
    {
        assertNull(this.configuration.getProperty("key"));

        when(this.xwikiPropertiesSource.containsKey("key")).thenReturn(true);
        when(this.xwikiPropertiesSource.getProperty("key")).thenReturn("xwikiPropertiesSource");

        assertEquals("xwikiPropertiesSource", this.configuration.getProperty("key"));

        when(this.documentsPreferencesSource.containsKey("key")).thenReturn(true);
        when(this.documentsPreferencesSource.getProperty("key")).thenReturn("documentsPreferencesSource");

        assertEquals("documentsPreferencesSource", this.configuration.getProperty("key"));

        when(this.userPreferencesSource.containsKey("key")).thenReturn(true);
        when(this.userPreferencesSource.getProperty("key")).thenReturn("userPreferencesSource");

        assertEquals("userPreferencesSource", this.configuration.getProperty("key"));
    }
}
