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
package org.xwiki.attachment.test.po;

import org.openqa.selenium.By;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.test.ui.po.AttachmentHistoryPage;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object for the revisions of an attachment: the content served by the {@code downloadrev} action for a given
 * revision, and the revision history displayed by the {@code viewattachrev} action. Both actions also accept the
 * identifier under which a deleted attachment is stored in the attachment recycle bin, so that the revisions of a
 * deleted attachment remain reachable.
 * <p>
 * The {@code downloadrev} action serves the raw content of the attachment, which is not an XWiki page: this page
 * object doesn't extend {@link ViewPage} so that reaching such content doesn't wait for a page to become ready.
 *
 * @version $Id$
 * @since 18.8.0RC1
 * @since 18.4.6
 * @since 17.10.14
 * @since 16.10.19
 */
public class AttachmentRevisionPage extends BaseElement
{
    private static final String DOWNLOAD_REVISION_ACTION = "downloadrev";

    private static final String VIEW_REVISIONS_ACTION = "viewattachrev";

    private static final String ATTACHMENT_DOES_NOT_EXIST_MESSAGE = "The attachment does not exist.";

    private static final By REVISIONS_TABLE = By.id("attachment-revisions");

    /**
     * Download a revision of an attachment.
     *
     * @param attachmentReference the reference of the attachment to download
     * @param revision the revision to download (for instance, "1.1")
     * @return the page object of the resulting page
     */
    public static AttachmentRevisionPage downloadRevision(AttachmentReference attachmentReference, String revision)
    {
        return gotoRevisionPage(attachmentReference, DOWNLOAD_REVISION_ACTION, "rev=" + revision);
    }

    /**
     * Download a revision of a deleted attachment, looked up in the attachment recycle bin.
     *
     * @param attachmentReference the reference the attachment had before being deleted
     * @param recycleBinId the identifier of the attachment in the attachment recycle bin
     * @param revision the revision to download (for instance, "1.1")
     * @return the page object of the resulting page
     */
    public static AttachmentRevisionPage downloadDeletedRevision(AttachmentReference attachmentReference,
        long recycleBinId, String revision)
    {
        return gotoRevisionPage(attachmentReference, DOWNLOAD_REVISION_ACTION,
            String.format("rid=%d&rev=%s", recycleBinId, revision));
    }

    /**
     * Display the revision history of an attachment.
     *
     * @param attachmentReference the reference of the attachment whose history to display
     * @return the page object of the resulting page
     */
    public static AttachmentRevisionPage viewRevisions(AttachmentReference attachmentReference)
    {
        return gotoRevisionPage(attachmentReference, VIEW_REVISIONS_ACTION, "");
    }

    /**
     * Display the revision history of a deleted attachment, looked up in the attachment recycle bin.
     *
     * @param attachmentReference the reference the attachment had before being deleted
     * @param recycleBinId the identifier of the attachment in the attachment recycle bin
     * @return the page object of the resulting page
     */
    public static AttachmentRevisionPage viewDeletedRevisions(AttachmentReference attachmentReference,
        long recycleBinId)
    {
        return gotoRevisionPage(attachmentReference, VIEW_REVISIONS_ACTION, "rid=" + recycleBinId);
    }

    /**
     * @return the content served for the downloaded revision
     */
    public String getDownloadedContent()
    {
        return getDriver().findElementWithoutWaiting(By.xpath("/*")).getText();
    }

    /**
     * @return {@code true} when the page reports that the requested attachment doesn't exist
     */
    public boolean isAttachmentMissing()
    {
        return getDriver().hasElementWithoutWaiting(By.xpath(
            String.format("//*[contains(@class, 'xwikimessage')][contains(., '%s')]",
                ATTACHMENT_DOES_NOT_EXIST_MESSAGE)));
    }

    /**
     * @return {@code true} when the page reports that the current user isn't allowed to access the requested revision
     */
    public boolean isForbidden()
    {
        return new ViewPage().isForbidden();
    }

    /**
     * @return {@code true} when the table of the available revisions is displayed, which is also where the
     *     {@code downloadrev} action redirects when the requested revision can't be found
     */
    public boolean isRevisionHistoryDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(REVISIONS_TABLE);
    }

    /**
     * @return the page object of the displayed revision history
     */
    public AttachmentHistoryPage getRevisionHistory()
    {
        return new AttachmentHistoryPage();
    }

    private static AttachmentRevisionPage gotoRevisionPage(AttachmentReference attachmentReference, String action,
        String queryString)
    {
        getUtil().gotoPage(attachmentReference, action, queryString);
        return new AttachmentRevisionPage();
    }
}
