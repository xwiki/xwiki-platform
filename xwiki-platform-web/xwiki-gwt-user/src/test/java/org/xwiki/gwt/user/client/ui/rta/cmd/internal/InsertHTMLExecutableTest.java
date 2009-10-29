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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextAreaTestCase;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

import com.google.gwt.user.client.Command;

/**
 * Unit tests for {@link InsertHTMLExecutable}.
 * 
 * @version $Id: InsertHTMLExecutableTest.java 24533 2009-10-16 14:42:07Z mflorea $
 */
public class InsertHTMLExecutableTest extends RichTextAreaTestCase
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
            executable = new InsertHTMLExecutable();
        }
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes between DOM child nodes after the selection is
     * deleted.
     */
    public void testInsertBetweenChildren()
    {
        deferTest(new Command()
        {
            public void execute()
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
        });
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes at the end of an element after the selection is
     * deleted.
     */
    public void testInsertAfterLastChild()
    {
        deferTest(new Command()
        {
            public void execute()
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
        });
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes inside a text node after the selection is deleted.
     */
    public void testInsertInTextNode()
    {
        deferTest(new Command()
        {
            public void execute()
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
        });
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the selection spans multiple list items.
     */
    public void testReplaceCrossListItemSelection()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li>foo</li><li>bar</li></ul>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 1);
                range.setEnd(getBody().getFirstChild().getLastChild().getFirstChild(), 1);
                select(range);

                assertEquals("oob", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute(rta, "<img/>"));
                assertEquals("<ul><li>f<img>ar</li></ul>", clean(rta.getHTML()));
            }
        });
    }

    /**
     * Tests if the selection is contracted to perfectly wrap the inserted nodes.
     */
    public void testSelectionContractionAfterInsertion()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestSelectionContractionAfterInsertion();
            }
        });
    }

    /**
     * Tests if the selection is contracted to perfectly wrap the inserted nodes.
     */
    private void doTestSelectionContractionAfterInsertion()
    {
        rta.setHTML("2009");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 3);
        select(range);

        assertEquals("00", rta.getDocument().getSelection().toString());
        assertTrue(executable.execute(rta, "<img title=\"march 11th\"/>"));
        assertEquals("2<img title=\"march 11th\">9", clean(rta.getHTML()));

        // Lets test if the selection wraps the inserted image.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(getBody(), range.getStartContainer());
        assertEquals(getBody(), range.getEndContainer());
        assertEquals(1, range.getStartOffset());
        assertEquals(2, range.getEndOffset());
    }
}
