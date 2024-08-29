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
package org.xwiki.flamingo.test.docker;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.DeletedPageEntry;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests related to recycle bin operations.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@UITest
class RecycleBinIT
{
    /**
     * @see "XWIKI-9421: Attachment version is incremented when a document is restored from recycle bin"
     */
    @Test
    @Order(1)
    void restore(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Clean up.
        setup.rest().deletePage(testReference.getLastSpaceReference().getName(), testReference.getName());

        // Create a new page.
        ViewPage page = setup.createPage(testReference, "Once upon a time..", "A story");

        // Add an attachment.
        new AttachmentsViewPage().openAttachmentsDocExtraPane().setFileToUpload(
            new File(testConfiguration.getBrowser().getTestResourcesPath(),
                "RecycleBinIT/SmallAttachment.txt").getAbsolutePath());

        // Delete the page.
        page.delete().clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();

        DeletePageOutcomePage deletePageOutcomePage = deletingPage.getDeletePageOutcomePage();

        // Open the page in preview mode (by clicking on the link of the second column), and check that the content is
        // the one expected.
        ViewPage viewPage = deletePageOutcomePage.clickViewDocument(1);
        assertEquals("A story", viewPage.getDocumentTitle());
        assertEquals("Once upon a time..", viewPage.getContent());

        // Goes back to the previous page to continue the page restoration.
        setup.getDriver().navigate().back();

        // Restore the page.
        page = deletePageOutcomePage.clickRestore();

        // Check the page title and content.
        assertEquals("A story", page.getDocumentTitle());
        assertEquals("Once upon a time..", page.getContent());

        // Check document version/history.
        HistoryPane historyPane = page.openHistoryDocExtraPane();
        assertEquals("2.1", historyPane.getCurrentVersion());

        // Check the attachment.
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment("SmallAttachment.txt"));

