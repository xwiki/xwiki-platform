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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Special class editor tests that address only the Database List class field type.
 * 
 * @version $Id$
 * @since 11.3RC1
 */
public class DBListClassFieldTest extends AbstractListClassFieldTest
{
    public DBListClassFieldTest()
    {
        super("Database List");
    }

    /**
     * Tests that the field preview is properly updated when the display type is changed. Currently selected items must
     * be preserved.
     */
    @Test
    public void testDisplayType()
    {
        // Add a new database list field.
        DBListClassFieldEditPane dbListField = new DBListClassFieldEditPane(editor.addField(this.fieldName).getName());

        // Check that the input suggest picker is working.
        dbListField.getPicker().sendKeys("db").waitForSuggestions().selectByVisibleText("DBList");

        // Enable multiple selection.
        dbListField.openConfigPanel();
        dbListField.getMultipleSelectionCheckBox().click();

        // The size field should be disabled (it can be used only when display type is select).
        assertTrue(isReadOnly(dbListField.getSizeInput()));

        // Change the display type to 'select'.
        dbListField.getDisplayTypeSelect().selectByVisibleText("select");
        assertFalse(isReadOnly(dbListField.getSizeInput()));
        dbListField.closeConfigPanel();

        // Assert that the selected values were preserved.
        assertEquals(Arrays.asList("AppWithinMinutes.DBList"), dbListField.getDefaultSelectedValues());

        // Select a second option.
        dbListField.getItemByValue("AppWithinMinutes.Date").click();

        // Change the display type back to 'input'.
        dbListField.openConfigPanel();
        dbListField.getDisplayTypeSelect().selectByVisibleText("input");
        assertTrue(isReadOnly(dbListField.getSizeInput()));
        dbListField.closeConfigPanel();
        // Assert that the selected values have been preserved.
        assertEquals(Arrays.asList("AppWithinMinutes.DBList", "AppWithinMinutes.Date"),
            dbListField.getPicker().getValues());
    }
}
