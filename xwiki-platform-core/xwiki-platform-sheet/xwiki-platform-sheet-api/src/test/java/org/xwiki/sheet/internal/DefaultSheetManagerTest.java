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
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSheetManager}.
 *
 * @version $Id$
 * @since 4.2M1
 */
@ComponentTest
class DefaultSheetManagerTest
{
    /**
     * The name of the execution context sheet property.
     */
    private static final String SHEET_PROPERTY = "sheet";

    /**
     * The action property of the sheet descriptor class. See {@link #SHEET_CLASS_REFERENCE}.
     */
    private static final String ACTION_PROPERTY = "action";

    /**
     * The name of a wiki.
     */
    private static final String WIKI_NAME = "wiki";

    /**
     * The sheet descriptor class reference.
     */
    private static final DocumentReference SHEET_CLASS_REFERENCE =
        new DocumentReference(WIKI_NAME, "XWiki", "SheetDescriptorClass");

    /**
     * The reference of the document whose sheets are retrieved.
     */
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference(WIKI_NAME, "Space", "Page");

    @InjectMockComponents
    private DefaultSheetManager sheetManager;

    /**
     * The component used to access the documents.
     */
    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to retrieve the custom document sheets.
     */
    @MockComponent
    @Named("document")
    private SheetBinder documentSheetBinder;

    /**
     * The component used to retrieve the class sheets.
     */
    @MockComponent
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * The component used to access the old model.
     */
    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private Execution execution;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The execution context.
     */
    private ExecutionContext context = new ExecutionContext();

    /**
     * The document whose sheets are retrieved.
     */
    private DocumentModelBridge document;

    @BeforeEach
    void configure()
    {
        this.document = mock(DocumentModelBridge.class);
        when(this.execution.getContext()).thenReturn(this.context);
        when(this.document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.documentReferenceResolver.resolve(eq("XWiki.SheetDescriptorClass"), any(DocumentReference.class)))
            .thenReturn(SHEET_CLASS_REFERENCE);
    }

    /**
     * Tests that the sheet specified on the execution context overwrites the document and class sheets.
     */
    @Test
    void executionContextSheet() throws Exception
    {
        // (1) The sheet is specified on the execution context and the target document is the current document.
        this.context.setProperty(SHEET_PROPERTY, "Code.Sheet");
        DocumentReference sheetReference = new DocumentReference(WIKI_NAME, "Code", "Sheet");
        when(this.documentReferenceResolver.resolve("Code.Sheet", DOCUMENT_REFERENCE)).thenReturn(sheetReference);
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.documentAccessBridge.exists(sheetReference)).thenReturn(true);
        when(this.documentAccessBridge.getProperty(sheetReference, SHEET_CLASS_REFERENCE, ACTION_PROPERTY))
            .thenReturn("");

        assertEquals(List.of(sheetReference), this.sheetManager.getSheets(this.document, "view"));

        // (2) The sheet is specified on the execution context but the target document is not the current document.
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(null);
        when(this.documentSheetBinder.getSheets(this.document)).thenReturn(Collections.emptyList());
        when(this.modelBridge.getXObjectClassReferences(this.document)).thenReturn(Collections.emptySet());

        assertTrue(this.sheetManager.getSheets(this.document, "edit").isEmpty());

        // (3) The sheet is not specified on the execution context.
        this.context.removeProperty(SHEET_PROPERTY);

        assertTrue(this.sheetManager.getSheets(this.document, "get").isEmpty());
    }

    /**
     * Tests the order in which sheets are determined: execution context, document sheets and finally class sheets.
     */
    @Test
    void sheetResolutionSequence() throws Exception
    {
        String contextSheetName = "ContextSheet";
        this.context.setProperty(SHEET_PROPERTY, contextSheetName);
        DocumentReference documentSheetReference = new DocumentReference(WIKI_NAME, "ABC", "DocumentSheet");
        DocumentReference classSheetReference = new DocumentReference(WIKI_NAME, "BlogCode", "BlogPostSheet");
        DocumentReference classReference = new DocumentReference(WIKI_NAME, "Blog", "BlogPostClass");
        DocumentModelBridge classDocument = mock(DocumentModelBridge.class, "xclass");
        String currentAction = "foo";

        DocumentReference contextSheetRef = new DocumentReference(WIKI_NAME, "Space", contextSheetName);
        when(this.documentReferenceResolver.resolve(contextSheetName, DOCUMENT_REFERENCE)).thenReturn(contextSheetRef);
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(DOCUMENT_REFERENCE);

        // (1) Look for the sheet specified in the execution context.
        when(this.documentAccessBridge.exists(contextSheetRef)).thenReturn(false);

        // (2) Look for the custom document sheets.
        when(this.documentSheetBinder.getSheets(this.document))
            .thenReturn(Collections.singletonList(documentSheetReference));
        when(this.documentAccessBridge.exists(documentSheetReference)).thenReturn(true);
        when(this.documentAccessBridge.getProperty(documentSheetReference, SHEET_CLASS_REFERENCE, ACTION_PROPERTY))
            .thenReturn("bar");

        // (3) Look for the class sheets.
        when(this.modelBridge.getXObjectClassReferences(this.document))
            .thenReturn(Collections.singleton(classReference));
        when(this.documentAccessBridge.getTranslatedDocumentInstance(classReference)).thenReturn(classDocument);
        when(this.classSheetBinder.getSheets(classDocument))
            .thenReturn(Collections.singletonList(classSheetReference));
        when(this.documentAccessBridge.exists(classSheetReference)).thenReturn(true);
        when(this.documentAccessBridge.getProperty(classSheetReference, SHEET_CLASS_REFERENCE, ACTION_PROPERTY))
            .thenReturn(currentAction);

        assertEquals(List.of(classSheetReference), this.sheetManager.getSheets(this.document, currentAction));
    }
}
