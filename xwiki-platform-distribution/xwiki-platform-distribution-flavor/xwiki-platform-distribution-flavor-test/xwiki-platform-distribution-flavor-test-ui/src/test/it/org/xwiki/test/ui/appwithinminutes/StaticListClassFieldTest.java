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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.StaticListClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.StaticListItemsEditor;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;

/**
 * Special class editor tests that address only the Static List class field type.
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class StaticListClassFieldTest extends AbstractClassEditorTest
{
    /**
     * Tests that the field preview is properly updated when the display type is changed. Currently selected items must
     * be preserved.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testDisplayType()
    {
        // Add a new static list field.
        StaticListClassFieldEditPane staticListField =
            new StaticListClassFieldEditPane(editor.addField("Static List").getName());
        // By default the list is displayed using check boxes.
        Assert.assertEquals("checkbox", staticListField.getPreviewInputType());

        // Enable multiple selection.
        staticListField.openConfigPanel();
        staticListField.getMultipleSelectionCheckBox().click();

        // Select the first and third options.
        staticListField.getItemByValue("value1").click();
        staticListField.getItemByValue("value3").click();

        // Change the display type to 'select'.
        staticListField.getDisplayTypeSelect().selectByVisibleText("select");
        staticListField.closeConfigPanel();

        // Assert that the field preview has been updated.
        Assert.assertEquals("select", staticListField.getPreviewInputType());
        // Assert that the selected values were preserved.
        Assert.assertEquals(Arrays.asList("value1", "value3"), staticListField.getDefaultSelectedValues());

        // Select only the second option.
        staticListField.setDefaultValue("value2");

        // Change the display type to 'radio'.
        staticListField.openConfigPanel();
        staticListField.getDisplayTypeSelect().selectByVisibleText("radio");
        staticListField.closeConfigPanel();
        // Assert that the field preview has been updated.
        Assert.assertEquals("radio", staticListField.getPreviewInputType());
        // Assert that the selected value was preserved.
        Assert.assertEquals("value2", staticListField.getDefaultValue());
    }

    /**
     * Tests that multiple select state is synchronized with the rest of the meta properties.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testMultipleSelect()
    {
        // Add a new static list field.
        StaticListClassFieldEditPane staticListField =
            new StaticListClassFieldEditPane(editor.addField("Static List").getName());

        // Open the configuration panel and play with the multiple selection option.
        staticListField.openConfigPanel();

        // Radio display type should disable multiple selection.
        staticListField.getMultipleSelectionCheckBox().click();
        Assert.assertTrue(staticListField.getMultipleSelectionCheckBox().isSelected());
        staticListField.getDisplayTypeSelect().selectByVisibleText("radio");
        Assert.assertFalse(staticListField.getMultipleSelectionCheckBox().isSelected());

        // Enabling multiple selection when display type is radio should change display type to check box.
        staticListField.getMultipleSelectionCheckBox().click();
        Assert.assertEquals("checkbox",
            staticListField.getDisplayTypeSelect().getFirstSelectedOption().getAttribute("value"));

        // 'select' display type supports properly multiple selection only if size is greater than 1.
        Assert.assertEquals("1", staticListField.getSizeInput().getAttribute("value"));
        staticListField.getDisplayTypeSelect().selectByVisibleText("select");
        Assert.assertEquals("5", staticListField.getSizeInput().getAttribute("value"));
    }

    /**
     * Tests the ability to add, edit and remove list items.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testItemsEditor()
    {
        // Add a new static list field.
        StaticListClassFieldEditPane staticListField =
            new StaticListClassFieldEditPane(editor.addField("Static List").getName());

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

        Assert.assertEquals(4, itemsEditor.size());

        // Enable multiple selection and change display type to 'select' to check the value of the size property.
        staticListField.getDisplayTypeSelect().selectByVisibleText("select");
        staticListField.getMultipleSelectionCheckBox().click();
        Assert.assertEquals("5", staticListField.getSizeInput().getAttribute("value"));

        // Apply configuration changes and assert the result.
        staticListField.closeConfigPanel();
        // The initial second item was removed.
        Assert.assertNull(staticListField.getItemByValue("value2"));
        // We should have a new item with value "XWiki".
        staticListField.getItemByValue("XWiki").click();
        // Assert the order of the items.
        staticListField.getItemByValue("value1").click();
        Assert.assertEquals(Arrays.asList("XWiki", "value1"), staticListField.getDefaultSelectedValues());
    }
}
