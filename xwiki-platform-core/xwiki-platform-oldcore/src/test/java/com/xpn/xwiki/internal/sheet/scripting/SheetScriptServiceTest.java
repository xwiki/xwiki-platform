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
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link SheetScriptService}.
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
        sheetScriptService = (SheetScriptService) mocker.getComponentUnderTest();
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
        Document classDocumentApi = new Document(classDocument, null);

        final DocumentReference sheetReference =
            new DocumentReference("MySheet", classReference.getLastSpaceReference());
        final SheetBinder mockClassSheetBinder = mocker.getInstance(SheetBinder.class, "class");

        when(mockClassSheetBinder.bind(argThat(new ArgumentMatcher<DocumentModelBridge>()
        {
            @Override
            public boolean matches(Object argument)
            {
                return classDocument.equals(argument) && classDocument != argument;
            }
        }), same(sheetReference))).thenReturn(true);

        Assert.assertTrue(sheetScriptService.bindClassSheet(classDocumentApi, sheetReference));
    }
}
