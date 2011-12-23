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
package org.xwiki.test.ui.po.editor.wysiwyg;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
        clickMenuWithLabel("Image");
    }

    /**
     * Clicks on the "Attached Image..." menu.
     * 
     * @return the pane used to select an attached image to insert
     */
    public AttachedImageSelectPane clickInsertAttachedImageMenu()
    {
        WebElement insertAttachedImageMenu =
            container.findElement(By.xpath("//td[contains(@class, 'gwt-MenuItem') and . = 'Attached Image...']"));
        if (!isMenuEnabled(insertAttachedImageMenu)) {
            return null;
        }
        insertAttachedImageMenu.click();
        return new AttachedImageSelectPane().waitToLoad();
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
     * Clicks on the menu item with the given label.
     * 
     * @param label the label of the menu item to click
     */
    private void clickMenuWithLabel(String label)
    {
        WebElement menu =
            container.findElement(By.xpath("//td[contains(@class, 'gwt-MenuItem') and . = '" + label + "']"));
        menu.click();
    }
}
