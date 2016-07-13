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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMailStorageConfiguration}.
 *
 * @version $Id$
 * @since 6.4.1
 */
public class DefaultMailStorageConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMailStorageConfiguration> mocker =
        new MockitoComponentMockingRule<>(DefaultMailStorageConfiguration.class);

    @Test
    public void discardSuccessStatusesWhenNotDefined() throws Exception
    {
        ConfigurationSource xwikiPropertiesSource =
            this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(xwikiPropertiesSource.getProperty("mail.sender.database.discardSuccessStatuses", 1)).thenReturn(1);

        assertEquals(true, this.mocker.getComponentUnderTest().discardSuccessStatuses());
    }

    @Test
    public void discardSuccessStatusesFalseWhenDefinedInMailConfig() throws Exception
    {
        ConfigurationSource mailConfigSource = this.mocker.getInstance(ConfigurationSource.class, "mailsend");
        when(mailConfigSource.getProperty("discardSuccessStatuses")).thenReturn(0);

        assertEquals(false, this.mocker.getComponentUnderTest().discardSuccessStatuses());
    }
}
