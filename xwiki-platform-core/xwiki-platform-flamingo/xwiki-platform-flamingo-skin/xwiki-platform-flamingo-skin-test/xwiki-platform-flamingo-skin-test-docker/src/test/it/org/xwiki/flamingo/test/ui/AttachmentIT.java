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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ViewPage;

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

    private String getFileToUpload(TestConfiguration testConfiguration, String filename)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), "AttachmentIT/" + filename)
            .getAbsolutePath();
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
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, firstAttachment));
        attachmentsPane.waitForUploadToFinish(firstAttachment);
        attachmentsPane.clickHideProgress();
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, secondAttachment));
        attachmentsPane.waitForUploadToFinish(secondAttachment);
        attachmentsPane.clickHideProgress();
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentExistsByFileName(firstAttachment));
        assertTrue(attachmentsPane.attachmentExistsByFileName(secondAttachment));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment(firstAttachment));
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment(secondAttachment));

        String attachmentURLScheme = String.format("%sdownloadrev/%s/%%s?rev=1.1", setup.getBaseBinURL(),
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
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, "v2/" + firstAttachment));
        attachmentsPane.waitForUploadToFinish(firstAttachment);
        attachmentsPane.clickHideProgress();
        assertTrue(attachmentsPane.attachmentExistsByFileName(firstAttachment));
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(firstAttachment));
        String attachmentURL = String.format("%sdownloadrev/%s/%s?rev=1.2", setup.getBaseBinURL(),
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
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, imageAttachment));
        attachmentsPane.waitForUploadToFinish(imageAttachment);
        assertTrue(attachmentsPane.attachmentExistsByFileName(imageAttachment));
    }
}
