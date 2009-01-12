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
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;

/**
 * This is a cross-browser implementation of the W3C Range specification.
 * <p>
 * Acknowledgment to Mozilla Foundation for making nsRange.cpp public.
 * 
 * @version $Id$
 * @see http://hg.mozilla.org/mozilla-central/file/b945b4f67e7e/content/base/src/nsRange.cpp
 */
public class DefaultRange implements Range
{
    /**
     * The DOM node containing the start of this range.
     */
    private Node startContainer;

    /**
     * The offset within the {@link #startContainer}.
     */
    private int startOffset;

    /**
     * The DOM node containing the end of this range.
     */
    private Node endContainer;

    /**
     * The offset within the {@link #endContainer}.
     */
    private int endOffset;

    /**
     * Specifies if both boundaries of this range have been successfully set to valid DOM nodes.
     */
    private boolean positioned;

    /**
     * Specifies if this range is in use.
     */
    private boolean detached;

    /**
     * Collection of DOM utility methods.
     */
    private DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * Sets the boundaries of this range.
     * 
     * @param startContainer {@link #startContainer}
     * @param startOffset {@link #startOffset}
     * @param endContainer {@link #endContainer}
     * @param endOffset {@link #endOffset}
     */
    private void setRange(Node startContainer, int startOffset, Node endContainer, int endOffset)
    {
        boolean valid = !(startContainer == null ^ endContainer == null);
        if (valid && startContainer != null) {
            valid = valid && startContainer.getOwnerDocument() == endContainer.getOwnerDocument();
            valid = valid && startOffset >= 0 && startOffset <= domUtils.getLength(startContainer);
            valid = valid && endOffset >= 0 && endOffset <= domUtils.getLength(endContainer);
        }

        if (!valid) {
            throw new IllegalArgumentException();
        }

        this.startContainer = startContainer;
        this.startOffset = startOffset;
        this.endContainer = endContainer;
        this.endOffset = endOffset;
        positioned = startContainer != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneContents()
     */
    public DocumentFragment cloneContents()
    {
        if (detached) {
            throw new IllegalStateException();
        }

        Node root = getCommonAncestorContainer();

        if (startContainer == endContainer) {
            return domUtils.cloneNodeContents(root, startOffset, endOffset);
        }

        DocumentFragment contents = ((Document) root.getOwnerDocument()).createDocumentFragment();

        int startIndex = startOffset;
        if (startContainer != root) {
            contents.appendChild(domUtils.cloneNode(root, startContainer, startOffset, false));
            startIndex = domUtils.getNodeIndex(domUtils.getChild(root, startContainer)) + 1;
        }

        if (endContainer != root) {
            int endIndex = domUtils.getNodeIndex(domUtils.getChild(root, endContainer));
            contents.appendChild(domUtils.cloneNodeContents(root, startIndex, endIndex));
            contents.appendChild(domUtils.cloneNode(root, endContainer, endOffset, true));
        } else {
            contents.appendChild(domUtils.cloneNodeContents(root, startIndex, endOffset));
        }

        return contents;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneRange()
     */
    public Range cloneRange()
    {
        if (detached) {
            throw new IllegalStateException();
        }

        // We should use Object.clone when it is implemented in GWT
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=1843
        DefaultRange clone = new DefaultRange();
        clone.setRange(startContainer, startOffset, endContainer, endOffset);
        return clone;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#collapse(boolean)
     */
    public void collapse(boolean toStart)
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        if (toStart) {
            setRange(startContainer, startOffset, startContainer, startOffset);
        } else {
            setRange(endContainer, endOffset, endContainer, endOffset);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#compareBoundaryPoints(RangeCompare, Range)
     */
    public short compareBoundaryPoints(RangeCompare how, Range sourceRange)
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }
        if (startContainer.getOwnerDocument() != sourceRange.getStartContainer().getOwnerDocument()) {
            throw new IllegalArgumentException();
        }

        switch (how) {
            case START_TO_START:
                return domUtils.comparePoints(startContainer, startOffset, sourceRange.getStartContainer(), sourceRange
                    .getStartOffset());
            case START_TO_END:
                return domUtils.comparePoints(endContainer, endOffset, sourceRange.getStartContainer(), sourceRange
                    .getStartOffset());
            case END_TO_START:
                return domUtils.comparePoints(startContainer, startOffset, sourceRange.getEndContainer(), sourceRange
                    .getEndOffset());
            case END_TO_END:
                return domUtils.comparePoints(endContainer, endOffset, sourceRange.getEndContainer(), sourceRange
                    .getEndOffset());
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#deleteContents()
     */
    public void deleteContents()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        Node root = getCommonAncestorContainer();

        if (startContainer == endContainer) {
            domUtils.deleteNodeContents(root, startOffset, endOffset);
        } else {
            int startIndex = startOffset;
            if (startContainer != root) {
                domUtils.deleteNodeContents(root, startContainer, startOffset, false);
                startIndex = domUtils.getNodeIndex(domUtils.getChild(root, startContainer)) + 1;
            }

            int endIndex = endOffset;
            if (endContainer != root) {
                endIndex = domUtils.getNodeIndex(domUtils.getChild(root, endContainer));
                domUtils.deleteNodeContents(root, endContainer, endOffset, true);
            }
            domUtils.deleteNodeContents(root, startIndex, endIndex);
        }

        setRange(startContainer, startOffset, startContainer, startOffset);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#detach()
     */
    public void detach()
    {
        if (detached) {
            throw new IllegalStateException();
        }

        detached = true;
        setRange(null, 0, null, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#extractContents()
     */
    public DocumentFragment extractContents()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getCommonAncestorContainer()
     */
    public Node getCommonAncestorContainer()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        return domUtils.getNearestCommonAncestor(startContainer, endContainer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndContainer()
     */
    public Node getEndContainer()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return endContainer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndOffset()
     */
    public int getEndOffset()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return endOffset;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartContainer()
     */
    public Node getStartContainer()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return startContainer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartOffset()
     */
    public int getStartOffset()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return startOffset;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#insertNode(Node)
     */
    public void insertNode(Node newNode)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#isCollapsed()
     */
    public boolean isCollapsed()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        return startContainer == endContainer && startOffset == endOffset;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNode(Node)
     */
    public void selectNode(Node refNode)
    {
        Node parent = refNode.getParentNode();
        int index = domUtils.getNodeIndex(refNode);
        setRange(parent, index, parent, index + 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNodeContents(Node)
     */
    public void selectNodeContents(Node refNode)
    {
        setRange(refNode, 0, refNode, domUtils.getLength(refNode));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEnd(Node, int)
     */
    public void setEnd(Node refNode, int offset)
    {
        if (!positioned || startContainer.getOwnerDocument() != refNode.getOwnerDocument()
            || domUtils.comparePoints(startContainer, startOffset, refNode, offset) == 1) {
            setRange(refNode, offset, refNode, offset);
        } else {
            setRange(startContainer, startOffset, refNode, offset);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndAfter(Node)
     */
    public void setEndAfter(Node refNode)
    {
        setEnd(refNode.getParentNode(), domUtils.getNodeIndex(refNode) + 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndBefore(Node)
     */
    public void setEndBefore(Node refNode)
    {
        setEnd(refNode.getParentNode(), domUtils.getNodeIndex(refNode));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStart(Node, int)
     */
    public void setStart(Node refNode, int offset)
    {
        if (!positioned || endContainer.getOwnerDocument() != refNode.getOwnerDocument()
            || domUtils.comparePoints(refNode, offset, endContainer, endOffset) == 1) {
            setRange(refNode, offset, refNode, offset);
        } else {
            setRange(refNode, offset, endContainer, endOffset);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartAfter(Node)
     */
    public void setStartAfter(Node refNode)
    {
        setStart(refNode.getParentNode(), domUtils.getNodeIndex(refNode) + 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartBefore(Node)
     */
    public void setStartBefore(Node refNode)
    {
        setStart(refNode.getParentNode(), domUtils.getNodeIndex(refNode));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#surroundContents(Node)
     */
    public void surroundContents(Node newParent)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#toHTML()
     */
    public String toHTML()
    {
        if (detached) {
            throw new IllegalStateException();
        }

        if (positioned) {
            return cloneContents().getInnerHTML();
        } else {
            return "";
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#toString()
     */
    public String toString()
    {
        if (detached) {
            throw new IllegalStateException();
        }

        if (positioned) {
            return cloneContents().getInnerText();
        } else {
            return "";
        }
    }
}
