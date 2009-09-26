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

import org.xwiki.gwt.dom.client.AbstractDOMTest;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.RangeCompare;
import org.xwiki.gwt.dom.client.Text;

import com.google.gwt.dom.client.Node;

/**
 * Unit tests for {@link IERange}.
 * 
 * @version $Id$
 */
public class IESelectionTest extends AbstractDOMTest
{
    /**
     * Used to mark the start or end of a text range.
     */
    public static final String PIPE = "|";

    /**
     * Sets the given HTML fragment as the inner HTML of the {@link #container} and creates a new range. The range is
     * specified using | (pipe) symbol. For instance, in the following HTML fragment "&lt;em&gt;fo|o&lt;/em&gt; ba|r"
     * the returned range will contain "&lt;em&gt;o&lt;/em&gt; ba".
     * 
     * @param html The HTML fragment containing the range.
     * @return The newly created range.
     */
    protected Range getRange(String html)
    {
        getContainer().xSetInnerHTML(html);

        TextRange textRange = TextRange.newInstance(getDocument());

        TextRange refRange = textRange.duplicate();
        refRange.moveToElementText(getContainer());
        if (refRange.findText(PIPE, 0, 0)) {
            refRange.setText("");
            textRange.setEndPoint(RangeCompare.END_TO_START, refRange);
            refRange.findText(PIPE, html.length() - html.indexOf(PIPE), 0);
            refRange.setText("");
            textRange.setEndPoint(RangeCompare.START_TO_END, refRange);
        } else {
            textRange.moveToElementText(getContainer());
        }

        // We cannot select the textRange because IE doesn't support collapsed selection in view mode (which is somehow
        // normal since you cannot have the caret in view mode). Instead we use a mock native selection which returns
        // our textRange like it has been selected.
        return (new IESelection(MockNativeSelection.newInstance(textRange))).getRangeAt(0);
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
        Range range = getRange("a|<!--x--><img src=\"none.ong\"/><em>#</em>");
        assertCollapsed(range);
        assertEquals(getContainer().getFirstChild(), range.getStartContainer());
        assertEquals(range.getStartContainer().getNodeValue().length(), range.getStartOffset());
    }

    /**
     * Caret after a comment node.
     */
    public void testCaretAfterACommentNode()
    {
        Range range = getRange("<em>#</em><img src=\"none.ong\"/><!--x-->|a");
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
        // assertEquals(getContainer().getLastChild(), range.getEndContainer());
        // The end point is found correctly in the first place, but as soon as the text range is created the end point
        // moves after the last selected character.
        assertEquals(getContainer().getChildNodes().getItem(1).getFirstChild(), range.getEndContainer());
        assertEquals(getContainer().getFirstChild().getNodeValue().length(), range.getStartOffset());
        // assertEquals(0, range.getEndOffset());
        assertEquals(((Text) getContainer().getChildNodes().getItem(1).getFirstChild()).getLength(), range
            .getEndOffset());
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
        assertEquals("<ins style=\"width: 30px\"></ins>", range.toHTML().toLowerCase());
        assertEquals(getContainer(), range.getCommonAncestorContainer());
        assertEquals(getContainer().getFirstChild(), range.getStartContainer());
        // assertEquals(getContainer().getLastChild(), range.getEndContainer());
        assertEquals(getContainer().getChildNodes().getItem(1), range.getEndContainer());
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
        assertEquals("<img>", range.toHTML().toLowerCase());
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
}
