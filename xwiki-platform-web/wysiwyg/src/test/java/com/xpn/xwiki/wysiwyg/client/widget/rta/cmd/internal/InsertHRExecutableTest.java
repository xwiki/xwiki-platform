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
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Unit test for {@link InsertHRExecutable}.
 * 
 * @version $Id$
 */
public class InsertHRExecutableTest extends AbstractRichTextAreaTest
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRichTextAreaTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (executable == null) {
            executable = new InsertHRExecutable();
        }
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when there is no text selected.
     */
    public void testInsertAtCaret()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestInsertAtCaret();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when there is no text selected.
     */
    private void doTestInsertAtCaret()
    {
        rta.setHTML("a<p>b<ins><!--x-->cd</ins></p>e");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(1).getChildNodes().getItem(1).getLastChild(), 1);
        range.collapse(true);
        select(range);

        assertTrue(executable.execute(rta, null));
        assertEquals("a<p>b<ins><!--x-->c</ins></p><hr><p><ins>d</ins></p>e", removeNonBreakingSpaces(clean(rta
            .getHTML())));
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when there is text selected.
     */
    public void testReplaceSelection()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestReplaceSelection();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when there is text selected.
     */
    private void doTestReplaceSelection()
    {
        rta.setHTML("<ul><li>a<em>bc</em>d<del>ef</del>g</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(3).getFirstChild(), 1);
        select(range);

        assertEquals("cde", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>a<em>b</em><hr><em></em><del>f</del>g</li></ul>", removeNonBreakingSpaces(clean(rta
            .getHTML())));

        range = rta.getDocument().getSelection().getRangeAt(0);
        assertTrue(range.isCollapsed());
        assertEquals("<em></em>", ((Element) range.getStartContainer().getParentNode()).getString().toLowerCase());
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when the rich text area is empty.
     */
    public void testEmptyDocument()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestEmptyDocument();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when the rich text area is empty.
     */
    private void doTestEmptyDocument()
    {
        rta.setHTML("");

        assertTrue(executable.execute(rta, null));
        assertEquals("<hr>", clean(rta.getHTML()));
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when the selection wraps an element's inner text.
     */
    public void testReplaceElementText()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestReplaceElementText();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}
     * when the selection wraps an element's inner text.
     */
    private void doTestReplaceElementText()
    {
        rta.setHTML("a<strong>b</strong>c");

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getChildNodes().getItem(1).getFirstChild(), 1);
        range.setStart(getBody().getChildNodes().getItem(1).getFirstChild(), 0);
        select(range);

        assertEquals("b", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, null));
        assertEquals("a<strong></strong><hr><strong></strong>c", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Inserts a horizontal rule in place of a selection that spans multiple list items.
     * 
     * @see XWIKI-2993: Insert horizontal line on a selection of unordered list
     */
    public void testReplaceCrossListItemSelection()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestReplaceCrossListItemSelection();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Inserts a horizontal rule in place of a selection that spans multiple list items.
     * 
     * @see XWIKI-2993: Insert horizontal line on a selection of unordered list
     */
    private void doTestReplaceCrossListItemSelection()
    {
        rta.setHTML("<ul><li>foo</li><li>bar</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild().getLastChild().getFirstChild(), 1);
        select(range);

        assertEquals("oob", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>f<hr>ar</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Inserts a horizontal rule in place of a selection that spans all the list items of a list.
     * 
     * @see XWIKI-2993: Insert horizontal line on a selection of unordered list
     */
    public void testReplaceSelectedList()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestReplaceSelectedList();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Inserts a horizontal rule in place of a selection that spans all the list items of a list.
     * 
     * @see XWIKI-2993: Insert horizontal line on a selection of unordered list
     */
    private void doTestReplaceSelectedList()
    {
        rta.setHTML("x<ul><li>one</li><li>two</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getLastChild().getFirstChild().getFirstChild(), 0);
        range.setEnd(getBody().getLastChild().getLastChild().getFirstChild(), 3);
        select(range);

        assertEquals("onetwo", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, null));
        assertEquals("x<hr>", clean(rta.getHTML()));
    }

    /**
     * Inserts a horizontal rule and then reverts.
     */
    public void testInsertAndUndo()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestInsertAndUndo();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Inserts a horizontal rule and then reverts.
     */
    private void doTestInsertAndUndo()
    {
        String html = "<ul><li><!--x-->alice</li><li><em></em>bob</li></ul>";
        rta.setHTML(html);

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild().getLastChild(), 5);
        range.setEnd(getBody().getFirstChild().getLastChild().getLastChild(), 3);
        select(range);

        assertEquals("bob", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, null));
        assertTrue(rta.getCommandManager().execute(Command.UNDO));
        assertEquals(html, clean(rta.getHTML()));
    }
}
