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

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Provider;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletSession;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

import jakarta.servlet.http.HttpSession;

/**
 * Tests for {@link DefaultTemporaryAttachmentSessionsManager}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
@ComponentList(TestEnvironment.class)
class DefaultTemporaryAttachmentSessionsManagerTest
{
    private static final String ATTRIBUTE_KEY = "xwikiTemporaryAttachments";

    @InjectMockComponents
    private DefaultTemporaryAttachmentSessionsManager attachmentManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Provider<AttachmentValidator> attachmentValidatorProvider;

    @MockComponent
    private Container container;

    @Mock
    private AttachmentValidator attachmentValidator;

    @Mock
    private XWikiContext context;

    @Mock
    private HttpSession httpSession;

    @BeforeEach
    void setup(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.context);

        ServletSession session = mock(ServletSession.class);
        when(session.getJakartaHttpSession()).thenReturn(this.httpSession);
        when(this.container.getSession()).thenReturn(session);
        Utils.setComponentManager(mockitoComponentManager);

        when(this.attachmentValidatorProvider.get()).thenReturn(this.attachmentValidator);
    }

    @Test
    void uploadAttachment() throws Exception
    {
        String sessionId = "mySession";
        when(this.httpSession.getId()).thenReturn(sessionId);

        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Document");
        Part part = mock(Part.class);

        String filename = "fileFoo.xml";
        when(part.getSubmittedFileName()).thenReturn(filename);
        InputStream inputStream = new ByteArrayInputStream("foo".getBytes(UTF_8));
        when(part.getInputStream()).thenReturn(inputStream);

        XWiki xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xwiki);
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userReference);
        SpaceReference lastSpaceReference = documentReference.getLastSpaceReference();
        when(xwiki.getSpacePreference(UPLOAD_MAXSIZE_PARAMETER, lastSpaceReference, this.context)).thenReturn("42");
        when(part.getSize()).thenReturn(41L);

        doAnswer(invocationOnMock -> {
            TemporaryAttachmentSession temporaryAttachmentSession = invocationOnMock.getArgument(1);
            assertEquals(sessionId, temporaryAttachmentSession.getSessionId());
            return null;
        }).when(this.httpSession).setAttribute(eq(ATTRIBUTE_KEY), any(TemporaryAttachmentSession.class));

        XWikiAttachment attachment = this.attachmentManager.uploadAttachment(documentReference, part);
        assertNotNull(attachment);
        assertEquals(filename, attachment.getFilename());
        assertEquals(userReference, attachment.getAuthorReference());
        assertEquals(documentReference, attachment.getDoc().getDocumentReference());

        verify(this.httpSession).setAttribute(eq(ATTRIBUTE_KEY), any(TemporaryAttachmentSession.class));
        verify(part).getSubmittedFileName();
    }

    @Test
    void uploadAttachmentWithFilename() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Document");
        SpaceReference spaceReference = documentReference.getLastSpaceReference();

        XWiki xwiki = mock(XWiki.class);
        Part part = mock(Part.class);

        when(this.context.getWiki()).thenReturn(xwiki);
        when(xwiki.getSpacePreference(UPLOAD_MAXSIZE_PARAMETER, spaceReference, this.context))
            .thenReturn("42");

        when(part.getInputStream()).thenReturn(new ByteArrayInputStream("foo".getBytes(UTF_8)));

        XWikiAttachment xWikiAttachment =
            this.attachmentManager.uploadAttachment(documentReference, part, "newFilename");
        assertEquals("newFilename", xWikiAttachment.getFilename());
        verify(part, never()).getSubmittedFileName();
    }

    @Test
    void uploadAttachmentInvalid() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Document");
        SpaceReference spaceReference = documentReference.getLastSpaceReference();

        XWiki xwiki = mock(XWiki.class);
        Part part = mock(Part.class);

        when(this.context.getWiki()).thenReturn(xwiki);
        when(xwiki.getSpacePreference(UPLOAD_MAXSIZE_PARAMETER, spaceReference, this.context))
            .thenReturn("42");

        when(part.getInputStream()).thenReturn(new ByteArrayInputStream("foo".getBytes(UTF_8)));

        doThrow(AttachmentValidationException.class).when(this.attachmentValidator).validateAttachment(any());

        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        when(this.httpSession.getAttribute(ATTRIBUTE_KEY)).thenReturn(temporaryAttachmentSession);

        assertThrows(AttachmentValidationException.class,
            () -> this.attachmentManager.uploadAttachment(documentReference, part, "newFilename"));

        verify(part, never()).getSubmittedFileName();
        verifyNoInteractions(temporaryAttachmentSession);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "",
        " ",
        "  ",
        "\t"
    })
    void uploadAttachmentWithEmptyFilename(String filename) throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Document");
        SpaceReference spaceReference = documentReference.getLastSpaceReference();

        XWiki xwiki = mock(XWiki.class);
        Part part = mock(Part.class);

        when(this.context.getWiki()).thenReturn(xwiki);
        when(xwiki.getSpacePreference(UPLOAD_MAXSIZE_PARAMETER, spaceReference, this.context))
            .thenReturn("42");

        when(part.getInputStream()).thenReturn(new ByteArrayInputStream("foo".getBytes(UTF_8)));
        when(part.getSubmittedFileName()).thenReturn("partFilename");

        XWikiAttachment xWikiAttachment = this.attachmentManager.uploadAttachment(documentReference, part, filename);
        assertEquals("partFilename", xWikiAttachment.getFilename());
    }

    @Test
    void getUploadedAttachments()
    {
        String sessionId = "uploadedAttachments";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        when(httpSession.getAttribute(ATTRIBUTE_KEY)).thenReturn(temporaryAttachmentSession);
        DocumentReference documentReference = mock(DocumentReference.class);

        XWikiAttachment attachment1 = mock(XWikiAttachment.class);
        XWikiAttachment attachment2 = mock(XWikiAttachment.class);
        XWikiAttachment attachment3 = mock(XWikiAttachment.class);

        List<XWikiAttachment> expectedList = Arrays.asList(attachment1, attachment2, attachment3);
        when(temporaryAttachmentSession.getAttachments(documentReference)).thenReturn(expectedList);

        assertEquals(expectedList,
            this.attachmentManager.getUploadedAttachments(documentReference));
    }

    @Test
    void getUploadedAttachment()
    {
        String sessionId = "uploadedAttachmentSingular";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        when(httpSession.getAttribute(ATTRIBUTE_KEY)).thenReturn(temporaryAttachmentSession);

        DocumentReference documentReference = mock(DocumentReference.class);
        String filename = "foobar";

        XWikiAttachment attachment1 = mock(XWikiAttachment.class);

        when(temporaryAttachmentSession.getAttachment(documentReference, filename))
            .thenReturn(Optional.of(attachment1));
        assertEquals(Optional.of(attachment1),
            this.attachmentManager.getUploadedAttachment(documentReference, filename));
    }

    @Test
    void removeUploadedAttachment()
    {
        String sessionId = "removeUploadedAttachment";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        when(httpSession.getAttribute(ATTRIBUTE_KEY)).thenReturn(temporaryAttachmentSession);

        DocumentReference documentReference = mock(DocumentReference.class);
        String filename = "foobar";
        when(temporaryAttachmentSession.removeAttachment(documentReference, filename)).thenReturn(true);
        assertTrue(this.attachmentManager.removeUploadedAttachment(documentReference, filename));
    }

    @Test
    void removeUploadedAttachments()
    {
        String sessionId = "removeUploadedAttachmentsPlural";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        when(httpSession.getAttribute(ATTRIBUTE_KEY)).thenReturn(temporaryAttachmentSession);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(temporaryAttachmentSession.removeAttachments(documentReference)).thenReturn(true);
        assertTrue(this.attachmentManager.removeUploadedAttachments(documentReference));
    }

    @Test
    void temporarilyAttach() throws TemporaryAttachmentException
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        String sessionId = "removeUploadedAttachmentsPlural";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        when(httpSession.getAttribute(ATTRIBUTE_KEY)).thenReturn(temporaryAttachmentSession);

        this.attachmentManager.temporarilyAttach(attachment, documentReference);
        verify(temporaryAttachmentSession).addAttachment(documentReference, attachment);

        verifyNoInteractions(attachment);
    }

    @Test
    void attachTemporaryAttachmentsInDocument()
    {
        XWikiDocument document = mock(XWikiDocument.class);
        this.attachmentManager.attachTemporaryAttachmentsInDocument(document, Collections.emptyList());
        verifyNoInteractions(this.context);

        String sessionId = "removeUploadedAttachmentsPlural";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        when(httpSession.getAttribute(ATTRIBUTE_KEY)).thenReturn(temporaryAttachmentSession);

        DocumentReference documentReference = mock(DocumentReference.class);
        when(document.getDocumentReference()).thenReturn(documentReference);

        String file1 = "myfile.txt";
        String file2 = "otherfile.gif";

        XWikiAttachment attachment1 = mock(XWikiAttachment.class, "file1");
        XWikiAttachment attachment2 = mock(XWikiAttachment.class, "file2");

        XWikiAttachment previousAttachment1 = mock(XWikiAttachment.class, "previousFile1");
        when(document.setAttachment(attachment1)).thenReturn(previousAttachment1);

        String version = "4.5";
        when(previousAttachment1.getNextVersion()).thenReturn(version);

        when(temporaryAttachmentSession.getAttachment(documentReference, file1)).thenReturn(Optional.of(attachment1));
        when(temporaryAttachmentSession.getAttachment(documentReference, file2)).thenReturn(Optional.of(attachment2));

        this.attachmentManager.attachTemporaryAttachmentsInDocument(document, List.of(
            "foo",
            file1,
            "bar",
            file2,
            ""
        ));
        verify(document).setAttachment(attachment1);
        verify(document).setAttachment(attachment2);
        verify(attachment1).setVersion(version);
        verifyNoInteractions(attachment2);
    }
}
