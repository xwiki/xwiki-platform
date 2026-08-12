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
import org.openqa.selenium.By;
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
 * Tests of the move attachment feature.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@UITest
class MoveAttachmentIT
{
    private static final String SOURCE_FILENAME = "moveme.txt";

    private static final String TARGET_FILENAME = "newname.txt";

    @Test
    @Order(1)
    void moveAttachment(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        DocumentReference sourcePage = new DocumentReference("Source", testReference.getLastSpaceReference());
        DocumentReference targetPage = new DocumentReference("Target", testReference.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        setup.deletePage(sourcePage);
        setup.deletePage(targetPage);

        // Create the pages and upload a document with a first user U1.
        setup.createUserAndLogin("U1", "pU1");
        setup.createPage(sourcePage, "");
        AttachmentReference sourceAttachmentReference = new AttachmentReference(SOURCE_FILENAME, sourcePage);
        String serializedSourceAttachmentReference = setup.serializeReference(sourceAttachmentReference);
        setup.createPage(targetPage, String.format("[[attach:%s]]", serializedSourceAttachmentReference));

        setup.gotoPage(sourcePage);
        AttachmentsPane sourceAttachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        sourceAttachmentsPane.setFileToUpload(buildMovemePath(testConfiguration, "v0"));
        sourceAttachmentsPane.waitForUploadToFinish(SOURCE_FILENAME);
        sourceAttachmentsPane.setFileToUpload(buildMovemePath(testConfiguration, "v1"));
        sourceAttachmentsPane.waitForUploadToFinish(SOURCE_FILENAME);

        // Switch to a second user U2 and come back to the attachment pane of the source page.
        setup.createUserAndLogin("U2", "pU2");
        setup.gotoPage(sourcePage);
        new AttachmentsViewPage().openAttachmentsDocExtraPane();

        AttachmentPane attachmentsPane = AttachmentPane.moveAttachment(SOURCE_FILENAME);

        attachmentsPane.setName(TARGET_FILENAME);
        attachmentsPane.setRedirect(true);
        attachmentsPane.setLocation(setup.serializeReference(targetPage));
        attachmentsPane.submit();
        attachmentsPane.waitForJobDone();

        ViewPage viewTargetPage = setup.gotoPage(targetPage);

        // Validate the history pane first because we'll move to the attachment history page when validating the
        // attachment pane.
        // The document was modified by the move (attachment added and content refactored), so its current author is
        // the user who performed the move (U2), not the original attachment author (U1).
        assertEquals("U2", viewTargetPage.openHistoryDocExtraPane().getCurrentAuthor());

        // Validate the attachments pane.
        AttachmentsPane attachmentsPaneTarget = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        WebElement attachmentLink = attachmentsPaneTarget.getAttachmentLink(TARGET_FILENAME);
        assertNotNull(attachmentLink);
        // Verify that the author is correct in the attachments pane.
        assertEquals("U1", attachmentsPaneTarget.getUploaderOfAttachment(TARGET_FILENAME));
        assertEquals("1.2", attachmentsPaneTarget.getLatestVersionOfAttachment(TARGET_FILENAME));
        // Validate that the attachment history is still right after the move.
        AttachmentHistoryPage attachmentHistoryPage = attachmentsPaneTarget.goToAttachmentHistory(TARGET_FILENAME);
        assertEquals("1.1", attachmentHistoryPage.getVersion(1));
        assertEquals("1.2", attachmentHistoryPage.getVersion(2));
        assertEquals(13, attachmentHistoryPage.getSize(1));
        assertEquals(44, attachmentHistoryPage.getSize(2));
        assertEquals("U1", attachmentHistoryPage.getAuthor(1));
        assertEquals("U1", attachmentHistoryPage.getAuthor(2));
        assertEquals("Move me (v0).", attachmentHistoryPage.geAttachmentContent(1));
        assertEquals("Another content with a different size (v1).", attachmentHistoryPage.geAttachmentContent(2));

        // Verify that the redirection object has been created on the source page.
        Object object = setup.rest().object(sourcePage, setup.serializeReference(REFERENCE));
        assertNotNull(object);

        // Verify that the refactoring is properly applied.
        assertEquals("[[attach:newname.txt]]", setup.rest().<Page>get(targetPage).getContent());
    }

    @Test
    @Order(2)
    void renameAttachmentInSameDocument(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        DocumentReference page = new DocumentReference("RenameSameDocument", testReference.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        setup.deletePage(page);

        // Create the page and upload the attachment with a first user U3, so that the rename below is performed by
        // another user and we can tell the attachment author apart from the document author.
        setup.createUserAndLogin("U3", "pU3");
        setup.createPage(page, "");
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(buildMovemePath(testConfiguration, "v0"));
        attachmentsPane.waitForUploadToFinish(SOURCE_FILENAME);

        // Switch to a second user U4 and come back to the attachment pane of the page.
        setup.createUserAndLogin("U4", "pU4");
        setup.gotoPage(page);
        new AttachmentsViewPage().openAttachmentsDocExtraPane();

        // Rename the attachment without changing its location, i.e. move it inside the document it is already in.
        AttachmentPane movePane = AttachmentPane.moveAttachment(SOURCE_FILENAME);
        movePane.setName(TARGET_FILENAME);
        movePane.setRedirect(true);
        movePane.submit();
        movePane.waitForJobDone();

        ViewPage viewPage = setup.gotoPage(page);

        // Validate the history pane first because we'll move to the attachment history page when validating the
        // attachment pane.
        // The document was modified by the rename, so its current author is the user who performed it (U4), not the
        // attachment author (U3).
        assertEquals("U4", viewPage.openHistoryDocExtraPane().getCurrentAuthor());

        // The page must hold exactly one attachment, the renamed one, i.e. the attachment must not be lost by the
        // rename.
        AttachmentsPane attachmentsPaneAfterRename = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPaneAfterRename.getNumberOfAttachments());
        assertEquals(TARGET_FILENAME, attachmentsPaneAfterRename.getAttachmentNameByIndex(1));
        // Verify that the rename preserves the attachment author and version.
        assertEquals("U3", attachmentsPaneAfterRename.getUploaderOfAttachment(TARGET_FILENAME));
        assertEquals("1.1", attachmentsPaneAfterRename.getLatestVersionOfAttachment(TARGET_FILENAME));

        // Validate that the attachment history is still right after the rename.
        AttachmentHistoryPage attachmentHistoryPage = attachmentsPaneAfterRename.goToAttachmentHistory(TARGET_FILENAME);
        assertEquals("1.1", attachmentHistoryPage.getVersion(1));
        assertEquals(13, attachmentHistoryPage.getSize(1));
        assertEquals("U3", attachmentHistoryPage.getAuthor(1));
        assertEquals("Move me (v0).", attachmentHistoryPage.geAttachmentContent(1));

        // Verify that the redirection object has been created on the page.
        Object object = setup.rest().object(page, setup.serializeReference(REFERENCE));
        assertNotNull(object);

        // Verify that the redirection is effective and not just recorded: downloading the attachment under its old
        // name must serve the renamed attachment.
        setup.getDriver().get(setup.getURL(new AttachmentReference(SOURCE_FILENAME, page), "download", ""));
        assertEquals("Move me (v0).", setup.getDriver().findElementWithoutWaiting(By.xpath("/*")).getText());
    }

    private String buildMovemePath(TestConfiguration testConfiguration, String dirName)
    {
        return new File(new File(testConfiguration.getBrowser().getTestResourcesPath(), dirName), SOURCE_FILENAME)
            .getAbsolutePath();
    }
}
