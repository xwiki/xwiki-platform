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
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;

/**
 * Adapts the Firefox Range implementation to meet the W3C Range specification. We address versions prior to 3.0 since
 * Firefox 3.0 and later seems to follow closely the W3C Range specification.
 * 
 * @version $Id$
 */
public class MozillaRange extends DefaultRange
{
    /**
     * Creates a new instance that wraps the given native range object.
     * 
     * @param jsRange The native range object to be wrapped.
     */
    MozillaRange(JavaScriptObject jsRange)
    {
        super(jsRange);
    }

    /**
     * {@inheritDoc}<br/>
     * We have to overwrite because text nodes are not handled correctly.
     * 
     * @see DefaultRange#selectNodeContents(Node)
     */
    public void selectNodeContents(Node refNode)
    {
        if (refNode.getNodeType() == Node.TEXT_NODE) {
            setStart(refNode, 0);
            setEnd(refNode, refNode.getNodeValue().length());
        } else {
            super.selectNodeContents(refNode);
        }
    }

    /**
     * {@inheritDoc}<br/>
     * We have to overwrite in order to handle the case when the end is moved before the start.
     * 
     * @see DefaultRange#setEnd(Node, int)
     */
    public void setEnd(Node refNode, int offset)
    {
        int endOffset = adjustOffset(refNode, offset);
        if (getStartContainer().getOwnerDocument() == refNode.getOwnerDocument()) {
            Range refRange = ((Document) refNode.getOwnerDocument().cast()).createRange();
            refRange.setStart(refNode, endOffset);
            if (compareBoundaryPoints(RangeCompare.START_TO_START, refRange) > 0) {
                // The end would be before the start so we have to move the start also.
                super.setStart(refNode, endOffset);
            }
        }
        super.setEnd(refNode, endOffset);
    }

    /**
     * Adjusts the specified offset within the given node to avoid NS_ERROR_DOM_INDEX_SIZE_ERR.
     * 
     * @param node A DOM node.
     * @param offset The offset within the given node.
     * @return the adjusted value of the specified offset.
     */
    protected int adjustOffset(Node node, int offset)
    {
        if (node.getNodeType() == DOMUtils.COMMENT_NODE) {
            // Only 0 is allowed.
            return 0;
        } else {
            return offset;
        }
    }

    /**
     * {@inheritDoc}<br/>
     * We have to overwrite in order to handle the case when the end is moved before the start.
     * 
     * @see DefaultRange#setEndAfter(Node)
     */
    public void setEndAfter(Node refNode)
    {
        if (getStartContainer().getOwnerDocument() == refNode.getOwnerDocument()) {
            Range refRange = ((Document) refNode.getOwnerDocument().cast()).createRange();
            refRange.setStartAfter(refNode);
            if (compareBoundaryPoints(RangeCompare.START_TO_START, refRange) > 0) {
                // The end would be before the start so we have to move the start also.
                super.setStartAfter(refNode);
            }
        }
        super.setEndAfter(refNode);
    }

    /**
     * {@inheritDoc}<br/>
     * We have to overwrite in order to handle the case when the end is moved before the start.
     * 
     * @see DefaultRange#setEndBefore(Node)
     */
    public void setEndBefore(Node refNode)
    {
        if (getStartContainer().getOwnerDocument() == refNode.getOwnerDocument()) {
            Range refRange = ((Document) refNode.getOwnerDocument().cast()).createRange();
            refRange.setStartBefore(refNode);
            if (compareBoundaryPoints(RangeCompare.START_TO_START, refRange) > 0) {
                // The end would be before the start so we have to move the start also.
                super.setStartBefore(refNode);
            }
        }
        super.setEndBefore(refNode);
    }

    /**
     * {@inheritDoc}<br/>
     * We have to overwrite in order to handle the case when the start is moved after the end.
     * 
     * @see DefaultRange#setStart(Node, int)
     */
    public void setStart(Node refNode, int offset)
    {
        int startOffset = adjustOffset(refNode, offset);
        if (getStartContainer().getOwnerDocument() == refNode.getOwnerDocument()) {
            Range refRange = ((Document) refNode.getOwnerDocument().cast()).createRange();
            refRange.setEnd(refNode, startOffset);
            if (compareBoundaryPoints(RangeCompare.END_TO_END, refRange) < 0) {
                // The start would be after the end so we have to move the end also.
                super.setEnd(refNode, startOffset);
            }
        }
        super.setStart(refNode, startOffset);
    }

    /**
     * {@inheritDoc}<br/>
     * We have to overwrite in order to handle the case when the start is moved after the end.
     * 
     * @see DefaultRange#setStartAfter(Node)
     */
    public void setStartAfter(Node refNode)
    {
        if (getStartContainer().getOwnerDocument() == refNode.getOwnerDocument()) {
            Range refRange = ((Document) refNode.getOwnerDocument().cast()).createRange();
            refRange.setEndAfter(refNode);
            if (compareBoundaryPoints(RangeCompare.END_TO_END, refRange) < 0) {
                // The start would be after the end so we have to move the end also.
                super.setEndAfter(refNode);
            }
        }
        super.setStartAfter(refNode);
    }

    /**
     * {@inheritDoc}<br/>
     * We have to overwrite in order to handle the case when the start is moved after the end.
     * 
     * @see DefaultRange#setStartBefore(Node)
     */
    public void setStartBefore(Node refNode)
    {
        if (getStartContainer().getOwnerDocument() == refNode.getOwnerDocument()) {
            Range refRange = ((Document) refNode.getOwnerDocument().cast()).createRange();
            refRange.setEndBefore(refNode);
            if (compareBoundaryPoints(RangeCompare.END_TO_END, refRange) < 0) {
                // The start would be after the end so we have to move the end also.
                super.setEndBefore(refNode);
            }
        }
        super.setStartBefore(refNode);
    }
}
