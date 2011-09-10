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
package org.xwiki.gwt.dom.client.internal.ie;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.RangeCompare;
import org.xwiki.gwt.dom.client.internal.AbstractSelection;
import org.xwiki.gwt.dom.client.internal.ie.TextRange.Unit;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;

/**
 * The implementation of Mozilla's selection specification using Internet Explorer's old selection API.
 * 
 * @version $Id$
 */
public class IEOldSelection extends AbstractSelection
{
    /**
     * Specifies where a range starts or ends inside the DOM tree.
     */
    protected static final class RangeBoundary
    {
        /**
         * The node containing the range boundary.
         */
        private final Node container;

        /**
         * The offset within the {@link #container} where the boundary is placed.
         */
        private final int offset;

        /**
         * Creates a new range boundary.
         * 
         * @param container {@link #container}
         * @param offset {@link #offset}
         */
        public RangeBoundary(Node container, int offset)
        {
            this.container = container;
            this.offset = offset;
        }

        /**
         * @return {@link #container}
         */
        public Node getContainer()
        {
            return container;
        }

        /**
         * @return {@link #offset}
         */
        public int getOffset()
        {
            return offset;
        }

    }

    /**
     * The underlying native selection object provided by the browser.
     */
    private final NativeSelection nativeSelection;

    /**
     * The element used to mark the start of the selection.
     * 
     * @see #addRange(Range)
     */
    private final Element startMarker;

    /**
     * The native text range used to set the start of the selection.
     * 
     * @see TextRange#setEndPoint(RangeCompare, TextRange)
     * @see #addRange(Range)
     */
    private final TextRange startRef;

    /**
     * The element used to mark the end of the selection.
     * 
     * @see #addRange(Range)
     */
    private final Element endMarker;

    /**
     * The native text range used to set the end of the selection.
     * 
     * @see TextRange#setEndPoint(RangeCompare, TextRange)
     * @see #addRange(Range)
     */
    private final TextRange endRef;

    /**
     * Creates a new instance that wraps the given native selection object. This object will be used to implement
     * Mozilla's selection specification.
     * 
     * @param nativeSelection the underlying native selection object to be used
     */
    public IEOldSelection(NativeSelection nativeSelection)
    {
        this.nativeSelection = nativeSelection;
        startMarker = createBoundaryMarker();
        startRef = TextRange.newInstance(nativeSelection.getOwnerDocument());
        endMarker = (Element) startMarker.cloneNode(true);
        endRef = startRef.duplicate();
    }

    /**
     * @return {@link #nativeSelection}
     */
    protected NativeSelection getNativeSelection()
    {
        return nativeSelection;
    }

    @Override
    public void addRange(Range range)
    {
        DOMUtils.getInstance().scrollIntoView(range);
        if (range.getStartContainer() == range.getEndContainer()
            && range.getStartContainer().getNodeType() == Node.ELEMENT_NODE
            && range.getStartOffset() == range.getEndOffset() - 1) {
            // The given range wraps a DOM node.
            Node selectedNode = range.getStartContainer().getChildNodes().getItem(range.getStartOffset());
            // Try to make a control selection.
            try {
                ControlRange controlRange = ControlRange.newInstance(nativeSelection.getOwnerDocument());
                controlRange.add((Element) selectedNode);
                controlRange.select();
                return;
            } catch (Exception e) {
                // The selected node doesn't support control selection.
            }
        }

        // Otherwise use text selection.
        addTextRange(adjustRangeAndDOM(range));
    }

    /**
     * Adjusts the given range and the DOM tree prior to selecting the range. We have to do this because IE selection
     * boundaries cannot, in most of the cases, be placed inside empty elements or empty text nodes or at the beginning
     * of a text node.
     * 
     * @param range the range to be adjusted
     * @return the adjusted range
     */
    private Range adjustRangeAndDOM(Range range)
    {
        Node start = range.getStartContainer();
        if (range.isCollapsed() && start.getNodeType() == Node.TEXT_NODE && range.getStartOffset() == 0
            && !isTextBefore(start)) {
            start.setNodeValue("\u00A0" + start.getNodeValue());
            Range adjusted = range.cloneRange();
            adjusted.setEnd(start, 1);
            return adjusted;
        }
        return range;
    }

    /**
     * Utility method for testing if the previous sibling (skipping empty text nodes) of a DOM node is a non-empty text
     * node.
     * 
     * @param node a DOM node
     * @return true if the previous sibling, skipping empty text nodes, of the given node is a non-empty text node
     */
    private boolean isTextBefore(Node node)
    {
        Node sibling = node.getPreviousSibling();
        // Skip empty text nodes.
        while (sibling != null && sibling.getNodeType() == Node.TEXT_NODE && sibling.getNodeValue().length() == 0) {
            sibling = sibling.getPreviousSibling();
        }
        return sibling != null && sibling.getNodeType() == Node.TEXT_NODE;
    }

