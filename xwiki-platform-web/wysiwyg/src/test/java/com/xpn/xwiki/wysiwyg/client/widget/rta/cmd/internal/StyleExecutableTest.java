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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Style;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Unit tests for {@link StyleExecutable}.
 * 
 * @version $Id$
 */
public class StyleExecutableTest extends AbstractRichTextAreaTest
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    /**
     * Utility executable used for inserting text after styling.
     */
    private Executable insertHTML;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRichTextAreaTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (executable == null) {
            executable = new StyleExecutable("em", null, Style.FONT_STYLE, Style.FontStyle.ITALIC, true, false);
            insertHTML = new InsertHTMLExecutable();
        }
    }

    /**
     * Apply style when the document is empty and the caret is inside body.
     */
    public void testStyleWhenBodyIsEmpty()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleWhenBodyIsEmpty();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the document is empty and the caret is inside body.
     */
    private void doTestStyleWhenBodyIsEmpty()
    {
        rta.setHTML("");
        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertTrue(insertHTML.execute(rta, "a"));
        assertTrue(executable.isExecuted(rta));
        assertEquals("<em>a</em>", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertEquals("<em></em>a<em></em>", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is inside a text node.
     */
    public void testStyleWhenCaretIsInsideText()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleWhenCaretIsInsideText();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the caret is inside a text node.
     */
    private void doTestStyleWhenCaretIsInsideText()
    {
        rta.setHTML("ac");

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.collapse(true);
        select(range);

        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertTrue(insertHTML.execute(rta, "b"));
        assertTrue(executable.isExecuted(rta));
        assertEquals("a<em>b</em>c", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertEquals("a<em></em>b<em></em>c", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is at the beginning of a text node.
     */
    public void testStyleWhenCaretIsBeforeText()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleWhenCaretIsBeforeText();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the caret is at the beginning of a text node.
     */
    private void doTestStyleWhenCaretIsBeforeText()
    {
        rta.setHTML("d");

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStart(getBody().getFirstChild(), 0);
        range.collapse(true);
        select(range);

        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertTrue(insertHTML.execute(rta, "c"));
        assertFalse(executable.isExecuted(rta));
        assertEquals("cd", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is at the end of a text node.
     */
    public void testStyleWhenCaretIsAfterText()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleWhenCaretIsAfterText();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the caret is at the end of a text node.
     */
    private void doTestStyleWhenCaretIsAfterText()
    {
        rta.setHTML("e");

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.collapse(true);
        select(range);

        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertTrue(insertHTML.execute(rta, "f"));
        assertTrue(executable.isExecuted(rta));
        assertEquals("e<em>f</em>", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertEquals("e<em></em>f<em></em>", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is after an image.
     */
    public void testStyleWhenCaretIsAfterImage()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleWhenCaretIsAfterImage();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the caret is after an image.
     */
    private void doTestStyleWhenCaretIsAfterImage()
    {
        rta.setHTML("#<img/>");

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStartAfter(getBody().getLastChild());
        range.collapse(true);
        select(range);

        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertTrue(insertHTML.execute(rta, "x"));
        assertFalse(executable.isExecuted(rta));
        assertEquals("#<img>x", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is before an image.
     */
    public void testStyleWhenCaretIsBeforeImage()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleWhenCaretIsBeforeImage();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the caret is before an image.
     */
    private void doTestStyleWhenCaretIsBeforeImage()
    {
        rta.setHTML("<img/>*");

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStartBefore(getBody().getFirstChild());
        range.collapse(true);
        select(range);

        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertTrue(insertHTML.execute(rta, "y"));
        assertTrue(executable.isExecuted(rta));
        assertEquals("<em>y</em><img>*", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertEquals("<em></em>y<em></em><img>*", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the selection is inside a text node.
     */
    public void testStyleWhenSelectionIsInsideText()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleWhenSelectionIsInsideText();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the selection is inside a text node.
     */
    private void doTestStyleWhenSelectionIsInsideText()
    {
        String text = "123";
        rta.setHTML(text);

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 2);
        select(range);

        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertEquals("1<em>2</em>3", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertEquals(text, rta.getHTML().toLowerCase());
    }

    /**
     * Remove the style when the selection wraps the style element.
     * 
     * @see XWIKI-3110: Cannot unbold a bold word selected with double-click
     */
    public void testRemoveStyleWhenSelectionWrapsTheStyleElement()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRemoveStyleWhenSelectionWrapsTheStyleElement();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Remove the style when the selection wraps the style element.
     * 
     * @see XWIKI-3110: Cannot unbold a bold word selected with double-click
     */
    private void doTestRemoveStyleWhenSelectionWrapsTheStyleElement()
    {
        String text = "x<em>y</em>z";
        rta.setHTML(text);

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getLastChild(), 0);
        select(range);

        assertTrue(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertEquals("xyz", rta.getHTML());
    }

    /**
     * Apply style when the selection spans two paragraphs.
     */
    public void testStyleCrossParagraphSelection()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestStyleCrossParagraphSelection();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Apply style when the selection spans two paragraphs.
     */
    private void doTestStyleCrossParagraphSelection()
    {
        String html = "<p>abc</p><p>xyz</p>";
        rta.setHTML(html);

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStart(getBody().getFirstChild().getFirstChild(), 2);
        range.setEnd(getBody().getLastChild().getFirstChild(), 2);
        select(range);

        String selectedText = "cxy";
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
        assertFalse(executable.isExecuted(rta));
        assertTrue(executable.execute(rta, null));
        assertTrue(executable.isExecuted(rta));
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, null));
        assertFalse(executable.isExecuted(rta));
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
        assertEquals(html, clean(rta.getHTML()));
    }
}
