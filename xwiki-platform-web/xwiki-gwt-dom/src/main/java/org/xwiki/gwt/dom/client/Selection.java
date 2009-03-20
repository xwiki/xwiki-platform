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
package org.xwiki.gwt.dom.client;

import com.google.gwt.dom.client.Node;

/**
 * A selection object represents the ranges that the user has selected, or the cursor position when the user didn't
 * select any range. It is the only was to get access to the current DOM nodes being edited with the WYSIWYG editor.
 * 
 * @version $Id$
 */
public interface Selection
{
    /**
     * @return the node in which the selection begins
     */
    Node getAnchorNode();

    /**
     * @return the offset within the {@link #getAnchorNode()} where the selection begins
     */
    int getAnchorOffset();

    /**
     * @return the node in which the selection ends
     */
    Node getFocusNode();

    /**
     * @return the offset within the {@link #getFocusNode()} where the selection ends.
     */
    int getFocusOffset();

    /**
     * @return true if the selection is collapsed
     */
    boolean isCollapsed();

    /**
     * @return the number of ranges in the selection
     */
    int getRangeCount();

    /**
     * @param index the index of the range to retrieve. Usually the selection contains just one range.
     * @return the range at the specified index
     */
    Range getRangeAt(int index);

    /**
     * Collapses the selection to a single point, at the specified offset in the given DOM node. When the selection is
     * collapsed, and the content is focused and editable, the caret will blink there.
     * 
     * @param parentNode the DOM node where the selection will be set
     * @param offset specifies where to place the selection in the given node
     */
    void collapse(Node parentNode, int offset);

    /**
     * Extends the selection by moving the selection end to the specified node and offset, preserving the selection
     * begin position. The new selection end result will always be from the anchorNode to the new focusNode, regardless
     * of direction.
     * 
     * @param parentNode the node where the selection will be extended to
     * @param offset specifies where to end the selection in the given node
     */
    void extend(Node parentNode, int offset);

    /**
     * Collapses the whole selection to a single point at the start of the current selection (irrespective of
     * direction). If content is focused and editable, the caret will blink there.
     */
    void collapseToStart();

    /**
     * Collapses the whole selection to a single point at the end of the current selection (irrespective of direction).
     * If content is focused and editable, the caret will blink there.
     */
    void collapseToEnd();

    /**
     * Indicates whether the given node is part of the selection.
     * 
     * @param node the DOM node to be tested
     * @param partlyContained if false, the entire subtree rooted in the given node is tested
     * @return true when the entire node is part of the selection
     */
    boolean containsNode(Node node, boolean partlyContained);

    /**
     * Adds all children of the specified node to the selection. Previous selection is lost.
     * 
     * @param parentNode the parent of the children to be added to the selection
     */
    void selectAllChildren(Node parentNode);

    /**
     * Adds a range to this selection.
     * 
     * @param range the range to be added
     */
    void addRange(Range range);

    /**
     * Removes the given range from the selection.
     * 
     * @param range the range to be removed from the selection.
     */
    void removeRange(Range range);

    /**
     * Removes all ranges from the current selection.
     */
    void removeAllRanges();

    /**
     * Deletes this selection from document the nodes belong to.
     */
    void deleteFromDocument();

    /**
     * Modifies the cursor Bidi level after a change in keyboard direction.
     * 
     * @param langRTL is true if the new language is right-to-left or false if the new language is left-to-right
     */
    void selectionLanguageChange(boolean langRTL);

    /**
     * @return the currently selected text
     */
    String toString();
}