        // Check the attachment content.
        attachmentsPane.getAttachmentLink("SmallAttachment.txt").click();
        assertEquals("This is a small attachment.", setup.getDriver().findElement(By.tagName("html")).getText());
    }

    /**
     * Check that viewing a terminal deleted page works and that rights are properly checked for viewing a deleted page.
     */
    @Test
    @Order(2)
    void viewDeletedPage(TestUtils testUtils, TestReference parentReference)
    {
        /*
        Fixture:
          * 3 pages:
            - ParentSpace
              - Page1.WebHome
              - Page1 -> terminal
          * 2 users
            - viewDeletedPageUser1: allow edit, delete, view -> deleter of the pages
            - viewDeletedPageUser2: denied edit, allowed view on the space
            - viewDeletedPageUser3: denied view / edit on the space
         */

        String testUserPrefix = parentReference.getName();
        String user1 = testUserPrefix + "User1";
        String user2 = testUserPrefix + "User2";
        String user3 = testUserPrefix + "User3";

        testUtils.loginAsSuperAdmin();
        testUtils.createUser(user1, user1, null);
        testUtils.createUser(user2, user2, null);
        testUtils.createUser(user3, user3, null);

        testUtils.createPage(parentReference, "Parent space");

        DocumentReference page1 =
            new DocumentReference("WebHome", new SpaceReference("Page1", parentReference.getLastSpaceReference()));
        DocumentReference page1Terminal = new DocumentReference("Page1", parentReference.getLastSpaceReference());
        DocumentReference page2 =
            new DocumentReference("WebHome", new SpaceReference("Page2", parentReference.getLastSpaceReference()));

        testUtils.createPage(page1, "Page 1 non terminal page content", "Page 1 Non Terminal");
        testUtils.createPage(page1Terminal, "Page 1 **terminal** page content", "Page 1 Terminal");
        testUtils.createPage(page2, "Page 2 content", "Page 2");

        testUtils.setRightsOnSpace(parentReference.getLastSpaceReference(), "",
            String.format("XWiki.%s", user1), "delete", true);
        testUtils.setRightsOnSpace(parentReference.getLastSpaceReference(), "",
            String.format("XWiki.%s,XWiki.%s", user2, user3), "edit", false);
        testUtils.setRightsOnSpace(parentReference.getLastSpaceReference(), "",
            String.format("XWiki.%s", user3), "view", false);

        testUtils.login(user3, user3);
        ViewPage viewPage = testUtils.gotoPage(page1);
        assertTrue(viewPage.isForbidden());

        viewPage = testUtils.gotoPage(page1Terminal);
        assertTrue(viewPage.isForbidden());

        viewPage = testUtils.gotoPage(page2);
        assertTrue(viewPage.isForbidden());

        // Delete all 3 pages
        testUtils.login(user1, user1);

        viewPage = testUtils.gotoPage(page1);
        viewPage.deletePage().confirmDeletePage().waitUntilFinished();

        viewPage = testUtils.gotoPage(page1Terminal);
        viewPage.deletePage().confirmDeletePage().waitUntilFinished();

        viewPage = testUtils.gotoPage(page2);
        viewPage.deletePage().confirmDeletePage().waitUntilFinished();

        // Check view deleted page with User2
        testUtils.login(user2, user2);
        testUtils.gotoPage(page1);

        DeletePageOutcomePage recycleBinPage1 = new DeletePageOutcomePage();
        List<DeletedPageEntry> deletedPagesEntries = recycleBinPage1.getDeletedPagesEntries();
        List<DeletedPageEntry> deletedTerminalPagesEntries = recycleBinPage1.getDeletedTerminalPagesEntries();

        // Check that both Page1 and Page1 terminal are shown
        assertEquals(1, deletedPagesEntries.size());
        assertEquals(1, deletedTerminalPagesEntries.size());

        assertEquals(user1, deletedPagesEntries.get(0).getDeleter());
        assertEquals(user1, deletedTerminalPagesEntries.get(0).getDeleter());

        // Check that the user cannot view the page as it's not the deleter
        assertFalse(deletedPagesEntries.get(0).canBeViewed());
        assertFalse(deletedTerminalPagesEntries.get(0).canBeViewed());

        // Check that however other actions are not possible
        assertFalse(deletedPagesEntries.get(0).canBeDeleted());
        assertFalse(deletedTerminalPagesEntries.get(0).canBeDeleted());

        // Check that however other actions are not possible
        assertFalse(deletedPagesEntries.get(0).canBeRestored());
        assertFalse(deletedTerminalPagesEntries.get(0).canBeRestored());

        // Check view deleted page with User1
        testUtils.login(user1, user1);
        testUtils.gotoPage(page1);

        recycleBinPage1 = new DeletePageOutcomePage();
        deletedPagesEntries = recycleBinPage1.getDeletedPagesEntries();
        deletedTerminalPagesEntries = recycleBinPage1.getDeletedTerminalPagesEntries();

        // Check that both Page1 and Page1 terminal are shown
        assertEquals(1, deletedPagesEntries.size());
        assertEquals(1, deletedTerminalPagesEntries.size());

        assertEquals(user1, deletedPagesEntries.get(0).getDeleter());
        assertEquals(user1, deletedTerminalPagesEntries.get(0).getDeleter());

        // Check that both are viewable by the user
        assertTrue(deletedPagesEntries.get(0).canBeViewed());
        assertTrue(deletedTerminalPagesEntries.get(0).canBeViewed());

        // Check that permanently delete is not possible
        assertFalse(deletedPagesEntries.get(0).canBeDeleted());
        assertFalse(deletedTerminalPagesEntries.get(0).canBeDeleted());

        // Check that restore is also possible
        assertTrue(deletedPagesEntries.get(0).canBeRestored());
        assertTrue(deletedTerminalPagesEntries.get(0).canBeRestored());

        // View content of the deleted terminal page
        viewPage = deletedTerminalPagesEntries.get(0).clickView();

        // Keep track of the URL to try accessing it later on
        String terminalPageDeletedViewUrl = testUtils.getDriver().getCurrentUrl();

        assertEquals("Page 1 Terminal", viewPage.getDocumentTitle());
        assertEquals("Page 1 terminal page content", viewPage.getContent());

        // Check view deleted page with User3
        testUtils.login(user3, user3);
        viewPage = testUtils.gotoPage(page1);

        assertTrue(viewPage.isForbidden());

        // Try to access the deleted revision
        testUtils.gotoPage(terminalPageDeletedViewUrl);
        viewPage = new ViewPage();

        assertTrue(viewPage.isForbidden());

        // Also try access the deleted revision with user2
        testUtils.login(user2, user2);
        testUtils.gotoPage(terminalPageDeletedViewUrl);
        viewPage = new ViewPage();

        assertTrue(viewPage.isForbidden());

        // Check view deleted page with superadmin
        testUtils.loginAsSuperAdmin();
        viewPage = testUtils.gotoPage(page1);

        recycleBinPage1 = new DeletePageOutcomePage();
        deletedPagesEntries = recycleBinPage1.getDeletedPagesEntries();
        deletedTerminalPagesEntries = recycleBinPage1.getDeletedTerminalPagesEntries();

        // Check that both Page1 and Page1 terminal are shown
        assertEquals(1, deletedPagesEntries.size());
        assertEquals(1, deletedTerminalPagesEntries.size());

        // Check that all actions are possible
        assertTrue(deletedPagesEntries.get(0).canBeViewed());
        assertTrue(deletedTerminalPagesEntries.get(0).canBeViewed());

        assertTrue(deletedPagesEntries.get(0).canBeDeleted());
        assertTrue(deletedTerminalPagesEntries.get(0).canBeDeleted());

        assertTrue(deletedPagesEntries.get(0).canBeRestored());
        assertTrue(deletedTerminalPagesEntries.get(0).canBeRestored());
    }

    @Test
    @Order(3)
    void deletedDocumentIsRestricted(TestUtils testUtils, TestReference testReference)
    {
        testUtils.loginAsSuperAdmin();

        ViewPage viewPage = testUtils.createPage(testReference, "{{velocity}}Velocity content{{/velocity}}", "Title");
        // Make sure the Velocity macro is executed normally.
        assertEquals("Velocity content", viewPage.getContent());

        viewPage.deletePage().confirmDeletePage().waitUntilFinished();

        testUtils.gotoPage(testReference);
        DeletePageOutcomePage recycleBinPage = new DeletePageOutcomePage();

        List<DeletedPageEntry> deletedPageEntries = recycleBinPage.getDeletedPagesEntries();
        assertFalse(deletedPageEntries.isEmpty());
        viewPage = deletedPageEntries.get(0).clickView();

        assertEquals("Title", viewPage.getDocumentTitle());
        // In the preview the Velocity macro should be forbidden.
        assertThat(viewPage.getContent(), startsWith("Failed to execute the [velocity] macro."));

        testUtils.gotoPage(testReference);
        recycleBinPage = new DeletePageOutcomePage();
        viewPage = recycleBinPage.getDeletedPagesEntries().get(0).clickRestore();
        // Assert that in the restored document, scripts are allowed again.
        assertEquals("Velocity content", viewPage.getContent());
    }
}
