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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ChangesPane;
import org.xwiki.test.ui.po.ComparePage;
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
public class AttachmentIT
{
    private static String firstAttachment = "SmallAttachment.txt";
    private static String secondAttachment = "SmallAttachment2.txt";
    private static String imageAttachment = "image.gif";

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterEach
    public void teardown(LogCaptureConfiguration logCaptureConfiguration)
    {
        logCaptureConfiguration.registerExcludes(
            "require.min.js?r=1, line 7: Error: Script error for \"jsTree\", needed by: tree, tree-finder",
            "require.min.js?r=1, line 7: Error: Script error for \"jquery\", needed by: jQueryNoConflict",
            "require.min.js?r=1, line 7: Error: Script error for \"/xwiki/webjars/wiki%3Axwiki/Keypress/2.1.5/keypress"
                + ".min.js?r=1\""
        );
    }

    private File getFileToUpload(TestConfiguration testConfiguration, String filename)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), "AttachmentIT/" + filename);
    }

    /**
     * Ensure that the attachment is properly deleted through the UI.
     */
    @Test
    @Order(1)
    public void uploadAttachments(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        String testPageName = setup.serializeReference(testReference).split(":")[1];
        ViewPage viewPage = setup.createPage(testReference, "", "");
        AttachmentsPane attachmentsPane = viewPage.openAttachmentsDocExtraPane();

        // Upload two attachments and check them
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, firstAttachment).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(firstAttachment);
        attachmentsPane.clickHideProgress();
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, secondAttachment).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(secondAttachment);
        attachmentsPane.clickHideProgress();
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentExistsByFileName(firstAttachment));
        assertTrue(attachmentsPane.attachmentExistsByFileName(secondAttachment));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment(firstAttachment));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment(secondAttachment));

        String attachmentURLScheme = String.format("%sdownload/%s/%%s?rev=1.1", setup.getBaseBinURL(),
            testPageName.replace('.', '/'));
        assertEquals(String.format(attachmentURLScheme, firstAttachment),
            attachmentsPane.getAttachmentLink(firstAttachment).getAttribute("href"));
        assertEquals(String.format(attachmentURLScheme, secondAttachment),
            attachmentsPane.getAttachmentLink(secondAttachment).getAttribute("href"));

        attachmentsPane.getAttachmentLink(firstAttachment).click();
        assertEquals("This is a small attachment.", setup.getDriver().findElement(By.tagName("html")).getText());
        setup.getDriver().navigate().back();

        // TODO: remove when https://jira.xwiki.org/browse/XWIKI-15513 is fixed
        setup.getDriver().navigate().refresh();
        viewPage.waitForDocExtraPaneActive("attachments");

        attachmentsPane.getAttachmentLink(secondAttachment).click();
        assertEquals("This is another small attachment.", setup.getDriver().findElement(By.tagName("html")).getText());
        setup.getDriver().navigate().back();
        // TODO: remove when https://jira.xwiki.org/browse/XWIKI-15513 is fixed
        setup.getDriver().navigate().refresh();
        viewPage.waitForDocExtraPaneActive("attachments");

        // Upload another version of the first attachment
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, "v2/" + firstAttachment).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(firstAttachment);
        attachmentsPane.clickHideProgress();
        assertTrue(attachmentsPane.attachmentExistsByFileName(firstAttachment));
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(firstAttachment));
        String attachmentURL = String.format("%sdownload/%s/%s?rev=1.2", setup.getBaseBinURL(),
            testPageName.replace('.', '/'), firstAttachment);
        assertEquals(attachmentURL, attachmentsPane.getAttachmentLink(firstAttachment).getAttribute("href"));
        attachmentsPane.getAttachmentLink(firstAttachment).click();
        assertEquals("This is a small attachment v2.", setup.getDriver().findElement(By.tagName("html")).getText());
        setup.getDriver().navigate().back();
        // TODO: remove when https://jira.xwiki.org/browse/XWIKI-15513 is fixed
        setup.getDriver().navigate().refresh();
        viewPage.waitForDocExtraPaneActive("attachments");

        attachmentsPane.deleteAttachmentByFileByName(firstAttachment);
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentExistsByFileName(secondAttachment));

        // Go back to the page so we can check that the right attachment has really been deleted
        viewPage = setup.gotoPage(testReference);
        attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals(String.format(attachmentURLScheme, secondAttachment),
            attachmentsPane.getAttachmentLink(secondAttachment).getAttribute("href"));
    }

    @Test
    @Order(2)
    public void attachAndViewGifImage(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        // Prepare the page to display the GIF image. We explicitly set the width to a value greater than the actual
        // image width because we want the code that resizes the image on the server side to be executed (even if the
        // image is not actually resized).
        ViewPage viewPage = setup.createPage(testReference,
            String.format("[[image:image.gif||width=%s]]", 142), "");

        // Attach the GIF image.
        AttachmentsPane attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, imageAttachment).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(imageAttachment);
        assertTrue(attachmentsPane.attachmentExistsByFileName(imageAttachment));
    }

    @Test
    @Order(3)
    public void diffWithDeletedAttachments(TestUtils setup, TestReference testReference,
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
}
