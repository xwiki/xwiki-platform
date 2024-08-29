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
package org.xwiki.store.filesystem.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultFilesystemAttachmentsConfigurationTest
{
    @InjectMockComponents
    private DefaultFilesystemAttachmentsConfiguration configuration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Test
    void cleanOnStartup()
    {
        when(configurationSource.getProperty("store.fsattach.cleanOnStartup", Boolean.TRUE)).thenReturn(true);
        assertTrue(configuration.cleanOnStartup());
        verify(configurationSource, times(1)).getProperty("store.fsattach.cleanOnStartup", Boolean.TRUE);
    }

    @Test
    void getDirectory() throws IOException
    {
        assertNull(configuration.getDirectory());

        Path tempDir = Files.createTempDirectory("testxwiki");
        when(configurationSource.getProperty("store.file.directory")).thenReturn(tempDir.toString());
        assertEquals(tempDir.toFile(), configuration.getDirectory());
    }
}
