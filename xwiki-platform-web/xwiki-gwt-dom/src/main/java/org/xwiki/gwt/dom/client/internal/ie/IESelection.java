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
 * The implementation of Mozilla's selection specification using Internet Explorer's selection API.
 * 
 * @version $Id$
 */
public class IESelection extends AbstractSelection
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
     * The list of elements supporting control selection.
     */
    private static final String[] SELECTABLE_ELEMENTS = new String[] {"img", "button"};

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
    public IESelection(NativeSelection nativeSelection)
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

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelection#addRange(Range)
     */
    public void addRange(Range range)
    {
        DOMUtils.getInstance().scrollIntoView(range);
        if (range.getStartContainer() == range.getEndContainer()
            && range.getStartContainer().getNodeType() == Node.ELEMENT_NODE
            && range.getStartOffset() == range.getEndOffset() - 1) {
            Node selectedNode = range.getStartContainer().getChildNodes().getItem(range.getStartOffset());
            // Test if the selected node supports control selection.
            if (supportsControlSelection(selectedNode)) {
                ControlRange controlRange = ControlRange.newInstance(nativeSelection.getOwnerDocument());
                controlRange.add((Element) selectedNode);
                controlRange.select();
                return;
            }
        }

        // Otherwise use text selection.
        addTextRange(adjustRangeAndDOM(range));
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node supports control selection, {@code false} otherwise
     */
    private boolean supportsControlSelection(Node node)
    {
        for (int i = 0; i < SELECTABLE_ELEMENTS.length; i++) {
            if (SELECTABLE_ELEMENTS[i].equalsIgnoreCase(node.getNodeName())) {
                return true;
            }
        }
        return false;
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
    protected Element createBoundaryMarker()
    {
        ImageElement marker = nativeSelection.getOwnerDocument().xCreateImageElement();
        marker.setWidth(0);
        marker.setHeight(0);
        return marker.cast();
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

            RangeBoundary start = getBoundary(textRange, true);
            range.setStart(start.getContainer(), start.getOffset());

            RangeBoundary end = getBoundary(textRange, false);
            range.setEnd(end.getContainer(), end.getOffset());
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
    protected RangeBoundary getBoundary(TextRange textRange, boolean start)
    {
        // Determine the element containing the specified range boundary.
        TextRange refRange = textRange.duplicate();
        refRange.collapse(start);
        Node container = refRange.getParentElement();
        // The specified range boundary is either between child nodes or inside a text node.

        // We use a text range to find the text node that could possibly contain the range boundary.
        TextRange searchRange = TextRange.newInstance(textRange.getOwnerDocument());
        searchRange.moveToElementText((Element) container);

        // The claws used to catch the specified range boundary.
        RangeCompare compareStart = RangeCompare.valueOf(true, start);
        RangeCompare compareEnd = RangeCompare.valueOf(false, start);

        // Iterate through all child nodes in search for the range boundary.
        Node child = container.getFirstChild();
        int offset = 0;
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                // Move the reference range before this child element.
                moveTextRangeBeforeElement(refRange, (Element) child);
                // Let's see if the boundary is before this child element.
                if (textRange.compareEndPoints(compareStart, refRange) <= 0) {
                    break;
                }
            } else if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().length() > 0) {
                // Select this text node.
                refRange = searchRange.duplicate();
                // We have to convert nbsp's to plain spaces because TextRange#getText seems to do so.
                if (!refRange.findText(child.getNodeValue().replace('\u00A0', '\u0020'), 0, 4)) {
                    // We shouldn't get here!
                    throw new RuntimeException("Unexpected behavior of TextRange#findText");
                }
                // See if the range boundary is inside this text node.
                if (textRange.compareEndPoints(compareEnd, refRange) <= 0) {
                    if (textRange.compareEndPoints(compareStart, refRange) >= 0) {
                        container = child;
                        // Now we have to compute the offset within this text node.
                        offset = getOffset(refRange, textRange, start);
                    }
                    // We either found the boundary or passed beyond it.
                    break;
                }
                // Update the search range.
                searchRange.setEndPoint(RangeCompare.END_TO_START, refRange);
            }
            child = child.getNextSibling();
            offset++;
        }
        return new RangeBoundary(container, offset);
    }

    /**
     * Moves the given text range before the specified element.
     * 
     * @param textRange the text range to be moved
     * @param element the element before which the text range is moved
     */
    protected void moveTextRangeBeforeElement(TextRange textRange, Element element)
    {
        textRange.moveToElementText(element);
        // Sometimes moveToElementText has unexpected results. Let's test if textRange starts before element and if not
        // then try something different.
        int left = getLeft(element);
        int top = getTop(element);
        if (textRange.getOffsetLeft() != left || top < textRange.getOffsetTop()
            || top > (textRange.getOffsetTop() + getFirstLineHeight(textRange))) {
            // This can fail moving the text range before the element too (that's why we tried moveToElementText first).
            textRange.moveToPoint(left, top);
            // Don't bet on the result!
        }
    }

    /**
     * NOTE: For a {@code strong} element that starts in the middle of a line and spans multiple lines this method
     * returns the distance in pixels from its first character (provided it starts with text) to the left boundary of
     * the parent window. This is important since the bounding rectangle of the {@code strong} element can have the
     * width of the parent window so the distance from the left side of this bounding rectangle to the left boundary of
     * the parent window could be 0.
     * 
     * @param element a DOM element
     * @return the distance, in pixels, from the given element's start point to the left boundary of the parent window
     */
    protected native int getLeft(Element element)
    /*-{
        var left = -element.ownerDocument.documentElement.scrollLeft;
        while (element) {
            left += element.offsetLeft + element.clientLeft - element.scrollLeft;
            element = element.offsetParent;
        }
        return left;
    }-*/;

    /**
     * @param element a DOM element
     * @return the distance, in pixels, from the given element's start point to the top boundary of the parent window
     */
    protected native int getTop(Element element)
    /*-{
        var top = -element.ownerDocument.documentElement.scrollTop;
        while (element) {
            top += element.offsetTop + element.clientTop - element.scrollTop;
            element = element.offsetParent;
        }
        return top;
    }-*/;

    /**
     * @param textRange a text range
     * @return the height, in pixels, of the first line selected by the given text range
     */
    protected native int getFirstLineHeight(TextRange textRange)
    /*-{
        var firstLineRect = textRange.getClientRects()[0];
        return firstLineRect.bottom - firstLineRect.top;
    }-*/;

    /**
     * Computes the number of characters between the start of the left range to the specified boundary of the right
     * range. The given ranges need to overlap and the start of the left range has to be before or equal to the
     * specified boundary of the right range.
     * 
     * @param left the left text range
     * @param right the rich text range
     * @param rightBoundary specifies which boundary of the right text range to consider. Use {@code true} for start
     *            boundary and {@code false} for end boundary.
     * @return the offset of the right range from the start of the left range
     */
    protected int getOffset(TextRange left, TextRange right, boolean rightBoundary)
    {
        int offset = 0;
        RangeCompare whichEndPoints = RangeCompare.valueOf(rightBoundary, true);
        while (left.compareEndPoints(whichEndPoints, right) < 0 && left.moveStart(Unit.CHARACTER, 1) > 0) {
            offset++;
        }
        return offset;
    }
}
