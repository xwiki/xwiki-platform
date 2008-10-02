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

public final class DefaultSelection extends AbstractSelection
{
    DefaultSelection(JavaScriptObject jsSelection)
    {
        super(jsSelection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#addRange(JavaScriptObject)
     */
    protected native void addRange(JavaScriptObject range) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().addRange(range);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapse(Node, int)
     */
    public native void collapse(Node parentNode, int offset) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().collapse(parentNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapseToEnd()
     */
    public native void collapseToEnd() /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().collapseToEnd();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapseToStart()
     */
    public native void collapseToStart() /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().collapseToStart();
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
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().deleteFromDocument();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#extend(Node, int)
     */
    public native void extend(Node parentNode, int offset) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().extend(parentNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getAnchorNode()
     */
    public native Node getAnchorNode() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().anchorNode;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getAnchorOffset()
     */
    public native int getAnchorOffset() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().anchorOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getFocusNode()
     */
    public native Node getFocusNode() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().focusNode;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getFocusOffset()
     */
    public native int getFocusOffset() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().focusOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getRangeAt(int)
     */
    public Range getRangeAt(int index)
    {
        return new DefaultRange(getJSRangeAt(index));
    }

    protected native JavaScriptObject getJSRangeAt(int index) /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().getRangeAt(index);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getRangeCount()
     */
    public native int getRangeCount() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().rangeCount;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#isCollapsed()
     */
    public native boolean isCollapsed() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().isCollapsed;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#removeAllRanges()
     */
    public native void removeAllRanges() /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().removeAllRanges();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#removeRange(JavaScriptObject)
     */
    protected native void removeRange(JavaScriptObject range) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().removeRange(range);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#selectAllChildren(Node)
     */
    public native void selectAllChildren(Node parentNode) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().selectAllChildren(parentNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#selectionLanguageChange(boolean)
     */
    public native void selectionLanguageChange(boolean langRTL) /*-{
        this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().selectionLanguageChange(langRTL);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see Selection#toString()
     */
    public native String toString() /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.selection.internal.AbstractSelection::getJSSelection()().toString();
    }-*/;
}
