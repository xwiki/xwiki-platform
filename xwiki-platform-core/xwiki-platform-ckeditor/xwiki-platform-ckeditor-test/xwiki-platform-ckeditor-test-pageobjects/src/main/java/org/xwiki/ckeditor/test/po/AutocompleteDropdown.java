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
package org.xwiki.ckeditor.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models an auto-complete drop-down.
 *
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 */
@Unstable
public class AutocompleteDropdown extends BaseElement
{
    /**
     * Describes an item from the auto-complete drop-down.
     */
    public class Item extends BaseElement
    {
        private WebElement container;

        public Item(WebElement container)
        {
            this.container = container;
        }

        public String getLabel()
        {
            return getDriver().findElementWithoutWaiting(this.container,
                By.cssSelector(".ckeditor-autocomplete-item-label, :scope > span:first-child")).getText();
        }

        public String getHint()
        {
            return getDriver()
                .findElementWithoutWaiting(this.container, By.className("ckeditor-autocomplete-item-hint")).getText();
        }

        public String getShortcut()
        {
            return getDriver()
                .findElementWithoutWaiting(this.container, By.className("ckeditor-autocomplete-item-shortcut"))
                .getText();
        }
        
        /**
         * Clicks on the item.
         * @since 15.7RC1
        */
        public void click() {
            container.click();
        }

        /**
         * @return the list of badges of an item.
         * @since 15.7RC1
         */
        public Iterable<String> getBadges() {
            Iterable<WebElement> badgeElements = getDriver().findElementsWithoutWaiting(this.container, 
                    By.className("ckeditor-autocomplete-item-badge"));
            
            ArrayList<String> badges = new ArrayList<>();
            for (WebElement badge : badgeElements) {
                badges.add(badge.getText());
            }
            
            return badges;
        }
    }

    /**
     * We keep a reference to the previously selected item in order to detect when the items are updated.
     */
    private WebElement selectedItem;

    /**
     * Default constructor.
     */
    public AutocompleteDropdown()
    {
        this(true);
    }

    /**
     * Creates a new instance, optionally waiting for the drop down to be visible.
     * 
     * @param wait whether to wait for the auto-complete drop down to be visible or not
     */
    public AutocompleteDropdown(boolean wait)
    {
        if (wait) {
            getDriver().waitUntilElementIsVisible(By.className("cke_autocomplete_opened"));
        }

        List<WebElement> selectedItems = getDriver()
            .findElementsWithoutWaiting(By.cssSelector(".cke_autocomplete_opened .cke_autocomplete_selected"));
        if (!selectedItems.isEmpty()) {
            this.selectedItem = selectedItems.get(0);
        }
    }

    /**
     * @return the selected item
     */
    public Item getSelectedItem()
    {
        return new Item(this.selectedItem);
    }

    /**
     * Waits for the given item to be selected.
     * 
     * @param query the auto-complete query for which the item is selected
     * @param label the name of the item
     */
    public AutocompleteDropdown waitForItemSelected(String query, String label)
    {
        // We don't use an XPath selector because the given query and label can contain quotes or apostrophes which
        // can't be escaped in a generic way because the Web browsers are supporting only XPath 1.0 (double
        // quote/apostrophe encoding was introduced in XPath 2.0).
        this.selectedItem = getDriver().waitUntilCondition(driver -> {
            try {
                // First check if the auto-complete drop down is opened and matches the given query.
                WebElement dropdown = getDriver().findElementWithoutWaiting(By.className("cke_autocomplete_opened"));
                if (query.equals(dropdown.getDomAttribute("data-query"))) {
                    // Then check if the selected item matches the given label.
                    WebElement selectedItem =
                        getDriver().findElementWithoutWaiting(dropdown, By.className("cke_autocomplete_selected"));
                    if (selectedItem.isDisplayed() && selectedItem.getText().contains(label)) {
                        return selectedItem;
                    }
                }
            } catch (Exception e) {
                // Try again.
            }
            return null;
        });

        return this;
    }

    /**
     * Waits for the auto-complete drop-down to be updated as a result of an item being submitted. Note that we don't
     * wait for the drop down to disappear because submitting an item can open a new drop down (e.g. the link quick
     * action opens the link auto-complete drop down).
     */
    public void waitForItemSubmitted()
    {
        if (this.selectedItem != null) {
            // Wait for the previously selected item to be hidden, unselected or detached from the DOM.
            getDriver().waitUntilCondition((driver) -> {
                try {
                    // Either hidden (e.g. when the drop down is closed)
                    return !this.selectedItem.isDisplayed()
                        // or not selected,
                        || !this.selectedItem.getAttribute("class").contains("cke_autocomplete_selected");
                } catch (StaleElementReferenceException e) {
                    // or detached from the DOM (e.g. when the items have been updated).
                    return true;
                }
            });
        }
    }
}
