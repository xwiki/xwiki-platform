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
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
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
    private Executable executable = new InsertHRExecutable();

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
        assertEquals("a<p>b<ins><!--x-->c</ins></p><hr><p><ins>d</ins></p>e", clean(rta.getHTML()));
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
        assertEquals("<ul><li>a<em>b</em><hr><del>f</del>g</li></ul>", clean(rta.getHTML()));
        assertTrue(rta.getDocument().getSelection().isCollapsed());
        assertEquals(3, rta.getDocument().getSelection().getRangeAt(0).getStartOffset());
        assertEquals(getBody().getFirstChild().getFirstChild(), rta.getDocument().getSelection().getRangeAt(0)
            .getStartContainer());
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
        assertEquals("a<strong></strong><hr><strong></strong>c", clean(rta.getHTML()));
    }
}
