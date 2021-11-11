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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

/**
 * The page object used to edit a document or object property in-place.
 * 
 * @param <T> the property value type
 * @version $Id$
 * @since 12.6.3
 * @since 12.9RC1
 */
public class EditablePropertyPane<T> extends BaseElement
{
    protected final WebElement element;

    protected final WebElement viewer;

    protected final WebElement editor;

    protected final WebElement editButton;

    protected final WebElement cancelButton;

    protected final WebElement saveButton;

    public EditablePropertyPane(String property)
    {
        this.element =
            getDriver().findElement(By.cssSelector("dt.editableProperty[data-property=\"" + property + "\"]"));
        this.viewer =
            this.element.findElement(By.xpath("./following-sibling::dd[contains(@class, 'editableProperty-viewer')]"));
        this.editor =
            this.viewer.findElement(By.xpath("./following-sibling::dd[contains(@class, 'editableProperty-editor')]"));
        this.editButton = this.element.findElement(By.className("editableProperty-edit"));
        this.cancelButton = this.element.findElement(By.className("editableProperty-cancel"));
        this.saveButton = this.element.findElement(By.className("editableProperty-save"));
    }

    public EditablePropertyPane<T> clickEdit()
    {
        this.editButton.click();
        getDriver().waitUntilCondition(visibilityOf(this.editor));
        return this;
    }

    public EditablePropertyPane<T> clickCancel()
    {
        this.cancelButton.click();
        getDriver().waitUntilCondition(visibilityOf(this.editButton));
        return this;
    }

    public EditablePropertyPane<T> clickSave()
    {
        this.saveButton.click();
        getDriver().waitUntilCondition(visibilityOf(this.editButton));
        return this;
    }

    public String getLabel()
    {
        return this.element.findElement(By.tagName("label")).getText();
    }

    public String getDisplayValue()
    {
        return this.viewer.getText();
    }

    /**
     * @return the property value while the property is being edited
     * @since 12.10.11
     * @since 13.4.6
     * @since 13.10RC1
     */
    @SuppressWarnings("unchecked")
    public T getValue()
    {
        WebElement inputField = getInputField();
        if ("checkbox".equals(inputField.getAttribute("type"))) {
            return (T) Boolean.valueOf(inputField.isSelected());
        } else {
            return (T) inputField.getAttribute("value");
        }
    }

    /**
     * Sets the property value while the property is being edited.
     * 
     * @param value the new property value
     * @return this property pane
     * @since 12.10.11
     * @since 13.4.6
     * @since 13.10RC1
     */
    public EditablePropertyPane<T> setValue(T value)
    {
        WebElement inputField = getInputField();
        if (value instanceof Boolean) {
            // If the value is boolean then we assume the input field is a checkbox.
            if (inputField.isSelected() != (Boolean) value) {
                inputField.click();
            }
        } else {
            // Assume the input field is a text field (text input or text area).
            inputField.clear();
            inputField.sendKeys(String.valueOf(value));
        }
        return this;
    }

    private WebElement getInputField()
    {
        return getDriver().findElementWithoutWaiting(this.editor, By.cssSelector("[name]"));
    }
}
