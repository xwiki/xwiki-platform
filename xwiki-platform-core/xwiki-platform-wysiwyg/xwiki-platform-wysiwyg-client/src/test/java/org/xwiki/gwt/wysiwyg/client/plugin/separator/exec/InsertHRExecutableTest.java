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
package org.xwiki.gwt.wysiwyg.client.plugin.separator.exec;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.RichTextAreaTestCase;
import org.xwiki.gwt.wysiwyg.client.plugin.history.exec.UndoExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.history.internal.DefaultHistory;


/**
 * Unit test for {@link InsertHRExecutable}.
 * 
 * @version $Id$
 */
public class InsertHRExecutableTest extends RichTextAreaTestCase
{
    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        rta.getCommandManager().registerCommand(Command.INSERT_HORIZONTAL_RULE, new InsertHRExecutable(rta));
        rta.getCommandManager().registerCommand(Command.UNDO, new UndoExecutable(new DefaultHistory(rta, 10)));
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} when
     * there is no text selected.
     */
    public void testInsertAtCaret()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("a<p>b<ins><!--x-->cd</ins></p>e");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getChildNodes().getItem(1).getChildNodes().getItem(1).getLastChild(), 1);
                range.collapse(true);
                select(range);

                assertTrue(rta.getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE));
                assertEquals("a<p>b<ins><!--x-->c</ins></p><hr><p><ins>d</ins></p>e", removeNonBreakingSpaces(clean(rta
                    .getHTML())));
            }
        });
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} when
     * there is text selected.
     */
    public void testReplaceSelection()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                doTestReplaceSelection();
            }
        });
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} when
     * there is text selected.
     */
    private void doTestReplaceSelection()
    {
        rta.setHTML("<ul><li>a<em>bc</em>d<del>ef</del>g</li></ul>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild().getFirstChild().getChildNodes().getItem(3).getFirstChild(), 1);
        select(range);

        assertEquals("cde", rta.getDocument().getSelection().toString());
        assertTrue(rta.getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE));
        assertEquals("<ul><li>a<em>b</em><hr><em></em><del>f</del>g</li></ul>", removeNonBreakingSpaces(clean(rta
            .getHTML())));

        range = rta.getDocument().getSelection().getRangeAt(0);
        assertTrue(range.isCollapsed());
        assertEquals("<em></em>", ((Element) range.getStartContainer().getParentNode()).getString().toLowerCase());
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} when the
     * rich text area is empty.
     */
    public void testEmptyDocument()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("");

                assertTrue(rta.getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE));
                assertEquals("<hr>", clean(rta.getHTML()));
            }
        });
    }

    /**
     * Unit test for {@link InsertHRExecutable#execute(org.xwiki.gwt.user.client.ui.rta.RichTextArea, String)} when the
     * selection wraps an element's inner text.
     */
    public void testReplaceElementText()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("a<strong>b</strong>c");

                Range range = rta.getDocument().getSelection().getRangeAt(0);
                range.setEnd(getBody().getChildNodes().getItem(1).getFirstChild(), 1);
                range.setStart(getBody().getChildNodes().getItem(1).getFirstChild(), 0);
                select(range);

                assertEquals("b", rta.getDocument().getSelection().toString());
                assertTrue(rta.getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE));
                assertEquals("a<strong></strong><hr><strong></strong>c", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Inserts a horizontal rule in place of a selection that spans multiple list items.
     * 
     * @see XWIKI-2993: Insert horizontal line on a selection of unordered list
     */
    public void testReplaceCrossListItemSelection()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>foo</li><li>bar</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 1);
                range.setEnd(getBody().getFirstChild().getLastChild().getFirstChild(), 1);
                select(range);

                assertEquals("oob", rta.getDocument().getSelection().toString());
                assertTrue(rta.getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE));
                assertEquals("<ul><li>f<hr>ar</li></ul>", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Inserts a horizontal rule in place of a selection that spans all the list items of a list.
     * 
     * @see XWIKI-2993: Insert horizontal line on a selection of unordered list
     */
    public void testReplaceSelectedList()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("x<ul><li>one</li><li>two</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getLastChild().getFirstChild().getFirstChild(), 0);
                range.setEnd(getBody().getLastChild().getLastChild().getFirstChild(), 3);
                select(range);

                assertEquals("onetwo", rta.getDocument().getSelection().toString());
                assertTrue(rta.getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE));
                assertEquals("x<hr>", clean(rta.getHTML()));
            }
        });
    }

    /**
     * Inserts a horizontal rule and then reverts.
     */
    public void testInsertAndUndo()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                String html = "<ul><li><!--x-->alice</li><li><em></em>bob</li></ul>";
                rta.setHTML(html);

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getLastChild(), 5);
                range.setEnd(getBody().getFirstChild().getLastChild().getLastChild(), 3);
                select(range);

                assertEquals("bob", rta.getDocument().getSelection().toString());
                assertTrue(rta.getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE));
                assertTrue(rta.getCommandManager().execute(Command.UNDO));
                assertEquals(html, clean(rta.getHTML()));
            }
        });
    }
}
