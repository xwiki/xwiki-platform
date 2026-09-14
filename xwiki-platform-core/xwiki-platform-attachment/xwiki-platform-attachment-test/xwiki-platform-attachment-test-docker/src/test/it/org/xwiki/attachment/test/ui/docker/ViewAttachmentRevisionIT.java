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
import org.xwiki.attachment.test.po.AttachmentRevisionPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentHistoryPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@code viewattachrev} action (com.xpn.xwiki.web.ViewAttachRevAction), which displays the revision
 * history of an attachment.
 *
 * @version $Id$
 */
@UITest
class ViewAttachmentRevisionIT
{
    private static final String ATTACHMENT_FILENAME = "moveme.txt";

    private static final String V0_CONTENT = "Move me (v0).";

    private static final String V1_CONTENT = "Another content with a different size (v1).";

    @Test
    @Order(1)
    void viewNonExistentAttachment(TestUtils setup, TestReference testReference)
    {
        DocumentReference page =
            new DocumentReference("ViewAttachRevMissing", testReference.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        setup.deletePage(page);
        setup.createPage(page, "");

        AttachmentReference attachmentReference = new AttachmentReference(ATTACHMENT_FILENAME, page);

        // No attachment was ever uploaded on that page: the action must report it as missing, not fail.
        assertTrue(AttachmentRevisionPage.viewRevisions(attachmentReference).isAttachmentMissing());
    }

    @Test
    @Order(2)
    void viewDeletedAttachmentHistory(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        DocumentReference page =
            new DocumentReference("ViewAttachRevDeleted", testReference.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        setup.deletePage(page);
        setup.createPage(page, "");

        // Upload two revisions, then delete the attachment so that it lands in the attachment recycle bin, keeping
        // its full revision history.
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(buildMovemePath(testConfiguration, "v0"));
        attachmentsPane.waitForUploadToFinish(ATTACHMENT_FILENAME);
        attachmentsPane.setFileToUpload(buildMovemePath(testConfiguration, "v1"));
        attachmentsPane.waitForUploadToFinish(ATTACHMENT_FILENAME);
        attachmentsPane.deleteAttachmentByFileByName(ATTACHMENT_FILENAME);

        // Retrieve the id under which the deleted attachment is now stored in the attachment recycle bin.
        ViewPage scriptPage = setup.createPage(page, String.format(
            "{{velocity}}$xwiki.getDeletedAttachments($doc.fullName, '%s').get(0).id{{/velocity}}",
            ATTACHMENT_FILENAME));
        long recycleId = Long.parseLong(scriptPage.getContent().trim());

        AttachmentReference attachmentReference = new AttachmentReference(ATTACHMENT_FILENAME, page);

        // The history of a deleted attachment must still be browsable, locating it in the recycle bin through the
        // "rid" parameter, and must still list both revisions with their original content.
        AttachmentHistoryPage historyPage =
            AttachmentRevisionPage.viewDeletedRevisions(attachmentReference, recycleId).getRevisionHistory();
        assertEquals("1.1", historyPage.getVersion(1));
        assertEquals("1.2", historyPage.getVersion(2));
        assertEquals(V0_CONTENT, historyPage.geAttachmentContent(1));
        assertEquals(V1_CONTENT, historyPage.geAttachmentContent(2));

        // Requesting the same deleted attachment through another document's reference must not leak it: the
        // recycle bin id is only valid for the document it was deleted from.
        AttachmentReference wrongDocumentReference = new AttachmentReference(ATTACHMENT_FILENAME,
            new DocumentReference("xwiki", "Main", "WebHome"));
        assertTrue(
            AttachmentRevisionPage.viewDeletedRevisions(wrongDocumentReference, recycleId).isAttachmentMissing());
    }

    @Test
    @Order(3)
    void viewDeletedAttachmentOfProtectedPage(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        DocumentReference page =
            new DocumentReference("ViewAttachRevProtected", testReference.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        setup.deletePage(page);
        setup.createPage(page, "");

        // Upload two revisions, then delete the attachment so that it lands in the attachment recycle bin.
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(buildMovemePath(testConfiguration, "v0"));
        attachmentsPane.waitForUploadToFinish(ATTACHMENT_FILENAME);
        attachmentsPane.setFileToUpload(buildMovemePath(testConfiguration, "v1"));
        attachmentsPane.waitForUploadToFinish(ATTACHMENT_FILENAME);
        attachmentsPane.deleteAttachmentByFileByName(ATTACHMENT_FILENAME);

        // Retrieve the id under which the deleted attachment is now stored in the attachment recycle bin.
        ViewPage scriptPage = setup.createPage(page, String.format(
            "{{velocity}}$xwiki.getDeletedAttachments($doc.fullName, '%s').get(0).id{{/velocity}}",
            ATTACHMENT_FILENAME));
        long recycleId = Long.parseLong(scriptPage.getContent().trim());

        // Create a regular user, then protect the page so that only superadmin can view it.
        String restrictedUser = "ViewAttachRevRestrictedUser";
        String restrictedUserPassword = "ViewAttachRevRestrictedUser";
        setup.createUser(restrictedUser, restrictedUserPassword, null);
        setup.setRights(page, "", "XWiki." + restrictedUser, "view", false);

        AttachmentReference attachmentReference = new AttachmentReference(ATTACHMENT_FILENAME, page);

        // A user without view rights on the page must not be able to browse the history of a deleted attachment
        // that belonged to it, even when providing a valid recycle bin id.
        setup.login(restrictedUser, restrictedUserPassword);
        assertTrue(AttachmentRevisionPage.viewDeletedRevisions(attachmentReference, recycleId).isForbidden());

        AttachmentReference wrongDocumentReference = new AttachmentReference(ATTACHMENT_FILENAME,
            new DocumentReference("xwiki", "Main", "WebHome"));
        assertTrue(
            AttachmentRevisionPage.viewDeletedRevisions(wrongDocumentReference, recycleId).isAttachmentMissing());
    }

    private String buildMovemePath(TestConfiguration testConfiguration, String dirName)
    {
        return new File(new File(testConfiguration.getBrowser().getTestResourcesPath(), dirName),
            ATTACHMENT_FILENAME).getAbsolutePath();
    }
}
