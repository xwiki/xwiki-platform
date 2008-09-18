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
import com.xpn.xwiki.wysiwyg.client.selection.Selection;

public class DefaultSelection extends JavaScriptObject implements Selection
{
    /**
     * {@inheritDoc}
     * 
     * @see Selection#addRange(Range)
     */
    public native void addRange(Range range) /*-{
        this.addRange(range);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapse(Node, long)
     */
    public native void collapse(Node parentNode, long offset) /*-{
        this.collapse(parentNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapseToEnd()
     */
    public native void collapseToEnd() /*-{
        this.collapseToEnd();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapseToStart()
     */
    public native void collapseToStart() /*-{
        this.collapseToStart();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#containsNode(Node, boolean)
     */
    public native boolean containsNode(Node node, boolean partlyContained) /*-{
        return containsNode(node, partlyContained);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#deleteFromDocument()
     */
    public native void deleteFromDocument() /*-{
        this.deleteFromDocument();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#extend(Node, long)
     */
    public native void extend(Node parentNode, long offset) /*-{
        this.extend(parentNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getAnchorNode()
     */
    public native Node getAnchorNode() /*-{
        return this.anchorNode;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getAnchorOffset()
     */
    public native long getAnchorOffset() /*-{
        return this.anchorOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getFocusNode()
     */
    public native Node getFocusNode() /*-{
        return this.focusNode;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getFocusOffset()
     */
    public native long getFocusOffset() /*-{
        return this.focusOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getRangeAt(long)
     */
    public native Range getRangeAt(long index) /*-{
        return this.getRangeAt(index);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getRangeCount()
     */
    public native long getRangeCount() /*-{
        return this.rangeCount;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#isCollapsed()
     */
    public native boolean isCollapsed() /*-{
        return this.isCollapsed;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#removeAllRanges()
     */
    public native void removeAllRanges() /*-{
        this.removeAllRanges();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#removeRange(Range)
     */
    public native void removeRange(Range range) /*-{
        this.removeRange(range);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#selectAllChildren(Node)
     */
    public native void selectAllChildren(Node parentNode) /*-{
        this.selectAllChildren(parentNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#selectionLanguageChange(boolean)
     */
    public native void selectionLanguageChange(boolean langRTL) /*-{
        this.selectionLanguageChange(langRTL);
    }-*/;
}
