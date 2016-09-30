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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.xwiki.gwt.dom.client.filter.HiddenElements;
import org.xwiki.gwt.dom.client.filter.NodeFilter;
import org.xwiki.gwt.dom.client.filter.WithName;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 * Utility class providing methods for manipulating the DOM tree. Add here only the methods that work with any kind of
 * DOM node and the methods that have different implementation for different browsers. For specific node types see
 * {@link Document}, {@link Element} or {@link Text}.
 * 
 * @version $Id$
 */
public class DOMUtils
{
    /**
     * Common error message used when a particular node type is not supported by a method.
     */
    public static final String UNSUPPORTED_NODE_TYPE = "Unsupported node type!";

    /**
     * The {@code <br/>} tag name.
     */
    public static final String BR = "br";

    /**
     * The {@code <hr/>} tag name.
     */
    public static final String HR = "hr";

    /**
     * The id attribute.
     */
    public static final String ID = "id";

    /**
     * Constant for the comment node type.
     */
    public static final short COMMENT_NODE = 8;

    /**
     * Constant for the CDATA node type.
     */
    public static final short CDATA_NODE = 4;

    /**
     * Constant for the DocumentFragment node type.
     */
    public static final short DOCUMENT_FRAGMENT_NODE = 11;

    /**
     * The list of all HTML tags that must be empty. All of them appear as <code>&lt;tagName/&gt;</code> in the HTML
     * code.
     * <p>
     * NOTE: We had to move this array from Element because there is a problem with static field initialization for
     * classes extending JavaScriptObject.
     * <p>
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=3192.
     */
    protected static final String[] HTML_EMPTY_TAGS =
        new String[] {"area", "base", "basefont", BR, "col", "frame", HR, "img", "input", "isindex", "link", "meta",
            "param", "nextid", "bgsound", "embed", "keygen", "spacer", "wbr"};

    /**
     * The list of all HTML elements that can have both in-line and block-level content, as specified by the XHTML 1.0
     * strict DTD.
     * <p>
     * NOTE: We added the {@code body} and {@code blockquote} elements since the editor allows us to write text directly
     * inside them. We also kept only the most important (used) flow containers to improve the search speed.
     */
    private static final String[] HTML_FLOW_CONTAINERS =
        new String[] {"body", "li", "td", "th", "dd", "div", "blockquote"};

