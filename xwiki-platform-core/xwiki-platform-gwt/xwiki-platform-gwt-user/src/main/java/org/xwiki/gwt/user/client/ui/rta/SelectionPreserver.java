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
package org.xwiki.gwt.user.client.ui.rta;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;

/**
 * Most of the plugins alter the DOM document edited with the rich text area by executing commands on the current
 * selection (thus on the current range). In some cases, a plugin needs to get user input before executing such a
 * command. It can gather the needed information by opening a dialog, for instance. In some browsers this may lead to
 * loosing the selection on the rich text area. In this case the plugin has to {@link #saveSelection()} before the
 * dialog is shown and {@link #restoreSelection()} after the dialog is closed.
 * <p>
 * The selection preserver can be also used to restore the selection after executing commands like insert HTML. In this
 * particular case the restored selection will contain the HTML that replaced the previous selection.
 * 
 * @version $Id$
 */
public class SelectionPreserver
{
    /**
     * Marks the end points of a range within its document so that it can be restored.
     */
    private static class RangePlaceHolder
    {
        /**
         * Marks the left boundary of the range.
         */
        private final Node start;

        /**
         * If it's greater than zero then it specifies the left offset within the next sibling of the start marker.
         * Otherwise the range starts immediately after the start marker.
         */
        private final int startOffset;

        /**
         * Marks the right boundary of the range.
         */
        private final Node end;

        /**
         * If it's greater than zero then it specifies the right offset within the previous sibling of the end marker.
         * Otherwise the range ends just before the end marker.
         */
        private final int endOffset;

        /**
         * Marks the end points of the given range within its document so that it can be restored.
         * 
         * @param range The range to be marked within its document.
         */
        RangePlaceHolder(Range range)
        {
            DOMUtils domUtils = DOMUtils.getInstance();
            Node startContainer = range.getStartContainer();
            Node endContainer = range.getEndContainer();

            end = createMarker((Document) startContainer.getOwnerDocument());
            if (endContainer.getNodeType() == Node.ELEMENT_NODE) {
                domUtils.insertAt(endContainer, end, range.getEndOffset());
                endOffset = -1;
            } else {
                domUtils.insertAfter(end, endContainer);
                endOffset = domUtils.getLength(endContainer) - range.getEndOffset();
            }

            start = end.cloneNode(true);
            if (startContainer.getNodeType() == Node.ELEMENT_NODE) {
                domUtils.insertAt(startContainer, start, range.getStartOffset());
                startOffset = -1;
            } else {
                startContainer.getParentNode().insertBefore(start, startContainer);
                startOffset = range.getStartOffset();
            }
        }

        /**
         * @return {@link #start}
         */
        public Node getStart()
        {
            return start;
        }

        /**
         * @return {@link #startOffset}
         */
        public int getStartOffset()
        {
            return startOffset;
        }

        /**
         * @return {@link #end}
         */
        public Node getEnd()
        {
            return end;
        }

        /**
         * @return {@link #endOffset}
         */
        public int getEndOffset()
        {
            return endOffset;
        }

        /**
         * Specifies if the start/end boundary is between DOM nodes or inside a text node.
         * 
         * @param startBoundary which boundary
         * @return {@code true} if the specified boundary is between DOM nodes, {@code false} otherwise
         */
        public boolean hasBoundaryBetweenNodes(boolean startBoundary)
        {
            Node container = startBoundary ? start.getNextSibling() : end.getPreviousSibling();
            int offset = startBoundary ? startOffset : endOffset;
            return offset < 0 || container == null || container.getNodeType() == Node.ELEMENT_NODE;
        }

        /**
         * @param document the document which includes the range.
         * @return a new DOM node to be used as a marker for a DOM Range boundary.
         */
        private static Node createMarker(Document document)
        {
            // We're using a source-less image node as a range boundary marker for two reasons:
            // * an image node cannot have child nodes so a range boundary cannot fall inside an image node.
            // * most web browsers (including FF and IE) allow a range boundary to be placed before and after an image
            // node.
            ImageElement img = document.createImageElement();
            // We have to make sure the range boundary markers don't appear in rich text area's HTML
            img.setAttribute(Element.META_DATA_ATTR, "");
            img.setWidth(0);
            img.setHeight(0);
            return img;
        }
    }

    /**
     * The rich text area whose selection is being preserved.
     */
    private final RichTextArea rta;

    /**
     * The list of saved range place holders.
     */
    private final List<RangePlaceHolder> placeHolders = new ArrayList<RangePlaceHolder>();

