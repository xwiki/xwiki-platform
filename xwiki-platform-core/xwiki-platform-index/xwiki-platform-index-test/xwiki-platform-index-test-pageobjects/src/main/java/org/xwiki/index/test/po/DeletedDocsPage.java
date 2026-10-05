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
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the Deleted Pages page.
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class DeletedDocsPage extends ViewPage
{
    private LiveTableElement documentsTrashLivetable;

    public DeletedDocsPage()
    {
        this.documentsTrashLivetable = new LiveTableElement("documentsTrash");
        this.documentsTrashLivetable.waitUntilReady();
    }

    public static DeletedDocsPage gotoPage()
    {
        getUtil().gotoPage("Main", "AllDocs", "view", "view=deletedDocs");
        return new DeletedDocsPage();
    }

    /**
     * Filters the deleted pages on the "Page" column and waits for the table to be reloaded.
     *
     * @param docName the text to look for in the full name of the deleted pages
     * @since 18.9.0RC1
     */
    public void filterByDocName(String docName)
    {
        this.documentsTrashLivetable.filterColumn("xwiki-livetable-documentsTrash-filter-1", docName);
    }

    public int getRowIndexByDocName(String docName)
    {
        return this.documentsTrashLivetable.getRowNumberForElement(By.xpath("//a[text()='" + docName + "']"));
    }

    private WebElement getReplaceAction(String docName)
    {
        return this.documentsTrashLivetable.getCell(getRowIndexByDocName(docName), 6)
            .findElement(By.className("replace"));
    }

    /**
     * Restores a deleted page that has no page at its original location anymore, using the "Restore" action of its
     * row.
     *
     * @param docName the full name of the deleted page, as displayed in the "Page" column
     * @return the restored page, to which the restore action redirects
     * @since 18.9.0RC1
     */
    public ViewPage restoreDoc(String docName)
    {
        WebElement restoreAction = this.documentsTrashLivetable.getCell(getRowIndexByDocName(docName), 6)
            .findElement(By.className("restore"));
        getDriver().addPageNotYetReloadedMarker();
        restoreAction.click();
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }

    /**
     * @param docName the full name of a deleted page, as displayed in the "Page" column
     * @return {@code true} if the deleted page is listed in the currently displayed rows, {@code false} otherwise
     * @since 18.9.0RC1
     */
    public boolean hasDoc(String docName)
    {
        return this.documentsTrashLivetable.hasRow("Page", docName);
    }

    public RestoreDocumentConfirmationModal tryReplaceDoc(String docName)
    {
        getReplaceAction(docName).click();
        RestoreDocumentConfirmationModal restoreModal = new RestoreDocumentConfirmationModal();
        return restoreModal;
    }
}
