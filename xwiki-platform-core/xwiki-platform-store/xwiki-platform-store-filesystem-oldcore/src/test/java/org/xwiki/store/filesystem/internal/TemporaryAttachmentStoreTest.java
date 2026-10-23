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

import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachmentStore} with a mocked blob store, for the cases a filesystem store cannot
 * reproduce (concurrent deletions, listing failures).
 *
 * @version $Id$
 */
@ComponentTest
class TemporaryAttachmentStoreTest
{
    private static final BlobPath PREFIX = BlobPath.absolute("s", "u", "d");

    @InjectMockComponents
    private TemporaryAttachmentStore store;

    @MockComponent
    private BlobStoreManager blobStoreManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private BlobStore blobStore;

    @BeforeEach
    void setup() throws Exception
    {
        this.blobStore = mock(BlobStore.class);
        when(this.blobStoreManager.getBlobStore(TemporaryAttachmentStore.STORE_NAME)).thenReturn(this.blobStore);
        when(this.serializer.serialize(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    void storeIsCreatedOnce() throws Exception
    {
        assertEquals(this.blobStore, this.store.getStore());
        assertEquals(this.blobStore, this.store.getStore());
        verify(this.blobStoreManager, times(1)).getBlobStore(TemporaryAttachmentStore.STORE_NAME);
    }

    @Test
    void keys()
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        BlobPath scopePath = this.store.getScopePath("scope");
        BlobPath userPath = this.store.getUserPath("scope", user);

        assertEquals(1, scopePath.getNameCount());
        assertEquals(64, scopePath.getFileName().toString().length());
        assertTrue(userPath.startsWith(scopePath));
        assertNotEquals(userPath, this.store.getUserPath("scope", null));
        assertNotEquals(userPath, this.store.getUserPath("other", user));
        // Nothing readable in the keys.
        assertFalse(userPath.toString().contains("User"));

        DocumentReference document = new DocumentReference("xwiki", "Space", "Page");
        BlobPath documentPath = this.store.getDocumentPath(userPath, document);
        assertNotEquals(documentPath,
            this.store.getDocumentPath(userPath, new DocumentReference(document, Locale.FRENCH)));
        assertEquals(4, this.store.getFilePath(documentPath, "file name.txt").getNameCount());
    }

    @Test
    void uploadNames()
    {
        BlobPath upload = BlobPath.absolute("0000000000042-123-uabcdefgh");
        assertEquals(OptionalLong.of(42), TemporaryAttachmentStore.getUploadTime(upload));
        assertEquals(OptionalLong.of(123), TemporaryAttachmentStore.getUploadSize(upload));
        assertEquals(OptionalLong.empty(), TemporaryAttachmentStore.getUploadTime(BlobPath.absolute("noDash")));
        assertEquals(OptionalLong.empty(), TemporaryAttachmentStore.getUploadTime(BlobPath.absolute("42-x")));
        assertEquals(OptionalLong.empty(), TemporaryAttachmentStore.getUploadTime(BlobPath.absolute("abc-1-ux")));
        assertEquals(OptionalLong.empty(),
            TemporaryAttachmentStore.getUploadTime(BlobPath.absolute("99999999999999999999999-1-ux")));
    }

    @Test
    void usageFromNames() throws Exception
    {
        BlobPath file = PREFIX.resolve(BlobPath.relative("f"));
        BlobPath ignored = PREFIX.resolve(BlobPath.relative("g"));
        List<Blob> blobs = List.of(
            blob(file.resolve(BlobPath.relative("0000000000001-10-uaaaaaaaa", "content"))),
            blob(file.resolve(BlobPath.relative("0000000000001-10-uaaaaaaaa", "metadata"))),
            // An interrupted upload is counted.
            blob(file.resolve(BlobPath.relative("0000000000002-5-ubbbbbbbb", "content"))),
            // A copy made by temporarilyAttach is not counted.
            blob(file.resolve(BlobPath.relative("0000000000003-100-acccccccc", "content"))),
            // The file about to be replaced is not counted.
            blob(ignored.resolve(BlobPath.relative("0000000000004-1000-udddddddd", "content"))));
        when(this.blobStore.listDescendants(PREFIX)).thenReturn(blobs.stream());

        assertEquals(new TemporaryAttachmentStore.Usage(2, 15), this.store.getUsage(PREFIX, ignored));
        // No HEAD request: the sizes come from the names.
        verify(this.blobStore, never()).getBlob(any());
    }

    @Test
    void nextUploadTime() throws Exception
    {
        Blob existing = blob(PREFIX.resolve(BlobPath.relative("f", "0000000000100-1-uaaaaaaaa", "content")));
        when(this.blobStore.listDescendants(PREFIX)).thenReturn(Stream.empty()).thenReturn(Stream.of(existing));

        assertEquals(50, this.store.getNextUploadTime(PREFIX, 50));
        assertEquals(101, this.store.getNextUploadTime(PREFIX, 50));
    }

    @Test
    void listingRetriedAfterConcurrentDeletion() throws Exception
    {
        BlobPath content = PREFIX.resolve(BlobPath.relative("f", "0000000000001-1-uaaaaaaaa", "content"));
        BlobPath metadata = PREFIX.resolve(BlobPath.relative("f", "0000000000001-1-uaaaaaaaa", "metadata"));
        Stream<Blob> failing = failingStream(content);
        Stream<Blob> complete = Stream.of(blob(content), blob(metadata));
        when(this.blobStore.listDescendants(PREFIX)).thenReturn(failing).thenReturn(complete);

        assertEquals(1, this.store.getNewestUploads(PREFIX).size());
    }

    @Test
    void deleteExpiredWithInterruptedListing() throws Exception
    {
        BlobPath expired = BlobPath.absolute("s", "u", "d", "f", "0000000000001-1-uaaaaaaaa");
        when(this.blobStore.listDescendants(BlobPath.root()))
            .thenAnswer(invocation -> failingStream(expired.resolve(TemporaryAttachmentStore.CONTENT)));

        // What was listed before the failure is still processed.
        assertEquals(1, this.store.deleteExpired(System.currentTimeMillis(), Duration.ofHours(1)));
        verify(this.blobStore).deleteDescendants(expired);
    }

    @Test
    void deleteOlderUploadsFailure() throws Exception
    {
        when(this.blobStore.listDescendants(PREFIX)).thenThrow(new BlobStoreException("down"));

        this.store.deleteOlderUploads(PREFIX, PREFIX.resolve(BlobPath.relative("0000000000001-1-uaaaaaaaa")));

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to delete the older uploads of a temporary attachment: [BlobStoreException: down]",
            this.logCapture.getMessage(0));
    }

    @Test
    void deleteWithRuntimeException() throws Exception
    {
        // The S3 store reports some failures of deleteDescendants as runtime exceptions.
        when(this.blobStore.hasDescendants(PREFIX)).thenReturn(true);
        doThrow(new IllegalStateException("throttled")).when(this.blobStore).deleteDescendants(PREFIX);

        assertTrue(this.store.delete(PREFIX));

        assertEquals("Failed to delete temporary attachments: [IllegalStateException: throttled]",
            this.logCapture.getMessage(0));
    }

    @Test
    void existsWithRuntimeException() throws Exception
    {
        when(this.blobStore.hasDescendants(PREFIX)).thenThrow(new IllegalStateException("down"));

        assertThrows(BlobStoreException.class, () -> this.store.exists(PREFIX));
    }

    @Test
    void deleteAlreadyDeletedByAnotherMember() throws Exception
    {
        when(this.blobStore.hasDescendants(PREFIX)).thenReturn(true, false);
        doThrow(new BlobStoreException("gone")).when(this.blobStore).deleteDescendants(PREFIX);

        assertTrue(this.store.delete(PREFIX));

        assertEquals(0, this.logCapture.size());
    }

    @Test
    void deleteFailure() throws Exception
    {
        when(this.blobStore.hasDescendants(PREFIX)).thenReturn(true);
        doThrow(new BlobStoreException("denied")).when(this.blobStore).deleteDescendants(PREFIX);

        assertTrue(this.store.delete(PREFIX));

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to delete temporary attachments: [BlobStoreException: denied]",
            this.logCapture.getMessage(0));
    }

