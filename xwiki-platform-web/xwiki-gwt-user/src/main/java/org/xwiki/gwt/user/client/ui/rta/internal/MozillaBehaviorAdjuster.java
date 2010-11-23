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
package org.xwiki.gwt.user.client.ui.rta.internal;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

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
     * @see BehaviorAdjuster#navigateOutsideTableCell(Event, boolean)
     */
    protected void navigateOutsideTableCell(Event event, boolean before)
    {
        super.navigateOutsideTableCell(event, before);

        if (!event.isCancelled()) {
            return;
        }

        Document document = getTextArea().getDocument();
        Selection selection = document.getSelection();
        Range caret = selection.getRangeAt(0);
        Node emptyTextNode = caret.getStartContainer();

        // (1) We need to add a BR to make the new empty line visible.
        // (2) The caret is rendered at the start of the document when we place it inside an empty text node. To fix
        // this, we move the caret before the BR and remove the empty text node.
        emptyTextNode.getParentNode().insertBefore(document.createBRElement(), emptyTextNode);
        caret.setStartBefore(emptyTextNode.getPreviousSibling());
        caret.collapse(true);
        emptyTextNode.getParentNode().removeChild(emptyTextNode);

        // Update the selection.
        selection.removeAllRanges();
        selection.addRange(caret);
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
                    // We have to avoid this Mozilla bug. Let's manually remove the button.
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
     * @see BehaviorAdjuster#onDelete(Event)
     */
    protected void onDelete(Event event)
    {
        if (deleteButton(false)) {
            // Prevent the default browser behavior if the button has been deleted.
            event.xPreventDefault();
        } else {
            super.onDelete(event);
        }
    }

    /**
     * {@inheritDoc}<br/>
     * NOTE: Fixes the issue with backspace after a button element.
     * 
     * @see BehaviorAdjuster#onBackSpace(Event)
     */
    protected void onBackSpace(Event event)
    {
        if (deleteButton(true)) {
            // Prevent the default browser behavior if the button has been deleted.
            event.xPreventDefault();
        } else {
            super.onBackSpace(event);
        }
    }

    /**
     * {@inheritDoc}<br/>
     * NOTE: Fixes the issue with button elements not being selected on mouse down.
     * 
     * @see BehaviorAdjuster#onBeforeMouseDown(Event)
     */
    protected void onBeforeMouseDown(Event event)
    {
        super.onBeforeMouseDown(event);

        Node target = (Node) event.getEventTarget().cast();
        // A button should be selected on mouse down.
        if (BUTTON.equalsIgnoreCase(target.getNodeName())) {
            Range range = getTextArea().getDocument().createRange();
            range.selectNode(target);
            Selection selection = getTextArea().getDocument().getSelection();
            if (!event.getCtrlKey()) {
                selection.removeAllRanges();
            }
            selection.addRange(range);
        }
    }
}
