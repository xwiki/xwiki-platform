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
package org.xwiki.ckeditor.test.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

@UITest
class TableIT extends AbstractCKEditorIT
{
    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        maybeLeaveEditMode(setup, testReference);
    }

    /**
     * See XWIKI-22403: WYSIWYG deletes typed text due to fake table selection
     */
    @Test
    @Order(1)
    void backspaceAfterInsertRow(TestUtils setup, TestReference testReference)
    {
        edit(setup, testReference);

        // Insert a table.
        editor.getToolBar().insertTable().submit();

        // The caret should be in the first table cell. Insert two paragraphs. It's important to have two paragraphs,
        // otherwise the bug doesn't reproduce.
        textArea.sendKeys("one", Keys.ENTER, "two");

        // Insert a row after the current one.
        textArea.sendKeys("/table_row_after");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/table_row_after", "Insert Row After");
        // We click instead of pressing Enter to be sure the fake selection is not lost.
        qa.getSelectedItem().click();
        qa.waitForItemSubmitted();

        // Type in the cell below the selected one (i.e. the first cell on the second row).
        textArea.click(By.cssSelector("tr:nth-of-type(2) > td:first-of-type"));
        textArea.sendKeys("three", Keys.BACK_SPACE);

        // Check the result.
        assertSourceEquals("|(((\none\n\ntwo\n)))| \n|thre| \n| | \n| | \n\n ");
    }
}
