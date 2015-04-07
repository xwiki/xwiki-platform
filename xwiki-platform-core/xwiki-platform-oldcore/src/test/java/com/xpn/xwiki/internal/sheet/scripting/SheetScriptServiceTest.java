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
package com.xpn.xwiki.internal.sheet.scripting;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.sheet.SheetManager;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.script.sheet.SheetScriptService;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link com.xpn.xwiki.script.sheet.SheetScriptService}.
 * 
 * @version $Id$
 */
// We need to register all components because we use XWikiDocument.
@AllComponents
public class SheetScriptServiceTest
{
    /**
     * Mocks the dependencies of the component under test.
     */
    @Rule
    public final MockitoComponentMockingRule<ScriptService> mocker = new MockitoComponentMockingRule<ScriptService>(
        SheetScriptService.class);

    /**
     * The script service being tested.
     */
    private SheetScriptService sheetScriptService;

    private SheetBinder mockClassSheetBinder;

    private SheetBinder mockDocumentSheetBinder;

    private DocumentAccessBridge mockDocumentAccessBridge;

    /**
     * Test setup.
     * 
     * @throws Exception if test setup fails
     */
    @Before
    public void setUp() throws Exception
    {
        // Required in order to create a new instance of XWikiDocument.
        Utils.setComponentManager(mocker);
        this.sheetScriptService = (SheetScriptService) mocker.getComponentUnderTest();
    }

    @AfterComponent
    public void afterComponent() throws Exception
    {
        // Because of @AllComponents all component are injected (for XWikiDocument) while in this case we would like
        // SheetScriptService to be isolated

        this.mockClassSheetBinder = this.mocker.registerMockComponent(SheetBinder.class, "class");
        this.mockDocumentSheetBinder = this.mocker.registerMockComponent(SheetBinder.class, "document");
        this.mockDocumentAccessBridge = this.mocker.registerMockComponent(DocumentAccessBridge.class);

        this.mocker.registerMockComponent(SheetManager.class);
        this.mocker.registerMockComponent(ConfigurationSource.class, "all");
        this.mocker.registerMockComponent(ConfigurationSource.class, "xwikiproperties");
    }

    /**
     * Tests that {@link SheetScriptService#bindClassSheet(Document, DocumentReference)} clones the document first to
     * follow the practice from {@link Document}. This is required in order to not modify the cached document.
     * 
     * @throws Exception if the test fails to lookup the class sheet binder
     */
    @Test
    public void bindClassSheetClonesDocument() throws Exception
    {
        DocumentReference classReference = new DocumentReference("wiki", "Space", "MyClass");
        final XWikiDocument classDocument = new XWikiDocument(classReference);
        classDocument.setSyntax(Syntax.PLAIN_1_0);
        Document classDocumentApi = new Document(classDocument, null);

        final DocumentReference sheetReference =
            new DocumentReference("MySheet", classReference.getLastSpaceReference());

        when(this.mockClassSheetBinder.bind(argThat(new ArgumentMatcher<DocumentModelBridge>()
        {
            @Override
            public boolean matches(Object argument)
            {
                return classDocument.equals(argument) && classDocument != argument;
            }
        }), same(sheetReference))).thenReturn(true);

        Assert.assertTrue(this.sheetScriptService.bindClassSheet(classDocumentApi, sheetReference));
    }

    /**
     * Unit test for {@link SheetScriptService#getDocuments(DocumentReference)}.
     */
    @Test
    public void getDocuments() throws Exception
    {
        DocumentReference sheetReference = new DocumentReference("wiki", "Space", "Sheet");
        DocumentReference publicDocumentReference = new DocumentReference("wiki", "Space", "PublicPage");
        DocumentReference privateDocumentReference = new DocumentReference("wiki", "Space", "PrivatePage");
        DocumentReference publicClassReference = new DocumentReference("wiki", "Space", "PublicClass");
        DocumentReference privateClassReference = new DocumentReference("wiki", "Space", "PrivateClass");

        when(this.mockDocumentSheetBinder.getDocuments(sheetReference)).thenReturn(
            Arrays.asList(publicDocumentReference, privateDocumentReference));

        when(this.mockClassSheetBinder.getDocuments(sheetReference)).thenReturn(
            Arrays.asList(privateClassReference, publicClassReference));

        when(this.mockDocumentAccessBridge.isDocumentViewable(publicClassReference)).thenReturn(true);
        when(this.mockDocumentAccessBridge.isDocumentViewable(publicDocumentReference)).thenReturn(true);

        Assert.assertEquals(Arrays.asList(publicDocumentReference, publicClassReference),
            this.sheetScriptService.getDocuments(sheetReference));
    }
}
