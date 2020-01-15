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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class FilesystemStoreToolsTest
{
    @InjectMockComponents
    private FilesystemStoreTools filesystemStoreTools;

    @MockComponent
    private FilesystemAttachmentsConfiguration config;

    @MockComponent
    private Environment environment;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Test
    public void initialize() throws IOException, InitializationException, InterruptedException
    {
        // initialize() method is triggered by the test framework first for injecting the component
        assertEquals(new File(new File("."), "store/file").getCanonicalFile(),
            filesystemStoreTools.getStoreRootDirectory());
        verify(this.environment, times(1)).getPermanentDirectory();

        File configDirectory = Files.createTempDirectory("xwikitest").toFile();

        when(config.getDirectory()).thenReturn(configDirectory);
        when(config.cleanOnStartup()).thenReturn(true);

        filesystemStoreTools.initialize();
        assertEquals(configDirectory, filesystemStoreTools.getStoreRootDirectory());
        verify(this.environment, times(1)).getPermanentDirectory();
        verify(this.config, times(2)).cleanOnStartup();
        assertEquals(String.format("Using filesystem store directory [%s]", configDirectory.toString()),
            logCapture.getMessage(0));
    }
}
