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
package org.xwiki.gwt.user.client.ui.rta;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Unit tests for {@link RichTextArea}.
 * 
 * @version $Id$
 */
public class RichTextAreaTest extends RichTextAreaTestCase
{
    @Override
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();

        // Reset the visibility since some tests hide the rich text area.
        rta.setVisible(true);
    }

    /**
     * Unit test for {@link RichTextArea#setHTML(String)}. We test the workaround we use for Issue 3147.
     * 
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=3147.
     */
    public void testSetHTML()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                String html = "<!--x--><em>test</em>";
                rta.setHTML(html);
                assertEquals(html, clean(rta.getHTML()));
            }
        });
    }

    /**
     * Unit test for {@link DOMUtils#getFirstLeaf(Range)} and {@link DOMUtils#getLastLeaf(Range)}. We put the test here
     * because we needed an empty document.
     */
    public void testGetRangeFirstAndLastLeafWithEmptyBody()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("");
                Range range = rta.getDocument().getSelection().getRangeAt(0);
                assertEquals(getBody(), range.getCommonAncestorContainer());
                assertEquals(0, range.getStartOffset());
                assertEquals(getBody(), DOMUtils.getInstance().getFirstLeaf(range));
                assertEquals(getBody(), DOMUtils.getInstance().getLastLeaf(range));
            }
        });
    }

    /**
     * Unit test for {@link Range#setStartAfter(com.google.gwt.dom.client.Node)}. We put the test here because we needed
     * an empty document.
     */
    public void testRangeSetStartAfterWithEmptyBody()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("");
                getBody().appendChild(rta.getDocument().createSpanElement());

                Range range = rta.getDocument().createRange();
                range.setStartAfter(getBody().getFirstChild());
                range.collapse(true);
                select(range);

                insertHTML("*");
                assertEquals("<span></span>*", clean(rta.getHTML()));
            }
        });
    }

    /**
     * Tests that text selection is not lost when the rich text area looses focus.
     */
    public void testTextSelectionIsNotLostOnBlur()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestTextSelectionIsNotLostOnBlur();
            }
        });
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
        deferTest(new Command()
        {
            public void execute()
            {
                doTestTextSelectionIsReadableWithoutFocus();
            }
        });
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
        deferTest(new Command()
        {
            public void execute()
            {
                doTestTextSelectionIsWritableWithoutFocus();
            }
        });
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
        deferTest(new Command()
        {
            public void execute()
            {
                doTestCaretAtTheEndOfABlockIsPreservedOnBlur();
            }
        });
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
        deferTest(new Command()
        {
            public void execute()
            {
                doTestControlSelectionIsNotLostOnBlur();
            }
        });
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
        deferTest(new Command()
        {
            public void execute()
            {
                doTestControlSelectionIsReadableWithoutFocus();
            }
        });
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
        deferTest(new Command()
        {
            public void execute()
            {
                doTestControlSelectionIsWritableWithoutFocus();
            }
        });
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
     * Tests if the selection can be read and written while the rich text area is hidden.
     */
    public void testReadWriteHiddenSelection()
    {
        if (!GWT.isScript()) {
            // When running in hosted mode the edited document has no defaultView if the in-line frame is hidden. This
            // doesn't happen in web mode. Let's skip this test for now when we run in hosted mode.
            return;
        }
        deferTest(new Command()
        {
            public void execute()
            {
                doTestReadWriteHiddenSelection();
            }
        });
    }

    /**
     * Tests if the selection can be read and written while the rich text area is hidden.
     */
    private void doTestReadWriteHiddenSelection()
    {
        // Hide the rich text area.
        rta.setVisible(false);

        rta.setHTML("123<img/>4");

        // Make a text selection.
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 2);
        select(range);

        // Check if the text was selected even if the rich text area is hidden.
        assertEquals("2", rta.getDocument().getSelection().toString());

        // Make a control selection.
        range.selectNode(getBody().getChildNodes().getItem(1));
        select(range);

        // Verify the control selection.
        assertSelectionWrapsNode(getBody().getChildNodes().getItem(1));
    }

    /**
     * Tests if an anchor can be selected when the rich text area doesn't have the focus.
     */
    public void testSelectAnchorWithoutFocus()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestSelectAnchorWithoutFocus();
            }
        });
    }

    /**
     * Tests if an anchor can be selected when the rich text area doesn't have the focus.
     */
    private void doTestSelectAnchorWithoutFocus()
    {
        String anchorText = "inside";
        rta.setHTML("before<a href=\"http://www.xwiki.org\">" + anchorText + "</a>after");

        // We use a text input to move the focus out of the rich text area.
        TextBox textBox = new TextBox();
        RootPanel.get().add(textBox);
        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Select the anchor while the rich text area doesn't have the focus.
        Node selectedNode = getBody().getChildNodes().getItem(1);
        Range range = rta.getDocument().createRange();
        range.selectNode(selectedNode);
        select(range);

        // Verify the selection when the rich text area doesn't have the focus.
        assertEquals("blured", anchorText, rta.getDocument().getSelection().toString());

        // Move the focus back and test the selection.
        rta.setFocus(true);
        assertEquals("focused", anchorText, rta.getDocument().getSelection().toString());

        // Cleanup
        textBox.removeFromParent();
    }
}
