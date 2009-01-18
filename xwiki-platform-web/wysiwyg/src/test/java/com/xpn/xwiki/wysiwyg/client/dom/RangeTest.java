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
package com.xpn.xwiki.wysiwyg.client.dom;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link Range}.
 * 
 * @version $Id$
 */
public class RangeTest extends AbstractWysiwygClientTest
{
    /**
     * The document in which we run the tests.
     */
    private Document document;

    /**
     * The DOM element in which we run the tests.
     */
    private Element container;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        document = Document.get().cast();
        container = document.xCreateDivElement().cast();
        document.getBody().appendChild(container);
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
     * Unit test for {@link Range#toHTML()}.
     */
    public void testToHTML()
    {
        container.setInnerHTML("<strong>aa</strong>b<em>cc</em>");

        Range range = document.createRange();
        range.setStart(container.getFirstChild().getFirstChild(), 1);
        range.setEnd(container.getLastChild().getFirstChild(), 1);

        assertEquals("<strong>a</strong>b<em>c</em>", range.toHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Range#extractContents()}.
     */
    public void testExtractContents()
    {
        container.setInnerHTML("ab<em>c</em><span>d<del><!--x-->ef</del></span>");

        Range range = document.createRange();
        range.setStart(container.getFirstChild(), 1);
        range.setEnd(container.getLastChild().getLastChild().getLastChild(), 2);

        Node span = container.getLastChild();
        Node em = container.getChildNodes().getItem(1);
        Node comment = container.getLastChild().getLastChild().getFirstChild();

        DocumentFragment extract = range.extractContents();
        assertTrue(range.isCollapsed());
        assertEquals("a<span><del></del></span>", container.getInnerHTML().toLowerCase());
        assertEquals("b<em>c</em><span>d<del><!--x-->ef</del></span>", extract.getInnerHTML().toLowerCase());
        assertEquals(em, extract.getChildNodes().getItem(1));
        assertEquals(comment, extract.getLastChild().getLastChild().getFirstChild());
        assertEquals(span, container.getLastChild());
    }

    /**
     * Unit test for {@link Range#insertNode(Node)}.
     */
    public void testInsertNode()
    {
        container.setInnerHTML("ab<ins>c</ins><strong>d</strong>");

        Range range = document.createRange();
        range.setStart(container.getFirstChild(), 1);
        range.setEnd(container.getFirstChild(), 2);

        range.insertNode(document.createTextNode("#"));
        assertEquals("#b", range.toString());

        range.setEndBefore(container.getLastChild());
        assertEquals("#b<ins>c</ins>", range.toHTML().toLowerCase());

        range.insertNode(document.xCreateImageElement());
        assertEquals("<img>#b<ins>c</ins>", range.toHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Range#surroundContents(Node)}.
     */
    public void testSurroundContents()
    {
        container.setInnerHTML("ab<sub>cd</sub>e");

        Range range = document.createRange();
        range.setStart(container.getFirstChild(), 1);
        range.setEnd(container.getChildNodes().getItem(1).getFirstChild(), 1);

        try {
            range.surroundContents(document.xCreateSpanElement());
            fail("The range partially selects a non-Text node.");
        } catch (Throwable t) {
            assertTrue(t instanceof IllegalStateException);
        }

        container.setInnerHTML("ab<sup>c</sup>de");

        // NOTE: we could reuse the range but IE breaks or throws InvalidArgument exception when we access the fields of
        // a DOM node that became orphan/detached after we overwrite the inner HTML of one of its ancestors.
        range = document.createRange();
        range.setStart(container.getFirstChild(), 1);
        range.setEnd(container.getLastChild(), 1);

        range.surroundContents(document.xCreateSpanElement());
        assertEquals("<span>b<sup>c</sup>d</span>", range.toHTML().toLowerCase());
        assertEquals("a<span>b<sup>c</sup>d</span>e", container.getInnerHTML().toLowerCase());
    }
}
