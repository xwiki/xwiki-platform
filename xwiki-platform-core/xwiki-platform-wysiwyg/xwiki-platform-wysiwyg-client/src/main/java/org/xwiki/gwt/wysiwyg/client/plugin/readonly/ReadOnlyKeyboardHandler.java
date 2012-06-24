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
package org.xwiki.gwt.wysiwyg.client.plugin.readonly;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.KeyboardAdaptor;

import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;

/**
 * Handles the keyboard events concerning read-only regions inside the rich text area.
 * 
 * @version $Id$
 */
public class ReadOnlyKeyboardHandler extends KeyboardAdaptor
{
    /**
     * The list of key codes that are allowed on the read-only regions.
     */
    private static final List<Integer> NON_PRINTING_KEY_CODES =
        Arrays.asList(KeyCodes.KEY_ESCAPE, KeyCodes.KEY_PAGEUP, KeyCodes.KEY_PAGEDOWN, KeyCodes.KEY_END,
            KeyCodes.KEY_HOME, KeyCodes.KEY_LEFT, KeyCodes.KEY_UP, KeyCodes.KEY_RIGHT, KeyCodes.KEY_DOWN);

    /**
     * Utility object to manipulate the DOM.
     */
    private final DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * Utility methods concerning read-only regions inside the rich text area.
     */
    private final ReadOnlyUtils readOnlyUtils = new ReadOnlyUtils();

