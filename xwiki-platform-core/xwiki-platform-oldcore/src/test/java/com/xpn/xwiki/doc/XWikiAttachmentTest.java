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
package com.xpn.xwiki.doc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.environment.Environment;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.LogRule;

import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.component.XWikiDocumentFilterUtilsComponentList;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiAttachment}.
 * 
 * @version $Id$
 */
@XWikiDocumentFilterUtilsComponentList
public class XWikiAttachmentTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    @Rule
    public LogRule logger = new LogRule();

    @Before
    public void configure() throws Exception
    {
        this.logger.recordLoggingForType(XWikiAttachment.class);

        this.oldcore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        this.oldcore.getMocker().registerMockComponent(AttachmentReferenceResolver.TYPE_STRING, "current");
        this.oldcore.getMocker().registerMockComponent(Environment.class);
    }

    /**
     * Unit test for <a href="https://jira.xwiki.org/browse/XWIKI-9075">XWIKI-9075</a> to prove that calling
     * {@code fromXML} doesn't set the metadata dirty flag.
     * <p>
     * Note: I think there's a bug in that fromXML should return a new instance of XWikiAttachment and not modify the
     * current one as this would mean changing its identity...
     */
    @Test
    public void fromXML() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.fromXML("<attachment>\n" + "<filename>XWikiLogo.png</filename>\n" + "<filesize>1390</filesize>\n"
            + "<mimetype>image/png2</mimetype>\n" + "<author>xwiki:XWiki.Admin</author>\n"
            + "<date>1252454400000</date>\n" + "<version>1.1</version>\n" + "<comment/>\n"
            + "<content>MDEyMzQ1Njc4OQ==</content>\n" + "</attachment>");

        assertEquals("XWikiLogo.png", attachment.getFilename());
        assertEquals(new Date(1252454400000L), attachment.getDate());
        assertEquals("1.1", attachment.getVersion());
        assertEquals("0123456789", IOUtils.toString(attachment.getContentInputStream(null)));
        assertEquals("image/png2", attachment.getMimeType());
        assertEquals("image/png2", attachment.getMimeType(null));

        assertFalse(attachment.isMetaDataDirty());
    }

    /** An InputStream which will return a stream of random bytes of length given in the constructor. */
    private static class RandomInputStream extends InputStream
    {
        private int bytes;

        private int state;

        public RandomInputStream(final int bytes, final int seed)
        {
            this.bytes = bytes;
            this.state = seed;
        }

        @Override
        public int available()
        {
            return this.bytes;
        }

        @Override
        public int read()
        {
            if (this.bytes == 0) {
                return -1;
            }
            this.bytes--;
            this.state = this.state << 13 | this.state >>> 19;
            return ++this.state & 0xff;
        }
    }

    // Tests

    @Test
    public void testGetSize() throws IOException
    {
        XWikiAttachment attachment = new XWikiAttachment();

        assertEquals(-1, attachment.getLongSize());

        attachment.setLongSize(42);

        assertEquals(42, attachment.getLongSize());

        attachment.setContent(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }));

        assertEquals(4, attachment.getLongSize());

        attachment.setLongSize(42);

        assertEquals(4, attachment.getLongSize());
    }

    @Test
    public void testGetVersionList() throws Exception
    {
        final XWikiAttachment attach = new XWikiAttachment();
        attach.setVersion("1.1");
        assertEquals("Version list was not one element long for version 1.1", 1, attach.getVersionList().size());
        attach.setVersion("1.2");
        assertEquals("Version list was not two elements long for version 1.2.", 2, attach.getVersionList().size());
        attach.setVersion("1.3");
        assertEquals("Version list was not two elements long for version 1.3.", 3, attach.getVersionList().size());
    }

    /**
     * Create an attachment, populate it with enough data to make it flush to disk cache, read back data and make sure
     * it's the same.
     */
    @Test
    public void testStoreContentInDiskCache() throws Exception
    {
        int attachLength = 20000;
        // Check for data dependent errors.
        int seed = (int) System.currentTimeMillis();
        final XWikiAttachment attach = new XWikiAttachment();
        final InputStream ris = new RandomInputStream(attachLength, seed);
        attach.setContent(ris);
        assertEquals("Not all of the stream was read", 0, ris.available());
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed),
            attach.getAttachment_content().getContentInputStream()));
    }

    @Test
    public void testSetContentViaOutputStream() throws Exception
    {
        int attachLength = 20;
        int seed = (int) System.currentTimeMillis();
        final XWikiAttachment attach = new XWikiAttachment();
        final InputStream ris = new RandomInputStream(attachLength, seed);
        attach.setContent(ris);
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed),
            attach.getAttachment_content().getContentInputStream()));
        // Now write to the attachment via an OutputStream.
        final XWikiAttachmentContent xac = attach.getAttachment_content();
        xac.setContentDirty(false);
        final OutputStream os = xac.getContentOutputStream();

        // Adding content with seed+1 will make a radically different set of content.
        IOUtils.copy(new RandomInputStream(attachLength, seed + 1), os);

        // It should still be the old content.
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed), xac.getContentInputStream()));
        assertFalse(xac.isContentDirty());

        os.close();

        // Now it should be the new content.
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed + 1), xac.getContentInputStream()));
        assertTrue(xac.isContentDirty());
    }

    @Test
    public void testSetContentWithMaxSize() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new ReaderInputStream(new StringReader("123456789")), 5);

        assertEquals("12345", IOUtils.toString(attachment.getContentInputStream(null)));
    }

    @Test
    public void testGetMime() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setFilename("image.jpg");

        assertNull("image/jpeg", attachment.getMimeType());
        assertEquals("image/jpeg", attachment.getMimeType(null));

        attachment.setFilename("xml.xml");

        assertEquals("application/xml", attachment.getMimeType(null));

        attachment.setFilename("zip.zip");

        assertEquals("application/zip", attachment.getMimeType(null));

        attachment.setFilename("unknown");
        attachment.setDoc(new XWikiDocument(new DocumentReference("wiki", "Space", "Page")));
        assertEquals(0, this.logger.size());
        assertEquals("application/octet-stream", attachment.getMimeType(null));
        assertTrue(this.logger.contains("Failed to read the content of "
            + "[Attachment wiki:Space.Page@unknown] in order to detect its mime type."));

        // Test content-based detection.
        attachment.setFilename("unknown");
        attachment.setContent(new ByteArrayInputStream("content".getBytes()));
        assertEquals("text/plain", attachment.getMimeType(null));
    }

    @Test
    public void testSetMimeType()
    {
        XWikiAttachment attachment = new XWikiAttachment();

        assertEquals(null, attachment.getMimeType());
        assertEquals("application/octet-stream", attachment.getMimeType(null));

        attachment.setMimeType("image/jpeg");

        assertEquals("image/jpeg", attachment.getMimeType());
        assertEquals("image/jpeg", attachment.getMimeType(null));
    }

    @Test
    public void testResetMimeType()
    {
        XWikiAttachment attachment = new XWikiAttachment();

        assertEquals(null, attachment.getMimeType());
        assertEquals("application/octet-stream", attachment.getMimeType(null));

        attachment.resetMimeType(null);

        assertEquals("application/octet-stream", attachment.getMimeType());
        assertEquals("application/octet-stream", attachment.getMimeType(null));
    }

    @Test
    public void testAuthorWithDocument() throws Exception
    {
        EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer =
            this.oldcore.getMocker().getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        DocumentReferenceResolver<EntityReference> explicitDocumentReferenceResolver =
            this.oldcore.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "explicit");
        EntityReferenceResolver<String> xclassEntityReferenceResolver =
            this.oldcore.getMocker().registerMockComponent(EntityReferenceResolver.TYPE_STRING, "xclass");

        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiAttachment attachment = new XWikiAttachment(document, "filename");

        // getAuthor() based on getAuthorReference()
        DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        attachment.setAuthorReference(userReference);
        assertEquals(userReference, attachment.getAuthorReference());
        when(compactWikiEntityReferenceSerializer.serialize(userReference, attachment.getReference()))
            .thenReturn("stringUserReference");
        assertEquals("stringUserReference", attachment.getAuthor());

        // getAuthorReference() based on getAuthor()
        attachment.setAuthor("author");
        assertEquals("author", attachment.getAuthor());
        userReference = new DocumentReference("wiki", "XWiki", "author");
        EntityReference relativeUserReference = userReference.removeParent(userReference.getWikiReference());
        when(xclassEntityReferenceResolver.resolve("author", EntityType.DOCUMENT)).thenReturn(relativeUserReference);
        when(explicitDocumentReferenceResolver.resolve(relativeUserReference, attachment.getReference()))
            .thenReturn(userReference);
        assertEquals(userReference, attachment.getAuthorReference());

        // Guest author.
        attachment.setAuthor(XWikiRightService.GUEST_USER);
        userReference = new DocumentReference("wiki", "XWiki", XWikiRightService.GUEST_USER);
        relativeUserReference = userReference.removeParent(userReference.getWikiReference());
        when(xclassEntityReferenceResolver.resolve(any(String.class), eq(EntityType.DOCUMENT)))
            .thenReturn(relativeUserReference);
        when(explicitDocumentReferenceResolver.resolve(relativeUserReference, attachment.getReference()))
            .thenReturn(userReference);
        assertNull(attachment.getAuthorReference());
    }

    @Test
    public void testAuthorWithoutDocument() throws Exception
    {
        EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer =
            this.oldcore.getMocker().getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        AttachmentReferenceResolver<String> currentAttachmentReferenceResolver =
            this.oldcore.getMocker().getInstance(AttachmentReferenceResolver.TYPE_STRING, "current");
        DocumentReferenceResolver<EntityReference> explicitDocumentReferenceResolver =
            this.oldcore.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "explicit");
        EntityReferenceResolver<String> xclassEntityReferenceResolver =
            this.oldcore.getMocker().registerMockComponent(EntityReferenceResolver.TYPE_STRING, "xclass");

        XWikiAttachment attachment = new XWikiAttachment(null, "filename");
        DocumentReference currentDocumentReference =
            new DocumentReference("currentWiki", "currentSpage", "currentPage");
        AttachmentReference attachmentReference =
            new AttachmentReference(attachment.getFilename(), currentDocumentReference);

        // getAuthor() based on getAuthorReference()
        DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        attachment.setAuthorReference(userReference);
        assertEquals(userReference, attachment.getAuthorReference());
        when(currentAttachmentReferenceResolver.resolve(attachment.getFilename())).thenReturn(attachmentReference);
        when(compactWikiEntityReferenceSerializer.serialize(userReference, attachmentReference))
            .thenReturn("stringUserReference");
        assertEquals("stringUserReference", attachment.getAuthor());

        // getAuthorReference() based on getAuthor()
        attachment.setAuthor("author");
        assertEquals("author", attachment.getAuthor());
        userReference = new DocumentReference("wiki", "XWiki", "author");
        EntityReference relativeUserReference = userReference.removeParent(userReference.getWikiReference());
        when(xclassEntityReferenceResolver.resolve("author", EntityType.DOCUMENT)).thenReturn(relativeUserReference);
        when(explicitDocumentReferenceResolver.resolve(relativeUserReference, attachment.getReference()))
            .thenReturn(userReference);
        assertEquals(userReference, attachment.getAuthorReference());
    }

    @Test
    public void getContentInputStreamForLatestVersion() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Space", "Page"));

        when(this.oldcore.getXWikiContext().getWiki().getDocument(document.getDocumentReference(),
            this.oldcore.getXWikiContext())).thenReturn(document);

        XWikiAttachment attachment = new XWikiAttachment(document, "file.txt");
        when(document.getAttachment(attachment.getFilename())).thenReturn(attachment);
        attachment.setVersion("3.5");

        try {
            attachment.getContentInputStream(this.oldcore.getXWikiContext());
            fail();
        } catch (NullPointerException e) {
            // Expected because the attachment content is not set. The attachment content is normally set by the
            // loadAttachmentContent call we verify below.
        }
    }

    @Test
    public void getContentInputStreamFromArchive() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Space", "Page"));

        when(this.oldcore.getXWikiContext().getWiki().getDocument(document.getDocumentReference(),
            this.oldcore.getXWikiContext())).thenReturn(document);

        XWikiAttachment attachment = new XWikiAttachment(document, "file.txt");
        attachment.setVersion("3.5");

        XWikiAttachment newAttachment = new XWikiAttachment(document, attachment.getFilename());
        newAttachment.setVersion("5.1");
        when(document.getAttachment(attachment.getFilename())).thenReturn(newAttachment);

        XWikiAttachmentContent content = mock(XWikiAttachmentContent.class);
        when(content.getContentInputStream()).thenReturn(mock(InputStream.class));

        XWikiAttachment archivedAttachment = new XWikiAttachment(document, attachment.getFilename());
        archivedAttachment.setAttachment_content(content);

        XWikiAttachmentArchive archive = mock(XWikiAttachmentArchive.class);
        when(archive.getRevision(attachment, attachment.getVersion(), this.oldcore.getXWikiContext()))
            .thenReturn(archivedAttachment);

        AttachmentVersioningStore store = mock(AttachmentVersioningStore.class);
        when(this.oldcore.getXWikiContext().getWiki().getDefaultAttachmentArchiveStore()).thenReturn(store);
        when(store.loadArchive(attachment, this.oldcore.getXWikiContext(), true)).thenReturn(archive);

        assertSame(content.getContentInputStream(), attachment.getContentInputStream(this.oldcore.getXWikiContext()));
    }
}
