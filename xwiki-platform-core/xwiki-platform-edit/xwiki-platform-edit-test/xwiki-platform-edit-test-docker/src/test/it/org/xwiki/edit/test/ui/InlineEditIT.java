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
package org.xwiki.edit.test.ui;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.PreviewEditPage;
import org.xwiki.test.ui.po.editor.PreviewableEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the inline form editor.
 *
 * @version $Id$
 */
@UITest
class InlineEditIT
{
    private static final String EDIT = "edit";

    private static final String PARENT_CHILD = "parentchild";

    private static final String REFERENCE = "reference";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Verify that the title and the parent can be set from the request when editing inline, and that editing inline
     * preserves them afterwards.
     */
    @Test
    @Order(1)
    void titleAndParent(TestUtils setup, TestReference testReference)
    {
        SpaceReference space = testReference.getLastSpaceReference();
        DocumentReference parentReference = new DocumentReference("Parent", space);
        setup.createPage(parentReference, "", "Inline Parent");

        setup.gotoPage(testReference, EDIT, "editor", "inline", "title", "Inline Title", "parent",
            setup.serializeLocalReference(parentReference));
        InlinePage inlinePage = new InlinePage();
        assertEquals("Inline Title", inlinePage.getDocumentTitle());
        assertThat(inlinePage.getBreadcrumbContent(), containsString("Inline Title"));
        ViewPage viewPage = inlinePage.clickSaveAndView();
        assertEquals("Inline Title", viewPage.getDocumentTitle());

        try {
            // The parent is displayed in the breadcrumb only in the parent/child hierarchy mode.
            setup.setHierarchyMode(PARENT_CHILD);
            viewPage = setup.gotoPage(testReference);
            assertTrue(viewPage.hasBreadcrumbContent("Inline Parent", false));

            // Editing inline preserves the title and the parent.
            viewPage = viewPage.editInline().clickSaveAndView();
            assertEquals("Inline Title", viewPage.getDocumentTitle());
            assertTrue(viewPage.hasBreadcrumbContent("Inline Parent", false));

            // Editing inline in the reference hierarchy mode, which doesn't display the parent, preserves it too.
            setup.setHierarchyMode(REFERENCE);
            setup.gotoPage(testReference, EDIT, "editor=inline");
            new InlinePage().clickSaveAndView();
            setup.setHierarchyMode(PARENT_CHILD);
            assertTrue(setup.gotoPage(testReference).hasBreadcrumbContent("Inline Parent", false));
        } finally {
            setup.setHierarchyMode(REFERENCE);
        }
    }

    /**
     * Verify the inline edition of a page whose class has a sheet.
     */
    @Test
    @Order(2)
    void editWithSheet(TestUtils setup, TestReference testReference)
    {
        SpaceReference space = testReference.getLastSpaceReference();
        DocumentReference classReference = new DocumentReference("ColorClass", space);
        DocumentReference sheetReference = new DocumentReference("ColorSheet", space);
        DocumentReference templateReference = new DocumentReference("ColorTemplate", space);
        String className = setup.serializeLocalReference(classReference);
        setup.addClassProperty(classReference, "color", "String");
        setup.createPage(sheetReference, "{{velocity}}$doc.display('color'){{/velocity}}");
        setup.addObject(classReference, "XWiki.ClassSheetBinding", "sheet",
            setup.serializeLocalReference(sheetReference));
        setup.addObject(templateReference, className, "color", "red");

        // Creating a page from the template opens the inline editor, where the sheet displays the class properties.
        setup.gotoPage(testReference, EDIT, Map.of("template", setup.serializeLocalReference(templateReference)));
        new InlinePage().setValue("color", "green");

        // The sheet is applied when previewing a page that isn't saved yet.
        PreviewEditPage previewPage = new PreviewableEditPage().clickPreview();
        assertEquals("green", previewPage.getContent());
        ViewPage viewPage = previewPage.clickBackToEdit().clickSaveAndView();
        assertEquals("green", viewPage.getContent());

        // The Edit button opens the inline editor for a page that has a sheet.
        viewPage.edit();
        assertTrue(new ViewPage().isInlinePage());

        // The sheet is also applied when previewing a saved page.
        assertEquals("green", new PreviewableEditPage().clickPreview().getContent());
    }
}
