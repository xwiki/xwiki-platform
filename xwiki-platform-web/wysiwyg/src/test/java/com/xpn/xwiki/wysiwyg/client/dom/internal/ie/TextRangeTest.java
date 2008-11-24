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
package com.xpn.xwiki.wysiwyg.client.dom.internal.ie;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;
import com.xpn.xwiki.wysiwyg.client.dom.Text;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.TextRange.Unit;

/**
 * Unit tests for {@link TextRange}.
 * 
 * @version $Id$
 */
public class TextRangeTest extends AbstractWysiwygClientTest
{
    /**
     * Used to mark the start or end of a text range.
     */
    public static final String PIPE = "|";

    /**
     * The DOM element in which we run the tests.
     */
    private Element container;

    /**
     * The tested text range.
     */
    private TextRange textRange;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        container = ((Document) Document.get()).xCreateDivElement().cast();
        Document.get().getBody().appendChild(container);
        textRange = TextRange.newInstance((Document) Document.get());
        textRange.moveToElementText(container);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtTearDown()
     */
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();

        container.getParentNode().removeChild(container);
    }

    /**
     * Places the pipe ("|") symbol before and after the given range.
     * 
     * @param range The range to be wrapped.
     */
    public static void wrap(TextRange range)
    {
        TextRange clone = range.duplicate();
        clone.collapse(true);
        clone.setText(PIPE);

        if (!range.isCollapsed()) {
            clone = range.duplicate();
            clone.collapse(false);
            clone.setText(PIPE);
        }
    }

    /**
     * Unit test for {@link TextRange#moveToTextNode(Text)}.
     */
    public void testMoveToTextNode()
    {
        container.xSetInnerHTML("a<em>b</em>c");
        textRange.moveToTextNode((Text) container.getChildNodes().getItem(1).getFirstChild());
        wrap(textRange);
        assertEquals("a<em>|b|</em>c", container.getInnerHTML().toLowerCase());

        container.xSetInnerHTML("a<span>b<!--x-->c<img/></span>d");
        textRange.moveToTextNode((Text) container.getChildNodes().getItem(1).getChildNodes().getItem(2));
        wrap(textRange);
        assertEquals("a<span>b<!--x-->|c|<img></span>d", container.getInnerHTML().toLowerCase());

        container.xSetInnerHTML("b<span><img/>a<!--x-->c</span>d");
        textRange.moveToTextNode((Text) container.getChildNodes().getItem(1).getChildNodes().getItem(1));
        wrap(textRange);
        assertEquals("b<span><img>|a|<!--x-->c</span>d", container.getInnerHTML().toLowerCase());

        container.xSetInnerHTML("c<span>d<em style=\"width: 30px\"></em>e</span>f");
        textRange.moveToTextNode((Text) container.getChildNodes().getItem(1).getLastChild());
        wrap(textRange);
        assertEquals("c<span>d<em style=\"width: 30px\"></em>|e|</span>f", container.getInnerHTML().toLowerCase());

        container.xSetInnerHTML("a<em></em>b");
        Element parent = container.getChildNodes().getItem(1).cast();
        parent.appendChild(container.getOwnerDocument().createTextNode(""));
        textRange.moveToTextNode((Text) parent.getFirstChild());
        textRange.select();
        assertEquals(" ", ((TextRange) NativeSelection.getInstance(textRange.getOwnerDocument()).createRange())
            .getText());
        wrap(textRange);
        assertEquals("a<em>| </em>b", container.getInnerHTML().toLowerCase());

        container.xSetInnerHTML("a<em><del></del></em>b");
        parent = container.getChildNodes().getItem(1).cast();
        parent.insertBefore(container.getOwnerDocument().createTextNode(""), parent.getFirstChild());
        textRange.moveToTextNode((Text) parent.getFirstChild());
        wrap(textRange);
        assertEquals("a<em>|<del></del></em>b", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link TextRange#setEndPoint(RangeCompare, com.google.gwt.dom.client.Node, int)}.
     */
    public void testSetEndPoint()
    {
        container.setInnerHTML("a<em>bcd</em>e");
        textRange.setEndPoint(RangeCompare.START_TO_START, container.getChildNodes().getItem(1).getFirstChild(), 2);
        textRange.setEndPoint(RangeCompare.END_TO_END, container.getChildNodes().getItem(1).getFirstChild());
        assertEquals("d", textRange.getHTML());

        container.setInnerHTML("a<ins>b</ins>c");
        textRange.setEndPoint(RangeCompare.END_TO_START, container.getFirstChild());
        textRange.setEndPoint(RangeCompare.START_TO_END, container.getLastChild());
        assertEquals("<ins>b</ins>", textRange.getHTML().toLowerCase());

        container.setInnerHTML("<em>x</em>y<del>z</del>");
        textRange.setEndPoint(RangeCompare.END_TO_START, container.getFirstChild());
        textRange.setEndPoint(RangeCompare.START_TO_END, container.getLastChild());
        assertEquals("y", textRange.getHTML());

        container.setInnerHTML("<em>x</em><!--#-->y<ins style=\"width: 30px\"></ins><del>z</del>");
        textRange.setEndPoint(RangeCompare.END_TO_START, container.getFirstChild());
        textRange.setEndPoint(RangeCompare.START_TO_END, container.getLastChild());
        assertEquals("<!--#-->y<ins style=\"width: 30px\"></ins>", textRange.getHTML().toLowerCase());
    }

    /**
     * Unit test for {@link TextRange#getHTML()}.
     */
    public void testGetHTML()
    {
        container.setInnerHTML("e<del>fg</del>h");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        textRange.moveStart(Unit.CHARACTER, 1);
        assertEquals("g", textRange.getHTML());

        container.setInnerHTML("1<em><!--x-->2</em>");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        textRange.setEndPoint(RangeCompare.START_TO_END, textRange);
        textRange.setEndPoint(RangeCompare.START_TO_START, container.getFirstChild());
        assertEquals("1", textRange.getHTML());

        container.setInnerHTML("1+1<strong>=</strong>2");
        textRange.moveToTextNode((Text) container.getChildNodes().getItem(1).getFirstChild());
        // assertEquals("=", textRange.getHTML());
        assertEquals("=", textRange.getText());

        container.setInnerHTML("a<ins>bc</ins>d<del>ef</del>g");
        textRange.setEndPoint(RangeCompare.START_TO_START, container.getChildNodes().getItem(1).getFirstChild(), 1);
        textRange.setEndPoint(RangeCompare.START_TO_END, container.getChildNodes().getItem(3).getFirstChild(), 1);
        assertEquals("<ins>c</ins>d<del>e</del>", textRange.getHTML().toLowerCase());
    }

    /**
     * Unit test for {@link TextRange#setHTML(String)}.
     */
    public void testSetHTML()
    {
        container.setInnerHTML("ab");
        textRange.moveToElementText(container);
        textRange.move(Unit.CHARACTER, 1);
        textRange.moveEnd(Unit.CHARACTER, 1);
        textRange.setHTML("<em>b</em>");
        assertEquals("a<em>b</em>", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("e<em><!--x-->y</em>f");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        textRange.setHTML("<!--#-->z");
        assertEquals("e<!--#-->zf", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("e<em><!--x-->yz</em>f");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        textRange.moveStart(Unit.CHARACTER, 1);
        textRange.shift(Unit.CHARACTER, -1);
        textRange.setHTML("+");
        assertEquals("e<em><!--x-->+z</em>f", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("a<em>bc</em>d");
        textRange.moveToTextNode((Text) container.getChildNodes().getItem(1).getFirstChild());
        textRange.collapse(true);
        textRange.moveEnd(Unit.CHARACTER, 1);
        textRange.setHTML("#");
        assertEquals("a<em>#c</em>d", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link TextRange#moveToElementText(Element)}.
     */
    public void testMoveToElementText()
    {
        container.setInnerHTML("$<del></del>#");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        assertEquals("", textRange.getHTML());
        textRange.setHTML("%");
        assertEquals("$<del>%</del>#", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("$<img/>#");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        assertEquals("<img>", textRange.getHTML().toLowerCase());
        textRange.setHTML("@");
        assertEquals("$@#", container.getInnerHTML());

        // The following two test are just for reminding how odd moveToElementText behaves.
        container.setInnerHTML("r<span><!--x-->a<em>#</em></span>q");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        wrap(textRange);
        assertEquals("r|<span><!--x-->a<em>#|</em></span>q", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("r<div><!--x-->a<em>#</em></div>q");
        textRange.moveToElementText((Element) container.getChildNodes().getItem(1));
        wrap(textRange);
        assertEquals("r\r\n<div><!--x-->|a<em>#</em></div>|q", container.getInnerHTML().toLowerCase());
    }
}
