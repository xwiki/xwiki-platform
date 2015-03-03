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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents the pane used to edit a 'Static List' class field.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class StaticListClassFieldEditPane extends ClassFieldEditPane
{
    /**
     * The element that wraps the input used to specify the field default value.
     */
    private WebElement defaultValueContainer;

    /**
     * Creates a new instance.
     * 
     * @param fieldName the name of the date field
     */
    public StaticListClassFieldEditPane(String fieldName)
    {
        super(fieldName);

        defaultValueContainer = getContainer().findElement(By.xpath(".//dl[@class = 'field-viewer']/dd"));
    }

    /**
     * @return the list of available display types
     */
    public Select getDisplayTypeSelect()
    {
        return new Select(getPropertyInput("displayType"));
    }

    /**
     * @return the input used to specify the size
     */
    public WebElement getSizeInput()
    {
        return getPropertyInput("size");
    }

    /**
     * @return the check box used to enable multiple selection
     */
    public WebElement getMultipleSelectionCheckBox()
    {
        return getPropertyInput("multiSelect");
    }

    @Override
    public String getDefaultValue()
    {
        String displayType = getPreviewInputType();
        if ("input".equals(displayType)) {
            return super.getDefaultValue();
        } else {
            for (WebElement selectedItem : getSelectedItems()) {
                // Return the first selected value.
                return selectedItem.getAttribute("value");
            }
        }
        return null;
    }

    @Override
    public void setDefaultValue(String defaultValue)
    {
        if ("input".equals(getPreviewInputType())) {
            super.setDefaultValue(defaultValue);
        } else {
            // Clear current selection.
            for (WebElement selectedItem : getSelectedItems()) {
                selectedItem.click();
            }
            // Select the specified item.
            getItemByValue(defaultValue).click();
        }
    }

    /**
     * Do not use this method when display type is input.
     * 
     * @return the list items selected by default
     */
    public List<String> getDefaultSelectedValues()
    {
        List<String> selectedValues = new ArrayList<String>();
        for (WebElement selectedItem : getSelectedItems()) {
            selectedValues.add(selectedItem.getAttribute("value"));
        }
        return selectedValues;
    }

    /**
     * @return the list of selected items
     */
    protected List<WebElement> getSelectedItems()
    {
        By xpath = By.xpath(".//*[local-name() = 'option' or @type = 'radio' or @type = 'checkbox']");
        List<WebElement> selectedItems = new ArrayList<WebElement>();
        for (WebElement item : getDriver().findElementsWithoutWaiting(defaultValueContainer, xpath)) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    /**
     * Do not used this method when display type is input.
     * 
     * @param value the value of the list item to return
     * @return returns the list item that has the given value
     */
    public WebElement getItemByValue(String value)
    {
        By xpath = By.xpath(".//*[@value = '" + value + "']");
        try {
            return getDriver().findElementWithoutWaiting(defaultValueContainer, xpath);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return the type of HTML input used to preview the list; possible returned values are specified by
     *         {@link #getDisplayTypeSelect()}
     */
    public String getPreviewInputType()
    {
        By xpath = By.xpath(".//*[local-name() = 'select' or (local-name() = 'input' and not(@type = 'hidden'))]");
        List<WebElement> inputs = getDriver().findElementsWithoutWaiting(defaultValueContainer, xpath);
        if (inputs.size() > 0) {
            WebElement input = inputs.get(0);
            return "select".equalsIgnoreCase(input.getTagName()) ? "select" : input.getAttribute("type").toLowerCase();
        }
        return null;
    }

    /**
     * @return the static list items editor
     */
    public StaticListItemsEditor getItemsEditor()
    {
        return new StaticListItemsEditor(getDriver().findElementWithoutWaiting(getContainer(),
            By.className("staticListEditor")));
    }
}
