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
package org.xwiki.rest.internal.resources.attachments;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import javax.activation.DataHandler;
import javax.inject.Named;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.tika.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.rest.internal.resources.AbstractAttachmentsResourceTest;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttachmentsResourceImpl}.
 * 
 * @version $Id$
 */
@OldcoreTest
public class AttachmentsResourceImplTest extends AbstractAttachmentsResourceTest
{
    @InjectMockComponents
    AttachmentsResourceImpl attachmentsResource;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    @Named("currentgetdocument")
    DocumentReferenceResolver<EntityReference> currentGetDocumentReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @BeforeEach
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        setUriInfo(this.attachmentsResource);
        this.oldCore.registerMockEnvironment();
        this.xcontext.setRequest(mock(XWikiRequest.class));
    }

    @Test
    public void getAttachments() throws Exception
    {
        this.xcontext.setWikiId("other");

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("select doc.space, doc.name, doc.version, attachment "
            + "from XWikiDocument as doc, XWikiAttachment as attachment "
            + "where attachment.docId = doc.id and doc.fullName = :localDocumentReference and"
            + " upper(attachment.author) like :author and upper(attachment.filename) like :name and"
            + " (attachment.mimeType is null or attachment.mimeType = '' or upper(attachment.mimeType) like :mediaType0"
            + " or upper(attachment.filename) like :extension0)", Query.HQL)).thenReturn(query);
        mockContainsQueryParam(query, "author", "MFLOREA");
        mockContainsQueryParam(query, "name", "LOGO");
        mockContainsQueryParam(query, "mediaType0", "VIDEO/");
        mockSuffixQueryParam(query, "extension0", ".PNG");
        when(query.setOffset(10)).thenReturn(query);
        when(query.setLimit(5)).thenReturn(query);

        XWikiAttachment imageAttachment = mock(XWikiAttachment.class, "image");
        when(imageAttachment.getFilename()).thenReturn("logo.pNg");

        XWikiAttachment videoAttachment = mock(XWikiAttachment.class, "video");
        when(videoAttachment.getFilename()).thenReturn("video.mp4");
        when(videoAttachment.getMimeType(this.xcontext)).thenReturn("video/mp4");

        XWikiAttachment textAttachment = mock(XWikiAttachment.class, "text");
        when(textAttachment.getFilename()).thenReturn("plain.txt");
        when(textAttachment.getMimeType(this.xcontext)).thenReturn("text/plain");

        List<Object> results = Arrays.asList(new Object[] {"Path.To", "Page", "1.3", videoAttachment},
            new Object[] {"Path.To", "Page", "1.3", textAttachment},
            new Object[] {"Path.To", "Page", "1.3", imageAttachment});
        when(query.execute()).thenReturn(results);

        DocumentReference documentReference = new DocumentReference("test", Arrays.asList("Path", "To"), "Page");
        when(this.defaultSpaceReferenceResover.resolve(eq("Path.To"), any()))
            .thenReturn(documentReference.getLastSpaceReference());
        when(this.localEntityReferenceSerializer.serialize(documentReference)).thenReturn("Path.To.Page");

        Attachment videoRestAttachment = mock(Attachment.class, "video");
        Attachment imageRestAttachment = mock(Attachment.class, "image");
        when(this.modelFactory.toRestAttachment(eq(this.uriInfo.getBaseUri()), any(), eq(true), eq(false)))
            .thenReturn(videoRestAttachment, imageRestAttachment);

        Attachments attachments = this.attachmentsResource.getAttachments("test", "Path/spaces/To", "Page", 10, 5, true,
            "logo", "mflorea", ".png,video/");

        verify(query).bindValue("localDocumentReference", "Path.To.Page");
        verify(this.modelFactory, times(2)).toRestAttachment(eq(this.uriInfo.getBaseUri()), any(), eq(true), eq(false));

        assertEquals(Arrays.asList(videoRestAttachment, imageRestAttachment), attachments.getAttachments());
        assertEquals("other", this.xcontext.getWikiId());
    }

    @Test
    public void createAttachment() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("test", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = createXWikiDocument(documentReference, "test:Path.To.Page", true, true);

        AttachmentReference attachmentReference = new AttachmentReference("myBio.txt", documentReference);
        when(this.currentGetDocumentReferenceResolver.resolve(attachmentReference)).thenReturn(documentReference);

        Multipart multipart = createMultipart("bio.txt", "myBio.txt", "blah", "text/plain");

        Attachment attachment = mock(Attachment.class);
        when(this.modelFactory.toRestAttachment(eq(this.uriInfo.getBaseUri()), any(com.xpn.xwiki.api.Attachment.class),
            eq(false), eq(false))).thenReturn(attachment);

        Response response = this.attachmentsResource.addAttachment("test", "Path/spaces/To", "Page", multipart, false);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(attachment, response.getEntity());

        // The cached document should not have been modified.
        assertNull(document.getAttachment("myBio.txt"));

        XWikiAttachment xwikiAttachment =
            this.xwiki.getDocument(documentReference, this.xcontext).getAttachment("myBio.txt");
        assertEquals("myBio.txt", xwikiAttachment.getFilename());
        assertEquals("text/plain", xwikiAttachment.getMimeType());
        assertEquals("blah", IOUtils.toString(xwikiAttachment.getContentInputStream(this.xcontext)));
    }

    @Test
    public void updateAttachment() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("test", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = createXWikiDocument(documentReference, "test:Path.To.Page", true, true);

        document.setAttachment("pom.xml", new ByteArrayInputStream("<pom/>".getBytes()), this.xcontext);
        this.xwiki.saveDocument(document, this.xcontext);

        AttachmentReference attachmentReference = new AttachmentReference("pom.xml", documentReference);
        when(this.currentGetDocumentReferenceResolver.resolve(attachmentReference)).thenReturn(documentReference);

        Multipart multipart = createMultipart("pom.xml", null, "<project/>", "application/xml");

        Attachment attachment = mock(Attachment.class);
        when(this.modelFactory.toRestAttachment(eq(this.uriInfo.getBaseUri()), any(com.xpn.xwiki.api.Attachment.class),
            eq(true), eq(false))).thenReturn(attachment);

        Response response = this.attachmentsResource.addAttachment("test", "Path/spaces/To", "Page", multipart, true);

        assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertEquals(attachment, response.getEntity());

        XWikiAttachment xwikiAttachment =
            this.xwiki.getDocument(documentReference, this.xcontext).getAttachment("pom.xml");
        assertEquals("pom.xml", xwikiAttachment.getFilename());
        assertEquals("application/xml", xwikiAttachment.getMimeType());
        assertEquals("<project/>", IOUtils.toString(xwikiAttachment.getContentInputStream(this.xcontext)));
    }

    private Multipart createMultipart(String originalFileName, String overwritingFileName, String content,
        String mediaType) throws Exception
    {
        MimeMultipart multipart = new MimeMultipart();

        if (originalFileName != null && content != null) {
            MimeBodyPart filePart = new MimeBodyPart();
            filePart.setDisposition("form-data");
            filePart.setFileName(originalFileName);
            filePart.setDataHandler(new DataHandler(new ByteArrayDataSource(content, mediaType)));
            multipart.addBodyPart(filePart);
        }

        if (overwritingFileName != null) {
            MimeBodyPart fileNamePart = new MimeBodyPart();
            fileNamePart.setHeader("Content-Disposition", "form-data; name=\"filename\"");
            fileNamePart.setText(overwritingFileName);
            multipart.addBodyPart(fileNamePart);
        }

        return multipart;
    }

    private XWikiDocument createXWikiDocument(DocumentReference documentReference, String serializedDocumentReference,
        boolean hasView, boolean hasEdit) throws Exception
    {
        when(this.defaultEntityReferenceSerializer.serialize(documentReference))
            .thenReturn(serializedDocumentReference);

        XWikiDocument document = new XWikiDocument(documentReference);
        this.xwiki.saveDocument(document, this.xcontext);

        when(this.oldCore.getMockRightService().hasAccessLevel("view", "XWiki.XWikiGuest", "test:Path.To.Page",
            this.xcontext)).thenReturn(hasView);
        when(this.authorization.hasAccess(Right.EDIT, documentReference)).thenReturn(hasEdit);

        return this.xwiki.getDocument(documentReference, this.xcontext);
    }
}
