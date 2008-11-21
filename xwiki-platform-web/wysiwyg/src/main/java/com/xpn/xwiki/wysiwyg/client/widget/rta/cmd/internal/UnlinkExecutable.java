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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Executable for the unlink command, to remove a link in the wiki document. The following rules apply:
 * <ul>
 * <li>if there is no selection and the cursor is inside a wikilink, remove the enclosing link</li>
 * <li>if there is a selection which is fully inside or equal to a wikilink, remove the enclosing link</li>
 * <li>the command is not enabled in any other situation</li>
 * </ul>
 * 
 * @version $Id$
 */
public class UnlinkExecutable implements Executable
{
    /**
     * Hold the name of the anchor tag.
     */
    private static final String ANCHOR = "a";

    /**
     * Constant for the comment node type.
     */
    private static final short NODE_TYPE_COMMENT = 8;

    /**
     * {@inheritDoc}
     */
    public boolean execute(RichTextArea rta, String param)
    {
        // Get the wrapping anchor
        Range range = DOMUtils.getInstance().getTextRange(rta.getDocument().getSelection().getRangeAt(0));
        Node anchor = DOMUtils.getInstance().getFirstAncestor(range.getCommonAncestorContainer(), ANCHOR);
        if (anchor == null) {
            return false;
        }
        // check its parent, if it's a span
        Node wrappingSpan = anchor.getParentNode();
        if (wrappingSpan == null || wrappingSpan.getNodeType() != Node.ELEMENT_NODE
            || !wrappingSpan.getNodeName().equalsIgnoreCase("span")) {
            // we have no span wrapping the anchor, is not a wikilink
            boolean result = removeAnchor(Element.as(anchor), rta, range);
        }
        // check the span class
        String spanClass = Element.as(wrappingSpan).getClassName();
        if (!spanClass.equalsIgnoreCase("wikilink") && !spanClass.equalsIgnoreCase("wikiexternallink")
            && !spanClass.equalsIgnoreCase("wikicreatelink")) {
            // we haven't the expected class on the span
            return removeAnchor(Element.as(anchor), rta, range);
        }
        // check the comments before and after
        Node nodeBefore = wrappingSpan.getPreviousSibling();
        Node nodeAfter = wrappingSpan.getNextSibling();
        if (nodeBefore == null || nodeAfter == null || nodeBefore.getNodeType() != NODE_TYPE_COMMENT
            || nodeAfter.getNodeType() != NODE_TYPE_COMMENT) {
            // missing or wrong sibling nodes for the wikilink span
            return removeAnchor(Element.as(anchor), rta, range);
        }
        if (nodeBefore.getNodeValue().startsWith("startwikilink:")
            && nodeAfter.getNodeValue().equalsIgnoreCase("stopwikilink")) {
            // WOUHOU! found wikilink, remove it
            return removeWikiLink(nodeBefore, Element.as(anchor), rta, range);
        }
        return removeAnchor(Element.as(anchor), rta, range);
    }

    /**
     * @param anchor the anchor to return the text range from
     * @param rta the {@link RichTextArea} whose document we're using to create and extract ranges
     * @return the textrange inside the passed the anchor.
     */
    private Range getAnchorTextRange(Element anchor, RichTextArea rta)
    {
        // get anchor's text range
        Range anchorRange = rta.getDocument().createRange();
        anchorRange.setStart(anchor, 0);
        anchorRange.setEnd(anchor, anchor.getChildNodes().getLength());
        Range anchorTextRange = DOMUtils.getInstance().getTextRange(anchorRange);
        return anchorTextRange;
    }

