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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ChangesPane;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.SourceViewer;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.diff.RawChanges;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin",
    // The script needs PR right.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern="
        + ".*:Test\\.Execute\\..+"
})
class VersionIT
{
    private static final String TITLE = "Page Title";

    private static final String CONTENT1 = "First version of Content";

    private static final String CONTENT2 = "Second version of Content";

    @BeforeEach
    void beforeEach(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
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
        vp.waitForDocExtraPaneActive("Comments");

        // Verify that we can rollback to the first version
        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        vp = historyTab.rollbackToVersion("1.1");

        // Rollback doesn't wait...
        // Wait for the comment tab to be selected since we're currently on the history tab and rolling
        // back is going to load a new page and make the focus active on the comments tab.
        vp.waitForDocExtraPaneActive("Comments");

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
        vp.waitForDocExtraPaneActive("Comments");

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
        vp.openHistoryDocExtraPane().rollbackToVersion("3.1");

        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));
        attachmentsPane.getAttachmentLink(attachmentReference.getName()).click();
        assertEquals("attachment2", utils.getDriver().findElement(By.tagName("html")).getText());

        // Revert to 2.1 (first update of the attachment)
        utils.gotoPage(testReference).openHistoryDocExtraPane().rollbackToVersion("2.1");

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
        vp.openHistoryDocExtraPane().rollbackToVersion("2.1");

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

        utils.gotoPage(testReference);
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("file.txt"));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment("file.txt"));

        // Revert to first attachment (2.1)
        HistoryPane historyPane = utils.gotoPage(testReference).openHistoryDocExtraPane();

        historyPane.rollbackToVersion("2.1");
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
        vp.waitForDocExtraPaneActive("Comments");

        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        vp = historyTab.viewVersion("1.1");

        // In the preview the Velocity macro should be forbidden.
        assertThat(vp.getContent(), startsWith("Failed to execute the [velocity] macro."));

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("Comments");

        historyTab = vp.openHistoryDocExtraPane();
        vp = historyTab.rollbackToVersion("1.1");

        // Rollback doesn't wait...
        // Wait for the comment tab to be selected since we're currently on the history tab and rolling
        // back is going to load a new page and make the focus active on the comments tab.
        vp.waitForDocExtraPaneActive("Comments");

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
        vp.waitForDocExtraPaneActive("Comments");

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
        // We want a minor version.
        objectEditPage.clickSaveAndContinue();
        objectEditPage.clickCancel();

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
        // We want a minor version.
        objectEditPage.clickSaveAndContinue();

        // We don't use the Cancel button to leave the edit mode because it takes us to the "view" mode which, depending
        // on whether the XWiki.XWikiPreferences page has an XWiki.XWikiPreferences object or not, can be redirected to
        // the "admin" mode (by the XWiki preferences sheet), which locks back the XWiki.XWikiPreferences page. We want
        // to switch the user next, which doesn't remove the lock (see XWIKI-22430: Logging out does not unlock pages
        // that were being edited), so we leave the edit mode by going to a page that doesn't add the edit lock back.
        setup.gotoPage(xwikiPreferences.getLastSpaceReference());

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
            // We don't use the Cancel button to leave the edit mode because the XWiki preferences sheet redirects to
            // "admin" mode which locks back the page. Instead, we go to a page that doesn't add the edit lock back.
            setup.gotoPage(xwikiPreferences.getLastSpaceReference());
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
        // We want a minor version.
        objectEditPage.clickSaveAndContinue();

        // We don't use the Cancel button to leave the edit mode because it takes us to the "view" mode which, depending
        // on whether the XWiki.XWikiPreferences page has an XWiki.XWikiPreferences object or not, can be redirected to
        // the "admin" mode (by the XWiki preferences sheet), which locks back the XWiki.XWikiPreferences page. We want
        // to switch the user next, which doesn't remove the lock (see XWIKI-22430: Logging out does not unlock pages
        // that were being edited), so we leave the edit mode by going to a page that doesn't add the edit lock back.
        setup.gotoPage(xwikiPreferences.getLastSpaceReference());

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
            // We don't use the Cancel button to leave the edit mode because the XWiki preferences sheet redirects to
            // "admin" mode which locks back the page. Instead, we go to a page that doesn't add the edit lock back.
            setup.gotoPage(xwikiPreferences.getLastSpaceReference());
        } finally {
            // Put back the page in the state it was before our changes
            setup.loginAsSuperAdmin();
            setup.gotoPage(xwikiPreferences, "view", "viewer=history");
            historyPane = new HistoryPane();
            historyPane = historyPane.showMinorEdits();
            historyPane.rollbackToVersion(latestVersionBeforeChanges);
        }
    }

    @Test
    @Order(10)
    void getRevisionsWithCriteria(TestUtils testUtils, TestReference testReference,
        LogCaptureConfiguration logCaptureConfiguration) throws Exception
    {
        testUtils.rest().delete(testReference);
        testUtils.rest().savePage(testReference, "Some content", "Title");
        testUtils.rest().savePage(testReference, "Some content 1", "Title");
        testUtils.rest().savePage(testReference, "Some content 1 2", "Title");
        testUtils.rest().savePage(testReference, "Some content 1 2 3", "Title");
        testUtils.rest().savePage(testReference, "Some content 1 2 3 4", "Title");

        ViewPage viewPage = testUtils.gotoPage(testReference);
        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        assertEquals("5.1", historyPane.getCurrentVersion());
        assertEquals(5, historyPane.getNumberOfVersions());

        EntityReference targetTestReference =
            testUtils.resolveDocumentReference("xwiki:Test.getRevisionsWithCriteriaFoo.WebHome");
        String script = String.format("""
            {{velocity}}
            #set ($oldRef = "%s")
            #set ($startAt = 0)
            #set ($endAt = -1)
            #set ($criteria = $xwiki.criteriaService.revisionCriteriaFactory.createRevisionCriteria('', $minorVersions))
            #set ($range = $xwiki.criteriaService.rangeFactory.createRange($startAt, $endAt))
            #set ($discard = $criteria.setRange($range))
            #set ($myDoc = $xwiki.getDocument($oldRef))
            #set ($xwikiDoc = $myDoc.document)
            #set ($discard = $myDoc.document.loadArchive($xcontext.context))
            XWiki Doc: $xwikiDoc
            #set ($revisions = $xwikiDoc.getRevisions($criteria, $xcontext.context))
            Revision: $revisions
            #set ($newRef = $services.model.resolveDocument("%s"))
            #set ($oldRef = $xwikiDoc.documentReference)
            #set ($discard = $xwikiDoc.setDocumentReference($newRef))
            XWiki Doc: $xwikiDoc
            #set ($revisions = $xwikiDoc.getRevisions($criteria, $xcontext.context))
            Revision: $revisions
            ## Restore the original document reference on the cached document instance otherwise we can't delete the
            ## document (without restarting the XWiki instance or clearing the cache).
            #set ($discard = $xwikiDoc.setDocumentReference($oldRef))
            {{/velocity}}
            """, testReference, targetTestReference);

        String obtainedResult = testUtils.executeWikiPlain(script, Syntax.XWIKI_2_1);
        String expectedResult = String.format("""
            XWiki Doc: %s
            Revision: [5.1]
            XWiki Doc: %s
            Revision: [5.1]""", testUtils.serializeLocalReference(testReference),
            testUtils.serializeLocalReference(targetTestReference));
        assertEquals(expectedResult, obtainedResult);
        logCaptureConfiguration.registerExpectedRegexes("^.*\\QDeprecated usage of method "
            + "[com.xpn.xwiki.doc.XWikiDocument.setDocumentReference] in xwiki:Test.Execute\\E.*$");
    }

    private void assertDiff(List<String> actualLines, String... expectedLines)
    {
        if (expectedLines.length > 0 && !expectedLines[0].startsWith("@@")) {
            assertEquals(List.of(expectedLines), actualLines.subList(1, actualLines.size()));
        } else {
            assertEquals(List.of(expectedLines), actualLines);
        }
    }

    private File getFileToUpload(TestConfiguration testConfiguration, String filename)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), "AttachmentIT/" + filename);
    }

    @Test
    @Order(11)
    void versionNavigation(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
    {
        // Version 1.1
        ViewPage viewPage = testUtils.createPage(testReference, "one\ntwo\nthree", "Test");

        // Change the content and the meta data.
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("content", "one\n**two**\nfour");
        queryMap.put("title", "Compare verSions test");
        queryMap.put("parent", "Sandbox.WebHome");
        queryMap.put("commentinput", "Changed content and meta data.");
        queryMap.put("minorEdit", "true");
        // Version 1.2
        testUtils.gotoPage(testReference, "save", queryMap);

        queryMap = new HashMap<>();
        queryMap.put("title", "Compare versions test");
        queryMap.put("commentinput", "Fix typo in title.");
        queryMap.put("minorEdit", "true");
        // Version 1.3
        testUtils.gotoPage(testReference, "save", queryMap);

        viewPage = testUtils.gotoPage(testReference);
        // Add objects.
        ObjectEditPage objectEditPage = ObjectEditPage.gotoPage(testReference);
        FormContainerElement form = objectEditPage.addObject("XWiki.JavaScriptExtension");
        Map<String, String> assignment = new HashMap<>();
        assignment.put("XWiki.JavaScriptExtension_0_name", "JavaScript code");
        assignment.put("XWiki.JavaScriptExtension_0_code", "var tmp = alice;\nalice = bob;\nbob = tmp;");
        assignment.put("XWiki.JavaScriptExtension_0_use", "onDemand");
        form.fillFieldsByName(assignment);
        // Version 1.4
        objectEditPage.clickSaveAndContinue();
        assignment.put("XWiki.JavaScriptExtension_0_name", "Code snippet");
        assignment.put("XWiki.JavaScriptExtension_0_code", "var tmp = alice;\nalice = 2 * bob;\nbob = tmp;");
        form.fillFieldsByName(assignment);
        // Version 1.5
        objectEditPage.clickSaveAndContinue();

        // Create class.
        ClassEditPage classEditPage = objectEditPage.editClass();
        // Version 1.6
        classEditPage.addProperty("age", "Number");
        // Version 1.7
        classEditPage.addProperty("color", "String");
        classEditPage.getNumberClassEditElement("age").setNumberType("integer");
        // Version 1.8
        classEditPage.clickSaveAndContinue();
        // Version 1.9
        classEditPage.deleteProperty("color");
        // Version 1.10
        viewPage = classEditPage.clickSaveAndView();

        // Attach files.
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        // TODO: Update this code when we (re)add support for uploading multiple files at once.
        // Version 2.1, 3.1, 4.1
        for (String fileName : new String[] {"SmallAttachment.txt", "SmallAttachment2.txt", "SmallAttachment.txt"}) {
            attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, fileName).getAbsolutePath());
            attachmentsPane.waitForUploadToFinish(fileName);
            attachmentsPane.clickHideProgress();
        }
        // Version 5.1
        attachmentsPane.deleteAttachmentByFileByName("SmallAttachment2.txt");

        // Add comments.
        testUtils.createUserAndLogin("Alice", "ecila");
        viewPage = testUtils.gotoPage(testReference);
        CommentsTab commentsTab = viewPage.openCommentsDocExtraPane();
        // Version 5.2
        commentsTab.postComment("first line\nsecond line", true);
        commentsTab.editCommentByID(0, "first line\nline in between\nsecond line");

        // Version 5.5
        commentsTab.replyToCommentByID(0, "this is a reply");
        commentsTab.deleteCommentByID(1);

        testUtils.loginAsSuperAdmin();

        viewPage = testUtils.gotoPage(testReference);
        HistoryPane historyTab = viewPage.openHistoryDocExtraPane().showMinorEdits();
        String currentVersion = historyTab.getCurrentVersion();
        // If the document has many versions, like in this case, the versions are paginated and currently there's
        // no way to compare two versions from two different pagination pages using the UI. Thus we have to build the
        // URL and load the compare page manually. Update the code when we remove this UI limitation.
        // ChangesPane changesPane = historyTab.compare("1.1", currentVersion).getChangesPane();
        String queryString = String.format("viewer=changes&rev1=1.1&rev2=%s", currentVersion);
        testUtils.gotoPage(testReference, "view", queryString);
        ChangesPane changesPane = new ChangesPane();

        // Version summary.
        String today = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        assertTrue(changesPane.getFromVersionSummary().startsWith(
            "From version 1.1\nedited by superadmin\non " + today));
        assertTrue(changesPane.getToVersionSummary().startsWith(
            "To version " + currentVersion + "\nedited by Alice\non " + today));
        assertEquals("Change comment: Deleted object", changesPane.getChangeComment());

        RawChanges rawChanges = changesPane.getRawChanges();

        // Diff summary.
        DocumentDiffSummary diffSummary = rawChanges.getDiffSummary();
        assertThat(List.of("Page properties", "Attachments", "Objects", "Class properties"),
            containsInAnyOrder(diffSummary.getItems().toArray()));
        assertEquals("(4 modified, 0 added, 0 removed)", diffSummary.getPagePropertiesSummary());
        assertEquals("(0 modified, 1 added, 0 removed)", diffSummary.getAttachmentsSummary());
        assertEquals("(0 modified, 2 added, 0 removed)", diffSummary.getObjectsSummary());
        assertEquals("(0 modified, 1 added, 0 removed)", diffSummary.getClassPropertiesSummary());
        assertEquals(List.of("SmallAttachment.txt"), diffSummary.toggleAttachmentsDetails().getAddedAttachments());
        assertEquals(List.of("XWiki.JavaScriptExtension[0]", "XWiki.XWikiComments[0]"), diffSummary
            .toggleObjectsDetails().getAddedObjects());
        assertEquals(List.of("age"), diffSummary.toggleClassPropertiesDetails().getAddedClassProperties());

        // Diff details.
        assertThat(List.of("Page properties", "SmallAttachment.txt", "XWiki.JavaScriptExtension[0]",
            "XWiki.XWikiComments[0]", "age"), containsInAnyOrder(rawChanges.getChangedEntities().toArray()));

        // Page properties changes.
        EntityDiff pageProperties = rawChanges.getEntityDiff("Page properties");
        assertThat(List.of("Title", "Parent", "Author", "Content"),
            containsInAnyOrder(pageProperties.getPropertyNames().toArray()));
        assertDiff(pageProperties.getDiff("Title"), "-<del>T</del>est",
            "+<ins>Compar</ins>e<ins> ver</ins>s<ins>ions </ins>t<ins>est</ins>");
        assertDiff(pageProperties.getDiff("Parent"), "+Sandbox.WebHome");
        assertDiff(pageProperties.getDiff("Author"), "-XWiki.<del>superadm</del>i<del>n</del>",
            "+XWiki.<ins>Al</ins>i<ins>ce</ins>");
        assertDiff(pageProperties.getDiff("Content"), "@@ -1,3 +1,3 @@", " one", "-two",
            "-<del>th</del>r<del>ee</del>", "+<ins>**</ins>two<ins>**</ins>", "+<ins>fou</ins>r");

        // Attachment changes.
        EntityDiff attachmentDiff = rawChanges.getEntityDiff("SmallAttachment.txt");
        assertThat(List.of("Author", "Size", "Content"),
            containsInAnyOrder(attachmentDiff.getPropertyNames().toArray()));
        assertDiff(attachmentDiff.getDiff("Author"), "+XWiki.superadmin");
        assertDiff(attachmentDiff.getDiff("Size"), "+27 bytes");
        assertDiff(attachmentDiff.getDiff("Content"), "+This is a small attachment.");

        // Object changes.
        EntityDiff jsxDiff = rawChanges.getEntityDiff("XWiki.JavaScriptExtension[0]");
        assertThat(List.of("Caching policy", "Name", "Use this extension", "Code"),
            containsInAnyOrder(jsxDiff.getPropertyNames().toArray()));
        assertDiff(jsxDiff.getDiff("Caching policy"), "+long");
        assertDiff(jsxDiff.getDiff("Name"), "+Code snippet");
        assertDiff(jsxDiff.getDiff("Use this extension"), "+onDemand");
        assertDiff(jsxDiff.getDiff("Code"), "+var tmp = alice;", "+alice = 2 * bob;", "+bob = tmp;");

        // Comment changes.
        EntityDiff commentDiff = rawChanges.getEntityDiff("XWiki.XWikiComments[0]");
        assertThat(List.of("Author", "Date", "Comment"),
            containsInAnyOrder(commentDiff.getPropertyNames().toArray()));
        assertDiff(commentDiff.getDiff("Author"), "+XWiki.Alice");
        assertEquals(2, commentDiff.getDiff("Date").size());
        assertDiff(commentDiff.getDiff("Comment"), "+first line", "+line in between", "+second line");

        // Class property changes.
        EntityDiff ageDiff = rawChanges.getEntityDiff("age");
        assertThat(List.of("Name", "Number", "Pretty Name", "Size", "Number Type"),
            containsInAnyOrder(ageDiff.getPropertyNames().toArray()));
        assertDiff(ageDiff.getDiff("Name"), "+age");
        assertDiff(ageDiff.getDiff("Number"), "+1");
        assertDiff(ageDiff.getDiff("Pretty Name"), "+age");
        assertDiff(ageDiff.getDiff("Size"), "+30");
        assertDiff(ageDiff.getDiff("Number Type"), "+integer");

        // Version navigation
        queryString = "viewer=changes&rev1=1.2&rev2=5.4";
        testUtils.gotoPage(testReference, "view", queryString);
        changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.4", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousChange();
        changesPane = new ChangesPane();
        assertEquals("1.1", changesPane.getFromVersion());
        assertEquals("1.2", changesPane.getToVersion());
        assertFalse(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertFalse(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextChange();
        changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousFromVersion();
        changesPane = new ChangesPane();
        assertEquals("1.1", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertFalse(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertFalse(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextFromVersion();
        changesPane = new ChangesPane();
        changesPane.clickNextFromVersion();
        changesPane = new ChangesPane();
        assertEquals("1.3", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextFromVersion();
        changesPane = new ChangesPane();
        assertEquals("1.4", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousChange();
        changesPane = new ChangesPane();
        assertEquals("1.3", changesPane.getFromVersion());
        assertEquals("1.2", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        queryString = "viewer=changes&rev1=1.9&rev2=2.1";
        testUtils.gotoPage(testReference, "view", queryString);
        changesPane = new ChangesPane();
        assertEquals("1.9", changesPane.getFromVersion());
        assertEquals("2.1", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextChange();
        changesPane = new ChangesPane();
        assertEquals("2.1", changesPane.getFromVersion());
        assertEquals("3.1", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        queryString = "viewer=changes&rev1=1.2&rev2=5.4";
        testUtils.gotoPage(testReference, "view", queryString);
        changesPane.clickNextToVersion();
        changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.5", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertFalse(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertFalse(changesPane.hasNextToVersion());

        changesPane.clickPreviousToVersion();
        changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.4", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        // Tests that the unified diff (for multi-line text) shows the inline changes.
        viewPage = testUtils.gotoPage(testReference);
        changesPane = viewPage.openHistoryDocExtraPane().showMinorEdits().compare("1.4", "1.5").getChangesPane();
        jsxDiff = changesPane.getRawChanges().getEntityDiff("XWiki.JavaScriptExtension[0]");
        assertDiff(jsxDiff.getDiff("Code"), "@@ -1,3 +1,3 @@", " var tmp = alice;", "-alice = bob;",
            "+alice = <ins>2 * </ins>bob;", " bob = tmp;");

        // Tests that a message is displayed when there are no changes.
        viewPage = testUtils.gotoPage(testReference);
        historyTab = viewPage.openHistoryDocExtraPane();
        currentVersion = historyTab.getCurrentVersion();
        assertTrue(historyTab.compare(currentVersion, currentVersion).getChangesPane().getRawChanges().hasNoChanges());

        // Check that the source of a specific version is correct
        viewPage = testUtils.gotoPage(testReference);
        historyTab = viewPage.openHistoryDocExtraPane();
        historyTab = historyTab.showMinorEdits();
        viewPage = historyTab.viewVersion("1.1");
        assertTrue(viewPage.getLastModifiedText().startsWith("Version 1.1 by superadmin on " + today), "Version "
            + "information incorrect: " + viewPage.getLastModifiedText());
        testUtils.getDriver().addPageNotYetReloadedMarker();
        viewPage.clickMoreActionsSubMenuEntry("tmViewSource");
        testUtils.getDriver().waitUntilPageIsReloaded();
        SourceViewer sourceViewer = new SourceViewer();
        assertEquals(3, sourceViewer.getLineNumber());
        assertEquals("one\ntwo\nthree", sourceViewer.getEntireSource());

        // Test delete versions
        viewPage = testUtils.gotoPage(testReference);
        historyTab = viewPage.openHistoryDocExtraPane().showMinorEdits();
        historyTab.deleteRangeVersions("1.3", "5.4");
        queryString = "viewer=changes&rev1=1.1&rev2=1.2";
        testUtils.gotoPage(testReference, "view", queryString);
        changesPane = new ChangesPane();
        assertEquals("1.1", changesPane.getFromVersion());
        assertEquals("1.2", changesPane.getToVersion());
        assertFalse(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertFalse(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());
        changesPane.clickNextChange();

        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.5", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertFalse(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertFalse(changesPane.hasNextToVersion());
    }
}
