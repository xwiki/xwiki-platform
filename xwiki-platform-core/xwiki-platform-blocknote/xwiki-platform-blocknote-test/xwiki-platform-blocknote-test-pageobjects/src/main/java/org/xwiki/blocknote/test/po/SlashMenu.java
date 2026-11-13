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
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the drop-down menu that appears when typing a slash in the BlockNote editor.
 *
 * @version $Id$
 * @since 18.6.0
 */
public class SlashMenu extends BaseElement
{
    private static final By MENU = By.className("bn-suggestion-menu");

    private static final By SELECTED_ITEM = By.cssSelector(".bn-suggestion-menu-item[aria-selected=\"true\"]");

    private static final By ITEM_TITLE = By.cssSelector(".bn-suggestion-menu-item .bn-mt-suggestion-menu-item-title");

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
            getDriver().waitUntilElementIsVisible(MENU);
        }

        List<WebElement> selectedItems = getDriver().findElementsWithoutWaiting(SELECTED_ITEM);
        if (!selectedItems.isEmpty()) {
            this.selectedItem = selectedItems.get(0);
        }
    }

    /**
     * Waits for the given menu item to be selected.
     * 
     * @param title the menu item title
     * @return this menu
     */
    public SlashMenu waitForItemSelected(String title)
    {
        this.selectedItem = getDriver()
            .findElement(By.xpath("//div[contains(@class, 'bn-suggestion-menu-item') and @aria-selected='true' and "
                + " .//p[contains(@class, 'bn-mt-suggestion-menu-item-title') and text()='" + title + "']]"));
        return this;
    }

    /**
     * Waits for the menu to show exactly the given items, which is also how you wait for the suggestions of a query
     * to have replaced those of the previous one.
     *
     * @param titles the expected menu item titles, in the order they are displayed
     * @return this menu
     * @since 18.10.0RC1
     */
    public SlashMenu waitForItemTitles(List<String> titles)
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                return titles.equals(getItemTitles());
            } catch (StaleElementReferenceException e) {
                // The menu items are being updated.
                return false;
            }
        });
        return this;
    }

    /**
     * @return the titles of the menu items, in the order they are displayed
     * @since 18.10.0RC1
     */
    public List<String> getItemTitles()
    {
        return getDriver().findElementsWithoutWaiting(ITEM_TITLE).stream().map(WebElement::getText).toList();
    }

    /**
     * Clicks the menu item with the given title and whose subtitle contains the given text, e.g. to pick one of
     * several suggestions with the same title, told apart by their location.
     *
     * @param title the menu item title
     * @param subtitleFragment a fragment of the menu item subtitle
     * @since 18.10.0RC1
     */
    public void clickItem(String title, String subtitleFragment)
    {
        getDriver().findElement(By.xpath("//div[contains(@class, 'bn-suggestion-menu-item') and "
            + ".//p[contains(@class, 'bn-mt-suggestion-menu-item-title') and text()='" + title + "'] and "
            + ".//p[contains(@class, 'bn-mt-suggestion-menu-item-subtitle') and contains(., '" + subtitleFragment
            + "')]]")).click();
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

    /**
     * Waits for the selected menu item to be in view, i.e. inside the visible area of the drop-down menu. Note that
     * {@link WebElement#isDisplayed()} doesn't help here: an item that can be scrolled into view is considered
     * displayed even when the drop-down menu is too small to show it.
     *
     * @return {@code true} if the selected menu item is in view, {@code false} if it remains (partially) out of view
     * @since 18.9.0
     */
    public boolean isSelectedItemInView()
    {
        // The drop-down menu is positioned (and sized) asynchronously.
        try {
            getDriver().waitUntilCondition(driver -> {
                try {
                    Rectangle menu = getDriver().findElement(MENU).getRect();
                    Rectangle item = getDriver().findElement(SELECTED_ITEM).getRect();
                    return item.getY() >= menu.getY()
                        && item.getY() + item.getHeight() <= menu.getY() + menu.getHeight();
                } catch (StaleElementReferenceException e) {
                    // The menu items are being updated.
                    return false;
                }
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
