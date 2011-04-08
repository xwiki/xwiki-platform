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
package org.xwiki.gwt.dom.client.internal.mozilla;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.dom.client.Node;

/**
 * The native range implementation for browsers that follow the W3C Range specification.
 * 
 * @version $Id$
 */
public final class NativeRange extends JavaScriptObject
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected NativeRange()
    {
    }

    /**
     * Creates a new native range using the given document.
     * 
     * @param document the document to use for creating the new native range
     * @return the newly created native range
     */
    public static native NativeRange newInstance(Document document)
    /*-{
        return document.createRange();
    }-*/;

    /**
     * @return true if this range is collapsed
     */
    public native boolean isCollapsed()
    /*-{
        return this.collapsed;
    }-*/;

    /**
     * @return the deepest common ancestor container of this range's two boundary-points
     */
    public native Node getCommonAncestorContainer()
    /*-{
        return this.commonAncestorContainer;
    }-*/;

    /**
     * @return the node within which this range ends
     */
    public native Node getEndContainer()
    /*-{
        return this.endContainer;
    }-*/;

    /**
     * @return the offset within the ending node of this range
     */
    public native int getEndOffset()
    /*-{
        return this.endOffset;
    }-*/;

    /**
     * @return the node within which this range begins
     */
    public native Node getStartContainer()
    /*-{
        return this.startContainer;
    }-*/;

    /**
     * @return the offset within the starting node of this range
     */
    public native int getStartOffset()
    /*-{
        return this.startOffset;
    }-*/;

    /**
     * Sets the attributes describing the start of this range.
     * 
     * @param refNode the {@link #startContainer} value. This parameter must be different from null.
     * @param offset the {@link #startOffset} value
     */
    public native void setStart(Node refNode, int offset)
    /*-{
        this.setStart(refNode, offset);
    }-*/;

    /**
     * Sets the attributes describing the end of this range.
     * 
     * @param refNode the {@link #endContainer} value. This parameter must be different from null.
     * @param offset the {@link #endOffset} value
     */
    public native void setEnd(Node refNode, int offset)
    /*-{
        this.setEnd(refNode, offset);
    }-*/;

    /**
     * Sets the start position to be before the given node.
     * 
     * @param refNode the reference node, before which this range will start
     */
    public native void setStartBefore(Node refNode)
    /*-{
        this.setStartBefore(refNode);
    }-*/;

    /**
     * Sets the start position to be after the given node.
     * 
     * @param refNode the reference node, after which this range will start
     */
    public native void setStartAfter(Node refNode)
    /*-{
        this.setStartAfter(refNode);
    }-*/;

    /**
     * Sets the end position to be before the given node.
     * 
     * @param refNode the reference node, before which this range will end
     */
    public native void setEndBefore(Node refNode)
    /*-{
        this.setEndBefore(refNode);
    }-*/;

    /**
     * Sets the end of this Range to be after the given node.
     * 
     * @param refNode the reference node, after which this range will end
     */
    public native void setEndAfter(Node refNode)
    /*-{
        this.setEndAfter(refNode);
    }-*/;

    /**
     * Select a node and its contents.
     * 
     * @param refNode the node to select
     */
    public native void selectNode(Node refNode)
    /*-{
        this.selectNode(refNode);
    }-*/;

    /**
     * Select the contents within a node.
     * 
     * @param refNode the node to select from
     */
    public native void selectNodeContents(Node refNode)
    /*-{
        this.selectNodeContents(refNode);
    }-*/;

    /**
     * Collapse this range onto one of its boundary-points.
     * 
     * @param toStart if true, collapses this range onto its start; if false, collapses it onto its end.
     */
    public native void collapse(boolean toStart)
    /*-{
        this.collapse(toStart);
    }-*/;

    /**
     * Duplicates the contents of this range.
     * 
     * @return a DocumentFragment that contains content equivalent to this range
     */
    public native DocumentFragment cloneContents()
    /*-{
        return this.cloneContents();
    }-*/;

    /**
     * Removes the contents of this range from the containing document or document fragment without returning a
     * reference to the removed content.
     */
    public native void deleteContents()
    /*-{
        this.deleteContents();
    }-*/;

    /**
     * Moves the contents of this range from the containing document or document fragment to a new DocumentFragment.
     * 
     * @return a DocumentFragment containing the extracted contents
     */
    public native DocumentFragment extractContents()
    /*-{
        return this.extractContents();
    }-*/;

    /**
     * Inserts a node into the Document or DocumentFragment at the start of the Range. If the container is a Text node,
     * this will be split at the start of the Range (as if the Text node's splitText method was performed at the
     * insertion point) and the insertion will occur between the two resulting Text nodes. Adjacent Text nodes will not
     * be automatically merged. If the node to be inserted is a DocumentFragment node, the children will be inserted
     * rather than the DocumentFragment node itself.
     * 
     * @param newNode the node to insert at the start of this range.
     */
    public native void insertNode(Node newNode)
    /*-{
        this.insertNode(newNode);
    }-*/;

    /**
     * Re-parents the contents of this range to the given node and inserts the node at the position of the start of this
     * range.
     * 
     * @param newParent the node to surround the contents with
     */
    public native void surroundContents(Node newParent)
    /*-{
        this.surroundContents(newParent);
    }-*/;

    /**
     * Compare the boundary-points of two ranges in a document.
     * 
     * @param how the type of comparison
     * @param sourceRange the range to compared to
     * @return -1, 0 or 1 depending on whether the corresponding boundary-point of this range is respectively before,
     *         equal to, or after the corresponding boundary-point of sourceRange
     */
    public native short compareBoundaryPoints(int how, NativeRange sourceRange)
    /*-{
        return this.compareBoundaryPoints(how, sourceRange);
    }-*/;

    /**
     * Produces a new range whose boundary-points are equal to the boundary-points of this range.
     * 
     * @return the duplicated range
     */
    public native NativeRange cloneRange()
    /*-{
        return this.cloneRange();
    }-*/;

    /**
     * Called to indicate that this range is no longer in use and that the implementation may relinquish any resources
     * associated with this range.
     */
    public native void detach()
    /*-{
        this.detach();
    }-*/;
}
