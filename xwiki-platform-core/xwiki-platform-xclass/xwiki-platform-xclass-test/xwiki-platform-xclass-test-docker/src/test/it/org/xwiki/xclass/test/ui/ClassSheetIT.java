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
package org.xwiki.xclass.test.ui;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.EditablePropertyPane;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.xclass.test.po.ClassSheetPage;
import org.xwiki.xclass.test.po.DataTypesPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the default class sheet (XWiki.ClassSheet).
 *
 * @version $Id$
 * @since 12.4RC1
 */
@UITest
class ClassSheetIT
{
    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.setHierarchyMode("parentchild");
    }

    @AfterEach
    void tearDown(TestUtils setup)
    {
        setup.setHierarchyMode("reference");
    }

    /**
     * Tests the process of creating a class, its template, its sheet and an instance.
     */
    @Test
    @Order(1)
    void createClass(TestUtils setup, TestReference reference) throws Exception
    {
        //TODO: rewrite the test to not rely on the breadcrumb based on parent/child mechanism.
        String spaceName = reference.getLastSpaceReference().getName();
        String className = reference.getClass().getSimpleName();
        String classDocName = className + "Class";
        String classTitle = className + " Class";
        String pageName = "createClass";
        // Make sure the document doesn't exist.
        setup.deletePage(spaceName, pageName);

        // Ensure that class listing also works as guest user.
        setup.forceGuestUser();
        DataTypesPage dataTypesPage = DataTypesPage.gotoPage();
        String dataTypesPageTitle = dataTypesPage.getDocumentTitle();
        assertTrue(dataTypesPage.isClassListed("XWiki", "XWikiRights"));
        assertFalse(dataTypesPage.isClassListed(spaceName, classDocName));
        setup.loginAsSuperAdmin();
        dataTypesPage = DataTypesPage.gotoPage();
        // Create the class document.
        ClassSheetPage classSheetPage = dataTypesPage.createClass(spaceName, className);
        assertEquals(classTitle, classSheetPage.getDocumentTitle());
        assertTrue(classSheetPage.hasBreadcrumbContent(dataTypesPageTitle, false));

        // Add a property.
        ClassEditPage classEditor = classSheetPage.clickDefineClassLink();
        classEditor.addProperty("color", "String").setPrettyName("Your favorite color");
        // Test that adding a property with an existing name returns a notification error.
        classEditor.addPropertyWithoutWaiting("color", "String");
        classEditor.waitForNotificationErrorMessage("Property color already exists");
        classEditor.clickSaveAndView();

        // Add a new property.
        classEditor = classSheetPage.clickEditClassLink();
        classEditor.addProperty("age", "Number").setPrettyName("Your current age");

        // Add a computed property.
        String titleDisplayer =
            IOUtils.toString(this.getClass().getResourceAsStream("/contentDisplayer.wiki"), "UTF-8");
        classEditor.addProperty("description", "ComputedField").setPrettyName("Description")
            .setMetaProperty("customDisplay", titleDisplayer);
        classEditor.clickSaveAndView();

        // Assert that the properties are listed.
        assertTrue(classSheetPage.hasProperty("color", "Your favorite color", "String"));
        assertTrue(classSheetPage.hasProperty("age", "Your current age", "Number"));
        assertTrue(classSheetPage.hasProperty("description", "Description", "Computed Field"));

        // Create and bind a sheet.
        classSheetPage = classSheetPage.clickCreateSheetButton().clickBindSheetLink();
        ViewPage sheetPage = classSheetPage.clickSheetLink();
        assertEquals(className + " Sheet", sheetPage.getDocumentTitle());
        sheetPage.clickBreadcrumbLink(classTitle);

        // Create the template.
        classSheetPage = classSheetPage.clickCreateTemplateButton().clickAddObjectToTemplateLink();
        ViewPage templatePage = classSheetPage.clickTemplateLink();
        assertEquals(className + " Template", templatePage.getDocumentTitle());
        // The default edit button should take us to the In-line edit mode.
        templatePage.edit();
        InlinePage editPage = new InlinePage();
        editPage.setValue("color", "red");
        editPage.setValue("age", "13");
        editPage.clickSaveAndContinue();
        editPage.clickBreadcrumbLink(classTitle);

        // Create a document based on the class template.
        assertEquals(spaceName, classSheetPage.getNewPagePicker().getParentInput().getAttribute("value"));
        editPage = classSheetPage.createNewDocument(spaceName, pageName);

        assertEquals(pageName, editPage.getDocumentTitle());
        assertEquals("red", editPage.getValue("color"));
        assertEquals("13", editPage.getValue("age"));

        editPage.setValue("color", "blue");
        editPage.setValue("age", "27");
        ViewPage viewPage = editPage.clickSaveAndView();

        // Verify that the properties can be edited in-place.
        EditablePropertyPane<String> colorProperty =
            new EditablePropertyPane<>(String.format("%s.%s[0].color", spaceName, classDocName));
        assertEquals("blue", colorProperty.clickEdit().getValue());
        colorProperty.setValue("pink").clickSave();
        assertEquals("pink", colorProperty.getDisplayValue());

        EditablePropertyPane<String> descriptionProperty =
            new EditablePropertyPane<>(String.format("%s.%s[0].description", spaceName, classDocName));
        assertEquals("", descriptionProperty.clickEdit().getValue());
        descriptionProperty.setValue("Tester").clickSave();
        assertEquals("Tester", descriptionProperty.getDisplayValue());

        assertEquals(pageName, viewPage.getDocumentTitle());
        assertEquals("YOUR FAVORITE COLOR\npink\nYOUR CURRENT AGE\n27\nDESCRIPTION\nTester", viewPage.getContent());
        viewPage.clickBreadcrumbLink(classTitle);

        // Assert the created document is listed.
        assertTrue(classSheetPage.hasDocument(pageName));
    }
}
