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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.ControlRange;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.NativeRange;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.TextRange;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.TextRange.Unit;

/**
 * In Internet Explorer we have two kinds of range: text and control. A text range is a fragment of the HTML document
 * that usually starts and ends inside a text node. A control range is a list of DOM elements. Throughout the code we'll
 * consider that a control range includes at least one element on which we'll operate most of the time. An empty control
 * range will be replaced by a collapsed text range. In fact, whenever it's possible, we transform a control range in a
 * text range.
 * 
 * @version $Id$
 */
public final class IERange extends AbstractRange<NativeRange>
{
    /**
     * Utility class for locating the end points of a range inside the DOM tree.
     */
    private static class EndPointLocator
    {
        /**
         * The base node. The other locator properties are relative to this node.
         */
        private final Node base;

        /**
         * The number of possible-cursor-positions, counting from the {@link #base} node.
         */
        private final int offset;

        /**
         * The direction to go from the {@link #base} node. If {@link #left} is true the end point is located
         * {@link #offset} positions to the left of the {@link #base} node.
         */
        private final boolean left;

        /**
         * Creates a new instance with the specified values.
         * 
         * @param base The base node.
         * @param offset The number of possible-cursor-positions, counting from the {@link #base} node.
         * @param left The direction to go from the {@link #base} node.
         */
        public EndPointLocator(Node base, int offset, boolean left)
        {
            this.base = base;
            this.offset = offset;
            this.left = left;
        }

        /**
         * @return the base node.
         */
        public Node getBase()
        {
            return base;
        }

        /**
         * @return the number of possible-cursor-positions, counting from the {@link #base} node.
         */
        public int getOffset()
        {
            return offset;
        }

        /**
         * @return the direction to go from the {@link #base} node.
         */
        public boolean isLeft()
        {
            return left;
        }
    }

