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
 * Tests for the custom list support, to handle and generate valid XHTML lists in the wysiwyg. At the moment, this class
 * tests processing the rendered lists rather than creating new lists from the wysiwyg, to ensure that valid rendered
 * lists are managed correctly.
 * 
 * @version $Id$
 */
public class ListTest extends AbstractWysiwygTestCase
{
    /**
     * @see XWIKI-2734: Cannot edit the outer list item. The test is not deeply relevant as we are positioning the range
     *      programatically. The correct test would prove that the caret can be positioned there with the keys.
     */
    @Test
    public void testEmptyListItemsEditable()
    {
        switchToSource();
        setSourceText("** rox");
        switchToWysiwyg();
        // Check that a br is added in the parent list item so that it becomes editable
        assertContent("<ul><li><br><ul><li>rox</li></ul></li></ul>");
        // Place the caret in the first list item
        moveCaret("document.body.firstChild.firstChild", 0);
        typeText("x");
        assertContent("<ul><li>x<br><ul><li>rox</li></ul></li></ul>");
    }

    /**
     * Test the case when hitting enter in a list item before a sublist, that it creates an editable list item under.
     * The test is not deeply relevant since we are positioning the range in the item under programatically. The correct
     * test would prove that the caret can be positioned there with the keys.
     */
    @Test
    public void testEnterBeforeSublist()
    {
        switchToSource();
        setSourceText("* x\n** rox");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 1);
        typeEnter();
        // Check the created item is editable
        assertContent("<ul><li>x</li><li><br><ul><li>rox</li></ul></li></ul>");
        moveCaret("document.body.firstChild.childNodes[1]", 0);
        typeText("w");
        assertContent("<ul><li>x</li><li>w<br><ul><li>rox</li></ul></li></ul>");
    }

    /**
     * Test the midas bug which causes the list items in a list to be replaced with an empty list and the caret to be
     * left inside the ul, not editable.
     */
    @Test
    public void testEnterOnEntireList()
    {
        switchToSource();
        setSourceText("* foo\n* bar");
        switchToWysiwyg();
        // Set the selection around the list
        select("document.body.firstChild.firstChild.firstChild", 0, "document.body.firstChild.lastChild.firstChild", 3);
        typeEnter();
        typeText("x");
        assertContent("<p><br></p>x");
    }

    /**
     * Test delete works fine inside a list item, and before another element (such as bold).
     */
    @Test
    public void testDeleteInsideItem()
    {
        switchToSource();
        setSourceText("* foo**bar**\n** far");
        switchToWysiwyg();
        // Set the selection inside the foo text
        moveCaret("document.body.firstChild.firstChild.firstChild", 1);
        typeDelete();
        assertContent("<ul><li>fo<strong>bar</strong><ul><li>far</li></ul></li></ul>");

        // set the selection just before the bold text but inside the text before
        moveCaret("document.body.firstChild.firstChild.firstChild", 2);
        typeDelete();
        assertContent("<ul><li>fo<strong>ar</strong><ul><li>far</li></ul></li></ul>");
    }

    /**
     * Test backspace works fine inside a list item, and after another element (such as italic).
     */
    @Test
    public void testBackspaceInsideItem()
    {
        switchToSource();
        setSourceText("* foo\n** b//arf//ar");
        switchToWysiwyg();
        // Set the selection in the "ar" text in the second list item
        moveCaret("document.body.firstChild.firstChild.lastChild.firstChild.lastChild", 1);
        typeBackspace();
        assertContent("<ul><li>foo<ul><li>b<em>arf</em>r</li></ul></li></ul>");
        // delete again, now it should delete inside the em
        typeBackspace();
        assertContent("<ul><li>foo<ul><li>b<em>ar</em>r</li></ul></li></ul>");
    }

    /**
     * Test that the delete at the end of the list works fine
     */
    @Test
    public void testDeleteInSameList()
    {
        switchToSource();
        setSourceText("* foo\n* bar");
        switchToWysiwyg();
        // Set the selection at the end of the first item
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>foobar</li></ul>");
    }

    /**
     * Test that the backspace at the beginning of the second item in a list moves the items together in the first list
     * item.
     */
    @Test
    public void testBackspaceInSameList()
    {
        switchToSource();
        setSourceText("* foo\n* bar");
        switchToWysiwyg();
        // Set the selection at the end of the first item
        moveCaret("document.body.firstChild.lastChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foobar</li></ul>");
    }

    /**
     * Test that delete at the end of a list preserves browser default behaviour: for firefox is to join the two lists. <br>
     * TODO: re-activate when https://bugzilla.mozilla.org/show_bug.cgi?id=519751 will be fixed
     */
    public void failingTestDeleteInDifferentLists()
    {
        switchToSource();
        setSourceText("* foo\n\n* bar");
        switchToWysiwyg();
        // Set the selection at the end of the first item
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>foo</li><li>bar</li></ul>");
    }

    /**
     * Test that backspace at the beginning of a list preserves browser default behaviour: for firefox is to join the
     * two lists.
     */
    @Test
    public void testBackspaceInDifferentLists()
    {
        switchToSource();
        setSourceText("* foo\n\n* bar");
        switchToWysiwyg();
        // Set the selection at the end of the first item
        moveCaret("document.body.lastChild.firstChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foo</li><li>bar</li></ul>");
    }

    /**
     * Test that backspace at the beginning of a list after another list in an embedded document (two lists on the
     * second level) preserves default behaviour: for firefox is to join the two lists.
     */
    @Test
    public void testBackspaceInEmbeddedDocumentDifferentLists()
    {
        switchToSource();
        setSourceText("* foo\n* bar (((\n* foo 2\n1. bar 2)))");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.childNodes[1].childNodes[1].childNodes[1].firstChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foo</li><li>bar<div><ul><li>foo 2</li><li>bar 2</li></ul></div></li></ul>");
    }

    /**
     * Test that delete at the end of a list before another list in an embedded document (two lists on the second level)
     * preserves default behaviour: for firefox is to join the two lists. <br>
     * TODO: re-activate when https://bugzilla.mozilla.org/show_bug.cgi?id=519751 will be fixed
     */
    public void failingTestDeleteInEmbeddedDocumentDifferentLists()
    {
        switchToSource();
        setSourceText("* foo\n* bar (((\n1. foo 2\n* bar 2)))");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.childNodes[1].childNodes[1].firstChild.firstChild.firstChild", 5);
        typeDelete();
        assertContent("<ul><li>foo</li><li>bar<div><ol><li>foo 2</li><li>bar 2</li></ol></div></li></ul>");
    }

    @Test
    public void testBackspaceInEmbeddedDocumentList()
    {
        switchToSource();
        setSourceText("* foo(((bar\n* foar)))");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.childNodes[1].childNodes[1].firstChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foo<div><p>bar</p>foar</div></li></ul>");
    }

    /**
     * TODO: re-activate when https://bugzilla.mozilla.org/show_bug.cgi?id=519751 will be fixed
     */
    public void failingTestBackspaceAndDeleteToMergeEmbeddedDocumentListAndParagraph()
    {
        switchToSource();
        setSourceText("* foo(((bar\n* foar)))");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.childNodes[1].childNodes[1].firstChild.firstChild", 0);
        typeBackspace();
        typeDelete();
        assertContent("<ul><li>foo<div><p>barfoar</p></div></li></ul>");
    }

    /**
     * TODO: re-activate when https://bugzilla.mozilla.org/show_bug.cgi?id=519751 will be fixed
     */
    public void failingTestDeleteInEmbeddedDocumentList()
    {
        switchToSource();
        setSourceText("* foo(((* bar\n\nfoar)))");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.childNodes[1].firstChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>foo<div><ul><li>barfoar</li></ul></div></li></ul>");
    }

    /**
     * Tests that the delete moves the first item on another level in the item in which is executed.
     */
    @Test
    public void testDeleteBeforeSublist()
    {
        // 1/ with only one item -> the sublist should be removed
        switchToSource();
        setSourceText("* foo\n** bar\n");
        switchToWysiwyg();
        // Set the selection at the end of the first item
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>foobar</li></ul>");

        // 2/ with more than one item -> only the first item should be moved to the list above
        switchToSource();
        setSourceText("* foo\n** bar\n** far");
        switchToWysiwyg();
        // Set the selection at the end of the first item
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>foobar<ul><li>far</li></ul></li></ul>");
    }

    /**
     * Test that backspace at the beginning of an item in a sublist moves the item in the list item before, on a lower
     * list level.
     */
    @Test
    public void testBackspaceBeginSublist()
    {
        // 1/ with only one item -> the sublist should be deleted
        switchToSource();
        setSourceText("* foo\n** bar\n");
        switchToWysiwyg();
        // Set the selection at beginning of the first item in sublist
        moveCaret("document.body.firstChild.firstChild.lastChild.firstChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foobar</li></ul>");

        // 2/ with more than one item -> only the first item should be moved to the list above
        switchToSource();
        setSourceText("* foo\n** bar\n** far");
        switchToWysiwyg();
        // Set the selection at beginning of the first item in sublist
        moveCaret("document.body.firstChild.firstChild.lastChild.firstChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foobar<ul><li>far</li></ul></li></ul>");
    }

    /**
     * Test that deleting at the end of a list item with a sublist with another sublist inside, moves the first sublist
     * and the elements on level 3 are moved to level 2.
     */
    @Test
    public void testDeleteDecreasesLevelWithEmptyItem()
    {
        switchToSource();
        setSourceText("* foo\n*** bar\n");
        switchToWysiwyg();
        // Set the selection at beginning of the first item in sublist
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>foo<br><ul><li>bar</li></ul></li></ul>");
    }

    /**
     * Test that hitting backspace at the beginning of a list item with a sublist moves this element in its parent list
     * item and decreases the level of the subitems.
     */
    @Test
    public void testBackspaceDecreasesLevelWithEmptyItem()
    {
        switchToSource();
        setSourceText("* foo\n*** bar\n");
        switchToWysiwyg();
        // Set the selection at beginning of the first item in sublist
        moveCaret("document.body.firstChild.firstChild.lastChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foo<br><ul><li>bar</li></ul></li></ul>");
    }

    /**
     * Test delete at the end of a sublist item on a higher level moves the list item on the lower level inside it.
     * 
     * @see XWIKI-3114: Backspace is ignored at the beginning of a list item if the previous list item is on a lower
     *      level.
     */
    @Test
    public void testDeleteBeforePreviousLevelItem()
    {
        switchToSource();
        setSourceText("* foo\n** bar\n* bar minus one");
        switchToWysiwyg();
        // Set the selection at the end of "bar"
        moveCaret("document.body.firstChild.firstChild.lastChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>foo<ul><li>barbar minus one</li></ul></li></ul>");
    }

    /**
     * Test backspace at the beginning of a sublist item before a sublist moves the item on the lower level to the item
     * on the higher level.
     * 
     * @see XWIKI-3114: Backspace is ignored at the beginning of a list item if the previous list item is on a lower
     *      level.
     */
    @Test
    public void testBackspaceAfterPreviousLevelItem()
    {
        switchToSource();
        setSourceText("* foo\n** bar\n* bar minus one");
        switchToWysiwyg();
        // Set the selection at the end of "bar"
        moveCaret("document.body.firstChild.lastChild.firstChild", 0);
        typeBackspace();
        assertContent("<ul><li>foo<ul><li>barbar minus one</li></ul></li></ul>");
    }

    /**
     * Test deleting the last piece of text inside a list item with sublists, keeps the remaining list item empty but
     * editable. The test is weak, since we position the range programatically. The true test should try to navigate to
     * the list item.
     */
    @Test
    public void testDeleteAllTextInListItem()
    {
        switchToSource();
        setSourceText("* foo\n* b\n** ar");
        switchToWysiwyg();

        // Set the selection at the beginning of the text in the second list item
        moveCaret("document.body.firstChild.lastChild.firstChild", 0);
        typeDelete();
        // test that the list structure is correct: two items one with a sublist
        assertContent("<ul><li>foo</li><li><br><ul><li>ar</li></ul></li></ul>");
        // type in the empty list item
        moveCaret("document.body.firstChild.lastChild", 0);
        typeText("x");
        assertContent("<ul><li>foo</li><li>x<br><ul><li>ar</li></ul></li></ul>");
        // now delete, to test that it jumps the <br>
        typeDelete();
        assertContent("<ul><li>foo</li><li>xar</li></ul>");
    }

    /**
     * Test backspacing the last piece of text inside a list item with sublists, keeps the remaining list item empty but
     * editable. The test is weak, since we position the range programatically. The true test should try to navigate to
     * the list item.
     */
    @Test
    public void testBackspaceAllTextInListItem()
    {
        switchToSource();
        setSourceText("* foo\n* b\n** ar");
        switchToWysiwyg();

        // Set the selection at the end of the text in the second list item
        moveCaret("document.body.firstChild.lastChild.firstChild", 1);
        typeBackspace();
        // test that the list structure is correct: two items one with a sublist
        assertContent("<ul><li>foo</li><li><br><ul><li>ar</li></ul></li></ul>");
        // type in the empty list item
        moveCaret("document.body.firstChild.lastChild", 0);
        typeText("x");
        assertContent("<ul><li>foo</li><li>x<br><ul><li>ar</li></ul></li></ul>");
        // Put the caret at the beginning of the sublist
        moveCaret("document.body.firstChild.lastChild.lastChild.firstChild.firstChild", 0);
        // now backspace, to test that it jumps the <br>
        typeBackspace();
        assertContent("<ul><li>foo</li><li>xar</li></ul>");
    }

    /**
     * Test delete before text outside lists. <br>
     * TODO: re-activate when https://bugzilla.mozilla.org/show_bug.cgi?id=519751 will be fixed
     */
    public void failingTestDeleteBeforeParagraph()
    {
        switchToSource();
        setSourceText("* one\n* two\n\nFoobar");
        switchToWysiwyg();

        // Set the selection at the end of the "two" list item
        moveCaret("document.body.firstChild.lastChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>one</li><li>twoFoobar</li></ul>");

        // now run the case with delete in a sublist
        switchToSource();
        setSourceText("* one\n** two\n\nFoobar");
        switchToWysiwyg();

        // Set the selection at the end of the "two" list item
        moveCaret("document.body.firstChild.firstChild.lastChild.firstChild.firstChild", 3);
        typeDelete();
        assertContent("<ul><li>one<ul><li>twoFoobar</li></ul></li></ul>");
    }

    /**
     * Test backspace at the beginning of list item, after text outside lists.
     */
    @Test
    public void testBackspaceAfterParagraph()
    {
        switchToSource();
        setSourceText("Foobar\n\n* one\n* two");
        switchToWysiwyg();

        // Set the selection at the beginning of the "one" list item
        moveCaret("document.body.lastChild.firstChild.firstChild", 0);
        typeBackspace();
        assertContent("<p>Foobarone</p><ul><li>two</li></ul>");

        // Now test the case when the list has a sublist, in which case FF keeps the sublist parent, as empty and
        // editable
        // Note that this behaves differently on Internet Explorer, unwrapping the sublist
        switchToSource();
        setSourceText("Foobar\n\n* one\n** two");
        switchToWysiwyg();

        // Set the selection at the beginning of the "one" list item
        moveCaret("document.body.lastChild.firstChild.firstChild", 0);
        typeBackspace();
        assertContent("<p>Foobarone</p><ul><li><br><ul><li>two</li></ul></li></ul>");
    }

    /**
     * Test deleting the whole selection on a list, on multiple list levels keeps the list valid. Test that the parents
     * of the indented list items that stay are editable.
     */
    @Test
    public void testDeleteSelectionPreserveSublists()
    {
        switchToSource();
        setSourceText("* one\n** two\n** three\n*** four\n*** five");
        switchToWysiwyg();

        // Set the selection starting in the one element and ending in the four element
        select("document.body.firstChild.firstChild.firstChild", 2,
            "document.body.firstChild.firstChild.lastChild.lastChild.lastChild.firstChild.firstChild", 2);
        typeDelete();
        assertContent("<ul><li>onur<ul><li><br><ul><li>five</li></ul></li></ul></li></ul>");
    }

    /**
     * Test deleting the whole selection on a list, on multiple list levels deletes all the fully enclosed list items
     * and lists, and keeps the result in a single list item if the selection ends are on the same list level.
     */
    @Test
    public void testDeleteSelectionDeletesEnclosedSublists()
    {
        switchToSource();
        setSourceText("* one\n** two\n** three\n*** four\n** five\n* six");
        switchToWysiwyg();

        // Set the selection starting in the one element and ending in the six element
        select("document.body.firstChild.firstChild.firstChild", 2, "document.body.firstChild.lastChild.firstChild", 1);
        typeDelete();
        assertContent("<ul><li>onix</li></ul>");
    }

    /**
     * Test creating a list with two items and indenting the second item. The indented item should be a sublist of the
     * first item and the resulted HTML should be valid.
     */
    @Test
    public void testIndentNoSublist()
    {
        typeText("1");
        clickUnorderedListButton();
        typeEnter();
        typeText("2");
        assertContent("<ul><li>1</li><li>2<br></li></ul>");
        // The indent tool bar button is disabled when the caret is not inside a list. We have to wait for the indent
        // tool bar button to become enabled because the tool bar is updated with delay.
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        assertContent("<ul><li>1<ul><li>2<br></li></ul></li></ul>");
        // test that the indented item cannot be indented once more
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, false);
        moveCaret("document.body.firstChild.firstChild.childNodes[1].firstChild.firstChild", 0);
        typeTab();
        // check that nothing happened
        assertContent("<ul><li>1<ul><li>2<br></li></ul></li></ul>");

        switchToSource();
        assertSourceText("* 1\n** 2");
    }

    /**
     * Test that indenting an item to the second level under a list item with a list already on the second level unifies
     * the two lists.
     */
    @Test
    public void testIndentUnderSublist()
    {
        typeText("1");
        clickUnorderedListButton();
        typeEnter();
        typeTextThenEnter("2");
        typeTab();
        typeTextThenEnter("+");
        typeShiftTab();
        typeText("3");
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        assertContent("<ul><li>1</li><li>2<ul><li>+</li><li>3<br></li></ul></li></ul>");
        switchToSource();
        assertSourceText("* 1\n* 2\n** +\n** 3");
    }

    /**
     * Test indenting (using the tab key) an item with a sublist, that the child sublist is indented with its parent.
     * 
     * @see XWIKI-3118: Indenting a list item with a sublist works incorrectly.
     * @see XWIKI-3117: Shift + Tab does works incorrect on an item that contains a sublist.
     */
    @Test
    public void testIndentOutdentWithSublist()
    {
        typeText("1");
        clickUnorderedListButton();
        typeEnter();
        typeText("2");
        // The indent tool bar button is disabled when the caret is not inside a list. We have to wait for the indent
        // tool bar button to become enabled because the tool bar is updated with delay.
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        // move to the end of the "1" element, hit enter, tab and type. Should create a new list item, parent of the "2"
        // sublist, tab should indent and type should add content
        moveCaret("document.body.firstChild.firstChild.firstChild", 1);
        typeText("3");
        moveCaret("document.body.firstChild.firstChild.firstChild", 1);
        typeEnter();
        assertContent("<ul><li>1</li><li>3<ul><li>2<br></li></ul></li></ul>");
        // Check that the list item is indentable i.e. the list plugin is correctly recognizing lists (XWIKI-3061).
        // The tool bar is not updated instantly and thus we have to wait for the indent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        assertContent("<ul><li>1<ul><li>3<ul><li>2<br></li></ul></li></ul></li></ul>");
        switchToSource();
        assertSourceText("* 1\n** 3\n*** 2");
        switchToWysiwyg();
        // select second element "3"
        select("document.body.firstChild.firstChild.childNodes[1].firstChild.firstChild", 0,
            "document.body.firstChild.firstChild.childNodes[1].firstChild.firstChild", 1);
        // Check that the list item is outdentable i.e. the list plugin is correctly recognizing lists (XWIKI-3061).
        // The tool bar is not updated instantly and thus we have to wait for the outdent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_OUTDENT_TITLE, true);
        clickOutdentButton();
        assertContent("<ul><li>1</li><li>3<ul><li>2<br></li></ul></li></ul>");
        moveCaret("document.body.firstChild.childNodes[1].childNodes[1].firstChild.firstChild", 0);
        clickOutdentButton();
        assertContent("<ul><li>1</li><li>3</li><li>2<br></li></ul>");
        switchToSource();
        assertSourceText("* 1\n* 3\n* 2");
    }

    /**
     * Test outdenting an item on the first level in a list: it should split the list in two and put the content of the
     * unindented item in between.
     */
    @Test
    public void testOutdentOnFirstLevel()
    {
        typeText("1");
        clickUnorderedListButton();
        typeEnter();
        typeTextThenEnter("2");
        typeTab();
        typeTextThenEnter("+");
        typeShiftTab();
        typeText("3");
        // move caret at the beginning of the "two" item
        moveCaret("document.body.firstChild.childNodes[1].firstChild", 0);
        typeShiftTab();
        assertContent("<ul><li>1</li></ul><p>2</p><ul><li>+</li><li>3<br></li></ul>");
        switchToSource();
        assertSourceText("* 1\n\n2\n\n* +\n* 3");
    }

    /**
     * Test outdenting an item on the second level inside a list item which also contains content after the sublist
     * correctly moves the content in the outdented list item.
     */
    @Test
    public void testOutdentWithContentAfter()
    {
        setContent("<ul><li>one<br>before<ul><li>two</li><li>three</li><li>four</li></ul>after</li></ul>");
        moveCaret("document.body.firstChild.firstChild.childNodes[3].childNodes[1].firstChild", 0);
        // The tool bar is not updated instantly and thus we have to wait for the outdent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_OUTDENT_TITLE, true);
        clickOutdentButton();
        assertContent("<ul><li>one<br>before<ul><li>two</li></ul></li><li>three<ul><li>four</li></ul>"
            + "after</li></ul>");
    }

    /**
     * @see XWIKI-3447: List detection is reversed
     */
    @Test
    public void testListDetection()
    {
        switchToSource();
        setSourceText("before\n\n" + "* unordered list item\n*1. ordered sub-list item\n\n"
            + "1. ordered list item\n1*. unordered sub-list item");
        switchToWysiwyg();

        // Outside lists
        moveCaret("document.body.firstChild.firstChild", 3);
        waitForOrderedListDetected(false);
        waitForUnorderedListDetected(false);

        // Inside unordered list item
        moveCaret("document.body.childNodes[1].firstChild.firstChild", 4);
        waitForOrderedListDetected(false);
        waitForUnorderedListDetected(true);
        // Inside ordered sub-list item
        moveCaret("document.body.childNodes[1].firstChild.lastChild.firstChild.firstChild", 7);
        waitForOrderedListDetected(true);
        waitForUnorderedListDetected(true);

        // Inside ordered list item
        moveCaret("document.body.childNodes[2].firstChild.firstChild", 10);
        waitForOrderedListDetected(true);
        waitForUnorderedListDetected(false);
        // Inside unordered sub-list item
        moveCaret("document.body.childNodes[2].firstChild.lastChild.firstChild.firstChild", 9);
        waitForOrderedListDetected(true);
        waitForUnorderedListDetected(true);
    }

    /**
     * @see XWIKI-3773: Adding and editing lists in table cells.
     */
    @Test
    public void testCreateListInTableCell()
    {
        insertTable();
        typeText("a");
        clickUnorderedListButton();
        typeEnter();
        typeText("b");
        switchToSource();
        assertSourceText("|=(((\n* a\n* b\n)))|= \n| | ");
    }

    /**
     * Test indenting a list fragment by selecting all the items and hitting the indent button.
     */
    @Test
    public void testIndentListFragment()
    {
        switchToSource();
        setSourceText("* one\n* two\n* three\n* three point one\n* three point two\n* three point three\n* four");
        switchToWysiwyg();
        select("document.body.firstChild.childNodes[3].firstChild", 0, "document.body.firstChild.childNodes[5].firstChild", 17);
        // The tool bar is not updated instantly and thus we have to wait for the indent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        switchToSource();
        assertSourceText("* one\n* two\n* three\n** three point one\n** three point two\n** three point three\n* four");
    }

    /**
     * Test the usual use case about only indenting the parent one level further, without its sublist. This cannot be
     * done in one because it is not a correct indent, from a semantic point of view, but most users expect it to
     * happen. The correct steps are to indent the parent and then outdent the sublist, which is the case tested by this
     * function.
     */
    @Test
    public void testIndentParentWithNoSublist()
    {
        switchToSource();
        setSourceText("* one\n* two\n* three\n** three point one\n** three point two\n** three point three\n* four");
        switchToWysiwyg();
        select("document.body.firstChild.childNodes[2].firstChild", 0, "document.body.firstChild.childNodes[2].firstChild", 5);
        // The tool bar is not updated instantly and thus we have to wait for the indent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        select("document.body.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[1].firstChild.firstChild", 0,
            "document.body.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[1].childNodes[2].firstChild", 17);
        // The tool bar is not updated instantly and thus we have to wait for the outdent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_OUTDENT_TITLE, true);
        clickOutdentButton();
        switchToSource();
        assertSourceText("* one\n* two\n** three\n** three point one\n** three point two\n** three point three\n* four");
    }

    /**
     * Tests indenting two items, amongst which one with a sublist and then outdenting the item with the sublist.
     */
    @Test
    public void testIndentItemWithSublistAndOutdent()
    {
        switchToSource();
        setSourceText("* one\n* two\n* three\n** foo\n** bar\n* four\n* four\n* five");
        switchToWysiwyg();
        select("document.body.firstChild.childNodes[2].firstChild", 0, "document.body.firstChild.childNodes[3].firstChild", 4);
        // The tool bar is not updated instantly and thus we have to wait for the indent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        switchToSource();
        assertSourceText("* one\n* two\n** three\n*** foo\n*** bar\n** four\n* four\n* five");
        switchToWysiwyg();
        select("document.body.firstChild.childNodes[1].childNodes[1].firstChild.firstChild", 0,
            "document.body.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[1].childNodes[1].firstChild", 3);
        // The tool bar is not updated instantly and thus we have to wait for the outdent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_OUTDENT_TITLE, true);
        clickOutdentButton();
        switchToSource();
        assertSourceText("* one\n* two\n* three\n** foo\n** bar\n** four\n* four\n* five");
    }

    /**
     * Tests a few indent and outdent operations on a list inside an embedded document (in this case, a table cell),
     * preceded by another list in the previous table cell.
     */
    @Test
    public void testIndentOutdentInTableCell()
    {
        switchToSource();
        setSourceText("|(((* item 1\n* item 2)))|(((* one\n** one plus one\n** one plus two\n* two\n* three)))\n| | ");
        switchToWysiwyg();
        select(
            "document.body.firstChild.firstChild.firstChild.childNodes[1].firstChild.firstChild.firstChild.childNodes[1]."
                + "childNodes[1].firstChild", 0,
            "document.body.firstChild.firstChild.firstChild.childNodes[1].firstChild.firstChild.childNodes[1]."
                + "firstChild", 3);
        // The tool bar is not updated instantly and thus we have to wait for the indent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_INDENT_TITLE, true);
        clickIndentButton();
        switchToSource();
        assertSourceText("|(((\n* item 1\n* item 2\n)))"
            + "|(((\n* one\n** one plus one\n*** one plus two\n** two\n* three\n)))\n| | ");
        switchToWysiwyg();
        select(
            "document.body.firstChild.firstChild.firstChild.childNodes[1].firstChild.firstChild.firstChild.childNodes[1]."
                + "childNodes[1].firstChild", 0,
            "document.body.firstChild.firstChild.firstChild.childNodes[1].firstChild.firstChild.childNodes[1]."
                + "firstChild", 5);
        // The tool bar is not updated instantly and thus we have to wait for the outdent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_OUTDENT_TITLE, true);
        clickOutdentButton();
        switchToSource();
        assertSourceText("|(((\n* item 1\n* item 2\n)))"
            + "|(((\n* one\n** one plus one\n*** one plus two\n* two\n\nthree\n)))\n| | ");
    }

    /**
     * Test that outdenting multiple list items on the first level of a list preserves distinct lines for the content of
     * the list items.
     * <p>
     * See https://jira.xwiki.org/browse/XWIKI-3921.
     */
    @Test
    public void testOutdentFirstLevelPreservesLines()
    {
        switchToSource();
        setSourceText("* one\n* two\n* three\n** three plus one\n"
            + "* four\n* (((before\n* inner five\n* inner five + 1\n\nafter)))\n* six");
        switchToWysiwyg();
        select("document.body.firstChild.firstChild.firstChild", 0, "document.body.firstChild.childNodes[5].firstChild", 3);
        // The tool bar is not updated instantly and thus we have to wait for the outdent button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_OUTDENT_TITLE, true);
        clickOutdentButton();
        switchToSource();
        assertSourceText("one\n\ntwo\n\nthree\n\n* three plus one\n\nfour\n\n"
            + "(((\nbefore\n\n* inner five\n* inner five + 1\n\nafter\n)))\n\nsix");
    }

    /**
     * Tests that a backspace between two list items with headings inside moves the second heading in the first list
     * item.
     * <p>
     * See https://jira.xwiki.org/browse/XWIKI-3877.
     */
    @Test
    public void testBackspaceBetweenHeadingListItems()
    {
        typeText("abc");
        clickUnorderedListButton();
        applyStyleTitle1();
        moveCaret("document.body.firstChild.firstChild.firstChild.firstChild", 2);
        typeEnter();
        typeBackspace();
        // expecting a single list item, with 2 headings
        assertContent("<ul><li><h1>ab</h1><h1>c</h1></li></ul>");
        switchToSource();
        assertSourceText("* (((\n= ab =\n\n= c =\n)))");
    }

    /**
     * Tests that the headings in two list items can be merged by a backspace followed by a delete: only one backspace,
     * as the previous test shows, is not enough because two items of the same type should not be automatically merged
     * on backspace between list items, since it's not the desired behaviour for all types of elements. <br>
     * TODO: re-activate when https://bugzilla.mozilla.org/show_bug.cgi?id=519751 will be fixed
     * <p>
     * See https://jira.xwiki.org/browse/XWIKI-3877.
     */
    public void failingTestBackspaceAndDeleteToMergeHeadingListItems()
    {
        // split the previous test in 2 so that this one can be marked as failing
        // now try to reunite two heading list items, with a backspace and a delete
        typeText("abc");
        clickUnorderedListButton();
        applyStyleTitle1();
        moveCaret("document.body.firstChild.firstChild.firstChild.firstChild", 2);
        typeEnter();
        typeBackspace();
        typeDelete();
        // expecting a single list item, with 2 headings
        assertContent("<ul><li><h1>abc</h1></li></ul>");
        switchToSource();
        assertSourceText("* (((\n= abc =\n)))");
    }

    /**
     * Tests that a delete between two list items with headings inside moves the second heading in the first list item. <br>
     * TODO: re-activate when https://bugzilla.mozilla.org/show_bug.cgi?id=519751 will be fixed
     * 
     * See https://jira.xwiki.org/browse/XWIKI-3877.
     */
    public void failingTestDeleteBetweenHeadingListItems()
    {
        typeText("cba");
        clickUnorderedListButton();
        applyStyleTitle1();
        moveCaret("document.body.firstChild.firstChild.firstChild.firstChild", 2);
        typeEnter();
        moveCaret("document.body.firstChild.firstChild.firstChild.firstChild", 2);
        typeDelete();
        // expecting a single list item, with 2 headings
        assertContent("<ul><li><h1>cb</h1><h1>a</h1></li></ul>");
        switchToSource();
        assertSourceText("* (((\n= cb =\n\n= a =\n)))");

        setSourceText("");
        switchToWysiwyg();

        // now try to reunite the two, with 2 deletes
        typeText("cba");
        clickUnorderedListButton();
        applyStyleTitle1();
        moveCaret("document.body.firstChild.firstChild.firstChild.firstChild", 2);
        typeEnter();
        moveCaret("document.body.firstChild.firstChild.firstChild.firstChild", 2);
        typeDelete();
        typeDelete();
        // expecting a single list item, with 2 headings
        assertContent("<ul><li><h1>cba</h1></li></ul>");
        switchToSource();
        assertSourceText("* (((\n= cba =\n)))");
    }

    /**
     * Waits for the editor to detect if the current selection is inside an ordered list.
     */
    public void waitForOrderedListDetected(boolean down)
    {
        waitForToggleButtonState("Numbering On/Off", down);
    }

    /**
     * Waits for the editor to detect is the current selection is inside an unordered list.
     */
    public void waitForUnorderedListDetected(boolean down)
    {
        waitForToggleButtonState("Bullets On/Off", down);
    }
}
