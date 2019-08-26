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
package org.xwiki.test.ui.appwithinminutes;

import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.appwithinminutes.test.po.ListClassFieldEditPane;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Base class for class editor tests that address list fields (e.g. Static List, Database List)
 * 
 * @version $Id$
 * @since 11.3RC1
 */
public abstract class AbstractListClassFieldTest extends AbstractClassEditorTest
{
    protected final String fieldName;

    public AbstractListClassFieldTest(String fieldName)
    {
        this.fieldName = fieldName;
    }

    /**
     * Tests that multiple select state is synchronized with the rest of the meta properties.
     */
    @Test
    public void testMultipleSelect()
    {
        // Add a new list field.
        ListClassFieldEditPane listField = new ListClassFieldEditPane(editor.addField(this.fieldName).getName());

        // Open the configuration panel and play with the multiple selection option.
        listField.openConfigPanel();

        // Radio display type should disable multiple selection.
        listField.getMultipleSelectionCheckBox().click();
        assertTrue(listField.getMultipleSelectionCheckBox().isSelected());
        listField.getDisplayTypeSelect().selectByVisibleText("radio");
        assertFalse(listField.getMultipleSelectionCheckBox().isSelected());

        // Enabling multiple selection when display type is radio should change display type to check box.
        listField.getMultipleSelectionCheckBox().click();
        assertEquals("checkbox", listField.getDisplayTypeSelect().getFirstSelectedOption().getAttribute("value"));

        // 'select' display type supports properly multiple selection only if size is greater than 1.
        assertEquals("1", listField.getSizeInput().getAttribute("value"));
        listField.getDisplayTypeSelect().selectByVisibleText("select");
        assertEquals("5", listField.getSizeInput().getAttribute("value"));

        // Check that the specified size is not modified if it's greater than 1.
        listField.getSizeInput().clear();
        listField.getSizeInput().sendKeys("2");
        listField.getDisplayTypeSelect().selectByVisibleText("input");
        assertTrue(isReadOnly(listField.getSizeInput()));
        listField.getDisplayTypeSelect().selectByVisibleText("select");
        assertFalse(isReadOnly(listField.getSizeInput()));
        assertEquals("2", listField.getSizeInput().getAttribute("value"));
    }

    protected boolean isReadOnly(WebElement input)
    {
        return input.getAttribute("readOnly") != null;
    }
}
