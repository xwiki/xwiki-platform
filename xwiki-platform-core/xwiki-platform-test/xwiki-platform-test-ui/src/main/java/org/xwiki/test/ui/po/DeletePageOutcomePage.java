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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible after a page has been deleted.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class DeletePageOutcomePage extends ViewPage
{
    @FindBy(xpath = "//p[@class='xwikimessage']")
    private WebElement message;

    @FindBy(xpath = "//table[@class='centered']/tbody/tr/td[3]/a")
    private WebElement batchLink;

    /**
     * Return first page deleted deleter.
     * @since 3.2M3
     * @deprecated Since 14.10 prefer using {@link #getDeletedPagesEntries()} and
     *             {@link DeletedPageEntry#getDeleter()}.
     */
    @Deprecated(since = "14.10")
    public String getPageDeleter()
    {
        List<DeletedPageEntry> deletedPagesEntries = getDeletedPagesEntries();
        if (deletedPagesEntries == null) {
            return "";
        } else {
            return deletedPagesEntries.get(0).getDeleter();
        }
    }

    /**
     * @since 4.0M2
     */
    public String getMessage()
    {
        return this.message.getText();
    }

    private List<DeletedPageEntry> getDeletedEntries(WebElement table, boolean isTerminal)
    {
        List<DeletedPageEntry> result = new ArrayList<>();
        List<WebElement> entries = getDriver().findElementsWithoutWaiting(table, By.tagName("tr"));
        // We ignore the first entry since it's the table header row.
        if (entries.size() > 1) {
            for (int i = 1; i < entries.size(); i++) {
                result.add(new DeletedPageEntry(i, isTerminal, entries.get(i)));
            }
        }

        return result;
    }

    /**
     * @return the list of non-terminal deleted page entries.
     * @since 14.10
     */
    public List<DeletedPageEntry> getDeletedPagesEntries()
    {
        try {
            WebElement table =
                getDriver().findElementWithoutWaiting(By.cssSelector("div.xwikimessage div.docs table"));
            return getDeletedEntries(table, false);
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    /**
     * @return the list of terminal deleted page entries.
     */
    public List<DeletedPageEntry> getDeletedTerminalPagesEntries()
    {
        try {
            WebElement table =
                getDriver().findElementWithoutWaiting(By.cssSelector("div.xwikimessage div.terminal-docs table"));
            return getDeletedEntries(table, true);
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Clicks on the first link to restore the deleted page from the recycle bin.
     * 
     * @return the restored view page
     * @since 5.2M2
     * @deprecated Since 14.10 prefer using {@link #getDeletedPagesEntries()} and
     *              {@link DeletedPageEntry#clickRestore()}.
     */
    @Deprecated(since = "14.10")
    public ViewPage clickRestore()
    {
        List<DeletedPageEntry> deletedPagesEntries = getDeletedPagesEntries();
        if (deletedPagesEntries == null) {
            throw new RuntimeException("There's no deleted entries.");
        } else {
            return deletedPagesEntries.get(0).clickRestore();
        }
    }

    /**  
     * @return if there are terminal pages in the recycle bin
     * 
     * @since 7.2RC1 
     */
    public boolean hasTerminalPagesInRecycleBin()
    {
        return !getDeletedTerminalPagesEntries().isEmpty();
    }

    /**
     * Clicks on the link to permanently delete the deleted page from the recycle bin.
     *
     * @return the recycle bin page updated
     * @since 7.2RC1
     * @deprecated Since 14.10 prefer using {@link #getDeletedPagesEntries()} and
     *             {@link DeletedPageEntry#clickDelete()}.
     */
    @Deprecated(since = "14.10")
    public DeletePageOutcomePage clickDeletePage()
    {
        List<DeletedPageEntry> deletedPagesEntries = getDeletedPagesEntries();
        if (deletedPagesEntries == null) {
            throw new RuntimeException("There's no deleted entries.");
        } else {
            return deletedPagesEntries.get(0).clickDelete();
        }
    }

    /**
     * Clicks on the link to restore the deleted terminal page from the recycle bin.
     *
     * @return the restored view page
     * @since 7.2RC1
     * @deprecated Since 14.10 prefer using {@link #getDeletedTerminalPagesEntries()} and
     *             {@link DeletedPageEntry#clickDelete()}.
     */
    @Deprecated(since = "14.10")
    public DeletePageOutcomePage clickDeleteTerminalPage()
    {
        List<DeletedPageEntry> deletedPagesEntries = getDeletedTerminalPagesEntries();
        if (deletedPagesEntries == null) {
            throw new RuntimeException("There's no deleted entries.");
        } else {
            return deletedPagesEntries.get(0).clickDelete();
        }
    }

    /**
     * Click on the link to view the deleted page.
     *
     * @param row the revision row for which to view the deleted page, starting at 1
     * @return the view page of the deleted page
     * @since 13.10.4
     * @since 14.2RC1
     * @deprecated Since 14.10 prefer using {@link #getDeletedPagesEntries()} and
     *            {@link DeletedPageEntry#clickDelete()}.
     */
    @Deprecated(since = "14.10")
    public ViewPage clickViewDocument(int row)
    {
        int actualEntryNumber = row - 1;
        List<DeletedPageEntry> deletedPagesEntries = getDeletedPagesEntries();
        if (deletedPagesEntries == null || actualEntryNumber >= deletedPagesEntries.size()) {
            throw new RuntimeException(String.format(
                "Requested row is [%s] while number of deleted entries is [%s].", row, deletedPagesEntries.size()));
        } else {
            return deletedPagesEntries.get(actualEntryNumber).clickView();
        }
    }

    /**
     * Clicks on the batch link. Goes to the undelete action.
     *
     * @since 15.8RC1
     */
    public void clickBatchLink()
    {
        this.batchLink.click();
    }
}
