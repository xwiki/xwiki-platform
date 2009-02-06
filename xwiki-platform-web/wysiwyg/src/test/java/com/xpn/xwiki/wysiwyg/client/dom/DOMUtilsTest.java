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
import com.google.gwt.dom.client.SpanElement;
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
     * The collection of DOM utility methods being tested.
     */
    private DOMUtils domUtils;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (domUtils == null) {
            domUtils = DOMUtils.getInstance();
        }

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

        Range textRange = domUtils.getTextRange(range);

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

        Range textRange = domUtils.getTextRange(range);

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
        assertSame("There isn't an anchor ancestor for the bold inside the anchor", anchor, domUtils.getFirstAncestor(
            boldWiki, anchorTagName));
        // check if there is a first ancestor of type a for the text inside bold in the anchor
        assertSame("There isn't an anchor ancestor for the text in the bold inside the anchor", anchor, DOMUtils
            .getInstance().getFirstAncestor(labelBoldWiki, anchorTagName));
        // check there is no a ancestor of the wikilink span
        assertNull("There is an anchor ancestor for the wikilink span", domUtils.getFirstAncestor(wrappingSpan,
            anchorTagName));
        // check a finds itself as ancestor
        assertSame("The anchor is not an anhor ancestor of itself", anchor, domUtils.getFirstAncestor(anchor,
            anchorTagName));
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
        assertSame("Anchor does not show up as descendant of startContainer", anchor, domUtils.getFirstDescendant(
            container.getFirstChild(), anchorTagName));
        // check anchor shows up as descendant of itself
        assertSame("Anchor does not show up as descendant of itself", anchor, domUtils.getFirstDescendant(anchor,
            anchorTagName));
        // check there is no descendant of type bold in the wrapping span
        assertNull("There is a descendant of type bold in the wrapping span", domUtils.getFirstDescendant(wrappingSpan,
            "strong"));
        // check the first span descendant stops at the wrapping span
        assertSame("The first span descendant does not stop at the wrapping span", wrappingSpan, domUtils
            .getFirstDescendant(container, wrappingSpan.getNodeName()));
        // check there is no anchor descendant of a text
        assertNull("There is an anchor descendant of a text", domUtils.getFirstDescendant(preambleText, anchorTagName));
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
        assertEquals(ancestors, domUtils.getAncestors(ancestors.get(0)));
    }

    /**
     * Unit test for {@link DOMUtils#getNearestCommonAncestor(Node, Node)}.
     */
    public void testGetNearestCommonAncestor()
    {
        container.setInnerHTML("<em>x</em>y<del>z</del>");
        assertEquals(container, domUtils.getNearestCommonAncestor(container.getFirstChild().getFirstChild(), container
            .getLastChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#cloneNodeContents(Node, int, int)}.
     */
    public void testCloneNodeContents()
    {
        container.setInnerHTML("xwiki<span><em>$</em><ins>#</ins></span>");

        DocumentFragment contents = domUtils.cloneNodeContents(container.getFirstChild(), 0, 2);
        assertEquals(1, contents.getChildNodes().getLength());
        assertEquals("xw", contents.getInnerHTML());

        contents = domUtils.cloneNodeContents(container.getFirstChild(), 5, 5);
        assertEquals(0, contents.getChildNodes().getLength());

        contents = domUtils.cloneNodeContents(container.getLastChild(), 0, 2);
        assertEquals(2, contents.getChildNodes().getLength());
        assertEquals("<em>$</em><ins>#</ins>", contents.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, int, int)}.
     */
    public void testCloneNodeBetweenOffsets()
    {
        container.setInnerHTML("toucan<span><em>+</em><ins>-</ins></span>");
        assertEquals("an", domUtils.cloneNode(container.getFirstChild(), 4, 6).getNodeValue());
        assertEquals("", domUtils.cloneNode(container.getFirstChild(), 0, 0).getNodeValue());

        Element clone = domUtils.cloneNode(container.getLastChild(), 1, 2).cast();
        assertEquals("<ins>-</ins>", clone.getInnerHTML().toLowerCase());

        clone = domUtils.cloneNode(container.getLastChild(), 0, 0).cast();
        assertEquals("<span></span>", clone.getString().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getLength(Node)}.
     */
    public void testGetLength()
    {
        container.setInnerHTML("xwiki<strong></strong><ins>x<del>y</del>z</ins>");
        assertEquals(5, domUtils.getLength(container.getFirstChild()));
        assertEquals(0, domUtils.getLength(container.getChildNodes().getItem(1)));
        assertEquals(3, domUtils.getLength(container.getLastChild()));
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, int, boolean)}.
     */
    public void testCloneNodeLeftRight()
    {
        container.setInnerHTML("abc");
        assertEquals("ab", domUtils.cloneNode(container.getFirstChild(), 2, true).getNodeValue());
        assertEquals("c", domUtils.cloneNode(container.getFirstChild(), 2, false).getNodeValue());

        container.setInnerHTML("a<!--x--><em>b</em>");

        assertEquals("a<!--x-->", ((Element) domUtils.cloneNode(container, 2, true)).getInnerHTML().toLowerCase());
        assertEquals("<em>b</em>", ((Element) domUtils.cloneNode(container, 2, false)).getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, Node, int, boolean)}.
     */
    public void testCloneNodeUpwards()
    {
        container.setInnerHTML("<em><ins>abc<del>d</del></ins></em>e");

        Element clone =
            domUtils.cloneNode(container.getParentNode(), container.getFirstChild().getFirstChild().getFirstChild(), 2,
                false).cast();
        assertEquals("<em><ins>c<del>d</del></ins></em>e", clone.getInnerHTML().toLowerCase());

        clone =
            domUtils.cloneNode(container.getParentNode(), container.getFirstChild().getFirstChild(), 1, true).cast();
        assertEquals("<em><ins>abc</ins></em>", clone.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getChild(Node, Node)}.
     */
    public void testGetChild()
    {
        container.setInnerHTML("%<strong>@<em>^</em></strong>");
        assertEquals(container.getFirstChild(), domUtils.getChild(container, container.getFirstChild()));
        assertEquals(container.getLastChild(), domUtils.getChild(container, container.getLastChild().getLastChild()
            .getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, int, int)}.
     */
    public void testDeleteNodeContentsBetweenOffsets()
    {
        container.setInnerHTML("foo");
        domUtils.deleteNodeContents(container.getFirstChild(), 1, 2);
        assertEquals("fo", container.getInnerHTML());

        container.setInnerHTML("<em>1</em>2<!--3-->");
        domUtils.deleteNodeContents(container, 1, 2);
        assertEquals("<em>1</em><!--3-->", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, int, boolean)}.
     */
    public void testDeleteNodeContentsLeftRight()
    {
        container.setInnerHTML("foo<del></del>bar");
        domUtils.deleteNodeContents(container.getFirstChild(), 2, true);
        assertEquals("o<del></del>bar", container.getInnerHTML().toLowerCase());
        domUtils.deleteNodeContents(container.getLastChild(), 0, false);
        assertEquals("o<del></del>", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("<ins>1</ins><!--2-->3");
        domUtils.deleteNodeContents(container, 1, true);
        assertEquals("<!--2-->3", container.getInnerHTML());
        domUtils.deleteNodeContents(container, 1, false);
        assertEquals("<!--2-->", container.getInnerHTML());
    }

    /**
     * Unit test for {@link DOMUtils#deleteSiblings(Node, boolean)}.
     */
    public void testDeleteSiblings()
    {
        container.setInnerHTML("1<strong>2</strong><!--3-->");
        domUtils.deleteSiblings(container.getFirstChild(), true);
        assertEquals(3, container.getChildNodes().getLength());
        domUtils.deleteSiblings(container.getFirstChild(), false);
        assertEquals("1", container.getInnerHTML());
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, Node, int, boolean)}.
     */
    public void testDeleteNodeContentsUpwards()
    {
        container.setInnerHTML("<span>x<em>y<!--z--><del>wiki</del></em></span>");
        domUtils.deleteNodeContents(container, container.getFirstChild().getChildNodes().getItem(1).getChildNodes()
            .getItem(2).getFirstChild(), 2, true);
        assertEquals("<span><em><del>ki</del></em></span>", container.getInnerHTML().toLowerCase());

        container.setInnerHTML("<span><em><del>wiki</del><!--z-->y</em>x</span>");
        domUtils.deleteNodeContents(container, container.getFirstChild().getFirstChild().getFirstChild()
            .getFirstChild(), 1, false);
        assertEquals("<span><em><del>w</del></em></span>", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#splitNode(Node, int)}.
     */
    public void testSplitNodeAtOffset()
    {
        container.setInnerHTML("xwiki");
        Node rightNode = domUtils.splitNode(container.getFirstChild(), 3);
        assertEquals(container.getLastChild(), rightNode);
        assertEquals(2, container.getChildNodes().getLength());
        assertEquals("xwi", container.getFirstChild().getNodeValue());
        assertEquals("ki", container.getLastChild().getNodeValue());

        container.setInnerHTML("q<span><!--x--><em>w</em>e</span>rty");
        rightNode = domUtils.splitNode(container.getChildNodes().getItem(1), 0);
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
            domUtils.splitNode(container, container.getChildNodes().getItem(1).getChildNodes().getItem(1)
                .getFirstChild(), 1);
        assertEquals("<ins>y</ins>", Element.as(rightNode).getString().toLowerCase());
        assertEquals("u<del>v<strong><ins><!--x--></ins></strong></del>"
            + "<del><strong><ins>y</ins>z</strong><em>a</em></del>b", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#splitNode(Node, Node, int)} at the beginning of a paragraph.
     * 
     * @see XWIKI-3053: When a HR is inserted at the beginning of a paragraph an extra empty paragraph is generated
     *      before that HR
     */
    public void testSplitNodeUpwardsAtTheBeginningOfAParagraph()
    {
        container.setInnerHTML("<p>test</p>");
        Node rightNode = domUtils.splitNode(container, container.getFirstChild().getFirstChild(), 0);
        assertEquals("test", rightNode.getNodeValue());
        // I think it's normal to have the empty paragraph. We just have to make sure it is editable.
        assertEquals("<p></p><p>test</p>", container.getInnerHTML().toLowerCase().replaceAll("[\r\n\t]+", ""));
        assertEquals(1, container.getFirstChild().getChildNodes().getLength());
    }

    /**
     * Unit test for {@link DOMUtils#isFlowContainer(Node)}.
     */
    public void testIsFlowContainer()
    {
        container.setInnerHTML("<ul><li>foo</li></ul>");
        assertTrue(domUtils.isFlowContainer(container));
        assertFalse(domUtils.isFlowContainer(container.getFirstChild()));
        assertTrue(domUtils.isFlowContainer(container.getFirstChild().getFirstChild()));
        assertFalse(domUtils.isFlowContainer(container.getFirstChild().getFirstChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#getNearestFlowContainer(Node)}.
     */
    public void testGetNearestFlowContainer()
    {
        container.setInnerHTML("x<del>y</del>z");
        assertEquals(container, domUtils.getNearestFlowContainer(container.getChildNodes().getItem(1).getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#insertAt(Node, Node, int)}.
     */
    public void testInsertAt()
    {
        container.xSetInnerHTML("<!--x-->y<em>z</em>");

        Text text = container.getOwnerDocument().createTextNode(":").cast();
        domUtils.insertAt(container, text, container.getChildNodes().getLength());
        assertEquals("<!--x-->y<em>z</em>:", container.getInnerHTML().toLowerCase());

        text = container.getOwnerDocument().createTextNode("{").cast();
        domUtils.insertAt(container, text, 0);
        assertEquals("{<!--x-->y<em>z</em>:", container.getInnerHTML().toLowerCase());

        text = container.getOwnerDocument().createTextNode("}").cast();
        domUtils.insertAt(container, text, 2);
        assertEquals("{<!--x-->}y<em>z</em>:", container.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getFarthestInlineAncestor(Node)}.
     */
    public void testGetFarthestInlineAncestor()
    {
        container.xSetInnerHTML("#<em>$</em>#");

        assertEquals(container.getChildNodes().getItem(1), domUtils.getFarthestInlineAncestor(container.getChildNodes()
            .getItem(1).getFirstChild()));
        assertEquals(container.getChildNodes().getItem(1), domUtils.getFarthestInlineAncestor(container.getChildNodes()
            .getItem(1)));
        assertNull(domUtils.getFarthestInlineAncestor(container));
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
        assertEquals(container.getFirstChild(), domUtils.getFirstLeaf(range));
        assertEquals(container.getFirstChild(), domUtils.getLastLeaf(range));

        range.setEnd(container.getFirstChild(), 2);
        assertEquals(container.getFirstChild(), domUtils.getFirstLeaf(range));
        assertEquals(container.getFirstChild(), domUtils.getLastLeaf(range));

        range.setEnd(container.getLastChild(), 1);
        assertEquals(container.getFirstChild(), domUtils.getFirstLeaf(range));
        assertEquals(container.getLastChild(), domUtils.getLastLeaf(range));

        range.setStart(container.getChildNodes().getItem(1), 1);
        range.setEnd(container.getChildNodes().getItem(3), 0);
        Node lastLeaf = domUtils.getLastLeaf(range);
        assertEquals(lastLeaf, domUtils.getFirstLeaf(range));
        assertEquals(container.getChildNodes().getItem(2).getFirstChild().getFirstChild(), lastLeaf);

        range.collapse(true);
        assertNull(domUtils.getFirstLeaf(range));
        assertNull(domUtils.getLastLeaf(range));
    }

    /**
     * Unit test for {@link DOMUtils#detach(Node)}.
     */
    public void testDetach()
    {
        container.setInnerHTML("1<span>2</span>3");
        Node node = container.getChildNodes().getItem(1);
        domUtils.detach(node);
        assertEquals("13", container.getInnerHTML());
        // IE fails because orphan nodes that have been created with the innerHTML property are attached to a document
        // fragment.
        assertNull(node.getParentNode());
        // The following shoudn't fail.
        domUtils.detach(node);
    }

    /**
     * Unit test for {@link DOMUtils#isSerializable(Node)}.
     */
    public void testIsSerializable()
    {
        Text text = Document.get().createTextNode("1983").cast();
        assertTrue(domUtils.isSerializable(text));

        text.setData("");
        assertFalse(domUtils.isSerializable(text));

        Element element = Document.get().createSpanElement().cast();
        assertTrue(domUtils.isSerializable(element));

        element.setAttribute(Element.META_DATA_ATTR, "");
        assertFalse(domUtils.isSerializable(element));

        Node comment = ((Document) Document.get()).createComment("");
        assertTrue(domUtils.isSerializable(comment));
    }

    /**
     * Unit test for {@link DOMUtils#getNormalizedNodeIndex(Node)}.
     */
    public void testGetNormalizedNodeIndex()
    {
        container.appendChild(Document.get().createTextNode("void"));
        assertEquals(0, domUtils.getNormalizedNodeIndex(container.getFirstChild()));

        container.getFirstChild().setNodeValue("");
        assertEquals(0, domUtils.getNormalizedNodeIndex(container.getFirstChild()));

        container.appendChild(Document.get().createSpanElement());
        assertEquals(0, domUtils.getNormalizedNodeIndex(container.getLastChild()));

        container.getFirstChild().setNodeValue("null");
        assertEquals(1, domUtils.getNormalizedNodeIndex(container.getLastChild()));

        container.appendChild(Document.get().createTextNode("int"));
        assertEquals(2, domUtils.getNormalizedNodeIndex(container.getLastChild()));

        Element.as(container.getChildNodes().getItem(1)).setAttribute(Element.META_DATA_ATTR, "");
        assertEquals(0, domUtils.getNormalizedNodeIndex(container.getLastChild()));
    }

    /**
     * Unit test for {@link DOMUtils#getNormalizedChildCount(Node)}.
     */
    public void testGetNormalizedChildCount()
    {
        assertEquals(0, domUtils.getNormalizedChildCount(container));

        container.appendChild(Document.get().createTextNode(""));
        assertEquals(0, domUtils.getNormalizedChildCount(container));

        container.getFirstChild().setNodeValue("double");
        assertEquals(1, domUtils.getNormalizedChildCount(container));

        container.appendChild(Document.get().createSpanElement());
        assertEquals(2, domUtils.getNormalizedChildCount(container));

        Element.as(container.getLastChild()).setAttribute(Element.META_DATA_ATTR, "");
        assertEquals(1, domUtils.getNormalizedChildCount(container));

        container.getFirstChild().setNodeValue("");
        assertEquals(0, domUtils.getNormalizedChildCount(container));
    }

    /**
     * Unit test for {@link DOMUtils#getNextLeaf(Range)} for the cases when there is an empty element and it is either
     * the next leaf or the selection is placed inside.
     */
    public void testGetNextLeafRangeAroundEmptyElement()
    {
        container.setInnerHTML("our<span>xwiki<strong></strong></span><br />");
        SpanElement wrappingSpan = (SpanElement) container.getChildNodes().getItem(1);

        Range range = null;
        Node textElement = wrappingSpan.getFirstChild();
        Element strongEmptyElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Element brElement = (Element) wrappingSpan.getNextSibling();

        // test that the empty strong element is found as next leaf of a selection xw|ik|i
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(textElement, 2);
        range.setEnd(textElement, 4);
        assertEquals(strongEmptyElement, domUtils.getNextLeaf(range));

        // test that the empty strong element is found as next leaf of a selection placed in the wrapping span at
        // position 1
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(wrappingSpan, 1);
        range.setEnd(wrappingSpan, 1);
        assertEquals(strongEmptyElement, domUtils.getNextLeaf(range));

        // test that the br element is found as next leaf of a selection placed at the end of the wrapping span
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(wrappingSpan, 2);
        range.setEnd(wrappingSpan, 2);
        assertEquals(brElement, domUtils.getNextLeaf(range));

        // test that the br element is found as next leaf of a selection that ends inside the strong empty element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(textElement, 4);
        range.setEnd(strongEmptyElement, 0);
        assertEquals(brElement, domUtils.getNextLeaf(range));
    }

    /**
     * Unit tests for {@link DOMUtils#getNextLeaf(Range)} for the case when there is a wrapping element around the range
     * and its next leaf.
     */
    public void testGetNextLeafRangeWhenSelectionIsInLastChild()
    {
        container.setInnerHTML("our<span>xwiki<strong>a</strong></span><br />");
        SpanElement wrappingSpan = (SpanElement) container.getChildNodes().getItem(1);
        Element strongElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Node insideTextNode = strongElement.getFirstChild();
        Element brElement = (Element) wrappingSpan.getNextSibling();

        Range range = null;

        // test the br element is found as next leaf of a selection placed on the text inside the strong element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(insideTextNode, 1);
        assertEquals(brElement, domUtils.getNextLeaf(range));
    }

    /**
     * Unit tests for {@link DOMUtils#getNextLeaf(Range)} for the case when the next leaf is a text and the range is
     * inside a text.
     */
    public void testGetNextLeafRangeWhenSelectionIsBeforeTextNode()
    {
        container.setInnerHTML("our<span>xwiki<strong>r</strong>ox</span>");
        SpanElement wrappingSpan = (SpanElement) container.getChildNodes().getItem(1);
        Element strongElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Node insideTextNode = strongElement.getFirstChild();
        Node oxText = wrappingSpan.getChildNodes().getItem(2);
        Node ourText = container.getFirstChild();
        Node xwikiText = wrappingSpan.getFirstChild();

        Range range = null;

        // test the "ox" text is found as next leaf of a selection placed on the text inside the strong element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(insideTextNode, 1);
        assertEquals(oxText, domUtils.getNextLeaf(range));

        // test that "ox" text is found as next leaf of a non-collapsed selection starting in the our text and ending
        // inside the strong element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(ourText, 1);
        range.setEnd(insideTextNode, 1);
        assertEquals(oxText, domUtils.getNextLeaf(range));

        // test that the xwiki text is found as next leaf of a selection placed in the "our" text
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(ourText, 1);
        range.setEnd(ourText, 2);
        assertEquals(xwikiText, domUtils.getNextLeaf(range));
    }

    /**
     * Unit test for {@link DOMUtils#getPreviousLeaf(Range) for the cases when there is an empty element and it either
     * is the previous leaf or the range is placed inside.
     */
    public void testGetPreviousLeafRangeAroundEmptyElement()
    {
        container.setInnerHTML("<br /><span><strong></strong>our</span>xwiki");
        SpanElement wrappingSpan = (SpanElement) container.getChildNodes().getItem(1);

        Range range = null;
        Node ourText = wrappingSpan.getChildNodes().getItem(1);
        Element strongEmptyElement = (Element) wrappingSpan.getFirstChild();
        Element brElement = (Element) wrappingSpan.getPreviousSibling();

        // test that the empty strong element is found as previous leaf of a selection o|ur|
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(ourText, 1);
        range.setEnd(ourText, 3);
        assertEquals(strongEmptyElement, domUtils.getPreviousLeaf(range));

        // test that the empty strong element is found as previous leaf of a selection placed in the wrapping span at
        // position 1
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(wrappingSpan, 1);
        range.setEnd(wrappingSpan, 1);
        assertEquals(strongEmptyElement, domUtils.getPreviousLeaf(range));

        // test that the br element is found as previous leaf of a selection placed at the beginning of the wrapping
        // span
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(wrappingSpan, 0);
        range.setEnd(wrappingSpan, 0);
        assertEquals(brElement, domUtils.getPreviousLeaf(range));

        // test that the br element is found as previous leaf of a selection that begins inside the strong empty element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(strongEmptyElement, 0);
        range.setEnd(ourText, 2);
        assertEquals(brElement, domUtils.getPreviousLeaf(range));
    }

    /**
     * Unit tests for {@link DOMUtils#getPreviousLeaf(Range)} for the case when there is a wrapping element around the
     * range and its previous leaf.
     */
    public void testGetPreviousLeafRangeWhenSelectionIsInFirstChild()
    {
        container.setInnerHTML("<br /><span><strong>a</strong>our</span>xwiki");
        SpanElement wrappingSpan = (SpanElement) container.getChildNodes().getItem(1);
        Element strongElement = (Element) wrappingSpan.getFirstChild();
        Node insideTextNode = strongElement.getFirstChild();
        Element brElement = (Element) wrappingSpan.getPreviousSibling();

        Range range = null;

        // test the br element is found as next leaf of a selection placed on the text inside the strong element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(insideTextNode, 1);
        assertEquals(brElement, domUtils.getPreviousLeaf(range));
    }

    /**
     * Unit tests for {@link DOMUtils#getPreviousLeaf(Range)} for the case when the previous leaf is a text and the
     * range is inside a text.
     */
    public void testGetPreviousLeafRangeWhenSelectionIsAfterTextNode()
    {
        container.setInnerHTML("<span>our<strong>x</strong>wiki</span>rox");
        SpanElement wrappingSpan = (SpanElement) container.getChildNodes().getItem(0);
        Element strongElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Node insideTextNode = strongElement.getFirstChild();
        Node roxText = wrappingSpan.getNextSibling();
        Node ourText = wrappingSpan.getFirstChild();
        Node wikiText = strongElement.getNextSibling();

        Range range = null;

        // test the "our" text is found as previous leaf of a selection placed on the text inside the strong element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(insideTextNode, 1);
        assertEquals(ourText, domUtils.getPreviousLeaf(range));

        // test that "our" text is found as next leaf of a non-collapsed selection starting inside the strong element
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(wikiText, 2);
        assertEquals(ourText, domUtils.getPreviousLeaf(range));

        // test that the wiki text is found as previous leaf of a selection placed "rox" text
        range = ((Document) container.getOwnerDocument()).createRange();
        range.setStart(roxText, 1);
        range.setEnd(roxText, 2);
        assertEquals(wikiText, domUtils.getPreviousLeaf(range));
    }

    /**
     * Unit test for {@link DOMUtils#splitHTMLNode(Node, Node, int)}.
     * 
     * @see XWIKI-3053: When a HR is inserted at the beginning of a paragraph an extra empty paragraph is generated
     *      before that HR
     */
    public void testSplitHTMLNode()
    {
        container.setInnerHTML("<p><em>a</em>b</p>");
        domUtils.splitHTMLNode(container, container.getFirstChild().getFirstChild().getFirstChild(), 0);
        assertEquals("<p><em></em><br></p><p><em>a</em>b</p>", container.getInnerHTML());

        container.setInnerHTML("<p><em>b</em>a</p>");
        domUtils.splitHTMLNode(container, container.getFirstChild().getFirstChild().getFirstChild(), 1);
        assertEquals("<p><em>b</em></p><p><em></em>a</p>", container.getInnerHTML());

        container.setInnerHTML("<p><em>x</em>y</p>");
        domUtils.splitHTMLNode(container, container.getFirstChild().getLastChild(), 1);
        assertEquals("<p><em>x</em>y</p><p><br></p>", container.getInnerHTML());

        container.setInnerHTML("<p><em>y</em>x</p>");
        domUtils.splitHTMLNode(container, container.getFirstChild().getLastChild(), 0);
        assertEquals("<p><em>y</em></p><p>x</p>", container.getInnerHTML());

        container.setInnerHTML("<p><em>1</em>2</p>");
        domUtils.splitHTMLNode(container.getFirstChild(), container.getFirstChild().getFirstChild(), 0);
        assertEquals("<p><em></em><em>1</em>2</p>", container.getInnerHTML());

        container.setInnerHTML("<p><em>2</em>1</p>");
        domUtils.splitHTMLNode(container.getFirstChild(), container.getFirstChild().getFirstChild(), 1);
        assertEquals("<p><em>2</em><em></em>1</p>", container.getInnerHTML());
    }
}
