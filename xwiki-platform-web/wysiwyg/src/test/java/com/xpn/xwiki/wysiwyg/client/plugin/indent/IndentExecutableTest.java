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
package com.xpn.xwiki.wysiwyg.client.plugin.indent;

import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.plugin.indent.exec.IndentExecutable;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Unit tests for {@link IndentExecutable}.
 * 
 * @version $Id$
 */
public class IndentExecutableTest extends AbstractRichTextAreaTest
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
            executable = new IndentExecutable();
        }
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple list (with no sublists), when a list item should be indented as the sublist of its parent.
     */
    public void testIndentNoSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestIndentNoSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple list (with no sublists), when a list item should be indented as the sublist of its parent.
     */
    private void doTestIndentNoSublist()
    {
        rta.setHTML("<ul><li>one</li><li>two</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>one<ul><li>two</li></ul></li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a list with an item with a sublist, to test that the first element in a list cannot be indented.
     */
    public void testIndentDisabledOnFirstItem()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestIndentDisabledOnFirstItem();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a list with an item with a sublist, to test that the first element in a list cannot be indented.
     */
    private void doTestIndentDisabledOnFirstItem()
    {
        String rtaInnerHTML = "<ul><li>foo<ul><li>bar</li></ul></li></ul>";
        rta.setHTML(rtaInnerHTML);

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        // is not enabled
        assertFalse(executable.isEnabled(rta));
        // does not execute
        assertFalse(executable.execute(rta, null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));

        // and also in sublist
        range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(1)
            .getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        // is not enabled
        assertFalse(executable.isEnabled(rta));
        // does not execute
        assertFalse(executable.execute(rta, null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}, for
     * the case when the selection is outside a list.
     */
    public void testIndentDisabledOutsideList()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestIndentDisabledOutsideList();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}, for
     * the case when the selection is outside a list.
     */
    private void doTestIndentDisabledOutsideList()
    {
        String rtaInnerHTML = "<p>xwiki rocks!</p><ul><li>foo<ul><li>bar</li></ul></li></ul>";
        rta.setHTML(rtaInnerHTML);

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.setEnd(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0), 4);
        select(range);

        // is not enabled
        assertFalse(executable.isEnabled(rta));
        // does not execute
        assertFalse(executable.execute(rta, null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}, for
     * the case when the selection is not inside a list item, when the indent button should be disabled. One of the
     * cases when this happens is when selection is around all (or some) of the elements in a list on the first level.
     * This behavior should be fixed in the future, to allow indenting all impacted elements, in which situation this
     * test will fail.
     */
    public void testIndentDisabledWhenSelectionNotInListItem()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestIndentDisabledWhenSelectionNotInListItem();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}, for
     * the case when the selection is not inside a list item, when the indent button should be disabled. One of the
     * cases when this happens is when selection is around all (or some) of the elements in a list on the first level.
     * This behavior should be fixed in the future, to allow indenting all impacted elements, in which situation this
     * test will fail.
     */
    private void doTestIndentDisabledWhenSelectionNotInListItem()
    {
        String rtaInnerHTML = "<ul><li>foo</li><li>bar</li></ul>";
        rta.setHTML(rtaInnerHTML);

        // set the selection around the two list items
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.setEnd(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(0), 3);
        select(range);

        // is not enabled
        assertFalse(executable.isEnabled(rta));
        // does not execute
        assertFalse(executable.execute(rta, null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}, for
     * the case when the selection is around a sublist: the indent button should be enabled and indenting should indent
     * the whole list item which is the parent of the list sublist.
     */
    public void testIndentParentListItemWhenSelectedSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestIndentParentListItemWhenSelectedSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)}, for
     * the case when the selection is around a sublist: the indent button should be enabled and indenting should indent
     * the whole list item which is the parent of the list sublist.
     */
    private void doTestIndentParentListItemWhenSelectedSublist()
    {
        rta.setHTML("<ul><li>foo</li><li>bar<ul><li>one</li><li>two</li><li>three</li></ul></li></ul>");

        // set the selection around the first two list items in the sublist of "bar"
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(1)
            .getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.setEnd(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(1)
            .getChildNodes().getItem(1).getChildNodes().getItem(0), 3);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>foo<ul><li>bar<ul><li>one</li><li>two</li><li>three</li></ul></li></ul></li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a list with sublists.
     */
    public void testIndentWithSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestIndentWithSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a list with sublists.
     */
    private void doTestIndentWithSublist()
    {
        rta.setHTML("<ul><li>one</li><li>two<ul><li>three</li></ul></li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>one<ul><li>two<ul><li>three</li></ul></li></ul></li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple list (with no sublists), under a list item with a sublist, when the indented item should become the last
     * child in its previous sibling's sublist.
     */
    public void testIndentUnderSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestIndentUnderSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link IndentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple list (with no sublists), under a list item with a sublist, when the indented item should become the last
     * child in its previous sibling's sublist.
     */
    private void doTestIndentUnderSublist()
    {
        rta.setHTML("<ul><li>one<ul><li>one plus one</li></ul></li><li>two</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>one<ul><li>one plus one</li><li>two</li></ul></li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }
}