    /**
     * Creates a new instance that wraps the given native range object, which can be either a text range or a control
     * range.
     * 
     * @param jsRange The native range object to be wrapped.
     */
    IERange(NativeRange jsRange)
    {
        super(jsRange);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#compareBoundaryPoints(RangeCompare, Range)
     */
    public short compareBoundaryPoints(RangeCompare how, Range sourceRange)
    {
        return compareBoundaryPoints(how, IERangeFactory.cast(sourceRange).getJSRange());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRange#compareBoundaryPoints(RangeCompare, com.google.gwt.core.client.JavaScriptObject)
     */
    protected short compareBoundaryPoints(RangeCompare how, NativeRange sourceRange)
    {
        if (getJSRange() instanceof TextRange) {
            TextRange textRange = (TextRange) getJSRange();
            if (sourceRange instanceof TextRange) {
                return textRange.compareEndPoints(how, (TextRange) sourceRange);
            } else {
                // TODO
                throw new UnsupportedOperationException();
            }
        } else {
            if (sourceRange instanceof TextRange) {
                // TODO
                throw new UnsupportedOperationException();
            } else {
                // TODO
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneContents()
     */
    public DocumentFragment cloneContents()
    {
        if (getJSRange() instanceof TextRange) {
            // TODO
            throw new UnsupportedOperationException();
        } else {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#cloneRange()
     */
    public Range cloneRange()
    {
        NativeRange clone;
        if (getJSRange() instanceof TextRange) {
            clone = ((TextRange) getJSRange()).duplicate();
        } else {
            ControlRange controlRange = (ControlRange) getJSRange();
            ControlRange controlRangeClone = ControlRange.newInstance(controlRange.getOwnerDocument());
            for (int i = 0; i < controlRange.getLength(); i++) {
                controlRangeClone.add(controlRange.get(i));
            }
            clone = controlRangeClone;
        }
        return IERangeFactory.createRange(clone);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#collapse(boolean)
     */
    public void collapse(boolean toStart)
    {
        if (getJSRange() instanceof TextRange) {
            ((TextRange) getJSRange()).collapse(toStart);
        } else {
            TextRange textRange = TextRange.newInstance((ControlRange) getJSRange());
            textRange.collapse(toStart);
            setJSRange(textRange);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#deleteContents()
     */
    public void deleteContents()
    {
        if (getJSRange() instanceof TextRange) {
            ((TextRange) getJSRange()).setHTML("");
        } else {
            ControlRange range = (ControlRange) getJSRange();
            for (int i = 0; i < range.getLength(); i++) {
                Element element = range.get(i);
                if (element.getParentNode() != null) {
                    element.getParentNode().removeChild(element);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#detach()
     */
    public void detach()
    {
        if (getJSRange() instanceof TextRange) {
            // TODO
            throw new UnsupportedOperationException();
        } else {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#extractContents()
     */
    public DocumentFragment extractContents()
    {
        if (getJSRange() instanceof TextRange) {
            // TODO
            throw new UnsupportedOperationException();
        } else {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getCommonAncestorContainer()
     */
    public Node getCommonAncestorContainer()
    {
        if (getJSRange() instanceof TextRange) {
            Node startContainer = getStartContainer();
            // If the range is within a text node then the common ancestor container is that text node.
            if (startContainer.getNodeType() == Node.TEXT_NODE
                && (isCollapsed() || startContainer == getEndContainer())) {
                return startContainer;
            } else {
                return ((TextRange) getJSRange()).getParentElement();
            }
        } else {
            return ((ControlRange) getJSRange()).get(0).getParentNode();
        }
    }

    /**
     * Utility method for retrieving the sibling of the given node in the specified direction. If right is true the next
     * sibling is returned. Otherwise, the previous sibling is returned. (reduces cyclomatic complexity)
     * 
     * @param node The node whose sibling will be returned.
     * @param right The direction to look for the sibling.
     * @return The sibling of the specified node, in the specified direction.
     */
    private Node getSibling(Node node, boolean right)
    {
        return right ? node.getNextSibling() : node.getPreviousSibling();
    }

    /**
     * Returns the locator for the specified end point.
     * 
     * @param start Specifies the end point.
     * @return the locator of the start point, if start is true, or the locator of the end point, otherwise.
     */
    private EndPointLocator getEndPointLocator(boolean start)
    {
        // We use a collapsed text range to iterate through nodes.
        TextRange cursor = ((TextRange) getJSRange()).duplicate();
        cursor.collapse(start);
        Element parent = cursor.getParentElement();

        // We iterate through neighbor nodes as long as we can and till the parent node changes.
        // We count the number of possible-cursor-positions we jump.
        int offset = 0;
        int unitCount = start ? -1 : 1;
        while (cursor.move(Unit.CHARACTER, unitCount) != 0 && cursor.getParentElement() == parent) {
            offset++;
        }

        Node base;
        Node currentParent = cursor.getParentElement();
        if (currentParent == parent || currentParent.getParentNode() != parent) {
            // We stopped because we couldn't move anymore or because we jumped outside of the parent node.
            if (parent.hasChildNodes()) {
                base = start ? parent.getFirstChild() : parent.getLastChild();
            } else {
                // The range is inside an empty element. It must be collapsed.
                // The offset should be 0.
                assert (offset == 0);
                base = parent;
            }
        } else {
            // We stopped because we entered inside a child element of the parent node.
            base = currentParent;
            // We need to jump over this node too.
            offset++;
        }

        return new EndPointLocator(base, offset, !start);
    }

    /**
     * Returns the container of the specified end point. The container is deepest DOM node which contains the end point.
     * 
     * @param start Specifies the end point.
     * @return the container of the range's start , if start is true, or the container of the range's end, otherwise.
     */
    private Node getEndPointContainer(boolean start)
    {
        EndPointLocator locator = getEndPointLocator(start);
        Node sibling = locator.getBase();
        int offset = locator.getOffset();

        // Between sibling and the end point container there are only text nodes and elements inside which we cannot
        // position the cursor.
        while (sibling != null) {
            if (sibling.getNodeType() == Node.TEXT_NODE) {
                if (offset <= sibling.getNodeValue().length()) {
                    // We cannot move further to the left.
                    return sibling;
                } else {
                    // We jump over this text node.
                    offset -= sibling.getNodeValue().length();
                    sibling = getSibling(sibling, start);
                }
            } else if (--offset <= 0) {
                // The current sibling is an element
                // We can position the cursor between elements so we must decrease the number of
                // possible-cursor-positions.

                // The end point is before or after the current sibling.
                Node nextSibling = getSibling(sibling, start);
                // We prefer that the end point is inside a text node
                if (nextSibling == null || nextSibling.getNodeType() == Node.ELEMENT_NODE) {
                    // The end point is before the first child, after the last child or between elements.
                    return sibling.getParentNode();
                } else {
                    sibling = nextSibling;
                }
            }
        }
        // We should not get here.
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndContainer()
     */
    public Node getEndContainer()
    {
        if (getJSRange() instanceof TextRange) {
            return getEndPointContainer(false);
        } else {
            // ControlRange
            return getCommonAncestorContainer();
        }
    }

    /**
     * Returns the offset of the specified end point within its container. The container is deepest DOM node which
     * contains the end point.
     * 
     * @param start Specifies the end point.
     * @return the offset of the range's start , if start is true, or the offset of the range's end, otherwise.
     */
    private int getEndPointOffset(boolean start)
    {
        EndPointLocator locator = getEndPointLocator(start);
        Node sibling = locator.getBase();
        int offset = locator.getOffset();

        if (offset == 0 && sibling.getNodeType() == Node.ELEMENT_NODE) {
            // The end point is before the first child or after the last child of the parent node.
            return start ? 0 : sibling.getParentNode().getChildNodes().getLength();
        }

        // Between the found sibling and the end point container there are only text nodes and elements inside which we
        // cannot position the cursor.
        while (sibling != null) {
            if (sibling.getNodeType() == Node.TEXT_NODE) {
                if (offset <= sibling.getNodeValue().length()) {
                    // We cannot move further to the left.
                    return start ? offset : sibling.getNodeValue().length() - offset;
                }
                // We jump over this text node.
                offset -= sibling.getNodeValue().length();
                sibling = getSibling(sibling, start);
            } else if (--offset == 0) {
                // The current sibling is an element
                // We can position the cursor between elements so we must decrease the number of
                // possible-cursor-positions.

                // The range ends before the current sibling.
                Node nextSibling = getSibling(sibling, start);
                // We prefer that the end point is inside a text node.
                if (nextSibling == null || nextSibling.getNodeType() == Node.ELEMENT_NODE) {
                    // The end point is before the first child, after the last child or between elements.
                    return DOMUtils.getInstance().getNodeIndex(sibling) + (start ? 1 : 0);
                }
                sibling = nextSibling;
            }
        }
        // We should not get here.
        return -1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndOffset()
     */
    public int getEndOffset()
    {
        if (getJSRange() instanceof TextRange) {
            return getEndPointOffset(false);
        } else {
            // ControlRange
            return 1 + getStartOffset();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartContainer()
     */
    public Node getStartContainer()
    {
        if (getJSRange() instanceof TextRange) {
            return getEndPointContainer(true);
        } else {
            // ControlRange
            return getCommonAncestorContainer();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getStartOffset()
     */
    public int getStartOffset()
    {
        if (getJSRange() instanceof TextRange) {
            return getEndPointOffset(true);
        } else {
            return DOMUtils.getInstance().getNodeIndex(((ControlRange) getJSRange()).get(0));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#insertNode(Node)
     */
    public void insertNode(Node newNode)
    {
        if (getJSRange() instanceof TextRange) {
            // TODO
            throw new UnsupportedOperationException();
        } else {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#isCollapsed()
     */
    public boolean isCollapsed()
    {
        if (getJSRange() instanceof TextRange) {
            TextRange textRange = (TextRange) getJSRange();
            return textRange.getText().length() == 0 && textRange.getHTML().length() == 0;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNode(Node)
     */
    public void selectNode(Node refNode)
    {
        if (getJSRange() instanceof TextRange) {
            // TODO
            throw new UnsupportedOperationException();
        } else {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNodeContents(Node)
     */
    public void selectNodeContents(Node refNode)
    {
        if (getJSRange() instanceof TextRange) {
            TextRange textRange = (TextRange) getJSRange();
            if (refNode.getNodeType() == Node.ELEMENT_NODE) {
                textRange.moveToElementText(Element.as(refNode));
            } else if (refNode.getNodeType() == Node.TEXT_NODE) {
                textRange.moveToTextNode(Text.as(refNode));
            }
        } else {
            // ControlRange
            collapse(true);
            selectNodeContents(refNode);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEnd(Node, int)
     */
    public void setEnd(Node refNode, int offset)
    {
        if (getJSRange() instanceof TextRange) {
            if (refNode.getNodeType() == Node.ELEMENT_NODE) {
                if (offset < refNode.getChildNodes().getLength()) {
                    setEndBefore(refNode.getChildNodes().getItem(offset));
                } else {
                    setEndAfter(refNode.getLastChild());
                }
            } else if (refNode.getNodeType() == Node.TEXT_NODE) {
                ((TextRange) getJSRange()).setEndPoint(RangeCompare.START_TO_END, refNode, offset);
            }
        } else {
            collapse(true);
            setEnd(refNode, offset);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndAfter(Node)
     */
    public void setEndAfter(Node refNode)
    {
        if (getJSRange() instanceof TextRange) {
            ((TextRange) getJSRange()).setEndPoint(RangeCompare.END_TO_END, refNode);
        } else {
            collapse(true);
            setEndAfter(refNode);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setEndBefore(Node)
     */
    public void setEndBefore(Node refNode)
    {
        if (getJSRange() instanceof TextRange) {
            ((TextRange) getJSRange()).setEndPoint(RangeCompare.START_TO_END, refNode);
        } else {
            collapse(true);
            setEndBefore(refNode);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStart(Node, int)
     */
    public void setStart(Node refNode, int offset)
    {
        if (getJSRange() instanceof TextRange) {
            if (refNode.getNodeType() == Node.ELEMENT_NODE) {
                if (offset < refNode.getChildNodes().getLength()) {
                    setStartBefore(refNode.getChildNodes().getItem(offset));
                } else {
                    setStartAfter(refNode.getLastChild());
                }
            } else if (refNode.getNodeType() == Node.TEXT_NODE) {
                ((TextRange) getJSRange()).setEndPoint(RangeCompare.START_TO_START, refNode, offset);
            }
        } else {
            collapse(false);
            setStart(refNode, offset);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartAfter(Node)
     */
    public void setStartAfter(Node refNode)
    {
        if (getJSRange() instanceof TextRange) {
            ((TextRange) getJSRange()).setEndPoint(RangeCompare.END_TO_START, refNode);
        } else {
            collapse(false);
            setStartAfter(refNode);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#setStartBefore(Node)
     */
    public void setStartBefore(Node refNode)
    {
        if (getJSRange() instanceof TextRange) {
            ((TextRange) getJSRange()).setEndPoint(RangeCompare.START_TO_START, refNode);
        } else {
            collapse(false);
            setStartBefore(refNode);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#surroundContents(Node)
     */
    public void surroundContents(Node newParent)
    {
        if (getJSRange() instanceof TextRange) {
            // TODO
            throw new UnsupportedOperationException();
        } else {
            setJSRange(TextRange.newInstance((ControlRange) getJSRange()));
            surroundContents(newParent);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#toString()
     */
    public String toString()
    {
        if (getJSRange() instanceof TextRange) {
            return ((TextRange) getJSRange()).getText();
        } else {
            // ControlRange
            return "";
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#toHTML()
     */
    public String toHTML()
    {
        if (getJSRange() instanceof TextRange) {
            return ((TextRange) getJSRange()).getHTML();
        } else {
            return ((ControlRange) getJSRange()).get(0).getString();
        }
    }
}
