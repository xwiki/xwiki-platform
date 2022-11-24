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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents a deleted page row in the recycle bin.
 *
 * @version $Id$
 * @since 14.10
 */
public class DeletedPageEntry extends BaseElement
{
    private static final String ACTION_RESTORE_CSS_SELECTOR = "a.action-restore";

    private static final String ACTION_DELETE_CSS_SELECTOR = "a.action-delete";

    private static final String ACTION_VIEW_CSS_SELECTOR = "td:nth-child(2) a";

    private final boolean isTerminal;
    private final int rowNumber;

    private final WebElement row;

    /**
     * Default constructor.
     *
     * @param rowNumber the row number in the table.
     * @param isTerminal {@code true} if the page was terminal (so the entry comes from terminal pages table),
     *                   {@code false} otherwise.
     * @param row the actual web element entry
     */
    public DeletedPageEntry(int rowNumber, boolean isTerminal, WebElement row)
    {
        this.rowNumber = rowNumber;
        this.isTerminal = isTerminal;
        this.row = row;
    }

    private WebElement getRow()
    {
        return this.row;
    }

    /**
     * @return the name of the deleter.
     */
    public String getDeleter()
    {
        return getDriver().findElementWithoutWaiting(getRow(), By.cssSelector("td:nth-child(1)")).getText();
    }

    /**
     * @return {@code true} if the restore link action is available.
     */
    public boolean canBeRestored()
    {
        return getDriver().hasElementWithoutWaiting(getRow(), By.cssSelector(ACTION_RESTORE_CSS_SELECTOR));
    }

    /**
     * @return {@code true} if the delete link action is available.
     */
    public boolean canBeDeleted()
    {
        return getDriver().hasElementWithoutWaiting(getRow(), By.cssSelector(ACTION_DELETE_CSS_SELECTOR));
    }

    /**
     * @return {@code true} if the view link is available.
     */
    public boolean canBeViewed()
    {
        return getDriver().hasElementWithoutWaiting(getRow(), By.cssSelector(ACTION_VIEW_CSS_SELECTOR));
    }

    /**
     * Click on the view link.
     *
     * @return the view page of the deleted page.
     */
    public ViewPage clickView()
    {
        getDriver().findElementWithoutWaiting(getRow(), By.cssSelector(ACTION_VIEW_CSS_SELECTOR)).click();
        ViewPage viewPage = new ViewPage();
        viewPage.waitUntilPageIsReady();
        return viewPage;
    }

    /**
     * Click on the permanently delete action and bypass the confirm dialog.
     *
     * @return the {@link DeletePageOutcomePage} updated.
     */
    public DeletePageOutcomePage clickDelete()
    {
        getDriver().makeConfirmDialogSilent(true);
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElementWithoutWaiting(getRow(), By.cssSelector(ACTION_DELETE_CSS_SELECTOR)).click();
        getDriver().waitUntilPageIsReloaded();
        return new DeletePageOutcomePage();
    }

    /**
     * Click on the restore action.
     *
     * @return the view page of the restored page.
     */
    public ViewPage clickRestore()
    {
        getDriver().findElementWithoutWaiting(getRow(), By.cssSelector(ACTION_RESTORE_CSS_SELECTOR)).click();
        ViewPage viewPage = new ViewPage();
        viewPage.waitUntilPageIsReady();
        return viewPage;
    }
}
