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
package org.xwiki.store.legacy.store.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.locks.dummy.internal.DummyLockProvider;
import org.xwiki.store.serialization.xml.internal.AttachmentListMetadataSerializer;
import org.xwiki.store.serialization.xml.internal.AttachmentMetadataSerializer;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for FilesystemAttachmentStore.
 *
 * @version $Id$
 * @since 3.0M2
 */
@ComponentTest
@ComponentList({
    FilesystemStoreTools.class,
    DummyLockProvider.class,
    AttachmentListMetadataSerializer.class,
    AttachmentMetadataSerializer.class
})
class FilesystemAttachmentStoreTest
{
    private static final String HELLO = "Hello World";

    private static final byte[] HELLO_BYTES;

    private static final InputStream HELLO_STREAM;

    @XWikiTempDir
    private File tmpDir;

    @InjectMockComponents
    private FilesystemAttachmentStore attachStore;

    @MockComponent
    private BlobStoreManager blobStoreManager;

    @MockComponent
    private AttachmentVersioningStore mockAttachVersionStore;

    @Mock
    private XWikiAttachment mockAttach;

    @Mock
    private XWikiAttachmentArchive mockArchive;

    @Mock
    private Session mockHibernateSession;

    @Mock
    private XWikiHibernateStore mockHibernate;

    @Mock
    private XWikiContext mockContext;

    private AttachmentReference mockAttachReference;
    private FilesystemStoreTools fileTools;
    private XWikiDocument doc;

    /**
     * The file which will hold content for this attachment.
     */
    private Blob storeFile;

    static {
        try {
            HELLO_BYTES = HELLO.getBytes("UTF-8");
            HELLO_STREAM = new ByteArrayInputStream(HELLO_BYTES);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8!!");
        }
    }

    @BeforeComponent
    void beforeComponent() throws BlobStoreException
    {
        FileSystemBlobStoreProperties fileSystemBlobStoreProperties = new FileSystemBlobStoreProperties();
        fileSystemBlobStoreProperties.setRootDirectory(this.tmpDir.toPath());
        BlobStore blobStore = new FileSystemBlobStore("store/file", fileSystemBlobStoreProperties);
        when(blobStoreManager.getBlobStore("store/file")).thenReturn(blobStore);
    }

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        this.fileTools = componentManager.getInstance(FilesystemStoreTools.class);
        this.doc = new XWikiDocument(new DocumentReference("xwiki", "Main", "WebHome"));
        this.mockAttachReference = new AttachmentReference("file.name", doc.getDocumentReference());
        XWiki xWiki = mock(XWiki.class);
        when(this.mockContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getStore()).thenReturn(this.mockHibernate);
        when(xWiki.getHibernateStore()).thenReturn(this.mockHibernate);
        when(xWiki.getDefaultAttachmentArchiveStore()).thenReturn(this.mockAttachVersionStore);

        when(this.mockHibernate.getSession(this.mockContext)).thenReturn(this.mockHibernateSession);

        when(this.mockAttach.getContentInputStream(this.mockContext)).thenReturn(HELLO_STREAM);
        when(this.mockAttach.getDoc()).thenReturn(this.doc);
        when(this.mockAttach.getFilename()).thenReturn(mockAttachReference.getName());
        when(this.mockAttach.getReference()).thenReturn(mockAttachReference);
        when(this.mockAttach.getAttachment_archive()).thenReturn(this.mockArchive);
        when(this.mockAttach.getArchiveStore()).thenReturn("file");

        XWikiAttachmentContent attachmentContent = mock(XWikiAttachmentContent.class);
        when(this.mockAttach.getAttachment_content()).thenReturn(attachmentContent);
        when(this.mockAttach.isContentDirty()).thenReturn(true);
        when(attachmentContent.isContentDirty()).thenReturn(true);

