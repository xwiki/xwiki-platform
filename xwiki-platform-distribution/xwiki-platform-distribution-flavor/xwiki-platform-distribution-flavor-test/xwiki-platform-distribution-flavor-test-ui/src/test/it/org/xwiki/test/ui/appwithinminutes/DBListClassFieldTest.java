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

import java.util.Arrays;

import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.DBListClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.ListClassFieldEditPane;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Special class editor tests that address only the Database List class field type.
 *
 * @version $Id$
 * @since 11.3RC1
 */
public class DBListClassFieldTest extends AbstractClassEditorTest
{
    private final String fieldName = "Database List";

    /**
     * Tests that the field preview is properly updated when the display type is changed. Currently selected items must
     * be preserved.
     */
    @Test
    public void displayType()
    {
        // Add a new database list field.
        DBListClassFieldEditPane dbListField = new DBListClassFieldEditPane(editor.addField(this.fieldName).getName());

        // Check that the input suggest picker is working.
        dbListField.getPicker().sendKeys("db").waitForSuggestions().selectByVisibleText("DBList");

        // Enable multiple selection.
        dbListField.openConfigPanel();
        dbListField.getMultipleSelectionCheckBox().click();

        // The size field should be disabled (it can be used only when display type is select).
        assertTrue(dbListField.isReadOnly());

        // Change the display type to 'select'.
        dbListField.getDisplayTypeSelect().selectByVisibleText("select");
        assertFalse(dbListField.isReadOnly());
        dbListField.closeConfigPanel();

        // Assert that the selected values were preserved.
        assertEquals(Arrays.asList("AppWithinMinutes.DBList"), dbListField.getDefaultSelectedValues());

        // Select a second option.
        dbListField.getItemByValue("AppWithinMinutes.Date").click();

        // Change the display type back to 'input'.
        dbListField.openConfigPanel();
        dbListField.getDisplayTypeSelect().selectByVisibleText("input");
        assertTrue(dbListField.isReadOnly());
        dbListField.closeConfigPanel();
        // Assert that the selected values have been preserved.
        assertEquals(Arrays.asList("AppWithinMinutes.DBList", "AppWithinMinutes.Date"),
            dbListField.getPicker().getValues());
    }

    /**
     * Tests that multiple select state is synchronized with the rest of the meta properties.
     *
     * @since 13.3RC1
     * @since 12.10.6
     */
    @Test
    public void multipleSelect()
    {
        // Add a new list field.
        ListClassFieldEditPane listField = new ListClassFieldEditPane(this.editor.addField(this.fieldName).getName());

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
        assertTrue(listField.isReadOnly());
        listField.getDisplayTypeSelect().selectByVisibleText("select");
        assertFalse(listField.isReadOnly());
        assertEquals("2", listField.getSizeInput().getAttribute("value"));
    }
}
