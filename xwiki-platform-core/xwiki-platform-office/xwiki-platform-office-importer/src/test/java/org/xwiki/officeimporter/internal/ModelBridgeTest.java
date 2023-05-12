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
package org.xwiki.officeimporter.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.document.FileOfficeDocumentArtifact;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ModelBridge}.
 * 
 * @version $Id$
 */
@ComponentTest
class ModelBridgeTest
{
    @InjectMockComponents
    private ModelBridge modelBridge;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @XWikiTempDir
    private File tempDir;

    @Test
    void saveWithOverwrite() throws Exception
    {
        XDOMOfficeDocument doc = mock(XDOMOfficeDocument.class);
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        DocumentReference parentReference = new DocumentReference("wiki", "Space", "Parent");
        String syntaxId = "test/1.0";
        String title = "Office Document Title";
        String content = "Office Document Content";
        String fileName = "logo.png";
        byte[] fileContent = new byte[] {65, 82};
        File artifact = new File(tempDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(artifact)) {
            IOUtils.write(fileContent, fos);
        }

        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        when(doc.getContentAsString(syntaxId)).thenReturn(content);
        when(doc.getArtifactsMap())
            .thenReturn(Collections.singletonMap(fileName, new FileOfficeDocumentArtifact(fileName, artifact)));
        // Store all attachment contents that are set on the mock.
        Map<AttachmentReference, byte[]> attachmentContents = new HashMap<>();
        doAnswer((invocation) -> attachmentContents.put(
            invocation.getArgument(0, AttachmentReference.class),
            IOUtils.toByteArray(invocation.getArgument(1, InputStream.class))
        )).when(this.documentAccessBridge)
            .setAttachmentContent(any(AttachmentReference.class), any(InputStream.class));

        this.modelBridge.save(doc, documentReference, syntaxId, parentReference, title, false);

        verify(documentAccessBridge).setDocumentSyntaxId(documentReference, syntaxId);
        verify(documentAccessBridge).setDocumentContent(documentReference, content, "Created by office importer.",
            false);
        verify(documentAccessBridge).setDocumentParentReference(documentReference, parentReference);
        verify(documentAccessBridge).setDocumentTitle(documentReference, title);
        assertEquals(1, attachmentContents.size());
        AttachmentReference expectedAttachmentReference = new AttachmentReference(fileName, documentReference);
        assertEquals(expectedAttachmentReference, attachmentContents.keySet().iterator().next());
        assertArrayEquals(fileContent, attachmentContents.get(expectedAttachmentReference));
    }

    @Test
    void saveWithAppend() throws Exception
    {
        XDOMOfficeDocument doc = mock(XDOMOfficeDocument.class);
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        String syntaxId = "test/1.0";

        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        when(documentAccessBridge.exists(documentReference)).thenReturn(true);

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        when(documentAccessBridge.getTranslatedDocumentInstance(documentReference)).thenReturn(document);
        when(document.getSyntax()).thenReturn(new Syntax(new SyntaxType("test", "Test"), "1.0"));

        when(documentAccessBridge.getDocumentContent(documentReference, null)).thenReturn("before");
        when(doc.getContentAsString(syntaxId)).thenReturn("after");

        this.modelBridge.save(doc, documentReference, syntaxId, null, null, true);

        verify(documentAccessBridge).setDocumentContent(documentReference, "before\nafter",
            "Updated by office importer.", false);
    }
}
