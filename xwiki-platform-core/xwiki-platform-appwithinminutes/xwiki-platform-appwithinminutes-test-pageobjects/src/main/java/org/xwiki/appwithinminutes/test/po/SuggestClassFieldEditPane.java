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
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents the pane used to edit a class field with a suggest.
 * Example of class fields: User and Page.
 * 
 * @version $Id$
 * @since 4.5
 */
public class SuggestClassFieldEditPane extends ClassFieldEditPane
{
    /**
     * Creates a new instance.
     * 
     * @param fieldName the name of the date field
     */
    public SuggestClassFieldEditPane(String fieldName)
    {
        super(fieldName);
    }

    /**
     * Sets whether this field supports multiple selection.
     * 
     * @param multiple {@code true} to enable multiple selection, {@code false} to disable it
     */
    public void setMultipleSelect(boolean multiple)
    {
        WebElement multiSelectCheckBox = getPropertyInput("multiSelect");
        if (multiSelectCheckBox.isSelected() != multiple) {
            multiSelectCheckBox.click();
        }
    }

    /**
     * @return the picker
     */
    public SuggestInputElement getPicker()
    {
        return new SuggestInputElement(getDefaultValueInput());
    }
}
