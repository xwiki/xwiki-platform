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
package org.xwiki.extension.versioncheck.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultVersionCheckConfigurationTest
{
    @InjectMockComponents
    private DefaultVersionCheckConfiguration configuration;

    @MockComponent
    private ConfigurationSource configurationSource;

    @Test
    void environmentCheckEnabled()
    {
        when(this.configurationSource.getProperty("extension.versioncheck.environment.enabled", false))
            .thenReturn(false);
        assertFalse(this.configuration.isEnvironmentCheckEnabled());

        when(this.configurationSource.getProperty("extension.versioncheck.environment.enabled", false))
            .thenReturn(true);
        assertTrue(this.configuration.isEnvironmentCheckEnabled());
    }

    @Test
    void environmentCheckInterval()
    {
        when(this.configurationSource.getProperty("extension.versioncheck.environment.interval", 3600))
            .thenReturn(1);
        assertEquals(1, this.configuration.environmentCheckInterval());
    }
}