    @Test
    void deleteFailureWhenTheStoreCannotBeChecked() throws Exception
    {
        when(this.blobStore.hasDescendants(PREFIX)).thenReturn(true).thenThrow(new BlobStoreException("down"));
        doThrow(new BlobStoreException("denied")).when(this.blobStore).deleteDescendants(PREFIX);

        assertTrue(this.store.delete(PREFIX));

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to delete temporary attachments: [BlobStoreException: denied]",
            this.logCapture.getMessage(0));
    }

    @Test
    void deleteExpiredCountsOnlyDeletedUploads() throws Exception
    {
        BlobPath upload = PREFIX.resolve(BlobPath.relative("f", "0000000000001-1-uaaaaaaaa"));
        Blob content = mock(Blob.class);
        when(content.getPath()).thenReturn(upload.resolve(TemporaryAttachmentStore.CONTENT));
        when(this.blobStore.listDescendants(BlobPath.root())).thenReturn(Stream.of(content));
        doThrow(new BlobStoreException("denied")).when(this.blobStore).deleteDescendants(upload);
        when(this.blobStore.hasDescendants(upload)).thenReturn(true);

        assertEquals(0, this.store.deleteExpired(System.currentTimeMillis(), Duration.ofHours(1)));
        assertEquals("Failed to delete temporary attachments: [BlobStoreException: denied]",
            this.logCapture.getMessage(0));
    }

    @Test
    void listingFailure() throws Exception
    {
        when(this.blobStore.listDescendants(PREFIX)).thenAnswer(invocation -> Stream.generate(() -> {
            throw new IllegalStateException("listing failed");
        }));

        BlobStoreException exception =
            assertThrows(BlobStoreException.class, () -> this.store.getNewestUploads(PREFIX));
        assertEquals("Failed to list the temporary attachments", exception.getMessage());
        verify(this.blobStore, times(3)).listDescendants(PREFIX);
    }

    private static Blob blob(BlobPath path)
    {
        Blob blob = mock(Blob.class);
        when(blob.getPath()).thenReturn(path);
        return blob;
    }

    /**
     * A listing that returns one blob, then fails as a filesystem walk does when a directory is deleted meanwhile.
     */
    private static Stream<Blob> failingStream(BlobPath path)
    {
        Blob blob = blob(path);
        return Stream.of(0, 1).map(index -> {
            if (index == 1) {
                throw new UncheckedIOException(new NoSuchFileException("gone"));
            }
            return blob;
        });
    }
}
