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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.ui.rta.RichTextAreaTestCase;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

import com.google.gwt.user.client.Command;

/**
 * Unit tests for {@link InlineStyleExecutable}.
 * 
 * @version $Id$
 */
public class StyleExecutableTest extends RichTextAreaTestCase
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (executable == null) {
            executable = new ToggleInlineStyleExecutable(rta, Style.FONT_STYLE, Style.FontStyle.ITALIC, "em");
        }
    }

    /**
     * Apply style when the document is empty and the caret is inside body.
     */
    public void testStyleWhenBodyIsEmpty()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("");
                assertFalse(executable.isExecuted());
                assertTrue(executable.execute(null));
                assertTrue(executable.isExecuted());
                insertHTML("a");
                assertTrue(executable.isExecuted());
                assertEquals("<em>a</em>", rta.getHTML().toLowerCase());
                assertTrue(executable.execute(null));
                assertFalse(executable.isExecuted());
                assertEquals("<em></em>a<em></em>", rta.getHTML().toLowerCase());
            }
        });
    }

    /**
     * Apply style when the caret is inside a text node.
     */
    public void testStyleWhenCaretIsInsideText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestStyleWhenCaretIsInsideText();
            }
        });
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

        assertFalse(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertTrue(executable.isExecuted());
        insertHTML("b");
        assertTrue(executable.isExecuted());
        assertEquals("a<em>b</em>c", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(null));
        assertFalse(executable.isExecuted());
        assertEquals("a<em></em>b<em></em>c", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is at the beginning of a text node.
     */
    public void testStyleWhenCaretIsBeforeText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestStyleWhenCaretIsBeforeText();
            }
        });
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

        assertFalse(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertTrue(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertFalse(executable.isExecuted());
        insertHTML("c");
        assertFalse(executable.isExecuted());
        assertEquals("cd", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is at the end of a text node.
     */
    public void testStyleWhenCaretIsAfterText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestStyleWhenCaretIsAfterText();
            }
        });
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

        assertFalse(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertTrue(executable.isExecuted());
        insertHTML("f");
        assertTrue(executable.isExecuted());
        assertEquals("e<em>f</em>", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(null));
        assertFalse(executable.isExecuted());
        assertEquals("e<em></em>f<em></em>", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is after an image.
     */
    public void testStyleWhenCaretIsAfterImage()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestStyleWhenCaretIsAfterImage();
            }
        });
    }

    /**
     * Apply style when the caret is after an image.
     */
    private void doTestStyleWhenCaretIsAfterImage()
    {
        // NOTE: In IE8 with the document rendered in standards mode we can't place the caret after an image if it's the
        // last child of its parent..
        rta.setHTML("#<img/><em>$</em>");

        Range range = ((Document) rta.getDocument()).createRange();
        range.setStartAfter(getBody().getChildNodes().getItem(1));
        range.collapse(true);
        select(range);

        assertFalse(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertTrue(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertFalse(executable.isExecuted());
        insertHTML("x");
        assertFalse(executable.isExecuted());
        assertEquals("#<img>x<em>$</em>", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the caret is before an image.
     */
    public void testStyleWhenCaretIsBeforeImage()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestStyleWhenCaretIsBeforeImage();
            }
        });
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

        assertFalse(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertTrue(executable.isExecuted());
        insertHTML("y");
        assertTrue(executable.isExecuted());
        assertEquals("<em>y</em><img>*", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(null));
        assertFalse(executable.isExecuted());
        assertEquals("<em></em>y<em></em><img>*", rta.getHTML().toLowerCase());
    }

    /**
     * Apply style when the selection is inside a text node.
     */
    public void testStyleWhenSelectionIsInsideText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestStyleWhenSelectionIsInsideText();
            }
        });
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

        assertFalse(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertTrue(executable.isExecuted());
        assertEquals("1<em>2</em>3", rta.getHTML().toLowerCase());
        assertTrue(executable.execute(null));
        assertFalse(executable.isExecuted());
        assertEquals(text, rta.getHTML().toLowerCase());
    }

    /**
     * Remove the style when the selection wraps the style element.
     * 
     * @see XWIKI-3110: Cannot unbold a bold word selected with double-click
     */
    public void testRemoveStyleWhenSelectionWrapsTheStyleElement()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                String text = "x<em>y</em>z";
                rta.setHTML(text);

                Range range = ((Document) rta.getDocument()).createRange();
                range.setStart(getBody().getFirstChild(), 1);
                range.setEnd(getBody().getLastChild(), 0);
                select(range);

                assertTrue(executable.isExecuted());
                assertTrue(executable.execute(null));
                assertFalse(executable.isExecuted());
                assertEquals("xyz", rta.getHTML());
            }
        });
    }

    /**
     * Apply style when the selection spans two paragraphs.
     */
    public void testStyleCrossParagraphSelection()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestStyleCrossParagraphSelection();
            }
        });
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
        assertFalse(executable.isExecuted());
        assertTrue(executable.execute(null));
        assertTrue(executable.isExecuted());
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(null));
        assertFalse(executable.isExecuted());
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
        assertEquals(html, clean(rta.getHTML()));
    }
}
