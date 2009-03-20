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
import com.xpn.xwiki.wysiwyg.client.plugin.indent.exec.OutdentExecutable;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Unit tests for {@link OutdentExecutable}.
 * 
 * @version $Id$
 */
public class OutdentExecutableTest extends AbstractRichTextAreaTest
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
            executable = new OutdentExecutable();
        }
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)},
     * for the case when the selection is outside a list.
     */
    public void testOutdentDisabledOutsideList()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentDisabledOutsideList();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)},
     * for the case when the selection is outside a list.
     */
    private void doTestOutdentDisabledOutsideList()
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
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)},
     * for the case when the selection is not inside a list item, when the indent button should be disabled. One of the
     * cases when this happens is when selection is around all (or some) of the elements in a list on the first level.
     * This behavior should be fixed in the future, to allow outdenting all impacted elements, in which situation this
     * test will fail.
     */
    public void testOutdentDisabledWhenSelectionNotInListItem()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentDisabledWhenSelectionNotInListItem();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)},
     * for the case when the selection is not inside a list item, when the indent button should be disabled. One of the
     * cases when this happens is when selection is around all (or some) of the elements in a list on the first level.
     * This behavior should be fixed in the future, to allow outdenting all impacted elements, in which situation this
     * test will fail.
     */
    private void doTestOutdentDisabledWhenSelectionNotInListItem()
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
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple sublist list item (with no other sublists), and which is the last item in its sublist.
     */
    public void testOutdentLastItemNoSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentLastItemNoSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple sublist list item (with no other sublists), and which is the last item in its sublist.
     */
    private void doTestOutdentLastItemNoSublist()
    {
        rta.setHTML("<ul><li>one<ul><li>two</li><li>three</li></ul></li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(1)
            .getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>one<ul><li>two</li></ul></li><li>three</li></ul>", removeNonBreakingSpaces(clean(rta
            .getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a sublist list item with sublists which is the last item in its sublist.
     */
    public void testOutdentLastItemWithSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentLastItemWithSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a sublist list item with sublists which is the last item in its sublist.
     */
    private void doTestOutdentLastItemWithSublist()
    {
        rta.setHTML("<ul><li>one<ul><li>two</li><li>three<ul><li>four</li></ul></li></ul></li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(1)
            .getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>one<ul><li>two</li></ul></li><li>three<ul><li>four</li></ul></li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple sublist list item (with no other sublists), which is in the middle of its sublist and on outdent it
     * needs to be split in two sublists.
     */
    public void testOutdentSplitNoSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentSplitNoSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple sublist list item (with no other sublists), which is in the middle of its sublist and on outdent it
     * needs to be split in two sublists.
     */
    private void doTestOutdentSplitNoSublist()
    {
        rta.setHTML("<ul><li>one<ul><li>two</li><li>three</li><li>three plus one</li></ul></li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(1)
            .getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>one<ul><li>two</li></ul></li><li>three<ul><li>three plus one</li></ul></li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple sublist list item with a sublist, which is in the middle of its sublist and on outdent it needs to be
     * split in two sublists.
     */
    public void testOutdentSplitWithSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentSplitWithSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} for
     * a simple sublist list item with a sublist, which is in the middle of its sublist and on outdent it needs to be
     * split in two sublists.
     */
    private void doTestOutdentSplitWithSublist()
    {
        rta.setHTML("<ul><li>one<ul><li>two</li><li>three<ul><li>four</li></ul></li>"
            + "<li>three plus one</li></ul></li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(1)
            .getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals(
            "<ul><li>one<ul><li>two</li></ul></li><li>three<ul><li>four</li><li>three plus one</li></ul></li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is the first item in its list.
     */
    public void testOutdentFirstLevelItemAtListTop()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentFirstLevelItemAtListTop();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is the first item in its list.
     */
    private void doTestOutdentFirstLevelItemAtListTop()
    {
        rta.setHTML("<ul><li>one</li><li>two</li><li>three</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("one<ul><li>two</li><li>three</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is in the middle of its list and outdent needs to split it in two.
     */
    public void testOutdentFirstLevelItemSplitList()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentFirstLevelItemSplitList();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is in the middle of its list and outdent needs to split it in two.
     */
    private void doTestOutdentFirstLevelItemSplitList()
    {
        rta.setHTML("<ul><li>foo</li><li>bar</li><li>far</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>foo</li></ul>bar<ul><li>far</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is in the middle of its list and outdent needs to split it in two.
     */
    public void testOutdentFirstLevelItemWithSublist()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentFirstLevelItemWithSublist();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is in the middle of its list and outdent needs to split it in two.
     */
    private void doTestOutdentFirstLevelItemWithSublist()
    {
        rta.setHTML("<ul><li>foo</li><li>bar<ul><li>bar plus one</li></ul></li><li>far</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>foo</li></ul>bar<ul><li>bar plus one</li></ul><ul><li>far</li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is the last item in its list.
     */
    public void testOutdentFirstLevelItemAtListEnd()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOutdentFirstLevelItemAtListEnd();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea, String)} the
     * case of a first level item, which is the last item in its list.
     */
    private void doTestOutdentFirstLevelItemAtListEnd()
    {
        rta.setHTML("<ul><li>first</li><li>second</li><li>third</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(2).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        assertTrue(executable.isEnabled(rta));
        assertTrue(executable.execute(rta, null));
        assertEquals("<ul><li>first</li><li>second</li></ul>third", removeNonBreakingSpaces(clean(rta.getHTML())));
    }
}
