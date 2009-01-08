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
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;

/**
 * Abstract range that implements the W3C range specification using the API offered by the browser. Concrete extensions
 * of these class have the role to adapt the specific range API offered by each browser to the W3C range specification.
 * 
 * @param <R> The underlying range API used to implement the W3C range specification.
 * @version $Id$
 */
public abstract class AbstractRange<R extends JavaScriptObject> implements Range
{
    /**
     * Browser specific range implementation.
     */
    private R jsRange;

    /**
     * Creates a new instance that has to adapt the given browser-specific range to the W3C specification.
     * 
     * @param jsRange The range implementation to adapt.
     */
    AbstractRange(R jsRange)
    {
        setJSRange(jsRange);
    }

    /**
     * @return The underlying range implementation used.
     */
    public final R getJSRange()
    {
        return jsRange;
    }

    /**
     * Sets the underlying range implementation to be used. This method should not be called unless the browser provides
     * more than one type of range and you need to swap between them.
     * 
     * @param jsRange The underlying range implementation to be used.
     */
    protected final void setJSRange(R jsRange)
    {
        this.jsRange = jsRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#compareBoundaryPoints(RangeCompare, Range)
     */
    @SuppressWarnings("unchecked")
    public short compareBoundaryPoints(RangeCompare how, Range sourceRange)
    {
        return compareBoundaryPoints(how, ((AbstractRange<R>) sourceRange).getJSRange());
    }

    /**
     * Compare the boundary-points of two Ranges in a document.
     * 
     * @param how The type of comparison.
     * @param sourceJSRange The Range on which this current Range is compared to.
     * @return -1, 0 or 1 depending on whether the corresponding boundary-point of the Range is respectively before,
     *         equal to, or after the corresponding boundary-point of sourceRange.
     */
    protected abstract short compareBoundaryPoints(RangeCompare how, R sourceJSRange);

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneContents()
     */
    public DocumentFragment cloneContents()
    {
        Node start = getStartContainer();
        Node end = getEndContainer();
        Node root = DOMUtils.getInstance().getNearestCommonAncestor(start, end);
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();

        if (start == end) {
            return DOMUtils.getInstance().cloneNodeContents(root, startOffset, endOffset);
        }

        DocumentFragment contents = ((Document) root.getOwnerDocument()).createDocumentFragment();

        int startIndex = startOffset;
        if (start != root) {
            contents.appendChild(DOMUtils.getInstance().cloneNode(root, start, startOffset, false));
            startIndex = DOMUtils.getInstance().getNodeIndex(DOMUtils.getInstance().getChild(root, start)) + 1;
        }

        if (end != root) {
            int endIndex = DOMUtils.getInstance().getNodeIndex(DOMUtils.getInstance().getChild(root, end));
            contents.appendChild(DOMUtils.getInstance().cloneNodeContents(root, startIndex, endIndex));
            contents.appendChild(DOMUtils.getInstance().cloneNode(root, end, endOffset, true));
        } else {
            contents.appendChild(DOMUtils.getInstance().cloneNodeContents(root, startIndex, endOffset));
        }

        return contents;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#deleteContents()
     */
    public void deleteContents()
    {
        Node start = getStartContainer();
        Node end = getEndContainer();
        Node root = DOMUtils.getInstance().getNearestCommonAncestor(start, end);
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();

        if (start == end) {
            DOMUtils.getInstance().deleteNodeContents(root, startOffset, endOffset);
        } else {
            int startIndex = startOffset;
            if (start != root) {
                DOMUtils.getInstance().deleteNodeContents(root, start, startOffset, false);
                startIndex = DOMUtils.getInstance().getNodeIndex(DOMUtils.getInstance().getChild(root, start)) + 1;
            }

            int endIndex = endOffset;
            if (end != root) {
                endIndex = DOMUtils.getInstance().getNodeIndex(DOMUtils.getInstance().getChild(root, end));
                DOMUtils.getInstance().deleteNodeContents(root, end, endOffset, true);
            }
            DOMUtils.getInstance().deleteNodeContents(root, startIndex, endIndex);
        }

        setEnd(start, startOffset);
        collapse(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getCommonAncestorContainer()
     */
    public Node getCommonAncestorContainer()
    {
        return DOMUtils.getInstance().getNearestCommonAncestor(getStartContainer(), getEndContainer());
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#isCollapsed()
     */
    public boolean isCollapsed()
    {
        return getStartContainer() == getEndContainer() && getStartOffset() == getEndOffset();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNode(Node)
     */
    public void selectNode(Node refNode)
    {
        Node parent = refNode.getParentNode();
        int index = DOMUtils.getInstance().getNodeIndex(refNode);
        setStart(parent, index);
        setEnd(parent, index + 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#toHTML()
     */
    public String toHTML()
    {
        return cloneContents().getInnerHTML();
    }
}
