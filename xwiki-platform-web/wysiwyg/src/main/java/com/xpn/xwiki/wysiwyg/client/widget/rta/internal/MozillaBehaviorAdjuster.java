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
package com.xpn.xwiki.wysiwyg.client.widget.rta.internal;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.dom.client.Node;

/**
 * Adjusts the behavior of the rich text area in Mozilla based browsers, like Firefox.
 * 
 * @version $Id$
 */
public class MozillaBehaviorAdjuster extends BehaviorAdjuster
{
    /**
     * Button element node name.
     */
    private static final String BUTTON = "button";

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#adjustDragDrop(Document)
     */
    public native void adjustDragDrop(Document document)
    /*-{
        // block default drag and drop mechanism to not allow content to be dropped on this document 
        document.addEventListener("dragdrop", function(event) {
            event.stopPropagation();
        }, true);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#navigateOutsideTableCell(boolean)
     */
    protected void navigateOutsideTableCell(boolean before)
    {
        super.navigateOutsideTableCell(before);

        Event event = getTextArea().getCurrentEvent();
        if (!event.isCancelled()) {
            return;
        }

        Document document = getTextArea().getDocument();
        Node paragraph = document.getSelection().getRangeAt(0).getStartContainer().getParentNode();
        paragraph.appendChild(document.xCreateBRElement());
    }

    /**
     * Looks for a button element in the specified direction relative to the caret position and deletes it if found.
     * 
     * @param left specifies where to look for the button, relative to the caret position. Pass {@code true} to look on
     *            the left side of the caret or {@code false} to look on the right side.
     * @return {@code true} if a button has been deleted, {@code false} otherwise
     */
    protected boolean deleteButton(boolean left)
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (selection.isCollapsed()) {
            Range range = selection.getRangeAt(0);
            int border = left ? 0 : domUtils.getLength(range.getStartContainer());
            // See if the caret is between nodes.
            if (range.getStartContainer().getNodeType() == Node.ELEMENT_NODE || range.getStartOffset() == border) {
                // See if the caret is before or after a button element.
                Node leaf = left ? domUtils.getPreviousNode(range) : domUtils.getNextNode(range);
                if (leaf != null && BUTTON.equalsIgnoreCase(leaf.getNodeName())) {
                    // Pressing delete before or backspace after a button element places the caret inside that element.
                    // We have to avoid this Mozilla bug. Let's prevent the default delete behavior and manually remove
                    // the button.
                    getTextArea().getCurrentEvent().xPreventDefault();
                    leaf.getParentNode().removeChild(leaf);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}<br/>
     * NOTE: Fixes the issue with delete before a button element.
     * 
     * @see BehaviorAdjuster#onDelete()
     */
    protected void onDelete()
    {
        if (!deleteButton(false)) {
            super.onDelete();
        }
    }

    /**
     * {@inheritDoc}<br/>
     * NOTE: Fixes the issue with backspace after a button element.
     * 
     * @see BehaviorAdjuster#onBackSpace()
     */
    protected void onBackSpace()
    {
        if (!deleteButton(true)) {
            super.onBackSpace();
        }
    }

    /**
     * {@inheritDoc}<br/>
     * NOTE: Fixes the issue with button elements not being selected on mouse down.
     * 
     * @see BehaviorAdjuster#onBeforeMouseDown()
     */
    protected void onBeforeMouseDown()
    {
        super.onBeforeMouseDown();

        Event event = getTextArea().getCurrentEvent();
        Node target = event.getTarget();
        // A button should be selected on mouse down.
        if (BUTTON.equalsIgnoreCase(target.getNodeName())) {
            Range range = getTextArea().getDocument().createRange();
            range.selectNode(target);
            Selection selection = getTextArea().getDocument().getSelection();
            if (!event.getCtrlKey()) {
                selection.removeAllRanges();
            }
            selection.addRange(range);
            refreshSelection();
        }
    }

    /**
     * Repaints the edit area in Gecko-based browsers. This method removes ghost resize handlers and other trailing
     * graphics.
     */
    protected void refreshSelection()
    {
        // Backup current ranges.
        List<Range> ranges = new ArrayList<Range>();
        Document document = getTextArea().getDocument();
        Selection selection = document.getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            ranges.add(selection.getRangeAt(i));
        }

        // Refresh selection.
        document.getBody().getStyle().setProperty(Style.DISPLAY, Style.Display.NONE);
        document.execCommand("selectall", null);
        selection.removeAllRanges();
        document.getBody().getStyle().setProperty(Style.DISPLAY, Style.Display.BLOCK);

        // Restore the ranges.
        for (int i = 0; i < ranges.size(); i++) {
            selection.addRange(ranges.get(i));
        }
    }
}
