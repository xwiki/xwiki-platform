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

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /**
     * See XWIKI-21188
     */
    @Test
    @Order(6)
    void testDeleteAllButFirstVersion(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.rest().delete(testReference);

        // Create first version of the page
        setup.rest().savePage(testReference, "1.1", TITLE);

        // Create a few more
        setup.rest().savePage(testReference, "2.1", TITLE);
        setup.rest().savePage(testReference, "3.1", TITLE);
        setup.rest().savePage(testReference, "4.1", TITLE);

        // View the page
        ViewPage vp = setup.gotoPage(testReference);
        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        // Verify and delete the latest version.
        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        assertEquals("4.1", historyTab.getCurrentVersion());
        historyTab = historyTab.deleteRangeVersions("2.1", "4.1");

        // Verify that the current version is now the first one.
        assertEquals("1.1", historyTab.getCurrentVersion());
        Page page = (Page) setup.rest().get(testReference);
        assertEquals("1.1", page.getVersion());
        assertEquals("1.1", page.getContent());
    }

    /**
     * Scenario:
     *   * Create a user RollbackTestUser
     *   * Create a page, allow RollbackTestUser script right on it, and then deny it
     *   * Login with RollbackTestUser and try to rollback the page to the version where the right was allowed
     *   * Check that the page still has the right xobject set to deny
     */
    @Test
    @Order(7)
    void testRollbackDontMessUpRights(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.rest().delete(testReference);
        String rollbackTestUser = "RollbackTestUser";
        setup.rest().delete(new DocumentReference("xwiki", "XWiki", rollbackTestUser));
        setup.createUser(rollbackTestUser, rollbackTestUser, "");
        setup.createPage(testReference, "Test Rollback Page");

        setup.setRights(testReference, "", "XWiki." + rollbackTestUser, "script", true);

        // We don't use setRights twice as it would create another object and we want to edit the existing one.
        setup.gotoPage(testReference, "edit", "editor=object");
        ObjectEditPage objectEditPage = new ObjectEditPage();
        List<ObjectEditPane> rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiRights", true);
        assertEquals(1, rightObjects.size());
        ObjectEditPane objectEditPane = rightObjects.get(0);
        assertEquals("XWiki." + rollbackTestUser, objectEditPane.getFieldValue(objectEditPane.byPropertyName("users")));
        assertEquals("script", objectEditPane.getFieldValue(objectEditPane.byPropertyName("levels")));
        assertEquals("1", objectEditPane.getFieldValue(objectEditPane.byPropertyName("allow")));
        objectEditPane.setFieldValue(objectEditPane.byPropertyName("allow"), "0");
        // We want a minor version
        objectEditPage.clickSaveAndContinue();
        setup.gotoPage(testReference);

        setup.login(rollbackTestUser, rollbackTestUser);

        // check that the right is as expected
        setup.gotoPage(testReference, "edit", "editor=object");
        objectEditPage = new ObjectEditPage();
        rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiRights", true);
        assertEquals(1, rightObjects.size());
        objectEditPane = rightObjects.get(0);
        assertEquals("XWiki." + rollbackTestUser, objectEditPane.getFieldValue(objectEditPane.byPropertyName("users")));
        assertEquals("script", objectEditPane.getFieldValue(objectEditPane.byPropertyName("levels")));
        assertEquals("0", objectEditPane.getFieldValue(objectEditPane.byPropertyName("allow")));

        ViewPage viewPage = objectEditPage.clickCancel();
        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        historyPane = historyPane.showMinorEdits();

        // Check that the history contains what we're expecting
        assertEquals(3, historyPane.getNumberOfVersions());
        assertEquals("2.2", historyPane.getCurrentVersion());
        assertTrue(historyPane.hasVersion("2.1"));

        viewPage = historyPane.rollbackToVersion("2.1");
        historyPane = viewPage.openHistoryDocExtraPane();
        historyPane = historyPane.showMinorEdits();

        // Check that the history contains what we're expecting
        assertEquals(4, historyPane.getNumberOfVersions());

        assertEquals("3.1", historyPane.getCurrentVersion());
        assertEquals("Rollback to version 2.1", historyPane.getCurrentVersionComment());
        assertTrue(historyPane.hasVersion("2.2"));
        assertTrue(historyPane.hasVersion("2.1"));

        // check that the right is still the same
        setup.gotoPage(testReference, "edit", "editor=object");
        objectEditPage = new ObjectEditPage();
        rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiRights", true);
        assertEquals(1, rightObjects.size());
        objectEditPane = rightObjects.get(0);
        assertEquals("XWiki." + rollbackTestUser, objectEditPane.getFieldValue(objectEditPane.byPropertyName("users")));
        assertEquals("script", objectEditPane.getFieldValue(objectEditPane.byPropertyName("levels")));
        assertEquals("0", objectEditPane.getFieldValue(objectEditPane.byPropertyName("allow")));

        objectEditPage.clickCancel();
    }

    /**
     * Scenario:
     *   * Create a user DeleteVersionTestUser
     *   * Give DeleteVersionTestUser Admin right with a dedicated xobject in XWiki.XWikiPreferences
     *   * Give DeleteVersionTestUser PR right with a dedicated xobject in XWiki.XWikiPreferences
     *   * Edit the xobject to deny PR right to DeleteVersionTestUser
     *   * Login with DeleteVersionTestUser and delete last version of XWiki.XWikiPreferences
     *   * Check that the version has been deleted but the PR right is still denied
     */
    @Test
    @Order(8)
    void testDeleteVersionDontMessUpRights(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        String deleteVersionTestUser = "DeleteVersionTestUser";
        setup.rest().delete(new DocumentReference("xwiki", "XWiki", deleteVersionTestUser));
        setup.createUser(deleteVersionTestUser, deleteVersionTestUser, "");

        setup.setGlobalRights("", deleteVersionTestUser, "admin", true);

        DocumentReference xwikiPreferences = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        setup.gotoPage(xwikiPreferences, "view", "viewer=history");
        HistoryPane historyPane = new HistoryPane();
        historyPane = historyPane.showMinorEdits();
        // store the current version as it will be our basis for next steps
        String latestVersionBeforeChanges = historyPane.getCurrentVersion();
        int numberOfVersions = historyPane.getNumberOfVersions();

        // We just create a new major version in the history
        setup.gotoPage(xwikiPreferences, "edit", "editor=wiki");
        WikiEditPage wikiEditPage = new WikiEditPage();
        wikiEditPage.clickSaveAndView();

        setup.gotoPage(xwikiPreferences, "view", "viewer=history");
        historyPane = new HistoryPane();
        // Version where we start our changes
        String startChangesVersion = historyPane.getCurrentVersion();

        String currentMajor = startChangesVersion.split("\\.")[0];

        setup.setGlobalRights("", deleteVersionTestUser, "programming", true);
        currentMajor = String.valueOf(Integer.parseInt(currentMajor) + 1);

        // We don't use setRights twice as it would create another object and we want to edit the existing one.
        setup.gotoPage(xwikiPreferences, "edit", "editor=object");
        ObjectEditPage objectEditPage = new ObjectEditPage();
        List<ObjectEditPane> rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiGlobalRights", false);
        ObjectEditPane rightObject = rightObjects.get(rightObjects.size() - 1);
        rightObject.displayObject();
        assertEquals(deleteVersionTestUser, rightObject.getFieldValue(rightObject.byPropertyName("users")));
        assertEquals("programming", rightObject.getFieldValue(rightObject.byPropertyName("levels")));
        assertEquals("1", rightObject.getFieldValue(rightObject.byPropertyName("allow")));

        rightObject.setFieldValue(rightObject.byPropertyName("allow"), "0");
        // We want a minor version
        objectEditPage.clickSaveAndContinue();

        setup.gotoPage(xwikiPreferences);

        setup.login(deleteVersionTestUser, deleteVersionTestUser);

        // first check that the right is properly denied in the objects
        setup.gotoPage(xwikiPreferences, "edit", "editor=object");
        objectEditPage = new ObjectEditPage();
        rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiGlobalRights", false);
        rightObject = rightObjects.get(rightObjects.size() - 1);
        rightObject.displayObject();
        assertEquals(deleteVersionTestUser, rightObject.getFieldValue(rightObject.byPropertyName("users")));
        assertEquals("programming", rightObject.getFieldValue(rightObject.byPropertyName("levels")));
        assertEquals("0", rightObject.getFieldValue(rightObject.byPropertyName("allow")));

        setup.gotoPage(xwikiPreferences, "view", "viewer=history");
        historyPane = new HistoryPane();
        historyPane = historyPane.showMinorEdits();
        assertEquals(numberOfVersions + 3, historyPane.getNumberOfVersions());
        assertEquals(currentMajor + ".2", historyPane.getCurrentVersion());
        assertTrue(historyPane.hasVersion(currentMajor + ".1"));

        try {
            historyPane = historyPane.deleteVersion(historyPane.getCurrentVersion());
            historyPane = historyPane.showMinorEdits();
            assertEquals(numberOfVersions + 2, historyPane.getNumberOfVersions());
            assertEquals(currentMajor + ".1", historyPane.getCurrentVersion());

            // Check that the page remained with rights unchanged
            setup.gotoPage(xwikiPreferences, "edit", "editor=object");
            objectEditPage = new ObjectEditPage();
            rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiGlobalRights", false);
            rightObject = rightObjects.get(rightObjects.size() - 1);
            rightObject.displayObject();
            assertEquals(deleteVersionTestUser,
                rightObject.getFieldValue(rightObject.byPropertyName("users")));
            assertEquals("programming", rightObject.getFieldValue(rightObject.byPropertyName("levels")));
            assertEquals("0", rightObject.getFieldValue(rightObject.byPropertyName("allow")));
            objectEditPage.clickCancel();
        } finally {
            // Put back the page in the state it was before our changes
            setup.loginAsSuperAdmin();
            setup.gotoPage(xwikiPreferences, "view", "viewer=history");
            historyPane = new HistoryPane();
            historyPane = historyPane.showMinorEdits();
            historyPane.rollbackToVersion(latestVersionBeforeChanges);
        }
    }

    /**
     * Scenario:
     *   * Same as above but using DeleteVersionTestUserCancelEvent as user to trigger the
     *   {@link org.xwiki.test.CustomUserUpdatedDocumentEventListener} which should cancel immediately the change
     *   * Expectation here is that the reset is not performed at all
     */
    @Test
    @Order(9)
    void testDeleteVersionDontMessUpRightsWithCancellingEvent(TestUtils setup)
        throws Exception
    {
        setup.loginAsSuperAdmin();

        String deleteVersionTestUser = "DeleteVersionTestUserCancelEvent";
        setup.rest().delete(new DocumentReference("xwiki", "XWiki", deleteVersionTestUser));
        setup.createUser(deleteVersionTestUser, deleteVersionTestUser, "");

        setup.setGlobalRights("", deleteVersionTestUser, "admin", true);

        DocumentReference xwikiPreferences = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        setup.gotoPage(xwikiPreferences, "view", "viewer=history");
        HistoryPane historyPane = new HistoryPane();
        historyPane = historyPane.showMinorEdits();
        // store the current version as it will be our basis for next steps
        String latestVersionBeforeChanges = historyPane.getCurrentVersion();
        int numberOfVersions = historyPane.getNumberOfVersions();

        // We just create a new major version in the history
        setup.gotoPage(xwikiPreferences, "edit", "editor=wiki");
        WikiEditPage wikiEditPage = new WikiEditPage();
        wikiEditPage.clickSaveAndView();

        setup.gotoPage(xwikiPreferences, "view", "viewer=history");
        historyPane = new HistoryPane();
        // Version where we start our changes
        String startChangesVersion = historyPane.getCurrentVersion();

        String currentMajor = startChangesVersion.split("\\.")[0];

        setup.setGlobalRights("", deleteVersionTestUser, "programming", true);
        currentMajor = String.valueOf(Integer.parseInt(currentMajor) + 1);

        // We don't use setRights twice as it would create another object and we want to edit the existing one.
        setup.gotoPage(xwikiPreferences, "edit", "editor=object");
        ObjectEditPage objectEditPage = new ObjectEditPage();
        List<ObjectEditPane> rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiGlobalRights", false);
        ObjectEditPane rightObject = rightObjects.get(rightObjects.size() - 1);
        rightObject.displayObject();
        assertEquals(deleteVersionTestUser, rightObject.getFieldValue(rightObject.byPropertyName("users")));
        assertEquals("programming", rightObject.getFieldValue(rightObject.byPropertyName("levels")));
        assertEquals("1", rightObject.getFieldValue(rightObject.byPropertyName("allow")));

        rightObject.setFieldValue(rightObject.byPropertyName("allow"), "0");
        // We want a minor version
        objectEditPage.clickSaveAndContinue();

        setup.gotoPage(xwikiPreferences);

        setup.login(deleteVersionTestUser, deleteVersionTestUser);

        // first check that the right is properly denied in the objects
        setup.gotoPage(xwikiPreferences, "edit", "editor=object");
        objectEditPage = new ObjectEditPage();
        rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiGlobalRights", false);
        rightObject = rightObjects.get(rightObjects.size() - 1);
        rightObject.displayObject();
        assertEquals(deleteVersionTestUser, rightObject.getFieldValue(rightObject.byPropertyName("users")));
        assertEquals("programming", rightObject.getFieldValue(rightObject.byPropertyName("levels")));
        assertEquals("0", rightObject.getFieldValue(rightObject.byPropertyName("allow")));

        setup.gotoPage(xwikiPreferences, "view", "viewer=history");
        historyPane = new HistoryPane();
        historyPane = historyPane.showMinorEdits();
        assertEquals(numberOfVersions + 3, historyPane.getNumberOfVersions());
        assertEquals(currentMajor + ".2", historyPane.getCurrentVersion());
        assertTrue(historyPane.hasVersion(currentMajor + ".1"));

        try {
            historyPane.deleteVersion(historyPane.getCurrentVersion());

            setup.gotoPage(xwikiPreferences, "view", "viewer=history");
            historyPane = new HistoryPane();
            historyPane = historyPane.showMinorEdits();

            // here the history shouldn't have changed because of the CustomUserUpdatedDocumentEventListener
            // that should have cancel the event
            assertEquals(numberOfVersions + 3, historyPane.getNumberOfVersions());
            assertEquals(currentMajor + ".2", historyPane.getCurrentVersion());

            // Check that the page remained with rights unchanged
            setup.gotoPage(xwikiPreferences, "edit", "editor=object");
            objectEditPage = new ObjectEditPage();
            rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiGlobalRights", false);
            rightObject = rightObjects.get(rightObjects.size() - 1);
            rightObject.displayObject();
            assertEquals(deleteVersionTestUser, rightObject.getFieldValue(rightObject.byPropertyName("users")));
            assertEquals("programming", rightObject.getFieldValue(rightObject.byPropertyName("levels")));
            assertEquals("0", rightObject.getFieldValue(rightObject.byPropertyName("allow")));
            objectEditPage.clickCancel();

            setup.gotoPage(xwikiPreferences, "view", "viewer=history");
            historyPane = new HistoryPane();
            historyPane = historyPane.showMinorEdits();

            // Check that deleting another version still works
            historyPane = historyPane.deleteVersion(currentMajor + ".1");
            historyPane = historyPane.showMinorEdits();

            assertEquals(numberOfVersions + 2, historyPane.getNumberOfVersions());
            assertEquals(currentMajor + ".2", historyPane.getCurrentVersion());
            assertFalse(historyPane.hasVersion(currentMajor + ".1"));

            // Check that the page remained with rights unchanged
            setup.gotoPage(xwikiPreferences, "edit", "editor=object");
            objectEditPage = new ObjectEditPage();
            rightObjects = objectEditPage.getObjectsOfClass("XWiki.XWikiGlobalRights", false);
            rightObject = rightObjects.get(rightObjects.size() - 1);
            rightObject.displayObject();
            assertEquals(deleteVersionTestUser, rightObject.getFieldValue(rightObject.byPropertyName("users")));
            assertEquals("programming", rightObject.getFieldValue(rightObject.byPropertyName("levels")));
            assertEquals("0", rightObject.getFieldValue(rightObject.byPropertyName("allow")));
            objectEditPage.clickCancel();
        } finally {
            // Put back the page in the state it was before our changes
            setup.loginAsSuperAdmin();
            setup.gotoPage(xwikiPreferences, "view", "viewer=history");
            historyPane = new HistoryPane();
            historyPane = historyPane.showMinorEdits();
            historyPane.rollbackToVersion(latestVersionBeforeChanges);
        }
    }
}