    /**
     * The list of all block-level HTML elements that can have only in-line content, as specified by the XHTML 1.0
     * strict DTD.
     * <p>
     * NOTE: We kept only the most important (used) elements to improve the search speed.
     */
    private static final String[] HTML_BLOCK_LEVEL_INLINE_CONTAINERS =
        new String[] {"p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "dt", "address"};

    /**
     * The list of all block-level HTML elements that can have only special content, or no content at all, as specified
     * by the XHTML 1.0 strict DTD.
     */
    private static final String[] HTML_SPECIAL_BLOCK_LEVEL_ELEMENTS =
        new String[] {HR, "ul", "ol", "dl", "table", "tbody", "thead", "tfoot", "tr", "form"};

    /**
     * The name of the attribute that controls if the content of an element can be edited or not.
     */
    private static final String CONTENT_EDITABLE_ATTR = "contentEditable";

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
     * <p>
     * NOTE: You have to pass the JavaScript name of the property and not its CSS name. The JavaScript name has camel
     * case style ({@code fontWeight}) and it is used like this {@code object.style.propertyJSName = value}. The CSS
     * name has dash style ({@code font-weight}) and it is used like this {@code propertyCSSName: value;}.
     * 
     * @param element the element for which we retrieve the property value.
     * @param propertyName the script name of the CSS property whose value is returned.
     * @return the computed value of the specified CSS property for the given element.
     */
    public native String getComputedStyleProperty(Element element, String propertyName)
    /*-{
        var computedStyle = element.ownerDocument.defaultView.getComputedStyle(element, null);
        if (computedStyle) {
          // We force it to be a string because we treat it as a string in the java code.
          return '' + computedStyle[propertyName];
        } else {
          // Computed style can be null if the element is not displayed.
          return null;
        }
    }-*/;

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
     * @param range the range after which to look for a leaf
     * @return the next leaf which is not touched by the specified range.
     */
    public Node getNextLeaf(Range range)
    {
        Node endContainer = range.getEndContainer();
        if (endContainer.getNodeType() != Node.ELEMENT_NODE) {
            // The node is either a text node or a comment node, return next leaf
            return getNextLeaf(endContainer);
        } else {
            // the node is an element node, and the selection ends somewhere in between two child nodes. Check if it's
            // at the end of it's parent and return the parent's next leaf or the first leaf of the next node otherwise
            if (endContainer.hasChildNodes() && range.getEndOffset() < endContainer.getChildNodes().getLength()) {
                // get the first leaf of the node after the end container
                Node nodeAfter = endContainer.getChildNodes().getItem(range.getEndOffset());
                return getFirstLeaf(nodeAfter);
            } else {
                return getNextLeaf(endContainer);
            }
        }
    }

    /**
     * @param range the range before which to look for a leaf
     * @return the previous leaf which is not touched by the specified range.
     */
    public Node getPreviousLeaf(Range range)
    {
        Node startContainer = range.getStartContainer();
        if (startContainer.getNodeType() != Node.ELEMENT_NODE) {
            return getPreviousLeaf(startContainer);
        } else {
            // the node is an element node and the selection begins somewhere in between two child nodes. Check if it's
            // at the beginning of its parent and return the parent's previous leaf or the last leaf of the previous
            // node otherwise.
            if (startContainer.hasChildNodes() && range.getStartOffset() > 0) {
                Node nodeBefore = startContainer.getChildNodes().getItem(range.getStartOffset() - 1);
                return getLastLeaf(nodeBefore);
            } else {
                return getPreviousLeaf(startContainer);
            }

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
     * Computes the index that can be used with <code>getChildNodes().getItem()</code> to retrieve the given node from
     * its parent after the parent is serialized and deserialized.
     * 
     * @param node a DOM node
     * @return the index of the given DOM node among its siblings, considering successive text nodes as one single node
     *         and ignoring hidden siblings
     */
    public int getNormalizedNodeIndex(Node node)
    {
        int count = 0;
        Node sibling = node;
        while (sibling != null) {
            Node left = sibling.getPreviousSibling();
            if (sibling.getNodeType() == Node.TEXT_NODE) {
                while (left != null && (left.getNodeType() == Node.TEXT_NODE || !isSerializable(left))) {
                    left = left.getPreviousSibling();
                }
            } else {
                while (left != null && !isSerializable(left)) {
                    left = left.getPreviousSibling();
                }
            }
            count += (left != null) ? 1 : 0;
            sibling = left;
        }
        return count;
    }

    /**
     * Specifies if a node's HTML serialization is included in its parent node's HTML serialization. Normally the inner
     * HTML of an element includes the HTML representation of all of its descendants. This is not the case when one of
     * the descendants is an element with an empty meta data associated. Associating empty meta data to an element is a
     * way to hide that element from the inner HTML of his ancestors.
     * 
     * @param node a DOM node
     * @return true if the given node is represented in its parent inner HTML
     */
    public boolean isSerializable(Node node)
    {
        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                return node.getNodeValue().length() > 0;
            case Node.ELEMENT_NODE:
                Element element = Element.as(node);
                return !element.xHasAttribute(Element.META_DATA_ATTR)
                    || !"".equals(element.getAttribute(Element.META_DATA_ATTR));
            default:
                return true;
        }
    }

    /**
     * @param node a DOM node.
     * @return the child count for the given DOM node, considering successive child text nodes as one single child.
     * @see #getNormalizedNodeIndex(Node)
     */
    public int getNormalizedChildCount(Node node)
    {
        if (!node.hasChildNodes()) {
            return 0;
        } else {
            Node last = node.getLastChild();
            return (isSerializable(last) ? 1 : 0) + getNormalizedNodeIndex(last);
        }
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given DOM node represents in-line content
     */
    public boolean isInline(Node node)
    {
        return !isBlock(node);
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is a block-level element, {@code false} otherwise
     */
    public boolean isBlock(Node node)
    {
        return isFlowContainer(node) || isBlockLevelInlineContainer(node) || isSpecialBlock(node);
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is a block-level element that can have only in-line content, {@code false}
     *         otherwise
     * @see #HTML_BLOCK_LEVEL_INLINE_CONTAINERS
     */
    public boolean isBlockLevelInlineContainer(Node node)
    {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        String tagName = node.getNodeName().toLowerCase();
        for (int i = 0; i < HTML_BLOCK_LEVEL_INLINE_CONTAINERS.length; i++) {
            if (tagName.equals(HTML_BLOCK_LEVEL_INLINE_CONTAINERS[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is a block-level element that can have only special content, {@code false}
     *         otherwise
     * @see #HTML_SPECIAL_BLOCK_LEVEL_ELEMENTS
     */
    public boolean isSpecialBlock(Node node)
    {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        String tagName = node.getNodeName().toLowerCase();
        for (int i = 0; i < HTML_SPECIAL_BLOCK_LEVEL_ELEMENTS.length; i++) {
            if (tagName.equals(HTML_SPECIAL_BLOCK_LEVEL_ELEMENTS[i])) {
                return true;
            }
        }
        return false;
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
        Range textRange = range.cloneRange();
        Node firstLeaf = getFirstLeaf(range);
        if (firstLeaf != null) {
            Node lastLeaf = getLastLeaf(range);
            // Find the first text node in the range and start the range there.
            while (firstLeaf != lastLeaf && firstLeaf.getNodeType() != Node.TEXT_NODE) {
                firstLeaf = getNextLeaf(firstLeaf);
            }
            if (firstLeaf.getNodeType() == Node.TEXT_NODE && firstLeaf != textRange.getStartContainer()) {
                textRange.setStart(firstLeaf, 0);
            }
            // Find the last text node in the range and end the range there.
            while (lastLeaf != firstLeaf && lastLeaf.getNodeType() != Node.TEXT_NODE) {
                lastLeaf = getPreviousLeaf(lastLeaf);
            }
            if (lastLeaf.getNodeType() == Node.TEXT_NODE && lastLeaf != textRange.getEndContainer()) {
                textRange.setEnd(lastLeaf, lastLeaf.getNodeValue().length());
            }
        }
        return textRange;
    }

    /**
     * Computes the maximal <strong>sub-range</strong> of the given range that satisfies the following two conditions:
     * <ul>
     * <li>the start point is before a <strong>leaf</strong> element node that can't have child nodes (e.g. an image) or
     * inside a leaf node at position 0 (e.g. an empty span or a text node)</li>
     * <li>the end point is after a leaf element node that can't have child nodes (e.g. an image) or inside a leaf node
     * at the end (e.g. inside an empty span or at the end of a text node)</li>
     * </ul>
     * . If no such sub-range exists (because the given range doesn't wrap any leaf node and none of its end points
     * satisfies the corresponding condition) then the given range is returned unmodified.
     * 
     * @param range a DOM range
     * @return the maximal sub-range that selects the same content as the given range
     */
    public Range shrinkRange(Range range)
    {
        if (range == null || range.isCollapsed()) {
            return range;
        }

        // Find the start and end points that satisfy the conditions.
        Range start = getShrunkenRangeStart(range);
        Range end = getShrunkenRangeEnd(range);

        // If at least one of the end points moved and the range is still valid.
        if ((start != range || end != range) && start.compareBoundaryPoints(RangeCompare.END_TO_START, end) <= 0) {
            Range result = range.cloneRange();
            result.setStart(start.getEndContainer(), start.getEndOffset());
            result.setEnd(end.getStartContainer(), end.getStartOffset());
            return result;
        }
        return range;
    }

    /**
     * Utility method to get the start point of the shrunken range (obtained with {@link #shrinkRange(Range)}).
     * 
     * @param range a DOM range
     * @return the start point of the range returned by {@link #shrinkRange(Range)}
     * @see #shrinkRange(Range)
     */
    private Range getShrunkenRangeStart(Range range)
    {
        Node startContainer = range.getStartContainer();
        if (startContainer.hasChildNodes()) {
            if (range.getStartOffset() < startContainer.getChildCount()) {
                // Before a child node of an element.
                startContainer = getFirstLeaf(startContainer.getChild(range.getStartOffset()));
            } else {
                // After the last child of an element.
                startContainer = getNextLeaf(startContainer);
            }
        } else if (range.getStartOffset() > 0 && range.getStartOffset() == startContainer.getNodeValue().length()) {
            // At the end of a non-empty text node.
            startContainer = getNextLeaf(startContainer);
        }

        if (startContainer != null) {
            int startOffset = 0;
            if (startContainer.getNodeType() == Node.ELEMENT_NODE && !canHaveChildren(startContainer)) {
                startOffset = getNodeIndex(startContainer);
                startContainer = startContainer.getParentNode();
            }

            // Return a new range only if we managed to move the start.
            if (startContainer != range.getStartContainer()) {
                Range start = range.cloneRange();
                start.setEnd(startContainer, startOffset);
                start.collapse(false);
                return start;
            }
        }

        return range;
    }

    /**
     * Utility method to get the end point of the shrunken range (obtained with {@link #shrinkRange(Range)}).
     * 
     * @param range a DOM range
     * @return the end point of the range returned by {@link #shrinkRange(Range)}
     * @see #shrinkRange(Range)
     */
    private Range getShrunkenRangeEnd(Range range)
    {
        Node endContainer = range.getEndContainer();
        if (endContainer.hasChildNodes()) {
            if (range.getEndOffset() > 0) {
                // After a child node of an element.
                endContainer = getLastLeaf(endContainer.getChild(range.getEndOffset() - 1));
            } else {
                // Before the first child of an element.
                endContainer = getPreviousLeaf(endContainer);
            }
        } else if (range.getEndOffset() == 0 && getLength(endContainer) > 0) {
            // At the start of a non-empty text node.
            endContainer = getPreviousLeaf(endContainer);
        }

        if (endContainer != null) {
            int endOffset = getLength(endContainer);
            if (endContainer.getNodeType() == Node.ELEMENT_NODE && !canHaveChildren(endContainer)) {
                endOffset = getNodeIndex(endContainer) + 1;
                endContainer = endContainer.getParentNode();
            }

            // Return a new range only if we managed to move the end.
            if (endContainer != range.getEndContainer()) {
                Range end = range.cloneRange();
                end.setStart(endContainer, endOffset);
                end.collapse(true);
                return end;
            }
        }

        return range;
    }

    /**
     * Creates a copy of a node from an external document that can be inserted into the given document.
     * 
     * @param doc The document in which we want to insert the returned copy.
     * @param externalNode The node from another document to be imported.
     * @param deep Indicates whether the children of the given node need to be imported.
     * @return a copy of the given node that can be inserted into the specified document.
     */
    public native Node importNode(Document doc, Node externalNode, boolean deep)
    /*-{
        return doc.importNode(externalNode, deep);
    }-*/;

    /**
     * @param element The DOM element whose attribute names are returned.
     * @return The names of DOM attributes present on the given element.
     */
    public native JsArrayString getAttributeNames(Element element)
    /*-{
        var attrNames = [];
        for(var i = 0; i < element.attributes.length; i++) {
            attrNames.push(element.attributes.item(i).nodeName);
        }
        return attrNames;
    }-*/;

    /**
     * Searches for the first ancestor with a name from {@code tagNames} of the passed node, including the node itself.
     * The search order starts with {@code node} and continues to the root of the tree.
     * 
     * @param node the node to find ancestor for
     * @param tagNames the tag names to look for up in the DOM tree.
     * @return the first node a name from <code>tagNames</code> found.
     */
    public Node getFirstAncestor(Node node, String... tagNames)
    {
        return getFirstAncestor(node, new WithName(tagNames));
    }

    /**
     * Searches for the first ancestor of the given node, including the node itself, that matches the specified filter.
     * The search order starts with {@code node} and continues to the root of the tree.
     * 
     * @param node the node whose ancestor needs to be found
     * @param filter the object used to filter the ancestors
     * @return the first ancestor of the given node that matches the filter
     */
    public Node getFirstAncestor(Node node, NodeFilter filter)
    {
        Node ancestor = node;
        while (ancestor != null) {
            if (filter.acceptNode(ancestor) == NodeFilter.Action.ACCEPT) {
                return ancestor;
            }
            ancestor = ancestor.getParentNode();
        }
        return null;
    }

    /**
     * Searches for the last ancestor of the given node, including the node itself, that matches the specified filter.
     * The search order starts with {@code node} and continues to the root of the tree.
     * 
     * @param node the node whose ancestor needs to be found
     * @param filter the object used to filter the ancestors
     * @return the last ancestor of the given node that matches the filter
     */
    public Node getLastAncestor(Node node, NodeFilter filter)
    {
        Node ancestor = node;
        Node lastAncestor = null;
        while (ancestor != null) {
            if (filter.acceptNode(ancestor) == NodeFilter.Action.ACCEPT) {
                lastAncestor = ancestor;
            }
            ancestor = ancestor.getParentNode();
        }
        return lastAncestor;
    }

    /**
     * Searches for the first element descendant with the name <code>tagName</code>. Searching is done in a DFS order
     * with node processing on first pass through them.
     * 
     * @param node the node to start the search from
     * @param tagName the name of the searched element
     * @return the first descendant of type <code>tagName</code> of the passed node, in DFS order.
     */
    public Node getFirstDescendant(Node node, String tagName)
    {
        Iterator<Node> it = ((Document) node.getOwnerDocument()).getIterator(node);
        while (it.hasNext()) {
            Node currentNode = it.next();
            if (currentNode.getNodeType() == Node.ELEMENT_NODE && currentNode.getNodeName().equalsIgnoreCase(tagName)) {
                return currentNode;
            }
        }
        return null;
    }

    /**
     * Helps setting the inner HTML for an element, in a cross-browser manner, because IE seems to trim leading comments
     * in the inner HTML. This method is overwritten in IE's specific implementation.
     * <p>
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=3146.
     * 
     * @param element element to set the inner HTML for
     * @param html the HTML string to set
     */
    public void setInnerHTML(Element element, String html)
    {
        element.setInnerHTML(html);
    }

    /**
     * @param alice A DOM node.
     * @param bob A DOM node.
     * @return The nearest common ancestor of the given nodes.
     */
    public Node getNearestCommonAncestor(Node alice, Node bob)
    {
        if (alice == bob) {
            return alice;
        }

        // Build the chain of parents
        List<Node> aliceAncestors = getAncestors(alice);
        List<Node> bobAncestors = getAncestors(bob);

        // Find where the parent chain differs
        int count = Math.min(aliceAncestors.size(), bobAncestors.size());
        int aliceIndex = aliceAncestors.size();
        int bobIndex = bobAncestors.size();
        Node ancestor = null;
        while (count-- > 0 && aliceAncestors.get(--aliceIndex) == bobAncestors.get(--bobIndex)) {
            ancestor = aliceAncestors.get(aliceIndex);
        }
        return ancestor;
    }

    /**
     * @param node a DOM node
     * @return the list of ancestors of the given node, starting with it
     */
    public List<Node> getAncestors(Node node)
    {
        if (node == null) {
            return Collections.emptyList();
        }
        List<Node> ancestors = new ArrayList<Node>();
        Node ancestor = node;
        do {
            ancestors.add(ancestor);
            ancestor = ancestor.getParentNode();
        } while (ancestor != null);
        return ancestors;
    }

    /**
     * Clones the contents of the given node. If node type is text, CDATA or comment then only the data between
     * startOffset (including) and endOffset is kept. If node type is element then only the child nodes with indexes
     * between startOffset (including) and endOffset are included in the document fragment returned.
     * 
     * @param node The DOM node whose contents will be cloned.
     * @param startOffset the index of the first child to clone or the first character to include in the cloned
     *            contents.
     * @param endOffset specifies where the cloned contents end.
     * @return the cloned contents of the given node, between start offset and end offset.
     */
    public DocumentFragment cloneNodeContents(Node node, int startOffset, int endOffset)
    {
        DocumentFragment contents = ((Document) node.getOwnerDocument()).createDocumentFragment();
        switch (node.getNodeType()) {
            case CDATA_NODE:
            case COMMENT_NODE:
            case Node.TEXT_NODE:
                if (startOffset < endOffset) {
                    Node clone = node.cloneNode(false);
                    clone.setNodeValue(node.getNodeValue().substring(startOffset, endOffset));
                    contents.appendChild(clone);
                }
                break;
            case Node.ELEMENT_NODE:
                for (int i = startOffset; i < endOffset; i++) {
                    contents.appendChild(node.getChildNodes().getItem(i).cloneNode(true));
                }
                break;
            default:
                // ignore
        }
        return contents;
    }

    /**
     * Clones the given DOM node, keeping only the contents between start and end offset. If node type is text, CDATA or
     * comment then both offsets represent character indexes. Otherwise they represent child indexes.
     * 
     * @param node The DOM node to be cloned.
     * @param startOffset specifies where to start the cloning.
     * @param endOffset specifies where to end the cloning.
     * @return A clone of the given node, containing only the contents between start and end offset.
     */
    public Node cloneNode(Node node, int startOffset, int endOffset)
    {
        Node clone = node.cloneNode(false);
        switch (node.getNodeType()) {
            case CDATA_NODE:
            case COMMENT_NODE:
            case Node.TEXT_NODE:
                clone.setNodeValue(node.getNodeValue().substring(startOffset, endOffset));
                return clone;
            case Node.ELEMENT_NODE:
                for (int i = startOffset; i < endOffset; i++) {
                    clone.appendChild(node.getChildNodes().getItem(i).cloneNode(true));
                }
                return clone;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_NODE_TYPE);
        }
    }

    /**
     * @param node A DOM node.
     * @return the number of characters if the given node is a text, a CDATA section or a comment. Otherwise the
     *         returned value is the number of child nodes.
     */
    public int getLength(Node node)
    {
        switch (node.getNodeType()) {
            case CDATA_NODE:
            case COMMENT_NODE:
            case Node.TEXT_NODE:
                return node.getNodeValue().length();
            default:
                return node.getChildNodes().getLength();
        }
    }

    /**
     * Clones the left or right side of the subtree rooted in the given node.
     * 
     * @param node The root of the subtree whose left or right side will be cloned.
     * @param offset Marks the boundary between the left and the right subtrees. It can be either a character index or a
     *            child index, depending on the type of the given node.
     * @param left Specifies which of the subtrees to be cloned.
     * @return The clone of the specified subtree.
     */
    public Node cloneNode(Node node, int offset, boolean left)
    {
        return left ? cloneNode(node, 0, offset) : cloneNode(node, offset, getLength(node));
    }

    /**
     * Clones the node specified by its parent and its descendant, including only the left or right part of the tree
     * whose separator is the path from the given descendant to the parent of the cloned node.
     * 
     * @param parent The parent of the cloned node.
     * @param descendant A descendant of the cloned node.
     * @param offset The offset within the given descendant. It can be either a character index or a child index
     *            depending on the descendant node type.
     * @param left Specifies which subtree to be cloned. Left and right subtrees are delimited by the path from the
     *            given descendant to the parent of the cloned node.
     * @return The clone of the specified subtree.
     */
    public Node cloneNode(Node parent, Node descendant, int offset, boolean left)
    {
        int delta = left ? 0 : 1;
        int index = getNodeIndex(descendant) + delta;
        Node clone = cloneNode(descendant, offset, left);
        Node node = descendant.getParentNode();
        while (node != parent) {
            Node child = clone;
            clone = cloneNode(node, index, left);
            if (left || clone.getFirstChild() == null) {
                clone.appendChild(child);
            } else {
                clone.insertBefore(child, clone.getFirstChild());
            }
            index = getNodeIndex(node) + delta;
            node = node.getParentNode();
        }
        return clone;
    }

    /**
     * @param parent the parent node of the retrieved child
     * @param descendant a descendant of the retrieved child
     * @return the child of the given parent, which has the specified descendant
     */
    public Node getChild(Node parent, Node descendant)
    {
        Node child = descendant;
        while (child != null && child.getParentNode() != parent) {
            child = child.getParentNode();
        }
        return child;
    }

    /**
     * Inserts the given child node after the reference node.
     * 
     * @param newChild The child node to be inserted.
     * @param refChild The reference node.
     */
    public void insertAfter(Node newChild, Node refChild)
    {
        if (refChild.getNextSibling() != null) {
            refChild.getParentNode().insertBefore(newChild, refChild.getNextSibling());
        } else {
            refChild.getParentNode().appendChild(newChild);
        }
    }

    /**
     * Deletes the contents of the given node between the specified offsets. If node type is text, CDATA or comment then
     * only the data between startOffset (including) and endOffset is deleted. If node type is element then only the
     * child nodes with indexes between startOffset (including) and endOffset are deleted.
     * 
     * @param node The DOM node whose contents will be deleted.
     * @param startOffset the index of the first child or the first character to delete, depending on node type.
     * @param endOffset specifies where to stop deleting content.
     */
    public void deleteNodeContents(Node node, int startOffset, int endOffset)
    {
        switch (node.getNodeType()) {
            case CDATA_NODE:
            case COMMENT_NODE:
            case Node.TEXT_NODE:
                if (startOffset < endOffset) {
                    node.setNodeValue(node.getNodeValue().substring(0, startOffset)
                        + node.getNodeValue().substring(endOffset));
                }
                break;
            case Node.ELEMENT_NODE:
                for (int i = startOffset; i < endOffset; i++) {
                    node.removeChild(node.getChildNodes().getItem(startOffset));
                }
                break;
            default:
                // ignore
        }
    }

    /**
     * Deletes the left or right side of the subtree rooted in the given node.
     * 
     * @param node The root of the subtree whose left or right side will be deleted.
     * @param offset Marks the boundary between the left and the right subtrees. It can be either a character index or a
     *            child index, depending on the type of the given node.
     * @param left Specifies which of the subtrees to be deleted.
     */
    public void deleteNodeContents(Node node, int offset, boolean left)
    {
        if (left) {
            deleteNodeContents(node, 0, offset);
        } else {
            deleteNodeContents(node, offset, getLength(node));
        }
    }

    /**
     * Deletes left or right siblings of the given node.
     * 
     * @param node The DOM node whose left or right siblings will be deleted.
     * @param left Specifies which siblings to delete.
     */
    public void deleteSiblings(Node node, boolean left)
    {
        Node sibling = left ? node.getPreviousSibling() : node.getNextSibling();
        while (sibling != null) {
            node.getParentNode().removeChild(sibling);
            sibling = left ? node.getPreviousSibling() : node.getNextSibling();
        }
    }

    /**
     * Given a subtree specified by its root parent and one of the inner nodes, this method deletes the left or right
     * part delimited by the path from the given descendant (inner node) to the root parent.
     * 
     * @param parent The parent node of the subtree's root.
     * @param descendant An inner node within the specified subtree.
     * @param offset The offset within the given descendant. It can be either a character index or a child index
     *            depending on the descendant node type.
     * @param left Specifies which side of the subtree to be deleted. Left and right parts are delimited by the path
     *            from the given descendant to the parent of the subtree's root.
     */
    public void deleteNodeContents(Node parent, Node descendant, int offset, boolean left)
    {
        deleteNodeContents(descendant, offset, left);
        Node node = descendant;
        while (node.getParentNode() != parent) {
            deleteSiblings(node, left);
            node = node.getParentNode();
        }
    }

    /**
     * Splits the given DOM node at the specified offset.
     * 
     * @param node The node to be split.
     * @param offset Specifies where to split. It can be either a character index or a child index depending on node
     *            type.
     * @return The node resulted after the split. It should be the next sibling of the given node.
     */
    public Node splitNode(Node node, int offset)
    {
        Node clone = node.cloneNode(false);
        switch (node.getNodeType()) {
            case CDATA_NODE:
            case COMMENT_NODE:
            case Node.TEXT_NODE:
                clone.setNodeValue(node.getNodeValue().substring(offset));
                node.setNodeValue(node.getNodeValue().substring(0, offset));
                break;
            case Node.ELEMENT_NODE:
                Element.as(clone).removeAttribute(ID);
                for (int i = node.getChildNodes().getLength(); i > offset; i--) {
                    clone.appendChild(node.getChildNodes().getItem(offset));
                }
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_NODE_TYPE);
        }
        insertAfter(clone, node);
        return clone;
    }

    /**
     * Given a subtree specified by its root parent and one of the inner nodes, this method splits the subtree by the
     * path from the given descendant (inner node) to the root parent.
     * 
     * @param parent The parent node of the subtree's root.
     * @param descendant An inner node within the specified subtree.
     * @param offset The offset within the given descendant. It can be either a character index or a child index
     *            depending on the descendant node type.
     * @return The node resulted from splitting the descendant.
     */
    public Node splitNode(Node parent, Node descendant, int offset)
    {
        if (descendant == parent) {
            return descendant;
        }
        Node nextLevelSibling = splitNode(descendant, offset);
        Node node = descendant;
        while (node.getParentNode() != parent) {
            splitNode(node.getParentNode(), getNodeIndex(node) + 1);
            node = node.getParentNode();
        }
        return nextLevelSibling;
    }

    /**
     * Given a subtree specified by its root parent and one of the inner nodes, this method splits the subtree by the
     * path from the given descendant (inner node) to the root parent. Additionally to what
     * {@link #splitNode(Node, Node, int)}) does this method ensures that both subtrees are editable in design mode.
     * This method is required because most browsers prevent the user from placing the caret inside empty block elements
     * such as paragraphs or headers. This empty block elements can be obtained by splitting at the beginning or at the
     * end of such a block element.
     * 
     * @param parent the parent node of the subtree's root
     * @param descendant an inner node within the specified subtree
     * @param offset the offset within the given descendant. It can be either a character index or a child index
     *            depending on the descendant node type.
     * @return the node resulted from splitting the descendant
     * @see #splitNode(Node, Node, int)
     */
    public Node splitHTMLNode(Node parent, Node descendant, int offset)
    {
        // Save the length of the descendant before the split to be able to detect where the split took place.
        int length = getLength(descendant);

        // Split the subtree rooted in the given parent.
        Node nextLevelSibling = splitNode(parent, descendant, offset);

        // See if the split took place.
        if (nextLevelSibling != descendant) {
            if (offset == 0) {
                // The split took place at the beginning of the descendant. Ensure the first subtree is accessible.
                // But first see if the first subtree has any leafs besides the descendant.
                Node child = getChild(parent, descendant);
                if (!isInline(child) && getFirstLeaf(child) == descendant) {
                    Node refNode = getFarthestInlineAncestor(descendant);
                    refNode = refNode == null ? child : refNode.getParentNode();
                    ensureBlockIsEditable((Element) refNode);
                }
            }
            if (offset == length) {
                // The split took place at the end of the descendant. Ensure the second subtree is accessible.
                // But first see if the second subtree has any leafs besides the nextLevelSibling.
                Node child = getChild(parent, nextLevelSibling);
                if (!isInline(child) && getLastLeaf(child) == nextLevelSibling) {
                    Node refNode = getFarthestInlineAncestor(nextLevelSibling);
                    refNode = refNode == null ? child : refNode.getParentNode();
                    ensureBlockIsEditable((Element) refNode);
                }
            }
        }

        return nextLevelSibling;
    }

    /**
     * @param node A DOM node.
     * @return true is the given node is an element that can have both in-line and block content.
     */
    public boolean isFlowContainer(Node node)
    {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        String tagName = node.getNodeName().toLowerCase();
        for (int i = 0; i < HTML_FLOW_CONTAINERS.length; i++) {
            if (tagName.equals(HTML_FLOW_CONTAINERS[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param innerNode A DOM node.
     * @return The nearest ancestor of the given node that can contain both in-line and block content.
     */
    public Node getNearestFlowContainer(Node innerNode)
    {
        Node node = innerNode;
        while (node != null && !isFlowContainer(node)) {
            node = node.getParentNode();
        }
        return node;
    }

    /**
     * Inserts a node at the specified index under the given parent.
     * 
     * @param parent The parent node which will adopt the given node.
     * @param newChild The node to be inserted.
     * @param index Specifies the position inside the parent node where the new child should be placed.
     */
    public void insertAt(Node parent, Node newChild, int index)
    {
        int i = Math.max(0, index);
        if (i >= parent.getChildNodes().getLength()) {
            parent.appendChild(newChild);
        } else {
            parent.insertBefore(newChild, parent.getChildNodes().getItem(i));
        }
    }

    /**
     * Walks from the given node up to the root of the DOM tree as long as the ancestors represent in-line content.
     * Returns the last node in the walk.
     * 
     * @param node a DOM node
     * @return the farthest ancestor of the given node that represents in-line content
     */
    public Node getFarthestInlineAncestor(Node node)
    {
        Node ancestor = node;
        Node inlineAncestor = null;
        while (ancestor != null && isInline(ancestor)) {
            inlineAncestor = ancestor;
            ancestor = ancestor.getParentNode();
        }
        return inlineAncestor;
    }

    /**
     * @param range A DOM range.
     * @return the first leaf node that is partially or entirely included in the given range.
     */
    public Node getFirstLeaf(Range range)
    {
        if (range.getStartContainer().hasChildNodes()) {
            if (range.isCollapsed()) {
                return null;
            } else if (range.getStartOffset() >= range.getStartContainer().getChildNodes().getLength()) {
                return getNextLeaf(range.getStartContainer());
            } else {
                return getFirstLeaf(range.getStartContainer().getChildNodes().getItem(range.getStartOffset()));
            }
        } else {
            return range.getStartContainer();
        }
    }

    /**
     * @param range A DOM range.
     * @return the last leaf node that is partially or entirely included in the given range.
     */
    public Node getLastLeaf(Range range)
    {
        if (range.getEndContainer().hasChildNodes()) {
            if (range.isCollapsed()) {
                return null;
            } else if (range.getEndOffset() == 0) {
                return getPreviousLeaf(range.getEndContainer());
            } else {
                return getLastLeaf(range.getEndContainer().getChildNodes().getItem(range.getEndOffset() - 1));
            }
        } else {
            return range.getEndContainer();
        }
    }

    /**
     * Removes the given node from its parent.
     * 
     * @param node A DOM node.
     */
    public void detach(Node node)
    {
        if (node != null && node.getParentNode() != null) {
            node.getParentNode().removeChild(node);
        }
    }

    /**
     * @param node A DOM node
     * @return The nearest block level ancestor of the given node.
     */
    public Node getNearestBlockContainer(Node node)
    {
        Node ancestor = DOMUtils.getInstance().getFarthestInlineAncestor(node);
        if (ancestor == null) {
            return node;
        } else {
            return ancestor.getParentNode();
        }
    }

    /**
     * Returns the value of the named attribute of the specified element. This method will be overwritten for Internet
     * Explorer browsers to overcome the fact that the native implementation doesn't return the value from some of the
     * standard attributes like {@code style} and {@code class}. This method should be used only when the attribute name
     * is not know.
     * 
     * @param element the element to get the attribute for
     * @param name the name of the attribute to return
     * @return the value of the attribute
     */
    public String getAttribute(Element element, String name)
    {
        return element.getAttribute(name);
    }

    /**
     * Sets the value of the specified attribute for the given element. This method will be overwritten for Internet
     * Explorer to overcome that fact that the style attribute cannot be set in the standard way.
     * 
     * @param element the element whose attribute is set
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void setAttribute(Element element, String name, String value)
    {
        element.setAttribute(name, value);
    }

    /**
     * Compares two points in a {@link Document}. Each point is specified by a DOM node and an offset within that node.
     * 
     * @param alice first point's node
     * @param aliceOffset first point's offset
     * @param bob second point's node
     * @param bobOffset second point's offset
     * @return -1, 0 or 1 depending on whether the first point is respectively before, equal to, or after the second
     *         point
     */
    public short comparePoints(Node alice, int aliceOffset, Node bob, int bobOffset)
    {
        if (alice == bob) {
            return (short) Integer.signum(aliceOffset - bobOffset);
        }

        // Build the chain of parents.
        List<Node> aliceAncestors = getAncestors(alice);
        List<Node> bobAncestors = getAncestors(bob);

        // Test if the input nodes are disconnected.
        int aliceIndex = aliceAncestors.size() - 1;
        int bobIndex = bobAncestors.size() - 1;
        if (aliceAncestors.get(aliceIndex) != bobAncestors.get(bobIndex)) {
            throw new IllegalArgumentException();
        }

        // Find where the parent chain differs.
        for (int count = Math.min(aliceIndex, bobIndex); count > 0; --count) {
            Node aliceAncestor = aliceAncestors.get(--aliceIndex);
            Node bobAncestor = bobAncestors.get(--bobIndex);
            if (aliceAncestor != bobAncestor) {
                return (short) (getNodeIndex(aliceAncestor) < getNodeIndex(bobAncestor) ? -1 : 1);
            }
        }

        // The parent chains never differed, so one of the nodes is an ancestor of the other.
        if (aliceIndex == 0) {
            Node bobAncestor = bobAncestors.get(--bobIndex);
            return (short) (aliceOffset <= getNodeIndex(bobAncestor) ? -1 : 1);
        }

        Node aliceAncestor = aliceAncestors.get(--aliceIndex);
        return (short) (getNodeIndex(aliceAncestor) < bobOffset ? -1 : 1);
    }

    /**
     * We need our own implementation because the one provided by GWT includes commented text in the output.
     * <p>
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=3275.
     * 
     * @param element the element whose inner text to return
     * @return the text between the start and end tags of the given element
     */
    public String getInnerText(Element element)
    {
        // To mimic IE's 'innerText' property in the W3C DOM, we need to recursively
        // concatenate all child text nodes (depth first).
        StringBuffer text = new StringBuffer();
        Node child = element.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                text.append(getInnerText((Element) child));
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                text.append(child.getNodeValue());
            }
            child = child.getNextSibling();
        }
        return text.toString();
    }

    /**
     * @param element a DOM element
     * @return {@code true} if the given element has any attributes
     */
    public native boolean hasAttributes(Element element)
    /*-{
        return element.hasAttributes();
    }-*/;

    /**
     * Extracts the contents of the given node. If node type is text, CDATA or comment then only the data between
     * startOffset (including) and endOffset is kept. If node type is element then only the child nodes with indexes
     * between startOffset (including) and endOffset are included in the document fragment returned.
     * 
     * @param node the DOM node whose contents will be extracted
     * @param startOffset the index of the first child to extract or the first character to include in the extracted
     *            contents
     * @param endOffset specifies where the extracted contents end
     * @return the extracted contents of the given node, between start offset and end offset
     */
    public DocumentFragment extractNodeContents(Node node, int startOffset, int endOffset)
    {
        DocumentFragment contents = ((Document) node.getOwnerDocument()).createDocumentFragment();
        switch (node.getNodeType()) {
            case CDATA_NODE:
            case COMMENT_NODE:
            case Node.TEXT_NODE:
                if (startOffset < endOffset) {
                    Node clone = node.cloneNode(false);
                    clone.setNodeValue(node.getNodeValue().substring(startOffset, endOffset));
                    contents.appendChild(clone);
                    node.setNodeValue(node.getNodeValue().substring(0, startOffset)
                        + node.getNodeValue().substring(endOffset));
                }
                break;
            case Node.ELEMENT_NODE:
                // Collect all the child nodes that need to be extracted before removing them because the removal of a
                // node can have side effects (e.g. the browser inserts a BR element, changing the child node indexes).
                for (Node child : getChildNodes(node, startOffset, endOffset)) {
                    contents.appendChild(child);
                }
                break;
            default:
                // ignore
        }
        return contents;
    }

    /**
     * Obtain a sub-list of child nodes.
     * 
     * @param node the parent node
     * @param startOffset the index of the first child node to return
     * @param endOffset the index where to stop
     * @return the children of the given node that have the index between the specified values (including start offset
     *         and excluding end offset)
     */
    public List<Node> getChildNodes(Node node, int startOffset, int endOffset)
    {
        NodeList<Node> childNodes = node.getChildNodes();
        List<Node> subList = new ArrayList<Node>();
        for (int i = startOffset; i < endOffset && i < childNodes.getLength(); i++) {
            subList.add(childNodes.getItem(i));
        }
        return subList;
    }

    /**
     * Extracts the node specified by its parent and its descendant, including only the left or right part of the tree
     * whose separator is the path from the given descendant to the parent of the extracted node.
     * 
     * @param parent the parent of the extracted node
     * @param descendant a descendant of the extracted node
     * @param offset the offset within the given descendant. It can be either a character index or a child index
     *            depending on the descendant node type.
     * @param left specifies which subtree to be extracted. Left and right subtrees are delimited by the path from the
     *            given descendant to the parent of the extracted node.
     * @return the extracted subtree
     */
    public Node extractNode(Node parent, Node descendant, int offset, boolean left)
    {
        int delta = left ? 0 : 1;
        int index = getNodeIndex(descendant) + delta;
        Node clone = extractNode(descendant, offset, left);
        Node node = descendant.getParentNode();
        while (node != parent) {
            Node child = clone;
            clone = extractNode(node, index, left);
            if (left || clone.getFirstChild() == null) {
                clone.appendChild(child);
            } else {
                clone.insertBefore(child, clone.getFirstChild());
            }
            index = getNodeIndex(node) + delta;
            node = node.getParentNode();
        }
        return clone;
    }

    /**
     * Extracts the left or right side of the subtree rooted in the given node.
     * 
     * @param node the root of the subtree whose left or right side will be extracted
     * @param offset marks the boundary between the left and the right subtrees. It can be either a character index or a
     *            child index, depending on the type of the given node.
     * @param left specifies which of the subtrees to be extracted
     * @return the extracted subtree
     */
    public Node extractNode(Node node, int offset, boolean left)
    {
        return left ? extractNode(node, 0, offset) : extractNode(node, offset, getLength(node));
    }

    /**
     * Extracts the given DOM node, keeping only the contents between start and end offset. If node type is text, CDATA
     * or comment then both offsets represent character indexes. Otherwise they represent child indexes.
     * 
     * @param node the DOM node to be extracted
     * @param startOffset specifies where to start the extraction
     * @param endOffset specifies where to end the extraction
     * @return a shallow clone of the given node, containing only the contents between start and end offset, which have
     *         been extracted from the input node
     */
    public Node extractNode(Node node, int startOffset, int endOffset)
    {
        Node clone = node.cloneNode(false);
        switch (node.getNodeType()) {
            case CDATA_NODE:
            case COMMENT_NODE:
            case Node.TEXT_NODE:
                clone.setNodeValue(node.getNodeValue().substring(startOffset, endOffset));
                node.setNodeValue(node.getNodeValue().substring(0, startOffset)
                    + node.getNodeValue().substring(endOffset));
                return clone;
            case Node.ELEMENT_NODE:
                for (int i = startOffset; i < endOffset; i++) {
                    clone.appendChild(node.getChildNodes().getItem(startOffset));
                }
                return clone;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_NODE_TYPE);
        }
    }

    /**
     * @param range a DOM range
     * @return the node that follows after the end point of the given range, in a depth-first pre-order search
     */
    public Node getNextNode(Range range)
    {
        Node node = range.getEndContainer();
        if (node.hasChildNodes() && range.getEndOffset() < node.getChildNodes().getLength()) {
            return node.getChildNodes().getItem(range.getEndOffset());
        }
        while (node != null && node.getNextSibling() == null) {
            node = node.getParentNode();
        }
        return node == null ? null : node.getNextSibling();
    }

    /**
     * @param range a DOM range
     * @return the node that precedes the start point of the given range, in a depth-first pre-order search
     */
    public Node getPreviousNode(Range range)
    {
        Node node = range.getStartContainer();
        if (node.hasChildNodes() && range.getStartOffset() > 0) {
            return node.getChildNodes().getItem(range.getStartOffset() - 1);
        }
        while (node != null && node.getPreviousSibling() == null) {
            node = node.getParentNode();
        }
        return node == null ? null : node.getPreviousSibling();
    }

    /**
     * Makes sure that a given DOM range is visible by scrolling it into view.
     * 
     * @param range a DOM range
     */
    public void scrollIntoView(Range range)
    {
        // Look for an element to scroll into view.
        Node node = getFirstLeaf(range);
        if (node == null) {
            // The range is collapsed between nodes.
            node = getNextLeaf(range);
            if (node == null) {
                // The range is collapsed at the end of the document body.
                node = getPreviousLeaf(range);
                // At this point, node cannot be null: if a range is collapsed at the same time at the end and at the
                // start of the document body then the document is empty and thus the first leaf of the range is the
                // BODY element.
            }
        }
        // The node we have right now might not be displayed. Look for its first displayed ancestor.
        Node lastHiddenAncestor = getLastAncestor(node, new HiddenElements());
        if (lastHiddenAncestor != null) {
            node = lastHiddenAncestor.getParentNode();
            if (node == null) {
                return;
            }
        }
        // At this point we can be sure that the node we have is displayed, but we need an element.
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getParentElement();
        }
        // At this point, we should have an element to scroll into view.
        scrollIntoView((Element) node);
    }

    /**
     * Makes sure that the given DOM element is visible by scrolling it into view.
     * 
     * @param element the element to scroll into view
     */
    public void scrollIntoView(Element element)
    {
        if (element != null) {
            element.scrollIntoView();
        }
    }

    /**
     * Ensures the given block-level element can be edited in design mode. This method is required because most browsers
     * don't allow the caret inside elements that don't have any visible content and thus we cannot edit them otherwise.
     * <p>
     * The default implementation adds a BR element. Overwrite for browsers that don't like it.
     * 
     * @param block a block-level DOM element
     */
    public void ensureBlockIsEditable(Element block)
    {
        if (block.canHaveChildren()) {
            block.appendChild(block.getOwnerDocument().createBRElement());
        }
    }

    /**
     * @param node the node to check for line breaks
     * @return {@code true} if the given node or one of its descendants is a BR (line break), {@code false} otherwise
     */
    public boolean isOrContainsLineBreak(Node node)
    {
        return node != null
            && (BR.equalsIgnoreCase(node.getNodeName()) || (node.getNodeType() == Node.ELEMENT_NODE && Element.as(node)
                .getElementsByTagName(BR).getLength() > 0));
    }

    /**
     * Puts the given document in design mode or in view-only mode.
     * <p>
     * NOTE: The standard implementation of this method sets the {@link #CONTENT_EDITABLE_ATTR} attribute on the body
     * element because otherwise read-only regions, marked with {@code contentEditable=false}, would be ignored. Browser
     * specific implementations might use document's {@code designMode} property instead.
     * 
     * @param document a DOM document
     * @param designMode {@code true} to enter design mode, {@code false} to go back to view-only mode
     */
    public void setDesignMode(Document document, boolean designMode)
    {
        document.getBody().setAttribute(CONTENT_EDITABLE_ATTR, String.valueOf(designMode));
    }

    /**
     * @param document a DOM document
     * @return {@code true} if the given document is in design mode, {@code false} otherwise
     * @see #setDesignMode(Document, boolean)
     */
    public boolean isDesignMode(Document document)
    {
        return Boolean.valueOf(document.getBody().getAttribute(CONTENT_EDITABLE_ATTR));
    }

    /**
     * Removes a property from an element.
     * 
     * @param element a DOM element
     * @param propertyName the name of the property to be removed
     */
    public void removeProperty(Element element, String propertyName)
    {
        ((JavaScriptObject) element.cast()).remove(propertyName);
    }

    /**
     * @param node a HTML DOM node
     * @return {@code true} if the given node can have children, following the HTML strict DTD, {@code false} otherwise
     */
    public boolean canHaveChildren(Node node)
    {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        String tagName = node.getNodeName().toLowerCase();
        for (int i = 0; i < DOMUtils.HTML_EMPTY_TAGS.length; i++) {
            if (DOMUtils.HTML_EMPTY_TAGS[i].equals(tagName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param element a DOM element
     * @param attributeName the name of an attribute
     * @return {@code true} if the given element has the specified attribute, {@code false} otherwise
     */
    public boolean hasAttribute(Element element, String attributeName)
    {
        return element.hasAttribute(attributeName);
    }

    /**
     * Removes an attribute by name.
     * 
     * @param element a DOM element
     * @param attributeName the name of the attribute to remove
     */
    public void removeAttribute(Element element, String attributeName)
    {
        element.removeAttribute(attributeName);
    }

    /**
     * Isolates a node from its siblings. Previous siblings are moved in a clone of their parent placed before their
     * parent. Next siblings are moved in a clone of their parent placed after their parent. As an example, isolating
     * the {@code em} from
     * <br>{@code <ins>a<em>b</em>c</ins>}
     * <br>results in
     * <br>{@code <ins>a</ins><ins><em>b</em></ins><ins>c</ins>}.
     * 
     * @param node the node to isolate
     */
    public void isolate(Node node)
    {
        Node parent = node.getParentNode();
        if (parent == null) {
            return;
        }

        Node grandParent = parent.getParentNode();
        if (grandParent == null) {
            return;
        }

        // Isolate from previous siblings.
        if (node.getPreviousSibling() != null) {
            Node leftClone = parent.cloneNode(false);
            ((Element) leftClone).removeAttribute(ID);
            Node leftSibling = node.getPreviousSibling();
            leftClone.appendChild(leftSibling);
            leftSibling = node.getPreviousSibling();
            while (leftSibling != null) {
                leftClone.insertBefore(leftSibling, leftClone.getFirstChild());
                leftSibling = node.getPreviousSibling();
            }
            grandParent.insertBefore(leftClone, parent);
        }

        // Isolate from next siblings.
        if (node.getNextSibling() != null) {
            Node rightClone = parent.cloneNode(false);
            ((Element) rightClone).removeAttribute(ID);
            Node rightSibling = node.getNextSibling();
            while (rightSibling != null) {
                rightClone.appendChild(rightSibling);
                rightSibling = node.getNextSibling();
            }
            if (parent.getNextSibling() != null) {
                grandParent.insertBefore(rightClone, parent.getNextSibling());
            } else {
                grandParent.appendChild(rightClone);
            }
        }
    }
}
