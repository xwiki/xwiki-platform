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
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.environment.Environment;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
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
 * Unit tests for {@link XWikiFileSystemBlobStoreManager}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiFileSystemBlobStoreManagerTest
{
    private static final String LOG_MESSAGE = "Using filesystem store directory [%s]";

    @InjectMockComponents
    private XWikiFileSystemBlobStoreManager fileSystemBlobStoreManager;

    @MockComponent
    private FilesystemAttachmentsConfiguration config;

    @MockComponent
    private Environment environment;

    @RegisterExtension
    private static LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Test
    void getBlobStore(@XWikiTempDir File configDirectory) throws Exception
    {
        // Use Case 1: Null store directory and cleanOnStartup = false
        FileSystemBlobStore blobStore = this.fileSystemBlobStoreManager.getBlobStore("store/file");
        Path expectedPath = Path.of("store", "file").toAbsolutePath();
        assertEquals(expectedPath, blobStore.getBlobFilePath(BlobPath.of(List.of())));
        verify(this.environment).getPermanentDirectory();
        assertEquals(String.format(LOG_MESSAGE, expectedPath), logCapture.getMessage(0));

        // Use Case 2: Non-null store directory and cleanOnStartup = true

        when(config.getDirectory()).thenReturn(configDirectory);
        when(config.cleanOnStartup()).thenReturn(true);

        blobStore = this.fileSystemBlobStoreManager.getBlobStore("store/file");

        assertEquals(configDirectory, blobStore.getBlobFilePath(BlobPath.of(List.of())).toFile());
        verify(this.environment).getPermanentDirectory();
        verify(this.config, times(2)).cleanOnStartup();
        assertEquals(String.format(LOG_MESSAGE, configDirectory.toString()), logCapture.getMessage(1));
    }
}
