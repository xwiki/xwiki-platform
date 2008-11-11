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

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
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
     * The rich text area whose selection is preserved.
     */
    private final RichTextArea rta;

    /**
     * The parent of the start container.
     * 
     * @see Range#getStartContainer()
     */
    private Node startParent;

    /**
     * The index of the start container within its parent node.
     * 
     * @see Range#getStartContainer()
     */
    private int startIndex;

    /**
     * The start offset.
     * 
     * @see Range#getStartOffset()
     */
    private int startOffset;

    /**
     * The parent of the end container.
     * 
     * @see Range#getEndContainer()
     */
    private Node endParent;

    /**
     * The number of siblings to the left of the end container. In other words, the right-index (relative to the last
     * child) of the end container within its parent node.
     * 
     * @see Range#getEndContainer()
     */
    private int endIndex;

    /**
     * The right-offset, relative to the last child or last character (depending on end container's node type).
     * 
     * @see Range#getEndOffset()
     */
    private int endOffset;

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
     * Saves the first range in the current selection for later changes. The selection is taken from the target
     * document.
     * 
     * @see #range
     * @see #restoreSelection()
     */
    public void saveSelection()
    {
        Range range = rta.getDocument().getSelection().getRangeAt(0);

        Node start = range.getStartContainer();
        startParent = start.getParentNode();
        startIndex = DOMUtils.getInstance().getNodeIndex(start);
        startOffset = range.getStartOffset();

        Node end = range.getEndContainer();
        endParent = end.getParentNode();
        endIndex = endParent.getChildNodes().getLength() - DOMUtils.getInstance().getNodeIndex(end);
        if (end.getNodeType() == Node.TEXT_NODE) {
            endOffset = end.getNodeValue().length() - range.getEndOffset();
        } else {
            endOffset = end.getChildNodes().getLength() - range.getEndOffset();
        }
    }

    /**
     * Restores the saved selection on the target document.
     * 
     * @see #range
     * @see #saveSelection()
     */
    public void restoreSelection()
    {
        Range range = rta.getDocument().createRange();
        range.setStart(startParent.getChildNodes().getItem(startIndex), startOffset);
        Node end = endParent.getChildNodes().getItem(endParent.getChildNodes().getLength() - endIndex);
        if (end.getNodeType() == Node.TEXT_NODE) {
            range.setEnd(end, end.getNodeValue().length() - endOffset);
        } else {
            range.setEnd(end, end.getChildNodes().getLength() - endOffset);
        }
        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }
}
