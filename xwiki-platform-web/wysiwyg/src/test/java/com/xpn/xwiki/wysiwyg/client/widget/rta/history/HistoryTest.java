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

import com.google.gwt.user.client.ui.RootPanel;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.util.Timer;
import com.xpn.xwiki.wysiwyg.client.util.TimerListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Unit tests for any implementation of {@link History}.
 * 
 * @version $Id$
 */
public class HistoryTest extends AbstractWysiwygClientTest
{
    /**
     * Deferred {@link HistoryTest#testRestoreSelection()}.
     */
    public class TestRestoreSelection implements TimerListener
    {
        /**
         * The rich text area whose history is being tested.
         */
        private RichTextArea rta;

        /**
         * Creates a new deferred test for {@link HistoryTest#testRestoreSelection()}.
         * 
         * @param rta The rich text area whose history is being tested.
         */
        public TestRestoreSelection(RichTextArea rta)
        {
            this.rta = rta;
        }

        /**
         * {@inheritDoc}
         * 
         * @see TimerListener#onElapsed(Timer)
         */
        public void onElapsed(Timer sender)
        {
            rta.setHTML("<span>ab</span><span>cde</span>");
            rta.setFocus(true);

            Range range = rta.getDocument().getSelection().getRangeAt(0);
            range.setEnd(rta.getDocument().getBody().getLastChild().getFirstChild(), 2);
            range.setStart(rta.getDocument().getBody().getFirstChild().getFirstChild(), 1);
            rta.getDocument().getSelection().removeAllRanges();
            rta.getDocument().getSelection().addRange(range);

            String selectedText = "bcd";
            assertEquals(selectedText, rta.getDocument().getSelection().toString());

            rta.getCommandManager().execute(Command.BOLD);
            rta.getCommandManager().execute(Command.UNDO);
            assertEquals(selectedText, rta.getDocument().getSelection().toString());

            rta.getCommandManager().execute(Command.REDO);
            assertEquals(selectedText, rta.getDocument().getSelection().toString());

            finishTest();
        }
    }

    /**
     * Tests if undo and redo operations restore the previous selection.
     */
    public void testRestoreSelection()
    {
        final RichTextArea rta = new RichTextArea();
        RootPanel.get().add(rta);

        // We need to delay the test because the rich text area is not initialized immediately.
        Timer timer = new Timer();
        timer.addTimerListener(new TestRestoreSelection(rta));

        delayTestFinish(300);
        timer.schedule(100);
    }
}
