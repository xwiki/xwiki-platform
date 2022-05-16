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

import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the Deleted Pages page.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class DeletedDocsPage extends ViewPage
{
    private LiveTableElement documentsTrashLivetable;

    DeletedDocsPage()
    {
        this.documentsTrashLivetable = new LiveTableElement("documentsTrash");
        waitForDeletedDocumentsLivetable();
    }

    public static DeletedDocsPage gotoPage()
    {
        getUtil().gotoPage("Main", "AllDocs", "view", "view=deletedDocs");
        return new DeletedDocsPage();
    }

    /**
     * Wait for the deleted documents livetable to be ready.
     */
    public void waitForDeletedDocumentsLivetable()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return documentsTrashLivetable.isReady();
            }
        });
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

    public Optional<RestoreDocumentConfirmationModal> tryReplaceDoc(String docName)
    {
        try {
            getReplaceAction(docName).click();
            RestoreDocumentConfirmationModal restoreModal = new RestoreDocumentConfirmationModal();
            return Optional.of(restoreModal);
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }

}
