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
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.FormElement;

/**
 * Represents the pane used to edit a class property.
 * 
 * @version $Id$
 * @since 4.5
 */
public class ClassPropertyEditPane extends BaseElement
{
    /**
     * The class editor form.
     */
    private final FormElement form;

    /**
     * The edited property.
     */
    private final String propertyName;

    /**
     * Creates a new pane that can be used to set the meta-properties of the specified XClass property.
     * 
     * @param form the class editor form
     * @param propertyName the name of the edited property
     */
    public ClassPropertyEditPane(FormElement form, String propertyName)
    {
        this.form = form;
        this.propertyName = propertyName;
    }

    /**
     * Expands this property pane so that the meta-properties are visible and thus editable.
     * 
     * @return this
     */
    public ClassPropertyEditPane expand()
    {
        By containerLocator = By.id("xproperty_" + propertyName);
        By titleLocator = By.id("xproperty_" + propertyName + "_title");
        getDriver().waitUntilElementIsVisible(containerLocator);
        if (getDriver().findElementWithoutWaiting(containerLocator).getAttribute("class").contains("collapsed")) {
            getDriver().findElementWithoutWaiting(titleLocator).click();
        }
        return this;
    }

    /**
     * Sets a meta property of the edited XClass property.
     * 
     * @param metaPropertyName the name of the meta-property
     * @param value the value to set
     * @return this
     */
    protected ClassPropertyEditPane setMetaProperty(String metaPropertyName, String value)
    {
        form.setFieldValue(By.id(propertyName + "_" + metaPropertyName), value);
        return this;
    }

    /**
     * Sets the pretty name of the edited property.
     * 
     * @param prettyName the new pretty name
     * @return this
     */
    public ClassPropertyEditPane setPrettyName(String prettyName)
    {
        return setMetaProperty("prettyName", prettyName);
    }
}
