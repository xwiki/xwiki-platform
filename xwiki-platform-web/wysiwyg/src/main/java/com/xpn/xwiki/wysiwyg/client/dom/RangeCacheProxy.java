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
package com.xpn.xwiki.wysiwyg.client.dom;

import com.google.gwt.dom.client.Node;

/**
 * Adds a cache layer on top of a range. It is needed mainly by the range implementation that uses Internet Explorer's
 * API because otherwise it recomputes the range properties each time they are requested.
 * 
 * @version $Id$
 */
public class RangeCacheProxy implements Range
{
    /**
     * The range that is being cached.
     */
    private final Range range;

    /**
     * @see Range#getStartContainer()
     */
    private Node startContainer;

    /**
     * @see Range#getStartOffset()
     */
    private Integer startOffset;

    /**
     * @see Range#getEndContainer()
     */
    private Node endContainer;

    /**
     * @see Range#getEndOffset()
     */
    private Integer endOffset;

    /**
     * @see Range#isCollapsed()
     */
    private Boolean collapsed;

    /**
     * @see Range#getCommonAncestorContainer()
     */
    private Node commonAncestorContainer;

    /**
     * Constructs a new cache proxy for the given range.
     * 
     * @param range The range object that is cached.
     */
    public RangeCacheProxy(Range range)
    {
        this.range = range;
    }

    /**
     * @return The range that is being cached.
     */
    public Range getCachedRange()
    {
        return this.range;
    }

    /**
     * Clears the cache by setting range properties to null. This way they will be recomputed next time they will be
     * requested.
     */
    private void clearCache()
    {
        startContainer = null;
        startOffset = null;
        endContainer = null;
        endOffset = null;
        collapsed = null;
        commonAncestorContainer = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#compareBoundaryPoints(RangeCompare, Range)
     */
    public short compareBoundaryPoints(RangeCompare how, Range sourceRange)
    {
        return range.compareBoundaryPoints(how, ((RangeCacheProxy) sourceRange).getCachedRange());
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneContents()
     */
    public DocumentFragment cloneContents()
    {
        return range.cloneContents();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneRange()
     */
    public Range cloneRange()
    {
        return new RangeCacheProxy(range.cloneRange());
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#collapse(boolean)
     */
    public void collapse(boolean toStart)
    {
        clearCache();
        range.collapse(toStart);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#deleteContents()
     */
    public void deleteContents()
    {
        clearCache();
        range.deleteContents();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#detach()
     */
    public void detach()
    {
        clearCache();
        range.detach();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#extractContents()
     */
    public DocumentFragment extractContents()
    {
        clearCache();
        return range.extractContents();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getCommonAncestorContainer()
     */
    public Node getCommonAncestorContainer()
    {
        if (commonAncestorContainer == null) {
            commonAncestorContainer = range.getCommonAncestorContainer();
        }
        return commonAncestorContainer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndContainer()
     */
    public Node getEndContainer()
    {
        if (endContainer == null) {
            endContainer = range.getEndContainer();
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
        if (endOffset == null) {
            endOffset = Integer.valueOf(range.getEndOffset());
        }
        return endOffset.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartContainer()
     */
    public Node getStartContainer()
    {
        if (startContainer == null) {
            startContainer = range.getStartContainer();
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
        if (startOffset == null) {
            startOffset = Integer.valueOf(range.getStartOffset());
        }
        return startOffset.intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#insertNode(Node)
     */
    public void insertNode(Node newNode)
    {
        clearCache();
        range.insertNode(newNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#isCollapsed()
     */
    public boolean isCollapsed()
    {
        if (collapsed == null) {
            collapsed = Boolean.valueOf(range.isCollapsed());
        }
        return collapsed.booleanValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNode(Node)
     */
    public void selectNode(Node refNode)
    {
        clearCache();
        range.selectNode(refNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNodeContents(Node)
     */
    public void selectNodeContents(Node refNode)
    {
        clearCache();
        range.selectNodeContents(refNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEnd(Node, int)
     */
    public void setEnd(Node refNode, int offset)
    {
        clearCache();
        range.setEnd(refNode, offset);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndAfter(Node)
     */
    public void setEndAfter(Node refNode)
    {
        clearCache();
        range.setEndAfter(refNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndBefore(Node)
     */
    public void setEndBefore(Node refNode)
    {
        clearCache();
        range.setEndBefore(refNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStart(Node, int)
     */
    public void setStart(Node refNode, int offset)
    {
        clearCache();
        range.setStart(refNode, offset);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartAfter(Node)
     */
    public void setStartAfter(Node refNode)
    {
        clearCache();
        range.setStartAfter(refNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartBefore(Node)
     */
    public void setStartBefore(Node refNode)
    {
        clearCache();
        range.setStartBefore(refNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#surroundContents(Node)
     */
    public void surroundContents(Node newParent)
    {
        clearCache();
        range.surroundContents(newParent);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#toString()
     */
    public String toString()
    {
        return range.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#toHTML()
     */
    public String toHTML()
    {
        return range.toHTML();
    }
}
