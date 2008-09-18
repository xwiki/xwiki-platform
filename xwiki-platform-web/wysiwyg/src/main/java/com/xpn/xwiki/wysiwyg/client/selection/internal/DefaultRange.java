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
package com.xpn.xwiki.wysiwyg.client.selection.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.google.gwt.xml.client.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.RangeCompare;

public class DefaultRange extends JavaScriptObject implements Range
{
    /**
     * {@inheritDoc}
     * 
     * @see Range#isCollapsed()
     */
    public native boolean isCollapsed() /*-{
        return this.collapsed;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getCommonAncestorContainer()
     */
    public native Node getCommonAncestorContainer() /*-{
        return this.commonAncestorContainer;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndContainer()
     */
    public native Node getEndContainer() /*-{
        return this.endContainer;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndOffset()
     */
    public native long getEndOffset() /*-{
        return this.endOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartContainer()
     */
    public native Node getStartContainer() /*-{
        return this.startContainer;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartOffset()
     */
    public native long getStartOffset() /*-{
        return this.startOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStart(Node, long)
     */
    public native void setStart(Node refNode, long offset) /*-{
        this.setStart(refNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEnd(Node, long)
     */
    public native void setEnd(Node refNode, long offset) /*-{
        this.setEnd(refNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartBefore(Node)
     */
    public native void setStartBefore(Node refNode) /*-{
        this.setStartBefore(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartAfter(Node)
     */
    public native void setStartAfter(Node refNode) /*-{
        this.setStartAfter(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndBefore(Node)
     */
    public native void setEndBefore(Node refNode) /*-{
        this.setEndBefore(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndAfter(Node)
     */
    public native void setEndAfter(Node refNode) /*-{
        this.setEndAfter(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNode(Node)
     */
    public native void selectNode(Node refNode) /*-{
        this.selectNode(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNodeContents(Node)
     */
    public native void selectNodeContents(Node refNode) /*-{
        this.selectNodeContents(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#collapse(boolean)
     */
    public native void collapse(boolean toStart) /*-{
        this.collapse(toStart);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneContents()
     */
    public native DocumentFragment cloneContents() /*-{
        return this.cloneContents();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#deleteContents()
     */
    public native void deleteContents() /*-{
        this.deleteContents();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#extractContents()
     */
    public native DocumentFragment extractContents() /*-{
        return this.extractContents();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#insertNode(Node)
     */
    public native void insertNode(Node newNode) /*-{
        this.insertNode(newNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#surroundContents(Node)
     */
    public native void surroundContents(Node newParent) /*-{
        this.surroundContents(newParent);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#compareBoundaryPoints(RangeCompare, Range)
     */
    public short compareBoundaryPoints(RangeCompare how, Range sourceRange)
    {
        return compareBoundaryPoints(how.ordinal(), sourceRange);
    }

    private native short compareBoundaryPoints(int how, Range sourceRange) /*-{
        return this.compareBoundaryPoints(how, sourceRange);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneRange()
     */
    public native Range cloneRange() /*-{
        return this.cloneRange();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#detach()
     */
    public native void detach() /*-{
        this.detach();
    }-*/;
}
