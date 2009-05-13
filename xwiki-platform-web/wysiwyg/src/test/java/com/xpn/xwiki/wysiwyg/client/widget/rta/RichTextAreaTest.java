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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.xpn.xwiki.wysiwyg.client.widget.MockEventDispatcher;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHTMLExecutable;

/**
 * Unit tests for {@link RichTextArea}.
 * 
 * @version $Id$
 */
public class RichTextAreaTest extends AbstractRichTextAreaTest
{
    /**
     * The name of the paragraph DOM element.
     */
    public static final String PARAGRAPH = "p";

    /**
     * Unit test for {@link RichTextArea#setHTML(String)}. We test the workaround we use for Issue 3147.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3147
     */
    public void testSetHTML()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestSetHTML();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link RichTextArea#setHTML(String)}. We test the workaround we use for Issue 3147.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3147
     */
    private void doTestSetHTML()
    {
        String html = "<!--x--><em>test</em>";
        rta.setHTML(html);
        assertEquals(html, clean(rta.getHTML()));
    }

    /**
     * Unit test for {@link DOMUtils#getFirstLeaf(Range)} and {@link DOMUtils#getLastLeaf(Range)}. We put the test here
     * because we needed an empty document.
     */
    public void testGetRangeFirstAndLastLeafWithEmptyBody()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestGetRangeFirstAndLastLeafWithEmptyBody();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link DOMUtils#getFirstLeaf(Range)} and {@link DOMUtils#getLastLeaf(Range)}. We put the test here
     * because we needed an empty document.
     */
    private void doTestGetRangeFirstAndLastLeafWithEmptyBody()
    {
        rta.setHTML("");
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(getBody(), range.getCommonAncestorContainer());
        assertEquals(0, range.getStartOffset());
        assertEquals(getBody(), DOMUtils.getInstance().getFirstLeaf(range));
        assertEquals(getBody(), DOMUtils.getInstance().getLastLeaf(range));
    }

    /**
     * Unit test for {@link Range#setStartAfter(com.google.gwt.dom.client.Node)}. We put the test here because we needed
     * an empty document.
     */
    public void testRangeSetStartAfterWithEmptyBody()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRangeSetStartAfterWithEmptyBody();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link Range#setStartAfter(com.google.gwt.dom.client.Node)}. We put the test here because we needed
     * an empty document.
     */
    private void doTestRangeSetStartAfterWithEmptyBody()
    {
        rta.setHTML("");
        getBody().appendChild(rta.getDocument().xCreateSpanElement());

        Range range = rta.getDocument().createRange();
        range.setStartAfter(getBody().getFirstChild());
        range.collapse(true);
        select(range);

        assertTrue(new InsertHTMLExecutable().execute(rta, "*"));
        assertEquals("<span></span>*", clean(rta.getHTML()));
    }

    /**
     * Tests that text selection is not lost when the rich text area looses focus.
     */
    public void testTextSelectionIsNotLostOnBlur()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestTextSelectionIsNotLostOnBlur();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests that text selection is not lost when the rich text area looses focus.
     */
    private void doTestTextSelectionIsNotLostOnBlur()
    {
        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);

        rta.setHTML("1984");

