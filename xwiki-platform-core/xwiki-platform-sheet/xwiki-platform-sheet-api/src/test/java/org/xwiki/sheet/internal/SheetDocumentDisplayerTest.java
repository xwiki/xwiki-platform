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
package org.xwiki.sheet.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.sheet.SheetManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SheetDocumentDisplayer}.
 * 
 * @version $Id$
 */
public class SheetDocumentDisplayerTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentDisplayer> mocker = new MockitoComponentMockingRule<DocumentDisplayer>(
        SheetDocumentDisplayer.class);

    /**
     * The reference to the displayed document.
     */
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki1", "Space", "Page");

    /**
     * The reference to the sheet document.
     */
    private static final DocumentReference SHEET_REFERENCE = new DocumentReference("wiki2", "Code", "Sheet");

    /**
     * The component used to access the documents.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to access the model.
     */
    private ModelBridge modelBridge;

    @Before
    public void configure() throws Exception
    {
        this.modelBridge = this.mocker.getInstance(ModelBridge.class);
        // Assume the current action is view.
        when(this.modelBridge.getCurrentAction()).thenReturn("view");

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        // Assume all documents are viewable by the current user.
        when(this.documentAccessBridge.isDocumentViewable(any(DocumentReference.class))).thenReturn(true);
    }

    /**
     * Creates a mock {@link DocumentModelBridge} that has the specified reference.
     * 
     * @param documentReference the document reference
     * @return the mock {@link DocumentModelBridge}
     * @throws Exception if creating the mock fails
     */
    private DocumentModelBridge mockDocument(DocumentReference documentReference) throws Exception
    {
        StringBuilder id = new StringBuilder(documentReference.getLastSpaceReference().getName());
        // Allow different instances of the same document to exist.
        id.append('.').append(documentReference.getName()).append(RandomStringUtils.secure().nextAlphanumeric(3));
        DocumentModelBridge document = mock(DocumentModelBridge.class, id.toString());

        when(document.getDocumentReference()).thenReturn(documentReference);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference)).thenReturn(document);
        when(this.modelBridge.getDefaultEditMode(document)).thenReturn("edit");
        when(this.modelBridge.getDefaultTranslation(document)).thenReturn(document);

        return document;
    }

    /**
     * Sets the given document as the current document, adding the necessary expectations.
     * 
     * @param document an XWiki document
     */
    private void setCurrentDocument(DocumentModelBridge document)
    {
        DocumentReference documentReference = document.getDocumentReference();
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(documentReference);
        when(this.modelBridge.isCurrentDocument(document)).thenReturn(true);
    }

    /**
     * Tests if the programming rights of the sheet are preserved when the document is already on the context.
     * 
     * @throws Exception if something wrong happens
     */
    @Test
    public void testPreserveSheetPRWhenDocumentIsOnContext() throws Exception
    {
        DocumentModelBridge document = mockDocument(DOCUMENT_REFERENCE);
        DocumentModelBridge sheet = mockDocument(SHEET_REFERENCE);

        setCurrentDocument(document);

        SheetManager sheetManager = this.mocker.getInstance(SheetManager.class);
        when(sheetManager.getSheets(document, "view")).thenReturn(Collections.singletonList(SHEET_REFERENCE));

        DocumentModelBridge originalSecurityDoc = mock(DocumentModelBridge.class, "sdoc");
        // Required in order to preserve the programming rights of the sheet.
        when(this.modelBridge.setSecurityDocument(sheet)).thenReturn(originalSecurityDoc);

        XDOM output = new XDOM(Collections.<Block>emptyList());
        DocumentDisplayer documentDisplayer = this.mocker.getInstance(DocumentDisplayer.class);
        when(documentDisplayer.display(eq(sheet), any(DocumentDisplayerParameters.class))).thenReturn(output);

        assertSame(output, this.mocker.getComponentUnderTest().display(document, new DocumentDisplayerParameters()));

        // The security document must be reverted.
        verify(this.modelBridge).setSecurityDocument(originalSecurityDoc);
    }

    /**
     * Tests if the programming rights of the sheet are preserved when the document is not on the context.
     * 
     * @throws Exception if something wrong happens
     */
    @Test
    public void testPreserveSheetPRWhenDocumentIsNotOnContext() throws Exception
    {
        DocumentModelBridge document = mockDocument(DOCUMENT_REFERENCE);
        DocumentModelBridge sheet = mockDocument(SHEET_REFERENCE);

        // We test that the displayed document is put on the context even if the current document is just a different
        // instance of the displayed document. This is needed because the displayed document can have unsaved changes.
        setCurrentDocument(mockDocument(DOCUMENT_REFERENCE));

        // The sheet must be determined and displayed in a new execution context that has the target document as
        // the current document.
        Map<String, Object> backupObjects = new HashMap<String, Object>();
        when(this.modelBridge.pushDocumentInContext(document)).thenReturn(backupObjects);

        SheetManager sheetManager = this.mocker.getInstance(SheetManager.class);
        when(sheetManager.getSheets(document, "view")).thenReturn(Collections.singletonList(SHEET_REFERENCE));

        DocumentModelBridge originalSecurityDoc = mock(DocumentModelBridge.class, "sdoc");
        // Required in order to preserve the programming rights of the sheet.
        when(this.modelBridge.setSecurityDocument(sheet)).thenReturn(originalSecurityDoc);

        XDOM output = new XDOM(Collections.<Block>emptyList());
        DocumentDisplayer documentDisplayer = this.mocker.getInstance(DocumentDisplayer.class);
        when(documentDisplayer.display(eq(sheet), any(DocumentDisplayerParameters.class))).thenReturn(output);

        assertSame(output, this.mocker.getComponentUnderTest().display(document, new DocumentDisplayerParameters()));

        // The security document must be reverted.
        verify(this.modelBridge).setSecurityDocument(originalSecurityDoc);

        // The previous execution context must be restored.
        verify(this.documentAccessBridge).popDocumentFromContext(backupObjects);
    }
}
