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

import java.lang.String;import org.junit.Assert;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.xclass.test.po.ClassSheetPage;
import org.xwiki.xclass.test.po.DataTypesPage;

/**
 * Tests the default class sheet (XWiki.ClassSheet).
 * 
 * @version $Id$
 * @since 4.5
 */
public class ClassSheetTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    /**
     * Tests the process of creating a class, its template, its sheet and an instance.
     */
    @Test
    public void createClass()
    {
        String spaceName = getTestClassName();
        String className = RandomStringUtils.randomAlphabetic(5);
        String classDocName = className + "Class";
        String classTitle = className + " Class";
        String pageName = getTestMethodName();
        // Make sure the document doesn't exist.
        getUtil().deletePage(spaceName, pageName);

        // Create the class document.
        DataTypesPage dataTypesPage = DataTypesPage.gotoPage();
        String dataTypesPageTitle = dataTypesPage.getDocumentTitle();
        Assert.assertTrue(dataTypesPage.isClassListed("XWiki", "XWikiRights"));
        Assert.assertFalse(dataTypesPage.isClassListed(spaceName, classDocName));
        ClassSheetPage classSheetPage = dataTypesPage.createClass(spaceName, className);
        Assert.assertEquals(classTitle, classSheetPage.getDocumentTitle());
        Assert.assertTrue(classSheetPage.hasBreadcrumbContent(dataTypesPageTitle, false));

        // Add a property.
        ClassEditPage classEditor = classSheetPage.clickDefineClassLink();
        classEditor.addProperty("color", "String").setPrettyName("Your favorite color");
        classEditor.clickSaveAndView();

        // Add a new property.
        classEditor = classSheetPage.clickEditClassLink();
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
        classSheetPage = classSheetPage.clickCreateSheetButton().clickBindSheetLink();
        ViewPage sheetPage = classSheetPage.clickSheetLink();
        Assert.assertEquals(className + " Sheet", sheetPage.getDocumentTitle());
        sheetPage.clickBreadcrumbLink(classTitle);

        // Create the template.
        classSheetPage = classSheetPage.clickCreateTemplateButton().clickAddObjectToTemplateLink();
        ViewPage templatePage = classSheetPage.clickTemplateLink();
        Assert.assertEquals(className + " Template", templatePage.getDocumentTitle());
        // The default edit button should take us to the In-line edit mode.
        templatePage.edit();
        InlinePage editPage = new InlinePage();
        editPage.setValue("color", "red");
        editPage.setValue("age", "13");
        editPage.clickSaveAndContinue();
        editPage.clickBreadcrumbLink(classTitle);

        // Create a document based on the class template.
        Assert.assertEquals(spaceName, classSheetPage.getSpaceNameInput().getAttribute("value"));
        editPage = classSheetPage.createNewDocument(spaceName, pageName);

        Assert.assertEquals(pageName, editPage.getDocumentTitle());
        Assert.assertEquals("red", editPage.getValue("color"));
        Assert.assertEquals("13", editPage.getValue("age"));

        editPage.setValue("color", "blue");
        editPage.setValue("age", "27");
        ViewPage viewPage = editPage.clickSaveAndView();

        Assert.assertEquals(pageName, viewPage.getDocumentTitle());
        Assert.assertEquals("Your favorite color\nblue\nYour current age\n27", viewPage.getContent());
        viewPage.clickBreadcrumbLink(classTitle);

        // Assert the created document is listed.
        Assert.assertTrue(classSheetPage.hasDocument(pageName));
    }
}
