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
package org.xwiki.gwt.wysiwyg.client.plugin.indent.exec;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;
import org.xwiki.gwt.wysiwyg.client.RichTextAreaTestCase;

import com.google.gwt.user.client.Command;

/**
 * Unit tests for {@link IndentExecutable}.
 * 
 * @version $Id$
 */
public class IndentExecutableTest extends RichTextAreaTestCase
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    /**
     * {@inheritDoc}
     * 
     * @see RichTextAreaTestCase#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (executable == null) {
            executable = new IndentExecutable(rta);
        }
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a
     * simple list (with no sublists), when a list item should be indented as the sublist of its parent.
     */
    public void testIndentNoSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one</li><li>two</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>one<ul><li>two</li></ul></li></ul>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a list
     * with an item with a sublist, to test that the first element in a list cannot be indented.
     */
    public void testIndentDisabledOnFirstItem()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestIndentDisabledOnFirstItem();
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a list
     * with an item with a sublist, to test that the first element in a list cannot be indented.
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
        assertFalse(executable.isEnabled());
        // does not execute
        assertFalse(executable.execute(null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));

        // and also in sublist
        range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(1)
            .getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.collapse(true);
        select(range);

        // is not enabled
        assertFalse(executable.isEnabled());
        // does not execute
        assertFalse(executable.execute(null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when the selection is outside a list.
     */
    public void testIndentDisabledOutsideList()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                String rtaInnerHTML = "<p>xwiki rocks!</p><ul><li>foo<ul><li>bar</li></ul></li></ul>";
                rta.setHTML(rtaInnerHTML);

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
                range.setEnd(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0), 4);
                select(range);

                // is not enabled
                assertFalse(executable.isEnabled());
                // does not execute
                assertFalse(executable.execute(null));
                // doesn't change HTML at all
                assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when the selection is around both the list items in a list, on the first level. The indent cannot be made
     * because the rule is that if the selected items are part of the same list, either they should all be indented or
     * none.
     */
    public void testIndentDisabledWhenEntireListIsSelected()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestIndentDisabledWhenEntireListIsSelected();
            }
        });
    }

    /**
     * @see #testIndentDisabledWhenEntireListIsSelected()
     */
    private void doTestIndentDisabledWhenEntireListIsSelected()
    {
        String rtaInnerHTML = "<ul><li>foo</li><li>bar</li></ul>";
        rta.setHTML(rtaInnerHTML);

        // set the selection around the two list items
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(0).getChildNodes().getItem(0).getChildNodes().getItem(0), 0);
        range.setEnd(getBody().getChildNodes().getItem(0).getChildNodes().getItem(1).getChildNodes().getItem(0), 3);
        select(range);

        // is not enabled
        assertFalse(executable.isEnabled());
        // does not execute
        assertFalse(executable.execute(null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when the selection is around an entire sublist: the indent button should be disabled because not all the
     * items can be indented. The rule is that if the selected items are part of the same list, either they should all
     * be indented or none.
     */
    public void testIndentDisabledWhenEntireSublistIsSelected()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                String rtaInnerHTML =
                    "<ul><li>foo</li><li>bar<ul><li>one</li><li>two</li><li>three</li></ul></li></ul>";
                rta.setHTML(rtaInnerHTML);

                // set the selection around all the items of the sublist of "bar"
                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1)
                    .getFirstChild().getFirstChild(), 0);
                range.setEnd(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1)
                    .getChildNodes().getItem(2).getFirstChild(), 5);
                select(range);

                assertFalse(executable.isEnabled());
                assertFalse(executable.execute(null));
                assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a list
     * with sublists.
     */
    public void testIndentWithSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one</li><li>two<ul><li>three</li></ul></li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>one<ul><li>two<ul><li>three</li></ul></li></ul></li></ul>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a list
     * with sublists, entirely selected upon indenting.
     */
    public void testIndentEntirelySelectedItemWithSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>foo</li><li>bar<ul><li>foobar</li></ul></li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.setEnd(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1)
                    .getFirstChild().getFirstChild(), 5);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>foo<ul><li>bar<ul><li>foobar</li></ul></li></ul></li></ul>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a
     * simple list (with no sublists), under a list item with a sublist, when the indented item should become the last
     * child in its previous sibling's sublist.
     */
    public void testIndentUnderSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one<ul><li>one plus one</li></ul></li><li>two</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>one<ul><li>one plus one</li><li>two</li></ul></li></ul>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for
     * indenting an entire fragment of a list as a sublist.
     */
    public void testIndentListFragment()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ol><li>one</li><li>two</li><li>three</li><li>four</li></ol>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.setEnd(getBody().getFirstChild().getChildNodes().getItem(2).getFirstChild(), 5);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));

                assertEquals("<ol><li>one<ol><li>two</li><li>three</li></ol></li><li>four</li></ol>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for
     * indenting an entire fragment of a list as a sublist, under an existing second level list. In this case, the
     * indented items should be added as next items in the list.
     */
    public void testIndentListUnderSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ol><li>one<ul><li>under 1</li><li>under 2</li></ul>"
                    + "</li><li>two</li><li>three</li><li>four</li></ol>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.setEnd(getBody().getFirstChild().getChildNodes().getItem(2).getFirstChild(), 5);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));

                assertEquals("<ol><li>one<ul><li>under 1</li><li>under 2</li><li>two</li><li>three</li></ul>"
                    + "</li><li>four</li></ol>", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for
     * indenting a fragment of a list as a sublist, with the selection starting containing the whole sublist too. In
     * this case, the indent should only work on the fragment of the first level list, and the result should be the
     * alignment of the newly indented list items with the selected sublist.
     */
    public void testIndentAlignsSublists()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ol><li>one<ul><li>under</li></ul></li><li>two</li><li>three</li>" + "<li>four</li></ol>");

                Range range = rta.getDocument().createRange();
                // put the selection from under to the end of "two"
                range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getFirstChild()
                    .getFirstChild(), 0);
                range.setEnd(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 3);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));

                assertEquals("<ol><li>one<ul><li>under</li><li>two</li></ul></li><li>three</li><li>four</li></ol>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for
     * indenting a fragment of a sublist together with the first level list under it. In this case, the sublist fragment
     * should be indented correctly relative to its parent, and the first level list under it as the next sibling of the
     * original sublist.
     */
    public void testIndentSublistFragmentAndListFragment()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestIndentSublistFragmentAndListFragment();
            }
        });
    }

    /**
     * @see #testIndentSublistFragmentAndListFragment()
     */
    private void doTestIndentSublistFragmentAndListFragment()
    {
        rta.setHTML("<ul><li>one one</li><li>one two<ul><li>two one</li><li>two two</li></ul></li><li>one three</li>"
            + "<li>one four</li><li>one five</li></ul>");

        Range range = rta.getDocument().createRange();
        // put the selection from two two until one four
        range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1).getChildNodes()
            .getItem(1).getFirstChild(), 0);
        range.setEnd(getBody().getFirstChild().getChildNodes().getItem(3).getFirstChild(), 8);
        select(range);

        assertTrue(executable.isEnabled());
        assertTrue(executable.execute(null));

        assertEquals("<ul><li>one one</li><li>one two<ul><li>two one<ul><li>two two</li></ul></li><li>one three</li>"
            + "<li>one four</li></ul></li><li>one five</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link IndentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for
     * indenting a fragment of a sublist with some trailing text after it, together with the first level list under it.
     * In this case, the sublist fragment should be indented correctly relative to its parent, the first level item it
     * makes part of should be indented too (because of the trailing text) and the first level list under it as the next
     * sibling of the new second level list.
     */
    public void testIndentSublistFragmentWithTrailingTextAndListFragment()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestIndentSublistFragmentWithTrailingTextAndListFragment();
            }
        });
    }

    /**
     * @see #testIndentSublistFragmentAndListFragment()
     */
    private void doTestIndentSublistFragmentWithTrailingTextAndListFragment()
    {
        rta.setHTML("<ul><li>one one</li><li>one two<ul><li>two one</li><li>two two</li></ul>after</li>"
            + "<li>one three</li><li>one four</li><li>one five</li></ul>");

        Range range = rta.getDocument().createRange();
        // put the selection from two two until one four
        range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1).getChildNodes()
            .getItem(1).getFirstChild(), 0);
        range.setEnd(getBody().getFirstChild().getChildNodes().getItem(3).getFirstChild(), 8);
        select(range);

        assertTrue(executable.isEnabled());
        assertTrue(executable.execute(null));

        assertEquals("<ul><li>one one<ul><li>one two<ul><li>two one<ul><li>two two</li></ul></li></ul>"
            + "after</li><li>one three</li><li>one four</li></ul></li><li>one five</li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }
}
