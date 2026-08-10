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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.DBListClassFieldEditPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage.goToEditor;

/**
 * Special class editor tests that address only the Database List class field type.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class DBListClassFieldIT
{
    private final String fieldName = "Database List";

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.deleteSpace(testReference.getLastSpaceReference());
    }

    /**
     * Tests that the field preview is properly updated when the display type is changed. Currently selected items must
     * be preserved.
     */
    @Test
    @Order(1)
    void displayType(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);

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
}
