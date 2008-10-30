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

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.ControlRange;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.NativeRange;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.NativeSelection;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.TextRange;

/**
 * The implementation of Mozilla's selection specification using Internet Explorer's selection API.
 * 
 * @version $Id$
 */
public final class IESelection extends AbstractSelection<NativeSelection, NativeRange>
{
    /**
     * Selection types supported by Internet Explorer.
     */
    public static enum SelectionType
    {
        /**
         * No selection or insertion point.
         */
        NONE,
        /**
         * Specifies a text selection or the insertion point.
         */
        TEXT,
        /**
         * Specifies a control selection, which enables dimension controls allowing the selected object to be resized.
         */
        CONTROL
    }

    /**
     * Creates a new instance that wraps the given native selection object. This object will be used to implement
     * Mozilla's selection specification.
     * 
     * @param jsSelection The native selection object to be wrapped.
     */
    IESelection(NativeSelection jsSelection)
    {
        super(jsSelection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#addRange(Range)
     */
    public void addRange(Range range)
    {
        addRange(IERangeFactory.cast(range).getJSRange());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#addRange(com.google.gwt.core.client.JavaScriptObject)
     */
    protected void addRange(NativeRange range)
    {
        range.select();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#collapse(Node, int)
     */
    public void collapse(Node parentNode, int offset)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#collapseToEnd()
     */
    public void collapseToEnd()
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#collapseToStart()
     */
    public void collapseToStart()
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#containsNode(Node, boolean)
     */
    public boolean containsNode(Node node, boolean partlyContained)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#deleteFromDocument()
     */
    public void deleteFromDocument()
    {
        getJSSelection().clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#extend(Node, int)
     */
    public void extend(Node parentNode, int offset)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getAnchorNode()
     */
    public Node getAnchorNode()
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getAnchorOffset()
     */
    public int getAnchorOffset()
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getFocusNode()
     */
    public Node getFocusNode()
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getFocusOffset()
     */
    public int getFocusOffset()
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getRangeAt(int)
     */
    public Range getRangeAt(int index)
    {
        if (index == 0) {
            if (SelectionType.CONTROL.toString().equalsIgnoreCase(getJSSelection().getType())) {
                return IERangeFactory.createRange((ControlRange) getJSSelection().createRange());
            } else {
                return IERangeFactory.createRange((TextRange) getJSSelection().createRange());
            }
        } else {
            throw new IndexOutOfBoundsException(
                "Internet Explorer doesn't support multiple selection. Expected index is 0, was " + index + "!");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getRangeCount()
     */
    public int getRangeCount()
    {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#isCollapsed()
     */
    public boolean isCollapsed()
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#removeAllRanges()
     */
    public void removeAllRanges()
    {
        getJSSelection().empty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#removeRange(Range)
     */
    public void removeRange(Range range)
    {
        removeRange(IERangeFactory.cast(range).getJSRange());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#removeRange(com.google.gwt.core.client.JavaScriptObject)
     */
    protected void removeRange(NativeRange range)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#selectAllChildren(Node)
     */
    public void selectAllChildren(Node parentNode)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#selectionLanguageChange(boolean)
     */
    public void selectionLanguageChange(boolean langRTL)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#toString()
     */
    public String toString()
    {
        return getRangeAt(0).toString();
    }
}
