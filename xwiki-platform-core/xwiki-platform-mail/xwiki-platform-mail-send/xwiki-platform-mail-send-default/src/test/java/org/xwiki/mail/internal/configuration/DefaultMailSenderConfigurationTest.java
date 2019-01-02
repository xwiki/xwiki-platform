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

import java.util.Arrays;
import java.util.Properties;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.configuration.DefaultMailSenderConfiguration}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
public class DefaultMailSenderConfigurationTest
{
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultMailSenderConfiguration configuration;

    @MockComponent
    @Named("documents")
    private ConfigurationSource documentsSource;

    @MockComponent
    @Named("mailsend")
    private ConfigurationSource mailConfigDocumentSource;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Test
    public void getFromAddressWhenNotConfigured()
    {
        assertNull(this.configuration.getFromAddress());
    }

    @Test
    public void getFromAddressWhenDefinedInXWikiProperties()
    {
        when(this.documentsSource.getProperty("admin_email", String.class)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("from", null)).thenReturn(null);
        when(this.xwikiPropertiesSource.getProperty("mail.sender.from", String.class)).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.configuration.getFromAddress());
    }

    @Test
    public void getFromAddressWhenDefinedInXWikiPreferences()
    {
        when(this.documentsSource.getProperty("admin_email", String.class)).thenReturn("john@doe.com");
        when(this.mailConfigDocumentSource.getProperty("from", "john@doe.com")).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.configuration.getFromAddress());
    }

    @Test
    public void getFromAddressWhenDefinedInMailConfig()
    {
        when(this.documentsSource.getProperty("admin_email", String.class)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("from", (String) null)).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.configuration.getFromAddress());
    }

    @Test
    public void getAdditionalPropertiesFromMailConfigDocument()
    {
        when(this.documentsSource.getProperty("javamail_extra_props", String.class)).thenReturn("key=value");
        when(this.mailConfigDocumentSource.getProperty("properties", "key=value")).thenReturn(
            "key1=value1\nkey2=value2");

        Properties properties = this.configuration.getAdditionalProperties();
        assertEquals("value1", properties.getProperty("key1"));
        assertEquals("value2", properties.getProperty("key2"));
    }

    @Test
    public void getAdditionalPropertiesFromXWikiPreferences()
    {
        when(this.documentsSource.getProperty("javamail_extra_props", String.class)).thenReturn(
            "key1=value1\nkey2=value2");
        when(this.mailConfigDocumentSource.getProperty("properties", "key1=value1\nkey2=value2")).thenReturn(
            "key1=value1\nkey2=value2");

        Properties properties = this.configuration.getAdditionalProperties();
        assertEquals("value1", properties.getProperty("key1"));
        assertEquals("value2", properties.getProperty("key2"));
    }

    @Test
    public void getAdditionalPropertiesFromXWikiProperties()
    {
        when(this.documentsSource.getProperty("javamail_extra_props")).thenReturn(null);

        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        when(this.xwikiPropertiesSource.getProperty("mail.sender.properties", Properties.class)).thenReturn(properties);

        Properties returnedProperties = this.configuration.getAdditionalProperties();
        assertEquals("value1", returnedProperties.getProperty("key1"));
        assertEquals("value2", returnedProperties.getProperty("key2"));
    }

    @Test
    public void getAdditionalPropertiesWhenErrorInFormat()
    {
        when(this.documentsSource.getProperty("javamail_extra_props", String.class)).thenReturn("\\uinvalid");
        when(this.mailConfigDocumentSource.getProperty("properties", "\\uinvalid")).thenReturn("\\uinvalid");

        assertTrue(this.configuration.getAdditionalProperties().isEmpty());

        // Verify the logs
        assertEquals(1, logCapture.size());
        assertEquals("Error while parsing mail properties [\\uinvalid]. Root cause [IllegalArgumentException: "
            + "Malformed \\uxxxx encoding.]. Ignoring configuration...", logCapture.getMessage(0));
    }

    @Test
    public void getAllProperties()
    {
        when(this.mailConfigDocumentSource.getProperty("properties", (String) null)).thenReturn(
            "mail.smtp.starttls.enable=true");
        when(this.mailConfigDocumentSource.getProperty("username", (String) null)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("password", (String) null)).thenReturn(null);
        when(this.mailConfigDocumentSource.getProperty("host", (String) null)).thenReturn("server");
        when(this.mailConfigDocumentSource.getProperty("port", Integer.class)).thenReturn(25);
        when(this.mailConfigDocumentSource.getProperty("from", (String) null)).thenReturn("john@doe.com");

        Properties returnedProperties = this.configuration.getAllProperties();

        assertEquals(5, returnedProperties.size());
        assertEquals("true", returnedProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("server", returnedProperties.getProperty("mail.smtp.host"));
        assertEquals("25", returnedProperties.getProperty("mail.smtp.port"));
        assertEquals("smtp", returnedProperties.getProperty("mail.transport.protocol"));
        assertEquals("john@doe.com", returnedProperties.getProperty("mail.smtp.from"));
        assertNull(returnedProperties.getProperty("mail.smtp.user"));
    }

    @Test
    public void usesAuthenticationWhenNoUserNameAndPassword()
    {
        when(this.documentsSource.getProperty("smtp_server_username", (String) null)).thenReturn(null);
        when(this.documentsSource.getProperty("smtp_server_password", (String) null)).thenReturn(null);

        assertFalse(this.configuration.usesAuthentication());
    }

    @Test
    public void usesAuthenticationWhenUserNameAndPasswordExist()
    {
        when(this.mailConfigDocumentSource.getProperty("username", (String) null)).thenReturn("user");
        when(this.mailConfigDocumentSource.getProperty("password", (String) null)).thenReturn("pass");

        assertTrue(this.configuration.usesAuthentication());
    }

    @Test
    public void getPortFromXWikiPreferences()
    {
        when(this.documentsSource.getProperty("smtp_port")).thenReturn("25");
        when(this.mailConfigDocumentSource.getProperty("port", 25)).thenReturn(25);

        assertEquals(25, this.configuration.getPort());
    }

    @Test
    public void getBCCAddressesFromMailConfig()
    {
        when(this.mailConfigDocumentSource.getProperty("bcc", String.class)).thenReturn("john@doe.com, mary@doe.com");

        assertThat(Arrays.asList("john@doe.com", "mary@doe.com"),
            containsInAnyOrder(this.configuration.getBCCAddresses().toArray()));
    }

    @Test
    public void getPortWhenMailConfigDoesntExist()
    {
        when(this.xwikiPropertiesSource.getProperty("mail.sender.port", 25)).thenReturn(25);

        assertEquals(25, this.configuration.getPort());
    }
}