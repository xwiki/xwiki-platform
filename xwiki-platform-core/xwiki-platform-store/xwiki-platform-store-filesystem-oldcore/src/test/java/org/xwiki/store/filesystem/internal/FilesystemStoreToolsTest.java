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
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.store.internal.FileSystemStoreUtils;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FilesystemStoreTools}.
 *
 * @version $Id$
 */
@SuppressWarnings("checkstyle:MultipleStringLiterals")
@ComponentTest
class FilesystemStoreToolsTest
{
    @InjectMockComponents
    private FilesystemStoreTools filesystemStoreTools;

    @MockComponent
    private BlobStoreManager blobStoreManager;

    @XWikiTempDir
    private File tempDir;

    private BlobStore blobStore;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
        properties.setRootDirectory(this.tempDir.toPath());
        this.blobStore = new FileSystemBlobStore("Test", properties);
        when(this.blobStoreManager.getBlobStore("store/" + FileSystemStoreUtils.HINT)).thenReturn(this.blobStore);
    }

    @Test
    void getStore() throws Exception
    {
        // Initialize the component to trigger the store setup
        this.filesystemStoreTools.initialize();

        BlobStore store = this.filesystemStoreTools.getStore();
        assertNotNull(store);
        assertEquals(this.blobStore, store);
    }

    @Test
    void getWikiDir()
    {
        BlobPath wikiDir = this.filesystemStoreTools.getWikiDir("testwiki");

        assertEquals("/testwiki", wikiDir.toString());
    }

    @Test
    void getDocumentContentDir()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        BlobPath docDir = this.filesystemStoreTools.getDocumentContentDir(docRef);

        assertNotNull(docDir);
        assertTrue(docDir.toString().startsWith("/wiki/"));
        // The path should contain MD5-based subdirectories
        assertTrue(docDir.getNameCount() >= 4);
    }

    @Test
    void getDocumentContentDirWithLocale()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        BlobPath docDir = this.filesystemStoreTools.getDocumentContentDir(docRef);

        assertNotNull(docDir);
        assertTrue(docDir.toString().startsWith("/wiki/"));
        // Should contain the locales directory and the locale
        assertTrue(docDir.toString().contains(FilesystemStoreTools.DOCUMENT_LOCALES_DIR_NAME));
        assertTrue(docDir.toString().contains(Locale.FRENCH.toString()));
    }

    @Test
    void getDocumentContentDirWithRootLocale()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page", Locale.ROOT);
        BlobPath docDirWithRoot = this.filesystemStoreTools.getDocumentContentDir(docRef);

        DocumentReference docRefNoLocale = new DocumentReference("wiki", "Space", "Page");
        BlobPath docDirNoLocale = this.filesystemStoreTools.getDocumentContentDir(docRefNoLocale);

        // Root locale should be treated the same as no locale
        assertEquals(docDirNoLocale, docDirWithRoot);
        assertFalse(docDirWithRoot.toString().contains(FilesystemStoreTools.DOCUMENT_LOCALES_DIR_NAME));
    }

    @Test
    void getAttachmentDir()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        AttachmentReference attachRef = new AttachmentReference("file.txt", docRef);

        BlobPath attachDir = this.filesystemStoreTools.getAttachmentDir(attachRef);

        assertNotNull(attachDir);
        assertTrue(attachDir.toString().contains(FilesystemStoreTools.ATTACHMENTS_DIR_NAME));
        // Should be under the document directory
        BlobPath docDir = this.filesystemStoreTools.getDocumentContentDir(docRef);
        assertTrue(attachDir.toString().startsWith(docDir.toString()));
    }

    @Test
    void getDeletedAttachmentDir()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        AttachmentReference attachRef = new AttachmentReference("file.txt", docRef);
        long index = 123456789L;

        BlobPath deletedDir = this.filesystemStoreTools.getDeletedAttachmentDir(attachRef, index);

        assertNotNull(deletedDir);
        assertTrue(deletedDir.toString().contains(FilesystemStoreTools.DELETED_ATTACHMENTS_DIR_NAME));
        assertTrue(deletedDir.toString().endsWith(String.valueOf(index)));
    }

    @Test
    void getDeletedDocumentContentDir()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        long index = 987654321L;

        BlobPath deletedDir = this.filesystemStoreTools.getDeletedDocumentContentDir(docRef, index);

        assertNotNull(deletedDir);
        assertTrue(deletedDir.toString().contains(FilesystemStoreTools.DELETED_DOCUMENTS_DIR_NAME));
        assertTrue(deletedDir.toString().endsWith(String.valueOf(index)));
    }

    @Test
    void getAttachmentFileProvider()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        AttachmentReference attachRef = new AttachmentReference("file.txt", docRef);

        AttachmentBlobProvider provider = this.filesystemStoreTools.getAttachmentFileProvider(attachRef);

        assertNotNull(provider);
    }

    @Test
    void getDeletedAttachmentFileProvider()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        AttachmentReference attachRef = new AttachmentReference("file.txt", docRef);
        long index = 12345L;

        DeletedAttachmentBlobProvider provider =
            this.filesystemStoreTools.getDeletedAttachmentFileProvider(attachRef, index);

        assertNotNull(provider);
    }

    @Test
    void getDeletedDocumentFileProvider()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        long index = 54321L;

        DeletedDocumentContentBlobProvider provider =
            this.filesystemStoreTools.getDeletedDocumentFileProvider(docRef, index);

        assertNotNull(provider);
    }

    @Test
    void getBackupFile() throws Exception
    {
        this.filesystemStoreTools.initialize();

        BlobPath originalPath = BlobPath.absolute("test", "file.txt");
        Blob originalBlob = this.blobStore.getBlob(originalPath);

        Blob backupBlob = this.filesystemStoreTools.getBackupFile(originalBlob);

        assertNotNull(backupBlob);
        assertNotEquals(originalPath, backupBlob.getPath());
        // Should have a backup suffix
        assertTrue(backupBlob.getPath().getFileName().toString().contains("~bak"));
        // Should be in the same directory as the original.
        assertEquals(originalPath.getParent(), backupBlob.getPath().getParent());
    }

    @Test
    void getTempFile() throws Exception
    {
        this.filesystemStoreTools.initialize();

        BlobPath originalPath = BlobPath.absolute("test", "file.txt");
        Blob originalBlob = this.blobStore.getBlob(originalPath);

        Blob tempBlob = this.filesystemStoreTools.getTempFile(originalBlob);

        assertNotNull(tempBlob);
        assertNotEquals(originalPath, tempBlob.getPath());
        // Should have a temp suffix
        assertTrue(tempBlob.getPath().getFileName().toString().contains("~tmp"));
        // Should be in the same directory as the original.
        assertEquals(originalPath.getParent(), tempBlob.getPath().getParent());
    }

    @Test
    void getBackupFileIsUnique() throws Exception
    {
        this.filesystemStoreTools.initialize();

        BlobPath originalPath = BlobPath.absolute("test", "file.txt");
        Blob originalBlob = this.blobStore.getBlob(originalPath);

        Blob backupBlob1 = this.filesystemStoreTools.getBackupFile(originalBlob);
        Blob backupBlob2 = this.filesystemStoreTools.getBackupFile(originalBlob);

        // Each call should generate a unique path
        assertNotEquals(backupBlob1.getPath(), backupBlob2.getPath());
    }

    @Test
    void getTempFileIsUnique() throws Exception
    {
        this.filesystemStoreTools.initialize();

        BlobPath originalPath = BlobPath.absolute("test", "file.txt");
        Blob originalBlob = this.blobStore.getBlob(originalPath);

        Blob tempBlob1 = this.filesystemStoreTools.getTempFile(originalBlob);
        Blob tempBlob2 = this.filesystemStoreTools.getTempFile(originalBlob);

        // Each call should generate a unique path
        assertNotEquals(tempBlob1.getPath(), tempBlob2.getPath());
    }

    @Test
    void getAttachmentDirWithDifferentAttachmentNames()
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        AttachmentReference attachRef1 = new AttachmentReference("file1.txt", docRef);
        AttachmentReference attachRef2 = new AttachmentReference("file2.txt", docRef);

        BlobPath attachDir1 = this.filesystemStoreTools.getAttachmentDir(attachRef1);
        BlobPath attachDir2 = this.filesystemStoreTools.getAttachmentDir(attachRef2);

        // Different attachment names should result in different directories
        assertNotEquals(attachDir1, attachDir2);
    }

    @Test
    void getDocumentContentDirWithDifferentDocuments()
    {
        DocumentReference docRef1 = new DocumentReference("wiki", "Space", "Page1");
        DocumentReference docRef2 = new DocumentReference("wiki", "Space", "Page2");

        BlobPath docDir1 = this.filesystemStoreTools.getDocumentContentDir(docRef1);
        BlobPath docDir2 = this.filesystemStoreTools.getDocumentContentDir(docRef2);

        // Different documents should have different directories
        assertNotEquals(docDir1, docDir2);
    }

    @Test
    void getLinkContent() throws Exception
    {
        DocumentReference docRef = new DocumentReference("wiki", "Space", "Page");
        AttachmentReference attachRef = new AttachmentReference("file.txt", docRef);

        com.xpn.xwiki.doc.XWikiAttachment attachment = mock();
        when(attachment.getReference()).thenReturn(attachRef);
        when(attachment.getVersion()).thenReturn("1.1");

        String linkContent = this.filesystemStoreTools.getLinkContent(attachment);

        assertEquals("fv1.1.txt", linkContent);
    }
}
