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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link DOMUtils}.
 * 
 * @version $Id$
 */
public class DOMUtilsTest extends AbstractWysiwygClientTest
{
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

        container = ((Document) Document.get()).xCreateDivElement().cast();
        Document.get().getBody().appendChild(container);
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
     * Unit test for {@link DOMUtils#getTextRange(Range)}.
     */
    public void testGetTextRange()
    {
        container.setInnerHTML("x<a href=\"http://www.xwiki.org\"><strong>a</strong>b<em>cd</em></a>y");

        Range range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(container, 1);
        range.setEnd(container, 2);

        Range textRange = DOMUtils.getInstance().getTextRange(range);

        assertEquals(range.toString(), textRange.toString());

        assertEquals(Node.TEXT_NODE, textRange.getStartContainer().getNodeType());
        assertEquals(Node.TEXT_NODE, textRange.getEndContainer().getNodeType());

        assertEquals(0, textRange.getStartOffset());
        assertEquals(2, textRange.getEndOffset());
    }

    /**
     * Unit test for {@link DOMUtils#getTextRange(Range)} when the input range ends inside a comment node.
     */
    public void testGetTextRangeFromARangeEndingInAComment()
    {
        container.setInnerHTML("a<!--xy-->");

        Range range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(container.getFirstChild(), container.getFirstChild().getNodeValue().length());
        range.setEnd(container.getLastChild(), container.getLastChild().getNodeValue().length());

        Range textRange = DOMUtils.getInstance().getTextRange(range);

        assertEquals(range.toString(), textRange.toString());
        assertEquals(Node.TEXT_NODE, textRange.getStartContainer().getNodeType());
        assertEquals(textRange.getStartContainer(), textRange.getEndContainer());
        assertEquals(1, textRange.getStartOffset());
        assertEquals(1, textRange.getEndOffset());
    }

    /**
     * Test the first ancestor search, on a wikilink-like HTML structure.
     * 
     * @see DOMUtils#getFirstAncestor(Node, String)
     */
    public void testGetFirstAncestor()
    {
        container.setInnerHTML("<div>our<!--startwikilink:Reference--><span class=\"wikilink\">"
            + "<a>x<strong>wiki</strong></a></span><!--stopwikilink-->rox</div>");
        Node wrappingSpan = container.getFirstChild().getChildNodes().getItem(2);
        Node anchor = wrappingSpan.getFirstChild();
        Node boldWiki = anchor.getChildNodes().getItem(1);
        Node labelBoldWiki = boldWiki.getFirstChild();
        String anchorTagName = anchor.getNodeName();

        // check if there is a first ancestor of type a for the bold inside the anchor
        assertSame("There isn't an anchor ancestor for the bold inside the anchor", anchor, DOMUtils.getInstance()
            .getFirstAncestor(boldWiki, anchorTagName));
        // check if there is a first ancestor of type a for the text inside bold in the anchor
        assertSame("There isn't an anchor ancestor for the text in the bold inside the anchor", anchor, DOMUtils
            .getInstance().getFirstAncestor(labelBoldWiki, anchorTagName));
        // check there is no a ancestor of the wikilink span
        assertNull("There is an anchor ancestor for the wikilink span", DOMUtils.getInstance().getFirstAncestor(
            wrappingSpan, anchorTagName));
        // check a finds itself as ancestor
        assertSame("The anchor is not an anhor ancestor of itself", anchor, DOMUtils.getInstance().getFirstAncestor(
            anchor, anchorTagName));
        // check div ancestor search stops at startContainer
        assertSame("Div ancestor search for the anchor does not stop at first div", container.getFirstChild(), DOMUtils
            .getInstance().getFirstAncestor(anchor, container.getTagName()));
    }

    /**
     * Test the first descendant search function, on a wikilink-like HTML structure.
     * 
     * @see DOMUtils#getFirstDescendant(Node, String)
     */
    public void testGetFirstDescendant()
    {
        container.setInnerHTML("<div>my<!--startwikilink:Reference--><span class=\"wikilink\">"
            + "<a>x<span>wiki</span></a></span><!--stopwikilink-->rules</div>");
        Node wrappingSpan = container.getFirstChild().getChildNodes().getItem(2);
        Node anchor = wrappingSpan.getFirstChild();
        Node preambleText = container.getFirstChild().getFirstChild();
        String anchorTagName = anchor.getNodeName();

        // check anchor shows up as descendant of startContainer
        assertSame("Anchor does not show up as descendant of startContainer", anchor, DOMUtils.getInstance()
            .getFirstDescendant(container.getFirstChild(), anchorTagName));
        // check anchor shows up as descendant of itself
        assertSame("Anchor does not show up as descendant of itself", anchor, DOMUtils.getInstance()
            .getFirstDescendant(anchor, anchorTagName));
        // check there is no descendant of type bold in the wrapping span
        assertNull("There is a descendant of type bold in the wrapping span", DOMUtils.getInstance()
            .getFirstDescendant(wrappingSpan, "strong"));
        // check the first span descendant stops at the wrapping span
        assertSame("The first span descendant does not stop at the wrapping span", wrappingSpan, DOMUtils.getInstance()
            .getFirstDescendant(container, wrappingSpan.getNodeName()));
        // check there is no anchor descendant of a text
        assertNull("There is an anchor descendant of a text", DOMUtils.getInstance().getFirstDescendant(preambleText,
            anchorTagName));
    }

    /**
     * Unit test for {@link DOMUtils#getAncestors(Node)}.
     */
    public void testGetAncestors()
    {
        container.setInnerHTML("<em>x</em>");
        List<Node> ancestors = new ArrayList<Node>();
        ancestors.add(container.getFirstChild().getFirstChild());
        ancestors.add(container.getFirstChild());
        ancestors.add(container);
        ancestors.add(container.getOwnerDocument().getBody());
        ancestors.add(((Document) container.getOwnerDocument()).getDocumentElement());
        ancestors.add(container.getOwnerDocument());
        assertEquals(ancestors, DOMUtils.getInstance().getAncestors(ancestors.get(0)));
    }

    /**
     * Unit test for {@link DOMUtils#getNearestCommonAncestor(Node, Node)}.
     */
    public void testGetNearestCommonAncestor()
    {
        container.setInnerHTML("<em>x</em>y<del>z</del>");
        assertEquals(container, DOMUtils.getInstance().getNearestCommonAncestor(
            container.getFirstChild().getFirstChild(), container.getLastChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#cloneNodeContents(Node, int, int)}.
     */
    public void testCloneNodeContents()
    {
        container.setInnerHTML("xwiki<span><em>$</em><ins>#</ins></span>");

        DocumentFragment contents = DOMUtils.getInstance().cloneNodeContents(container.getFirstChild(), 0, 2);
        assertEquals(1, contents.getChildNodes().getLength());
        assertEquals("xw", contents.getInnerHTML());

        contents = DOMUtils.getInstance().cloneNodeContents(container.getFirstChild(), 5, 5);
        assertEquals(0, contents.getChildNodes().getLength());

        contents = DOMUtils.getInstance().cloneNodeContents(container.getLastChild(), 0, 2);
        assertEquals(2, contents.getChildNodes().getLength());
        assertEquals("<em>$</em><ins>#</ins>", contents.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, int, int)}.
     */
    public void testCloneNodeBetweenOffsets()
    {
        container.setInnerHTML("toucan<span><em>+</em><ins>-</ins></span>");
        assertEquals("an", DOMUtils.getInstance().cloneNode(container.getFirstChild(), 4, 6).getNodeValue());
        assertEquals("", DOMUtils.getInstance().cloneNode(container.getFirstChild(), 0, 0).getNodeValue());

        Element clone = DOMUtils.getInstance().cloneNode(container.getLastChild(), 1, 2).cast();
        assertEquals("<ins>-</ins>", clone.getInnerHTML().toLowerCase());

        clone = DOMUtils.getInstance().cloneNode(container.getLastChild(), 0, 0).cast();
        assertEquals("<span></span>", clone.getString().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getLength(Node)}.
     */
    public void testGetLength()
    {
        container.setInnerHTML("xwiki<strong></strong><ins>x<del>y</del>z</ins>");
        assertEquals(5, DOMUtils.getInstance().getLength(container.getFirstChild()));
        assertEquals(0, DOMUtils.getInstance().getLength(container.getChildNodes().getItem(1)));
        assertEquals(3, DOMUtils.getInstance().getLength(container.getLastChild()));
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, int, boolean)}.
     */
    public void testCloneNodeLeftRight()
    {
        container.setInnerHTML("abc");
        assertEquals("ab", DOMUtils.getInstance().cloneNode(container.getFirstChild(), 2, true).getNodeValue());
        assertEquals("c", DOMUtils.getInstance().cloneNode(container.getFirstChild(), 2, false).getNodeValue());

        container.setInnerHTML("a<!--x--><em>b</em>");

        assertEquals("a<!--x-->", ((Element) DOMUtils.getInstance().cloneNode(container, 2, true)).getInnerHTML()
            .toLowerCase());
        assertEquals("<em>b</em>", ((Element) DOMUtils.getInstance().cloneNode(container, 2, false)).getInnerHTML()
            .toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, Node, int, boolean)}.
     */
    public void testCloneNodeUpwards()
    {
        container.setInnerHTML("<em><ins>abc<del>d</del></ins></em>e");

        Element clone =
            DOMUtils.getInstance().cloneNode(container.getParentNode(),
                container.getFirstChild().getFirstChild().getFirstChild(), 2, false).cast();
        assertEquals("<em><ins>c<del>d</del></ins></em>e", clone.getInnerHTML().toLowerCase());

        clone =
            DOMUtils.getInstance().cloneNode(container.getParentNode(), container.getFirstChild().getFirstChild(), 1,
                true).cast();
        assertEquals("<em><ins>abc</ins></em>", clone.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getChild(Node, Node)}.
     */
    public void testGetChild()
    {
        container.setInnerHTML("%<strong>@<em>^</em></strong>");
        assertEquals(container.getFirstChild(), DOMUtils.getInstance().getChild(container, container.getFirstChild()));
        assertEquals(container.getLastChild(), DOMUtils.getInstance().getChild(container,
            container.getLastChild().getLastChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, int, int)}.
     */
    public void testDeleteNodeContentsBetweenOffsets()
    {
        container.setInnerHTML("foo");
        DOMUtils.getInstance().deleteNodeContents(container.getFirstChild(), 1, 2);
        assertEquals("fo", container.getInnerHTML());

        container.setInnerHTML("<em>1</em>2<!--3-->");
        DOMUtils.getInstance().deleteNodeContents(container, 1, 2);
        assertEquals("<em>1</em><!--3-->", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, int, boolean)}.
     */
    public void testDeleteNodeContentsLeftRight()
    {
        container.setInnerHTML("foo<del></del>bar");
        DOMUtils.getInstance().deleteNodeContents(container.getFirstChild(), 2, true);
        assertEquals("o<del></del>bar", container.getInnerHTML().toLowerCase());
        DOMUtils.getInstance().deleteNodeContents(container.getLastChild(), 0, false);
        assertEquals("o<del></del>", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("<ins>1</ins><!--2-->3");
        DOMUtils.getInstance().deleteNodeContents(container, 1, true);
        assertEquals("<!--2-->3", container.getInnerHTML());
        DOMUtils.getInstance().deleteNodeContents(container, 1, false);
        assertEquals("<!--2-->", container.getInnerHTML());
    }

    /**
     * Unit test for {@link DOMUtils#deleteSiblings(Node, boolean)}.
     */
    public void testDeleteSiblings()
    {
        container.setInnerHTML("1<strong>2</strong><!--3-->");
        DOMUtils.getInstance().deleteSiblings(container.getFirstChild(), true);
        assertEquals(3, container.getChildNodes().getLength());
        DOMUtils.getInstance().deleteSiblings(container.getFirstChild(), false);
        assertEquals("1", container.getInnerHTML());
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, Node, int, boolean)}.
     */
    public void testDeleteNodeContentsUpwards()
    {
        container.setInnerHTML("<span>x<em>y<!--z--><del>wiki</del></em></span>");
        DOMUtils.getInstance().deleteNodeContents(container,
            container.getFirstChild().getChildNodes().getItem(1).getChildNodes().getItem(2).getFirstChild(), 2, true);
        assertEquals("<span><em><del>ki</del></em></span>", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("<span><em><del>wiki</del><!--z-->y</em>x</span>");
        DOMUtils.getInstance().deleteNodeContents(container,
            container.getFirstChild().getFirstChild().getFirstChild().getFirstChild(), 1, false);
        assertEquals("<span><em><del>w</del></em></span>", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#splitNode(Node, int)}.
     */
    public void testSplitNodeAtOffset()
    {
        container.setInnerHTML("xwiki");
        Node rightNode = DOMUtils.getInstance().splitNode(container.getFirstChild(), 3);
        assertEquals(container.getLastChild(), rightNode);
        assertEquals(2, container.getChildNodes().getLength());
        assertEquals("xwi", container.getFirstChild().getNodeValue());
        assertEquals("ki", container.getLastChild().getNodeValue());

        container.setInnerHTML("q<span><!--x--><em>w</em>e</span>rty");
        rightNode = DOMUtils.getInstance().splitNode(container.getChildNodes().getItem(1), 0);
        assertEquals("<span><!--x--><em>w</em>e</span>", Element.as(rightNode).getString().toLowerCase());
        assertEquals("q<span></span><span><!--x--><em>w</em>e</span>rty", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#splitNode(Node, Node, int)}.
     */
    public void testSplitNodeUpwards()
    {
        container.setInnerHTML("u<del>v<strong><ins><!--x-->y</ins>z</strong><em>a</em></del>b");
        Node rightNode =
            DOMUtils.getInstance().splitNode(container,
                container.getChildNodes().getItem(1).getChildNodes().getItem(1).getFirstChild(), 1);
        assertEquals("<ins>y</ins>", Element.as(rightNode).getString().toLowerCase());
        assertEquals("u<del>v<strong><ins><!--x--></ins></strong></del>"
            + "<del><strong><ins>y</ins>z</strong><em>a</em></del>b", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#isFlowContainer(Node)}.
     */
    public void testIsFlowContainer()
    {
        container.setInnerHTML("<ul><li>foo</li></ul>");
        assertTrue(DOMUtils.getInstance().isFlowContainer(container));
        assertFalse(DOMUtils.getInstance().isFlowContainer(container.getFirstChild()));
        assertTrue(DOMUtils.getInstance().isFlowContainer(container.getFirstChild().getFirstChild()));
        assertFalse(DOMUtils.getInstance().isFlowContainer(container.getFirstChild().getFirstChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#getNearestFlowContainer(Node)}.
     */
    public void testGetNearestFlowContainer()
    {
        container.setInnerHTML("x<del>y</del>z");
        assertEquals(container, DOMUtils.getInstance().getNearestFlowContainer(
            container.getChildNodes().getItem(1).getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#insertAt(Node, Node, int)}.
     */
    public void testInsertAt()
    {
        container.xSetInnerHTML("<!--x-->y<em>z</em>");

        Text text = container.getOwnerDocument().createTextNode(":").cast();
        DOMUtils.getInstance().insertAt(container, text, container.getChildNodes().getLength());
        assertEquals("<!--x-->y<em>z</em>:", container.getInnerHTML().toLowerCase());

        text = container.getOwnerDocument().createTextNode("{").cast();
        DOMUtils.getInstance().insertAt(container, text, 0);
        assertEquals("{<!--x-->y<em>z</em>:", container.getInnerHTML().toLowerCase());

        text = container.getOwnerDocument().createTextNode("}").cast();
        DOMUtils.getInstance().insertAt(container, text, 2);
        assertEquals("{<!--x-->}y<em>z</em>:", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getFarthestInlineAncestor(Node)}.
     */
    public void testGetFarthestInlineAncestor()
    {
        container.xSetInnerHTML("#<em>$</em>#");

        assertEquals(container.getChildNodes().getItem(1), DOMUtils.getInstance().getFarthestInlineAncestor(
            container.getChildNodes().getItem(1).getFirstChild()));
        assertEquals(container.getChildNodes().getItem(1), DOMUtils.getInstance().getFarthestInlineAncestor(
            container.getChildNodes().getItem(1)));
        assertNull(DOMUtils.getInstance().getFarthestInlineAncestor(container));
    }

    /**
     * Unit test for {@link DOMUtils#getFirstLeaf(Range)} and {@link DOMUtils#getLastLeaf(Range)}.
     */
    public void testGetRangeFirstAndLastLeaf()
    {
        container.xSetInnerHTML("ab<em>c</em><del><strong>d</strong></del><ins>e</ins>f");
        Range range = ((Document) container.getOwnerDocument()).createRange();

        range.setStart(container.getFirstChild(), 1);
        range.collapse(true);
        assertEquals(container.getFirstChild(), DOMUtils.getInstance().getFirstLeaf(range));
        assertEquals(container.getFirstChild(), DOMUtils.getInstance().getLastLeaf(range));

        range.setEnd(container.getFirstChild(), 2);
        assertEquals(container.getFirstChild(), DOMUtils.getInstance().getFirstLeaf(range));
        assertEquals(container.getFirstChild(), DOMUtils.getInstance().getLastLeaf(range));

        range.setEnd(container.getLastChild(), 1);
        assertEquals(container.getFirstChild(), DOMUtils.getInstance().getFirstLeaf(range));
        assertEquals(container.getLastChild(), DOMUtils.getInstance().getLastLeaf(range));

        range.setStart(container.getChildNodes().getItem(1), 1);
        range.setEnd(container.getChildNodes().getItem(3), 0);
        assertEquals(container.getChildNodes().getItem(2).getFirstChild().getFirstChild(), DOMUtils.getInstance()
            .getFirstLeaf(range));
        assertEquals(container.getChildNodes().getItem(2).getFirstChild().getFirstChild(), DOMUtils.getInstance()
            .getLastLeaf(range));

        range.collapse(true);
        assertNull(DOMUtils.getInstance().getFirstLeaf(range));
        assertNull(DOMUtils.getInstance().getLastLeaf(range));
    }

    /**
     * Unit test for {@link DOMUtils#detach(Node)}.
     */
    public void testDetach()
    {
        container.setInnerHTML("1<span>2</span>3");
        Node node = container.getChildNodes().getItem(1);
        DOMUtils.getInstance().detach(node);
        assertEquals("13", container.getInnerHTML());
        assertNull("IE fails because orphan nodes are attached to a document fragment.", node.getParentNode());
        // The following shoudn't fail.
        DOMUtils.getInstance().detach(node);
    }
}
