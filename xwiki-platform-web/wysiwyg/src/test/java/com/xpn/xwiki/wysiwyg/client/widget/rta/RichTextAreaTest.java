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
