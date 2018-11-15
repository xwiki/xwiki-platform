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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.*;

/**
 * Tests the Delete Page feature.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class DeletePageTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule adminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private ViewPage viewPage;

    private static final String LOGGED_USERNAME = "superadmin";

    private static final String CONFIRMATION = "The page has been deleted.";
    
    private static final String DOCUMENT_NOT_FOUND = "The requested page could not be found.";

    private static final String DELETE_ACTION = "delete";

    private static final String SPACE_VALUE = "Test";

    private static final String PAGE_VALUE = "DeletePageTest";

    private static final String PAGE_CONTENT = "This page is used for testing delete functionality";

    private static final String PAGE_TITLE = "Page title that will be deleted";

    private static final String DELETE_SUCCESSFUL = "Done.";

    @Before
    public void setUp() throws Exception
    {
        // Create a new Page that will be deleted
        this.viewPage = getUtil().createPage(SPACE_VALUE, PAGE_VALUE, PAGE_CONTENT, PAGE_TITLE);
    }

    @Test
    public void deleteOkWhenConfirming()
    {
        ConfirmationPage confirmationPage = this.viewPage.delete();
        // This tests for regression of XWIKI-1388
        assertNotNull("The interface should not show the user as logged out while deleting page",
            confirmationPage.getCurrentUser());
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        // Note: it's better to wait instead of using isSuccess() since there could be some timeframe between
        // the hiding of the progress UI and the display of the success message.
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        DeletePageOutcomePage deleteOutcome = deletingPage.getDeletePageOutcomePage();
        assertEquals(LOGGED_USERNAME, deleteOutcome.getPageDeleter());
        assertEquals(DOCUMENT_NOT_FOUND, deleteOutcome.getMessage());
    }

    /**
     * Verify that we can delete a page without showing the confirmation dialog box and that we can redirect to any page
     * we want when the delete is done.
     */
    @Test
    public void deletePageCanSkipConfirmationAndDoARedirect()
    {
        String pageURL = getUtil().getURL(SPACE_VALUE, PAGE_VALUE + "Whatever");
        getUtil().gotoPage(SPACE_VALUE, PAGE_VALUE, DELETE_ACTION, "confirm=1&xredirect=" + pageURL);
        ViewPage vp = new ViewPage();
        // Since the page PAGE_VALUE + "Whatever" doesn't exist the View Action will redirect to the Nested Document
        // SPACE_VALUE + "." + PAGE_VALUE + "Whatever + ".WebHome".
        assertEquals(SPACE_VALUE + "." + PAGE_VALUE + "Whatever", vp.getMetaDataValue("space"));
        assertEquals("WebHome", vp.getMetaDataValue("page"));
    }

    /**
     * Verify that we can skip the default delete result page and instead redirect to any page we want.
     */
    @Test
    public void deletePageCanDoRedirect()
    {
        // Set the current page to be any page (doesn't matter if it exists or not)
        String pageURL = getUtil().getURL(SPACE_VALUE, PAGE_VALUE + "Whatever");
        getUtil().gotoPage(SPACE_VALUE, PAGE_VALUE, DELETE_ACTION, "xredirect=" + pageURL);
        ConfirmationPage confirmation = new ConfirmationPage();
        confirmation.clickYes();
        ViewPage vp = new ViewPage();
        // Since the page PAGE_VALUE + "Whatever" doesn't exist the View Action will redirect to the Nested Document
        // SPACE_VALUE + "." + PAGE_VALUE + "Whatever + ".WebHome".
        assertEquals(SPACE_VALUE + "." + PAGE_VALUE + "Whatever", vp.getMetaDataValue("space"));
        assertEquals("WebHome", vp.getMetaDataValue("page"));
    }

    /**
     * Verify that hitting cancel on the delete confirmation dialog box goes back to the page being deleted.
     */
    @Test
    public void deletePageGoesToOriginalPageWhenCancelled()
    {
        this.viewPage.delete().clickNo();
        assertEquals(getUtil().getURL(SPACE_VALUE, PAGE_VALUE), getDriver().getCurrentUrl());
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    public void deletePageIsImpossibleWhenNoDeleteRights()
    {
        // Logs out to be guest and not have the right to delete
        this.viewPage.logout();
        assertFalse(this.viewPage.canDelete());
    }

    /**
     * Verify that when you delete a terminal and a non terminal page sharing the same location, both deleted versions
     * are present in the recycle bin list when you hit the location afterwards.
     * @see: "XWIKI-12563: Cannot restore a terminal page from its location"
     * @since 7.2RC1
     */
    @Test
    public void deleteTerminalAndNonTerminalPages()
    {
        DocumentReference terminalPageRef = new DocumentReference("xwiki",
                Arrays.asList(getTestClassName()),
                getTestMethodName());

        DocumentReference nonTerminalPageRef = new DocumentReference("xwiki",
                Arrays.asList(getTestClassName(), getTestMethodName()),
                "WebHome");

        // Clean up.
        getUtil().deletePage(terminalPageRef);
        getUtil().deletePage(nonTerminalPageRef);

        // Create the non terminal page.
        ViewPage nonTerminalPage = getUtil().createPage(nonTerminalPageRef, "Content", "Title");
        // Delete it
        nonTerminalPage.delete().clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        
        // Look at the recycle bin
        DeletePageOutcomePage deletePageOutcomePage = deletingPage.getDeletePageOutcomePage();
        assertFalse(deletePageOutcomePage.hasTerminalPagesInRecycleBin());
        
        // Create the terminal page.
        ViewPage terminalPage = getUtil().createPage(terminalPageRef, "Content", "Title");
        // Delete it
        terminalPage.delete().clickYes();
        deletingPage.waitUntilFinished();
        
        // Look at the recycle bin
        deletePageOutcomePage = deletingPage.getDeletePageOutcomePage();
        assertTrue(deletePageOutcomePage.hasTerminalPagesInRecycleBin());
        
        // Delete both version in the recycle bin
        deletePageOutcomePage.clickDeletePage();
        deletePageOutcomePage.clickDeleteTerminalPage();
    }

    /**
     * Test that when you delete a page and you select "affect children", it delete children properly. It also test
     * the opposite.
     * 
     * @since 7.2RC1
     */
    @Test
    public void deleteChildren()
    {
        // Initialize the parent
        DocumentReference parentReference = new DocumentReference("xwiki",
                Arrays.asList(getTestClassName(), getTestMethodName()),
                "WebHome");
        ViewPage parentPage = getUtil().createPage(parentReference, "Content", "Parent");

        // Test 1: Try to delete it to make sure we don't have the "affect children" option yet
        ConfirmationPage confirmationPage = parentPage.delete();
        assertFalse(confirmationPage.hasAffectChildrenOption());
        
        // Initialize the children pages
        final int NB_CHILDREN = 3;
        DocumentReference[] childrenReferences = new DocumentReference[NB_CHILDREN];
        for (int i = 0; i < NB_CHILDREN; ++i) {
            childrenReferences[i] = new DocumentReference("xwiki", 
                    Arrays.asList(getTestClassName(), getTestMethodName(), "Child_" + (i + 1)),
                    "WebHome");
            getUtil().createPage(childrenReferences[i], "Content", "Child " + (i + 1));
        }
        
        // Test 2: when you don't select "affect children", the children are not deleted
        parentPage = getUtil().gotoPage(parentReference);
        confirmationPage = parentPage.delete();
        assertTrue(confirmationPage.hasAffectChildrenOption());
        confirmationPage.setAffectChildren(false);
        DeletingPage deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        // Check the page have been effectively removed
        ViewPage page = getUtil().gotoPage(parentReference);
        assertFalse(page.exists());
        // But not the children 
        for (int i = 0; i < NB_CHILDREN; ++i) {
            page = getUtil().gotoPage(childrenReferences[i]);
            assertTrue(page.exists());
        }
        
        // Test 3: when you select "affect children", the children are deleted too
        parentPage = getUtil().createPage(parentReference, "Content", "Parent");
        confirmationPage = parentPage.delete();
        assertTrue(confirmationPage.hasAffectChildrenOption());
        confirmationPage.setAffectChildren(true);
        deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();
        // Check the page have been effectively removed
        page = getUtil().gotoPage(parentReference);
        assertFalse(page.exists());
        // And also the children
        for (int i = 0; i < NB_CHILDREN; ++i) {
            page = getUtil().gotoPage(childrenReferences[i]);
            assertFalse(page.exists());
        }
        
    }
}
