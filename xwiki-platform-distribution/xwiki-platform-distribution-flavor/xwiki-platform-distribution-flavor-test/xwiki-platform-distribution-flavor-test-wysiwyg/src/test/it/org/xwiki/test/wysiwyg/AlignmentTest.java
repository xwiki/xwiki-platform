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
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

/**
 * Functional tests for alignment support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class AlignmentTest extends AbstractWysiwygTestCase
{
    /**
     * The title of the tool bar button used to align text to the left.
     */
    public static final String TOOLBAR_BUTTON_ALIGN_LEFT_TITLE = "Align Left";

    /**
     * The title of the tool bar button used to center the text.
     */
    public static final String TOOLBAR_BUTTON_ALIGN_CENTER_TITLE = "Centered";

    /**
     * The title of the tool bar button used to align text to the right.
     */
    public static final String TOOLBAR_BUTTON_ALIGN_RIGHT_TITLE = "Align Right";

    /**
     * The title of the tool bar button used to justify text.
     */
    public static final String TOOLBAR_BUTTON_ALIGN_FULL_TITLE = "Justified";

    /**
     * Tests if the text directly under body can be aligned.
     */
    @Test
    public void testAlignBody()
    {
        clickAlignCenterButton();
        typeText("a");
        switchToSource();
        assertSourceText("(% style=\"text-align: center;\" %)\na");

        setSourceText("");
        switchToWysiwyg();

        typeText("a");
        // Wait for the tool bar to be updated, otherwise clicking on the toggle button has no effect.
        waitForToggleButton(TOOLBAR_BUTTON_ALIGN_RIGHT_TITLE, true);
        clickAlignRightButton();
        typeText("b");
        switchToSource();
        assertSourceText("(% style=\"text-align: right;\" %)\nab");

        setSourceText("");
        switchToWysiwyg();

        typeText("abc");
        select("document.body.firstChild", 1, "document.body.firstChild", 2);
        // Wait for the tool bar to be updated, otherwise clicking on the toggle button has no effect.
        waitForToggleButton(TOOLBAR_BUTTON_ALIGN_FULL_TITLE, true);
        clickAlignFullButton();
        typeText("x");
        switchToSource();
        assertSourceText("(% style=\"text-align: justify;\" %)\naxc");

        setSourceText("");
        switchToWysiwyg();

        typeText("a");
        typeShiftEnter();
        typeText("b");
        selectNode("document.body.childNodes[2]");
        // Wait for the tool bar to be updated, otherwise clicking on the toggle button has no effect.
        waitForToggleButton(TOOLBAR_BUTTON_ALIGN_LEFT_TITLE, true);
        clickAlignLeftButton();
        typeText("x");
        switchToSource();
        assertSourceText("(% style=\"text-align: left;\" %)\na\nx");
    }

    /**
     * Tests if a single paragraph can be aligned.
     */
    @Test
    public void testAlignParagraph()
    {
        // Create the paragraph.
        applyStyleTitle1();
        applyStylePlainText();

        // Wait for the tool bar to be updated.
        waitForPushButton(TOOLBAR_BUTTON_UNDO_TITLE, true);
        // Check the default alignment.
        boolean defaultAlignFull = isToggleButtonDown(TOOLBAR_BUTTON_ALIGN_FULL_TITLE);

        // Center text.
        clickAlignCenterButton();
        typeText("a");
        waitForAlignCenterDetected(true);
        switchToSource();
        assertSourceText("(% style=\"text-align: center;\" %)\na");

        // Assert again the center alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignCenterDetected(true);

        typeShiftEnter();
        typeText("b");
        clickAlignRightButton();
        waitForAlignRightDetected(true);
        switchToSource();
        assertSourceText("(% style=\"text-align: right;\" %)\na\nb");

        // Assert again the right alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignRightDetected(true);

        selectNode("document.body.firstChild.childNodes[2]");
        clickAlignFullButton();
        typeText("c");
        waitForAlignFullDetected(true);
        switchToSource();
        assertSourceText("(% style=\"text-align: justify;\" %)\na\nc");

        // Assert again the full alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignFullDetected(true);

        // Remove the full alignment (toggle full alignment off).
        clickAlignFullButton();
        // If paragraphs are justified by default then the "Justified" button remains toggled.
        waitForAlignFullDetected(defaultAlignFull);
        switchToSource();
        assertSourceText("a\nc");
        switchToWysiwyg();

        typeText("x");
        clickAlignLeftButton();
        waitForAlignLeftDetected(true);
        switchToSource();
        assertSourceText("(% style=\"text-align: left;\" %)\na\ncx");

        // Assert again the left alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignLeftDetected(true);
    }

    /**
     * Tests if a table cell can be aligned.
     */
    @Test
    public void testAlignTableCell()
    {
        switchToSource();
        setSourceText("|=a|=b\n|c|d");
        switchToWysiwyg();
        clickAlignRightButton();
        waitForAlignRightDetected(true);
        switchToSource();
        assertSourceText("|=(% style=\"text-align: right;\" %)a|=b\n|c|d");

        // Assert again the right alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignRightDetected(true);

        typeTextThenEnter("x");
        clickAlignFullButton();
        waitForAlignFullDetected(true);
        switchToSource();
        assertSourceText("|=(% style=\"text-align: justify;\" %)x\na|=b\n|c|d");
        switchToWysiwyg();

        selectNodeContents("document.body.getElementsByTagName('td')[0]");
        clickAlignCenterButton();
        waitForAlignCenterDetected(true);
        switchToSource();
        assertSourceText("|=(% style=\"text-align: justify;\" %)x\na|=b\n|(% style=\"text-align: center;\" %)c|d");
        switchToWysiwyg();

        selectNodeContents("document.body.getElementsByTagName('td')[0]");
        waitForAlignCenterDetected(true);
    }

    /**
     * Tests if more paragraphs can be aligned at once.
     */
    @Test
    public void testAlignParagraphs()
    {
        switchToSource();
        setSourceText("ab\n\ncd");
        switchToWysiwyg();

        moveCaret("document.body.getElementsByTagName('p')[0].firstChild", 1);
        clickAlignCenterButton();
        waitForAlignCenterDetected(true);

        select("document.body.getElementsByTagName('p')[0].firstChild", 1,
            "document.body.getElementsByTagName('p')[1].firstChild", 1);
        waitForAlignCenterDetected(false);
        clickAlignRightButton();
        waitForAlignRightDetected(true);
        switchToSource();
        assertSourceText("(% style=\"text-align: right;\" %)\nab\n\n(% style=\"text-align: right;\" %)\ncd");

        // Assert again the right alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignRightDetected(true);

        // Remove the right alignment (toggle off the 'Align right' button).
        select("document.body.getElementsByTagName('p')[0].firstChild", 1,
            "document.body.getElementsByTagName('p')[1].firstChild", 1);
        clickAlignRightButton();
        waitForAlignRightDetected(false);
        switchToSource();
        assertSourceText("ab\n\ncd");
    }

    /**
     * Tests if more table cells can be aligned at once.
     */
    @Test
    public void testAlignTableCells()
    {
        switchToSource();
        setSourceText("|ab|cd");
        switchToWysiwyg();

        moveCaret("document.body.getElementsByTagName('td')[1].firstChild", 2);
        clickAlignRightButton();
        waitForAlignRightDetected(true);

        select("document.body.getElementsByTagName('td')[0].firstChild", 0,
            "document.body.getElementsByTagName('td')[1].firstChild", 1);
        waitForAlignRightDetected(false);
        clickAlignCenterButton();
        waitForAlignCenterDetected(true);
        switchToSource();
        assertSourceText("|(% style=\"text-align: center;\" %)ab|(% style=\"text-align: center;\" %)cd");

        // Assert again the center alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignCenterDetected(true);

        // Remove the center alignment (toggle off the 'Align center' button).
        select("document.body.getElementsByTagName('td')[0].firstChild", 1,
            "document.body.getElementsByTagName('td')[1].firstChild", 2);
        clickAlignCenterButton();
        waitForAlignCenterDetected(false);
        switchToSource();
        assertSourceText("|ab|cd");
    }

    /**
     * Makes a selection that includes a paragraph and a table cell and aligns them.
     */
    @Test
    public void testSelectAndAlignParagraphAndTableCell()
    {
        switchToSource();
        setSourceText("ab\nxy\n\n|cd\n12|ef");
        switchToWysiwyg();

        // Align the paragraph to the right.
        clickAlignRightButton();
        waitForAlignRightDetected(true);

        // Select the paragraph and the first table cell and align them full.
        select("document.body.getElementsByTagName('p')[0].lastChild", 2,
            "document.body.getElementsByTagName('td')[0].firstChild", 0);
        waitForAlignRightDetected(false);
        clickAlignFullButton();
        waitForAlignFullDetected(true);
        switchToSource();
        assertSourceText("(% style=\"text-align: justify;\" %)\nab\nxy\n\n|(% style=\"text-align: justify;\" %)cd\n12|ef");

        // Assert again the full alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignFullDetected(true);

        // Remove the full alignment (toggle off the 'Align full' button).
        select("document.body.getElementsByTagName('p')[0].firstChild", 1,
            "document.body.getElementsByTagName('td')[0].lastChild", 0);
        clickAlignFullButton();
        waitForAlignFullDetected(false);
        switchToSource();
        assertSourceText("ab\nxy\n\n|cd\n12|ef");
    }

    /**
     * Tests if a paragraph inside a table cell can be aligned.
     */
    @Test
    public void testAlignParagraphInsideTableCell()
    {
        switchToSource();
        setSourceText("|(((ab\n\ncd)))|ef");
        switchToWysiwyg();

        // Place the caret inside the first paragraph from the first table cell.
        moveCaret("document.body.getElementsByTagName('p')[0].firstChild", 2);
        clickAlignRightButton();
        waitForAlignRightDetected(true);
        switchToSource();
        assertSourceText("|(((\n(% style=\"text-align: right;\" %)\nab\n\ncd\n)))|ef");

        // Assert again the right alignment after coming back to WYSIWYG editor.
        switchToWysiwyg();
        waitForAlignRightDetected(true);
    }

    /**
     * Clicks the 'Align left' button from the tool bar.
     */
    protected void clickAlignLeftButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_ALIGN_LEFT_TITLE);
    }

    /**
     * Clicks the 'Align center' button from the tool bar.
     */
    protected void clickAlignCenterButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_ALIGN_CENTER_TITLE);
    }

    /**
     * Clicks the 'Align right' button from the tool bar.
     */
    protected void clickAlignRightButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_ALIGN_RIGHT_TITLE);
    }

    /**
     * Clicks the 'Align full' button from the tool bar.
     */
    protected void clickAlignFullButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_ALIGN_FULL_TITLE);
    }

    /**
     * Waits for the left alignment toggle button to have the specified state.
     * 
     * @param detected {@code true} to wait till the left alignment is detected, {@code false} to wait till it is
     *            undetected
     */
    protected void waitForAlignLeftDetected(boolean detected)
    {
        triggerToolbarUpdate();
        waitForToggleButtonState(TOOLBAR_BUTTON_ALIGN_LEFT_TITLE, detected);
    }

    /**
     * Waits for the centered alignment toggle button to have the specified state.
     * 
     * @param detected {@code true} to wait till the centered alignment is detected, {@code false} to wait till it is
     *            undetected
     */
    protected void waitForAlignCenterDetected(boolean detected)
    {
        triggerToolbarUpdate();
        waitForToggleButtonState(TOOLBAR_BUTTON_ALIGN_CENTER_TITLE, detected);
    }

    /**
     * Waits for the right alignment toggle button to have the specified state.
     * 
     * @param detected {@code true} to wait till the right alignment is detected, {@code false} to wait till it is
     *            undetected
     */
    protected void waitForAlignRightDetected(boolean detected)
    {
        triggerToolbarUpdate();
        waitForToggleButtonState(TOOLBAR_BUTTON_ALIGN_RIGHT_TITLE, detected);
    }

    /**
     * Waits for the justified alignment toggle button to have the specified state.
     * 
     * @param detected {@code true} to wait till the justified alignment is detected, {@code false} to wait till it is
     *            undetected
     */
    protected void waitForAlignFullDetected(boolean detected)
    {
        triggerToolbarUpdate();
        waitForToggleButtonState(TOOLBAR_BUTTON_ALIGN_FULL_TITLE, detected);
    }
}
