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
 * A contiguous fragment of a {@link Document} or {@link DocumentFragment} that can contain nodes and parts of text
 * nodes.
 * 
 * @version $Id$
 * @see "http://www.w3.org/TR/DOM-Level-2-Traversal-Range/ranges.html"
 */
public interface Range
{
    /**
     * Duplicates the contents of this range.
     * 
     * @return a DocumentFragment that contains content equivalent to this range
     */
    DocumentFragment cloneContents();

    /**
     * Produces a new Range whose boundary-points are equal to the boundary-points of this range.
     * 
     * @return the duplicated range
     */
    Range cloneRange();

    /**
     * Collapse this range onto one of its boundary-points.
     * 
     * @param toStart if true, collapses this range onto its start; if false, collapses it onto its end.
     */
    void collapse(boolean toStart);

    /**
     * Compare the boundary-points of two Ranges in a document.
     * 
     * @param how the type of comparison
     * @param sourceRange the range to compared to
     * @return -1, 0 or 1 depending on whether the corresponding boundary-point of this range is respectively before,
     *         equal to, or after the corresponding boundary-point of sourceRange
     */
    short compareBoundaryPoints(RangeCompare how, Range sourceRange);

    /**
     * Removes the contents of this range from the containing document or document fragment without returning a
     * reference to the removed content.
     */
    void deleteContents();

    /**
     * Called to indicate that this range is no longer in use and that the implementation may relinquish any resources
     * associated with this range.
     */
    void detach();

    /**
     * Moves the contents of this range from the containing document or document fragment to a new DocumentFragment.
     * 
     * @return a DocumentFragment containing the extracted contents
     */
    DocumentFragment extractContents();

    /**
     * @return the deepest common ancestor container of this range's two boundary-points
     */
    Node getCommonAncestorContainer();

    /**
     * @return the node within which this range ends
     */
    Node getEndContainer();

    /**
     * @return the offset within the ending node of this range
     */
    int getEndOffset();

    /**
     * @return the node within which this range begins
     */
    Node getStartContainer();

    /**
     * @return the offset within the starting node of this range
     */
    int getStartOffset();

    /**
     * Inserts a node into the Document or DocumentFragment at the start of the Range. If the container is a Text node,
     * this will be split at the start of the Range (as if the Text node's splitText method was performed at the
     * insertion point) and the insertion will occur between the two resulting Text nodes. Adjacent Text nodes will not
     * be automatically merged. If the node to be inserted is a DocumentFragment node, the children will be inserted
     * rather than the DocumentFragment node itself.
     * 
     * @param newNode the node to insert at the start of this range.
     */
    void insertNode(Node newNode);

    /**
     * @return true if this range is collapsed
     */
    boolean isCollapsed();

    /**
     * Select a node and its contents.
     * 
     * @param refNode the node to select
     */
    void selectNode(Node refNode);

    /**
     * Select the contents within a node.
     * 
     * @param refNode the node to select from
     */
    void selectNodeContents(Node refNode);

    /**
     * Sets the attributes describing the end of this range.
     * 
     * @param refNode the {@link #getEndContainer()} value. This parameter must be different from null.
     * @param offset the {@link #getEndOffset()} value
     */
    void setEnd(Node refNode, int offset);

    /**
     * Sets the end of this Range to be after the given node.
     * 
     * @param refNode the reference node, after which this range will end
     */
    void setEndAfter(Node refNode);

    /**
     * Sets the end position to be before the given node.
     * 
     * @param refNode the reference node, before which this range will end
     */
    void setEndBefore(Node refNode);

    /**
     * Sets the attributes describing the start of this range.
     * 
     * @param refNode the {@link #getStartContainer()} value. This parameter must be different from null.
     * @param offset the {@link #getStartOffset()} value
     */
    void setStart(Node refNode, int offset);

    /**
     * Sets the start position to be after the given node.
     * 
     * @param refNode the reference node, after which this range will start
     */
    void setStartAfter(Node refNode);

    /**
     * Sets the start position to be before the given node.
     * 
     * @param refNode the reference node, before which this range will start
     */
    void setStartBefore(Node refNode);

    /**
     * Re-parents the contents of this range to the given node and inserts the node at the position of the start of this
     * range.
     * 
     * @param newParent the node to surround the contents with
     */
    void surroundContents(Node newParent);

    /**
     * @return the HTML contents of this range
     */
    String toHTML();

    /**
     * Returns the contents of this range as a string. This string contains only the data characters, not any mark-up.
     * 
     * @return the contents of this range
     */
    String toString();
}
