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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.xpn.xwiki.wysiwyg.client.selection.Range;

/**
 * Utility class providing methods for manipulating the DOM tree.
 * 
 * @version $Id$
 */
public abstract class DOMUtils
{
    /**
     * The instance in use.
     */
    private static DOMUtils instance;

    /**
     * NOTE: We use deferred binding because some of the methods don't have cross-browser implementation and we want to
     * load the implementation specific to the browser used.
     * 
     * @return the instance in use.
     */
    public static synchronized DOMUtils getInstance()
    {
        if (instance == null) {
            instance = GWT.create(DOMUtils.class);
        }
        return instance;
    }

    /**
     * Returns the value of the specified CSS property for the given element as it is computed by the browser before it
     * displays that element. The CSS property doesn't have to be applied explicitly or directly on the given element.
     * It can be inherited or assumed by default on that element.
     * 
     * @param el the element for which we retrieve the property value.
     * @param propertyName the name of the CSS property whose value is returned.
     * @return the computed value of the specified CSS property for the given element.
     */
    public abstract String getComputedStyleProperty(Element el, String propertyName);

    /**
     * @param node the node from where to begin the search for the next leaf.
     * @return the next leaf node in a deep-first search, considering we already looked in the subtree whose root is the
     *         given node.
     */
    public Node getNextLeaf(Node node)
    {
        Node ancestor = node;
        while (ancestor != null && ancestor.getNextSibling() == null) {
            ancestor = ancestor.getParentNode();
        }
        if (ancestor == null) {
            // There's no next leaf.
            return null;
        } else {
            // Return the first leaf in the subtree whose root is the next sibling of the ancestor.
            return getFirstLeaf(ancestor.getNextSibling());
        }
    }

    /**
     * @param node the node from where to begin the search for the previous leaf.
     * @return the previous leaf node in a reverse deep-first search, considering we already looked in the subtree whose
     *         root is the given node.
     */
    public Node getPreviousLeaf(Node node)
    {
        Node ancestor = node;
        while (ancestor != null && ancestor.getPreviousSibling() == null) {
            ancestor = ancestor.getParentNode();
        }
        if (ancestor == null) {
            // There's no previous leaf.
            return null;
        } else {
            // Return the last leaf in the subtree whose root is the next sibling of the ancestor.
            return getLastLeaf(ancestor.getPreviousSibling());
        }
    }

    /**
     * @param node the root of the DOM subtree whose first leaf is returned.
     * @return the first leaf node of the DOM subtree whose root is the given node.
     */
    public Node getFirstLeaf(Node node)
    {
        Node descendant = node;
        while (descendant.hasChildNodes()) {
            descendant = descendant.getFirstChild();
        }
        return descendant;
    }

    /**
     * @param node the root of the DOM subtree whose last leaf is returned.
     * @return the last leaf node of the DOM subtree whose root is the given node.
     */
    public Node getLastLeaf(Node node)
    {
        Node descendant = node;
        while (descendant.hasChildNodes()) {
            descendant = descendant.getLastChild();
        }
        return descendant;
    }

    /**
     * @param node the node whose index is returned.
     * @return the index of the given node among its siblings.
     */
    public int getNodeIndex(Node node)
    {
        int count = 0;
        Node leftSibling = node.getPreviousSibling();
        Node rightSibling = node.getNextSibling();
        while (leftSibling != null && rightSibling != null) {
            count++;
            leftSibling = leftSibling.getPreviousSibling();
            rightSibling = rightSibling.getNextSibling();
        }
        if (leftSibling == null) {
            return count;
        } else {
            return node.getParentNode().getChildNodes().getLength() - 1 - count;
        }
    }

    /**
     * @param node a DOM node.
     * @return the index of the given DOM node among its siblings, considering successive text nodes as one single node.
     */
    public int getNormalizedNodeIndex(Node node)
    {
        int count = 0;
        Node leftSibling = node;
        while (leftSibling.getPreviousSibling() != null) {
            if (leftSibling.getNodeType() != Node.TEXT_NODE
                || leftSibling.getPreviousSibling().getNodeType() != Node.TEXT_NODE) {
                count++;
            }
            leftSibling = leftSibling.getPreviousSibling();
        }
        return count;
    }

