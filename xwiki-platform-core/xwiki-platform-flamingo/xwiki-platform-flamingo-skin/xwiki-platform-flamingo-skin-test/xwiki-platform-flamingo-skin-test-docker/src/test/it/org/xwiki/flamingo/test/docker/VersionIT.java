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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify versioning features of documents and attachments.
 * 
 * @version $Id$
 */
@UITest(properties = {
    // Add the FileUploadPlugin which is needed by the test to upload attachment files
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"})
class VersionIT
{
    private static final String TITLE = "Page Title";

    private static final String CONTENT1 = "First version of Content";

    private static final String CONTENT2 = "Second version of Content";

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void testRollbackToFirstVersion(TestUtils utils, TestReference testReference) throws Exception
    {
        utils.rest().delete(testReference);

        // Create first version of the page
        ViewPage vp = utils.createPage(testReference, CONTENT1, TITLE);

        // Adds second version
        WikiEditPage wikiEditPage = vp.editWiki();
        wikiEditPage.setContent(CONTENT2);
        wikiEditPage.clickSaveAndView();

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        // Verify that we can rollback to the first version
        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        vp = historyTab.rollbackToVersion("1.1");

        // Rollback doesn't wait...
        // Wait for the comment tab to be selected since we're currently on the history tab and rolling
        // back is going to load a new page and make the focus active on the comments tab.
        vp.waitForDocExtraPaneActive("comments");

        assertEquals("First version of Content", vp.getContent());

        historyTab = vp.openHistoryDocExtraPane();
        assertEquals("Rollback to version 1.1", historyTab.getCurrentVersionComment());
        assertEquals("superadmin", historyTab.getCurrentAuthor());
    }

    /**
     * See XWIKI-8781 & XWIKI-20589
     */
    @Test
    @Order(2)
    void testDeleteLatestVersion(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.rest().delete(testReference);

        // Create first version of the page, as superadmin
        setup.createPage(testReference, CONTENT1, TITLE);

        // Log as another user having admin permissions (to be able to delete a revision)
        setup.createAdminUser();

        // Adds second version, as Admin
        ViewPage vp = setup.gotoPage(testReference);
        vp.edit();
        WikiEditPage wikiEditPage = new WikiEditPage();
        wikiEditPage.setContent(CONTENT2);
        wikiEditPage.clickSaveAndView();

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        // Verify and delete the latest version.
        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        assertEquals("2.1", historyTab.getCurrentVersion());
        historyTab = historyTab.deleteVersion("2.1");

        // Verify that the current version is now the previous one.
        assertEquals("1.1", historyTab.getCurrentVersion());
        assertEquals("superadmin", historyTab.getCurrentAuthor());

        // Verify that the last modified author of the page is the author from revision 1.1
        assertTrue(vp.getLastModifiedText().startsWith("Last modified by superadmin"));
    }

    @Test
    @Order(3)
    void rollbackAttachments(TestUtils utils, TestReference testReference) throws Exception
    {
        utils.rest().delete(testReference);

        // Create empty page
        utils.rest().savePage(testReference);

        // Add attachment
        AttachmentReference attachmentReference = new AttachmentReference("file.txt", testReference);
        utils.rest().attachFile(attachmentReference, "attachment1".getBytes(), true);

        // Add a second version of the attachment
        utils.rest().attachFile(attachmentReference, "attachment2".getBytes(), false);

        // Load page
        ViewPage vp = utils.gotoPage(testReference);

        // Make sure expected attachment is there
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));

        // Revert to 1.1 (empty page)
        vp = vp.openHistoryDocExtraPane().rollbackToVersion("1.1");

        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(0, attachmentsPane.getNumberOfAttachments());

        // Revert to 3.1 (second update of the attachment)
        vp = vp.openHistoryDocExtraPane().rollbackToVersion("3.1");

        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));
        attachmentsPane.getAttachmentLink(attachmentReference.getName()).click();
        assertEquals("attachment2", utils.getDriver().findElement(By.tagName("html")).getText());

        // Revert to 2.1 (first update of the attachment)
        vp = utils.gotoPage(testReference).openHistoryDocExtraPane().rollbackToVersion("2.1");

        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.3", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));
        attachmentsPane.getAttachmentLink(attachmentReference.getName()).click();
        assertEquals("attachment1", utils.getDriver().findElement(By.tagName("html")).getText());

        // Back to empty page again
        vp = utils.gotoPage(testReference).openHistoryDocExtraPane().rollbackToVersion("1.1");

        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(0, attachmentsPane.getNumberOfAttachments());

        // Revert to 2.1 (first update of the attachment)
        vp = vp.openHistoryDocExtraPane().rollbackToVersion("2.1");

        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.3", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));
        attachmentsPane.getAttachmentLink(attachmentReference.getName()).click();
        assertEquals("attachment1", utils.getDriver().findElement(By.tagName("html")).getText());
    }

    /**
     * Ensure that a previous deleted attachment with same name and version than an existing one is properly restored
     * after a rollback of a page version.
     */
    @Test
    @Order(4)
    void rollbackAttachmentWithSameNameAndVersion(TestUtils utils, TestReference testReference) throws Exception
    {
        utils.rest().delete(testReference);

        // Create empty page (1.1)
        utils.createPage(testReference, "");

        // Add attachment (2.1)
        AttachmentReference attachmentReference = new AttachmentReference("file.txt", testReference);
        utils.rest().attachFile(attachmentReference, "1".getBytes(), true);

        // Delete attachment (3.1)
        utils.rest().deleteAttachement(attachmentReference);

        // Make sure those two attachments are not saved at during the same second since the granularity is the second
        // in some databases
        Thread.sleep(1000);

        // Add a new attachment with the same name (4.1)
        utils.rest().attachFile(attachmentReference, "2".getBytes(), true);

        ViewPage viewPage = utils.gotoPage(testReference);
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("file.txt"));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment("file.txt"));

        // Revert to first attachment (2.1)
        viewPage = utils.gotoPage(testReference);
        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();

        viewPage = historyPane.rollbackToVersion("2.1");
        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("file.txt"));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment("file.txt"));
        attachmentsPane.getAttachmentLink("file.txt").click();
        assertEquals("1", utils.getDriver().findElement(By.tagName("html")).getText());
    }

    @Test
    @Order(5)
    void oldRevisionsAreRestricted(TestUtils utils, TestReference testReference) throws Exception
    {
        utils.loginAsSuperAdmin();

        utils.rest().delete(testReference);

        // Create first version of the page
        ViewPage vp = utils.createPage(testReference, "{{velocity}}" + CONTENT1 + "{{/velocity}}", TITLE);
        assertEquals(CONTENT1, vp.getContent());

        // Adds second version
        WikiEditPage wikiEditPage = vp.editWiki();
        wikiEditPage.setContent("{{velocity}}" + CONTENT2 + "{{velocity}}");
        vp = wikiEditPage.clickSaveAndView();

        assertEquals(CONTENT2, vp.getContent());

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        vp = historyTab.viewVersion("1.1");

        // In the preview the Velocity macro should be forbidden.
        assertThat(vp.getContent(), startsWith("Failed to execute the [velocity] macro."));

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        historyTab = vp.openHistoryDocExtraPane();
        vp = historyTab.rollbackToVersion("1.1");

        // Rollback doesn't wait...
        // Wait for the comment tab to be selected since we're currently on the history tab and rolling
        // back is going to load a new page and make the focus active on the comments tab.
        vp.waitForDocExtraPaneActive("comments");

        // Assert that scripts are executed again after restoring the version.
        assertEquals(CONTENT1, vp.getContent());
    }

}
