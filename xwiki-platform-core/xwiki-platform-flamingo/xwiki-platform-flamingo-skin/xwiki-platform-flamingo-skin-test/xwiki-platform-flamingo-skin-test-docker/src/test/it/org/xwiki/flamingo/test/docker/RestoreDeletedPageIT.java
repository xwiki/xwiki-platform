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

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests for restoring a deleted document from recycle bin.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@UITest
class RestoreDeletedPageIT
{
    /**
     * @see "XWIKI-9421: Attachment version is incremented when a document is restored from recycle bin"
     */
    @Test
    void restore(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Clean up.
        setup.rest().deletePage(testReference.getLastSpaceReference().getName(), testReference.getName());

        // Create a new page.
        ViewPage page = setup.createPage(testReference, "Once upon a time..", "A story");

        // Add an attachment.
        page.openAttachmentsDocExtraPane()
            .setFileToUpload(new File(testConfiguration.getBrowser().getTestResourcesPath(),
                "AttachmentIT/SmallAttachment.txt").getAbsolutePath());

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
        AttachmentsPane attachmentsPane = page.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment("SmallAttachment.txt"));

        // Check the attachment content.
        attachmentsPane.getAttachmentLink("SmallAttachment.txt").click();
        assertEquals("This is a small attachment.", setup.getDriver().findElement(By.tagName("html")).getText());
    }
}
