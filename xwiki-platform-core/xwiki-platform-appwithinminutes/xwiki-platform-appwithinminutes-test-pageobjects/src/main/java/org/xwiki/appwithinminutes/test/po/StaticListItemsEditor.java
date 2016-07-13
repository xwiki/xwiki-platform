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

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the static list items editor present on the configuration pane of a static list field.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class StaticListItemsEditor extends BaseElement
{
    /**
     * The element that wraps the editor.
     */
    private WebElement container;

    /**
     * The text input used to specify the item value.
     */
    private WebElement valueInput;

    /**
     * The text input used to specify the item label.
     */
    private WebElement labelInput;

    /**
     * The button used to add a new list item.
     */
    private WebElement addButton;

    /**
     * Creates a new instance.
     * 
     * @param container the element that wraps the editor
     */
    public StaticListItemsEditor(WebElement container)
    {
        this.container = container;

        By xpath = By.xpath(".//*[@class = 'xHint' and . = 'ID']/following-sibling::input[@type = 'text']");
        valueInput = getDriver().findElementWithoutWaiting(container, xpath);

        xpath = By.xpath(".//*[@class = 'xHint' and . = 'Value']/following-sibling::input[@type = 'text']");
        labelInput = getDriver().findElementWithoutWaiting(container, xpath);

        addButton = getDriver().findElementWithoutWaiting(container, By.className("add"));
    }

    /**
     * Removes the item with the specified value.
     * 
     * @param value the value of the item to be removed
     */
    public void remove(String value)
    {
        By xpath = By.xpath("ul/li/*[@title = '" + value + "']/following-sibling::*[@class = 'delete']");
        getDriver().findElementWithoutWaiting(container, xpath).click();
    }

    /**
     * Adds a new item with the specified value and label.
     * 
     * @param value item value
     * @param label item label
     */
    public void add(String value, String label)
    {
        valueInput.clear();
        valueInput.sendKeys(value);
        labelInput.clear();
        labelInput.sendKeys(label);
        addButton.click();
    }

    /**
     * @return the text input used to specify the item value
     */
    public WebElement getValueInput()
    {
        return valueInput;
    }

    /**
     * @return the text input used to specify the item label
     */
    public WebElement getLabelInput()
    {
        return labelInput;
    }

    /**
     * Changes item label.
     * 
     * @param value item value
     * @param newLabel the new item label
     */
    public void setLabel(String value, String newLabel)
    {
        getItem(value).click();
        labelInput.clear();
        labelInput.sendKeys(newLabel + Keys.RETURN);
    }

    /**
     * Reorders list items.
     * 
     * @param valueToMove the value of the item to be moved
     * @param beforeValue the value of the reference item
     */
    public void moveBefore(String valueToMove, String beforeValue)
    {
        new Actions(getDriver()).clickAndHold(getItem(valueToMove))
            .moveToElement(getItem(beforeValue), 0, 0).release().perform();
    }

    /**
     * @param valueOrLabel the value of the label of a list item
     * @return the first list item that has the specified value or label
     */
    public WebElement getItem(String valueOrLabel)
    {
        By xpath = By.xpath("ul/li/*[@title = '" + valueOrLabel + "' or . = '" + valueOrLabel + "']");
        return getDriver().findElementWithoutWaiting(container, xpath);
    }

    /**
     * @return the number of list items
     */
    public int size()
    {
        return getDriver().findElementsWithoutWaiting(container, By.tagName("li")).size();
    }
}
