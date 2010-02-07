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
 * Unit tests for {@link OutdentExecutable}.
 * 
 * @version $Id$
 */
public class OutdentExecutableTest extends RichTextAreaTestCase
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
            executable = new OutdentExecutable(rta);
        }
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when the selection is outside a list.
     */
    public void testOutdentDisabledOutsideList()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestOutdentDisabledOutsideList();
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when the selection is outside a list.
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
        assertFalse(executable.isEnabled());
        // does not execute
        assertFalse(executable.execute(null));
        // doesn't change HTML at all
        assertEquals(rtaInnerHTML, removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when the selection is around all items on a first level list, in which case they are both to be unindented.
     */
    public void testOutdentEntireFirstLevelList()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                String rtaInnerHTML = "<ul><li>foo</li><li>bar</li></ul>";
                rta.setHTML(rtaInnerHTML);

                // set the selection around the two list items
                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 0);
                range.setEnd(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 3);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<p>foo</p><p>bar</p>", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a
     * simple sublist list item (with no other sublists), and which is the last item in its sublist.
     */
    public void testOutdentLastItemNoSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one<ul><li>two</li><li>three</li></ul></li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes()
                    .getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>one<ul><li>two</li></ul></li><li>three</li></ul>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a
     * sublist list item with sublists which is the last item in its sublist.
     */
    public void testOutdentLastItemWithSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one<ul><li>two</li><li>three<ul><li>four</li></ul></li></ul></li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes()
                    .getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>one<ul><li>two</li></ul></li><li>three<ul><li>four</li></ul></li></ul>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a
     * simple sublist list item (with no other sublists), which is in the middle of its sublist and on outdent it needs
     * to be split in two sublists.
     */
    public void testOutdentSplitNoSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one<ul><li>two</li><li>three</li><li>three plus one</li></ul></li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes()
                    .getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>one<ul><li>two</li></ul></li><li>three<ul><li>three plus one"
                    + "</li></ul></li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for a
     * simple sublist list item with a sublist, which is in the middle of its sublist and on outdent it needs to be
     * split in two sublists.
     */
    public void testOutdentSplitWithSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one<ul><li>two</li><li>three<ul><li>four</li></ul></li>"
                    + "<li>three plus one</li></ul></li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes()
                    .getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>one<ul><li>two</li></ul></li><li>three<ul><li>four</li><li>three plus "
                    + "one</li></ul></li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for a
     * simple sublist list item with a sublist, which is in the middle of its sublist and on outdent it needs to be
     * split in two sublists, and the item to be outdented is fully selected.
     */
    public void testOutdentSplitWithSublistEntirelySelected()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestOutdentSplitWithSublistEntirelySelected();
            }
        });
    }

    /**
     * @see #testOutdentSplitWithSublistEntirelySelected()
     */
    private void doTestOutdentSplitWithSublistEntirelySelected()
    {
        rta.setHTML("<ol><li>one<ol><li>two</li><li>three<ol><li>four</li></ol></li>"
            + "<li>three plus one</li></ol></li></ol>");

        Range range = rta.getDocument().createRange();
        // put the selection around three and four
        range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1)
            .getFirstChild(), 0);
        range.setEnd(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1)
            .getChildNodes().getItem(1).getFirstChild().getFirstChild(), 4);
        select(range);

        assertTrue(executable.isEnabled());
        assertTrue(executable.execute(null));
        assertEquals("<ol><li>one<ol><li>two</li></ol></li><li>three<ol><li>four</li><li>three plus "
            + "one</li></ol></li></ol>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when the outdented item is in a sublist and content is present in the parent list item after the sublist.
     */
    public void testOutdentSplitWithContentAfter()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one<br />before<ul><li>two</li><li>three</li><li>four</li></ul>after</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(3).getChildNodes()
                    .getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));

                assertEquals("<ul><li>one<br>before<ul><li>two</li></ul></li><li>three<ul><li>four</li></ul>"
                    + "after</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} the case
     * of a first level item, which is the first item in its list.
     */
    public void testOutdentFirstLevelItemAtListTop()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>one</li><li>two</li><li>three</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<p>one</p><ul><li>two</li><li>three</li></ul>", removeNonBreakingSpaces(clean(rta
                    .getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} the case
     * of a first level item, which is in the middle of its list and outdent needs to split it in two.
     */
    public void testOutdentFirstLevelItemSplitList()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>foo</li><li>bar</li><li>far</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>foo</li></ul><p>bar</p><ul><li>far</li></ul>", removeNonBreakingSpaces(clean(rta
                    .getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} the case
     * of a first level item, which is in the middle of its list and outdent needs to split it in two.
     */
    public void testOutdentFirstLevelItemWithSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>foo</li><li>bar<ul><li>bar plus one</li></ul></li><li>far</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>foo</li></ul><p>bar</p><ul><li>bar plus one</li><li>far</li></ul>",
                    removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case of a first level item with a sublist and content after, which is in the middle of its list and outdent needs
     * to split it in two, and wrap the list text in a paragraph, as well as the text after, but leaving the inner
     * sublist unwrapped.
     */
    public void testOutdentFirstLevelItemWithSublistAndContentAfter()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>foo</li><li>bar<ul><li>bar plus one</li></ul>after</li><li>far</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>foo</li></ul><p>bar</p><ul><li>bar plus one</li></ul><p>after</p>"
                    + "<ul><li>far</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} the case
     * of a first level item, which is the last item in its list.
     */
    public void testOutdentFirstLevelItemAtListEnd()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>first</li><li>second</li><li>third</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getChildNodes().getItem(2).getFirstChild(), 0);
                range.collapse(true);
                select(range);

                assertTrue(executable.isEnabled());
                assertTrue(executable.execute(null));
                assertEquals("<ul><li>first</li><li>second</li></ul><p>third</p>", removeNonBreakingSpaces(clean(rta
                    .getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when a whole second level sublist is to be outdented.
     */
    public void testOutdentEntireSublist()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestOutdentEntireSublist();
            }
        });
    }

    /**
     * @see #testOutdentEntireSublist()
     */
    private void doTestOutdentEntireSublist()
    {
        rta.setHTML("<ul><li>foo</li><li>bar<ul><li>one</li><li>two</li><li>three</li></ul></li><li>boo</li></ul>");

        // set the selection around all the items of the sublist of "bar"
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1).getFirstChild()
            .getFirstChild(), 0);
        range.setEnd(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1).getChildNodes()
            .getItem(2).getFirstChild(), 5);
        select(range);

        assertTrue(executable.isEnabled());
        assertTrue(executable.execute(null));
        assertEquals("<ul><li>foo</li><li>bar</li><li>one</li><li>two</li><li>three</li><li>boo</li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)}, for the
     * case when a whole second level sublist is to be outdented.
     */
    public void testOutdentSublistFragmentWithFollowingListFragment()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestOutdentSublistFragmentWithFollowingListFragment();
            }
        });
    }

    /**
     * @see #testOutdentSublistFragmentWithFollowingListFragment()
     */
    private void doTestOutdentSublistFragmentWithFollowingListFragment()
    {
        rta.setHTML("<ul><li>foo<ul><li>one</li><li>two<ul><li>three</li></ul></li>"
            + "<li>two plus one</li></ul></li><li>bar</li></ul>");

        // place selection around three and two plus one
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1)
            .getChildNodes().getItem(1).getFirstChild().getFirstChild(), 0);
        range.setEnd(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(2)
            .getFirstChild(), 12);

        select(range);

        assertTrue(executable.isEnabled());
        assertTrue(executable.execute(null));
        assertEquals("<ul><li>foo<ul><li>one</li><li>two</li><li>three</li></ul></li><li>two plus one</li>"
            + "<li>bar</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for {@link OutdentExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} for
     * outdenting a fragment of a sublist with some trailing text after it, together with the first level list under it.
     * In this case, the sublist fragment should be outdented correctly to the first level (and grab the trailing text
     * in itself), the first level item it makes part of should be outdented too (because of the trailing text) and the
     * first level list under it pulled outside the list.
     */
    public void testOutdentSublistFragmentWithTrailingTextAndListFragment()
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
        rta.setHTML("<ul><li>one one</li><li>one two<ul><li>two one</li><li>two two</li></ul>"
            + "after</li><li>one three</li><li>one four</li><li>one five</li></ul>");

        Range range = rta.getDocument().createRange();
        // put the selection from two two until one three
        range.setStart(getBody().getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(1).getChildNodes()
            .getItem(1).getFirstChild(), 0);
        range.setEnd(getBody().getFirstChild().getChildNodes().getItem(2).getFirstChild(), 9);
        select(range);

        assertTrue(executable.isEnabled());
        assertTrue(executable.execute(null));

        assertEquals("<ul><li>one one</li></ul><p>one two</p><ul><li>two one</li><li>two twoafter</li></ul><p>one "
            + "three</p><ul><li>one four</li><li>one five</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
    }
}
