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
package org.xwiki.flamingo.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.Assert.assertEquals;

/**
 * Tests basic page and space creation.
 *
 * @version $Id$
 * @since 7.2M2
 */
public class CreatePageAndSpaceTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void createSpaceAndPage()
    {
        // Test 1:  Test Space creation when on an existing page (i.e. the space creation UI will open to ask for the
        //          space name.

        // Clean up before the test starts
        // Note that we introduce a special character to verify we support non-ASCII characters in page and space names
        String existingPageName = getTestMethodName() + "\u0219";
        String spaceName = getTestClassName() + "\u0219";
        getUtil().deleteSpace(spaceName);
        getUtil().deletePage(spaceName, existingPageName);

        // Create the page that's supposed to exist.
        getUtil().createPage(spaceName, existingPageName, "Dummy", "Dummy Title");

        // Since the Flamingo skin no longer supports creating a space from the UI, trigger the Space creation UI
        // by using directly the direct action URL for it.
        getUtil().gotoPage(getUtil().getURL("create", new String[] {spaceName, existingPageName}, "tocreate=space"));
        CreatePagePage cpp = new CreatePagePage();
        EditPage editSpaceWebhomePage = cpp.createPage("", spaceName);

        assertEquals(spaceName, editSpaceWebhomePage.getMetaDataValue("space"));
        assertEquals("WebHome", editSpaceWebhomePage.getMetaDataValue("page"));
        // The parent is the current document from where the space was created.
        assertEquals(spaceName + "." + existingPageName, editSpaceWebhomePage.getParent());
        // and the title the name of the space
        assertEquals(spaceName, editSpaceWebhomePage.getDocumentTitle());

        // Test 2:  Test Space creation when on an non-existing page (i.e. the create action will lead to editing the
        //          current document - No space name will be asked).

        // Since the Flamingo skin no longer supports creating a space from the UI, trigger the Space creation UI
        // by using directly the direct action URL for it. This time on a non-exsiting page.
        getUtil().gotoPage(getUtil().getURL("create", new String[]{ spaceName, "NonExistingPage" },
                "tocreate=space&type=blank"));
        EditPage editPage = new EditPage();

        assertEquals(spaceName, editPage.getMetaDataValue("space"));
        assertEquals("NonExistingPage", editPage.getMetaDataValue("page"));
        // The default parent is the home page of the current wiki (XWIKI-7572).
        assertEquals("Main.WebHome", editPage.getParent());
        // and the title the name of the space
        assertEquals("NonExistingPage", editPage.getDocumentTitle());

        // Test 3:  Test Terminal Page creation when on an existing page (i.e. the space creation UI will open to ask
        //          for the space + page names.

        // Note that we introduce a special character to verify we support non-ASCII characters in page names
        String newPageName = getTestMethodName() + "2" + "\u0219";
        getUtil().deletePage(spaceName, newPageName);

        // Navigate to an existing page before clicking on the Add button
        ViewPage vp = getUtil().gotoPage(spaceName, existingPageName);
        cpp = vp.createPage();
        editPage = cpp.createPage(spaceName, newPageName, true);

        // Verify the title field
        assertEquals(newPageName, editPage.getDocumentTitle());
        // Verify the document name in the metadata
        assertEquals(newPageName, editPage.getMetaDataValue("page"));
        // Save the page to verify it can be saved with a non-ascii name
        ViewPage savedPage = editPage.clickSaveAndView();
        assertEquals(newPageName, savedPage.getMetaDataValue("page"));
    }

    /**
     * Test that the inputs in the create UI are updated, depending on the case.
     */
    @Test
    public void testCreateUIInteraction()
    {
        // Cleanup of the test space for any leftovers from previous tests.
        getUtil().deleteSpace(getTestClassName());

        // Create an existent page that will also be the parent of our documents.
        String existingPageTitle = "Test Area";
        getUtil().createPage(getTestClassName(), "WebHome", "", existingPageTitle);

        CreatePagePage createPage = new ViewPage().createPage();
        // Check that by default we have an empty title and name and the parent is the current document's space.
        assertEquals("", createPage.getDocumentPicker().getTitle());
        assertEquals("", createPage.getDocumentPicker().getName());
        assertEquals(getTestClassName(), createPage.getDocumentPicker().getParent());
        // Check the initial state of the breadcrumb.
        createPage.waitForLocationPreviewContent("/" + existingPageTitle + "/");

        // Set a new title and check that the page name and the breadcrumb are also updated.
        String newTitle = "New Title";
        createPage.getDocumentPicker().setTitle(newTitle);
        assertEquals(newTitle, createPage.getDocumentPicker().getName());
        createPage.waitForLocationPreviewContent("/" + existingPageTitle + "/" + newTitle);

        // Set a new page name and check that the breadcrumb is not updated, since we have a title specified.
        String newName = "SomeNewName";
        createPage.getDocumentPicker().setName(newName);
        createPage.waitForLocationPreviewContent("/" + existingPageTitle + "/" + newTitle);

        // Clear the title, set a page name and check that the breadcrumb now uses the page name as a fallback.
        createPage.getDocumentPicker().setTitle("");
        createPage.waitForLocationPreviewContent("/" + existingPageTitle + "/");
        assertEquals("", createPage.getDocumentPicker().getName());
        createPage.getDocumentPicker().setName(newName);
        createPage.waitForLocationPreviewContent("/" + existingPageTitle + "/" + newName);

        // Set a new parent space and check that the breadcrumb is updated.
        // Before that, reset the title, just for completeness.
        createPage.getDocumentPicker().setTitle(newTitle);
        String newSpace = "SomeNewSpace";
        createPage.getDocumentPicker().setParent(newSpace);
        createPage.waitForLocationPreviewContent("/" + newSpace + "/" + newTitle);

        // Set a new parent in nested spaces and check that the breadcrumb is updated.
        String newSpaceLevel2 = "Level2";
        createPage.getDocumentPicker().setParent(newSpace + "." + newSpaceLevel2);
        createPage.waitForLocationPreviewContent("/" + newSpace + "/" + newSpaceLevel2 + "/" + newTitle);

        // Clear the parent and check that the breadcrumb is updated, since we are creating a top level document.
        createPage.getDocumentPicker().setParent("");
        createPage.waitForLocationPreviewContent("/" + newTitle);
    }

    // TODO: Add a test for the input validation.
}
