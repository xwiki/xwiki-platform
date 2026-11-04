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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @MockComponent
    private RemoteObservationManagerConfiguration remoteObservationConfiguration;

    @InjectMockComponents
    private DefaultAuthenticationConfiguration configuration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getCookieDomains()
    {
        // Test with empty configuration.
        String configurationKey = "xwiki.authentication.cookiedomains";
        when(this.xwikiCfgConfiguration.getProperty(configurationKey, List.class, List.of())).thenReturn(List.of());

        assertEquals(List.of(), this.configuration.getCookieDomains());

        // Test with domains.
        when(this.xwikiCfgConfiguration.getProperty(configurationKey, List.class, List.of()))
            .thenReturn(List.of("xwiki.org", "xwiki.com"));

        assertEquals(List.of("xwiki.org", "xwiki.com"), this.configuration.getCookieDomains());
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

    @Test
    void getKeysFromXWikiCfgWithClustering()
    {
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(true);
        when(this.xwikiCfgConfiguration.getProperty("xwiki.authentication.validationKey", String.class))
            .thenReturn("validationvalue");
        when(this.xwikiCfgConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("encryptionvalue");

        assertEquals("validationvalue", this.configuration.getValidationKey());
        assertEquals("encryptionvalue", this.configuration.getEncryptionKey());

        assertEquals(0, this.logCapture.size());
    }

    @Test
    void getGeneratedKeysWithClustering() throws IllegalAccessException
    {
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(true);

        String validationKey = this.configuration.getValidationKey();
        assertNotNull(validationKey);

        assertEquals(1, this.logCapture.size());
        String message = this.logCapture.getMessage(0);
        assertEquals("The authentication key [xwiki.authentication.validationKey] is not configured but clustering "
            + "is enabled (observation.remote.enabled=true). It comes from configuration.properties in the permanent "
            + "directory (randomly generated at this node's first start unless it was put there), so it differs "
            + "between nodes that do not share that file: users are logged out whenever the load balancer switches "
            + "node. Set the same value on all nodes, in xwiki.cfg or, for example from a secret store, with the "
            + "environment variable [XCONF_xwikicfg_xwiki_authentication_validationKey] or the Java system property "
            + "[xconf.xwikicfg.xwiki.authentication.validationKey].", message);
        // Never log the key value
        assertFalse(message.contains(validationKey));

        // Reading the key again (even when the cache is reset) does not log again
        assertEquals(validationKey, this.configuration.getValidationKey());
        resetCache("validationKey");
        this.configuration.getValidationKey();
        assertEquals(1, this.logCapture.size());

        // The other key is reported once as well, even when it comes from the permanent configuration
        when(this.permanentConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("permanentvalue");
        assertEquals("permanentvalue", this.configuration.getEncryptionKey());
        assertEquals("permanentvalue", this.configuration.getEncryptionKey());

        assertEquals(2, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(1)
            .startsWith("The authentication key [xwiki.authentication.encryptionKey] is not configured"));
        assertTrue(this.logCapture.getMessage(1).endsWith("with the environment variable "
            + "[XCONF_xwikicfg_xwiki_authentication_encryptionKey] or the Java system property "
            + "[xconf.xwikicfg.xwiki.authentication.encryptionKey]."));
        assertFalse(this.logCapture.getMessage(1).contains("permanentvalue"));
    }

    @Test
    void getGeneratedKeyWithClusteringWhenSaveFails() throws ConfigurationSaveException
    {
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(true);
        doThrow(new ConfigurationSaveException("Save failure")).when(this.permanentConfiguration)
            .setProperty(eq("xwiki.authentication.validationKey"), anyString());

        String validationKey = this.configuration.getValidationKey();
        assertEquals(32, validationKey.length());

        assertEquals(2, this.logCapture.size());
        assertEquals("Failed to store the key, it will be generated at each restart", this.logCapture.getMessage(0));
        assertTrue(this.logCapture.getMessage(1)
            .startsWith("The authentication key [xwiki.authentication.validationKey] is not configured"));
        assertFalse(this.logCapture.getMessage(1).contains(validationKey));
    }

    @Test
    void getGeneratedKeysWithoutClustering()
    {
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(false);
        when(this.permanentConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("permanentvalue");

        assertNotNull(this.configuration.getValidationKey());
        assertEquals("permanentvalue", this.configuration.getEncryptionKey());

        assertEquals(0, this.logCapture.size());
    }

    @Test
    void getKeysWhenBlankInXWikiCfg()
    {
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(true);
        when(this.xwikiCfgConfiguration.getProperty("xwiki.authentication.validationKey", String.class))
            .thenReturn("");
        when(this.xwikiCfgConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("  ");
        when(this.permanentConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("permanentvalue");

        // A blank value is considered as not set: the key falls back to the permanent configuration and the
        // clustering check applies
        assertEquals(32, this.configuration.getValidationKey().length());
        assertEquals("permanentvalue", this.configuration.getEncryptionKey());

        assertEquals(2, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0)
            .startsWith("The authentication key [xwiki.authentication.validationKey] is not configured"));
        assertTrue(this.logCapture.getMessage(1)
            .startsWith("The authentication key [xwiki.authentication.encryptionKey] is not configured"));
    }

    @Test
    void getKeysWithExampleValues() throws IllegalAccessException
    {
        when(this.xwikiCfgConfiguration.getProperty("xwiki.authentication.validationKey", String.class))
            .thenReturn("totototototototototototototototo");
        when(this.permanentConfiguration.getProperty("xwiki.authentication.encryptionKey", String.class))
            .thenReturn("titititititititititititititititi");

        assertEquals("totototototototototototototototo", this.configuration.getValidationKey());
        assertEquals("titititititititititititititititi", this.configuration.getEncryptionKey());

        assertEquals(2, this.logCapture.size());
        String message = this.logCapture.getMessage(0);
        assertEquals("The authentication key [xwiki.authentication.validationKey] is set to one of the example values "
            + "documented in xwiki.cfg. Such a well-known key weakens the protection of the authentication cookies: set "
            + "a random value instead.", message);
        assertFalse(message.contains("totototototototototototototototo"));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertTrue(this.logCapture.getMessage(1)
            .startsWith("The authentication key [xwiki.authentication.encryptionKey] is set to one of the example"));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(1).getLevel());

        // Reported only once per key, even when the cache is reset
        resetCache("validationKey");
        resetCache("encryptionKey");
        this.configuration.getValidationKey();
        this.configuration.getEncryptionKey();
        assertEquals(2, this.logCapture.size());
    }
}
