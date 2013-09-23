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
package org.xwiki.gwt.dom.client.internal.ie;

import java.util.Iterator;

import org.xwiki.gwt.dom.client.DOMTestCase;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.RangeCompare;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Node;

/**
 * Unit tests for {@link IERange}. These tests are useful only when running in IE browser because the range
 * implementation has to map the W3C Range API to IE's text range API. We test in fact this mapping. If there's no
 * mapping, like in browsers that support the W3C Range specification, then there's no point in running these tests.
 * <p>
 * We have to use a trick to ensure these tests run well in other browsers because we can't make them run only in IE. We
 * use GWT's deferred binding mechanism to instantiate a test range factory. Unfortunately we can't use deferred binding
 * outside the tests, when creating the test suite for instance.
 * 
 * @version $Id$
 */
public class IEOldRangeTest extends DOMTestCase
{
    /**
     * A range factory to be used in tests. It allows us to use deferred binding and load a IE specific implementation
     * that creates ranges without using our range API allowing us to test the mapping between W3C Range API and IE's
     * text range API.
     * <p>
     * The default implementation is not really useful since it creates ranges using our range API. The IE specific
     * implementation is our focus. The default implementation should just ensure the tests run well in other browsers.
     */
    private static class TestRangeFactory
    {
        /**
         * Used to mark the start or end of a text range.
         */
        protected static final String PIPE = "|";

