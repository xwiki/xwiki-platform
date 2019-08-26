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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify versioning features of documents and attachments.
 * 
 * @version $Id$
 */
public class VersionTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private static final String PAGE_NAME = "HistoryTest";

    private static final String SPACE_NAME = "HistorySpaceTest";

    private static final LocalDocumentReference REFERENCE = new LocalDocumentReference(SPACE_NAME, PAGE_NAME);

    private static final String TITLE = "Page Title";

    private static final String CONTENT1 = "First version of Content";

    private static final String CONTENT2 = "Second version of Content";

    @Test
    public void testRollbackToFirstVersion() throws Exception
    {
        getUtil().rest().deletePage(SPACE_NAME, PAGE_NAME);

        // Create first version of the page
        ViewPage vp = getUtil().createPage(SPACE_NAME, PAGE_NAME, CONTENT1, TITLE);

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

        Assert.assertEquals("First version of Content", vp.getContent());

        historyTab = vp.openHistoryDocExtraPane();
        Assert.assertEquals("Rollback to version 1.1", historyTab.getCurrentVersionComment());
        Assert.assertEquals("superadmin", historyTab.getCurrentAuthor());
    }

    /**
     * See XWIKI-8781
     */
    @Test
    public void testDeleteLatestVersion() throws Exception
    {
        getUtil().rest().deletePage(SPACE_NAME, PAGE_NAME);

        // Create first version of the page
        ViewPage vp = getUtil().createPage(SPACE_NAME, PAGE_NAME, CONTENT1, TITLE);

        // Adds second version
        WikiEditPage wikiEditPage = vp.editWiki();
        wikiEditPage.setContent(CONTENT2);
        wikiEditPage.clickSaveAndView();

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        // Verify and delete the latest version.
        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        Assert.assertEquals("2.1", historyTab.getCurrentVersion());
        historyTab = historyTab.deleteVersion("2.1");

        // Verify that the current version is now the previous one.
        Assert.assertEquals("1.1", historyTab.getCurrentVersion());
        Assert.assertEquals("superadmin", historyTab.getCurrentAuthor());
    }

    @Test
    public void rollbackAttachments() throws Exception
    {
        getUtil().rest().deletePage(SPACE_NAME, PAGE_NAME);

        Page page = new Page();
        page.setSpace(SPACE_NAME);
        page.setName(PAGE_NAME);

        // Create empty page
        getUtil().rest().save(page);

        // Add attachment
        EntityReference attachmentReference = new EntityReference("file.txt", EntityType.ATTACHMENT, REFERENCE);
        getUtil().rest().attachFile(attachmentReference, "attachment1".getBytes(), true);

        // Add a second version of the attachment
        getUtil().rest().attachFile(attachmentReference, "attachment2".getBytes(), false);

        // Load page
        ViewPage vp = getUtil().gotoPage(REFERENCE);

        // Make sure expected attachment is there
        AttachmentsPane attachmentsPane = vp.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));

        // Revert to 1.1 (empty page)
        vp = vp.openHistoryDocExtraPane().rollbackToVersion("1.1");

        attachmentsPane = vp.openAttachmentsDocExtraPane();
        assertEquals(0, attachmentsPane.getNumberOfAttachments());

        // Revert to 3.1 (second update of the attachment)
        vp = vp.openHistoryDocExtraPane().rollbackToVersion("3.1");

        attachmentsPane = vp.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.2", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));
        attachmentsPane.getAttachmentLink(attachmentReference.getName()).click();
        Assert.assertEquals("attachment2", getDriver().findElement(By.tagName("html")).getText());

        // Revert to 2.1 (first update of the attachment)
        vp = getUtil().gotoPage(REFERENCE).openHistoryDocExtraPane().rollbackToVersion("2.1");

        attachmentsPane = vp.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.3", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));
        attachmentsPane.getAttachmentLink(attachmentReference.getName()).click();
        Assert.assertEquals("attachment1", getDriver().findElement(By.tagName("html")).getText());

        // Back to empty page again
        vp = getUtil().gotoPage(REFERENCE).openHistoryDocExtraPane().rollbackToVersion("1.1");

        attachmentsPane = vp.openAttachmentsDocExtraPane();
        assertEquals(0, attachmentsPane.getNumberOfAttachments());

        // Revert to 2.1 (first update of the attachment)
        vp = vp.openHistoryDocExtraPane().rollbackToVersion("2.1");

        attachmentsPane = vp.openAttachmentsDocExtraPane();
        assertEquals(1, attachmentsPane.getNumberOfAttachments());
        assertEquals("1.3", attachmentsPane.getLatestVersionOfAttachment(attachmentReference.getName()));
        attachmentsPane.getAttachmentLink(attachmentReference.getName()).click();
        Assert.assertEquals("attachment1", getDriver().findElement(By.tagName("html")).getText());
    }
}
