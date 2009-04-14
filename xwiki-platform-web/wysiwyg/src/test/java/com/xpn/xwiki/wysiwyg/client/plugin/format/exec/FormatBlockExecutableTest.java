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
package com.xpn.xwiki.wysiwyg.client.plugin.format.exec;

import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHTMLExecutable;

/**
 * Unit tests for {@link FormatBlockExecutable}.
 * 
 * @version $Id$
 */
public class FormatBlockExecutableTest extends AbstractRichTextAreaTest
{
    /**
     * Heading level 1.
     */
    public static final String H1 = "h1";

    /**
     * Paragraph.
     */
    public static final String P = "p";

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
            executable = new FormatBlockExecutable();
        }
    }

    /**
     * @see http://jira.xwiki.org/jira/browse/XWIKI-2730
     */
    public void testInsertHeaderOnEmptyDocument()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestInsertHeaderOnEmptyDocument();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see http://jira.xwiki.org/jira/browse/XWIKI-2730
     */
    private void doTestInsertHeaderOnEmptyDocument()
    {
        rta.setHTML("");
        assertEquals("", rta.getHTML());
        assertTrue(executable.execute(rta, H1));
        assertEquals("<h1></h1>", clean(rta.getHTML()));
        assertFalse(getBody().getFirstChild().hasChildNodes());
        assertTrue(new InsertHTMLExecutable().execute(rta, "title 1"));
        assertEquals("<h1>title 1</h1>", clean(rta.getHTML()));
        assertEquals(H1, executable.getParameter(rta));
    }

    /**
     * Unit test for inserting header when the selection is empty.
     */
    public void testInsertHeaderAroundCaret()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestInsertHeaderAroundCaret();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for inserting header when the selection is empty.
     */
    private void doTestInsertHeaderAroundCaret()
    {
        rta.setHTML("xwiki <span><em>is</em> the</span> <p>best</p>");
        Node xwiki = getBody().getFirstChild();
        Node best = getBody().getLastChild().getFirstChild();

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getChildNodes().getItem(1).getFirstChild().getFirstChild(), 1);
        range.collapse(false);
        select(range);
        assertTrue(executable.execute(rta, H1));
        assertEquals("<h1>xwiki <span><em>is</em> the</span> </h1><p>best</p>", clean(rta.getHTML()));
        assertEquals(H1, executable.getParameter(rta));

        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(1, range.getStartOffset());
        assertEquals("is", range.getStartContainer().getNodeValue());

        range.selectNodeContents(xwiki);
        select(range);
        assertEquals(H1, executable.getParameter(rta));

        range.selectNodeContents(best);
        select(range);
        assertEquals(P, executable.getParameter(rta));
    }

    /**
     * Unit test for inserting header around the selected text.
     */
    public void testInsertHeaderAroundSelection()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestInsertHeaderAroundSelection();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for inserting header around the selected text.
     */
    private void doTestInsertHeaderAroundSelection()
    {
        rta.setHTML("<strong>toucan</strong> is <!--my--><em>favorite</em> skin");
        Node toucan = getBody().getFirstChild();

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getChildNodes().getItem(3).getFirstChild(), 2);
        range.setStart(toucan.getFirstChild(), 3);
        select(range);
        String selectedText = "can is fa";
        assertEquals(selectedText, range.toString());
        assertTrue(executable.execute(rta, H1));
        assertEquals("<h1><strong>toucan</strong> is <!--my--><em>favorite</em> skin</h1>", clean(rta.getHTML()));
        assertEquals(H1, executable.getParameter(rta));

        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(selectedText, range.toString());

        range.selectNodeContents(toucan);
        select(range);
        assertEquals(H1, executable.getParameter(rta));
    }

    /**
     * Unit test for changing a paragraph formatting to in-line.
     */
    public void testRemoveParagraph()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRemoveParagraph();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for changing a paragraph formatting to in-line.
     */
    private void doTestRemoveParagraph()
    {
        rta.setHTML("once <p>upon a <ins>time</ins></p> there..");

        // We need to do some adjustments for IE because it strips leading spaces after a block element.
        getBody().getLastChild().setNodeValue(" there..");

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getChildNodes().getItem(1).getLastChild().getFirstChild(), 3);
        range.setStart(getBody().getFirstChild(), 1);
        select(range);
        assertTrue(executable.execute(rta, ""));
        assertEquals("once upon a <ins>time</ins> there..", clean(rta.getHTML()));
        assertEquals("", executable.getParameter(rta));

        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals("nce upon a tim", range.toString());
    }

    /**
     * Unit test for changing the current block format.
     */
    public void testChangeBlock()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestChangeBlock();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for changing the current block format.
     */
    private void doTestChangeBlock()
    {
        rta.setHTML("a <h1>b</h1> c <h2>d</h2> e");

        // We need to do some adjustments for IE because it strips leading spaces after a block element.
        getBody().getChildNodes().getItem(2).setNodeValue(" c ");
        getBody().getLastChild().setNodeValue(" e");

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        range.setEnd(getBody().getChildNodes().getItem(3).getFirstChild(), 1);
        range.setStart(getBody().getChildNodes().getItem(1).getFirstChild(), 0);
        String selectedText = "b c d";
        assertEquals(selectedText, clean(range.toString()));

        select(range);
        assertNull(executable.getParameter(rta));
        assertTrue(executable.execute(rta, P));
        assertEquals("a <p>b</p><p> c </p><p>d</p> e", clean(rta.getHTML()));
        assertEquals(P, executable.getParameter(rta));

        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(selectedText, clean(range.toString()));
    }

    /**
     * Tests if the header is detected correctly when the caret is inside a {@code span} with {@code display:block}.
     * 
     * @see XWIKI-3109: Headers generated from wiki syntax look and behave differently
     */
    public void testDetectHeaderFromASpanWithDisplayBlock()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestDetectHeaderFromASpanWithDisplayBlock();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests if the header is detected correctly when the caret is inside a {@code span} with {@code display:block}.
     * 
     * @see XWIKI-3109: Headers generated from wiki syntax look and behave differently
     */
    private void doTestDetectHeaderFromASpanWithDisplayBlock()
    {
        rta.setHTML("<h1><span style=\"display: block;\">header</span></h1>");

        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 3);
        range.collapse(true);
        select(range);

        assertEquals(H1, executable.getParameter(rta));
        assertTrue(executable.execute(rta, P));
        assertEquals("<p><span style=\"display: block;\">header</span></p>", clean(rta.getHTML()));
        assertEquals(P, executable.getParameter(rta));

        range = rta.getDocument().getSelection().getRangeAt(0);
        assertTrue(range.isCollapsed());
        assertEquals(3, range.getStartOffset());
    }
}
