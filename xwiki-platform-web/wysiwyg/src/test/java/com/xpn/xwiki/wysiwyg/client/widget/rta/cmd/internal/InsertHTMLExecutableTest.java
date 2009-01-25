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
 * Unit tests for {@link InsertHTMLExecutable}.
 * 
 * @version $Id$
 */
public class InsertHTMLExecutableTest extends AbstractRichTextAreaTest
{
    /**
     * The executable being tested.
     */
    private Executable executable = new InsertHTMLExecutable();

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes between DOM child nodes after the selection is
     * deleted.
     */
    public void testInsertBetweenChildren()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestInsertBetweenChildren();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes between DOM child nodes after the selection is
     * deleted.
     */
    private void doTestInsertBetweenChildren()
    {
        rta.setHTML("<em>ab</em><strong>cd</strong><ins>ef</ins>");

        Range range = rta.getDocument().createRange();
        range.setStartBefore(getBody().getChildNodes().getItem(1));
        range.setEndAfter(getBody().getChildNodes().getItem(1));
        select(range);

        assertEquals("cd", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, "<!--x-->y<del>z</del>"));
        assertEquals("<em>ab</em><!--x-->y<del>z</del><ins>ef</ins>", clean(rta.getHTML()));
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes at the end of an element after the selection is
     * deleted.
     */
    public void testInsertAfterLastChild()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestInsertAfterLastChild();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes at the end of an element after the selection is
     * deleted.
     */
    private void doTestInsertAfterLastChild()
    {
        rta.setHTML("<em>ab</em><strong>ij</strong>");

        Range range = rta.getDocument().createRange();
        range.setStartBefore(getBody().getChildNodes().getItem(1));
        range.setEndAfter(getBody().getChildNodes().getItem(1));
        select(range);

        assertEquals("ij", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, "#"));
        assertEquals("<em>ab</em>#", clean(rta.getHTML()));
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes inside a text node after the selection is deleted.
     */
    public void testInsertInTextNode()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestInsertInTextNode();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes inside a text node after the selection is deleted.
     */
    private void doTestInsertInTextNode()
    {
        rta.setHTML("xyz");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 2);
        select(range);

        assertEquals("y", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, "*2<em>=</em>1+"));
        assertEquals("x*2<em>=</em>1+z", clean(rta.getHTML()));
    }
}
