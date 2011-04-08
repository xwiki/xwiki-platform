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
package org.xwiki.gwt.wysiwyg.client.plugin.history;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.RichTextAreaTestCase;
import org.xwiki.gwt.wysiwyg.client.plugin.history.exec.RedoExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.history.exec.UndoExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.history.internal.DefaultHistory;
import org.xwiki.gwt.wysiwyg.client.plugin.text.exec.BoldExecutable;


/**
 * Unit tests for any implementation of {@link History}.
 * 
 * @version $Id$
 */
public class HistoryTest extends RichTextAreaTestCase
{
    /**
     * {@inheritDoc}
     * 
     * @see RichTextAreaTestCase#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        History history = new DefaultHistory(rta, 10);
        rta.getCommandManager().registerCommand(Command.UNDO, new UndoExecutable(history));
        rta.getCommandManager().registerCommand(Command.REDO, new RedoExecutable(history));
        rta.getCommandManager().registerCommand(Command.BOLD, new BoldExecutable(rta));
    }

    /**
     * Tests if undo and redo operations restore the previous selection.
     */
    public void testRestoreSelection()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                doTestRestoreSelection();
            }
        });
    }

    /**
     * Tests if undo and redo operations restore the previous selection.
     */
    private void doTestRestoreSelection()
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
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                doTestRestoreSelectionEndingInAComment();
            }
        });
    }

    /**
     * Tests if undo and redo operations restore the previous selection even when it ends within a comment node.
     */
    private void doTestRestoreSelectionEndingInAComment()
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
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                doTestUndoInsideParagraph();
            }
        });
    }

    /**
     * @see XWIKI-3047: Undo fails in IE when called inside a paragraph
     */
    private void doTestUndoInsideParagraph()
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
