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
                rta.setFocus(true);
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
        range.setEnd(rta.getDocument().getBody().getLastChild().getFirstChild(), 2);
        range.setStart(rta.getDocument().getBody().getFirstChild().getFirstChild(), 1);
        select(range);

        String selectedText = "bcd";
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        rta.getCommandManager().execute(Command.BOLD);
        rta.getCommandManager().execute(Command.UNDO);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());

        rta.getCommandManager().execute(Command.REDO);
        assertEquals(selectedText, rta.getDocument().getSelection().toString());
    }
}
