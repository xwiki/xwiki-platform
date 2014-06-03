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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
    public void getProperties() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("key1=value1\nkey2=value2");

        Properties properties = this.mocker.getComponentUnderTest().getProperties();
        assertEquals("value1", properties.getProperty("key1"));
        assertEquals("value2", properties.getProperty("key2"));
    }

    @Test
    public void getPropertiesFromXWikiProperties() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("");

        ConfigurationSource xwikiPropertiesSource =
            this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        when(xwikiPropertiesSource.getProperty("mail.sender.properties", Properties.class)).thenReturn(properties);

        Properties returnedProperties = this.mocker.getComponentUnderTest().getProperties();
        assertEquals("value1", returnedProperties.getProperty("key1"));
        assertEquals("value2", returnedProperties.getProperty("key2"));
    }

    @Test
    public void getPropertiesWhenErrorInFormat() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("javamail_extra_props")).thenReturn("\\uinvalid");

        assertTrue(this.mocker.getComponentUnderTest().getProperties().isEmpty());

        // Verify the logs
        verify(this.mocker.getMockedLogger()).warn(
            "Error while parsing mail properties [{}]. Root cause [{}]. Ignoring configuration...",
            "\\uinvalid", "IllegalArgumentException: Malformed \\uxxxx encoding.");
    }

    @Test
    public void usesAuthenticationWhenNoUserNameAndPassword() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("smtp_server_username", null)).thenReturn(null);
        when(documentsSource.getProperty("smtp_server_password", null)).thenReturn(null);

        assertFalse(this.mocker.getComponentUnderTest().usesAuthentication());
    }

    @Test
    public void usesAuthenticationWhenUserNameAndPasswordExist() throws Exception
    {
        ConfigurationSource documentsSource = this.mocker.getInstance(ConfigurationSource.class, "documents");
        when(documentsSource.getProperty("smtp_server_username", null)).thenReturn("user");
        when(documentsSource.getProperty("smtp_server_password", null)).thenReturn("pass");

        assertFalse(this.mocker.getComponentUnderTest().usesAuthentication());
    }
}
