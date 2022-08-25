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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ChangesPane;
import org.xwiki.test.ui.po.ComparePage;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the attachments.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest(properties = {
    // Add the FileUploadPlugin which is needed by the test to upload attachment files
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"
})
class AttachmentIT
{
    private static final String FIRST_ATTACHMENT = "SmallAttachment.txt";

    private static final String SECOND_ATTACHMENT = "SmallAttachment2.txt";

    private static final String IMAGE_ATTACHMENT = "image.gif";

    private static final String SMALL_SIZE_ATTACHMENT = "SmallSizeAttachment.png";

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.createUser("User2", "pass", "");
        setup.createUserAndLogin("User1", "pass");
    }

    private File getFileToUpload(TestConfiguration testConfiguration, String filename)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), "AttachmentIT/" + filename);
    }

    /**
     * Ensure that the attachment is properly deleted through the UI.
     *
     * @throws Exception in case of errors
     */
    @Test
    @Order(1)
    void uploadAttachments(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        String testPageName = setup.serializeReference(testReference).split(":")[1];
        setup.rest().delete(testReference);
        setup.rest().savePage(testReference, "", "");
        Page page = setup.rest().get(testReference);
        // We make the page hidden as we identified some issues specific to hidden pages (see XWIKI-20093).
        // If it happens that some issues are specific to non-hidden pages, the test will need to be improved to 
        // cover both cases (which will make the execution time of the test suite larger).
        page.setHidden(true);
        setup.rest().save(page);
        ViewPage viewPage = setup.gotoPage(testReference);
        AttachmentsPane attachmentsPane = viewPage.openAttachmentsDocExtraPane();

        // Upload two attachments and check them
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, FIRST_ATTACHMENT).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(FIRST_ATTACHMENT);
        attachmentsPane.clickHideProgress();
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, SECOND_ATTACHMENT).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(SECOND_ATTACHMENT);
        attachmentsPane.clickHideProgress();
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentExistsByFileName(FIRST_ATTACHMENT));
        assertTrue(attachmentsPane.attachmentExistsByFileName(SECOND_ATTACHMENT));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment(FIRST_ATTACHMENT));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment(SECOND_ATTACHMENT));

        String attachmentURLScheme = String.format("%sdownload/%s/%%s?rev=1.1", setup.getBaseBinURL(),
            testPageName.replace('.', '/'));
        assertEquals(String.format(attachmentURLScheme, FIRST_ATTACHMENT),
            attachmentsPane.getAttachmentLink(FIRST_ATTACHMENT).getAttribute("href"));
        assertEquals(String.format(attachmentURLScheme, SECOND_ATTACHMENT),
            attachmentsPane.getAttachmentLink(SECOND_ATTACHMENT).getAttribute("href"));

        attachmentsPane.getAttachmentLink(FIRST_ATTACHMENT).click();
        assertEquals("This is a small attachment.", setup.getDriver().findElement(By.tagName("html")).getText());
        setup.getDriver().navigate().back();

        // TODO: remove when https://jira.xwiki.org/browse/XWIKI-15513 is fixed
        this.refreshAttachmentsPane(setup);
        viewPage.waitForDocExtraPaneActive("attachments");

        attachmentsPane.getAttachmentLink(SECOND_ATTACHMENT).click();
        assertEquals("This is another small attachment.", setup.getDriver().findElement(By.tagName("html")).getText());
        setup.getDriver().navigate().back();
        // TODO: remove when https://jira.xwiki.org/browse/XWIKI-15513 is fixed
        this.refreshAttachmentsPane(setup);
        viewPage.waitForDocExtraPaneActive("attachments");

        // Upload another version of the first attachment
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, "v2/" + FIRST_ATTACHMENT).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(FIRST_ATTACHMENT);
        attachmentsPane.clickHideProgress();
        assertTrue(attachmentsPane.attachmentExistsByFileName(FIRST_ATTACHMENT));
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(FIRST_ATTACHMENT));
        String attachmentURL = String.format("%sdownload/%s/%s?rev=1.2", setup.getBaseBinURL(),
            testPageName.replace('.', '/'), FIRST_ATTACHMENT);
        assertEquals(attachmentURL, attachmentsPane.getAttachmentLink(FIRST_ATTACHMENT).getAttribute("href"));
        attachmentsPane.getAttachmentLink(FIRST_ATTACHMENT).click();
        assertEquals("This is a small attachment v2.", setup.getDriver().findElement(By.tagName("html")).getText());
        setup.getDriver().navigate().back();
        // TODO: remove when https://jira.xwiki.org/browse/XWIKI-15513 is fixed
        this.refreshAttachmentsPane(setup);
        viewPage.waitForDocExtraPaneActive("attachments");
        attachmentsPane.waitForAttachmentsLivetable();

        attachmentsPane.deleteAttachmentByFileByName(FIRST_ATTACHMENT);
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentExistsByFileName(SECOND_ATTACHMENT));

        // Go back to the page so we can check that the right attachment has really been deleted
        viewPage = setup.gotoPage(testReference);
        attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals(String.format(attachmentURLScheme, SECOND_ATTACHMENT),
            attachmentsPane.getAttachmentLink(SECOND_ATTACHMENT).getAttribute("href"));
    }

    @Test
    @Order(2)
    void attachAndViewGifImage(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        // Prepare the page to display the GIF image. We explicitly set the width to a value greater than the actual
        // image width because we want the code that resizes the image on the server side to be executed (even if the
        // image is not actually resized).
        ViewPage viewPage = setup.createPage(testReference,
            String.format("[[image:image.gif||width=%s]]", 142), "");

        // Attach the GIF image.
        AttachmentsPane attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, IMAGE_ATTACHMENT).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(IMAGE_ATTACHMENT);
        assertTrue(attachmentsPane.attachmentExistsByFileName(IMAGE_ATTACHMENT));
    }

    @Test
    @Order(3)
    void diffWithDeletedAttachments(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        // v1.1
        setup.createPage(testReference, "", "");
        // v2.1
        setup.attachFile(testReference, "toto.txt",
            getClass().getResourceAsStream("/AttachmentIT/testDiff/v1.1/toto.txt"), true);
        // v3.1
        setup.deleteAttachement(testReference, "toto.txt");
        // Milliseconds are dropped from attachment date: if we create them too fast
        // we cannot rely on date to compare them. So make sure to wait at least 1 sec before continue the work.
        Thread.sleep(1000);
        // v4.1
        setup.attachFile(testReference, "toto.txt",
            getClass().getResourceAsStream("/AttachmentIT/testDiff/v2.1/toto.txt"), true);
        // v5.1
        setup.attachFile(testReference, "toto.txt",
            getClass().getResourceAsStream("/AttachmentIT/testDiff/v2.2/toto.txt"), false);
        // v6.1
        setup.attachFile(testReference, "toto.txt",
            getClass().getResourceAsStream("/AttachmentIT/testDiff/v2.3/toto.txt"), false);
        // v7.1
        setup.deleteAttachement(testReference, "toto.txt");

        ViewPage viewPage = setup.gotoPage(testReference);
        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        ComparePage compare = historyPane.compare("1.1", "2.1");

        ChangesPane changesPane = compare.getChangesPane();
        DocumentDiffSummary diffSummary = changesPane.getDiffSummary();
        assertEquals(Collections.singletonList("toto.txt"),
            diffSummary.toggleAttachmentsDetails().getAddedAttachments());
        EntityDiff content = changesPane.getEntityDiff("toto.txt");
        assertEquals(Arrays.asList("@@ -1,0 +1,1 @@", "+v1.1"), content.getDiff("Content"));

        viewPage = setup.gotoPage(testReference);
        historyPane = viewPage.openHistoryDocExtraPane();
        compare = historyPane.compare("2.1", "3.1");
        changesPane = compare.getChangesPane();
        diffSummary = changesPane.getDiffSummary();
        assertEquals(Collections.singletonList("toto.txt"),
            diffSummary.toggleAttachmentsDetails().getRemovedAttachments());
        content = changesPane.getEntityDiff("toto.txt");
        assertEquals(Arrays.asList("@@ -1,1 +1,0 @@", "-v1.1"), content.getDiff("Content"));

        viewPage = setup.gotoPage(testReference);
        historyPane = viewPage.openHistoryDocExtraPane();
        compare = historyPane.compare("5.1", "7.1");
        changesPane = compare.getChangesPane();
        diffSummary = changesPane.getDiffSummary();
        assertEquals(Collections.singletonList("toto.txt"),
            diffSummary.toggleAttachmentsDetails().getRemovedAttachments());
        content = changesPane.getEntityDiff("toto.txt");
        assertEquals(Arrays.asList("@@ -1,1 +1,0 @@", "-v2.2"), content.getDiff("Content"));

        viewPage = setup.gotoPage(testReference);
        historyPane = viewPage.openHistoryDocExtraPane();
        compare = historyPane.compare("2.1", "6.1");
        changesPane = compare.getChangesPane();
        diffSummary = changesPane.getDiffSummary();
        assertEquals(Collections.singletonList("toto.txt"),
            diffSummary.toggleAttachmentsDetails().getModifiedAttachments());
        content = changesPane.getEntityDiff("toto.txt");
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@", "-v<del>1</del>.<del>1</del>", "+v<ins>2</ins>.<ins>3</ins>"),
            content.getDiff("Content"));

    }

    /**
     * Ensure that an attachment is properly restored after a rollback of a page that has been restored from deletion.
     */
    @Test
    @Order(4)
    void rollbackAttachmentFromRestoredPage(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.deletePage(testReference);

        // v1.1
        setup.createPage(testReference, "");

        // v2.1
        setup.attachFile(testReference, "toto.txt",
            getClass().getResourceAsStream("/AttachmentIT/testDiff/v1.1/toto.txt"), true);
        // v3.1
        setup.attachFile(testReference, "toto.txt",
            getClass().getResourceAsStream("/AttachmentIT/testDiff/v2.1/toto.txt"), false);

        setup.deletePage(testReference);
        setup.gotoPage(testReference);

        DeletePageOutcomePage deletePageOutcomePage = new DeletePageOutcomePage();
        ViewPage viewPage = deletePageOutcomePage.clickRestore();

        AttachmentsPane attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("toto.txt"));
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment("toto.txt"));
        attachmentsPane.getAttachmentLink("toto.txt").click();
        assertEquals("v2.1", setup.getDriver().findElement(By.tagName("html")).getText());

        viewPage = setup.gotoPage(testReference);
        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        viewPage = historyPane.rollbackToVersion("2.1");
        attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("toto.txt"));
        assertEquals("1.3", attachmentsPane.getLatestVersionOfAttachment("toto.txt"));
        attachmentsPane.getAttachmentLink("toto.txt").click();
        assertEquals("v1.1", setup.getDriver().findElement(By.tagName("html")).getText());
    }

    @Test
    @Order(5)
    void filterAttachmentsLivetable(TestUtils setup, TestReference testReference) throws Exception
    {
        ViewPage viewPage = setup.createPage(testReference, "", "");

        // Upload attachments with 2 different users.
        AttachmentsPane attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        setup.attachFile(testReference, FIRST_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + FIRST_ATTACHMENT), false);
        setup.attachFile(testReference, SECOND_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + SECOND_ATTACHMENT), false);

        setup.login("User2", "pass");
        setup.gotoPage(testReference);
        attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        setup.attachFile(testReference, SMALL_SIZE_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + SMALL_SIZE_ATTACHMENT), false);

        attachmentsPane.filterColumn("filename", "SmallAttachment");
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        attachmentsPane.filterColumn("filename", "");

        attachmentsPane.filterColumn("mimeType", "Image");
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals(SMALL_SIZE_ATTACHMENT, attachmentsPane.getAttachmentNameByPosition(1));
        attachmentsPane.filterColumn("mimeType", "Text");
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        attachmentsPane.filterColumn("mimeType", "All");

        attachmentsPane.filterColumn("filesize", "Tiny");
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        attachmentsPane.filterColumn("filesize", "Small");
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals(SMALL_SIZE_ATTACHMENT, attachmentsPane.getAttachmentNameByPosition(1));
        attachmentsPane.filterColumn("filesize", "All");

        attachmentsPane.filterColumn("author", "User1");
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        attachmentsPane.filterColumn("author", "User2");
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals(SMALL_SIZE_ATTACHMENT, attachmentsPane.getAttachmentNameByPosition(1));
        attachmentsPane.filterColumn("author", "");

        String firstAttachUploadDate = attachmentsPane.getDateOfLastUpload(FIRST_ATTACHMENT);
        List<String> uploadDates =
            Arrays.asList(firstAttachUploadDate, attachmentsPane.getDateOfLastUpload(SECOND_ATTACHMENT),
                attachmentsPane.getDateOfLastUpload(SMALL_SIZE_ATTACHMENT));
        attachmentsPane.filterColumn("date", firstAttachUploadDate);
        long expected = uploadDates.stream().filter(d -> d.equals(firstAttachUploadDate)).count();
        assertEquals(expected, attachmentsPane.getNumberOfAttachments());
        attachmentsPane.filterColumn("date", "");
    }

    @Test
    @Order(6)
    void addAttachmentsMacroToPageContent(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        DocumentReference attachmentsDocRef =
            new DocumentReference("PageWithAttachments", testReference.getLastSpaceReference());
        setup.createPage(attachmentsDocRef, "", "");
        setup.attachFile(attachmentsDocRef, FIRST_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + FIRST_ATTACHMENT), false);
        setup.attachFile(attachmentsDocRef, SECOND_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + SECOND_ATTACHMENT), false);
        setup.attachFile(attachmentsDocRef, IMAGE_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + IMAGE_ATTACHMENT), false);

        ViewPage viewPage = setup.createPage(testReference, getAttachmentsMacroContent(attachmentsDocRef), "");

        setup.attachFile(testReference, FIRST_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + FIRST_ATTACHMENT), false);
        setup.attachFile(testReference, SECOND_ATTACHMENT,
            getClass().getResourceAsStream("/AttachmentIT/" + SECOND_ATTACHMENT), false);

        viewPage.reloadPage();
        AttachmentsPane pageAttachmentsPane = viewPage.openAttachmentsDocExtraPane();
        AttachmentsPane macroAttachmentsPane = new AttachmentsPane("testAttachments");

        // Check the delete action with multiple attachments livetables displayed.
        assertEquals(3, macroAttachmentsPane.getNumberOfAttachmentsDisplayed());
        assertEquals(2, pageAttachmentsPane.getNumberOfAttachmentsDisplayed());
        macroAttachmentsPane.deleteAttachmentByFileByName(FIRST_ATTACHMENT);
        assertEquals(2, macroAttachmentsPane.getNumberOfAttachmentsDisplayed());
        assertEquals(2, pageAttachmentsPane.getNumberOfAttachmentsDisplayed());

        pageAttachmentsPane.deleteAttachmentByFileByName(FIRST_ATTACHMENT);
        assertEquals(2, macroAttachmentsPane.getNumberOfAttachmentsDisplayed());
        assertEquals(1, pageAttachmentsPane.getNumberOfAttachmentsDisplayed());

        // Check filtering with multiple attachments livetables displayed.
        macroAttachmentsPane.filterColumn("filename", IMAGE_ATTACHMENT);
        assertEquals(1, macroAttachmentsPane.getNumberOfAttachmentsDisplayed());
        assertEquals(1, pageAttachmentsPane.getNumberOfAttachmentsDisplayed());
        macroAttachmentsPane.filterColumn("filename", "");

        pageAttachmentsPane.filterColumn("filename", SECOND_ATTACHMENT);
        assertEquals(2, macroAttachmentsPane.getNumberOfAttachmentsDisplayed());
        assertEquals(1, pageAttachmentsPane.getNumberOfAttachmentsDisplayed());

        macroAttachmentsPane.filterColumn("mimeType", "Image");
        assertEquals(1, macroAttachmentsPane.getNumberOfAttachmentsDisplayed());
        assertEquals(1, pageAttachmentsPane.getNumberOfAttachmentsDisplayed());
        macroAttachmentsPane.filterColumn("mimeType", "All");
    }

    private String getAttachmentsMacroContent(DocumentReference docRef)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{{velocity}}\n{{html}}\n");
        sb.append("#template('attachment_macros.vm')\n");
        sb.append("#set($attachmentsDoc = $xwiki.getDocument(\"" + docRef + "\"))\n");
        sb.append("#showAttachmentsLivetable($attachmentsDoc 'testAttachments')\n");
        sb.append("{{/html}}\n{{/velocity}}");

        return sb.toString();
    }

    private void refreshAttachmentsPane(TestUtils setup)
    {
        setup.getDriver().navigate().refresh();
        // This is needed since when the attachments livetable is filtered, the fragment identifier is changed and
        // after a refresh the page will actually have the default tab opened. This will no longer be needed after
        // moving the attachments livetable to Live Data.
        (new ViewPage()).openAttachmentsDocExtraPane();
    }
}
