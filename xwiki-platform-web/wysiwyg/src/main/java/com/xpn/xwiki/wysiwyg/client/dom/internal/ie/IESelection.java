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
package com.xpn.xwiki.wysiwyg.client.dom.internal.ie;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;
import com.xpn.xwiki.wysiwyg.client.dom.Text;
import com.xpn.xwiki.wysiwyg.client.dom.internal.AbstractSelection;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.TextRange.Unit;

/**
 * The implementation of Mozilla's selection specification using Internet Explorer's selection API.
 * 
 * @version $Id$
 */
public class IESelection extends AbstractSelection
{
    /**
     * The underlying native selection object provided by the browser.
     */
    private final NativeSelection nativeSelection;

    /**
     * Creates a new instance that wraps the given native selection object. This object will be used to implement
     * Mozilla's selection specification.
     * 
     * @param nativeSelection the underlying native selection object to be used
     */
    public IESelection(NativeSelection nativeSelection)
    {
        this.nativeSelection = nativeSelection;
    }

    /**
     * @return {@link #nativeSelection}
     */
    protected NativeSelection getNativeSelection()
    {
        return nativeSelection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#addRange(Range)
     */
    public void addRange(Range range)
    {
        if (range.getStartContainer() == range.getEndContainer()
            && range.getStartContainer().getNodeType() == Node.ELEMENT_NODE
            && range.getStartOffset() == range.getEndOffset() - 1) {
            Node selectedNode = range.getStartContainer().getChildNodes().getItem(range.getStartOffset());
            // Test if the selected node supports control selection.
            if ("img".equalsIgnoreCase(selectedNode.getNodeName())) {
                ControlRange controlRange = ControlRange.newInstance(nativeSelection.getOwnerDocument());
                controlRange.add((Element) selectedNode);
                controlRange.select();
                return;
            }
        }

        // Otherwise use text selection.
        TextRange textRange = TextRange.newInstance(nativeSelection.getOwnerDocument());
        // This is not the best way to set the boundaries of the native range. Needs to be revisited.
        setStart(textRange, range.getStartContainer(), range.getStartOffset());
        setEnd(textRange, range.getEndContainer(), range.getEndOffset());
        textRange.select();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#getRangeAt(int)
     */
    public Range getRangeAt(int index)
    {
        if (index != 0) {
            throw new IndexOutOfBoundsException();
        }

        NativeRange nativeRange = nativeSelection.createRange();
        Range range = nativeRange.getOwnerDocument().createRange();

        if (nativeRange.isTextRange()) {
            TextRange textRange = (TextRange) nativeRange;
            Node startContainer = getContainer(textRange, true);
            range.setStart(startContainer, getOffset(textRange, startContainer, true));
            Node endContainer = getContainer(textRange, false);
            range.setEnd(endContainer, getOffset(textRange, endContainer, false));
        } else {
            range.selectNode(((ControlRange) nativeRange).get(0));
        }

        return range;
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
     * @see AbstractSelection#removeAllRanges()
     */
    public void removeAllRanges()
    {
        getNativeSelection().empty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#removeRange(Range)
     */
    public void removeRange(Range range)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Computes the start or end container of a text range.
     * 
     * @param textRange the text range for which to compute the boundary container
     * @param start specifies which boundary container to compute
     * @return the container of the range's start , if start is true, or the container of the range's end, otherwise
     */
    public static Node getContainer(TextRange textRange, boolean start)
    {
        TextRange refRange = textRange.duplicate();
        refRange.collapse(start);
        Element parent = refRange.getParentElement();
        RangeCompare compareStart = RangeCompare.valueOf(true, start);
        RangeCompare compareEnd = RangeCompare.valueOf(false, start);
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                refRange.moveToTextNode(Text.as(child));
                if (textRange.compareEndPoints(compareStart, refRange) >= 0
                    && textRange.compareEndPoints(compareEnd, refRange) <= 0) {
                    return child;
                }
            }
            child = child.getNextSibling();
        }
        return parent;
    }

    /**
     * Computes the offset of the specified text range boundary within its container.
     * 
     * @param textRange the text range for which to compute the offset
     * @param container the node containing the boundary
     * @param start whether the start of the end boundary
     * @return the offset of the range's start , if start is true, or the offset of the range's end, otherwise
     */
    public static int getOffset(TextRange textRange, Node container, boolean start)
    {
        TextRange refRange = textRange.duplicate();
        RangeCompare whichEndPoints = RangeCompare.valueOf(start, true);
        if (container.getNodeType() == Node.TEXT_NODE) {
            refRange.moveToTextNode(Text.as(container));
            while (refRange.compareEndPoints(whichEndPoints, textRange) < 0 && refRange.getText().length() > 0) {
                refRange.moveStart(Unit.CHARACTER, 1);
            }
            return container.getNodeValue().length() - refRange.getText().length();
        } else {
            int offset = 0;
            Node child = container.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    refRange.moveToElementText(Element.as(child));
                    if (refRange.compareEndPoints(whichEndPoints, textRange) >= 0) {
                        break;
                    }
                }
                child = child.getNextSibling();
                offset++;
            }
            return offset;
        }
    }