        /**
         * Creates a new range with the end points specified by {@link #PIPE} occurrences inside the given HTML
         * fragment.
         * 
         * @param container the element whose inner HTML will be set to the given HTML fragment
         * @param html a HTML fragment in which {@link #PIPE} symbols mark the range that needs to be created
         * @return a range that matches the {@link #PIPE} symbols in the given HTML fragment
         */
        public Range createRange(Element container, String html)
        {
            Document document = (Document) container.getOwnerDocument().cast();
            Range range = document.createRange();
            container.xSetInnerHTML(html);
            Iterator<Node> iterator = document.getIterator(container);
            // Look for the start point.
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getNodeType() != Node.TEXT_NODE) {
                    continue;
                }
                int index = node.getNodeValue().indexOf(PIPE);
                if (index < 0) {
                    continue;
                }
                range.setStart(node, index);
                node.setNodeValue(node.getNodeValue().substring(0, index) + node.getNodeValue().substring(index + 1));
                // Maybe the end point is in the same text node.
                index = node.getNodeValue().indexOf(PIPE, index);
                if (index < 0) {
                    break;
                } else {
                    range.setEnd(node, index);
                    node.setNodeValue(node.getNodeValue().substring(0, index)
                        + node.getNodeValue().substring(index + 1));
                    return range;
                }
            }
            // Look for the end point. End container is different than start container.
            while (iterator.hasNext()) {
                Node node = iterator.next();
                if (node.getNodeType() == Node.TEXT_NODE) {
                    int index = node.getNodeValue().indexOf(PIPE);
                    if (index >= 0) {
                        range.setEnd(node, index);
                        node.setNodeValue(node.getNodeValue().substring(0, index)
                            + node.getNodeValue().substring(index + 1));
                        return range;
                    }
                }
            }
            range.collapse(true);
            if (range.getStartContainer().getNodeValue().length() == 0) {
                // The caret is inside an empty text node. Remove the text node.
                range.selectNode(range.getStartContainer());
                range.collapse(true);
                range.getStartContainer().removeChild(
                    range.getStartContainer().getChildNodes().getItem(range.getStartOffset()));
            }
            return range;
        }
    }

    /**
     * Specific implementation of {@link TestRangeFactory} for older IE versions (6, 7 and 8).
     */
    @SuppressWarnings("unused")
    private static class IEOldTestRangeFactory extends TestRangeFactory
    {
        @Override
        public Range createRange(Element container, String html)
        {
            container.xSetInnerHTML(html);

            TextRange textRange = TextRange.newInstance((Document) container.getOwnerDocument().cast());

            TextRange refRange = textRange.duplicate();
            refRange.moveToElementText(container);
            if (refRange.findText(PIPE, 0, 0)) {
                refRange.setText("");
                textRange.setEndPoint(RangeCompare.END_TO_START, refRange);
                refRange.findText(PIPE, html.length() - html.indexOf(PIPE), 0);
                refRange.setText("");
                textRange.setEndPoint(RangeCompare.START_TO_END, refRange);
            } else {
                textRange.moveToElementText(container);
            }

            // We cannot select the textRange because IE doesn't support collapsed selection in view mode (which is
            // somehow normal since you cannot have the caret in view mode). Instead we use a mock native selection
            // which returns our textRange like it has been selected.
            return (new IEOldSelection(MockNativeSelection.newInstance(textRange))).getRangeAt(0);
        }
    }

    /**
     * The range factory used in these tests.
     */
    private TestRangeFactory factory;

    /**
     * Sets the given HTML fragment as the inner HTML of the {@link #container} and creates a new range. The range is
     * specified using | (pipe) symbol. For instance, in the following HTML fragment "&lt;em&gt;fo|o&lt;/em&gt; ba|r"
     * the returned range will contain "&lt;em&gt;o&lt;/em&gt; ba".
     * 
     * @param html the HTML fragment containing the range
     * @return the newly created range
     */
    protected Range getRange(String html)
    {
        if (factory == null) {
            factory = GWT.create(TestRangeFactory.class);
        }
        return factory.createRange(getContainer(), html);
    }

    /**
     * Asserts if the given range is collapsed.
     * 
     * @param range The tested range.
     */
    protected static void assertCollapsed(Range range)
    {
        assertTrue(range.isCollapsed());
        assertEquals("", range.toString());
        assertEquals("", range.toHTML());
        assertEquals(range.getStartContainer(), range.getEndContainer());
        assertEquals(range.getStartContainer(), range.getCommonAncestorContainer());
        assertEquals(range.getStartOffset(), range.getEndOffset());
    }

    /**
     * Asserts that the given node wraps the specified range. A node wraps a range if it is at the same time the common
     * ancestor container, the start container and the end container of that range.
     * 
     * @param wrapper A DOM node.
     * @param range The tested range.
     */
    protected static void assertWrapped(Node wrapper, Range range)
    {
        assertEquals(wrapper, range.getCommonAncestorContainer());
        assertEquals(range.getStartContainer(), range.getCommonAncestorContainer());
        assertEquals(range.getEndContainer(), range.getCommonAncestorContainer());
    }

    /**
     * Caret inside a text node.
     */
    public void testCaretInsideATextNode()
    {
        Range range = getRange("a|b");
        assertCollapsed(range);
        assertEquals(getContainer().getFirstChild(), range.getStartContainer());
        assertEquals(1, range.getStartOffset());
    }

    /**
     * Caret at the end of a text node.
     */
    public void testCaretAtTheEndOfATextNode()
    {
        Range range = getRange("ab|<em>#</em>");
        assertCollapsed(range);
        assertEquals(getContainer().getFirstChild(), range.getStartContainer());
        assertEquals(range.getStartContainer().getNodeValue().length(), range.getStartOffset());
    }

    /**
     * Caret at the beginning of a text node.
     */
    public void testCaretAtTheBeginningOfATextNode()
    {
        Range range = getRange("<em>#</em>|a");
        assertCollapsed(range);
        assertEquals(getContainer().getChildNodes().getItem(1), range.getStartContainer());
        assertEquals(0, range.getStartOffset());
    }

    /**
     * Selection inside a text node.
     */
    public void testSelectionInsideATextNode()
    {
        Range range = getRange("a|b|c");
        assertFalse(range.isCollapsed());
        assertEquals("b", range.toString());
        assertEquals(range.toString(), range.toHTML());
        assertWrapped(getContainer().getFirstChild(), range);
        assertEquals(1, range.getStartOffset());
        assertEquals(2, range.getEndOffset());
    }

    /**
     * Selection at the beginning of a text node.
     */
    public void testSelectionAtTheBeginningOfATextNode()
    {
        Range range = getRange("<em>#</em>|ab|c");
        assertFalse(range.isCollapsed());
        assertEquals("ab", range.toString());
        assertEquals(range.toString(), range.toHTML());
        assertWrapped(getContainer().getChildNodes().getItem(1), range);
        assertEquals(0, range.getStartOffset());
        assertEquals(2, range.getEndOffset());
    }

    /**
     * Selection at the end of a text node.
     */
    public void testSelectionAtTheEndOfATextNode()
    {
        Range range = getRange("ab|c|<em>#</em>");
        assertFalse(range.isCollapsed());
        assertEquals("c", range.toString());
        assertEquals(range.toString(), range.toHTML());
        assertWrapped(getContainer().getFirstChild(), range);
        assertEquals(2, range.getStartOffset());
        assertEquals(range.getEndContainer().getNodeValue().length(), range.getEndOffset());
    }

    /**
     * Caret before a comment node.
     */
    public void testCaretBeforeACommentNode()
    {
        Range range = getRange("a|<!--x--><img/><em>#</em>");
        assertCollapsed(range);
        assertEquals(getContainer().getFirstChild(), range.getStartContainer());
        assertEquals(range.getStartContainer().getNodeValue().length(), range.getStartOffset());
    }

    /**
     * Caret after a comment node.
     */
    public void testCaretAfterACommentNode()
    {
        Range range = getRange("<em>#</em><img/><!--x-->|a");
        assertCollapsed(range);
        assertEquals(getContainer().getChildNodes().getItem(3), range.getStartContainer());
        assertEquals(0, range.getStartOffset());
    }

    /**
     * Caret inside an empty element.
     */
    public void testCaretInsideAnEmptyElement()
    {
        Range range = getRange("a<em>|</em>b");
        assertCollapsed(range);
        assertEquals(getContainer().getChildNodes().getItem(1), range.getStartContainer());
        assertEquals(0, range.getStartOffset());
    }

    /**
     * Caret between elements.
     */
    public void testCaretBetweenElements()
    {
        Range range = getRange("a<em>x</em>|<ins>y</ins>b");
        assertCollapsed(range);
        assertEquals(getContainer(), range.getStartContainer());
        assertEquals(4, getContainer().getChildNodes().getLength());
        assertEquals(2, range.getStartOffset());
    }

    /**
     * Selection wraps the contents of an element.
     */
    public void testSelectionWrapsTheContentsOfAnElement()
    {
        Range range = getRange("a<em>|x|</em>b");
        assertFalse(range.isCollapsed());
        assertEquals("x", range.toString());
        assertEquals(range.toString(), range.toHTML());
        assertWrapped(getContainer().getChildNodes().getItem(1).getFirstChild(), range);
        assertEquals(0, range.getStartOffset());
        assertEquals(range.toString().length(), range.getEndOffset());
    }

    /**
     * Selection wraps an element.
     */
    public void testSelectionWrapsAnElement()
    {
        Range range = getRange("ab|<em>y</em>|c");
        assertFalse(range.isCollapsed());
        assertEquals("y", range.toString());
        assertEquals("<em>y</em>", range.toHTML().toLowerCase());
        assertEquals(getContainer(), range.getCommonAncestorContainer());
        assertEquals(getContainer().getFirstChild(), range.getStartContainer());
        assertEquals(getContainer().getFirstChild().getNodeValue().length(), range.getStartOffset());
        // The end point is found correctly in the first place, but as soon as the text range is created the end point
        // moves after the last selected character.
        // assertEquals(getContainer().getChildNodes().getItem(1).getFirstChild(), range.getEndContainer());
        // assertEquals(((Text) getContainer().getChildNodes().getItem(1).getFirstChild()).getLength(),
        // range.getEndOffset());
        assertEquals(getContainer().getLastChild(), range.getEndContainer());
        assertEquals(0, range.getEndOffset());
    }

    /**
     * Selection wraps an empty element.
     */
    public void testSelectionWrapsAnEmptyElement()
    {
        // It seems IE cannot wrap empty elements with 0-width.
        // We can place the caret inside though, but this is not what we want here.
        // Range range = getRange("ab|<ins></ins>|c");
        Range range = getRange("ab|<ins style=\"width: 30px\"></ins>|c");
        assertFalse(range.isCollapsed());
        assertEquals("", range.toString());
        // Some browsers add ; after the last CSS rule. We need to remove it.
        assertEquals("<ins style=\"width: 30px\"></ins>", range.toHTML().toLowerCase().replace(";", ""));
        assertEquals(getContainer(), range.getCommonAncestorContainer());
        assertEquals(getContainer().getFirstChild(), range.getStartContainer());
        assertEquals(getContainer().getLastChild(), range.getEndContainer());
        // assertEquals(getContainer().getChildNodes().getItem(1), range.getEndContainer());
        assertEquals(getContainer().getFirstChild().getNodeValue().length(), range.getStartOffset());
        assertEquals(0, range.getEndOffset());
    }

    /**
     * Selection end points are in different DOM elements.
     */
    public void testSelectionWithEndPointsInDifferentElements()
    {
        Range range = getRange("<em>a|b</em>cd<ins>ef|g</ins>");
        assertFalse(range.isCollapsed());
        assertEquals("bcdef", range.toString());
        assertEquals("<em>b</em>cd<ins>ef</ins>", range.toHTML().toLowerCase());
        assertEquals(getContainer(), range.getCommonAncestorContainer());
        assertEquals(getContainer().getFirstChild().getFirstChild(), range.getStartContainer());
        assertEquals(getContainer().getLastChild().getFirstChild(), range.getEndContainer());
        assertEquals(1, range.getStartOffset());
        assertEquals(2, range.getEndOffset());
    }

    /**
     * Unit test for {@link Range#cloneContents()} when the range is collapsed.
     */
    public void testCloneContentsOfCollapsedRange()
    {
        Range range = getRange("e|f");
        DocumentFragment contents = range.cloneContents();
        assertFalse(contents.hasChildNodes());
    }

    /**
     * Unit test for {@link Range#cloneContents()} when the range is not empty.
     */
    public void testCloneContentsOfNonEmptyRange()
    {
        Range range = getRange("a|b<!--x--><em>c</em><ins>de|f</ins>");
        DocumentFragment contents = range.cloneContents();
        Element wrapper = getDocument().createDivElement().cast();
        wrapper.appendChild(contents);
        assertEquals("b<!--x--><em>c</em><ins>de</ins>", wrapper.getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Range#collapse(boolean)}.
     */
    public void testCollapse()
    {
        Range range = getRange("<em>#</em>|a<ins>b|<!--x--></ins>");

        Range clone = range.cloneRange();
        clone.collapse(true);
        assertCollapsed(clone);
        assertEquals(range.getStartContainer(), clone.getStartContainer());
        assertEquals(range.getStartOffset(), clone.getStartOffset());

        clone = range.cloneRange();
        clone.collapse(false);
        assertCollapsed(clone);
        assertEquals(range.getEndContainer(), clone.getEndContainer());
        assertEquals(range.getEndOffset(), clone.getEndOffset());
    }

    /**
     * Unit test for {@link Range#deleteContents()}.
     */
    public void testDeleteContents()
    {
        Range range = getRange("<em>a|b</em><!--x-->cd|e");
        range.deleteContents();
        assertCollapsed(range);
        assertEquals("<em>a</em>e", getContainer().getInnerHTML().toLowerCase());
    }

    /**
     * Unit test for {@link Range#selectNodeContents(Node)} when the node parameter is a text node.
     */
    public void testSelectTextNodeContents()
    {
        getContainer().xSetInnerHTML("<em>#</em>d<!--x-->");
        Range range = getDocument().createRange();
        range.selectNodeContents(getContainer().getChildNodes().getItem(1));
        assertEquals("d", range.toHTML());
    }

    /**
     * Unit test for {@link Range#selectNodeContents(Node)} when the node parameter is an element.
     */
    public void testSelectElementContents()
    {
        getContainer().xSetInnerHTML("a<div><!--x-->b<em>#</em></div>c");
        Range range = getDocument().createRange();
        range.selectNodeContents(getContainer().getChildNodes().getItem(1));
        assertEquals(((Element) getContainer().getChildNodes().getItem(1)).xGetInnerHTML(), range.toHTML());
    }

    /**
     * Unit test for {@link Range#selectNodeContents(Node)} when the node parameter is an empty element.
     */
    public void testSelectEmptyElementContents()
    {
        getContainer().xSetInnerHTML("<!--x-->a<em></em>");
        Range range = getDocument().createRange();
        range.selectNodeContents(getContainer().getLastChild());
        assertCollapsed(range);
        assertEquals(getContainer().getChildNodes().getItem(2), range.getStartContainer());
        assertEquals("", range.toHTML());

        getContainer().xSetInnerHTML("<!--x-->a<img/>");
        range.selectNode(getContainer().getLastChild());
        assertFalse(range.isCollapsed());
        assertEquals("<img>", normalizeHTML(range.toHTML()));
        assertEquals(getContainer(), range.getCommonAncestorContainer());
    }

    /**
     * Unit test for {@link Range#setStart(Node, int)} and {@link Range#setEnd(Node, int)}. Both end-points of the range
     * are inside the same text node.
     */
    public void testSetEndPointsInsideSameTextNode()
    {
        getContainer().xSetInnerHTML("a<em>bcd</em>e");
        Range range = getDocument().createRange();
        range.setStart(getContainer().getChildNodes().getItem(1).getFirstChild(), 0);
        range.setEnd(getContainer().getChildNodes().getItem(1).getFirstChild(), 2);
        assertEquals("bc", range.toHTML());
    }

    /**
     * Unit test for {@link Range#setStart(Node, int)} and {@link Range#setEnd(Node, int)} when the end points of the
     * range are inside different DOM nodes.
     */
    public void testSetEndPointsInDifferentNodes()
    {
        getContainer().xSetInnerHTML("ab<em>cde</em>");
        Range range = getDocument().createRange();
        range.setStart(getContainer().getFirstChild(), 1);
        range.setEnd(getContainer().getLastChild().getFirstChild(), 2);
        assertEquals("b<em>cd</em>", range.toHTML().toLowerCase());
    }

    /**
     * Tests if the caret is well detected when placed inside an empty list item that has a nested sub list.
     */
    public void testDetectCaretInsideEmptyListItemWithSublist()
    {
        Range range = getRange("<ul><li>a</li></ul><ul><li>|<br/><ul><li>x</li></ul></li></ul>");
        assertTrue(range.isCollapsed());
        assertEquals(getContainer().getLastChild().getFirstChild(), range.getStartContainer());
        assertEquals(2, getContainer().getLastChild().getFirstChild().getChildNodes().getLength());
        assertEquals(0, range.getStartOffset());

        range = getRange("<ul><li>a</li></ul><ul><li><br/><ul><li>|<br/><ul><li>x</li></ul></li></ul></li></ul>");
        assertTrue(range.isCollapsed());
        Node startContainer = getContainer().getLastChild().getFirstChild().getLastChild().getFirstChild();
        assertEquals(startContainer, range.getStartContainer());
        assertEquals(2, startContainer.getChildNodes().getLength());
        assertEquals(0, range.getStartOffset());
    }

    /**
     * Checks if the caret is correctly detected inside a duplicated text.
     */
    public void testDetectCaretInsideDuplicatedText()
    {
        Range range = getRange("aaa<em>aaa</em>aa|a");
        assertTrue(range.isCollapsed());
        assertEquals(2, range.getStartOffset());
        assertEquals(getContainer().getLastChild(), range.getStartContainer());
    }

    /**
     * Checks if the caret is correctly detected inside relative positioned elements.
     */
    public void testDetectCaretInsideRelativePositionedElements()
    {
        Range range =
            getRange("<h1><span>Title 1</span></h1>"
                + "<h2 style=\"position:relative;\"><span>Title 2<br/>|foo</span></h2>");
        assertTrue(range.isCollapsed());
        assertEquals(0, range.getStartOffset());
        assertEquals(getContainer().getLastChild().getFirstChild().getLastChild(), range.getStartContainer());
    }

    /**
     * Checks if the caret is correctly detected inside text with non-breaking spaces, {@code &nbsp;}.
     */
    public void testDetectCaretInsideTextWithNonBreakingSpaces()
    {
        Range range = getRange("one<em>two</em> th&nbsp;&nbsp;|&nbsp;ree");
        assertTrue(range.isCollapsed());
        assertEquals(5, range.getStartOffset());
        assertEquals(getContainer().getLastChild(), range.getStartContainer());
    }

    /**
     * Checks if the caret is correctly detected after a hidden element.
     */
    public void testDetectCaretAfterHiddenElement()
    {
        Range range = getRange("one<!--x--><strong style=\"display:none;\">two</strong><em></em>thr|ee");
        assertTrue(range.isCollapsed());
        assertEquals(3, range.getStartOffset());
        assertEquals(getContainer().getLastChild(), range.getStartContainer());
    }

    /**
     * Checks if the caret is correctly detected after a horizontal ruler.
     */
    public void testDetectCaretAfterHorizontalRuler()
    {
        Range range = getRange("<p>before</p><hr/>|<p>after</p>");
        assertTrue(range.isCollapsed());
        assertEquals(2, range.getStartOffset());
        assertEquals(getContainer(), range.getStartContainer());
    }

    /**
     * Checks if the caret is correctly detected before an image, when that image is preceded by invisible nodes.
     */
    public void testDetectCaretBeforeImageWithGarbage()
    {
        Range range = getRange("before<!--x--><em></em><strong style=\"display:none\">y</strong>|<img/>after");
        assertTrue(range.isCollapsed());
        assertEquals(4, range.getStartOffset());
        assertEquals(getContainer(), range.getStartContainer());
    }

    /**
     * Checks if the caret is correctly detected inside a table cell. This test verifies if the text nodes are correctly
     * selected while determining the caret position.
     */
    public void testDetectCaretInsideTableCell()
    {
        Range range = getRange("<table><tr><td>alice</td><td>bo|b</td></tr></table>");
        assertTrue(range.isCollapsed());
        assertEquals(2, range.getStartOffset());
        assertEquals(getContainer().getElementsByTagName("td").getItem(1).getFirstChild(), range.getStartContainer());
    }

    /**
     * Checks if the caret is correctly detected inside a text node after a button element. This test verifies if the
     * text nodes are correctly selected while determining the caret position.
     */
    public void testDetectCaretInTextAfterButton()
    {
        Range range = getRange("before<button>inside</button>aft|er");
        assertTrue(range.isCollapsed());
        assertEquals(getContainer().getLastChild(), range.getStartContainer());
        assertEquals(3, range.getStartOffset());
    }
}
