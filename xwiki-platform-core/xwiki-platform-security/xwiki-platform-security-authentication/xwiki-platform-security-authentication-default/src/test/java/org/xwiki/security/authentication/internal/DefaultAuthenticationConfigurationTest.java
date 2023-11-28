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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @MockComponent
    @Named("permanent")
    private ConfigurationSource permanentConfiguration;

    @InjectMockComponents
    private DefaultAuthenticationConfiguration configuration;

    @Test
    void getCookieDomains()
    {
        // Test with empty configuration.
        String configurationKey = "xwiki.authentication.cookiedomains";
        when(this.xwikiCfgConfiguration.getProperty(configurationKey, List.class, List.of())).thenReturn(List.of());

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

    private void resetCache(String fieldName) throws IllegalAccessException
    {
        FieldUtils.writeField(this.configuration, fieldName, null, true);
    }

    @Test
    void getValidationKey() throws IllegalAccessException, ConfigurationSaveException
    {
        String validationKey = this.configuration.getValidationKey();

        assertNotNull(validationKey);
        assertEquals(32, validationKey.length());
        verify(permanentConfiguration).setProperty(eq("xwiki.authentication.validationKey"), anyString());

        resetCache("validationKey");
        String otherValidationKey = this.configuration.getValidationKey();

        assertNotNull(otherValidationKey);
        assertNotEquals(validationKey, otherValidationKey);

        resetCache("validationKey");
        when(this.permanentConfiguration.getProperty("xwiki.authentication.validationKey", String.class))
            .thenReturn("permanenentvalue");

        assertEquals("permanenentvalue", this.configuration.getValidationKey());

        resetCache("validationKey");
        when(this.xwikiCfgConfiguration.getProperty("xwiki.authentication.validationKey", String.class))
            .thenReturn("xwikicfgvalue");

        assertEquals("xwikicfgvalue", this.configuration.getValidationKey());
    }

    @Test
    void getEncryptionKey() throws IllegalAccessException, ConfigurationSaveException
    {
        String encryptionKey = this.configuration.getEncryptionKey();

        assertNotNull(encryptionKey);
        assertEquals(32, encryptionKey.length());
        verify(permanentConfiguration).setProperty(eq("xwiki.authentication.encryptionKey"), anyString());

        resetCache("encryptionKey");
        String otherEncryptionKey = this.configuration.getEncryptionKey();

        assertNotNull(otherEncryptionKey);
        assertNotEquals(encryptionKey, otherEncryptionKey);

        resetCache("encryptionKey");
        when(this.permanentConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("permanenentvalue");

        assertEquals("permanenentvalue", this.configuration.getEncryptionKey());

        resetCache("encryptionKey");
        when(this.xwikiCfgConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("xwikicfgvalue");

        assertEquals("xwikicfgvalue", this.configuration.getEncryptionKey());
    }
}
