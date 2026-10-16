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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.environment.Environment;
import org.xwiki.store.blob.BlobStorePropertiesBuilder;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XWikiFilesystemBlobStorePropertiesCustomizer}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiFilesystemBlobStorePropertiesCustomizerTest
{
    private static final String ROOT_DIRECTORY = "filesystem.rootDirectory";

    @InjectMockComponents
    private XWikiFilesystemBlobStorePropertiesCustomizer customizer;

    @MockComponent
    private FilesystemAttachmentsConfiguration configuration;

    @MockComponent
    private TemporaryAttachmentsConfiguration temporaryAttachmentsConfiguration;

    @MockComponent
    private Environment environment;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @XWikiTempDir
    private File tempDir;

    @BeforeEach
    void setup()
    {
        when(this.environment.getPermanentDirectory()).thenReturn(this.tempDir);
    }

    private Optional<Object> customize(String name, String type)
    {
        BlobStorePropertiesBuilder builder = new BlobStorePropertiesBuilder(name, type);
        this.customizer.customize(builder);
        return builder.get(ROOT_DIRECTORY);
    }

    @Test
    void otherTypesAndStores()
    {
        assertTrue(customize("store/file", "s3").isEmpty());
        assertTrue(customize(TemporaryAttachmentStore.STORE_NAME, "s3").isEmpty());
        assertTrue(customize("store/other", "filesystem").isEmpty());
    }

    @Test
    void attachmentsStore()
    {
        assertEquals(Optional.of(new File(this.tempDir, "store/file").toPath()),
            customize("store/file", "filesystem"));

        File directory = new File(this.tempDir, "shared/file");
        when(this.configuration.getDirectory()).thenReturn(directory);
        assertEquals(Optional.of(directory.toPath()), customize("store/file", "filesystem"));
    }

    @Test
    void temporaryAttachmentsStoreWithDefaults()
    {
        // The default of the blob store factory applies: <permanent directory>/store/temporary-attachments.
        assertTrue(customize(TemporaryAttachmentStore.STORE_NAME, "filesystem").isEmpty());
    }

    @Test
    void temporaryAttachmentsStoreNextToTheAttachments()
    {
        File shared = new File(this.tempDir, "shared");
        shared.mkdirs();
        when(this.configuration.getDirectory()).thenReturn(new File(shared, "file"));

        assertEquals(Optional.of(new File(shared, "temporary-attachments").toPath()),
            customize(TemporaryAttachmentStore.STORE_NAME, "filesystem"));
    }

    @Test
    void temporaryAttachmentsStoreWithMissingParent()
    {
        File missing = new File(this.tempDir, "missing/file");
        when(this.configuration.getDirectory()).thenReturn(missing);

        assertTrue(customize(TemporaryAttachmentStore.STORE_NAME, "filesystem").isEmpty());

        assertEquals(1, this.logCapture.size());
        assertEquals("Cannot store the temporary attachments next to [" + missing + "] (parent directory missing or"
            + " not writable): set store.temporaryAttachments.directory to a directory shared by the cluster members.",
            this.logCapture.getMessage(0));
    }

    @Test
    void temporaryAttachmentsStoreWithConfiguredDirectory()
    {
        Path directory = new File(this.tempDir, "temporary").toPath();
        when(this.temporaryAttachmentsConfiguration.getDirectory()).thenReturn(directory.toFile());
        when(this.configuration.getDirectory()).thenReturn(new File(this.tempDir, "missing/file"));

        assertEquals(Optional.of(directory), customize(TemporaryAttachmentStore.STORE_NAME, "filesystem"));
    }
}
