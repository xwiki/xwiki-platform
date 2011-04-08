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
package org.xwiki.gwt.dom.client.internal.mozilla;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.dom.client.Node;

/**
 * The native selection implementation provided by Mozilla.
 * 
 * @version $Id$
 */
public final class NativeSelection extends JavaScriptObject
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected NativeSelection()
    {
    }

    /**
     * Retrieves the native selection object using Mozilla's API.
     * 
     * @param document the document for which to retrieve the selection instance
     * @return the native selection object associated with the given document
     */
    public static native NativeSelection getInstance(Document document)
    /*-{
        var selection = document.defaultView.getSelection();
        // The selection is null if the window or one of its ancestors is hidden with display:none.
        if (selection) {
            // Let's save a reference to the selection object in order to use it when the window is hidden.
            document.defaultView.__selection = selection;
        } else {
            // Use the saved reference to the selection object when the window is hidden.
            selection = document.defaultView.__selection;
        }
        return selection;
    }-*/;

    /**
     * Adds a range to this selection.
     * 
     * @param range the range to be added
     */
    public native void addRange(NativeRange range)
    /*-{
        this.addRange(range);
    }-*/;

    /**
     * Collapses the selection to a single point, at the specified offset in the given DOM node. When the selection is
     * collapsed, and the content is focused and editable, the caret will blink there.
     * 
     * @param parentNode the DOM node where the selection will be set
     * @param offset specifies where to place the selection in the given node
     */
    public native void collapse(Node parentNode, int offset)
    /*-{
        this.collapse(parentNode, offset);
    }-*/;

    /**
     * Collapses the whole selection to a single point at the end of the current selection (irrespective of direction).
     * If content is focused and editable, the caret will blink there.
     */
    public native void collapseToEnd()
    /*-{
        this.collapseToEnd();
    }-*/;

    /**
     * Collapses the whole selection to a single point at the start of the current selection (irrespective of
     * direction). If content is focused and editable, the caret will blink there.
     */
    public native void collapseToStart()
    /*-{
        this.collapseToStart();
    }-*/;

    /**
     * Indicates whether the given node is part of the selection.
     * 
     * @param node the DOM node to be tested
     * @param partlyContained if false, the entire subtree rooted in the given node is tested
     * @return true when the entire node is part of the selection
     */
    public native boolean containsNode(Node node, boolean partlyContained)
    /*-{
        return this.containsNode(node, partlyContained);
    }-*/;

    /**
     * Deletes this selection from document the nodes belong to.
     */
    public native void deleteFromDocument()
    /*-{
        this.deleteFromDocument();
    }-*/;

    /**
     * Extends the selection by moving the selection end to the specified node and offset, preserving the selection
     * begin position. The new selection end result will always be from the anchorNode to the new focusNode, regardless
     * of direction.
     * 
     * @param parentNode the node where the selection will be extended to
     * @param offset specifies where to end the selection in the given node
     */
    public native void extend(Node parentNode, int offset)
    /*-{
        this.extend(parentNode, offset);
    }-*/;

    /**
     * @return the node in which the selection begins
     */
    public native Node getAnchorNode()
    /*-{
        return this.anchorNode;
    }-*/;

    /**
     * @return the offset within the {@link #getAnchorNode()} where the selection begins
     */
    public native int getAnchorOffset()
    /*-{
        return this.anchorOffset;
    }-*/;

    /**
     * @return the node in which the selection ends
     */
    public native Node getFocusNode()
    /*-{
        return this.focusNode;
    }-*/;

    /**
     * @return the offset within the {@link #getFocusNode()} where the selection ends.
     */
    public native int getFocusOffset()
    /*-{
        return this.focusOffset;
    }-*/;

    /**
     * @param index the index of the range to retrieve. Usually the selection contains just one range.
     * @return the range at the specified index
     */
    public native NativeRange getRangeAt(int index)
    /*-{
        return this.getRangeAt(index);
    }-*/;

    /**
     * @return the number of ranges in the selection
     */
    public native int getRangeCount()
    /*-{
        return this.rangeCount;
    }-*/;

    /**
     * @return true if the selection is collapsed
     */
    public native boolean isCollapsed()
    /*-{
        return this.isCollapsed;
    }-*/;

    /**
     * Removes all ranges from the current selection.
     */
    public native void removeAllRanges()
    /*-{
        this.removeAllRanges();
    }-*/;

    /**
     * Removes the given range from the selection.
     * 
     * @param range the range to be removed from the selection.
     */
    public native void removeRange(NativeRange range)
    /*-{
        this.removeRange(range);
    }-*/;

    /**
     * Adds all children of the specified node to the selection. Previous selection is lost.
     * 
     * @param parentNode the parent of the children to be added to the selection
     */
    public native void selectAllChildren(Node parentNode)
    /*-{
        this.selectAllChildren(parentNode);
    }-*/;

    /**
     * Modifies the cursor Bidi level after a change in keyboard direction.
     * 
     * @param langRTL is true if the new language is right-to-left or false if the new language is left-to-right
     */
    public native void selectionLanguageChange(boolean langRTL)
    /*-{
        this.selectionLanguageChange(langRTL);
    }-*/;
}
