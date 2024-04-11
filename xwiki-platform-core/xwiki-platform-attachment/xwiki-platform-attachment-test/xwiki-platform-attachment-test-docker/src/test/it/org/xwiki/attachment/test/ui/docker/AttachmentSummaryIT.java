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
package org.xwiki.attachment.test.ui.docker;

import java.io.File;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.attachment.test.po.AttachmentPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentHistoryPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.REFERENCE;

/**
 * Tests for setting and displaying attachment summaries.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@UITest(properties = {
    "xwikiPropertiesAdditionalProperties=attachment.upload.enableSummaries = true"
}, extraJARs = {
    "org.xwiki.platform:xwiki-platform-attachment-validation-default"
})
class AttachmentSummaryIT
{
    private static final String ATTACHMENT_FILENAME = "moveme.txt";

    @Test
    @Order(1)
    void checkSummary(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        DocumentReference attachmentsPage = new DocumentReference("Attachments", testReference.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        setup.deletePage(attachmentsPage);

        setup.createPage(attachmentsPage, "");

        String attachmentPath = new File(new File(testConfiguration.getBrowser().getTestResourcesPath(), "v0"),
            ATTACHMENT_FILENAME).getAbsolutePath();

        setup.gotoPage(attachmentsPage);
        AttachmentsPane sourceAttachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        sourceAttachmentsPane.setUploadSummary("Summary 1");
        sourceAttachmentsPane.setFileToUpload(attachmentPath);
        sourceAttachmentsPane.waitForUploadToFinish(ATTACHMENT_FILENAME);
        sourceAttachmentsPane.setUploadSummary("Summary 2");
        sourceAttachmentsPane.setFileToUpload(attachmentPath);
        sourceAttachmentsPane.waitForUploadToFinish(ATTACHMENT_FILENAME);

        AttachmentsPane attachmentsPaneTarget = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        AttachmentHistoryPage attachmentHistoryPage = attachmentsPaneTarget.goToAttachmentHistory(ATTACHMENT_FILENAME);
        assertEquals("1.1", attachmentHistoryPage.getVersion(1));
        assertEquals("Summary 1", attachmentHistoryPage.getSummary(1));
        assertEquals("1.2", attachmentHistoryPage.getVersion(2));
        assertEquals("Summary 2", attachmentHistoryPage.getSummary(2));
    }
}
