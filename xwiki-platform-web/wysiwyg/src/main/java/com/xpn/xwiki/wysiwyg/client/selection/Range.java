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
package com.xpn.xwiki.wysiwyg.client.selection;

import com.google.gwt.dom.client.Node;

public interface Range
{
    /**
     * @return Node within which the Range begins.
     */
    Node getStartContainer();

    /**
     * @return Offset within the starting node of the Range.
     */
    int getStartOffset();

    /**
     * @return Node within which the Range ends.
     */
    Node getEndContainer();

    /**
     * @return Offset within the ending node of the Range.
     */
    int getEndOffset();

    /**
     * @return true if the Range is collapsed.
     */
    boolean isCollapsed();

    /**
     * @return The deepest common ancestor container of the Range's two boundary-points.
     */
    Node getCommonAncestorContainer();

    /**
     * Sets the attributes describing the start of the Range.
     * 
     * @param refNode The refNode value. This parameter must be different from null.
     * @param offset The startOffset value.
     */
    void setStart(Node refNode, int offset);

    /**
     * Sets the attributes describing the end of a Range.
     * 
     * @param refNode The refNode value. This parameter must be different from null.
     * @param offset The endOffset value.
     */
    void setEnd(Node refNode, int offset);

    /**
     * Sets the start position to be before a node.
     * 
     * @param refNode Range starts before refNode.
     */
    void setStartBefore(Node refNode);

    /**
     * Sets the start position to be after a node.
     * 
     * @param refNode Range starts after refNode.
     */
    void setStartAfter(Node refNode);

    /**
     * Sets the end position to be before a node.
     * 
     * @param refNode Range ends before refNode.
     */
    void setEndBefore(Node refNode);

    /**
     * Sets the end of a Range to be after a node.
     * 
     * @param refNode Range ends after refNode.
     */
    void setEndAfter(Node refNode);

    /**
     * Collapse a Range onto one of its boundary-points.
     * 
     * @param toStart If true, collapses the Range onto its start; if false, collapses it onto its end.
     */
    void collapse(boolean toStart);

    /**
     * Select a node and its contents.
     * 
     * @param refNode The node to select.
     */
    void selectNode(Node refNode);

    /**
     * Select the contents within a node.
     * 
     * @param refNode Node to select from.
     */
    void selectNodeContents(Node refNode);

    /**
     * Compare the boundary-points of two Ranges in a document.
     * 
     * @param how The type of comparison.
     * @param sourceRange The Range on which this current Range is compared to.
     * @return -1, 0 or 1 depending on whether the corresponding boundary-point of the Range is respectively before,
     *         equal to, or after the corresponding boundary-point of sourceRange.
     */
    short compareBoundaryPoints(RangeCompare how, Range sourceRange);

    /**
     * Removes the contents of a Range from the containing document or document fragment without returning a reference
     * to the removed content.
     */
    void deleteContents();

    /**
     * Moves the contents of a Range from the containing document or document fragment to a new DocumentFragment.
     * 
     * @return A DocumentFragment containing the extracted contents.
     */
    DocumentFragment extractContents();

    /**
     * Duplicates the contents of a Range.
     * 
     * @return A DocumentFragment that contains content equivalent to this Range.
     */
    DocumentFragment cloneContents();

    /**
     * Inserts a node into the Document or DocumentFragment at the start of the Range. If the container is a Text node,
     * this will be split at the start of the Range (as if the Text node's splitText method was performed at the
     * insertion point) and the insertion will occur between the two resulting Text nodes. Adjacent Text nodes will not
     * be automatically merged. If the node to be inserted is a DocumentFragment node, the children will be inserted
     * rather than the DocumentFragment node itself.
     * 
     * @param newNode The node to insert at the start of the Range.
     */
    void insertNode(Node newNode);

    /**
     * Re-parents the contents of the Range to the given node and inserts the node at the position of the start of the
     * Range.
     * 
     * @param newParent The node to surround the contents with.
     */
    void surroundContents(Node newParent);

    /**
     * Produces a new Range whose boundary-points are equal to the boundary-points of the Range.
     * 
     * @return The duplicated Range.
     */
    Range cloneRange();

    /**
     * Returns the contents of a Range as a string. This string contains only the data characters, not any mark-up.
     * 
     * @return The contents of the Range.
     */
    String toString();

    /**
     * Called to indicate that the Range is no longer in use and that the implementation may relinquish any resources
     * associated with this Range.
     */
    void detach();
}
