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
package org.xwiki.gwt.dom.client;

import com.google.gwt.dom.client.Node;

/**
 * Unit tests for {@link Range}.
 * 
 * @version $Id$
 */
public class RangeTest extends DOMTestCase
{
    /**
     * Unit test for {@link Range#toHTML()}.
     */
    public void testToHTML()
    {
        getContainer().setInnerHTML("<strong>aa</strong>b<em>cc</em>");

        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild().getFirstChild(), 1);
        range.setEnd(getContainer().getLastChild().getFirstChild(), 1);

        assertEquals("<strong>a</strong>b<em>c</em>", range.toHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Range#extractContents()}.
     */
    public void testExtractContents()
    {
        getContainer().setInnerHTML("ab<em>c</em><span>d<del><!--x-->ef</del></span>");

        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 1);
        range.setEnd(getContainer().getLastChild().getLastChild().getLastChild(), 2);

        Node span = getContainer().getLastChild();
        Node em = getContainer().getChildNodes().getItem(1);
        Node comment = getContainer().getLastChild().getLastChild().getFirstChild();

        DocumentFragment extract = range.extractContents();
        assertTrue(range.isCollapsed());
        assertEquals("a<span><del></del></span>", getContainer().getInnerHTML().toLowerCase());
        assertEquals("b<em>c</em><span>d<del><!--x-->ef</del></span>", extract.getInnerHTML().toLowerCase());
        assertEquals(em, extract.getChildNodes().getItem(1));
        assertEquals(comment, extract.getLastChild().getLastChild().getFirstChild());
        assertEquals(span, getContainer().getLastChild());
    }

    /**
     * Unit test for {@link Range#insertNode(Node)}.
     */
    public void testInsertNode()
    {
        getContainer().setInnerHTML("ab<ins>c</ins><strong>d</strong>");

        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 1);
        range.setEnd(getContainer().getFirstChild(), 2);

        range.insertNode(getDocument().createTextNode("#"));
        assertEquals("#b", range.toString());

        range.setEndBefore(getContainer().getLastChild());
        assertEquals("#b<ins>c</ins>", range.toHTML().toLowerCase());

        range.insertNode(getDocument().createImageElement());
        assertEquals("<img>#b<ins>c</ins>", normalizeHTML(range.toHTML()));
    }

    /**
     * Unit test for {@link Range#surroundContents(Node)}.
     */
    public void testSurroundContents()
    {
        getContainer().setInnerHTML("ab<sub>cd</sub>e");

        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 1);
        range.setEnd(getContainer().getChildNodes().getItem(1).getFirstChild(), 1);

        try {
            range.surroundContents(getDocument().createSpanElement());
            fail("The range partially selects a non-Text node.");
        } catch (Throwable t) {
            assertTrue(t instanceof IllegalStateException);
        }

        getContainer().setInnerHTML("ab<sup>c</sup>de");

        // NOTE: we could reuse the range but IE breaks or throws InvalidArgument exception when we access the fields of
        // a DOM node that became orphan/detached after we overwrite the inner HTML of one of its ancestors.
        range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 1);
        range.setEnd(getContainer().getLastChild(), 1);

        range.surroundContents(getDocument().createSpanElement());
        assertEquals("<span>b<sup>c</sup>d</span>", range.toHTML().toLowerCase());
        assertEquals("a<span>b<sup>c</sup>d</span>e", getContainer().getInnerHTML().toLowerCase());
    }
}
