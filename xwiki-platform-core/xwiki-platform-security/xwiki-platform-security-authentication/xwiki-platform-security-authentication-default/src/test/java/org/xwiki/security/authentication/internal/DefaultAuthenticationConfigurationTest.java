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
package org.xwiki.security.authentication.internal;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit test of {@link DefaultAuthenticationConfiguration}.
 */
@ComponentTest
class DefaultAuthenticationConfigurationTest
{
    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgConfiguration;

    @InjectMockComponents
    private DefaultAuthenticationConfiguration configuration;

    @Test
    void getCookieDomains()
    {
        // Test with empty configuration.
        String configurationKey = "xwiki.authentication.cookiedomains";
        when(this.xwikiCfgConfiguration.getProperty(configurationKey, List.class, List.of()))
            .thenReturn(List.of());

        assertEquals(List.of(), this.configuration.getCookieDomains());

        // Test with domains without prefix.
        when(this.xwikiCfgConfiguration.getProperty(configurationKey, List.class, List.of()))
            .thenReturn(List.of("xwiki.org", "xwiki.com"));

        String xwikiComWithPrefix = ".xwiki.com";
        assertEquals(List.of(".xwiki.org", xwikiComWithPrefix), this.configuration.getCookieDomains());

        // Test with domains where some have a prefix already.
        when(this.xwikiCfgConfiguration.getProperty(configurationKey, List.class, List.of()))
            .thenReturn(List.of("example.com", xwikiComWithPrefix));

        assertEquals(List.of(".example.com", xwikiComWithPrefix), this.configuration.getCookieDomains());
    }
}
