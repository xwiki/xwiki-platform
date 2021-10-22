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
package org.xwiki.flamingo.skin.test.po;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.DeletePageOutcomePage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Represents the undelete action where a single page or a whole batch of pages can be restored.
 *
 * @version $Id$
 * @since 13.10RC1
 */
public class UndeletePage extends BasePage
{
    /**
     * Name of the page Live Data column.
     */
    public static final String LIVE_DATA_PAGE = "Page";

    /**
     * Name of the location Live Data column.
     */
    public static final String LIVE_DATA_LOCATION = "Location";

    /**
     * Name of the actions Live Data column.
     */
    public static final String LIVE_DATA_ACTIONS = "Actions";

    private static final String DELETER_PREFIX = "Deleted by:\n";

    private static final String DELETED_BATCH_ID_PREFIX = "Deleted Batch ID\n";

    @FindBy(xpath = "//input[@id = 'includeBatch']")
    private List<WebElement> includeBatchCheckBoxes;

    @FindBy(xpath = "//div[@id = 'mainContentArea']/div[@class='xcontent']/form//a[@class = 'panel-collapse-carret']")
    private WebElement panelToggleLink;

    @FindBy(xpath = "//div[@id = 'panel-batch']/div[@class = 'row']/div[@class='col-xs-12 col-lg-4']")
    private WebElement deleter;

    @FindBy(xpath = "//div[@id = 'panel-batch']/div[@class = 'row']/div[last()]")
    private WebElement deletedBatchId;

    @FindBy(xpath = "//div[@id = 'mainContentArea']/div[@class = 'xcontent']/form/a[text() = 'Cancel']")
    private WebElement cancelLink;

    @FindBy(xpath = "//div[@id = 'mainContentArea']/div[@class = 'xcontent']/form/button[text() = 'Restore']")
    private WebElement restoreButton;

    /**
     * @return If there is a batch of pages that can be restored (otherwise it's just a single page).
     */
    public boolean hasBatch()
    {
        return !this.includeBatchCheckBoxes.isEmpty();
    }

    /**
     * @return If the whole batch will be restored.
     */
    public boolean isBatchIncluded()
    {
        return this.includeBatchCheckBoxes.get(0).isSelected();
    }

    /**
     * @param includeBatch Set if the whole batch shall be included when restoring.
     */
    public void setBatchIncluded(boolean includeBatch)
    {
        if (isBatchIncluded() != includeBatch) {
            this.includeBatchCheckBoxes.get(0).click();
        }
    }

    /**
     * Toggles the visibility of the panel with the batch of documents.
     */
    public void toggleBatchPanel()
    {
        this.panelToggleLink.click();
    }

    /**
     * @return The live data element with the deleted pages.
     */
    public LiveDataElement getDeletedBatchLiveData()
    {
        return new LiveDataElement("deletedBatch");
    }

    /**
     * @return The name of the user who deleted the batch.
     */
    public String getPageDeleter()
    {
        return getTextAfterPrefix(DELETER_PREFIX, this.deleter.getText());
    }

    /**
     * @return The id of the deleted batch.
     */
    public String getDeletedBatchId()
    {
        return getTextAfterPrefix(DELETED_BATCH_ID_PREFIX, this.deletedBatchId.getText());
    }

    private String getTextAfterPrefix(String prefix, String fullText)
    {
        assertTrue(fullText.startsWith(prefix));
        return fullText.substring(prefix.length());
    }

    /**
     * @return The page with the result of the deletion.
     */
    public DeletePageOutcomePage clickCancel()
    {
        this.cancelLink.click();
        return new DeletePageOutcomePage();
    }

    /**
     * @return The status page of the restore refactoring job.
     */
    public RestoreStatusPage clickRestore()
    {
        this.restoreButton.click();
        return new RestoreStatusPage();
    }
}
