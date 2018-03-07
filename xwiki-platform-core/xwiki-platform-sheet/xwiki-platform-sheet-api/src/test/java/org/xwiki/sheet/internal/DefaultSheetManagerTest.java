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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;

import org.jmock.Expectations;
import org.jmock.Sequence;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.sheet.SheetManager;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.jmock.annotation.MockingRequirement;

/**
 * Unit tests for {@link DefaultSheetManager}.
 * 
 * @version $Id$
 * @since 4.2M1
 */
@AllComponents
@MockingRequirement(value = DefaultSheetManager.class, exceptions = { DocumentReferenceResolver.class })
public class DefaultSheetManagerTest extends AbstractMockingComponentTestCase<SheetManager>
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
    private static final DocumentReference SHEET_CLASS_REFERENCE = new DocumentReference(WIKI_NAME, "XWiki",
        "SheetDescriptorClass");

    /**
     * The execution context.
     */
    private ExecutionContext context = new ExecutionContext();

    /**
     * The component used to access the documents.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to retrieve the custom document sheets.
     */
    private SheetBinder documentSheetBinder;

    /**
     * The component used to retrieve the class sheets.
     */
    private SheetBinder classSheetBinder;

    /**
     * The component used to access the old model.
     */
    private ModelBridge modelBridge;

    /**
     * The document whose sheets are retrieved.
     */
    private DocumentModelBridge document;

    @Before
    public void configure() throws Exception
    {
        documentAccessBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
        documentSheetBinder = getComponentManager().getInstance(SheetBinder.class, "document");
        classSheetBinder = getComponentManager().getInstance(SheetBinder.class, "class");
        modelBridge = getComponentManager().getInstance(ModelBridge.class);
        document = getMockery().mock(DocumentModelBridge.class);
        final Execution execution = getComponentManager().getInstance(Execution.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(execution).getContext();
                will(returnValue(context));

                allowing(document).getDocumentReference();
                will(returnValue(new DocumentReference(WIKI_NAME, "Space", "Page")));
            }
        });
    }

    /**
     * Tests that the sheet specified on the execution context overwrites the document and class sheets.
     * 
     * @throws Exception if the test fails to lookup components
     */
    @Test
    public void testExecutionContextSheet() throws Exception
    {
        // (1) The sheet is specified on the execution context and the target document is the current document.
        context.setProperty(SHEET_PROPERTY, "Code.Sheet");
        final DocumentReference sheetReference = new DocumentReference(WIKI_NAME, "Code", "Sheet");

        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getCurrentDocumentReference();
                will(returnValue(document.getDocumentReference()));

                oneOf(documentAccessBridge).exists(sheetReference);
                will(returnValue(true));

                // The specified sheet matches the current action.
                oneOf(documentAccessBridge).getProperty(sheetReference, SHEET_CLASS_REFERENCE, ACTION_PROPERTY);
                will(returnValue(""));
            }
        });

        Assert.assertEquals(Arrays.asList(sheetReference), getMockedComponent().getSheets(document, "view"));

        // (2) The sheet is specified on the execution context but the target document is not the current document.
        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentAccessBridge).getCurrentDocumentReference();
                will(returnValue(null));

                oneOf(documentSheetBinder).getSheets(document);
                will(returnValue(Collections.emptyList()));

                oneOf(modelBridge).getXObjectClassReferences(document);
                will(returnValue(Collections.emptySet()));
            }
        });
        Assert.assertTrue(getMockedComponent().getSheets(document, "edit").isEmpty());

        // (3) The sheet is not specified on the execution context.
        context.removeProperty(SHEET_PROPERTY);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(documentSheetBinder).getSheets(document);
                will(returnValue(Collections.emptyList()));

                oneOf(modelBridge).getXObjectClassReferences(document);
                will(returnValue(Collections.emptySet()));
            }
        });

        Assert.assertTrue(getMockedComponent().getSheets(document, "get").isEmpty());
    }

    /**
     * Tests the order in which sheets are determined: execution context, document sheets and finally class sheets.
     * 
     * @throws Exception shouldn't happen, but some methods include "throws" in their signature
     */
    @Test
    public void testSheetResolutionSequence() throws Exception
    {
        final String contextSheetName = "ContextSheet";
        context.setProperty(SHEET_PROPERTY, contextSheetName);
        final DocumentReference documentSheetReference = new DocumentReference(WIKI_NAME, "ABC", "DocumentSheet");
        final DocumentReference classSheetReference = new DocumentReference(WIKI_NAME, "BlogCode", "BlogPostSheet");
        final DocumentReference classReference = new DocumentReference(WIKI_NAME, "Blog", "BlogPostClass");
        final DocumentModelBridge classDocument = getMockery().mock(DocumentModelBridge.class, "xclass");
        final String currentAction = "foo";
        final Sequence sheetResolutionSequence = getMockery().sequence("sheetResolutionSequence");

        getMockery().checking(new Expectations()
        {
            {
                // (1) Look for the sheet specified in the execution context.
                oneOf(documentAccessBridge).getCurrentDocumentReference();
                inSequence(sheetResolutionSequence);
                will(returnValue(document.getDocumentReference()));

                // The sheet is resolved relative to the target document.
                oneOf(documentAccessBridge).exists(
                    new DocumentReference(document.getDocumentReference().getWikiReference().getName(), document
                        .getDocumentReference().getLastSpaceReference().getName(), contextSheetName));
                inSequence(sheetResolutionSequence);
                will(returnValue(false));

                // (2) Look for the custom document sheets.
                oneOf(documentSheetBinder).getSheets(document);
                inSequence(sheetResolutionSequence);
                will(returnValue(Collections.singletonList(documentSheetReference)));

                oneOf(documentAccessBridge).exists(documentSheetReference);
                inSequence(sheetResolutionSequence);
                will(returnValue(true));

                oneOf(documentAccessBridge).getProperty(documentSheetReference, SHEET_CLASS_REFERENCE, ACTION_PROPERTY);
                inSequence(sheetResolutionSequence);
                will(returnValue("bar"));

                // (3) Look for the class sheets.
                oneOf(modelBridge).getXObjectClassReferences(document);
                inSequence(sheetResolutionSequence);
                will(returnValue(Collections.singleton(classReference)));

                oneOf(documentAccessBridge).getTranslatedDocumentInstance(classReference);
                inSequence(sheetResolutionSequence);
                will(returnValue(classDocument));

                oneOf(classSheetBinder).getSheets(classDocument);
                inSequence(sheetResolutionSequence);
                will(returnValue(Collections.singletonList(classSheetReference)));

                oneOf(documentAccessBridge).exists(classSheetReference);
                inSequence(sheetResolutionSequence);
                will(returnValue(true));

                oneOf(documentAccessBridge).getProperty(classSheetReference, SHEET_CLASS_REFERENCE, ACTION_PROPERTY);
                inSequence(sheetResolutionSequence);
                will(returnValue(currentAction));
            }
        });

        getMockedComponent().getSheets(document, currentAction);
    }
}
