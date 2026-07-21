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
package org.xwiki.blocknote.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the drop-down menu that appears when typing a slash in the BlockNote editor.
 *
 * @version $Id$
 * @since 18.6.0
 */
@Unstable
public class SlashMenu extends BaseElement
{
    /**
     * We keep a reference to the previously selected item in order to detect when the items are updated.
     */
    private WebElement selectedItem;

    /**
     * Default constructor.
     */
    public SlashMenu()
    {
        this(true);
    }

    /**
     * Creates a new instance, optionally waiting for the drop down menu to be visible.
     * 
     * @param wait whether to wait for the drop down menu to be visible or not
     */
    public SlashMenu(boolean wait)
    {
        if (wait) {
            getDriver().waitUntilElementIsVisible(By.className("bn-suggestion-menu"));
        }

        List<WebElement> selectedItems =
            getDriver().findElementsWithoutWaiting(By.cssSelector(".bn-suggestion-menu-item[aria-selected=\"true\"]"));
        if (!selectedItems.isEmpty()) {
            this.selectedItem = selectedItems.get(0);
        }
    }

    /**
     * Waits for the given menu item to be selected.
     * 
     * @param title the menu item title
     */
    public SlashMenu waitForItemSelected(String title)
    {
        this.selectedItem = getDriver()
            .findElement(By.xpath("//div[contains(@class, 'bn-suggestion-menu-item') and @aria-selected='true' and "
                + " .//p[contains(@class, 'bn-mt-suggestion-menu-item-title') and text()='" + title + "']]"));
        return this;
    }

    /**
     * Waits for the drop-down menu to be updated as a result of an item being submitted. Note that we don't wait for
     * the drop down to disappear because submitting an item can open a new drop down menu (e.g. the link quick action
     * opens the link auto-complete drop down).
     */
    public void waitForItemSubmitted()
    {
        if (this.selectedItem != null) {
            // Wait for the previously selected item to be hidden, unselected or detached from the DOM.
            getDriver().waitUntilCondition(driver -> {
                try {
                    // Either hidden (e.g. when the drop down is closed)
                    return !this.selectedItem.isDisplayed()
                        // or not selected,
                        || !"true".equals(this.selectedItem.getDomAttribute("aria-selected"));
                } catch (StaleElementReferenceException e) {
                    // or detached from the DOM (e.g. when the items have been updated).
                    return true;
                }
            });
        }
    }
}