    @Override
    protected void handleRepeatableKey(Event event)
    {
        if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE || event.getKeyCode() == KeyCodes.KEY_DELETE) {
            onDelete(event);
        } else if (!NON_PRINTING_KEY_CODES.contains(event.getKeyCode())) {
            onTyping(event);
        }
    }

    /**
     * Delete or backspace key has been pressed.
     * 
     * @param event the native event that was fired
     */
    private void onDelete(Event event)
    {
        Document document = Element.as(event.getEventTarget()).getOwnerDocument().cast();
        onDelete(event, document.getSelection().getRangeAt(0));
    }

    /**
     * Delete or backspace key has been pressed while the given range was selected.
     * 
     * @param event the native event that was fired
     * @param range the range targeted by the native event
     */
    private void onDelete(Event event, Range range)
    {
        if (range.isCollapsed()) {
            Element target = readOnlyUtils.getClosestReadOnlyAncestor(range.getCommonAncestorContainer());
            if (target == null || isDeleteAfter(event, target, range) || isBackspaceBefore(event, target, range)) {
                target = getNearbyReadOnlyContainer(range, event.getKeyCode() == KeyCodes.KEY_DELETE);
                if (target == null) {
                    return;
                }
            }
            onDelete(event, target);
        } else {
            Element startContainerReadOnlyAncestor =
                readOnlyUtils.getClosestReadOnlyAncestor(range.getStartContainer());
            Element endContainerReadOnlyAncestor =
                range.getStartContainer() != range.getEndContainer() ? readOnlyUtils.getClosestReadOnlyAncestor(range
                    .getEndContainer()) : startContainerReadOnlyAncestor;
            if (startContainerReadOnlyAncestor == endContainerReadOnlyAncestor) {
                if (startContainerReadOnlyAncestor != null) {
                    onDelete(event, startContainerReadOnlyAncestor);
                }
            } else {
                onDelete(event, startContainerReadOnlyAncestor, range, true);
                onDelete(event, endContainerReadOnlyAncestor, range, false);
            }
        }
    }

    /**
     * @param event the native event that was fired
     * @param container the read-only container
     * @param caret the caret
     * @return {@code true} if the {@link KeyCodes#KEY_BACKSPACE} was pressed and the caret is at the start of the given
     *         read-only container, {@code false} otherwise
     */
    private boolean isBackspaceBefore(Event event, Element container, Range caret)
    {
        return event.getKeyCode() == KeyCodes.KEY_BACKSPACE && isBefore(caret, container);
    }

    /**
     * @param caret the caret
     * @param container a DOM element
     * @return {@code true} if the caret is before the first visible leaf of the given container, {@code false}
     *         otherwise
     */
    private boolean isBefore(Range caret, Element container)
    {
        Node leaf = domUtils.getFirstLeaf(container);
        return domUtils.comparePoints(caret.getEndContainer(), caret.getEndOffset(), leaf, 0) <= 0;
    }

    /**
     * @param event the native event that was fired
     * @param container the read-only container
     * @param caret the caret
     * @return {@code true} if the {@link KeyCodes#KEY_DELETE} was pressed and the caret is at the end of the given
     *         read-only container, {@code false} otherwise
     */
    private boolean isDeleteAfter(Event event, Element container, Range caret)
    {
        return event.getKeyCode() == KeyCodes.KEY_DELETE && isAfter(caret, container);
    }

    /**
     * @param caret the caret
     * @param container a DOM element
     * @return {@code true} if the caret is after the last visible leaf of the given container, {@code false} otherwise
     */
    private boolean isAfter(Range caret, Element container)
    {
        Node leaf = domUtils.getLastLeaf(container);
        return domUtils
            .comparePoints(leaf, domUtils.getLength(leaf), caret.getStartContainer(), caret.getStartOffset()) <= 0;
    }

    /**
     * Deletes a read-only element when it is the only target of the given event.
     * 
     * @param event the native event that was fired
     * @param element the read-only element to be deleted
     */
    protected void onDelete(Event event, Element element)
    {
        element.getParentNode().removeChild(element);
        event.xPreventDefault();
    }

    /**
     * Deletes a read-only element when it is not the only target of the given event.
     * 
     * @param event the native event that was fired
     * @param element the read-only element to be deleted
     * @param range the range that touches the read-only element
     * @param start {@code true} if the start point of the given range is inside the read-only element, {@code false} if
     *            the end point is inside the read-only element
     */
    protected void onDelete(Event event, Element element, Range range, boolean start)
    {
        // Null safe.
        domUtils.detach(element);
    }

    /**
     * Looks for a read-only container to the left/right of the given caret. The caret has to be just after/before the
     * read-only container in order for this method to find it.
     * 
     * @param caret where to look for the read-only container
     * @param right {@code true} to look at the right of the caret, {@code false} to look at the left of the caret
     * @return the read-only container to the left/right of the caret
     */
    private Element getNearbyReadOnlyContainer(Range caret, boolean right)
    {
        if (caret.getStartContainer().getNodeType() == Node.TEXT_NODE) {
            boolean left = !right;
            boolean atStart = caret.getStartOffset() == 0;
            boolean atEnd = caret.getStartOffset() == caret.getStartContainer().getNodeValue().length();
            if ((left && !atStart) || (right && !atEnd)) {
                return null;
            }
        }
        Node leaf = right ? domUtils.getNextLeaf(caret) : domUtils.getPreviousLeaf(caret);
        return leaf != null ? readOnlyUtils.getClosestReadOnlyAncestor(leaf) : null;
    }

    /**
     * A printable character key was pressed.
     * 
     * @param event the native event that was fired
     */
    private void onTyping(Event event)
    {
        Document document = Element.as(event.getEventTarget()).getOwnerDocument().cast();
        Range range = document.getSelection().getRangeAt(0);
        if (isBoundary(range)) {
            Element readOnlyContainer = readOnlyUtils.getClosestReadOnlyAncestor(range.getStartContainer());
            if (readOnlyContainer != null) {
                int readOnlyBoundary = isBoundary(range, readOnlyContainer);
                if (readOnlyBoundary == 0) {
                    event.xPreventDefault();
                } else {
                    // Allow typing if the caret is at the boundary of a read-only area, by moving the caret outside.
                    moveCaretOutside(readOnlyContainer, readOnlyBoundary < 0);
                }
            }
        } else if (readOnlyUtils.isRangeBoundaryInsideReadOnlyElement(range)) {
            event.xPreventDefault();
        }
    }

    /**
     * @param range a DOM range
     * @return {@code true} if the given range is collapsed between two DOM nodes or at the start/end of a text node,
     *         {@code false} otherwise
     */
    private boolean isBoundary(Range range)
    {
        return range.isCollapsed()
            && (range.getStartContainer().getNodeType() == Node.ELEMENT_NODE || range.getStartOffset() == 0 || range
                .getStartOffset() == range.getStartContainer().getNodeValue().length());
    }

    /**
     * NOTE: We assume {@link #isBoundary(Range)} returns {@code true} for the given range.
     * 
     * @param caret a collapsed range within the given container
     * @param container a DOM node <strong>containing</strong> the caret
     * @return {@code -1} if the caret is at the start of the given node before any visible content, {@code 1} if it is
     *         at the end after all visible content, {@code 0} otherwise
     */
    private int isBoundary(Range caret, Element container)
    {
        Range betweenNodes = caret;
        // Make sure the caret is between DOM nodes and not inside a text node.
        if (!domUtils.canHaveChildren(caret.getStartContainer())) {
            betweenNodes = caret.cloneRange();
            betweenNodes.selectNode(caret.getStartContainer());
            betweenNodes.collapse(caret.getStartOffset() == 0);
        }
        if (!hasVisibleContentBefore(container, betweenNodes)) {
            return -1;
        } else if (!hasVisibleContentAfter(container, betweenNodes)) {
            return 1;
        }
        return 0;
    }

    /**
     * @param container a DOM node
     * @param caret specifies a position within the given container, <strong>between</strong> DOM nodes
     * @return {@code true} if the given container has visible content before the caret position, {@code false}
     *         otherwise
     */
    private boolean hasVisibleContentBefore(Node container, Range caret)
    {
        Node leaf = domUtils.getPreviousLeaf(caret);
        if (leaf == null || !container.isOrHasChild(leaf)) {
            // We jumped outside of the container without finding any leaf.
            return false;
        }
        // Move backward looking for a visible leaf, taking care not to jump over the first leaf.
        Node firstLeaf = domUtils.getFirstLeaf(container);
        while (!isVisible(leaf)) {
            if (leaf == firstLeaf) {
                return false;
            }
            leaf = domUtils.getPreviousLeaf(leaf);
        }
        return true;
    }

    /**
     * @param container a DOM node
     * @param caret specifies a position within the given container, <strong>between</strong> DOM nodes
     * @return {@code true} if the given container has visible content after the caret position, {@code false} otherwise
     */
    private boolean hasVisibleContentAfter(Node container, Range caret)
    {
        Node leaf = domUtils.getNextLeaf(caret);
        if (leaf == null || !container.isOrHasChild(leaf)) {
            // We jumped outside of the container without finding any leaf.
            return false;
        }
        // Move forward looking for a visible leaf, taking care not to jump over the last leaf.
        Node lastLeaf = domUtils.getLastLeaf(container);
        while (!isVisible(leaf)) {
            if (leaf == lastLeaf) {
                return false;
            }
            leaf = domUtils.getNextLeaf(leaf);
        }
        return true;
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is visible, {@code false} otherwise
     */
    private boolean isVisible(Node node)
    {
        if (node == null) {
            return false;
        }
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                return "br".equalsIgnoreCase(node.getNodeName()) || Element.as(node).getOffsetWidth() > 0;
            case Node.TEXT_NODE:
                return node.getNodeValue().length() > 0 && isVisible(node.getParentNode());
            default:
                return false;
        }
    }

    /**
     * Places the caret outside of the specified node, either before or after.
     * 
     * @param node a DOM node
     * @param before {@code true} to place the caret before the given node, {@code false} to place it after
     */
    protected void moveCaretOutside(Node node, boolean before)
    {
        Document document = node.getOwnerDocument().cast();
        Range range = document.createRange();
        range.selectNode(node);
        range.collapse(before);
        Selection selection = document.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }
}
