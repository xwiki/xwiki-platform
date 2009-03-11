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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHTMLExecutable;

/**
 * Unit tests for {@link RichTextArea}.
 * 
 * @version $Id$
 */
public class RichTextAreaTest extends AbstractRichTextAreaTest
{
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
    public void doTestGetRangeFirstAndLastLeafWithEmptyBody()
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
                doTestGetRangeFirstAndLastLeafWithEmptyBody();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link Range#setStartAfter(com.google.gwt.dom.client.Node)}. We put the test here because we needed
     * an empty document.
     */
    public void doTestRangeSetStartAfterWithEmptyBody()
    {
        rta.setHTML("");
        getBody().appendChild(rta.getDocument().xCreateSpanElement());
        rta.getDocument().getSelection().getRangeAt(0).setStartAfter(getBody().getFirstChild());
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
        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getChildNodes().getItem(1));
        select(range);

        // Verify the control selection.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(getBody(), range.getStartContainer());
        assertEquals(getBody(), range.getEndContainer());
        assertEquals(1, range.getStartOffset());
        assertEquals(2, range.getEndOffset());

        // Move the focus out of the rich text area.
        textBox.setFocus(true);

        // Move the focus back and test the selection.
        rta.setFocus(true);
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(getBody(), range.getStartContainer());
        assertEquals(getBody(), range.getEndContainer());
        assertEquals(1, range.getStartOffset());
        assertEquals(2, range.getEndOffset());

        // Cleanup
        textBox.removeFromParent();
    }
}
