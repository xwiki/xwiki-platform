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
package org.xwiki.index.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ConfirmationBox;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the DeletedAttachments Page.
 *
 * @since 12.2
 */
public class DeletedAttachmentsPage extends ViewPage
{
    public static DeletedAttachmentsPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "DeletedAttachments");
        return new DeletedAttachmentsPage();
    }

    public LiveTableElement getDeletedAttachmentsLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("attachmentTrash");
        lt.waitUntilReady();
        return lt;
    }

    /**
     * Filters the deleted attachments on the "Page" column and waits for the table to be reloaded.
     *
     * @param location the text to look for in the location of the page the attachments were deleted from
     * @since 18.9.0RC1
     */
    public void filterByLocation(String location)
    {
        getDeletedAttachmentsLiveTable().filterColumn("xwiki-livetable-attachmentTrash-filter-2", location);
    }

    /**
     * Filters the deleted attachments on the "Deleted by" column and waits for the table to be reloaded.
     *
     * @param deleter the text to look for in the name of the user who deleted the attachments
     * @since 18.9.0RC1
     */
    public void filterByDeleter(String deleter)
    {
        getDeletedAttachmentsLiveTable().filterColumn("xwiki-livetable-attachmentTrash-filter-4", deleter);
    }

    /**
     * Permanently deletes a deleted attachment, using the delete action of its row and confirming, and waits for its
     * row to be removed from the table.
     *
     * @param fileName the name of the deleted attachment, as displayed in the "Attachment" column; its row must be
     *     displayed
     * @since 18.9.0RC1
     */
    public void deletePermanently(String fileName)
    {
        LiveTableElement liveTable = getDeletedAttachmentsLiveTable();
        By row = By.xpath(String.format("//tbody[@id = 'attachmentTrash-display']/tr[td[1]/a[. = '%s']]", fileName));
        getDriver().findElement(row).findElement(By.cssSelector("td.itemActions a.delete")).click();
        new ConfirmationBox().clickYes();
        getDriver().waitUntilElementDisappears(row);
        liveTable.waitUntilReady();
    }
}
