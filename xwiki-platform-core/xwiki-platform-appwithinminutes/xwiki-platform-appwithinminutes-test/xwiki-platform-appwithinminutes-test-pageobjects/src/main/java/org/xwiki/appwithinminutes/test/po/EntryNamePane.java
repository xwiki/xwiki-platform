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
package org.xwiki.appwithinminutes.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the pane used to input the entry name.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class EntryNamePane extends BaseElement
{
    @FindBy(xpath = "//div[@id = 'entryNamePopup']//input[@type = 'text']")
    private WebElement nameInput;

    @FindBy(xpath = "//div[@id = 'entryNamePopup']//input[@alt = 'Add']")
    private WebElement addButton;

    /**
     * Types the given string into the name input.
     * 
     * @param name the entry name
     */
    public void setName(String name)
    {
        nameInput.clear();
        // Note: Normally we should use: nameInput.sendKeys(name);
        // However this fails on Vincent's Mac for some reason and the following seems to work everywhere.
        // Revert when Selenium is fixed.
        getDriver().executeScript("arguments[0].value='" + name + "';", nameInput);
    }

    /**
     * Clicks on the add button to add the entry with the specified name.
     * 
     * @return the page used to edit the new entry
     */
    public EntryEditPage clickAdd()
    {
        addButton.click();
        return new EntryEditPage();
    }
}
