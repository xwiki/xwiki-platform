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
package com.xpn.xwiki.wysiwyg.client.dom.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Range;

/**
 * The default selection implementation just forwards the calls to the underlying browser implementation. It should be
 * used only for browsers that follow Mozilla's selection specification.
 * 
 * @version $Id$
 */
public class DefaultSelection extends AbstractSelection<JavaScriptObject, JavaScriptObject>
{
    /**
     * Creates a new instance that wraps the given native selection object. All the calls will be forwarded to this
     * JavaScript object.
     * 
     * @param jsSelection The native selection object to be wrapped.
     */
    DefaultSelection(JavaScriptObject jsSelection)
    {
        super(jsSelection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#addRange(JavaScriptObject)
     */
    protected native void addRange(JavaScriptObject range)
    /*-{
        this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().addRange(range);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#collapse(Node, int)
     */
    public native void collapse(Node parentNode, int offset)
    /*-{
        var range = this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()();
        range.collapse(parentNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#collapseToEnd()
     */
    public native void collapseToEnd()
    /*-{
        this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().collapseToEnd();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#collapseToStart()
     */
    public native void collapseToStart()
    /*-{
        this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().collapseToStart();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#containsNode(Node, boolean)
     */
    public native boolean containsNode(Node node, boolean partlyContained)
    /*-{
        return containsNode(node, partlyContained);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#deleteFromDocument()
     */
    public native void deleteFromDocument()
    /*-{
        var range = this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()();
        range.deleteFromDocument();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#extend(Node, int)
     */
    public native void extend(Node parentNode, int offset)
    /*-{
        var range = this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()();
        range.extend(parentNode, offset);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getAnchorNode()
     */
    public native Node getAnchorNode()
    /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().anchorNode;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getAnchorOffset()
     */
    public native int getAnchorOffset()
    /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().anchorOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getFocusNode()
     */
    public native Node getFocusNode()
    /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().focusNode;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getFocusOffset()
     */
    public native int getFocusOffset()
    /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().focusOffset;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getRangeAt(int)
     */
    public Range getRangeAt(int index)
    {
        return new DefaultRange(getJSRangeAt(index));
    }

    /**
     * @param index The index of the range to retrieve. Usually the selection contains just one range.
     * @return The JavaScript range at the specified index in the JavaScript selection object wrapped by this object.
     */
    protected native JavaScriptObject getJSRangeAt(int index)
    /*-{
        var range = this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()();
        return range.getRangeAt(index);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getRangeCount()
     */
    public native int getRangeCount()
    /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().rangeCount;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#isCollapsed()
     */
    public native boolean isCollapsed()
    /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().isCollapsed;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#removeAllRanges()
     */
    public native void removeAllRanges()
    /*-{
        this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().removeAllRanges();
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#removeRange(JavaScriptObject)
     */
    protected native void removeRange(JavaScriptObject range)
    /*-{
        this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().removeRange(range);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#selectAllChildren(Node)
     */
    public native void selectAllChildren(Node parentNode)
    /*-{
        var range = this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()();
        range.selectAllChildren(parentNode);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#selectionLanguageChange(boolean)
     */
    public native void selectionLanguageChange(boolean langRTL)
    /*-{
        var range = this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()();
        range.selectionLanguageChange(langRTL);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#toString()
     */
    public native String toString()
    /*-{
        return this.@com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection::getJSSelection()().toString();
    }-*/;
}
