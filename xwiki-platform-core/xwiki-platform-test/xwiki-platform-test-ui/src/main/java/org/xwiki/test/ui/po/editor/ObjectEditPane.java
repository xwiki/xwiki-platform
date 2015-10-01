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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.FormElement;

/**
 * Represents a group of form fields that are used to edit an object of a specific type.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
public class ObjectEditPane extends FormElement
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
     * Opens the date picker for the specified date property of the edited object.
     * 
     * @param datePropertyName the name of a date property of the edited object
     * @return the date picker
     */
    public DatePicker openDatePicker(String datePropertyName)
    {
        getDriver().findElementWithoutWaiting(getForm(), byPropertyName(datePropertyName)).click();
        return new DatePicker();
    }

    /**
     * @param userPropertyName the name of a property of type List of Users
     * @return a user picker for a property of type List of Users
     */
    public UserPicker getUserPicker(String userPropertyName)
    {
        return new UserPicker(getDriver().findElementWithoutWaiting(getForm(), byPropertyName(userPropertyName)));
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
}
