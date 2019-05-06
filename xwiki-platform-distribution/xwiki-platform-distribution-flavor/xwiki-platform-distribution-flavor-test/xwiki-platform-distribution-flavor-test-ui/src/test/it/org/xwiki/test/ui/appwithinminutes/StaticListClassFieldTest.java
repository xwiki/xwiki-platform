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
import org.xwiki.appwithinminutes.test.po.StaticListClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.StaticListItemsEditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Special class editor tests that address only the Static List class field type.
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class StaticListClassFieldTest extends AbstractListClassFieldTest
{
    public StaticListClassFieldTest()
    {
        super("Static List");
    }

    /**
     * Tests that the field preview is properly updated when the display type is changed. Currently selected items must
     * be preserved.
     */
    @Test
    public void testDisplayType()
    {
        // Add a new static list field.
        StaticListClassFieldEditPane staticListField =
            new StaticListClassFieldEditPane(editor.addField(this.fieldName).getName());
        // By default the list is displayed using check boxes.
        assertEquals("checkbox", staticListField.getPreviewInputType());

        // Enable multiple selection.
        staticListField.openConfigPanel();
        staticListField.getMultipleSelectionCheckBox().click();

        // The size field should be disabled (it can be used only when display type is select).
        assertTrue(isReadOnly(staticListField.getSizeInput()));

        // Select the first and third options.
        staticListField.getItemByValue("value1").click();
        staticListField.getItemByValue("value3").click();

        // Change the display type to 'select'.
        staticListField.getDisplayTypeSelect().selectByVisibleText("select");
        assertFalse(isReadOnly(staticListField.getSizeInput()));
        staticListField.closeConfigPanel();

        // Assert that the field preview has been updated.
        assertEquals("select", staticListField.getPreviewInputType());
        // Assert that the selected values were preserved.
        assertEquals(Arrays.asList("value1", "value3"), staticListField.getDefaultSelectedValues());

        // Select only the second option.
        staticListField.setDefaultValue("value2");

        // Change the display type to 'radio'.
        staticListField.openConfigPanel();
        staticListField.getDisplayTypeSelect().selectByVisibleText("radio");
        assertTrue(isReadOnly(staticListField.getSizeInput()));
        staticListField.closeConfigPanel();
        // Assert that the field preview has been updated.
        assertEquals("radio", staticListField.getPreviewInputType());
        // Assert that the selected value was preserved.
        assertEquals("value2", staticListField.getDefaultValue());
    }

    /**
     * Tests the ability to add, edit and remove list items.
     */
    @Test
    public void testItemsEditor()
    {
        // Add a new static list field.
        StaticListClassFieldEditPane staticListField =
            new StaticListClassFieldEditPane(editor.addField(this.fieldName).getName());

        // Open the configuration panel and edit the list items.
        staticListField.openConfigPanel();
        StaticListItemsEditor itemsEditor = staticListField.getItemsEditor();

        // Remove the second option.
        itemsEditor.remove("value2");

        // Add two new items.
        itemsEditor.add("foo", "bar");
        // Leave the value empty for the second item: it should fall back on the label.
        itemsEditor.add("", "XWiki");

        // Change the label of the last added item.
        itemsEditor.setLabel("XWiki", "XWiki Enterprise");

        // Move the last item before the first.
        itemsEditor.moveBefore("XWiki", "value1");

        assertEquals(4, itemsEditor.size());

        // Enable multiple selection and change display type to 'select' to check the value of the size property.
        staticListField.getDisplayTypeSelect().selectByVisibleText("select");
        staticListField.getMultipleSelectionCheckBox().click();
        assertEquals("5", staticListField.getSizeInput().getAttribute("value"));

        // Apply configuration changes and assert the result.
        staticListField.closeConfigPanel();
        // The initial second item was removed.
        assertNull(staticListField.getItemByValue("value2"));
        // We should have a new item with value "XWiki".
        staticListField.getItemByValue("XWiki").click();
        // Assert the order of the items.
        staticListField.getItemByValue("value1").click();
        assertEquals(Arrays.asList("XWiki", "value1"), staticListField.getDefaultSelectedValues());
    }
}
