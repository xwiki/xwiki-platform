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
package org.xwiki.gwt.dom.client.internal;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.RangeCompare;

import com.google.gwt.dom.client.Node;

/**
 * This is a cross-browser implementation of the W3C Range specification.
 * <p>
 * Acknowledgment to Mozilla Foundation for making nsRange.cpp public.
 * 
 * @version $Id$
 * @see "http://hg.mozilla.org/mozilla-central/file/b945b4f67e7e/content/base/src/nsRange.cpp"
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void deleteContents()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        Node root = getCommonAncestorContainer();

        if (startContainer == endContainer) {
            domUtils.deleteNodeContents(root, startOffset, endOffset);
            // Take the range gravity into account.
            collapse(true);
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
                // Take the range gravity into account.
                setEnd(endContainer, 0);
            } else {
                // Take the range gravity into account.
                setEnd(root, startIndex);
            }
            domUtils.deleteNodeContents(root, startIndex, endIndex);
        }

        // At this point we should be checking for the case where we have 2 adjacent text nodes left, each containing
        // one of the range end points. The spec says the 2 nodes should be merged in that case, and to use Normalize()
        // to do the merging, but calling Normalize() on the common parent to accomplish this might also normalize nodes
        // that are outside the range but under the common parent. Need to verify with the range commitee members that
        // this was the desired behavior. For now we don't merge anything!
        // Filed as https://bugzilla.mozilla.org/show_bug.cgi?id=401276

        collapseRangeAfterDelete();
    }

    /**
     * Utility method that is used by {@link #deleteContents()} and {@link #extractContents()} to collapse the range in
     * the correct place, under the range's root container (the range end points common container) as outlined by the
     * Range spec: http://www.w3.org/TR/2000/REC-DOM-Level-2-Traversal-Range-20001113/ranges.html
     * <p>
     * The assumption made by this method is that the delete or extract has been done already, and left the range in a
     * state where there is no content between the 2 end points.
     */
    private void collapseRangeAfterDelete()
    {
        // Check if range gravity took care of collapsing the range for us!
        if (isCollapsed()) {
            // The range is collapsed so there's nothing for us to do.
            //
            // There are 2 possible scenarios here:
            //
            // 1. the range could've been collapsed prior to the delete/extract,
            // which would've resulted in nothing being removed, so the range
            // is already where it should be.
            //
            // 2. Prior to the delete/extract, the range's start and end were in
            // the same container which would mean everything between them
            // was removed, causing range gravity to collapse the range.
        } else {
            // The range isn't collapsed so figure out the appropriate place to collapse!
            Node commonAncestor = getCommonAncestorContainer();

            // Collapse to one of the end points if they are already in the
            // commonAncestor. This should work ok since this method is called
            // immediately after a delete or extract that leaves no content
            // between the 2 end points!
            if (startContainer == commonAncestor) {
                collapse(true);
            } else if (endContainer == commonAncestor) {
                collapse(false);
            } else {
                // End points are at differing levels. We want to collapse to the
                // point that is between the 2 subtrees that contain each point,
                // under the common ancestor.
                Node startAncestor = domUtils.getChild(commonAncestor, startContainer);
                selectNode(startAncestor);
                collapse(false);
            }
        }
    }

    @Override
    public void detach()
    {
        if (detached) {
            throw new IllegalStateException();
        }

        detached = true;
        setRange(null, 0, null, 0);
    }

    @Override
    public DocumentFragment extractContents()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        Node root = getCommonAncestorContainer();
        DocumentFragment contents;

        if (startContainer == endContainer) {
            contents = domUtils.extractNodeContents(root, startOffset, endOffset);
            // Take the range gravity into account.
            collapse(true);
        } else {
            contents = ((Document) root.getOwnerDocument()).createDocumentFragment();

            int startIndex = startOffset;
            if (startContainer != root) {
                contents.appendChild(domUtils.extractNode(root, startContainer, startOffset, false));
                startIndex = domUtils.getNodeIndex(domUtils.getChild(root, startContainer)) + 1;
            }

            if (endContainer != root) {
                int endIndex = domUtils.getNodeIndex(domUtils.getChild(root, endContainer));
                contents.appendChild(domUtils.extractNodeContents(root, startIndex, endIndex));
                contents.appendChild(domUtils.extractNode(root, endContainer, endOffset, true));
                // Take the range gravity into account.
                setEnd(endContainer, 0);
            } else {
                contents.appendChild(domUtils.extractNodeContents(root, startIndex, endOffset));
                // Take the range gravity into account.
                setEnd(root, startIndex);
            }
        }

        collapseRangeAfterDelete();

        return contents;
    }

    @Override
    public Node getCommonAncestorContainer()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        return domUtils.getNearestCommonAncestor(startContainer, endContainer);
    }

    @Override
    public Node getEndContainer()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return endContainer;
    }

    @Override
    public int getEndOffset()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return endOffset;
    }

    @Override
    public Node getStartContainer()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return startContainer;
    }

    @Override
    public int getStartOffset()
    {
        if (!positioned) {
            throw new IllegalStateException();
        }

        return startOffset;
    }

    @Override
    public void insertNode(Node newNode)
    {
        // Compute the number of nodes to insert.
        int delta = newNode.getNodeType() == DOMUtils.DOCUMENT_FRAGMENT_NODE ? newNode.getChildNodes().getLength() : 1;
        if (delta == 0) {
            // There's nothing to insert.
            return;
        }
        switch (startContainer.getNodeType()) {
            case DOMUtils.CDATA_NODE:
            case DOMUtils.COMMENT_NODE:
            case Node.TEXT_NODE:
                Node sibling = domUtils.splitNode(startContainer, startOffset);
                sibling.getParentNode().insertBefore(newNode, sibling);
                if (startContainer == endContainer) {
                    setEnd(sibling, endOffset - startOffset);
                } else if (startContainer.getParentNode() == endContainer) {
                    // Move the end with the number of inserted nodes plus 1 text node resulted after the split.
                    setEnd(endContainer, endOffset + delta + 1);
                }
                break;
            case Node.ELEMENT_NODE:
                domUtils.insertAt(startContainer, newNode, startOffset);
                if (startContainer == endContainer) {
                    // Move the end with the number of inserted nodes.
                    setEnd(endContainer, endOffset + delta);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public boolean isCollapsed()
    {
        if (detached || !positioned) {
            throw new IllegalStateException();
        }

        return startContainer == endContainer && startOffset == endOffset;
    }

    @Override
    public void selectNode(Node refNode)
    {
        Node parent = refNode.getParentNode();
        int index = domUtils.getNodeIndex(refNode);
        setRange(parent, index, parent, index + 1);
    }

    @Override
    public void selectNodeContents(Node refNode)
    {
        setRange(refNode, 0, refNode, domUtils.getLength(refNode));
    }

    @Override
    public void setEnd(Node refNode, int offset)
    {
        if (!positioned || startContainer.getOwnerDocument() != refNode.getOwnerDocument()
            || isAfterOrDisconnected(startContainer, startOffset, refNode, offset)) {
            setRange(refNode, offset, refNode, offset);
        } else {
            setRange(startContainer, startOffset, refNode, offset);
        }
    }

    @Override
    public void setEndAfter(Node refNode)
    {
        setEnd(refNode.getParentNode(), domUtils.getNodeIndex(refNode) + 1);
    }

    @Override
    public void setEndBefore(Node refNode)
    {
        setEnd(refNode.getParentNode(), domUtils.getNodeIndex(refNode));
    }

    @Override
    public void setStart(Node refNode, int offset)
    {
        if (!positioned || endContainer.getOwnerDocument() != refNode.getOwnerDocument()
            || isAfterOrDisconnected(refNode, offset, endContainer, endOffset)) {
            setRange(refNode, offset, refNode, offset);
        } else {
            setRange(refNode, offset, endContainer, endOffset);
        }
    }

    /**
     * Utility method for testing if one boundary point is after another or it they are disconnected.
     * 
     * @param alice first point's node
     * @param aliceOffset first point's offset
     * @param bob second point's node
     * @param bobOffset second point's offset
     * @return true if the first point is after the second point or if the given points are disconnected
     */
    private boolean isAfterOrDisconnected(Node alice, int aliceOffset, Node bob, int bobOffset)
    {
        try {
            return domUtils.comparePoints(alice, aliceOffset, bob, bobOffset) > 0;
        } catch (IllegalArgumentException e) {
            // The given boundary points are disconnected.
            return true;
        }
    }

    @Override
    public void setStartAfter(Node refNode)
    {
        setStart(refNode.getParentNode(), domUtils.getNodeIndex(refNode) + 1);
    }

    @Override
    public void setStartBefore(Node refNode)
    {
        setStart(refNode.getParentNode(), domUtils.getNodeIndex(refNode));
    }

    /**
     * The {@link #surroundContents(Node)} method raises an exception if the Range partially selects a non-Text node. An
     * example of a Range for which {@link #surroundContents(Node)} raises an exception is:
     * <code>&lt;FOO&gt;A<strong>B&lt;BAR&gt;C</strong>D&lt;/BAR&gt;E&lt;/FOO&gt;</code>
     * 
     * @return true if {@link #surroundContents(Node)} can be called on the current range
     */
    private boolean canSurroundContents()
    {
        if (startContainer == endContainer) {
            return true;
        }

        boolean startIsText = startContainer.getNodeType() == Node.TEXT_NODE;
        boolean endIsText = endContainer.getNodeType() == Node.TEXT_NODE;
        Node startParent = startContainer.getParentNode();
        Node endParent = endContainer.getParentNode();

        // I'm using the bitwise AND operator to reduce the cyclomatic complexity.
        boolean can = (startIsText & endIsText) && startParent != null && startParent == endParent;
        can = can || (startIsText && startParent != null && startParent == endContainer);
        return can || (endIsText && endParent != null && endParent == startContainer);
    }

    @Override
    public void surroundContents(Node newParent)
    {
        // The surroundContents() method raises an exception if the Range partially selects a non-Text node. An example
        // of a Range for which surroundContents() raises an exception is: <FOO>A|B<BAR>C|D</BAR>E</FOO>
        if (!canSurroundContents()) {
            throw new IllegalStateException();
        }

        // Extract the contents within the range.
        DocumentFragment contents = extractContents();

        // Spec says we need to remove all of newParent's children prior to insertion.
        Node child = newParent.getFirstChild();
        while (child != null) {
            newParent.removeChild(child);
            child = newParent.getFirstChild();
        }

        // Insert newParent at the range's start point.
        insertNode(newParent);

        // Append the contents we extracted under newParent.
        newParent.appendChild(contents);

        // Select newParent, and its contents.
        selectNode(newParent);
    }

    @Override
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

    @Override
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
