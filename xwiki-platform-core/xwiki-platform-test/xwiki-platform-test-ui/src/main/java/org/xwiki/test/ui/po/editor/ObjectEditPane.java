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
package org.xwiki.test.ui.po.editor;

import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents a group of form fields that are used to edit an object of a specific type.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
public class ObjectEditPane extends FormContainerElement
{
    /**
     * The object type.
     */
    private String className;

    /**
     * The object number (identifies the object in the set of objects of the same type).
     */
    private int objectNumber;

    /**
     * Creates a new edit pane for an object of the specified type. The form fields from the given container should
     * correspond to properties of the specified type.
     *
     * @param containerLocator the element that locates the form fields used to edit the object
     * @param className the object type
     * @param objectNumber the object number (identifies the object in the set of objects of the same type)
     */
    public ObjectEditPane(By containerLocator, String className, int objectNumber)
    {
        super(containerLocator);

        this.className = className;
        this.objectNumber = objectNumber;
    }

    /**
     * Creates a new edit pane for an object of the specified type. The form fields from the given container should
     * correspond to properties of the specified type.
     * 
     * @param container the element that wraps the form fields used to edit the object
     * @param className the object type
     * @param objectNumber the object number (identifies the object in the set of objects of the same type)
     */
    public ObjectEditPane(WebElement container, String className, int objectNumber)
    {
        super(container);

        this.className = className;
        this.objectNumber = objectNumber;
    }

    /**
     * Helper to retrieve the xobject content div. This method returns an empty optional if the information have not
     * been loaded.
     */
    private Optional<WebElement> getObjectContent()
    {
        String xobjectContentId = String.format("xobject_%s_%s_content", this.className, this.objectNumber);
        try {
            return Optional.of(getDriver().findElementWithoutWaiting(By.id(xobjectContentId)));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if the information are loaded and displayed.
     *
     * @return {@code true} if the object information are displayed (i.e. the object is expanded)
     * @since 13.1RC1
     */
    public boolean isObjectDisplayed()
    {
        Optional<WebElement> objectContent = getObjectContent();
        return objectContent.map(WebElement::isDisplayed).orElse(false);
    }

    /**
     * Click on the xobject div to expand it, and waits until the information are loaded and displayed.
     * This method checks if the information are already displayed to avoid collapsing them if it's already the case.
     *
     * @since 13.1RC1
     */
    public void displayObject()
    {
        String xobjectId = String.format("xobject_%s_%s_title", this.className, this.objectNumber);

        if (!isObjectDisplayed()) {
            getDriver().findElementWithoutWaiting(By.id(xobjectId)).click();
            getDriver().waitUntilCondition(driver -> isObjectDisplayed());
        }
    }

    /**
     * Opens the date picker for the specified date property of the edited object.
     * 
     * @param datePropertyName the name of a date property of the edited object
     * @return the date picker
     */
    public BootstrapDateTimePicker openDatePicker(String datePropertyName)
    {
        getDriver().findElementWithoutWaiting(getFormElement(), byPropertyName(datePropertyName)).click();
        return new BootstrapDateTimePicker();
    }

    /**
     * @param userPropertyName the name of a property
     * @return a {@link SuggestInputElement suggest input} for the given property
     */
    public SuggestInputElement getSuggestInput(String userPropertyName)
    {
        return new SuggestInputElement(
            getDriver().findElementWithoutWaiting(getFormElement(), byPropertyName(userPropertyName)));
    }

    /**
     * Creates a locator for the input fields corresponding to the given object property.
     * 
     * @param propertyName the name of an object property
     * @return the locator for the input field corresponding to the specified property
     */
    public By byPropertyName(String propertyName)
    {
        return By.id(this.className + "_" + this.objectNumber + "_" + propertyName);
    }

    /**
     * Helper to fill property values quickly.
     *
     * @param propertyName the name of the property to set
     * @param propertyValue the value of the property
     * @return the current instance.
     */
    public ObjectEditPane setPropertyValue(String propertyName, String propertyValue)
    {
        this.setFieldValue(byPropertyName(propertyName), propertyValue);
        return this;
    }

    /**
     * @return the div container of the current object edit. Note that this should only be used in the case of
     * ObjectEditor.
     */
    private WebElement getXobjectContainer()
    {
        return getDriver().findElement(By.id(String.format("%s_%s_%s", "xobject", className, objectNumber)));
    }

    /**
     * @return {@code true} if the delete link is displayed for this object.
     * @since 12.4RC1
     */
    public boolean isDeleteLinkDisplayed()
    {
        try {
            return getXobjectContainer().findElement(By.className("delete")).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the edit link is displayed for this object.
     * @since 12.4RC1
     */
    public boolean isEditLinkDisplayed()
    {
        try {
            return getXobjectContainer().findElement(By.className("edit")).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return the current object number.
     * @since 12.4RC1
     */
    public int getObjectNumber()
    {
        return objectNumber;
    }
}