    /**
     * @param node a DOM node.
     * @return the child count for the given DOM node, considering successive child text nodes as one single child.
     */
    public int getNormalizedChildCount(Node node)
    {
        if (!node.hasChildNodes()) {
            return 0;
        } else {
            return 1 + getNormalizedNodeIndex(node.getLastChild());
        }
    }

    /**
     * Merges all the neighbor text nodes of the given text node and returns a text fragment specifying where is the
     * given text placed in the final text node resulted after the merge.
     * 
     * @param text the text node whose neighbors of the same type will be merged in a single text node.
     * @return a text fragment showing the place of the given text in the node obtained after the merge.
     */
    public TextFragment normalize(Text text)
    {
        StringBuffer leftText = new StringBuffer();
        Node leftSibling = text.getPreviousSibling();
        while (leftSibling != null && leftSibling.getNodeType() == Node.TEXT_NODE) {
            leftText.insert(0, leftSibling.getNodeValue());
            leftSibling.getParentNode().removeChild(leftSibling);
            leftSibling = text.getPreviousSibling();
        }

        StringBuffer rightText = new StringBuffer();
        Node rightSibling = text.getNextSibling();
        while (rightSibling != null && rightSibling.getNodeType() == Node.TEXT_NODE) {
            rightText.append(rightSibling.getNodeValue());
            rightSibling.getParentNode().removeChild(rightSibling);
            rightSibling = text.getNextSibling();
        }

        int startIndex = leftText.length();
        int endIndex = startIndex + text.getLength();
        text.setData(leftText.toString() + text.getData() + rightText.toString());
        return new TextFragment(text, startIndex, endIndex);
    }

    /**
     * @param text a text node.
     * @return the offset of the given text node relative to the left-most successive text node sibling. The offset is
     *         expressed as the number of characters between the given text node and the reference point.
     */
    public int getOffset(Text text)
    {
        int offset = 0;
        Node leftSibling = text.getPreviousSibling();
        while (leftSibling != null && leftSibling.getNodeType() == Node.TEXT_NODE) {
            offset += leftSibling.getNodeValue().length();
            leftSibling = leftSibling.getPreviousSibling();
        }
        return offset;
    }

    /**
     * Tests if the computed value of the display CSS property on the given node is inline.
     * 
     * @param node a DOM node.
     * @return true if the given DOM node is displayed in-line.
     */
    public boolean isInline(Node node)
    {
        return "inline".equalsIgnoreCase(getDisplay(node));
    }

    /**
     * @param node a DOM node.
     * @return the computed value of the display CSS value on the specified DOM node.
     */
    public String getDisplay(Node node)
    {
        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                return "inline";
            case Node.ELEMENT_NODE:
                return getComputedStyleProperty((Element) node, "display");
            default:
                return null;
        }
    }

    /**
     * Computes the longest text range included in the specified range. By text range we understand any range that
     * starts and ends in a text node. The end points of a text range can be in different text nodes.
     * 
     * @param range any range
     * @return the longest text range included in the given range.
     */
    public Range getTextRange(Range range)
    {
        if (range.isCollapsed()) {
            return range.cloneRange();
        } else if ("".equals(range.toString())) {
            Range textRange = range.cloneRange();
            if (textRange.getStartContainer().getNodeType() == Node.TEXT_NODE) {
                textRange.collapse(true);
            } else {
                textRange.collapse(false);
            }
            return textRange;
        } else {
            Range textRange = range.cloneRange();

            // Find the first text node in the range and start the range there
            if (range.getStartContainer().getNodeType() != Node.TEXT_NODE) {
                Node leaf = getFirstLeaf(range.getStartContainer().getChildNodes().getItem(range.getStartOffset()));
                while (leaf.getNodeType() != Node.TEXT_NODE) {
                    leaf = getNextLeaf(leaf);
                }
                textRange.setStart(leaf, 0);
            }

            // Find the last text node in the range and end the range there
            if (range.getEndContainer().getNodeType() != Node.TEXT_NODE) {
                Node leaf = getLastLeaf(range.getEndContainer().getChildNodes().getItem(range.getEndOffset()));
                while (leaf.getNodeType() != Node.TEXT_NODE) {
                    leaf = getPreviousLeaf(leaf);
                }
                textRange.setEnd(leaf, leaf.getNodeValue().length());
            }

            return textRange;
        }
    }
}