    /**
     * Sets the start boundary of a text range.
     * 
     * @param textRange the text range whose start boundary is set
     * @param refNode the node containing the start boundary
     * @param offset the offset within the start boundary container
     */
    public static void setStart(TextRange textRange, Node refNode, int offset)
    {
        switch (refNode.getNodeType()) {
            case Node.ELEMENT_NODE:
                if (offset < refNode.getChildNodes().getLength()) {
                    // Before a child node
                    textRange.setEndPoint(RangeCompare.START_TO_START, refNode.getChildNodes().getItem(offset));
                } else if (refNode.hasChildNodes()) {
                    // After last child
                    textRange.setEndPoint(RangeCompare.END_TO_START, refNode.getLastChild());
                } else {
                    // Inside an empty element
                    refNode.appendChild(refNode.getOwnerDocument().createTextNode(""));
                    textRange.setEndPoint(RangeCompare.START_TO_START, refNode.getFirstChild());
                }
                break;
            case Node.TEXT_NODE:
                textRange.setEndPoint(RangeCompare.START_TO_START, refNode, offset);
                break;
            case 4:
                // CDATA
            case 8:
                // COMMENT
                // Since IE's text range cannot start inside a comment or CDATA node, we place the start point
                // before.
                textRange.setEndPoint(RangeCompare.START_TO_START, refNode);
                break;
            default:
                throw new IllegalArgumentException(DOMUtils.UNSUPPORTED_NODE_TYPE);
        }
    }

    /**
     * Sets the end boundary of a text range.
     * 
     * @param textRange the text range whose end boundary is set
     * @param refNode the node containing the end boundary
     * @param offset the offset within the end boundary container
     */
    public static void setEnd(TextRange textRange, Node refNode, int offset)
    {
        switch (refNode.getNodeType()) {
            case Node.ELEMENT_NODE:
                if (offset > 0) {
                    // After a child node
                    textRange.setEndPoint(RangeCompare.END_TO_END, refNode.getChildNodes().getItem(offset - 1));
                } else if (refNode.hasChildNodes()) {
                    // Before first child
                    textRange.setEndPoint(RangeCompare.START_TO_END, refNode.getFirstChild());
                } else {
                    // Inside an empty element
                    refNode.appendChild(refNode.getOwnerDocument().createTextNode(""));
                    textRange.setEndPoint(RangeCompare.END_TO_END, refNode.getFirstChild());
                }
                break;
            case Node.TEXT_NODE:
                textRange.setEndPoint(RangeCompare.START_TO_END, refNode, offset);
                break;
            case 4:
                // CDATA
            case 8:
                // COMMENT
                // Since IE's text range cannot end inside a comment or CDATA node, we place the end point after.
                textRange.setEndPoint(RangeCompare.END_TO_END, refNode);
                break;
            default:
                throw new IllegalArgumentException(DOMUtils.UNSUPPORTED_NODE_TYPE);
        }
    }
}
