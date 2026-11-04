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
     * @return if there are terminal pages in the recycle bin
     * 
     * @since 7.2RC1 
     */
    public boolean hasTerminalPagesInRecycleBin()
    {
        return !getDeletedTerminalPagesEntries().isEmpty();
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
