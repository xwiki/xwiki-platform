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
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    private String getFileToUpload(TestConfiguration testConfiguration, String filename)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), filename).getAbsolutePath();
    }

    /**
     * Ensure that the attachment is properly deleted through the UI.
     */
    @Test
    @Order(1)
    public void deleteAttachment(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        String testPageName = setup.serializeReference(testReference).split(":")[1];
        ViewPage viewPage = setup.createPage(testReference, "Attachment deletion","Attachment deletion");
        AttachmentsPane attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, "SmallAttachment.txt"));
        attachmentsPane.waitForUploadToFinish("SmallAttachment.txt");
        attachmentsPane.setFileToUpload(getFileToUpload(testConfiguration, "SmallAttachment2.txt"));
        attachmentsPane.waitForUploadToFinish("SmallAttachment2.txt");
        assertEquals(2, attachmentsPane.getNumberOfAttachments());
        String attachmentURL = String.format("%sdownloadrev/%s/SmallAttachment.txt?rev=1.1", setup.getBaseBinURL(),
            testPageName.replace('.', '/'));
        assertEquals(attachmentURL, attachmentsPane.getAttachmentLink("SmallAttachment.txt").getAttribute("href"));
        attachmentsPane.deleteAttachmentByFileByName("SmallAttachment.txt");
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentExistsByFileName("SmallAttachment2.txt"));

        // Go back to the page so we can check that the right attachment has really been deleted
        viewPage = setup.gotoPage(testReference);
        attachmentsPane = viewPage.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        attachmentURL = String.format("%sdownloadrev/%s/SmallAttachment2.txt?rev=1.1", setup.getBaseBinURL(),
            testPageName.replace('.', '/'));
        assertEquals(attachmentURL, attachmentsPane.getAttachmentLink("SmallAttachment2.txt").getAttribute("href"));
    }
}
