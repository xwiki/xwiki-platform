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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.configuration.DefaultMailSenderConfiguration}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class DefaultMailSenderConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailSenderConfiguration> mocker =
        new MockitoComponentMockingRule<>(DefaultMailSenderConfiguration.class);

    @Test
    public void getFromAddressWhenNotConfigured() throws Exception
    {
        assertNull(this.mocker.getComponentUnderTest().getFromAddress());
    }

    @Test
    public void getFromAddressWhenDefinedInXWikiProperties() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("admin_email", String.class)).thenReturn(null);
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("from", null)).thenReturn(null);

        ConfigurationSource xwikiPropertiesSource =
            this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(xwikiPropertiesSource.getProperty("mail.sender.from", String.class)).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.mocker.getComponentUnderTest().getFromAddress());
    }

    @Test
    public void getFromAddressWhenDefinedInXWikiPreferences() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("admin_email", String.class)).thenReturn("john@doe.com");
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("from", "john@doe.com")).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.mocker.getComponentUnderTest().getFromAddress());
    }

    @Test
    public void getFromAddressWhenDefinedInMailConfig() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("admin_email", String.class)).thenReturn(null);
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("from", (String) null)).thenReturn("john@doe.com");

        assertEquals("john@doe.com", this.mocker.getComponentUnderTest().getFromAddress());
    }

    @Test
    public void getAdditionalPropertiesFromMailConfigDocument() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props", String.class)).thenReturn("key=value");
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("properties", "key=value")).thenReturn(
            "key1=value1\nkey2=value2");

        Properties properties = this.mocker.getComponentUnderTest().getAdditionalProperties();
        assertEquals("value1", properties.getProperty("key1"));
        assertEquals("value2", properties.getProperty("key2"));
    }

    @Test
    public void getAdditionalPropertiesFromXWikiPreferences() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props", String.class)).thenReturn("key1=value1\nkey2=value2");
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("properties", "key1=value1\nkey2=value2")).thenReturn(
            "key1=value1\nkey2=value2");

        Properties properties = this.mocker.getComponentUnderTest().getAdditionalProperties();
        assertEquals("value1", properties.getProperty("key1"));
        assertEquals("value2", properties.getProperty("key2"));
    }

    @Test
    public void getAdditionalPropertiesFromXWikiProperties() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn(null);

        ConfigurationSource xwikiPropertiesSource =
            this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        when(xwikiPropertiesSource.getProperty("mail.sender.properties", Properties.class)).thenReturn(properties);

        Properties returnedProperties = this.mocker.getComponentUnderTest().getAdditionalProperties();
        assertEquals("value1", returnedProperties.getProperty("key1"));
        assertEquals("value2", returnedProperties.getProperty("key2"));
    }

    @Test
    public void getAdditionalPropertiesWhenErrorInFormat() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props", String.class)).thenReturn("\\uinvalid");
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("properties", "\\uinvalid")).thenReturn("\\uinvalid");

        assertTrue(this.mocker.getComponentUnderTest().getAdditionalProperties().isEmpty());

        // Verify the logs
        verify(this.mocker.getMockedLogger()).warn(
            "Error while parsing mail properties [{}]. Root cause [{}]. Ignoring configuration...",
            "\\uinvalid", "IllegalArgumentException: Malformed \\uxxxx encoding.");
    }

    @Test
    public void getAllProperties() throws Exception
    {
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("properties", (String) null)).thenReturn(
            "mail.smtp.starttls.enable=true");
        when(mailConfigDocumentSource.getProperty("username", (String) null)).thenReturn(null);
        when(mailConfigDocumentSource.getProperty("password", (String) null)).thenReturn(null);
        when(mailConfigDocumentSource.getProperty("host", (String) null)).thenReturn("server");
        when(mailConfigDocumentSource.getProperty("port", Integer.class)).thenReturn(25);
        when(mailConfigDocumentSource.getProperty("from", (String) null)).thenReturn("john@doe.com");

        Properties returnedProperties = this.mocker.getComponentUnderTest().getAllProperties();

        assertEquals(5, returnedProperties.size());
        assertEquals("true", returnedProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("server", returnedProperties.getProperty("mail.smtp.host"));
        assertEquals("25", returnedProperties.getProperty("mail.smtp.port"));
        assertEquals("smtp", returnedProperties.getProperty("mail.transport.protocol"));
        assertEquals("john@doe.com", returnedProperties.getProperty("mail.smtp.from"));
        assertNull(returnedProperties.getProperty("mail.smtp.user"));
    }

    @Test
    public void usesAuthenticationWhenNoUserNameAndPassword() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("smtp_server_username", (String) null)).thenReturn(null);
        when(documentsSource.getProperty("smtp_server_password", (String) null)).thenReturn(null);

        assertFalse(this.mocker.getComponentUnderTest().usesAuthentication());
    }

    @Test
    public void usesAuthenticationWhenUserNameAndPasswordExist() throws Exception
    {
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("username", (String) null)).thenReturn("user");
        when(mailConfigDocumentSource.getProperty("password", (String) null)).thenReturn("pass");

        assertTrue(this.mocker.getComponentUnderTest().usesAuthentication());
    }

    @Test
    public void getPortFromXWikiPreferences() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("smtp_port")).thenReturn("25");
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("port", 25)).thenReturn(25);

        assertEquals(25, this.mocker.getComponentUnderTest().getPort());
    }

    @Test
    public void getBCCAddressesFromMailConfig() throws Exception
    {
        ConfigurationSource mailConfigDocumentSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigDocumentSource.getProperty("bcc", String.class)).thenReturn("john@doe.com, mary@doe.com");

        assertThat(Arrays.asList("john@doe.com", "mary@doe.com"),
            containsInAnyOrder(this.mocker.getComponentUnderTest().getBCCAddresses().toArray()));
    }

    @Test
    public void getPortWhenMailConfigDoesntExist() throws Exception
    {
        ConfigurationSource xwikiPropertiesSource =
            this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(xwikiPropertiesSource.getProperty("mail.sender.port", 25)).thenReturn(25);

        assertEquals(25, this.mocker.getComponentUnderTest().getPort());
    }
}