    /**
     * Creates a text selection from the given range.
     * 
     * @param range the range to be added to the selection
     */
    protected void addTextRange(Range range)
    {
        DOMUtils domUtils = DOMUtils.getInstance();
        int leftOffset = 0;
        int rightOffset = domUtils.getLength(range.getEndContainer()) - range.getEndOffset();

        switch (range.getStartContainer().getNodeType()) {
            case Node.TEXT_NODE:
                leftOffset = range.getStartOffset();
                // fall through
            case DOMUtils.CDATA_NODE:
            case DOMUtils.COMMENT_NODE:
                range.getStartContainer().getParentNode().insertBefore(startMarker, range.getStartContainer());
                break;
            case Node.ELEMENT_NODE:
                domUtils.insertAt(range.getStartContainer(), startMarker, range.getStartOffset());
                break;
            default:
                throw new IllegalArgumentException();
        }
        startRef.moveToElementText(startMarker);
        startRef.moveEnd(Unit.CHARACTER, leftOffset);

        switch (range.getEndContainer().getNodeType()) {
            case DOMUtils.CDATA_NODE:
            case DOMUtils.COMMENT_NODE:
                rightOffset = 0;
                // fall through
            case Node.TEXT_NODE:
                domUtils.insertAfter(endMarker, range.getEndContainer());
                break;
            case Node.ELEMENT_NODE:
                domUtils.insertAt(range.getEndContainer(), endMarker, range.getEndContainer().getChildNodes()
                    .getLength()
                    - rightOffset);
                rightOffset = 0;
                break;
            default:
                throw new IllegalArgumentException();
        }
        endRef.moveToElementText(endMarker);
        endRef.moveStart(Unit.CHARACTER, -rightOffset);

        TextRange textRange = TextRange.newInstance(nativeSelection.getOwnerDocument());
        textRange.setEndPoint(RangeCompare.END_TO_START, startRef);
        textRange.setEndPoint(RangeCompare.START_TO_END, endRef);

        domUtils.detach(startMarker);
        domUtils.detach(endMarker);

        textRange.select();
    }

    /**
     * @return an element that can be used a range boundary marker for this selection
     */
    private Element createBoundaryMarker()
    {
        ImageElement marker = nativeSelection.getOwnerDocument().createImageElement();
        marker.setWidth(0);
        marker.setHeight(0);
        return marker.cast();
    }

    @Override
    public Range getRangeAt(int index)
    {
        if (index != 0) {
            throw new IndexOutOfBoundsException();
        }

        NativeRange nativeRange = nativeSelection.createRange();
        Range range = nativeRange.getOwnerDocument().createRange();

        if (nativeRange.isTextRange()) {
            TextRange textRange = (TextRange) nativeRange;

            RangeBoundary start = getBoundary(textRange, true);
            range.setStart(start.getContainer(), start.getOffset());

            RangeBoundary end = getBoundary(textRange, false);
            range.setEnd(end.getContainer(), end.getOffset());
        } else {
            range.selectNode(((ControlRange) nativeRange).get(0));
        }

        return range;
    }

    @Override
    public int getRangeCount()
    {
        return 1;
    }

    @Override
    public void removeAllRanges()
    {
        getNativeSelection().empty();
    }

    @Override
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
    protected RangeBoundary getBoundary(TextRange textRange, boolean start)
    {
        // Determine the element containing the specified range boundary.
        TextRange refRange = textRange.duplicate();
        refRange.collapse(start);
        Node container = refRange.getParentElement();

        // We use another text range to find the boundary position within its element container.
        TextRange searchRange = TextRange.newInstance(textRange.getOwnerDocument());
        // Initially the boundary can be anywhere inside its element container.
        searchRange.moveToElementText(Element.as(container));
        // The object used to compare the start point of the search range with the searched boundary.
        RangeCompare compareStart = RangeCompare.valueOf(start, true);

        // Iterate through all child nodes in search for the range boundary.
        Node child = container.getFirstChild();
        int offset = 0;
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                // Select the element.
                refRange.moveToElementText(Element.as(child));
                // Reduce the search range by moving its start point after the current element.
                searchRange.setEndPoint(RangeCompare.END_TO_START, refRange);
                if (searchRange.compareEndPoints(compareStart, textRange) > 0) {
                    break;
                }
            } else if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().length() > 0) {
                // Select the text node. Using TextRange#findText is so far the most reliable way of doing this. Moving
                // the start point of the search range with the number of characters in the text node doesn't always
                // jump over the text node because the search range can have caret positions left over before the text
                // node (e.g. when moving from one table cell to the next you have to jump over the border, which is
                // counted by TextRange#move as a character).
                refRange = searchRange.duplicate();
                // We have to convert nbsp's to plain spaces because TextRange#getText seems to do so.
                if (!refRange.findText(child.getNodeValue().replace('\u00A0', '\u0020'), 0, 4)) {
                    // We shouldn't get here!
                    throw new RuntimeException("Unexpected behavior of TextRange#findText");
                }
                // Reduce the search range by moving its start point after the current text node.
                searchRange.setEndPoint(RangeCompare.END_TO_START, refRange);
                if (searchRange.compareEndPoints(compareStart, textRange) >= 0) {
                    container = child;
                    // Now we have to compute the offset within this text node.
                    searchRange = textRange.duplicate();
                    searchRange.collapse(start);
                    offset = getOffset(searchRange, refRange);
                    break;
                }
            }
            child = child.getNextSibling();
            offset++;
        }

        return new RangeBoundary(container, offset);
    }

    /**
     * Binary search the position of the given caret inside the specified text.
     * 
     * @param caret a text range collapsed inside a text node
     * @param text a text range selecting a text node
     * @return the offset of the given caret inside the text selected by the second range
     */
    private int getOffset(TextRange caret, TextRange text)
    {
        int start = 0;
        int end = text.getText().length();
        TextRange finder = TextRange.newInstance(caret.getOwnerDocument());
        while (start < end) {
            int middle = (start + end) / 2;
            finder.setEndPoint(RangeCompare.START_TO_START, text);
            finder.move(Unit.CHARACTER, middle);
            int delta = caret.compareEndPoints(RangeCompare.START_TO_START, finder);
            if (delta == 0) {
                return middle;
            } else if (delta < 0) {
                end = middle;
            } else {
                start = middle + 1;
            }
        }
        return start;
    }
}
