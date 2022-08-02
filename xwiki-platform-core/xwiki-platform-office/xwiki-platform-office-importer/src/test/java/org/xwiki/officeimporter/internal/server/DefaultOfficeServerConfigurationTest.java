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
package org.xwiki.officeimporter.internal.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.officeimporter.server.OfficeServerConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link DefaultOfficeServerConfiguration}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@ComponentTest
class DefaultOfficeServerConfigurationTest
{
    @InjectMockComponents
    private DefaultOfficeServerConfiguration defaultOfficeServerConfiguration;

    @MockComponent
    private ConfigurationSource configuration;

    /**
     * Test if default configuration values are present.
     */
    @Test
    void defaultConfiguration()
    {
        when(configuration.getProperty(any(String.class), any(Object.class))).thenAnswer(
            invocationOnMock -> invocationOnMock.getArgument(1));
        assertEquals(OfficeServerConfiguration.SERVER_TYPE_INTERNAL, defaultOfficeServerConfiguration.getServerType());
        assertArrayEquals(new int[] {8100}, defaultOfficeServerConfiguration.getServerPorts());
        assertNull(defaultOfficeServerConfiguration.getProfilePath());
        assertTrue(defaultOfficeServerConfiguration.getMaxTasksPerProcess() > 0);
        assertTrue(defaultOfficeServerConfiguration.getTaskExecutionTimeout() > 0);
    }

    @Test
    void serverPorts()
    {
        when(configuration.getProperty("openoffice.serverPorts", List.class))
            .thenReturn(Arrays.asList("10", "12", "8569"));
        assertArrayEquals(new int[] {10, 12, 8569}, defaultOfficeServerConfiguration.getServerPorts());

        when(configuration.getProperty("openoffice.serverPorts", List.class))
            .thenReturn(Collections.emptyList());
        when(configuration.getProperty("openoffice.serverPort", 8100)).thenReturn(4242);
        assertArrayEquals(new int[] {4242}, defaultOfficeServerConfiguration.getServerPorts());
    }
}
