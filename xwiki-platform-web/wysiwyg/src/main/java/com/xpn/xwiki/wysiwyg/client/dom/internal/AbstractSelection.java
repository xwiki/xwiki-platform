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
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;

/**
 * Abstract selection that implements the Mozilla range specification using the API offered by the browser. Concrete
 * extensions of these class have the role to adapt the specific selection API offered by each browser to the Mozilla
 * selection specification.
 * 
 * @param <S> Browser specific selection implementation.
 * @param <R> Browser specific range implementation.
 * @version $Id$
 */
public abstract class AbstractSelection<S extends JavaScriptObject, R extends JavaScriptObject> implements Selection
{
    /**
     * Browser specific selection implementation.
     */
    private final S jsSelection;

    /**
     * Creates a new instance that has to adapt the given browser-specific selection to the Mozilla specification.
     * 
     * @param jsSelection The selection implementation to adapt.
     */
    AbstractSelection(S jsSelection)
    {
        this.jsSelection = jsSelection;
    }

    /**
     * @return The underlying selection implementation used.
     */
    public final S getJSSelection()
    {
        return this.jsSelection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#addRange(Range)
     */
    @SuppressWarnings("unchecked")
    public void addRange(Range range)
    {
        addRange(((AbstractRange<R>) range).getJSRange());
    }

    /**
     * @param range Adds this range to the current selection.
     */
    protected abstract void addRange(R range);

    /**
     * {@inheritDoc}
     * 
     * @see Selection#removeRange(Range)
     */
    @SuppressWarnings("unchecked")
    public void removeRange(Range range)
    {
        removeRange(((AbstractRange<R>) range).getJSRange());
    }

    /**
     * @param range Removes this range from the current selection.
     */
    protected abstract void removeRange(R range);

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapse(Node, int)
     */
    public void collapse(Node parentNode, int offset)
    {
        Range range = ((Document) parentNode.getOwnerDocument()).createRange();
        range.setStart(parentNode, offset);
        range.setEnd(parentNode, offset);
        removeAllRanges();
        addRange(range);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapseToEnd()
     */
    public void collapseToEnd()
    {
        // NOTE: We should collapse to the focus node, but since we don't know which is the focus node and which is the
        // anchor node we collapse to the end point of the first range.
        collapse(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#collapseToStart()
     */
    public void collapseToStart()
    {
        // NOTE: We should collapse to the anchor node, but since we don't know which is the focus node and which is the
        // anchor node we collapse to the start point of the first range.
        collapse(true);
    }

    /**
     * Collapses this selection to the specified end point.
     * 
     * @param toStart Whether to collapse to the start or to the end point of the first range in this selection.
     */
    private void collapse(boolean toStart)
    {
        if (getRangeCount() > 0) {
            Range range = getRangeAt(0);
            range.collapse(toStart);
            removeAllRanges();
            addRange(range);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#deleteFromDocument()
     */
    public void deleteFromDocument()
    {
        if (getRangeCount() > 0) {
            Range range = getRangeAt(0);
            range.deleteContents();
            removeAllRanges();
            addRange(range);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getAnchorNode()
     */
    public Node getAnchorNode()
    {
        // NOTE: We should return the anchor node, but since we don't know which is the focus node and which is the
        // anchor node we return the start point of the first range.
        if (getRangeCount() > 0) {
            return getRangeAt(0).getStartContainer();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getAnchorOffset()
     */
    public int getAnchorOffset()
    {
        // NOTE: We should return the anchor offset, but since we don't know which is the focus node and which is the
        // anchor node we return the start offset of the first range.
        if (getRangeCount() > 0) {
            return getRangeAt(0).getStartOffset();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getFocusNode()
     */
    public Node getFocusNode()
    {
        // NOTE: We should return the focus node, but since we don't know which is the focus node and which is the
        // anchor node we return the end point of the first range.
        if (getRangeCount() > 0) {
            return getRangeAt(0).getEndContainer();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#getFocusOffset()
     */
    public int getFocusOffset()
    {
        // NOTE: We should return the focus offset, but since we don't know which is the focus node and which is the
        // anchor node we return the end offset of the first range.
        if (getRangeCount() > 0) {
            return getRangeAt(0).getEndOffset();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#isCollapsed()
     */
    public boolean isCollapsed()
    {
        return getRangeCount() == 1 && getRangeAt(0).isCollapsed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#toString()
     */
    public String toString()
    {
        if (getRangeCount() > 0) {
            return getRangeAt(0).toString();
        } else {
            return null;
        }
    }
}
