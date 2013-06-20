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
package org.xwiki.wysiwyg.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the menu bar of the WYSIWYG content editor.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class MenuBarElement extends BaseElement
{
    /**
     * The XPath used to locate a menu item by its label.
     */
    private static final String MENU_ITEM_XPATH = "//td[contains(@class, 'gwt-MenuItem') and . = '%s']";

    /**
     * The element that wraps the menu bar.
     */
    private final WebElement container;

    /**
     * Creates a new instance that can be used to control the menu bar inside the given container.
     * 
     * @param container the element that wraps the menu bar
     */
    public MenuBarElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * Clicks on the image menu.
     */
    public void clickImageMenu()
    {
        openMenuWithLabel("Image");
    }

    /**
     * Click on the table menu.
     */
    public void clickTableMenu()
    {
        openMenuWithLabel("Table");
    }

    /**
     * Clicks on the "Attached Image..." menu.
     * 
     * @return the pane used to select an attached image to insert
     */
    public AttachedImageSelectPane clickInsertAttachedImageMenu()
    {
        String xpath = String.format(MENU_ITEM_XPATH, "Attached Image...");
        WebElement insertAttachedImageMenu = container.findElement(By.xpath(xpath));
        if (!isMenuEnabled(insertAttachedImageMenu)) {
            return null;
        }
        insertAttachedImageMenu.click();
        return new AttachedImageSelectPane().waitToLoad();
    }

    /**
     * Clicks on the "Insert Table..." menu.
     * 
     * @return the pane used to configure the table to be inserted
     */
    public TableConfigPane clickInsertTableMenu()
    {
        String xpath = String.format(MENU_ITEM_XPATH, "Insert Table...");
        WebElement insertTableMenu = container.findElement(By.xpath(xpath));
        if (!isMenuEnabled(insertTableMenu)) {
            return null;
        }
        insertTableMenu.click();
        return new TableConfigPane().waitToLoad();
    }

    /**
     * @param menu a menu item
     * @return {@code true} of the given menu item is enabled, {@code false} otherwise
     */
    private boolean isMenuEnabled(WebElement menu)
    {
        return !menu.getAttribute("class").contains("gwt-MenuItem-disabled");
    }

    /**
     * Opens a menu from the menu bar.
     * 
     * @param label the label of the menu to open
     */
    private void openMenuWithLabel(String label)
    {
        // Hover the menu bar to hide any page menus that may be displayed over the WYSIWYG editor menu bar. For
        // instance, when you return from preview after clicking the "Back to edit" button, if the mouse is not moved it
        // can end up over the edit menu after the page is loaded. This opens the edit menu which hides part of the
        // WYSIWYG editor menu bar.
        new Actions(getDriver()).moveToElement(container).perform();
        clickMenuWithLabel(label);
    }

    /**
     * Clicks on the menu item with the given label.
     * 
     * @param label the label of the menu item to click
     */
    private void clickMenuWithLabel(String label)
    {
        container.findElement(By.xpath(String.format(MENU_ITEM_XPATH, label))).click();
    }
}
