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
import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.sheet.SheetManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link SheetDocumentDisplayer}.
 * 
 * @version $Id$
 */
public class SheetDocumentDisplayerTest extends AbstractMockingComponentTestCase
{
    /**
     * The reference to the displayed document.
     */
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki1", "Space", "Page");

    /**
     * The reference to the sheet document.
     */
    private static final DocumentReference SHEET_REFERENCE = new DocumentReference("wiki2", "Code", "Sheet");

    /**
     * A user reference.
     */
    private static final DocumentReference ALICE = new DocumentReference("wiki3", "Users1", "Alice");

    /**
     * A user reference.
     */
    private static final DocumentReference BOB = new DocumentReference("wiki4", "Users2", "Bob");

    /**
     * The component being tested.
     */
    @MockingRequirement
    private SheetDocumentDisplayer sheetDocumentDisplayer;

    /**
     * The component used to access the documents.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to access the model.
     */
    private ModelBridge modelBridge;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        modelBridge = getComponentManager().getInstance(ModelBridge.class);
        documentAccessBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                // Assume the current action is view.
                allowing(modelBridge).getCurrentAction();
                will(returnValue("view"));

                // Assume all documents are viewable by the current user.
                allowing(documentAccessBridge).isDocumentViewable(with(any(DocumentReference.class)));
                will(returnValue(true));
            }
        });
    }

    /**
     * Creates a mock {@link DocumentModelBridge} that has the specified reference, author and programming rights.
     * 
     * @param documentReference the document reference
     * @param authorReference the author reference
     * @param hasProgrammingRights {@code true} if the document has programming rights, {@code false} otherwise
     * @return the mock {@link DocumentModelBridge}
     * @throws Exception if creating the mock fails
     */
    private DocumentModelBridge mockDocument(final DocumentReference documentReference,
        final DocumentReference authorReference, final boolean hasProgrammingRights) throws Exception
    {
        StringBuilder id = new StringBuilder(documentReference.getLastSpaceReference().getName());
        // Allow different instances of the same document to exist.
        id.append('.').append(documentReference.getName()).append(RandomStringUtils.randomAlphanumeric(3));
        final DocumentModelBridge document = getMockery().mock(DocumentModelBridge.class, id.toString());
        getMockery().checking(new Expectations()
        {
            {
                allowing(document).getDocumentReference();
                will(returnValue(documentReference));

                allowing(documentAccessBridge).getDocument(documentReference);
                will(returnValue(document));

                allowing(modelBridge).getDefaultEditMode(document);
                will(returnValue("edit"));

                allowing(modelBridge).getDefaultTranslation(document);
                will(returnValue(document));

                allowing(modelBridge).hasProgrammingRights(document);
                will(returnValue(hasProgrammingRights));
                allowing(modelBridge).getContentAuthorReference(document);
                will(returnValue(authorReference));
            }
        });

        return document;
    }

    /**
     * Sets the given document as the current document, adding the necessary expectations.
     * 
     * @param document an XWiki document
     */
    private void setCurrentDocument(final DocumentModelBridge document)
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(documentAccessBridge).getCurrentDocumentReference();
                will(returnValue(document.getDocumentReference()));

                allowing(modelBridge).getCurrentDocument();
                will(returnValue(document));
            }
        });
    }

    /**
     * Tests if the programming rights of the sheet are preserved when the document is already on the context.
     * 
     * @throws Exception if something wrong happens
     */
    @Test
    public void testPreserveSheetPRWhenDocumentIsOnContext() throws Exception
    {
        final DocumentModelBridge document = mockDocument(DOCUMENT_REFERENCE, ALICE, false);
        final DocumentModelBridge sheet = mockDocument(SHEET_REFERENCE, BOB, true);

        setCurrentDocument(document);

        final SheetManager sheetManager = getComponentManager().getInstance(SheetManager.class);
        final DocumentDisplayer documentDisplayer = getComponentManager().getInstance(DocumentDisplayer.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(sheetManager).getSheets(with(document), with(any(String.class)));
                will(returnValue(Collections.singletonList(SHEET_REFERENCE)));

                // Required in order to preserve the programming rights of the sheet.
                oneOf(modelBridge).setContentAuthorReference(document, BOB);

                oneOf(documentDisplayer).display(with(sheet), with(any(DocumentDisplayerParameters.class)));

                // Document author must be reverted.
                oneOf(modelBridge).setContentAuthorReference(document, ALICE);
            }
        });

        sheetDocumentDisplayer.display(document, new DocumentDisplayerParameters());
    }

    /**
     * Tests if the programming rights of the sheet are preserved when the document is not on the context.
     * 
     * @throws Exception if something wrong happens
     */
    @Test
    public void testPreserveSheetPRWhenDocumentIsNotOnContext() throws Exception
    {
        final DocumentModelBridge document = mockDocument(DOCUMENT_REFERENCE, ALICE, true);
        final DocumentModelBridge sheet = mockDocument(SHEET_REFERENCE, BOB, false);

        // We test that the displayed document is put on the context even if the current document is just a different
        // instance of the displayed document. This is needed because the displayed document can have unsaved changes.
        setCurrentDocument(mockDocument(DOCUMENT_REFERENCE, null, false));

        final SheetManager sheetManager = getComponentManager().getInstance(SheetManager.class);
        final DocumentDisplayer documentDisplayer = getComponentManager().getInstance(DocumentDisplayer.class);
        final Map<String, Object> backupObjects = new HashMap<String, Object>();
        getMockery().checking(new Expectations()
        {
            {
                oneOf(sheetManager).getSheets(with(document), with(any(String.class)));
                will(returnValue(Collections.singletonList(SHEET_REFERENCE)));

                // Required in order to preserve the programming rights of the sheet.
                oneOf(modelBridge).setContentAuthorReference(document, BOB);
                // The sheet must be displayed in the context of the target document.
                oneOf(modelBridge).pushDocumentInContext(document);
                will(returnValue(backupObjects));

                oneOf(documentDisplayer).display(with(sheet), with(any(DocumentDisplayerParameters.class)));

                // The previous context document must be restored.
                oneOf(documentAccessBridge).popDocumentFromContext(backupObjects);
                // Document content author must be restored.
                oneOf(modelBridge).setContentAuthorReference(document, ALICE);
            }
        });

        sheetDocumentDisplayer.display(document, new DocumentDisplayerParameters());
    }
}
