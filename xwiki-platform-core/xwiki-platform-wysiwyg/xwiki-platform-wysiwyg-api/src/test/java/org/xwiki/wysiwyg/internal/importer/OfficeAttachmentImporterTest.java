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
package org.xwiki.wysiwyg.internal.importer;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.officeimporter.server.OfficeServer.ServerState;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OfficeAttachmentImporter}.
 * 
 * @version $Id$
 * @since 9.8
 */
@ComponentTest
class OfficeAttachmentImporterTest
{
    private static final AttachmentReference ATTACHMENT_REFERENCE =
        new AttachmentReference("my.doc", new DocumentReference("wiki", "Some", "Page"));

    @InjectMockComponents
    private OfficeAttachmentImporter officeAttachmentImporter;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private OfficeServer officeServer;

    @MockComponent
    private OfficeMacroImporter officeMacroImporter;

    @MockComponent
    private XDOMOfficeDocumentBuilder documentBuilder;

    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeEach
    void configure() throws Exception
    {
        when(this.entityReferenceSerializer.serialize(ATTACHMENT_REFERENCE)).thenReturn("Some.Page@my.doc");
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void toHTMLWithOfficeViewer() throws Exception
    {
        XDOM xdom = mock(XDOM.class);
        when(officeMacroImporter.buildXDOM(ATTACHMENT_REFERENCE, false)).thenReturn(xdom);
        when(officeMacroImporter.render(xdom)).thenReturn("foo");

        Map<String, Object> parameters = Collections.singletonMap("useOfficeViewer", "true");
        assertEquals("foo", officeAttachmentImporter.toHTML(ATTACHMENT_REFERENCE, parameters));
    }

    @Test
    void toHTMLRequiresEditRight() throws Exception
    {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            officeAttachmentImporter.toHTML(ATTACHMENT_REFERENCE, Collections.emptyMap());
        });
        assertEquals("Edit right is required in order to import [Some.Page@my.doc].", runtimeException.getMessage());
    }

    @Test
    void toHTMLAttachmentNotFound() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, ATTACHMENT_REFERENCE)).thenReturn(true);
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            officeAttachmentImporter.toHTML(ATTACHMENT_REFERENCE, Collections.emptyMap());
        });
        assertEquals("Attachment not found: [Some.Page@my.doc].", runtimeException.getMessage());
    }

    @Test
    void toHTMLOfficeServerNotConnected() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, ATTACHMENT_REFERENCE)).thenReturn(true);
        when(this.documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn("1.3");

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            officeAttachmentImporter.toHTML(ATTACHMENT_REFERENCE, Collections.emptyMap());
        });
        assertEquals("The office server is not connected.", runtimeException.getMessage());
    }

    @Test
    void toHTML() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, ATTACHMENT_REFERENCE)).thenReturn(true);
        when(this.documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn("1.3");
        when(this.officeServer.getState()).thenReturn(ServerState.CONNECTED);

        InputStream attachmentContent = mock(InputStream.class);
        when(this.documentAccessBridge.getAttachmentContent(ATTACHMENT_REFERENCE)).thenReturn(attachmentContent);

        XDOMOfficeDocument xdomOfficeDocument = mock(XDOMOfficeDocument.class);
        when(documentBuilder.build(attachmentContent, "my.doc", ATTACHMENT_REFERENCE.getDocumentReference(), true))
            .thenReturn(xdomOfficeDocument);
        when(xdomOfficeDocument.getArtifactsMap()).thenReturn(Collections.emptyMap());
        when(xdomOfficeDocument.getContentAsString("annotatedxhtml/1.0")).thenReturn("test");

        Map<String, Object> parameters = Collections.singletonMap("filterStyles", "true");
        assertEquals("test", officeAttachmentImporter.toHTML(ATTACHMENT_REFERENCE, parameters));
    }

    @Test
    void toHTMLTemporaryUploadedAttachment() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, ATTACHMENT_REFERENCE)).thenReturn(true);
        when(this.documentAccessBridge.getAttachmentVersion(ATTACHMENT_REFERENCE)).thenReturn(null);
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(this.temporaryAttachmentSessionsManager.getUploadedAttachment(ATTACHMENT_REFERENCE))
            .thenReturn(Optional.of(attachment));

        when(this.officeServer.getState()).thenReturn(ServerState.CONNECTED);

        InputStream attachmentContent = mock(InputStream.class);
        when(attachment.getContentInputStream(this.context)).thenReturn(attachmentContent);

        XDOMOfficeDocument xdomOfficeDocument = mock(XDOMOfficeDocument.class);
        when(documentBuilder.build(attachmentContent, "my.doc", ATTACHMENT_REFERENCE.getDocumentReference(), true))
            .thenReturn(xdomOfficeDocument);
        when(xdomOfficeDocument.getArtifactsMap()).thenReturn(Collections.emptyMap());
        when(xdomOfficeDocument.getContentAsString("annotatedxhtml/1.0")).thenReturn("test");

        Map<String, Object> parameters = Collections.singletonMap("filterStyles", "true");
        assertEquals("test", officeAttachmentImporter.toHTML(ATTACHMENT_REFERENCE, parameters));
    }
}