    /**
     * Creates a new selection preserver for the specified rich text area.
     * 
     * @param rta The rich text area whose selection will be preserved.
     */
    public SelectionPreserver(RichTextArea rta)
    {
        this.rta = rta;
    }

    /**
     * Removes the markers from the document and clears the list of saved range place holders.
     */
    public void clearSelection()
    {
        for (int i = 0; i < placeHolders.size(); i++) {
            RangePlaceHolder placeHolder = placeHolders.get(i);
            DOMUtils.getInstance().detach(placeHolder.getStart());
            DOMUtils.getInstance().detach(placeHolder.getEnd());
        }
        placeHolders.clear();
    }

    /**
     * Saves the current selection for later changes. The selection is taken from the underlying rich text area.
     * 
     * @see #restoreSelection()
     */
    public void saveSelection()
    {
        clearSelection();
        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            placeHolders.add(new RangePlaceHolder(selection.getRangeAt(i)));
        }
        // We need to restore the selection because it might have been affected by the range boundary markers we have
        // inserted. Of course, we don't reset the state of the preserver.
        restoreSelection(false);
    }

    /**
     * @return {@code true} if there is a saved selection, {@code false} otherwise
     */
    public boolean hasSelection()
    {
        return !placeHolders.isEmpty();
    }

    /**
     * Restores the saved selection on the underlying rich text area and resets the state of the selection preserver.
     * 
     * @see #saveSelection()
     */
    public void restoreSelection()
    {
        restoreSelection(true);
    }

    /**
     * If there is a saved selection restores it, otherwise restores the default selection on the underlying rich text
     * area. You can specify if the selection preserver should reset its state. When you're calling this method multiple
     * times for the same selection it's more convenient to keep the state between the calls rather than call
     * {@link #saveSelection()} before.
     * 
     * @param reset true if you want to reset the state of the selection preserver, false otherwise.
     * @see #saveSelection()
     */
    public void restoreSelection(boolean reset)
    {
        if (placeHolders.isEmpty()) {
            restoreDefaultSelection();
        } else {
            restoreSavedSelection(reset);
        }
    }

    /**
     * Restores the default selection: places the caret at the beginning of the document.
     */
    private void restoreDefaultSelection()
    {
        Range range = rta.getDocument().createRange();
        Node firstLeaf = DOMUtils.getInstance().getFirstLeaf(rta.getDocument().getBody());
        if (firstLeaf.getNodeType() == Node.ELEMENT_NODE && Element.as(firstLeaf).canHaveChildren()) {
            range.selectNodeContents(firstLeaf);
        } else {
            range.setStartBefore(firstLeaf);
            range.collapse(true);
        }
        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * Restores the saved selection on the underlying rich text area. You can specify if the selection preserver should
     * reset its state. When you're calling this method multiple times for the same selection it's more convenient to
     * keep the state between the calls rather than call {@link #saveSelection()} before.
     * 
     * @param reset true if you want to reset the state of the selection preserver, false otherwise.
     * @see #saveSelection()
     */
    private void restoreSavedSelection(boolean reset)
    {
        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        int delta = reset ? 0 : 1;
        for (int i = 0; i < placeHolders.size(); i++) {
            RangePlaceHolder placeHolder = placeHolders.get(i);

            Node startContainer = placeHolder.getStart().getNextSibling();
            int startOffset = placeHolder.getStartOffset();
            if (placeHolder.hasBoundaryBetweenNodes(true)) {
                startContainer = placeHolder.getStart().getParentNode();
                startOffset = DOMUtils.getInstance().getNodeIndex(placeHolder.getStart()) + delta;
            }
            if (reset) {
                DOMUtils.getInstance().detach(placeHolder.getStart());
            }

            Node endContainer = placeHolder.getEnd().getPreviousSibling();
            int endOffset;
            if (placeHolder.hasBoundaryBetweenNodes(false)) {
                endContainer = placeHolder.getEnd().getParentNode();
                endOffset = DOMUtils.getInstance().getNodeIndex(placeHolder.getEnd());
            } else {
                endOffset = DOMUtils.getInstance().getLength(endContainer) - placeHolder.getEndOffset();
            }
            if (reset) {
                DOMUtils.getInstance().detach(placeHolder.getEnd());
            }

            Range range = rta.getDocument().createRange();
            range.setStart(startContainer, startOffset);
            range.setEnd(endContainer, endOffset);
            selection.addRange(range);
        }
        if (reset) {
            placeHolders.clear();
        }
    }
}