        // Make a text selection.
        String selectedText = "98";
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 3);
        select(range);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Move the focus back and test the selection.
        rta.setFocus(true);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Cleanup
        textBox.removeFromParent();
    }

    /**
     * Tests if the text selection is readable when the rich text area doesn't have the focus.
     */
    public void testTextSelectionIsReadableWithoutFocus()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestTextSelectionIsReadableWithoutFocus();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the text selection is readable when the rich text area doesn't have the focus.
     */
    private void doTestTextSelectionIsReadableWithoutFocus()
    {
        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);

        rta.setHTML("abc");

        // Make a text selection.
        String selectedText = "b";
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 2);
        select(range);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Read the current selection when the rich text area doesn't have the focus.
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Move the focus back and test the selection.
        rta.setFocus(true);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Cleanup
        textBox.removeFromParent();
    }

    /**
     * Tests if the text selection is writable when the rich text area doesn't have the focus.
     */
    public void testTextSelectionIsWritableWithoutFocus()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestTextSelectionIsWritableWithoutFocus();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the text selection is writable when the rich text area doesn't have the focus.
     */
    private void doTestTextSelectionIsWritableWithoutFocus()
    {
        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);

        rta.setHTML("xyz");

        // Make a text selection.
        String selectedText = "y";
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 2);
        select(range);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Read the current selection when the rich text area doesn't have the focus.
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Change the current selection when the rich text area doesn't have the focus.
        selectedText = "z";
        range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 2);
        range.setEnd(getBody().getFirstChild(), 3);
        select(range);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Move the focus back and test the selection.
        rta.setFocus(true);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        // Cleanup
        textBox.removeFromParent();
    }

    /**
     * Tests if the caret at the end of a block-level element (like paragraph, heading, list item or table cell) is
     * preserved when the rich text area looses the focus.
     */
    public void testCaretAtTheEndOfABlockIsPreservedOnBlur()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestCaretAtTheEndOfABlockIsPreservedOnBlur();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the caret at the end of a block-level element (like paragraph, heading, list item or table cell) is
     * preserved when the rich text area looses the focus.
     */
    private void doTestCaretAtTheEndOfABlockIsPreservedOnBlur()
    {
        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);

        rta.setHTML("<h1>mhz</h1><h2>xyz</h2>");

        // Place the caret at the end of the first heading.
        String text = "mhz";
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild(), 3);
        range.setEnd(getBody().getFirstChild().getFirstChild(), 3);
        select(range);

        // Verify the selection before moving the focus.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertTrue(range.isCollapsed());
        assertEquals(3, range.getStartOffset());
        assertEquals(text, range.getStartContainer().getNodeValue());

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Move the focus back and test the selection.
        rta.setFocus(true);
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertTrue(range.isCollapsed());
        assertEquals(3, range.getStartOffset());
        assertEquals(text, range.getStartContainer().getNodeValue());

        // Cleanup
        textBox.removeFromParent();
    }

    /**
     * Tests that control selection is not lost when the rich text area looses focus.
     */
    public void testControlSelectionIsNotLostOnBlur()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestControlSelectionIsNotLostOnBlur();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests that control selection is not lost when the rich text area looses focus.
     */
    private void doTestControlSelectionIsNotLostOnBlur()
    {
        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);

        rta.setHTML("):<img/>:(");

        // Make a control selection.
        Node selectedNode = getBody().getChildNodes().getItem(1);
        Range range = rta.getDocument().createRange();
        range.selectNode(selectedNode);
        select(range);

        // Verify the control selection.
        assertSelectionWrapsNode(selectedNode);

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Move the focus back and test the selection.
        rta.setFocus(true);
        assertSelectionWrapsNode(selectedNode);

        // Cleanup
        textBox.removeFromParent();
    }

    /**
     * Tests if the control selection is readable when the rich text area doesn't have the focus.
     */
    public void testControlSelectionIsReadableWithoutFocus()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestControlSelectionIsReadableWithoutFocus();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the control selection is readable when the rich text area doesn't have the focus.
     */
    private void doTestControlSelectionIsReadableWithoutFocus()
    {
        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);

        rta.setHTML("1<img/>2");

        // Make a control selection.
        Node selectedNode = getBody().getChildNodes().getItem(1);
        Range range = rta.getDocument().createRange();
        range.selectNode(selectedNode);
        select(range);

        // Verify the control selection.
        assertSelectionWrapsNode(selectedNode);

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Verify the control selection when the rich text area doesn't have the focus.
        assertSelectionWrapsNode(selectedNode);

        // Move the focus back and test the selection.
        rta.setFocus(true);
        assertSelectionWrapsNode(selectedNode);

        // Cleanup
        textBox.removeFromParent();
    }

    /**
     * Tests if the control selection is writable when the rich text area doesn't have the focus.
     */
    public void testControlSelectionIsWritableWithoutFocus()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestControlSelectionIsWritableWithoutFocus();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the control selection is writable when the rich text area doesn't have the focus.
     */
    private void doTestControlSelectionIsWritableWithoutFocus()
    {
        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);

        rta.setHTML("1<img/>2<img/>3");

        // Make a control selection.
        Node selectedNode = getBody().getChildNodes().getItem(1);
        Range range = rta.getDocument().createRange();
        range.selectNode(selectedNode);
        select(range);

        // Verify the control selection.
        assertSelectionWrapsNode(selectedNode);

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Verify the control selection when the rich text area doesn't have the focus.
        assertSelectionWrapsNode(selectedNode);

        // Change the control selection when the rich text area doesn't have the focus.
        selectedNode = getBody().getChildNodes().getItem(3);
        range = rta.getDocument().createRange();
        range.selectNode(selectedNode);
        select(range);

        // Verify the changed selection.
        assertSelectionWrapsNode(selectedNode);

        // Move the focus back and test the selection.
        rta.setFocus(true);
        assertSelectionWrapsNode(selectedNode);

        // Cleanup
        textBox.removeFromParent();
    }

    /**
     * Asserts the current selection of the rich text area wraps the give node.
     * 
     * @param node a DOM node
     */
    protected void assertSelectionWrapsNode(Node node)
    {
        Selection selection = rta.getDocument().getSelection();
        assertEquals(1, selection.getRangeCount());
        assertRangeWrapsNode(selection.getRangeAt(0), node);
    }

    /**
     * Asserts that the given range wraps the specified node.
     * 
     * @param range a DOM range
     * @param node a DOM node
     */
    protected void assertRangeWrapsNode(Range range, Node node)
    {
        assertEquals(node.getParentNode(), range.getStartContainer());
        assertEquals(node.getParentNode(), range.getEndContainer());
        int index = DOMUtils.getInstance().getNodeIndex(node);
        assertEquals(index, range.getStartOffset());
        assertEquals(index + 1, range.getEndOffset());
    }

    /**
     * @see XWIKI-3283: Hitting enter twice between 2 titles doesn't create new line in IE6
     */
    public void testEnterBetweenHeadingsWithInnerSpan()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestEnterBetweenHeadingsWithInnerSpan();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see XWIKI-3283: Hitting enter twice between 2 titles doesn't create new line in IE6
     */
    private void doTestEnterBetweenHeadingsWithInnerSpan()
    {
        rta.setHTML("<h2><span>title 2</span></h2><h3><span>title 3</span></h3>");

        // Place the caret at the end of the first heading.
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getFirstChild().getFirstChild().getFirstChild());
        range.collapse(false);
        select(range);

        // Type Enter.
        MockEventDispatcher dispatcher = new MockEventDispatcher(rta);
        dispatcher.keyPress(KeyboardListener.KEY_ENTER);

        // Check if the caret is inside a span which is inside paragraph.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), "span"));
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter between two headings that contain only
     * text.
     */
    public void testEnterBetweenHeadingsWithoutInnerSpan()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestEnterBetweenHeadingsWithoutInnerSpan();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter between two headings that contain only
     * text.
     */
    private void doTestEnterBetweenHeadingsWithoutInnerSpan()
    {
        rta.setHTML("<h2>title 2</h2><h3>title 3/h3>");

        // Place the caret at the end of the first heading.
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getFirstChild().getFirstChild());
        range.collapse(false);
        select(range);

        // Type Enter.
        MockEventDispatcher dispatcher = new MockEventDispatcher(rta);
        dispatcher.keyPress(KeyboardListener.KEY_ENTER);

        // Check if the caret is inside a paragraph.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a direct
     * child of body.
     */
    public void testEnterInBodyWithPlainText()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestEnterInBodyWithPlainText();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a direct
     * child of body.
     */
    private void doTestEnterInBodyWithPlainText()
    {
        rta.setHTML("amazing!");

        // Place the caret at the end of the first heading.
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 3);
        select(range);

        // Type Enter.
        MockEventDispatcher dispatcher = new MockEventDispatcher(rta);
        dispatcher.keyPress(KeyboardListener.KEY_ENTER);

        // Check if the caret is inside a paragraph.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));

        // Check the result.
        assertEquals("a<p>zing!</p>", clean(rta.getHTML()));
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a descendant
     * of body with only in-line parents.
     */
    public void testEnterInBodyWithStyledText()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestEnterInBodyWithStyledText();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a descendant
     * of body with only in-line parents.
     */
    private void doTestEnterInBodyWithStyledText()
    {
        rta.setHTML("<strong>xwiki</strong>enterprise");

        // Place the caret at the end of the first heading.
        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getFirstChild().getFirstChild());
        range.collapse(false);
        select(range);

        // Type Enter.
        MockEventDispatcher dispatcher = new MockEventDispatcher(rta);
        dispatcher.keyPress(KeyboardListener.KEY_ENTER);

        // Check if the caret is inside a strong element which is inside a paragraph.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), "strong"));
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));
    }
}
