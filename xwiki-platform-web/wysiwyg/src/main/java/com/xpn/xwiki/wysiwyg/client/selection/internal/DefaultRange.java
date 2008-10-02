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
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.util.DocumentFragment;

public class DefaultRange extends AbstractRange
{
    DefaultRange(JavaScriptObject jsRange)
    {
        super(jsRange);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#isCollapsed()
     */
    public native boolean isCollapsed() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().collapsed;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getCommonAncestorContainer()
     */
    public native Node getCommonAncestorContainer() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().commonAncestorContainer;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndContainer()
     */
    public native Node getEndContainer() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().endContainer;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndOffset()
     */
    public native int getEndOffset() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().endOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartContainer()
     */
    public native Node getStartContainer() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().startContainer;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartOffset()
     */
    public native int getStartOffset() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().startOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStart(Node, int)
     */
    public native void setStart(Node refNode, int offset) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().setStart(refNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEnd(Node, int)
     */
    public native void setEnd(Node refNode, int offset) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().setEnd(refNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartBefore(Node)
     */
    public native void setStartBefore(Node refNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().setStartBefore(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartAfter(Node)
     */
    public native void setStartAfter(Node refNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().setStartAfter(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndBefore(Node)
     */
    public native void setEndBefore(Node refNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().setEndBefore(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndAfter(Node)
     */
    public native void setEndAfter(Node refNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().setEndAfter(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNode(Node)
     */
    public native void selectNode(Node refNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().selectNode(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNodeContents(Node)
     */
    public native void selectNodeContents(Node refNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().selectNodeContents(refNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#collapse(boolean)
     */
    public native void collapse(boolean toStart) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().collapse(toStart);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneContents()
     */
    public native DocumentFragment cloneContents() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().cloneContents();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#deleteContents()
     */
    public native void deleteContents() /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().deleteContents();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#extractContents()
     */
    public native DocumentFragment extractContents() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().extractContents();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#insertNode(Node)
     */
    public native void insertNode(Node newNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().insertNode(newNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#surroundContents(Node)
     */
    public native void surroundContents(Node newParent) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().surroundContents(newParent);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRange#compareBoundaryPoints(int, JavaScriptObject)
     */
    protected native short compareBoundaryPoints(int how, JavaScriptObject sourceRange) /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().compareBoundaryPoints(how, sourceRange);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneRange()
     */
    public Range cloneRange()
    {
        return new DefaultRange(cloneJSRange());
    }

    protected native JavaScriptObject cloneJSRange() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().cloneRange();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#detach()
     */
    public native void detach() /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().detach();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Range#toString()
     */
    public native String toString() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractRange::getJSRange()().toString();
    }-*/;
}
