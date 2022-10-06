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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultTemporaryAttachmentSessionsManager}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
class DefaultTemporaryAttachmentSessionsManagerTest
{
    private static final String ATTRIBUTE_KEY = "xwikiTemporaryAttachments";

    @InjectMockComponents
    private DefaultTemporaryAttachmentSessionsManager attachmentManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @XWikiTempDir
    private File tmpDir;

    private XWikiContext context;
    private HttpSession httpSession;

    @BeforeEach
    void setup(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.httpSession = mock(HttpSession.class);
        XWikiRequest xWikiRequest = mock(XWikiRequest.class);
        when(xWikiRequest.getSession()).thenReturn(this.httpSession);
        when(this.context.getRequest()).thenReturn(xWikiRequest);
        Utils.setComponentManager(mockitoComponentManager);

        Environment environment = mockitoComponentManager.registerMockComponent(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(this.tmpDir);
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
        InputStream inputStream = new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8));
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
}
