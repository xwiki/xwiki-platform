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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ConfirmationBox;

/**
 * Represents the pane used to edit a class field.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ClassFieldEditPane extends BaseElement
{
    /**
     * The name of the edited field.
     */
    private final String fieldName;

    /**
     * The element that wraps the class field edit pane.
     */
    private final WebElement container;

    /**
     * The field tool box. Includes the icons for moving and deleting a field, as well as the icon to show/hide the
     * field configuration panel.
     */
    private final WebElement toolBox;

    /**
     * Creates a new instance that can be used to edit the specified class field.
     * 
     * @param fieldName the field name
     */
    public ClassFieldEditPane(String fieldName)
    {
        this.fieldName = fieldName;

        // Wait for the element to become visible.
        By containerLocator = By.id("field-" + fieldName);
        getDriver().waitUntilElementIsVisible(containerLocator);

        container = getDriver().findElement(containerLocator);
        toolBox = container.findElement(By.className("toolBox"));
    }

    /**
     * Sets the field pretty name.
     * 
     * @param prettyName the new field pretty name
     */
    public void setPrettyName(String prettyName)
    {
        WebElement prettyNameInput = getPropertyInput("prettyName");
        prettyNameInput.clear();
        prettyNameInput.sendKeys(prettyName);
    }

    /**
     * Sets the field default value
     * 
     * @param defaultValue the new field default value
     */
    public void setDefaultValue(String defaultValue)
    {
        WebElement defaultValueInput = getDefaultValueInput();
        defaultValueInput.clear();
        defaultValueInput.sendKeys(defaultValue);
    }

    /**
     * @return the default value of this field
     */
    public String getDefaultValue()
    {
        return getDefaultValueInput().getAttribute("value");
    }

    /**
     * @return the element used to input the default field value
     */
    protected WebElement getDefaultValueInput()
    {
        // Workaround for the fact that ends-with XPath function is not implemented.
        // substring(@id, string-length(@id) - string-length(suffix) + 1)
        String xpath = ".//*[substring(@id, string-length(@id) - %s - 2) = '_0_%s']";
        try {
            return getDriver().findElementWithoutWaiting(container,
                By.xpath(String.format(xpath, fieldName.length(), fieldName)));
        } catch (NoSuchElementException e) {
            // Return the first input element from the field display. This is needed for the Title and Content fields.
            return getDriver().findElementWithoutWaiting(container,
                By.xpath(".//dl[@class = 'field-viewer']/dd//*[@name]"));
        }
    }

    /**
     * Opens the field configuration panel.
     */
    public void openConfigPanel()
    {
        clickToolBoxIcon("Configure");
    }

    /**
     * Closes the field configuration panel.
     */
    public void closeConfigPanel()
    {
        clickToolBoxIcon("Preview");
        String previewXPath = "//*[@id = 'field-" + fieldName + "']//dl[@class = 'field-viewer']/dd";
        getDriver().waitUntilElementHasAttributeValue(By.xpath(previewXPath), "class", "");
    }

    /**
     * Clicks on the specified icon from the field tool box.
     * 
     * @param alt the alternative text of the tool box icon to be clicked
     */
    private void clickToolBoxIcon(String alt)
    {
        // This doesn't trigger the :hover CSS pseudo class so we're forced to manually set the display of the tool box.
        new Actions(getDriver()).moveToElement(container).perform();

        // FIXME: The following line is a hack to overcome the fact that the previous line doesn't trigger the :hover
        // CSS pseudo class on the field container (even if the mouse if moved over it).
        showToolBox();

        toolBox.findElement(By.xpath("img[@alt = '" + alt + "']")).click();

        // Reset the tool box display. Remove this line when the :hover CSS class will be triggered by mouse over.
        hideToolBox();
    }

    /**
     * Workaround for the fact that we can't yet hover the field container. We can move the mouse over the field
     * container but its :hover CSS class is not triggered so the tool box stays hidden.
     */
    private void showToolBox()
    {
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].style.display = 'block';", toolBox);
    }

    /**
     * Resets the tool box display.
     * 
     * @see #showToolBox()
     */
    private void hideToolBox()
    {
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].style.display = '';", toolBox);
    }

    /**
     * Sets the field name
     * 
     * @param fieldName the new field name
     */
    public void setName(String fieldName)
    {
        WebElement nameInput = getPropertyInput("name");
        nameInput.clear();
        nameInput.sendKeys(fieldName);
    }

    /**
     * @return the current value of the 'name' field meta property
     */
    public String getName()
    {
        return getPropertyInput("name").getAttribute("value");
    }

    /**
     * @param propertyName the name of a class field meta property
     * @return the form input used to edit the value of the specified meta property
     */
    protected WebElement getPropertyInput(String propertyName)
    {
        return container.findElement(By.id(String.format("field-%s_%s", this.fieldName, propertyName)));
    }

    /**
     * Clicks on the delete field icon.
     * 
     * @return the confirmation box to confirm the field delete
     */
    public ConfirmationBox delete()
    {
        clickToolBoxIcon("Delete");
        return new ConfirmationBox();
    }

    /**
     * Drag this field over the specified element. Use this method to reorder class fields.
     * 
     * @param element the element to drag this field to
     * @param xOffset offset from the top-left corner of the given element; a negative value means coordinates right
     *            from the given element
     * @param yOffset offset from the top-left corner of the given element; a negative value means coordinates above the
     *            given element
     */
    public void dragTo(WebElement element, int xOffset, int yOffset)
    {
        // This doesn't trigger the :hover CSS pseudo class so we're forced to manually set the display of the tool box.
        new Actions(getDriver()).moveToElement(container).perform();

        // FIXME: The following line is a hack to overcome the fact that the previous line doesn't trigger the :hover
        // CSS pseudo class on the field container (even if the mouse if moved over it).
        showToolBox();

        WebElement dragHandler = toolBox.findElement(By.xpath("img[@alt = 'Move']"));
        new Actions(getDriver()).clickAndHold(dragHandler).moveToElement(element, xOffset, yOffset).release().perform();

        // Reset the tool box display. Remove this line when the :hover CSS class will be triggered by mouse over.
        hideToolBox();
    }

    /**
     * @return the element that wraps the class field edit pane
     */
    protected WebElement getContainer()
    {
        return container;
    }

    /**
     * Do not mistaken this with {@link #getName()}.
     * 
     * @return the name of the edited field
     */
    protected String getFieldName()
    {
        return fieldName;
    }
}
