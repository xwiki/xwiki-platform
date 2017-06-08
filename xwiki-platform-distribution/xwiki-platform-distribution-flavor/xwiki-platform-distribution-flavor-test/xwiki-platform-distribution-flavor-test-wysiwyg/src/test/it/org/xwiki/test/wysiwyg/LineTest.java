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
 * Functional tests for line support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class LineTest extends AbstractWysiwygTestCase
{
    /**
     * Tests if the user can insert a line break in different contexts.
     */
    @Test
    public void testInsertLineBreak()
    {
        // Under body
        typeText("a");
        typeShiftEnter();
        typeText("b");
        switchToSource();
        assertSourceText("a\nb");

        setSourceText("");
        switchToWysiwyg();

        // Inside heading
        typeText("c");
        applyStyleTitle1();
        typeShiftEnter();
        typeText("d");
        switchToSource();
        assertSourceText("= c\nd =");

        setSourceText("");
        switchToWysiwyg();

        // Inside list item
        typeText("e");
        clickUnorderedListButton();
        typeShiftEnter();
        typeText("f");
        switchToSource();
        assertSourceText("* e\nf");

        // Inside table cell
        setSourceText("|h");
        switchToWysiwyg();
        typeText("g");
        typeShiftEnter();
        switchToSource();
        assertSourceText("|g\nh");

        // Inside table heading
        setSourceText("|=j");
        switchToWysiwyg();
        typeText("i");
        typeShiftEnter();
        switchToSource();
        assertSourceText("|=i\nj");

        // Inside paragraph
        setSourceText("l");
        switchToWysiwyg();
        typeText("k");
        typeShiftEnter();
        switchToSource();
        assertSourceText("k\nl");
    }

    /**
     * Tests if the user can split the current line of text.
     */
    @Test
    public void testSplitLine()
    {
        // Under body
        typeTextThenEnter("a");
        typeText("b");
        switchToSource();
        assertSourceText("a\n\nb");

        // Inside paragraph
        setSourceText("d");
        switchToWysiwyg();
        typeTextThenEnter("c");
        switchToSource();
        assertSourceText("c\n\nd");

        // Inside heading
        setSourceText("");
        switchToWysiwyg();
        applyStyleTitle2();
        typeTextThenEnter("e");
        typeText("f");
        switchToSource();
        assertSourceText("== e ==\n\nf");
    }

    /**
     * Tests if the user can split the current line when the caret is after a line break.
     */
    @Test
    public void testSplitLineAfterLineBreak()
    {
        // Under body
        typeText("a");
        typeShiftEnter();
        typeEnter();
        typeText("b");
        switchToSource();
        assertSourceText("a\n\nb");

        // Inside paragraph
        setSourceText("d");
        switchToWysiwyg();
        typeText("c");
        typeShiftEnter();
        typeEnter();
        switchToSource();
        assertSourceText("c\n\nd");

        // Inside heading
        setSourceText("");
        switchToWysiwyg();
        typeText("e");
        applyStyleTitle3();
        typeShiftEnter();
        typeEnter();
        typeText("f");
        switchToSource();
        assertSourceText("=== e ===\n\nf");
    }

    /**
     * Tests if pressing Enter at the beginning of a paragraph or heading inserts an empty line before them.
     * 
     * @see XWIKI-3035: Cannot type more than 2 consecutive &lt;enter&gt; in IE6, following are ignored
     */
    @Test
    public void testInsertEmptyLine()
    {
        // Before paragraph
        typeText("a");
        typeEnter(2);
        typeText("b");
        switchToSource();
        assertSourceText("a\n\n\nb");

        // Before heading
        setSourceText("");
        switchToWysiwyg();
        typeTextThenEnter("c");
        applyStyleTitle4();
        typeEnter();
        typeText("d");
        switchToSource();
        assertSourceText("c\n\n\n==== d ====");
    }

    /**
     * @see XWIKI-3573: Title style removed after hitting return then backspace at the beginning of a line
     */
    @Test
    public void testRemoveEmptyLineBefore()
    {
        // Remove empty lines before header.
        // Create the header first.
        typeText("x");
        applyStyleTitle5();
        // Place the caret at the beginning of the line
        moveCaret("document.body.firstChild.firstChild", 0);
        // Insert two empty lines before.
        typeEnter(2);
        // Remove them.
        typeBackspace(2);
        // Check the result.
        switchToSource();
        assertSourceText("===== x =====");
        switchToWysiwyg();

        // Remove empty lines before paragraph
        // Create the paragraph.
        triggerToolbarUpdate();
        applyStylePlainText();
        // Insert two empty lines before.
        typeEnter(2);
        // Remove them.
        typeBackspace(2);
        // Check the result.
        switchToSource();
        assertSourceText("x");
    }

    /**
     * @see XWIKI-2996: Text area sometimes loses focus when pressing Enter on an empty line
     */
    @Test
    public void testEnterOnEmptyLine()
    {
        typeEnter();
        typeText("foobar");
        applyStyleTitle1();
        typeEnter();
        typeText("x");
        switchToSource();
        assertSourceText("\n= foobar =\n\nx");
    }

    /**
     * @see XWIKI-3011: Different behavior of &lt;enter&gt; on FF2.0 and FF3.0
     */
    @Test
    public void testParagraphs()
    {
        typeTextThenEnter("a");
        typeTextThenEnter("b");
        typeText("c");
        switchToSource();
        assertSourceText("a\n\nb\n\nc");
    }

    /**
     * @see XWIKI-2991: Editor is losing the focus when pressing enter after an image
     */
    @Test
    public void testEnterAfterImage()
    {
        clickMenu(ImageTest.MENU_IMAGE);
        clickMenu(ImageTest.MENU_INSERT_ATTACHED_IMAGE);

        waitForDialogToLoad();
        // wait for the main step of the dialog to load
        waitForElement("//*[contains(@class, 'xSelectorAggregatorStep')]");
        // click the 'All pages' tab
        getSelenium().click("//div[. = 'All pages']");
        String imageSpace = "XWiki";
        waitForElement(ImageTest.SPACE_SELECTOR + "/option[@value = '" + imageSpace + "']");
        getSelenium().select(ImageTest.SPACE_SELECTOR, imageSpace);
        String imagePage = "AdminSheet";
        waitForElement(ImageTest.PAGE_SELECTOR + "/option[@value = '" + imagePage + "']");
        getSelenium().select(ImageTest.PAGE_SELECTOR, imagePage);
        getSelenium().click("//div[@class = 'xPageChooser']//button[. = 'Update']");
        String imageSelector = "//div[@class = 'xImagesSelector']//img[@title = 'import.png']";
        waitForElement(imageSelector);
        getSelenium().click(imageSelector);
        clickButtonWithText("Select");
        waitForElement("//*[contains(@class, 'xImageConfig')]");
        clickButtonWithText("Insert Image");
        waitForDialogToClose();

        // Make sure the editor is focused.
        getRichTextArea().sendKeys("");

        // The inserted image should be selected. By pressing the right arrow key the caret is not moved after the image
        // thus we are forced to collapse the selection to the end.
        getRichTextArea().executeScript("window.getSelection().collapseToEnd()");
        // If the editor loses focus after pressing Enter then the next typed text won't be submitted.
        getRichTextArea().sendKeys(Keys.RETURN, "xyz");

        // Submit the changes. If the editor lost focus after pressing Enter then the next line has no effect.
        blurRichTextArea();
        clickEditPageInWikiSyntaxEditor();
        // Check the result.
        assertEquals("[[image:XWiki.AdminSheet@import.png]]\n\nxyz", getFieldValue("content"));
    }

    /**
     * Creates two paragraphs, makes a selection that spans both paragraphs and then presses Enter.
     */
    @Test
    public void testEnterOnCrossParagraphSelection()
    {
        // Creates the two paragraphs.
        typeText("ab");
        applyStyleTitle1();
        applyStylePlainText();
        typeEnter();
        typeText("cd");

        // Make a cross paragraph selection.
        select("document.body.firstChild.firstChild", 1, "document.body.lastChild.firstChild", 1);

        // Press Enter.
        typeEnter();
        switchToSource();
        assertSourceText("a\n\nd");
    }

    /**
     * Inserts a table, types some text in each cell, makes a selection that spans some table cells and then presses
     * Enter.
     */
    @Test
    public void testEnterOnCrossTableCellSelection()
    {
        insertTable();

        // Fill the table.
        // Delete the non-breaking space that is found by default in empty table cells.
        typeBackspace();
        typeText("ab");
        typeTab();
        typeText("cd");
        // Delete the non-breaking space that is found by default in empty table cells.
        typeDelete();

        // Make a cross table cell selection.
        select("document.body.firstChild.rows[0].cells[0].firstChild", 1,
            "document.body.firstChild.rows[0].cells[1].firstChild", 1);

        // Press Enter.
        typeEnter();
        typeText("x");
        switchToSource();
        assertSourceText("|=a\nx|=d\n| | ");
    }

    /**
     * @see XWIKI-3109: Headers generated from wiki syntax look and behave differently
     */
    @Test
    public void testEnterInHeader()
    {
        typeText("H1");
        applyStyleTitle1();
        switchToSource();
        assertSourceText("= H1 =");
        switchToWysiwyg();

        // Place the caret in the middle of the header.
        moveCaret("document.body.firstChild.firstChild", 1);

        // Type some text to update the tool bar.
        typeText("#");

        // See if the header is detected.
        waitForStyleDetected("Title 1");

        // Press enter to split the header and generate a paragraph.
        typeEnter();

        // See if the paragraph is detected.
        waitForStyleDetected("Plain text");

        switchToSource();
        assertSourceText("= H# =\n\n1");
    }

    /**
     * Tests how the Enter key behaves inside a list item with complex content.
     */
    @Test
    public void testEnterInListItem()
    {
        typeText("a");
        clickUnorderedListButton();
        applyStyleTitle1();

        // Shift+Enter
        typeShiftEnter();
        typeText("b");

        // Control+Enter
        typeControlEnter();
        typeText("c");

        // Meta+Enter
        // FIXME: Selenium doesn't emulate correctly the Meta key.
        // typeMetaEnter();
        // typeText("d");

        // Enter
        typeEnter();
        typeText("e");

        // Check the result.
        switchToSource();
        assertSourceText("* (((\n= a\nb =\n\nc\n)))\n* e");
    }

    /**
     * @see XWIKI-4023: Cannot place the caret inside an empty heading.
     */
    @Test
    public void testEmptyHeadingsAreEditable()
    {
        switchToSource();
        setSourceText("before\n\n= =\n\nafter");
        switchToWysiwyg();
        // We can't test if the caret can be placed inside the empty heading because the selection is not updated on
        // fake events. We can only check the generated HTML for a BR spacer that will allow the user to write text
        // inside the heading.
        assertContent("<p>before</p><h1 id=\"H\"><span></span><br></h1><p>after</p>");
    }

    /**
     * Tests that splitting a line containing only invisible garbage generates two empty lines that can be edited.
     */
    @Test
    public void testSplitEmptyLineWithGarbage()
    {
        // Create a line that has only invisible garbage.
        setContent("<p><em></em><strong></strong></p>");
        // Move the caret between the garbage.
        moveCaret("document.body.firstChild", 1);
        // Split the line.
        typeEnter();
        typeText("x");
        // Check the result. The only way to test if the empty lines can be edited is to look for the BR spacers.
        assertContent("<p><em></em><br></p><p>x<strong></strong><br></p>");
    }

    /**
     * @see XWIKI-4193: When hitting Return at the end of the link the new line should not be a link.
     * @see XWIKI-3802: It is impossible to continue adding content if there is a link object at the bottom of the page.
     */
    @Test
    public void testEnterAtTheEndOfALink()
    {
        switchToSource();
        setSourceText("This is a [[link>>http://www.xwiki.org]]");
        switchToWysiwyg();
        // Move the caret at the end of the link.
        moveCaret("document.body.firstChild.lastChild.firstChild", 4);
        typeEnter();
        typeText("x");
        switchToSource();
        assertSourceText("This is a [[link>>http://www.xwiki.org]]\n\nx");
    }

    /**
     * @see XWIKI-3678: Justify not preserved when entering a new paragraph.
     */
    @Test
    public void testTextAlignmentIsPreservedOnANewParagraph()
    {
        // Create a level one heading.
        typeText("1");
        applyStyleTitle1();
        // Center the text.
        pushToolBarButton("Centered");
        // Create two paragraphs.
        typeEnter();
        typeTextThenEnter("2");
        typeText("34");
        // Split the last paragraph in half.
        moveCaret("document.body.lastChild.firstChild", 1);
        typeEnter();
        // Check the result.
        switchToSource();
        assertSourceText("(% style=\"text-align: center;\" %)\n= 1 =\n\n"
            + "(% style=\"text-align: center;\" %)\n2\n\n" + "(% style=\"text-align: center;\" %)\n3\n\n"
            + "(% style=\"text-align: center;\" %)\n4");
    }

    /**
     * @see XWIKI-2723: Empty paragraphs should not be displayed even if they have styles applied to them
     */
    @Test
    public void testEmptyParagraphsGenerateEmptyLines()
    {
        switchToSource();
        setSourceText("(% style=\"color: blue; text-align: center;\" %)\nHello world");
        switchToWysiwyg();

        // Place the caret after "Hello ".
        moveCaret("document.body.firstChild.firstChild", 6);

        typeEnter(3);

        switchToSource();
        assertSourceText("(% style=\"color: blue; text-align: center;\" %)\nHello\n\n\n\n"
            + "(% style=\"text-align: center;\" %)\nworld");
    }
}
