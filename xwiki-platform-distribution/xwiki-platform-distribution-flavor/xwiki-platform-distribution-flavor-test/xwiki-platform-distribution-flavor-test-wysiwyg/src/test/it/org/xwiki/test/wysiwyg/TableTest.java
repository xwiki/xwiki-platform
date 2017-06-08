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
package org.xwiki.test.wysiwyg;

import org.junit.Test;
import org.openqa.selenium.Keys;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

import static org.junit.Assert.*;

/**
 * Functional tests for the table support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class TableTest extends AbstractWysiwygTestCase
{
    public static final String ROWS_SELECTOR = "//input[@title = 'Row count']";

    public static final String COLUMNS_SELECTOR = "//input[@title = 'Column count']";

    /**
     * The caret should be moved to the next or previous cell, depending on the Shift key.
     * 
     * @see XWIKI-3043: Prevent tab from moving focus from the new WYSIWYG editor
     */
    @Test
    public void testTabInTableCell()
    {
        insertTable();
        typeText("a");
        // Shit+Tab should do nothing since we are in the first cell.
        typeShiftTab();
        typeText("b");
        typeTab(3);
        // Delete the non-breaking space that is found by default in empty table cells.
        typeDelete();
        typeText("c");
        // Tab should insert a new row since we are in the last cell.
        typeTab();
        // Delete the non-breaking space that is found by default in empty table cells.
        typeDelete();
        typeText("d");
        typeShiftTab(4);
        typeText("e");
        switchToSource();
        assertSourceText("|=eab|= \n| |c\n|d| ");
    }

    /**
     * @see XWIKI-3090: Cannot move cursor before table
     * @see XWIKI-3089: Cannot move cursor after table
     * @see XWIKI-3829: Use Control/Meta+Up/Down arrow keys to navigate before/after a table
     */
    @Test
    public void testMoveCaretBeforeAndAfterTable()
    {
        switchToSource();
        setSourceText("|=Space|=Page\n|Main|WebHome");
        switchToWysiwyg();

        // Place the caret in one of the table cells.
        moveCaret("document.body.getElementsByTagName('table')[0].rows[0].cells[0].firstChild", 2);

        // Move the caret before the table and type some text. This time using Control+Up.
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_UP), "1");

        // Place the caret again in one of the table cells.
        moveCaret("document.body.getElementsByTagName('table')[0].rows[0].cells[0].firstChild", 2);

        // Move the caret before the table and type some text. This time using Meta+Up.
        // FIXME: Selenium doesn't simulate correctly the Meta key.
        // getRichTextArea().sendKeys(Keys.chord(Keys.META, Keys.ARROW_UP), "2");

        // Place the caret again in one of the table cells.
        moveCaret("document.body.getElementsByTagName('table')[0].rows[1].cells[1].firstChild", 3);

        // Move the caret after the table and type some text. This time using Control+Down.
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_DOWN), "4");

        // Place the caret again in one of the table cells.
        moveCaret("document.body.getElementsByTagName('table')[0].rows[1].cells[1].firstChild", 3);

        // Move the caret after the table and type some text. This time using Meta+Down.
        // FIXME: Selenium doesn't simulate correctly the Meta key.
        // getRichTextArea().sendKeys(Keys.chord(Keys.META, Keys.ARROW_DOWN), "3");

        switchToSource();
        assertSourceText("1\n\n|=Space|=Page\n|Main|WebHome\n\n4");
    }

    /**
     * @see XWIKI-4017: The close X button from the "Insert Table" dialog acts like the "Insert" button after a table
     *      has been inserted.
     */
    @Test
    public void testCancelInsertTable()
    {
        openInsertTableDialog();
        // Cancel the insert table operation.
        closeDialog();

        // Insert a default table this time.
        insertTable();

        // Move the caret after the table.
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_DOWN));

        openInsertTableDialog();
        // Cancel the insert table operation again.
        closeDialog();

        // Check the result.
        switchToSource();
        assertSourceText("|= |= \n| | \n");
    }

    /**
     * @see XWIKI-4231: "Enter" doesn't work in the Table Dialog
     */
    @Test
    public void testEnterInTableConfigDialog()
    {
        openInsertTableDialog();
        // Make sure the input fields have valid data.
        getSelenium().type(ROWS_SELECTOR, "1");
        getSelenium().type(COLUMNS_SELECTOR, "1");
        getSelenium().uncheck("//div[@class = 'xDialogBody']//input[@type = 'checkbox']");
        // Press Enter
        getSelenium().typeKeys(ROWS_SELECTOR, "\\13");
        // Check the result.
        waitForDialogToClose();
        switchToSource();
        assertSourceText("| ");
    }

    /**
     * Tests if the values entered on the table configuration dialog are validated and if proper validation message are
     * displayed.
     */
    @Test
    public void testValidateTableConfigDialog()
    {
        openInsertTableDialog();
        // Validation messages should not be present.
        assertFieldErrorIsNotPresent();
        // Enter invalid values.
        getSelenium().type(ROWS_SELECTOR, "");
        getSelenium().type(COLUMNS_SELECTOR, "0");
        // Try to submit.
        getSelenium().click("//button[text()=\"Insert Table\"]");
        // Check that the first input is in error
        assertFieldErrorIsPresent("Please enter a number greater than zero.", "//input[@title = 'Row count']");
        // Check that the second input is in error
        assertFieldErrorIsPresent("Please enter a number greater than zero.", "//input[@title = 'Column count']");
        // Check that there are actually 2 error messages visible
        assertEquals(2, getSelenium().getXpathCount("//div[contains(@class, 'xErrorMsg') and @style = '']"));
        // Fix the value of the first input.
        getSelenium().type(ROWS_SELECTOR, "1");
        // Try to submit again.
        getSelenium().click("//button[text()=\"Insert Table\"]");
        // Check if the validation message is present for the second field.
        assertFieldErrorIsPresent("Please enter a number greater than zero.", "//input[@title = 'Column count']");
        // Cancel the dialog and open it again.
        closeDialog();
        openInsertTableDialog();
        // The previous validation message should not be present.
        assertFieldErrorIsNotPresent();
        // The dialog should have preserved its state so try to submit again.
        getSelenium().click("//button[text()=\"Insert Table\"]");
        // Check if the validation message is present for the second field.
        assertFieldErrorIsPresent("Please enter a number greater than zero.", "//input[@title = 'Column count']");
        // Fix the error and submit.
        getSelenium().type(COLUMNS_SELECTOR, "1");
        getSelenium().click("//button[text()=\"Insert Table\"]");
        waitForDialogToClose();
        // Check the result.
        switchToSource();
        assertSourceText("|= ");
    }

    /**
     * @see XWIKI-7606: Cannot delete a table row when the selection spans across multiple cells
     */
    @Test
    public void testDeleteRowWhenSelectionSpansAcrossMultipleCells()
    {
        // Insert a table.
        switchToSource();
        setSourceText("|=A|=B|=C\n|1|2|3\n|x|y|z");
        switchToWysiwyg();

        // Select the text from the first two cells of the second row.
        select("document.body.getElementsByTagName('td')[0].firstChild", 0,
            "document.body.getElementsByTagName('td')[1].firstChild", 1);

        // Delete the row.
        clickMenu("Table");
        assertTrue(isMenuEnabled("Delete Row"));
        clickMenu("Delete Row");

        // Check the result.
        switchToSource();
        assertSourceText("|=A|=B|=C\n|x|y|z");
    }
}
