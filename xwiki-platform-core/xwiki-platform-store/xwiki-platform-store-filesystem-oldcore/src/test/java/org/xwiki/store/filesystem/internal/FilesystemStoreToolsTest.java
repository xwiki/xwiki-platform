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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FilesystemStoreTools}.
 *
 * @version $Id$
 */
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
    private static LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Test
    void initialize(@XWikiTempDir File configDirectory) throws IOException, InitializationException
    {
        // Use Case 1: Null store directory and cleanOnStartup = false
        // Note: FilesystemStoreTools.initialize() method is first triggered by the test framework when injecting the
        // component.

        assertEquals(new File(new File("."), "store/file").getCanonicalFile(),
            filesystemStoreTools.getStoreRootDirectory());
        verify(this.environment, times(1)).getPermanentDirectory();

        // Use Case 2: Non-null store directory and cleanOnStartup = true

        when(config.getDirectory()).thenReturn(configDirectory);
        when(config.cleanOnStartup()).thenReturn(true);

        filesystemStoreTools.initialize();

        assertEquals(configDirectory, filesystemStoreTools.getStoreRootDirectory());
        verify(this.environment, times(1)).getPermanentDirectory();
        verify(this.config, times(2)).cleanOnStartup();
        assertEquals(String.format("Using filesystem store directory [%s]", configDirectory.toString()),
            logCapture.getMessage(1));
    }

    @AfterAll
    static void verifyLog() throws Exception
    {
        // Assert log happening in the first initialize() call.
        assertEquals(String.format("Using filesystem store directory [%s]",
            new File(new File("."), "store/file").getCanonicalFile()), logCapture.getMessage(0));
    }
}