        this.storeFile = this.fileTools.getAttachmentFileProvider(this.mockAttachReference).getAttachmentContentBlob();
        HELLO_STREAM.reset();
    }

    @Test
    void saveContentTest() throws Exception
    {
        final Blob storeFile =
            this.fileTools.getAttachmentFileProvider(this.mockAttachReference).getAttachmentContentBlob();
        assertFalse(this.storeFile.exists());

        this.attachStore.saveAttachmentContent(this.mockAttach, false, this.mockContext, false);

        assertTrue(this.storeFile.exists(), "The attachment file was not created.");
        assertEquals(HELLO, IOUtils.toString(storeFile.getStream(), StandardCharsets.UTF_8),
            "The attachment file contained the wrong content");
        verify(mockAttachVersionStore).saveArchive(mockArchive, mockContext, false);
    }

    @Test
    void saveTwoOfSameAttachmentInOneTransactionTest() throws Exception
    {
        final Blob storeFile =
            this.fileTools.getAttachmentFileProvider(this.mockAttachReference).getAttachmentContentBlob();
        assertFalse(this.storeFile.exists());

        final List<XWikiAttachment> attachments = new ArrayList<XWikiAttachment>();
        attachments.add(this.mockAttach);
        attachments.add(this.mockAttach);
        this.attachStore.saveAttachmentsContent(attachments, this.doc, false, this.mockContext, false);

        assertTrue(this.storeFile.exists(), "The attachment file was not created.");
        assertEquals(HELLO, IOUtils.toString(storeFile.getStream(), StandardCharsets.UTF_8),
            "The attachment file contained the wrong content");
        verify(mockAttachVersionStore, times(2)).saveArchive(mockArchive, mockContext, false);
    }

    @Test
    void loadContentTest() throws Exception
    {
        doAnswer(invocationOnMock -> {
            FilesystemAttachmentContent content = invocationOnMock.getArgument(0);
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(content.getContentInputStream(), baos);

                final String output = baos.toString("UTF-8");

                assertEquals(HELLO.length(), content.getLongSize());
                assertEquals(HELLO.length(), content.getSize());
                assertEquals(HELLO, output, "Not the same attachment content.");
                return null;
            } catch (IOException e) {
                throw new RuntimeException("Exception getting attachment content.", e);
            }
        }).when(this.mockAttach).setAttachment_content(any(FilesystemAttachmentContent.class));
        this.storeFile.writeFromStream(HELLO_STREAM);
        this.attachStore.loadAttachmentContent(this.mockAttach, this.mockContext, false);

        verify(mockAttach).setAttachment_content(any(FilesystemAttachmentContent.class));
        verify(mockAttach).setContentStore("file");
    }

    @Test
    void deleteAttachmentTest() throws Exception
    {
        this.createFile();

        this.attachStore.deleteXWikiAttachment(this.mockAttach, false, this.mockContext, false);
        assertFalse(this.storeFile.exists(), "The attachment file was not deleted.");
        verify(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
        verify(mockHibernateSession, times(2)).delete(any());
    }

    @Test
    void documentUpdateOnDeleteTest() throws Exception
    {
        doAnswer(invocationOnMock -> {
            XWikiDocument doc = invocationOnMock.getArgument(0);
            assertTrue(doc.getAttachmentList().isEmpty(), "Attachment was not removed from the list.");
            return null;
        }).when(this.mockHibernate).saveXWikiDoc(doc, mockContext, false);
        final List<XWikiAttachment> attachList = new ArrayList<XWikiAttachment>();
        attachList.add(this.mockAttach);
        this.doc.setAttachmentList(attachList);
        this.createFile();

        this.attachStore.deleteXWikiAttachment(this.mockAttach, true, this.mockContext, false);
        verify(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
        verify(mockHibernateSession, times(2)).delete(any());
        verify(mockHibernate).saveXWikiDoc(doc, mockContext, false);
    }

    @Test
    void documentUpdateOnSaveTest() throws Exception
    {
        this.attachStore.saveAttachmentContent(this.mockAttach, true, this.mockContext, false);
        verify(this.mockHibernate).saveXWikiDoc(doc, mockContext, false);
        verify(mockAttachVersionStore).saveArchive(mockArchive, mockContext, false);
    }

    /* -------------------- Helpers -------------------- */

    private void createFile() throws BlobStoreException
    {
        this.storeFile.writeFromStream(HELLO_STREAM);
        assertTrue(this.storeFile.exists(), "The attachment file not created for the test.");
    }
}
