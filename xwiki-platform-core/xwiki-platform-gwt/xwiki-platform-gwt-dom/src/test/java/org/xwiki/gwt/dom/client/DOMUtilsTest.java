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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;

/**
 * Unit tests for {@link DOMUtils}.
 * 
 * @version $Id$
 */
public class DOMUtilsTest extends DOMTestCase
{
    /**
     * The collection of DOM utility methods being tested.
     */
    private DOMUtils domUtils;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (domUtils == null) {
            domUtils = DOMUtils.getInstance();
        }
    }

    /**
     * Unit test for {@link DOMUtils#getTextRange(Range)}.
     */
    public void testGetTextRange()
    {
        getContainer().setInnerHTML("x<a href=\"http://www.xwiki.org\"><strong>a</strong>b<em>cd</em></a>y");

        Range range = getDocument().createRange();
        range.setStart(getContainer(), 1);
        range.setEnd(getContainer(), 2);

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
        getContainer().setInnerHTML("a<!--xy-->");

        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), getContainer().getFirstChild().getNodeValue().length());
        range.setEnd(getContainer().getLastChild(), getContainer().getLastChild().getNodeValue().length());

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
        getContainer().setInnerHTML(
            "<div>our<!--startwikilink:Reference--><span class=\"wikilink\">"
                + "<a>x<strong>wiki</strong></a></span><!--stopwikilink-->rox</div>");
        Node wrappingSpan = getContainer().getFirstChild().getChildNodes().getItem(2);
        Node anchor = wrappingSpan.getFirstChild();
        Node boldWiki = anchor.getChildNodes().getItem(1);
        Node labelBoldWiki = boldWiki.getFirstChild();
        String anchorTagName = "a";
        String spanTagName = "span";

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
        assertSame("Div ancestor search for the anchor does not stop at first div", getContainer().getFirstChild(),
            DOMUtils.getInstance().getFirstAncestor(anchor, getContainer().getTagName()));
        // check that the anchor is the first ancestor with tag name a or span of the text inside the strong element
        assertSame("Anchor or span ancestor search does not stop at first anchor", anchor, domUtils.getFirstAncestor(
            labelBoldWiki, anchorTagName, spanTagName));
        // check that the anchor is the first ancestor with tag name a or span of the text inside the strong element,
        // regardless of the order of the arguments
        assertSame("Span or ancestor ancestor search does not stop at first anchor", anchor, domUtils.getFirstAncestor(
            labelBoldWiki, spanTagName, anchorTagName));
        // check that the first ancestor search is case insensitive
        assertSame("Anchor or ul ancestor in upper case search does not stop at first anchor", anchor, domUtils
            .getFirstAncestor(labelBoldWiki, anchorTagName.toUpperCase(), "UL"));
    }

    /**
     * Test the first descendant search function, on a wikilink-like HTML structure.
     * 
     * @see DOMUtils#getFirstDescendant(Node, String)
     */
    public void testGetFirstDescendant()
    {
        getContainer().setInnerHTML(
            "<div>my<!--startwikilink:Reference--><span class=\"wikilink\">"
                + "<a>x<span>wiki</span></a></span><!--stopwikilink-->rules</div>");
        Node wrappingSpan = getContainer().getFirstChild().getChildNodes().getItem(2);
        Node anchor = wrappingSpan.getFirstChild();
        Node preambleText = getContainer().getFirstChild().getFirstChild();
        String anchorTagName = anchor.getNodeName();

        // check anchor shows up as descendant of startContainer
        assertSame("Anchor does not show up as descendant of startContainer", anchor, domUtils.getFirstDescendant(
            getContainer().getFirstChild(), anchorTagName));
        // check anchor shows up as descendant of itself
        assertSame("Anchor does not show up as descendant of itself", anchor, domUtils.getFirstDescendant(anchor,
            anchorTagName));
        // check there is no descendant of type bold in the wrapping span
        assertNull("There is a descendant of type bold in the wrapping span", domUtils.getFirstDescendant(wrappingSpan,
            "strong"));
        // check the first span descendant stops at the wrapping span
        assertSame("The first span descendant does not stop at the wrapping span", wrappingSpan, domUtils
            .getFirstDescendant(getContainer(), wrappingSpan.getNodeName()));
        // check there is no anchor descendant of a text
        assertNull("There is an anchor descendant of a text", domUtils.getFirstDescendant(preambleText, anchorTagName));
    }

    /**
     * Unit test for {@link DOMUtils#getAncestors(Node)}.
     */
    public void testGetAncestors()
    {
        getContainer().setInnerHTML("<em>x</em>");
        List<Node> ancestors = new ArrayList<Node>();
        ancestors.add(getContainer().getFirstChild().getFirstChild());
        ancestors.add(getContainer().getFirstChild());
        ancestors.add(getContainer());
        ancestors.add(getDocument().getBody());
        ancestors.add(getDocument().getDocumentElement());
        ancestors.add(getDocument());
        assertEquals(ancestors, domUtils.getAncestors(ancestors.get(0)));
    }

    /**
     * Unit test for {@link DOMUtils#getNearestCommonAncestor(Node, Node)}.
     */
    public void testGetNearestCommonAncestor()
    {
        getContainer().setInnerHTML("<em>x</em>y<del>z</del>");
        assertEquals(getContainer(), domUtils.getNearestCommonAncestor(getContainer().getFirstChild().getFirstChild(),
            getContainer().getLastChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#cloneNodeContents(Node, int, int)}.
     */
    public void testCloneNodeContents()
    {
        getContainer().setInnerHTML("xwiki<span><em>$</em><ins>#</ins></span>");

        DocumentFragment contents = domUtils.cloneNodeContents(getContainer().getFirstChild(), 0, 2);
        assertEquals(1, contents.getChildNodes().getLength());
        assertEquals("xw", contents.getInnerHTML());

        contents = domUtils.cloneNodeContents(getContainer().getFirstChild(), 5, 5);
        assertEquals(0, contents.getChildNodes().getLength());

        contents = domUtils.cloneNodeContents(getContainer().getLastChild(), 0, 2);
        assertEquals(2, contents.getChildNodes().getLength());
        assertEquals("<em>$</em><ins>#</ins>", contents.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, int, int)}.
     */
    public void testCloneNodeBetweenOffsets()
    {
        getContainer().setInnerHTML("toucan<span><em>+</em><ins>-</ins></span>");
        assertEquals("an", domUtils.cloneNode(getContainer().getFirstChild(), 4, 6).getNodeValue());
        assertEquals("", domUtils.cloneNode(getContainer().getFirstChild(), 0, 0).getNodeValue());

        Element clone = domUtils.cloneNode(getContainer().getLastChild(), 1, 2).cast();
        assertEquals("<ins>-</ins>", clone.getInnerHTML().toLowerCase());

        clone = domUtils.cloneNode(getContainer().getLastChild(), 0, 0).cast();
        assertEquals("<span></span>", clone.getString().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getLength(Node)}.
     */
    public void testGetLength()
    {
        getContainer().setInnerHTML("xwiki<strong></strong><ins>x<del>y</del>z</ins>");
        assertEquals(5, domUtils.getLength(getContainer().getFirstChild()));
        assertEquals(0, domUtils.getLength(getContainer().getChildNodes().getItem(1)));
        assertEquals(3, domUtils.getLength(getContainer().getLastChild()));
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, int, boolean)}.
     */
    public void testCloneNodeLeftRight()
    {
        getContainer().setInnerHTML("abc");
        assertEquals("ab", domUtils.cloneNode(getContainer().getFirstChild(), 2, true).getNodeValue());
        assertEquals("c", domUtils.cloneNode(getContainer().getFirstChild(), 2, false).getNodeValue());

        getContainer().setInnerHTML("a<!--x--><em>b</em>");

        assertEquals("a<!--x-->", ((Element) domUtils.cloneNode(getContainer(), 2, true)).getInnerHTML().toLowerCase());
        assertEquals("<em>b</em>", ((Element) domUtils.cloneNode(getContainer(), 2, false)).getInnerHTML()
            .toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#cloneNode(Node, Node, int, boolean)}.
     */
    public void testCloneNodeUpwards()
    {
        getContainer().setInnerHTML("<ins><del>abc<em>d</em></del></ins>e");

        Element clone =
            domUtils.cloneNode(getContainer().getParentNode(),
                getContainer().getFirstChild().getFirstChild().getFirstChild(), 2, false).cast();
        assertEquals("<ins><del>c<em>d</em></del></ins>e", clone.getInnerHTML().toLowerCase());

        clone =
            domUtils.cloneNode(getContainer().getParentNode(), getContainer().getFirstChild().getFirstChild(), 1, true)
                .cast();
        assertEquals("<ins><del>abc</del></ins>", clone.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getChild(Node, Node)}.
     */
    public void testGetChild()
    {
        getContainer().setInnerHTML("%<strong>@<em>^</em></strong>");
        assertEquals(getContainer().getFirstChild(), domUtils.getChild(getContainer(), getContainer().getFirstChild()));
        assertEquals(getContainer().getLastChild(), domUtils.getChild(getContainer(), getContainer().getLastChild()
            .getLastChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, int, int)}.
     */
    public void testDeleteNodeContentsBetweenOffsets()
    {
        getContainer().setInnerHTML("foo");
        domUtils.deleteNodeContents(getContainer().getFirstChild(), 1, 2);
        assertEquals("fo", getContainer().getInnerHTML());

        getContainer().setInnerHTML("<em>1</em>2<!--3-->");
        domUtils.deleteNodeContents(getContainer(), 1, 2);
        assertEquals("<em>1</em><!--3-->", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, int, boolean)}.
     */
    public void testDeleteNodeContentsLeftRight()
    {
        getContainer().setInnerHTML("foo<del></del>bar");
        domUtils.deleteNodeContents(getContainer().getFirstChild(), 2, true);
        assertEquals("o<del></del>bar", getContainer().getInnerHTML().toLowerCase());
        domUtils.deleteNodeContents(getContainer().getLastChild(), 0, false);
        assertEquals("o<del></del>", getContainer().getInnerHTML().toLowerCase());

        getContainer().setInnerHTML("<ins>1</ins><!--2-->3");
        domUtils.deleteNodeContents(getContainer(), 1, true);
        assertEquals("<!--2-->3", getContainer().getInnerHTML());
        domUtils.deleteNodeContents(getContainer(), 1, false);
        assertEquals("<!--2-->", getContainer().getInnerHTML());
    }

    /**
     * Unit test for {@link DOMUtils#deleteSiblings(Node, boolean)}.
     */
    public void testDeleteSiblings()
    {
        getContainer().setInnerHTML("1<strong>2</strong><!--3-->");
        domUtils.deleteSiblings(getContainer().getFirstChild(), true);
        assertEquals(3, getContainer().getChildNodes().getLength());
        domUtils.deleteSiblings(getContainer().getFirstChild(), false);
        assertEquals("1", getContainer().getInnerHTML());
    }

    /**
     * Unit test for {@link DOMUtils#deleteNodeContents(Node, Node, int, boolean)}.
     */
    public void testDeleteNodeContentsUpwards()
    {
        getContainer().setInnerHTML("<del>x<em>y<!--z--><span>wiki</span></em></del>");
        domUtils.deleteNodeContents(getContainer(), getContainer().getFirstChild().getChildNodes().getItem(1)
            .getChildNodes().getItem(2).getFirstChild(), 2, true);
        assertEquals("<del><em><span>ki</span></em></del>", getContainer().getInnerHTML().toLowerCase());

        getContainer().setInnerHTML("<del><em><span>wiki</span><!--z-->y</em>x</del>");
        domUtils.deleteNodeContents(getContainer(), getContainer().getFirstChild().getFirstChild().getFirstChild()
            .getFirstChild(), 1, false);
        assertEquals("<del><em><span>w</span></em></del>", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#splitNode(Node, int)}.
     */
    public void testSplitNodeAtOffset()
    {
        getContainer().setInnerHTML("xwiki");
        Node rightNode = domUtils.splitNode(getContainer().getFirstChild(), 3);
        assertEquals(getContainer().getLastChild(), rightNode);
        assertEquals(2, getContainer().getChildNodes().getLength());
        assertEquals("xwi", getContainer().getFirstChild().getNodeValue());
        assertEquals("ki", getContainer().getLastChild().getNodeValue());

        getContainer().setInnerHTML("q<span><!--x--><em>w</em>e</span>rty");
        rightNode = domUtils.splitNode(getContainer().getChildNodes().getItem(1), 0);
        assertEquals("<span><!--x--><em>w</em>e</span>", Element.as(rightNode).getString().toLowerCase());
        assertEquals("q<span></span><span><!--x--><em>w</em>e</span>rty", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#splitNode(Node, Node, int)}.
     */
    public void testSplitNodeUpwards()
    {
        getContainer().setInnerHTML("u<del>v<strong><em><!--x-->y</em>z</strong><ins>a</ins></del>b");
        Node rightNode =
            domUtils.splitNode(getContainer(), getContainer().getChildNodes().getItem(1).getChildNodes().getItem(1)
                .getFirstChild(), 1);
        assertEquals("<em>y</em>", Element.as(rightNode).getString().toLowerCase());
        assertEquals("u<del>v<strong><em><!--x--></em></strong></del>"
            + "<del><strong><em>y</em>z</strong><ins>a</ins></del>b", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#splitNode(Node, Node, int)} at the beginning of a paragraph.
     * 
     * @see XWIKI-3053: When a HR is inserted at the beginning of a paragraph an extra empty paragraph is generated
     *      before that HR
     */
    public void testSplitNodeUpwardsAtTheBeginningOfAParagraph()
    {
        getContainer().setInnerHTML("<p>test</p>");
        Node rightNode = domUtils.splitNode(getContainer(), getContainer().getFirstChild().getFirstChild(), 0);
        assertEquals("test", rightNode.getNodeValue());
        // I think it's normal to have the empty paragraph. We just have to make sure it is editable.
        assertEquals("<p></p><p>test</p>", getContainer().getInnerHTML().toLowerCase().replaceAll("[\r\n\t]+", ""));
        assertEquals(1, getContainer().getFirstChild().getChildNodes().getLength());
    }

    /**
     * Unit test for {@link DOMUtils#isFlowContainer(Node)}.
     */
    public void testIsFlowContainer()
    {
        getContainer().setInnerHTML("<ul><li>foo</li></ul>");
        assertTrue(domUtils.isFlowContainer(getContainer()));
        assertFalse(domUtils.isFlowContainer(getContainer().getFirstChild()));
        assertTrue(domUtils.isFlowContainer(getContainer().getFirstChild().getFirstChild()));
        assertFalse(domUtils.isFlowContainer(getContainer().getFirstChild().getFirstChild().getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#getNearestFlowContainer(Node)}.
     */
    public void testGetNearestFlowContainer()
    {
        getContainer().setInnerHTML("x<del>y</del>z");
        assertEquals(getContainer(), domUtils.getNearestFlowContainer(getContainer().getChildNodes().getItem(1)
            .getFirstChild()));
    }

    /**
     * Unit test for {@link DOMUtils#insertAt(Node, Node, int)}.
     */
    public void testInsertAt()
    {
        getContainer().xSetInnerHTML("<!--x-->y<em>z</em>");

        Text text = getDocument().createTextNode(":").cast();
        domUtils.insertAt(getContainer(), text, getContainer().getChildNodes().getLength());
        assertEquals("<!--x-->y<em>z</em>:", getContainer().getInnerHTML().toLowerCase());

        text = getDocument().createTextNode("{").cast();
        domUtils.insertAt(getContainer(), text, 0);
        assertEquals("{<!--x-->y<em>z</em>:", getContainer().getInnerHTML().toLowerCase());

        text = getDocument().createTextNode("}").cast();
        domUtils.insertAt(getContainer(), text, 2);
        assertEquals("{<!--x-->}y<em>z</em>:", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getFarthestInlineAncestor(Node)}.
     */
    public void testGetFarthestInlineAncestor()
    {
        getContainer().xSetInnerHTML("#<em>$</em>#");

        assertEquals(getContainer().getChildNodes().getItem(1), domUtils.getFarthestInlineAncestor(getContainer()
            .getChildNodes().getItem(1).getFirstChild()));
        assertEquals(getContainer().getChildNodes().getItem(1), domUtils.getFarthestInlineAncestor(getContainer()
            .getChildNodes().getItem(1)));
        assertNull(domUtils.getFarthestInlineAncestor(getContainer()));
    }

    /**
     * Unit test for {@link DOMUtils#getFirstLeaf(Range)} and {@link DOMUtils#getLastLeaf(Range)}.
     */
    public void testGetRangeFirstAndLastLeaf()
    {
        getContainer().xSetInnerHTML("ab<em>c</em><del><strong>d</strong></del><ins>e</ins>f");
        Range range = getDocument().createRange();

        range.setStart(getContainer().getFirstChild(), 1);
        range.collapse(true);
        assertEquals(getContainer().getFirstChild(), domUtils.getFirstLeaf(range));
        assertEquals(getContainer().getFirstChild(), domUtils.getLastLeaf(range));

        range.setEnd(getContainer().getFirstChild(), 2);
        assertEquals(getContainer().getFirstChild(), domUtils.getFirstLeaf(range));
        assertEquals(getContainer().getFirstChild(), domUtils.getLastLeaf(range));

        range.setEnd(getContainer().getLastChild(), 1);
        assertEquals(getContainer().getFirstChild(), domUtils.getFirstLeaf(range));
        assertEquals(getContainer().getLastChild(), domUtils.getLastLeaf(range));

        range.setStart(getContainer().getChildNodes().getItem(1), 1);
        range.setEnd(getContainer().getChildNodes().getItem(3), 0);
        Node lastLeaf = domUtils.getLastLeaf(range);
        assertEquals(lastLeaf, domUtils.getFirstLeaf(range));
        assertEquals(getContainer().getChildNodes().getItem(2).getFirstChild().getFirstChild(), lastLeaf);

        range.collapse(true);
        assertNull(domUtils.getFirstLeaf(range));
        assertNull(domUtils.getLastLeaf(range));
    }

    /**
     * Unit test for {@link DOMUtils#detach(Node)}.
     */
    public void testDetach()
    {
        getContainer().setInnerHTML("1<span>2</span>3");
        Node node = getContainer().getChildNodes().getItem(1);
        domUtils.detach(node);
        assertEquals("13", getContainer().getInnerHTML());
        // IE fails because orphan nodes that have been created with the innerHTML property are attached to a document
        // fragment.
        assertNull("Detached node still has a parent.", node.getParentNode());
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
        getContainer().appendChild(getDocument().createTextNode("void"));
        assertEquals(0, domUtils.getNormalizedNodeIndex(getContainer().getFirstChild()));

        getContainer().getFirstChild().setNodeValue("");
        assertEquals(0, domUtils.getNormalizedNodeIndex(getContainer().getFirstChild()));

        getContainer().appendChild(getDocument().createSpanElement());
        assertEquals(0, domUtils.getNormalizedNodeIndex(getContainer().getLastChild()));

        getContainer().getFirstChild().setNodeValue("null");
        assertEquals(1, domUtils.getNormalizedNodeIndex(getContainer().getLastChild()));

        getContainer().appendChild(getDocument().createTextNode("int"));
        assertEquals(2, domUtils.getNormalizedNodeIndex(getContainer().getLastChild()));

        Element.as(getContainer().getChildNodes().getItem(1)).setAttribute(Element.META_DATA_ATTR, "");
        assertEquals(0, domUtils.getNormalizedNodeIndex(getContainer().getLastChild()));
    }

    /**
     * Unit test for {@link DOMUtils#getNormalizedChildCount(Node)}.
     */
    public void testGetNormalizedChildCount()
    {
        assertEquals(0, domUtils.getNormalizedChildCount(getContainer()));

        getContainer().appendChild(getDocument().createTextNode(""));
        assertEquals(0, domUtils.getNormalizedChildCount(getContainer()));

        getContainer().getFirstChild().setNodeValue("double");
        assertEquals(1, domUtils.getNormalizedChildCount(getContainer()));

        getContainer().appendChild(getDocument().createSpanElement());
        assertEquals(2, domUtils.getNormalizedChildCount(getContainer()));

        Element.as(getContainer().getLastChild()).setAttribute(Element.META_DATA_ATTR, "");
        assertEquals(1, domUtils.getNormalizedChildCount(getContainer()));

        getContainer().getFirstChild().setNodeValue("");
        assertEquals(0, domUtils.getNormalizedChildCount(getContainer()));
    }

    /**
     * Unit test for {@link DOMUtils#getNextLeaf(Range)} for the cases when there is an empty element and it is either
     * the next leaf or the selection is placed inside.
     */
    public void testGetNextLeafRangeAroundEmptyElement()
    {
        getContainer().setInnerHTML("our<span>xwiki<strong></strong></span><br />");
        SpanElement wrappingSpan = (SpanElement) getContainer().getChildNodes().getItem(1);

        Range range = null;
        Node textElement = wrappingSpan.getFirstChild();
        Element strongEmptyElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Element brElement = (Element) wrappingSpan.getNextSibling();

        // test that the empty strong element is found as next leaf of a selection xw|ik|i
        range = getDocument().createRange();
        range.setStart(textElement, 2);
        range.setEnd(textElement, 4);
        assertEquals(strongEmptyElement, domUtils.getNextLeaf(range));

        // test that the empty strong element is found as next leaf of a selection placed in the wrapping span at
        // position 1
        range = getDocument().createRange();
        range.setStart(wrappingSpan, 1);
        range.setEnd(wrappingSpan, 1);
        assertEquals(strongEmptyElement, domUtils.getNextLeaf(range));

        // test that the br element is found as next leaf of a selection placed at the end of the wrapping span
        range = getDocument().createRange();
        range.setStart(wrappingSpan, 2);
        range.setEnd(wrappingSpan, 2);
        assertEquals(brElement, domUtils.getNextLeaf(range));

        // test that the br element is found as next leaf of a selection that ends inside the strong empty element
        range = getDocument().createRange();
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
        getContainer().setInnerHTML("our<span>xwiki<strong>a</strong></span><br />");
        SpanElement wrappingSpan = (SpanElement) getContainer().getChildNodes().getItem(1);
        Element strongElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Node insideTextNode = strongElement.getFirstChild();
        Element brElement = (Element) wrappingSpan.getNextSibling();

        Range range = null;

        // test the br element is found as next leaf of a selection placed on the text inside the strong element
        range = getDocument().createRange();
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
        getContainer().setInnerHTML("our<span>xwiki<strong>r</strong>ox</span>");
        SpanElement wrappingSpan = (SpanElement) getContainer().getChildNodes().getItem(1);
        Element strongElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Node insideTextNode = strongElement.getFirstChild();
        Node oxText = wrappingSpan.getChildNodes().getItem(2);
        Node ourText = getContainer().getFirstChild();
        Node xwikiText = wrappingSpan.getFirstChild();

        Range range = null;

        // test the "ox" text is found as next leaf of a selection placed on the text inside the strong element
        range = getDocument().createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(insideTextNode, 1);
        assertEquals(oxText, domUtils.getNextLeaf(range));

        // test that "ox" text is found as next leaf of a non-collapsed selection starting in the our text and ending
        // inside the strong element
        range = getDocument().createRange();
        range.setStart(ourText, 1);
        range.setEnd(insideTextNode, 1);
        assertEquals(oxText, domUtils.getNextLeaf(range));

        // test that the xwiki text is found as next leaf of a selection placed in the "our" text
        range = getDocument().createRange();
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
        getContainer().setInnerHTML("<br /><span><strong></strong>our</span>xwiki");
        SpanElement wrappingSpan = (SpanElement) getContainer().getChildNodes().getItem(1);

        Range range = null;
        Node ourText = wrappingSpan.getChildNodes().getItem(1);
        Element strongEmptyElement = (Element) wrappingSpan.getFirstChild();
        Element brElement = (Element) wrappingSpan.getPreviousSibling();

        // test that the empty strong element is found as previous leaf of a selection o|ur|
        range = getDocument().createRange();
        range.setStart(ourText, 1);
        range.setEnd(ourText, 3);
        assertEquals(strongEmptyElement, domUtils.getPreviousLeaf(range));

        // test that the empty strong element is found as previous leaf of a selection placed in the wrapping span at
        // position 1
        range = getDocument().createRange();
        range.setStart(wrappingSpan, 1);
        range.setEnd(wrappingSpan, 1);
        assertEquals(strongEmptyElement, domUtils.getPreviousLeaf(range));

        // test that the br element is found as previous leaf of a selection placed at the beginning of the wrapping
        // span
        range = getDocument().createRange();
        range.setStart(wrappingSpan, 0);
        range.setEnd(wrappingSpan, 0);
        assertEquals(brElement, domUtils.getPreviousLeaf(range));

        // test that the br element is found as previous leaf of a selection that begins inside the strong empty element
        range = getDocument().createRange();
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
        getContainer().setInnerHTML("<br /><span><strong>a</strong>our</span>xwiki");
        SpanElement wrappingSpan = (SpanElement) getContainer().getChildNodes().getItem(1);
        Element strongElement = (Element) wrappingSpan.getFirstChild();
        Node insideTextNode = strongElement.getFirstChild();
        Element brElement = (Element) wrappingSpan.getPreviousSibling();

        Range range = null;

        // test the br element is found as next leaf of a selection placed on the text inside the strong element
        range = getDocument().createRange();
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
        getContainer().setInnerHTML("<span>our<strong>x</strong>wiki</span>rox");
        SpanElement wrappingSpan = (SpanElement) getContainer().getChildNodes().getItem(0);
        Element strongElement = (Element) wrappingSpan.getChildNodes().getItem(1);
        Node insideTextNode = strongElement.getFirstChild();
        Node roxText = wrappingSpan.getNextSibling();
        Node ourText = wrappingSpan.getFirstChild();
        Node wikiText = strongElement.getNextSibling();

        Range range = null;

        // test the "our" text is found as previous leaf of a selection placed on the text inside the strong element
        range = getDocument().createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(insideTextNode, 1);
        assertEquals(ourText, domUtils.getPreviousLeaf(range));

        // test that "our" text is found as next leaf of a non-collapsed selection starting inside the strong element
        range = getDocument().createRange();
        range.setStart(insideTextNode, 0);
        range.setEnd(wikiText, 2);
        assertEquals(ourText, domUtils.getPreviousLeaf(range));

        // test that the wiki text is found as previous leaf of a selection placed "rox" text
        range = getDocument().createRange();
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
        getContainer().setInnerHTML("<p><em>a</em>b</p>");
        domUtils.splitHTMLNode(getContainer(), getContainer().getFirstChild().getFirstChild().getFirstChild(), 0);
        assertEquals("<p><em></em><br></p><p><em>a</em>b</p>", normalizeHTML(getContainer().getInnerHTML()));

        getContainer().setInnerHTML("<p><em>b</em>a</p>");
        domUtils.splitHTMLNode(getContainer(), getContainer().getFirstChild().getFirstChild().getFirstChild(), 1);
        assertEquals("<p><em>b</em></p><p><em></em>a</p>", getContainer().getInnerHTML().toLowerCase());

        getContainer().setInnerHTML("<p><em>x</em>y</p>");
        domUtils.splitHTMLNode(getContainer(), getContainer().getFirstChild().getLastChild(), 1);
        assertEquals("<p><em>x</em>y</p><p><br></p>", normalizeHTML(getContainer().getInnerHTML()));

        getContainer().setInnerHTML("<p><em>y</em>x</p>");
        domUtils.splitHTMLNode(getContainer(), getContainer().getFirstChild().getLastChild(), 0);
        assertEquals("<p><em>y</em></p><p>x</p>", getContainer().getInnerHTML().toLowerCase());

        getContainer().setInnerHTML("<p><em>1</em>2</p>");
        domUtils.splitHTMLNode(getContainer().getFirstChild(), getContainer().getFirstChild().getFirstChild(), 0);
        assertEquals("<p><em></em><em>1</em>2</p>", getContainer().getInnerHTML().toLowerCase());

        getContainer().setInnerHTML("<p><em>2</em>1</p>");
        domUtils.splitHTMLNode(getContainer().getFirstChild(), getContainer().getFirstChild().getFirstChild(), 1);
        assertEquals("<p><em>2</em><em></em>1</p>", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#getNextNode(Range)}.
     */
    public void testGetRangeNextNode()
    {
        Range range = getDocument().createRange();

        getContainer().setInnerHTML("<del>deleted</del>");
        range.setStart(getContainer(), 0);
        range.collapse(true);
        assertEquals(getContainer().getFirstChild(), domUtils.getNextNode(range));

        range.selectNode(getContainer());
        getContainer().setInnerHTML("<ins><em></em></ins><del>deleted</del>");
        range.selectNodeContents(getContainer().getFirstChild().getFirstChild());
        assertEquals(getContainer().getLastChild(), domUtils.getNextNode(range));

        range.selectNode(getContainer());
        getContainer().setInnerHTML("<strong>x</strong>y");
        range.setStart(getContainer().getFirstChild(), 1);
        range.collapse(true);
        assertEquals(getContainer().getLastChild(), domUtils.getNextNode(range));

        Element element = (Element) getContainer().cloneNode(false);
        element.setInnerHTML(":)");
        range.setStart(element.getFirstChild(), 1);
        range.collapse(true);
        assertNull(domUtils.getNextNode(range));
    }

    /**
     * Unit test for {@link DOMUtils#getPreviousNode(Range)}.
     */
    public void testGetRangePreviousNode()
    {
        Range range = getDocument().createRange();

        getContainer().setInnerHTML("<ins>inserted</ins>");
        range.setStart(getContainer(), 1);
        range.collapse(true);
        assertEquals(getContainer().getLastChild(), domUtils.getPreviousNode(range));

        range.selectNode(getContainer());
        getContainer().setInnerHTML("<del>deleted</del><ins><em></em></ins>");
        range.selectNodeContents(getContainer().getLastChild().getFirstChild());
        assertEquals(getContainer().getFirstChild(), domUtils.getPreviousNode(range));

        range.selectNode(getContainer());
        getContainer().setInnerHTML("x<strong>y</strong>");
        range.setStart(getContainer().getLastChild(), 0);
        range.collapse(true);
        assertEquals(getContainer().getFirstChild(), domUtils.getPreviousNode(range));

        Element element = (Element) getContainer().cloneNode(false);
        element.setInnerHTML(":(");
        range.setStart(element.getFirstChild(), 1);
        range.collapse(true);
        assertNull(domUtils.getPreviousNode(range));
    }

    /**
     * Unit test for {@link DOMUtils#isOrContainsLineBreak(Node)}.
     */
    public void testIsOrContainsLineBreak()
    {
        getContainer().setInnerHTML("a<strong></strong><del>x</del><br/><span><br/></span><ins><em><br/></em></ins>");
        assertFalse(domUtils.isOrContainsLineBreak(null));
        assertFalse(domUtils.isOrContainsLineBreak(getContainer().getChildNodes().getItem(0)));
        assertFalse(domUtils.isOrContainsLineBreak(getContainer().getChildNodes().getItem(1)));
        assertFalse(domUtils.isOrContainsLineBreak(getContainer().getChildNodes().getItem(2)));
        assertTrue(domUtils.isOrContainsLineBreak(getContainer().getChildNodes().getItem(3)));
        assertTrue(domUtils.isOrContainsLineBreak(getContainer().getChildNodes().getItem(4)));
        assertTrue(domUtils.isOrContainsLineBreak(getContainer().getChildNodes().getItem(5)));
    }

    /**
     * Unit test for {@link DOMUtils#canHaveChildren(Node)}.
     */
    public void testCanHaveChildren()
    {
        assertFalse(domUtils.canHaveChildren(null));
        assertFalse(domUtils.canHaveChildren(getDocument()));
        assertFalse(domUtils.canHaveChildren(getDocument().createTextNode("text")));
        assertFalse(domUtils.canHaveChildren(getDocument().createComment("comment")));
        assertFalse(domUtils.canHaveChildren(getDocument().createBRElement()));
        assertFalse(domUtils.canHaveChildren(getDocument().createHRElement()));
        assertTrue(domUtils.canHaveChildren(getDocument().createSpanElement()));
        assertTrue(domUtils.canHaveChildren(getDocument().getBody()));
    }

    /**
     * Unit test for {@link DOMUtils#ensureBlockIsEditable(Element)} when the parameter is a horizontal ruler.
     */
    public void testEnsureHRIsEditable()
    {
        domUtils.ensureBlockIsEditable(Element.as(getDocument().createHRElement()));
        // We should get here without any exception thrown and without any effect.
    }

    /**
     * Unit test for {@link DOMUtils#isolate(Node)}.
     */
    public void testIsolate()
    {
        getContainer().setInnerHTML("<span id=\"foo\" class=\"bar\"><em>one</em><strong>two</strong>three</span>");
        Node parent = getContainer().getFirstChild();
        domUtils.isolate(parent.getChild(1));
        // The isolated node must keep the original parent.
        assertEquals(parent, getContainer().getChild(1));
        assertEquals("<span class=\"bar\"><em>one</em></span>"
            + "<span id=\"foo\" class=\"bar\"><strong>two</strong></span>" + "<span class=\"bar\">three</span>",
            getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link DOMUtils#shrinkRange(Range)} when the returned range is the same.
     */
    public void testShrinkRangeSameResult()
    {
        getContainer().setInnerHTML("four");

        // four|
        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 4);
        range.collapse(true);
        Range result = domUtils.shrinkRange(range);
        assertSame(range, result);

        // f|ou|r
        range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 1);
        range.setEnd(getContainer().getFirstChild(), 3);
        result = domUtils.shrinkRange(range);
        assertSame(range, result);

        getContainer().setInnerHTML("before<em>center</em>after");

        // be|fore<em>center</em>afte|r
        range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 2);
        range.setEnd(getContainer().getLastChild(), 4);
        result = domUtils.shrinkRange(range);
        assertSame(range, result);

        // before<em>center|</em>|after
        range = getDocument().createRange();
        range.setStart(getContainer().getChild(1), 1);
        range.setEnd(getContainer(), 2);
        result = domUtils.shrinkRange(range);
        assertSame(range, result);
    }

    /**
     * Unit test for {@link DOMUtils#shrinkRange(Range)} when the returned range is different.
     */
    public void testShrinkRangeDifferentResult()
    {
        getContainer().setInnerHTML("one<em>two</em>three");
        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 3);
        range.setEnd(getContainer(), 2);
        Range result = domUtils.shrinkRange(range);
        assertEquals(0, result.getStartOffset());
        assertSame(getContainer().getChild(1).getFirstChild(), result.getStartContainer());
        assertEquals(3, result.getEndOffset());
        assertSame(getContainer().getChild(1).getFirstChild(), result.getEndContainer());

        getContainer().setInnerHTML("before<img/>after");
        getContainer().insertBefore(getDocument().createTextNode(""), getContainer().getLastChild());
        range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 6);
        range.setEnd(getContainer().getLastChild(), 0);
        result = domUtils.shrinkRange(range);
        assertEquals(1, result.getStartOffset());
        assertSame(getContainer(), result.getStartContainer());
        assertEquals(0, result.getEndOffset());
        assertSame(getContainer().getChild(2), result.getEndContainer());
    }
}
