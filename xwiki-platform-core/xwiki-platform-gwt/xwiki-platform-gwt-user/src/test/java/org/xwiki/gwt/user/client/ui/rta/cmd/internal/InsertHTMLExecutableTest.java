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

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextAreaTestCase;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

import com.google.gwt.user.client.Command;

/**
 * Unit tests for {@link InsertHTMLExecutable}.
 * 
 * @version $Id$
 */
public class InsertHTMLExecutableTest extends RichTextAreaTestCase
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (executable == null) {
            executable = new InsertHTMLExecutable(rta);
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
                assertTrue(executable.execute("<!--x-->y<del>z</del>"));
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
                range.selectNode(getBody().getLastChild());
                select(range);

                assertEquals("ij", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("#"));
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
                assertTrue(executable.execute("*2<em>=</em>1+"));
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
                assertTrue(executable.execute("<img/>"));
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
        assertTrue(executable.execute("<img title=\"march 11th\"/>"));
        assertEquals("2<img title=\"march 11th\">9", clean(rta.getHTML()));

        // Lets test if the selection wraps the inserted image.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertEquals(getBody(), range.getStartContainer());
        assertEquals(getBody(), range.getEndContainer());
        assertEquals(1, range.getStartOffset());
        assertEquals(2, range.getEndOffset());
    }

    /**
     * Tests if an anchor is properly replaced. What is important is that the caret doesn't move inside a sibling node
     * after the selected anchor is deleted. The caret must remain between nodes so that the HTML fragment is inserted
     * in the right place.
     */
    public void testReplaceAnchor()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<ul><li><a href=\"http://www.xwiki.com\">XWiki</a><sup>tm</sup></li></ul>");

                Range range = rta.getDocument().createRange();
                range.selectNode(getBody().getFirstChild().getFirstChild().getFirstChild());
                select(range);

                assertEquals("XWiki", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("x"));
                assertEquals("x<sup>tm</sup>", Element.as(getBody().getFirstChild().getFirstChild()).getInnerHTML()
                    .toLowerCase());
            }
        });
    }

    /**
     * Tests if the selected text is correctly deleted when it is followed by a space character.
     */
    public void testReplaceTextFollowedBySpace()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p><span>before</span> after</p>");

                Range range = rta.getDocument().createRange();
                range.selectNodeContents(getBody().getFirstChild().getFirstChild());
                select(range);

                assertEquals("before", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("start"));
                assertEquals("<p><span>start</span>&nbsp;after</p>", getBody().getInnerHTML().toLowerCase());
            }
        });
    }

    /**
     * Tests if a button is properly replaced. What is important is that the caret doesn't move inside a sibling node
     * after the selected button is deleted. The caret must remain between nodes so that the HTML fragment is inserted
     * in the right place.
     * <p>
     * NOTE: We need a special test for replacing a button because buttons support control selection, unlike anchors, so
     * they are deleted in another way.
     */
    public void testReplaceButton()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p>before</p><button>albatross</button><p>after</p>");

                Range range = rta.getDocument().createRange();
                range.selectNode(getBody().getChildNodes().getItem(1));
                select(range);

                assertEquals("albatross", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("<button>toucan</button>"));
                assertEquals("toucan", getBody().getChildNodes().getItem(1).getFirstChild().getNodeValue());
            }
        });
    }

    /**
     * Selects the first characters of a text node and replaces them.
     */
    public void testReplaceTextStart()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p>123</p><p><em>#</em>456</p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getLastChild().getLastChild(), 0);
                range.setEnd(getBody().getLastChild().getLastChild(), 2);
                select(range);

                assertEquals("45", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("87"));
                assertEquals("<em>#</em>876", Element.as(getBody().getLastChild()).getInnerHTML().toLowerCase());
            }
        });
    }

    /**
     * Selects the last characters of a text node and replaces them.
     */
    public void testReplaceTextEnd()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p>123<em>#</em></p><p>456</p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild(), 1);
                range.setEnd(getBody().getFirstChild().getFirstChild(), 3);
                select(range);

                assertEquals("23", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("xx"));
                assertEquals("1xx<em>#</em>", Element.as(getBody().getFirstChild()).getInnerHTML().toLowerCase());
            }
        });
    }

    /**
     * Tests if an in-line HTML element is properly replaced when its parent element contains unnormalized text.
     */
    public void testReplaceInlineElementWhenUnnormalizedTextIsPresent()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p>2<em>3</em></p>");
                getBody().getFirstChild().insertBefore(rta.getDocument().createTextNode(""),
                    getBody().getFirstChild().getFirstChild());
                getBody().getFirstChild().insertBefore(rta.getDocument().createTextNode("1"),
                    getBody().getFirstChild().getFirstChild());

                Range range = rta.getDocument().createRange();
                range.selectNode(getBody().getFirstChild().getLastChild());
                select(range);

                assertEquals("3", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("4"));
                assertEquals("124", Element.as(getBody().getFirstChild()).getInnerHTML().toLowerCase());
            }
        });
    }

    /**
     * Replaces a selection that starts in a text node which isn't modified after the selected content is deleted. This
     * is possible if the part that isn't deleted from the text where the selection ends matches the part that is
     * deleted from the text where the selection starts.
     */
    public void testReplaceSelectionThatStartsInATextNodeWhichIsntModifiedByTheDelete()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p>abc<em>de</em>fbc<strong>#</strong></p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild(), 1);
                range.setEnd(getBody().getFirstChild().getChildNodes().getItem(2), 1);
                select(range);

                assertEquals("bcdef", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("?"));
                assertEquals("a?bc<strong>#</strong>", getBody().getFirstChildElement().getInnerHTML().toLowerCase());
            }
        });
    }

    /**
     * Replaces a selection that crosses two in-line elements.
     */
    public void testReplaceCrossInlineElementSelection()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                // We added an image to the selection to be sure that at least one DOM node is deleted.
                rta.setHTML("<p><em>123</em><img/><strong>456</strong></p>");

                Range range = rta.getDocument().createRange();
                range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 2);
                range.setEnd(getBody().getFirstChild().getLastChild().getFirstChild(), 1);
                select(range);

                assertEquals("34", rta.getDocument().getSelection().toString());
                assertTrue(executable.execute("+"));
                assertEquals("<em>12+</em><strong>56</strong>", getBody().getFirstChildElement().getInnerHTML()
                    .toLowerCase());
            }
        });
    }

    /**
     * Replaces the entire text of a paragraph.
     */
    public void testReplaceParagraphText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<p>alice</p><p>bob</p><p>carol</p>");

                Range range = rta.getDocument().createRange();
                range.selectNodeContents(getBody().getChildNodes().getItem(1));
                select(range);

                assertEquals("bob", rta.getDocument().getSelection().toString());
                String insertedText = "*";
                assertTrue(executable.execute(insertedText));
                assertEquals(insertedText, Element.as(getBody().getChildNodes().getItem(1)).getInnerText());
            }
        });
    }
}
