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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;

/**
 * Most of the plugins alter the DOM document edited with the rich text area by executing commands on the current
 * selection (thus on the current range). In some cases, a plugin needs to get user input before executing such a
 * command. It can gather the needed information by opening a dialog, for instance. In some browsers this may lead to
 * loosing the selection on the rich text area. In this case the plugin has to {@link #saveSelection()} before the
 * dialog is shown and {@link #restoreSelection()} after the dialog is closed.<br/>
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
        public RangePlaceHolder(Range range)
        {
            DOMUtils.getInstance().normalize(range);
            Node startContainer = range.getStartContainer();
            start = ((Document) startContainer.getOwnerDocument()).xCreateSpanElement();
            if (startContainer.getNodeType() == Node.ELEMENT_NODE) {
                DOMUtils.getInstance().insertAt(startContainer, start, range.getStartOffset());
                range.setStartAfter(start);
                startOffset = -1;
            } else {
                startContainer.getParentNode().insertBefore(start, startContainer);
                startOffset = range.getStartOffset();
            }
            Node endContainer = range.getEndContainer();
            end = start.cloneNode(true);
            if (endContainer.getNodeType() == Node.ELEMENT_NODE) {
                DOMUtils.getInstance().insertAt(endContainer, end, range.getEndOffset());
                range.setEndBefore(end);
                endOffset = -1;
            } else {
                DOMUtils.getInstance().insertAfter(end, endContainer);
                endOffset = DOMUtils.getInstance().getLength(endContainer) - range.getEndOffset();
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
    public void clearPlaceHolders()
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
        clearPlaceHolders();
        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            placeHolders.add(new RangePlaceHolder(selection.getRangeAt(i)));
        }
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
     * Restores the saved selection on the underlying rich text area. You can specify if the selection preserver should
     * reset its state. When you're calling this method multiple times for the same selection it's more convenient to
     * keep the state between the calls rather than call {@link #saveSelection()} before.
     * 
     * @param reset true if you want to reset the state of the selection preserver, false otherwise.
     * @see #saveSelection()
     */
    public void restoreSelection(boolean reset)
    {
        if (placeHolders.isEmpty()) {
            return;
        }
        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        for (int i = 0; i < placeHolders.size(); i++) {
            RangePlaceHolder placeHolder = placeHolders.get(i);
            Range range = rta.getDocument().createRange();
            if (placeHolder.getStartOffset() >= 0) {
                range.setStart(placeHolder.getStart().getNextSibling(), placeHolder.getStartOffset());
            } else {
                range.setStartAfter(placeHolder.getStart());
            }
            if (placeHolder.getEndOffset() >= 0) {
                Node prevSibling = placeHolder.getEnd().getPreviousSibling();
                range.setEnd(prevSibling, DOMUtils.getInstance().getLength(prevSibling) - placeHolder.getEndOffset());
            } else {
                range.setEndBefore(placeHolder.getEnd());
            }
            selection.addRange(range);
        }
        if (reset) {
            clearPlaceHolders();
        }
    }
}
