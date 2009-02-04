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
package com.xpn.xwiki.wysiwyg.client.widget.rta.history;

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Unit tests for any implementation of {@link History}.
 * 
 * @version $Id$
 */
public class HistoryTest extends AbstractRichTextAreaTest
{
    /**
     * Tests if undo and redo operations restore the previous selection.
     */
    public void testRestoreSelection()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRestoreSelection();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if undo and redo operations restore the previous selection.
     */
    public void doTestRestoreSelection()
    {
        rta.setHTML("<span>ab</span><span>cde</span>");

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getLastChild().getFirstChild(), 2);
        range.setStart(getBody().getFirstChild().getFirstChild(), 1);
        select(range);

        String selectedText = "bcd";
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        assertTrue(rta.getCommandManager().execute(Command.BOLD));
        assertTrue(rta.getCommandManager().execute(Command.UNDO));
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        assertTrue(rta.getCommandManager().execute(Command.REDO));
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
    }

    /**
     * Tests if undo and redo operations restore the previous selection even when it ends within a comment node.
     */
    public void testRestoreSelectionEndingInAComment()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRestoreSelectionEndingInAComment();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if undo and redo operations restore the previous selection even when it ends within a comment node.
     */
    public void doTestRestoreSelectionEndingInAComment()
    {
        rta.setHTML("ab<!--x-->c");

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getChildNodes().getItem(1), 1);
        range.setStart(getBody().getFirstChild(), 1);
        select(range);

        String selectedText = "b";
        // NOTE: It seems there is a bug in Mozilla's Selection implementation that prevents us from calling toString
        // directly on the selection when it starts or ends inside a comment node. Calling toString on a range seems to
        // work though.
        assertEquals(selectedText, rta.getDocument().getSelection().getRangeAt(0).toString());

        assertTrue(rta.getCommandManager().execute(Command.BOLD));
        assertTrue(rta.getCommandManager().execute(Command.UNDO));
        assertEquals(selectedText, rta.getDocument().getSelection().getRangeAt(0).toString());

        assertTrue(rta.getCommandManager().execute(Command.REDO));
        assertEquals(selectedText, rta.getDocument().getSelection().getRangeAt(0).toString());
    }

    /**
     * @see XWIKI-3047: Undo fails in IE when called inside a paragraph
     */
    public void testUndoInsideParagraph()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestUndoInsideParagraph();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see XWIKI-3047: Undo fails in IE when called inside a paragraph
     */
    public void doTestUndoInsideParagraph()
    {
        String html = "<p>xyz</p>";
        rta.setHTML(html);

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getFirstChild().getFirstChild(), 2);
        range.setStart(getBody().getFirstChild().getFirstChild(), 1);
        select(range);

        String selectedText = "y";
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
        rta.getCommandManager().execute(Command.BOLD);
        rta.getCommandManager().execute(Command.UNDO);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
        assertEquals(html, clean(rta.getHTML()));
    }
}
