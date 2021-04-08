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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMailStorageConfiguration}.
 *
 * @version $Id$
 * @since 6.4.1
 */
@ComponentTest
class DefaultMailStorageConfigurationTest
{
    @InjectMockComponents
    private DefaultMailStorageConfiguration configuration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @MockComponent
    @Named("mailsend")
    private ConfigurationSource mailConfigSource;

    @Test
    void discardSuccessStatusesWhenNotDefined()
    {
        when(xwikiPropertiesSource.getProperty("mail.sender.database.discardSuccessStatuses", 1)).thenReturn(1);

        assertEquals(true, this.configuration.discardSuccessStatuses());
    }

    @Test
    void discardSuccessStatusesFalseWhenDefinedInMailConfig()
    {
        when(mailConfigSource.getProperty("discardSuccessStatuses")).thenReturn(0);

        assertEquals(false, this.configuration.discardSuccessStatuses());
    }

    @Test
    void resendAutomaticallyAtStartupWhenNotDefined()
    {
        when(xwikiPropertiesSource.getProperty(
            "mail.sender.database.resendAutomaticallyAtStartup", true)).thenReturn(true);

        assertEquals(true, this.configuration.resendAutomaticallyAtStartup());
    }

    @Test
    void resendAutomaticallyAtStartupWhenFalse()
    {
        when(xwikiPropertiesSource.getProperty(
            "mail.sender.database.resendAutomaticallyAtStartup", true)).thenReturn(false);

        assertEquals(false, this.configuration.resendAutomaticallyAtStartup());
    }
}
