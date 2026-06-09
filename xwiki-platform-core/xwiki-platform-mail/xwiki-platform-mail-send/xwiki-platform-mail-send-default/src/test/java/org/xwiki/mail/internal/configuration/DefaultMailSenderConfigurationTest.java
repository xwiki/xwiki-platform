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
package org.xwiki.mail.internal.configuration;

import java.util.List;
import java.util.Properties;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.configuration.DefaultMailSenderConfiguration}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class DefaultMailSenderConfigurationTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultMailSenderConfiguration configuration;

    @MockComponent
    @Named("mailsend")
    private ConfigurationSource mailConfigDocumentSource;

    @MockComponent
    @Named("mailsendmainwiki")
    private ConfigurationSource mainWikiMailConfigDocumentSource;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainwiki");
        when(this.wikiDescriptorManager.isMainWiki("mainwiki")).thenReturn(true);
    }

    @Test
    void getFromAddressWhenNotConfigured()
    {
        assertNull(this.configuration.getFromAddress());
    }

    @Test
    void getFromAddressWhenDefinedInXWikiProperties()
    {
        when(this.xwikiPropertiesSource.getProperty("mail.sender.from", String.class)).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.configuration.getFromAddress());
    }

    @Test
    void getFromAddressFromMailConfigDocumentInMainWiki()
    {
        when(this.mailConfigDocumentSource.getProperty("from", String.class)).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.configuration.getFromAddress());
    }

    @Test
    void getFromAddressFromMailConfigDocumentInSubwikiAndConfigInMainWiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        when(this.mailConfigDocumentSource.getProperty("from", String.class)).thenReturn(null);
        when(this.mainWikiMailConfigDocumentSource.getProperty("from", String.class)).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.configuration.getFromAddress());
    }

    /**
     * Verify the ability to define anonymous credentials for a SMTP host in a subwiki (i.e. credentials not taken from
     * the main wiki config).
     */
    @Test
    void getUsernameAndPasswordWhenNotDefinedAndHostModifiedInSubwiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        when(this.mailConfigDocumentSource.getProperty("host", String.class)).thenReturn("something");
        when(this.mailConfigDocumentSource.getProperty("username", String.class)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("password", String.class)).thenReturn(null);

        assertNull(this.configuration.getUsername());
        assertNull(this.configuration.getPassword());

        verifyNoInteractions(this.mainWikiMailConfigDocumentSource);
        verifyNoInteractions(this.xwikiPropertiesSource);
    }

    @Test
    void getUsernameAndPasswordWhenNotDefinedInSubWikiButInMainWikiAndHostNotModifiedInSubwiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        when(this.mailConfigDocumentSource.getProperty("host", String.class)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("username", String.class)).thenReturn(null);
        when(this.mainWikiMailConfigDocumentSource.getProperty("username", String.class)).thenReturn("username");
        when(this.mailConfigDocumentSource.getProperty("password", String.class)).thenReturn(null);
        when(this.mainWikiMailConfigDocumentSource.getProperty("password", String.class)).thenReturn("password");

        assertEquals("username", this.configuration.getUsername());
        assertEquals("password", this.configuration.getPassword());

        verifyNoInteractions(this.xwikiPropertiesSource);
    }

    @Test
    void getUsernameAndPasswordWhenNotDefinedInSubWikiButInPropertiesAndHostNotModifiedInSubwiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        when(this.mailConfigDocumentSource.getProperty("host", String.class)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("username", String.class)).thenReturn(null);
        when(this.xwikiPropertiesSource.getProperty("mail.sender.username", String.class)).thenReturn("username");
        when(this.mailConfigDocumentSource.getProperty("password", String.class)).thenReturn(null);
        when(this.xwikiPropertiesSource.getProperty("mail.sender.password", String.class)).thenReturn("password");

        assertEquals("username", this.configuration.getUsername());
        assertEquals("password", this.configuration.getPassword());
    }

    @Test
    void getUsernameAndPasswordWhenDefinedInSubwiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        when(this.mailConfigDocumentSource.getProperty("username", String.class)).thenReturn("username");
        when(this.mailConfigDocumentSource.getProperty("password", String.class)).thenReturn("password");

        assertEquals("username", this.configuration.getUsername());
        assertEquals("password", this.configuration.getPassword());

        verifyNoInteractions(this.mainWikiMailConfigDocumentSource);
        verifyNoInteractions(this.xwikiPropertiesSource);
    }

    @Test
    void getAdditionalPropertiesFromMailConfigDocumentInMainWiki()
    {
        when(this.mailConfigDocumentSource.getProperty("properties", String.class)).thenReturn(
            "key1=value1\nkey2=value2");

        Properties returnedProperties = this.configuration.getAdditionalProperties();
        assertEquals("value1", returnedProperties.getProperty("key1"));
        assertEquals("value2", returnedProperties.getProperty("key2"));
    }

    @Test
    void getAdditionalPropertiesFromMailConfigDocumentInSubwikiAndConfigInMainWiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        when(this.mailConfigDocumentSource.getProperty("properties", String.class)).thenReturn(null);
        when(this.mainWikiMailConfigDocumentSource.getProperty("properties", String.class)).thenReturn(
                "key1=value1\nkey2=value2");

        Properties returnedProperties = this.configuration.getAdditionalProperties();
        assertEquals("value1", returnedProperties.getProperty("key1"));
        assertEquals("value2", returnedProperties.getProperty("key2"));
    }

    @Test
    void getAdditionalPropertiesFromXWikiProperties()
    {
        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        when(this.xwikiPropertiesSource.getProperty("mail.sender.properties", Properties.class)).thenReturn(properties);

        Properties returnedProperties = this.configuration.getAdditionalProperties();
        assertEquals("value1", returnedProperties.getProperty("key1"));
        assertEquals("value2", returnedProperties.getProperty("key2"));
    }

    @Test
    void getAdditionalPropertiesWhenErrorInFormat()
    {
        when(this.mailConfigDocumentSource.getProperty("properties", String.class)).thenReturn("\\uinvalid");

        assertTrue(this.configuration.getAdditionalProperties().isEmpty());

        // Verify the logs
        assertEquals(1, logCapture.size());
        assertEquals("Error while parsing mail properties [\\uinvalid]. Root cause [IllegalArgumentException: "
            + "Malformed \\uxxxx encoding.]. Ignoring configuration...", logCapture.getMessage(0));
    }

    @Test
    void getAllProperties()
    {
        when(this.mailConfigDocumentSource.getProperty("properties", String.class)).thenReturn(
            "mail.smtp.starttls.enable=true");
        when(this.mailConfigDocumentSource.getProperty("username", String.class)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("password", String.class)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("host", String.class)).thenReturn("server");
        when(this.mailConfigDocumentSource.getProperty("port", Integer.class)).thenReturn(25);
        when(this.mailConfigDocumentSource.getProperty("from", String.class)).thenReturn("john@doe.com");

        Properties returnedProperties = this.configuration.getAllProperties();

        assertEquals(4, returnedProperties.size());
        assertEquals("true", returnedProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("server", returnedProperties.getProperty("mail.smtp.host"));
        assertEquals("25", returnedProperties.getProperty("mail.smtp.port"));
        assertEquals("smtp", returnedProperties.getProperty("mail.transport.protocol"));
        assertNull(returnedProperties.getProperty("mail.smtp.from"));
        assertNull(returnedProperties.getProperty("mail.smtp.user"));
    }

    @Test
    void usesAuthenticationWhenUserNameAndPasswordExist()
    {
        when(this.mailConfigDocumentSource.getProperty("username", String.class)).thenReturn("user");
        when(this.mailConfigDocumentSource.getProperty("password", String.class)).thenReturn("pass");

        assertTrue(this.configuration.usesAuthentication());
    }

    @Test
    void getPortFromXWikiPreferences()
    {
        when(this.mailConfigDocumentSource.getProperty("port", Integer.class)).thenReturn(25);

        assertEquals(25, this.configuration.getPort());
    }

    @Test
    void getBCCAddressesFromMailConfig()
    {
        when(this.mailConfigDocumentSource.getProperty("bcc", String.class)).thenReturn("john@doe.com, mary@doe.com");

        assertThat(List.of("john@doe.com", "mary@doe.com"),
            containsInAnyOrder(this.configuration.getBCCAddresses().toArray()));
    }

    @Test
    void getPortWhenMailConfigDoesntExist()
    {
        when(this.xwikiPropertiesSource.getProperty("mail.sender.port", 25)).thenReturn(25);

        assertEquals(25, this.configuration.getPort());
    }
}