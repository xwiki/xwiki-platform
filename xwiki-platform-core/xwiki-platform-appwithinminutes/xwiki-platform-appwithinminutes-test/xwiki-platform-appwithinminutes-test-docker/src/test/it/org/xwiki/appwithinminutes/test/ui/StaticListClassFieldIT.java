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
package org.xwiki.appwithinminutes.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ListClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.StaticListClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.StaticListItemsEditor;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage.goToEditor;

/**
 * Special class editor tests that address only the Static List class field type.
 *
 * @version $Id$
 * @since 13.3RC1
 * @since 12.10.6
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since 
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class StaticListClassFieldIT
{
    private final String fieldName = "Static List";

    /**
     * Tests that the field preview is properly updated when the display type is changed. Currently selected items must
     * be preserved.
     */
    @Test
    @Order(1)
    void displayType(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);

        // Add a new static list field.
        StaticListClassFieldEditPane staticListField =
            new StaticListClassFieldEditPane(editor.addField(this.fieldName).getName());
        // By default the list is displayed using check boxes.
        assertEquals("checkbox", staticListField.getPreviewInputType());

        // Enable multiple selection.
        staticListField.openConfigPanel();
        staticListField.getMultipleSelectionCheckBox().click();

        // The size field should be disabled (it can be used only when display type is select).
        assertTrue(staticListField.isReadOnly());

        // Select the first and third options.
        staticListField.getItemByValue("value1").click();
        staticListField.getItemByValue("value3").click();

        // Change the display type to 'select'.
        staticListField.getDisplayTypeSelect().selectByVisibleText("select");
        assertFalse(staticListField.isReadOnly());
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
        assertTrue(staticListField.isReadOnly());
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
    @Order(2)
    void itemsEditor(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);

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
        // The label of this item must be short, otherwise the call to moveBefore below fails to move this item in 
        // first position when the screen width is small (see XWIKI-18343).
        itemsEditor.setLabel("XWiki", "XS");

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

    @Test
    @Order(3)
    void multipleSelect(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);

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
        assertTrue(listField.isReadOnly());
        listField.getDisplayTypeSelect().selectByVisibleText("select");
        assertFalse(listField.isReadOnly());
        assertEquals("2", listField.getSizeInput().getAttribute("value"));
    }
}
