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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wysiwyg.importer.AttachmentImporter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OfficeAttachmentImporter}.
 * 
 * @version $Id$
 * @since 9.8
 */
public class OfficeAttachmentImporterTest
{
    @Rule
    public MockitoComponentMockingRule<AttachmentImporter> mocker =
        new MockitoComponentMockingRule<>(OfficeAttachmentImporter.class);

    private AttachmentReference attachmentReference =
        new AttachmentReference("my.doc", new DocumentReference("wiki", "Some", "Page"));

    private ContextualAuthorizationManager authorization;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private DocumentAccessBridge documentAccessBridge;

    private OfficeServer officeServer;

    @Before
    public void configure() throws Exception
    {
        this.authorization = this.mocker.getInstance(ContextualAuthorizationManager.class);
        this.entityReferenceSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.officeServer = this.mocker.getInstance(OfficeServer.class);

        when(this.entityReferenceSerializer.serialize(attachmentReference)).thenReturn("Some.Page@my.doc");
    }

    @Test
    public void toHTMLWithOfficeViewer() throws Exception
    {
        OfficeMacroImporter officeMacroImporter = this.mocker.getInstance(OfficeMacroImporter.class);
        XDOM xdom = mock(XDOM.class);
        when(officeMacroImporter.buildXDOM(attachmentReference, false)).thenReturn(xdom);
        when(officeMacroImporter.render(xdom)).thenReturn("foo");

        Map<String, Object> parameters = Collections.singletonMap("useOfficeViewer", "true");
        assertEquals("foo", this.mocker.getComponentUnderTest().toHTML(attachmentReference, parameters));
    }

    @Test
    public void toHTMLRequiresEditRight() throws Exception
    {
        try {
            this.mocker.getComponentUnderTest().toHTML(attachmentReference, Collections.emptyMap());
            fail();
        } catch (RuntimeException expected) {
            assertEquals("Edit right is required in order to import [Some.Page@my.doc].", expected.getMessage());
        }
    }

    @Test
    public void toHTMLAttachmentNotFound() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, attachmentReference)).thenReturn(true);

        try {
            this.mocker.getComponentUnderTest().toHTML(attachmentReference, Collections.emptyMap());
            fail();
        } catch (RuntimeException expected) {
            assertEquals("Attachment not found: [Some.Page@my.doc].", expected.getMessage());
        }
    }

    @Test
    public void toHTMLOfficeServerNotConnected() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, attachmentReference)).thenReturn(true);
        when(this.documentAccessBridge.getAttachmentVersion(attachmentReference)).thenReturn("1.3");

        try {
            this.mocker.getComponentUnderTest().toHTML(attachmentReference, Collections.emptyMap());
            fail();
        } catch (RuntimeException expected) {
            assertEquals("The office server is not connected.", expected.getMessage());
        }
    }

    @Test
    public void toHTML() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, attachmentReference)).thenReturn(true);
        when(this.documentAccessBridge.getAttachmentVersion(attachmentReference)).thenReturn("1.3");
        when(this.officeServer.getState()).thenReturn(ServerState.CONNECTED);

        InputStream attachmentContent = mock(InputStream.class);
        when(this.documentAccessBridge.getAttachmentContent(attachmentReference)).thenReturn(attachmentContent);

        XDOMOfficeDocumentBuilder documentBuilder = this.mocker.getInstance(XDOMOfficeDocumentBuilder.class);
        XDOMOfficeDocument xdomOfficeDocument = mock(XDOMOfficeDocument.class);
        when(documentBuilder.build(attachmentContent, "my.doc", attachmentReference.getDocumentReference(), true))
            .thenReturn(xdomOfficeDocument);
        when(xdomOfficeDocument.getArtifactsFiles()).thenReturn(Collections.emptySet());
        when(xdomOfficeDocument.getContentAsString("annotatedxhtml/1.0")).thenReturn("test");

        Map<String, Object> parameters = Collections.singletonMap("filterStyles", "true");
        assertEquals("test", this.mocker.getComponentUnderTest().toHTML(attachmentReference, parameters));
    }
}
