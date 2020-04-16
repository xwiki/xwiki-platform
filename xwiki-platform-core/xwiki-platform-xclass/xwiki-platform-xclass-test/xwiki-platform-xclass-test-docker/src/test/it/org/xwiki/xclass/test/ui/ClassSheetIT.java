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

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.xclass.test.po.ClassSheetPage;
import org.xwiki.xclass.test.po.DataTypesPage;

/**
 * Tests the default class sheet (XWiki.ClassSheet).
 *
 * @version $Id$
 * @since 12.3RC1
 */
@UITest
public class ClassSheetIT
{
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Tests the process of creating a class, its template, its sheet and an instance.
     */
    @Test
    @Order(1)
    public void createClass(TestUtils setup, TestReference reference)
    {
        //TODO: rewrite the test to not rely on the breadcrumb based on parent/child mechanism.
        setup.setHierarchyMode("parentchild");
        try {
            String spaceName = reference.getLastSpaceReference().getName();
            String className = reference.getClass().getSimpleName();
            String classDocName = className + "Class";
            String classTitle = className + " Class";
            String pageName = "createClass";
            // Make sure the document doesn't exist.
            setup.deletePage(spaceName, pageName);

            // Create the class document.
            DataTypesPage dataTypesPage = DataTypesPage.gotoPage().waitUntilPageIsLoaded();
            String dataTypesPageTitle = dataTypesPage.getDocumentTitle();
            Assert.assertTrue(dataTypesPage.isClassListed("XWiki", "XWikiRights"));
            Assert.assertFalse(dataTypesPage.isClassListed(spaceName, classDocName));
            ClassSheetPage classSheetPage = dataTypesPage.createClass(spaceName, className).waitUntilPageIsLoaded();
            Assert.assertEquals(classTitle, classSheetPage.getDocumentTitle());
            Assert.assertTrue(classSheetPage.hasBreadcrumbContent(dataTypesPageTitle, false));

            // Add a property.
            ClassEditPage classEditor = classSheetPage.clickDefineClassLink();
            classEditor.addProperty("color", "String").setPrettyName("Your favorite color");
            classEditor.clickSaveAndView();

            // Add a new property.
            classEditor = classSheetPage.waitUntilPageIsLoaded().clickEditClassLink();
            classEditor.addProperty("age", "Number").setPrettyName("Your current age");
            classEditor.clickSaveAndView();

            // We have to wait for the page to load because Selenium doesn't do it all the time when we click on Save & View
            // (even if the Save & View button triggers a plain form submit; there must be something with the JavaScript
            // code that is executed on submit that interferes with Selenium).
            classSheetPage.waitUntilPageIsLoaded();

            // Assert that the properties are listed.
            Assert.assertTrue(classSheetPage.hasProperty("color", "Your favorite color", "String"));
            Assert.assertTrue(classSheetPage.hasProperty("age", "Your current age", "Number"));

            // Create and bind a sheet.
            classSheetPage = classSheetPage.clickCreateSheetButton().waitUntilPageIsLoaded()
                .clickBindSheetLink().waitUntilPageIsLoaded();
            ViewPage sheetPage = classSheetPage.clickSheetLink();
            Assert.assertEquals(className + " Sheet", sheetPage.getDocumentTitle());
            sheetPage.clickBreadcrumbLink(classTitle);
            classSheetPage.waitUntilPageIsLoaded();

            // Create the template.
            classSheetPage = classSheetPage.clickCreateTemplateButton().waitUntilPageIsLoaded()
                .clickAddObjectToTemplateLink().waitUntilPageIsLoaded();
            ViewPage templatePage = classSheetPage.clickTemplateLink();
            Assert.assertEquals(className + " Template", templatePage.getDocumentTitle());
            // The default edit button should take us to the In-line edit mode.
            templatePage.edit();
            InlinePage editPage = new InlinePage();
            editPage.setValue("color", "red");
            editPage.setValue("age", "13");
            editPage.clickSaveAndContinue();
            editPage.clickBreadcrumbLink(classTitle);
            classSheetPage.waitUntilPageIsLoaded();

            // Create a document based on the class template.
            Assert.assertEquals(spaceName, classSheetPage.getNewPagePicker().getParentInput().getAttribute("value"));
            editPage = classSheetPage.createNewDocument(spaceName, pageName);

            Assert.assertEquals(pageName, editPage.getDocumentTitle());
            Assert.assertEquals("red", editPage.getValue("color"));
            Assert.assertEquals("13", editPage.getValue("age"));

            editPage.setValue("color", "blue");
            editPage.setValue("age", "27");
            ViewPage viewPage = editPage.clickSaveAndView();

            Assert.assertEquals(pageName, viewPage.getDocumentTitle());
            Assert.assertEquals("YOUR FAVORITE COLOR\nblue\nYOUR CURRENT AGE\n27", viewPage.getContent());
            viewPage.clickBreadcrumbLink(classTitle);
            classSheetPage.waitUntilPageIsLoaded();

            // Assert the created document is listed.
            Assert.assertTrue(classSheetPage.hasDocument(pageName));
        } finally {
            setup.setHierarchyMode("reference");
        }
    }

    /**
     * Integration test fop <a href="https://jira.xwiki.org/browse/XWIKI-6936">XWIKI-6936</a>. The test is successful if
     * the page save without error when a class with a field named "action" is defined in the class.
     */
    @Test
    @Order(2)
    public void createClassWithFieldNamedAction(TestUtils setup, TestReference reference)
    {
        setup.gotoPage(reference);
        ClassSheetPage csp = new ClassSheetPage();
        ClassEditPage ec = csp.editClass();
        ec.addProperty("action", "String");

        // the test succeed if this save action does not fail.
        ec.clickSaveAndContinue(true);
    }
}