    /**
     * Removes an anchor by replacing it with all its children.
     * 
     * @param anchor the anchor to remove
     * @param rta the rich text area to remove the anchor from
     * @param textRange the text area's original selection text range. Passed here just for cache purposes.
     * @return the result of this operation
     */
    private boolean removeAnchor(Element anchor, RichTextArea rta, Range textRange)
    {
        Range anchorTextRange = getAnchorTextRange(anchor, rta);
        boolean isBeginning = textRange.compareBoundaryPoints(RangeCompare.START_TO_START, anchorTextRange) <= 0;
        boolean isEnd = textRange.compareBoundaryPoints(RangeCompare.END_TO_END, anchorTextRange) >= 0;
        if (textRange.isCollapsed() && (isBeginning || isEnd)) {
            // cursor it's at the beginning or at the end, move it out of the anchor
            Range newRange = rta.getDocument().createRange();
            if (isBeginning) {
                newRange.setStartBefore(anchor);
            }
            if (isEnd) {
                newRange.setStartAfter(anchor);
            }
            newRange.collapse(true);
            // now set it on the document
            rta.getDocument().getSelection().removeAllRanges();
            rta.getDocument().getSelection().addRange(newRange);
            return true;
        }
        // the selection is either not collapsed, not at the beginning or end. Remove the link
        // Store the current selection
        Node startNode = textRange.getStartContainer();
        int startOffset = textRange.getStartOffset();
        Node endNode = textRange.getEndContainer();
        int endOffset = textRange.getEndOffset();
        // Remove all children from the anchor and insert them before the anchor.
        Node currentNode = anchor.getFirstChild();
        while (currentNode != null) {
            anchor.removeChild(currentNode);
            anchor.getParentNode().insertBefore(currentNode, anchor);
            currentNode = anchor.getFirstChild();
        }
        // remove the anchor from its parent
        anchor.getParentElement().removeChild(anchor);
        // restore the selection of the rta to the original text range
        Range newRange = rta.getDocument().createRange();
        newRange.setStart(startNode, startOffset);
        newRange.setEnd(endNode, endOffset);
        rta.getDocument().getSelection().removeAllRanges();
        rta.getDocument().getSelection().addRange(newRange);
        return true;
    }

    /**
     * Removes a wikilink by replacing it with the contents of the passed anchor. The wikilink is given by the
     * startComment, considering that, along with the next 2 nodes, it represents the wikilink.
     * 
     * @param startComment the start comment of this wikilink
     * @param anchor the anchor enclosed by this wikilink
     * @param rta the rich text area to remove the wikilink from
     * @param textRange the text area's original selection text range. Passed here just for cache purposes.
     * @return the result of the replacement
     */
    private boolean removeWikiLink(Node startComment, Element anchor, RichTextArea rta, Range textRange)
    {
        Node spanNode = startComment.getNextSibling();
        if (spanNode == null) {
            return false;
        }
        Node endComment = spanNode.getNextSibling();
        if (endComment == null) {
            return false;
        }
        Range anchorTextRange = getAnchorTextRange(anchor, rta);
        boolean isBeginning = textRange.compareBoundaryPoints(RangeCompare.START_TO_START, anchorTextRange) <= 0;
        boolean isEnd = textRange.compareBoundaryPoints(RangeCompare.END_TO_END, anchorTextRange) >= 0;
        if (textRange.isCollapsed() && (isBeginning || isEnd)) {
            // cursor it's at the beginning or at the end, move it out of the anchor
            Range newRange = rta.getDocument().createRange();
            if (isBeginning) {
                newRange.setStartBefore(startComment);
            }
            if (isEnd) {
                newRange.setStartAfter(endComment);
            }
            newRange.collapse(true);
            // now set it on the document
            rta.getDocument().getSelection().removeAllRanges();
            rta.getDocument().getSelection().addRange(newRange);
            return true;
        }
        // Selection is not collapsed, not at the beginning or the end, move it out

        // Store the current selection
        Node startNode = textRange.getStartContainer();
        int startOffset = textRange.getStartOffset();
        Node endNode = textRange.getEndContainer();
        int endOffset = textRange.getEndOffset();
        // now start moving contents of the anchor outside
        Node currentNode = anchor.getFirstChild();
        while (currentNode != null) {
            anchor.removeChild(currentNode);
            startComment.getParentNode().insertBefore(currentNode, startComment);
            currentNode = anchor.getFirstChild();
        }
        // Done, remove the comments & spans from the parent
        startComment.getParentNode().removeChild(startComment);
        spanNode.getParentNode().removeChild(spanNode);
        endComment.getParentNode().removeChild(endComment);
        // restore the selection of the rta to the original text range
        Range newRange = rta.getDocument().createRange();
        newRange.setStart(startNode, startOffset);
        newRange.setEnd(endNode, endOffset);
        rta.getDocument().getSelection().removeAllRanges();
        rta.getDocument().getSelection().addRange(newRange);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getParameter(RichTextArea rta)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled(RichTextArea rta)
    {
        // Check the selection, to be either void or inside a link.
        Range range = DOMUtils.getInstance().getTextRange(rta.getDocument().getSelection().getRangeAt(0));
        return DOMUtils.getInstance().getFirstAncestor(range.getCommonAncestorContainer(), ANCHOR) != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return !isEnabled(rta);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSupported(RichTextArea rta)
    {
        return true;
    }
}
