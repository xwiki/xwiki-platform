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
package org.xwiki.mail.internal;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMailSenderConfiguration}.
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
    public void getAdditionalProperties() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("key1=value1\nkey2=value2");

        Properties properties = this.mocker.getComponentUnderTest().getAdditionalProperties();
        assertEquals("value1", properties.getProperty("key1"));
        assertEquals("value2", properties.getProperty("key2"));
    }

    @Test
    public void getAdditionalPropertiesFromXWikiProperties() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("");

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
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("\\uinvalid");

        assertTrue(this.mocker.getComponentUnderTest().getAdditionalProperties().isEmpty());

        // Verify the logs
        verify(this.mocker.getMockedLogger()).warn(
                "Error while parsing mail properties [{}]. Root cause [{}]. Ignoring configuration...",
                "\\uinvalid", "IllegalArgumentException: Malformed \\uxxxx encoding.");
    }

    @Test
    public void getAllProperties() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("mail.smtp.starttls.enable=true");
        when(documentsSource.getProperty("smtp_server_username", (String) null)).thenReturn(null);
        when(documentsSource.getProperty("smtp_server_password", (String) null)).thenReturn(null);
        when(documentsSource.getProperty("smtp_server", (String) null)).thenReturn("server");
        when(documentsSource.getProperty("smtp_port")).thenReturn("25");
        when(documentsSource.getProperty("admin_email", (String) null)).thenReturn("john@doe.com");

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
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("smtp_server_username", (String) null)).thenReturn("user");
        when(documentsSource.getProperty("smtp_server_password", (String) null)).thenReturn("pass");

        assertTrue(this.mocker.getComponentUnderTest().usesAuthentication());
    }

    @Test
    public void getFromWhenNotDefined() throws Exception
    {
        assertEquals("no-reply@xwiki.org", this.mocker.getComponentUnderTest().getFromAddress());
    }
}
