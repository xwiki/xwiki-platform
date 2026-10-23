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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachmentsCleaner}, with a filesystem blob store in a temporary directory.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(TemporaryAttachmentStore.class)
class TemporaryAttachmentsCleanerTest
{
    private static final long HOUR = Duration.ofHours(1).toMillis();

    @InjectMockComponents
    private TemporaryAttachmentsCleaner cleaner;

    @MockComponent
    private BlobStoreManager blobStoreManager;

    @MockComponent
    private TemporaryAttachmentsConfiguration configuration;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @XWikiTempDir
    private File tempDir;

    private BlobStore blobStore;

    @BeforeEach
    void setup() throws Exception
    {
        FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
        properties.setRootDirectory(this.tempDir.toPath());
        this.blobStore = new FileSystemBlobStore(TemporaryAttachmentStore.STORE_NAME, properties);
        when(this.blobStoreManager.getBlobStore(TemporaryAttachmentStore.STORE_NAME)).thenReturn(this.blobStore);
        when(this.configuration.getMaxAge()).thenReturn(Duration.ofHours(24));
    }

    private void write(String path) throws BlobStoreException
    {
        this.blobStore.getBlob(BlobPath.parse(path)).writeFromStream(new ByteArrayInputStream("x".getBytes(UTF_8)));
    }

    private static String upload(long time)
    {
        return "%013d-1-uabcdefg".formatted(time);
    }

    private List<String> list() throws BlobStoreException
    {
        try (Stream<Blob> blobs = this.blobStore.listDescendants(BlobPath.root())) {
            return blobs.map(blob -> blob.getPath().toString()).sorted().toList();
        }
    }

    @Test
    void clean() throws Exception
    {
        long now = System.currentTimeMillis();
        String expired = "/s/u/d/f/" + upload(now - 25 * HOUR);
        String expiredIncomplete = "/s/u/d/g/" + upload(now - 30 * HOUR);
        String recent = "/s/u/d/f/" + upload(now - 23 * HOUR);
        write(expired + "/content");
        write(expired + "/metadata");
        write(expiredIncomplete + "/content");
        write(recent + "/content");
        write(recent + "/metadata");
        // Blobs that do not follow the layout are left alone.
        write("/marker");
        write("/s/u/d/f/notAnUpload/content");

        assertEquals(2, this.cleaner.clean());

        assertEquals(
            Stream.of("/marker", "/s/u/d/f/notAnUpload/content", recent + "/content", recent + "/metadata").sorted()
                .toList(),
            list());
        assertEquals(1, this.logCapture.size());
        assertEquals("Deleted [2] temporary attachments older than [24] hours.", this.logCapture.getMessage(0));

        // Nothing more to delete.
        assertEquals(0, this.cleaner.clean());
        assertEquals(1, this.logCapture.size());

        // With a shorter maximum age.
        when(this.configuration.getMaxAge()).thenReturn(Duration.ofHours(1));
        assertEquals(1, this.cleaner.clean());
        assertEquals(List.of("/marker", "/s/u/d/f/notAnUpload/content"), list());
        assertEquals("Deleted [1] temporary attachments older than [1] hours.", this.logCapture.getMessage(1));
    }

    @Test
    void cleanWithEmptyStore() throws Exception
    {
        assertEquals(0, this.cleaner.clean());
        assertTrue(list().isEmpty());
    }

    @Test
    void runWithFailure() throws Exception
    {
        when(this.blobStoreManager.getBlobStore(TemporaryAttachmentStore.STORE_NAME))
            .thenThrow(new BlobStoreException("unavailable"));

        assertThrows(BlobStoreException.class, () -> this.cleaner.clean());

        this.cleaner.run();
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to delete the expired temporary attachments: [BlobStoreException: unavailable]",
            this.logCapture.getMessage(0));
    }

    @Test
    void startAndDispose()
    {
        this.cleaner.onEvent(new ApplicationReadyEvent(), null, null);
        // Starting twice is harmless.
        this.cleaner.onEvent(new ApplicationReadyEvent(), null, null);
        this.cleaner.dispose();
        this.cleaner.dispose();
    }
}
