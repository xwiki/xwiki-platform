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
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;
import com.xpn.xwiki.wysiwyg.client.dom.Text;
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
        return compareBoundaryPoints(how, ((IERange) sourceRange).getJSRange());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRange#compareBoundaryPoints(RangeCompare, com.google.gwt.core.client.JavaScriptObject)
     */
    protected short compareBoundaryPoints(RangeCompare how, NativeRange sourceRange)
    {
        if (getJSRange().isTextRange()) {
            TextRange textRange = (TextRange) getJSRange();
            if (sourceRange.isTextRange()) {
                return textRange.compareEndPoints(how, (TextRange) sourceRange);
            } else {
                return textRange.compareEndPoints(how, TextRange.newInstance((ControlRange) sourceRange));
            }
        } else {
            if (sourceRange.isTextRange()) {
                return TextRange.newInstance((ControlRange) getJSRange())
                    .compareEndPoints(how, (TextRange) sourceRange);
            } else {
                return TextRange.newInstance((ControlRange) getJSRange()).compareEndPoints(how,
                    TextRange.newInstance((ControlRange) sourceRange));
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
        if (getJSRange().isTextRange()) {
            return super.cloneContents();
        } else {
            ControlRange controlRange = (ControlRange) getJSRange();
            DocumentFragment contents = controlRange.getOwnerDocument().createDocumentFragment();
            for (int i = 0; i < controlRange.getLength(); i++) {
                contents.appendChild(controlRange.get(i).cloneNode(true));
            }
            return contents;
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
        if (getJSRange().isTextRange()) {
            clone = ((TextRange) getJSRange()).duplicate();
        } else {
            ControlRange controlRange = (ControlRange) getJSRange();
            ControlRange controlRangeClone = ControlRange.newInstance(controlRange.getOwnerDocument());
            for (int i = 0; i < controlRange.getLength(); i++) {
                controlRangeClone.add(controlRange.get(i));
            }
            clone = controlRangeClone;
        }
        return new IERange(clone);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#collapse(boolean)
     */
    public void collapse(boolean toStart)
    {
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
            super.deleteContents();
        } else {
            ControlRange range = (ControlRange) getJSRange();
            Element element = range.get(0);
            collapse(true);
            if (element.getParentNode() != null) {
                element.getParentNode().removeChild(element);
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
            return super.getCommonAncestorContainer();
        } else {
            return ((ControlRange) getJSRange()).get(0).getParentNode();
        }
    }

    /**
     * Returns the container of the specified end point. The container is deepest DOM node which contains the end point.
     * 
     * @param start Specifies the end point.
     * @return the container of the range's start , if start is true, or the container of the range's end, otherwise.
     */
    private Node getEndPointContainer(boolean start)
    {
        TextRange currentRange = getJSRange().cast();
        TextRange refRange = currentRange.duplicate();
        refRange.collapse(start);
        Element parent = refRange.getParentElement();
        RangeCompare compareStart = RangeCompare.valueOf(true, start);
        RangeCompare compareEnd = RangeCompare.valueOf(false, start);
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                refRange.moveToTextNode(Text.as(child));
                if (currentRange.compareEndPoints(compareStart, refRange) >= 0
                    && currentRange.compareEndPoints(compareEnd, refRange) <= 0) {
                    return child;
                }
            }
            child = child.getNextSibling();
        }
        return parent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#getEndContainer()
     */
    public Node getEndContainer()
    {
        if (getJSRange().isTextRange()) {
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
        Node container = getEndPointContainer(start);
        TextRange currentRange = getJSRange().cast();
        TextRange refRange = currentRange.duplicate();
        RangeCompare whichEndPoints = RangeCompare.valueOf(start, true);
        if (container.getNodeType() == Node.TEXT_NODE) {
            refRange.moveToTextNode(Text.as(container));
            while (refRange.compareEndPoints(whichEndPoints, currentRange) < 0 && refRange.getText().length() > 0) {
                refRange.moveStart(Unit.CHARACTER, 1);
            }
            return container.getNodeValue().length() - refRange.getText().length();
        } else {
            int offset = 0;
            Node child = container.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    refRange.moveToElementText(Element.as(child));
                    if (refRange.compareEndPoints(whichEndPoints, currentRange) >= 0) {
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
     * {@inheritDoc}
     * 
     * @see Range#getEndOffset()
     */
    public int getEndOffset()
    {
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
            return super.isCollapsed();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#selectNodeContents(Node)
     */
    public void selectNodeContents(Node refNode)
    {
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
            switch (refNode.getNodeType()) {
                case Node.ELEMENT_NODE:
                    if (offset > 0) {
                        setEndAfter(refNode.getChildNodes().getItem(offset - 1));
                    } else if (refNode.hasChildNodes()) {
                        // The refNode must have child nodes if it is going to be the end container (following W3C Range
                        // specification), but there is a special case (see below).
                        setEndBefore(refNode.getFirstChild());
                    } else {
                        // This is a special case in IE when the body element is empty. This can happen when the rich
                        // text area has no text inside.
                        setEndAfter(refNode);
                    }
                    break;
                case Node.TEXT_NODE:
                    ((TextRange) getJSRange()).setEndPoint(RangeCompare.START_TO_END, refNode, offset);
                    break;
                case 4:
                    // CDATA
                case 8:
                    // COMMENT
                    // Since IE's text range cannot end inside a comment or CDATA node, we place the end point after.
                    setEndAfter(refNode);
                    break;
                default:
                    throw new IllegalArgumentException(DOMUtils.UNSUPPORTED_NODE_TYPE);
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
            switch (refNode.getNodeType()) {
                case Node.ELEMENT_NODE:
                    if (offset < refNode.getChildNodes().getLength()) {
                        setStartBefore(refNode.getChildNodes().getItem(offset));
                    } else if (refNode.hasChildNodes()) {
                        // The refNode must have child nodes if it is going to be the start container (following W3C
                        // Range specification), but there is a special case (see below).
                        setStartAfter(refNode.getLastChild());
                    } else {
                        // This is a special case in IE when the body element is empty. This can happen when the rich
                        // text area has no text inside.
                        setStartBefore(refNode);
                    }
                    break;
                case Node.TEXT_NODE:
                    ((TextRange) getJSRange()).setEndPoint(RangeCompare.START_TO_START, refNode, offset);
                    break;
                case 4:
                    // CDATA
                case 8:
                    // COMMENT
                    // Since IE's text range cannot start inside a comment or CDATA node, we place the start point
                    // before.
                    setEndBefore(refNode);
                    break;
                default:
                    throw new IllegalArgumentException(DOMUtils.UNSUPPORTED_NODE_TYPE);
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
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
        if (getJSRange().isTextRange()) {
            return ((TextRange) getJSRange()).getText();
        } else {
            // ControlRange
            return "";
        }
    }
}
