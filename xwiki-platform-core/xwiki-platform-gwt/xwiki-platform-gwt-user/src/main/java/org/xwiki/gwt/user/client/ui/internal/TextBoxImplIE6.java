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
package org.xwiki.gwt.user.client.ui.internal;

import com.google.gwt.user.client.Element;

/**
 * Extends the text box implementation provided by GWT for IE with the ability to access and modify the selection
 * (cursor position and selection length) even when the text box doesn't have the focus.
 * 
 * @version $Id$
 */
public class TextBoxImplIE6 extends com.google.gwt.user.client.ui.impl.TextBoxImplIE6
{
    /**
     * The name of the property that preserves the selection length.
     */
    private static final String SELECTION_LENGTH = "__selectionLength";

    /**
     * The name of the property that preserved the cursor position.
     */
    private static final String CURSOR_POS = "__cursorPos";

    /**
     * The name of the flag that specifies if the selection is preserver on a given element.
     */
    private static final String SELECTION_PRESERVED = "__selectionPreserved";

    @Override
    public int getSelectionLength(Element element)
    {
        if (isFocused(element)) {
            return super.getSelectionLength(element);
        } else {
            return element.getPropertyInt(SELECTION_LENGTH);
        }
    }

    @Override
    public int getTextAreaCursorPos(Element element)
    {
        if (isFocused(element)) {
            return super.getTextAreaCursorPos(element);
        } else {
            return element.getPropertyInt(CURSOR_POS);
        }
    }

    @Override
    public void setSelectionRange(Element element, int pos, int length)
    {
        if (!element.getPropertyBoolean(SELECTION_PRESERVED)) {
            element.setPropertyBoolean(SELECTION_PRESERVED, true);
            ensureSelectionIsPreserved(element);
        }
        if (isFocused(element)) {
            super.setSelectionRange(element, pos, length);
        } else {
            element.setPropertyInt(CURSOR_POS, pos);
            element.setPropertyInt(SELECTION_LENGTH, length);
        }
    }

    /**
     * @param element a DOM element
     * @return {@code true} if the given element is focused, {@code false} otherwise
     */
    private native boolean isFocused(Element element)
    /*-{
        return element.ownerDocument.selection.createRange().parentElement() == element;
    }-*/;

    /**
     * Ensures the selection of the given element (e.g. a text area, a text input) is preserved when the element looses
     * focus. This method is required because in IE we can't access the cursor position or the selection length while
     * the element is not focused.
     * 
     * @param element the element (e.g. a text area) whose selection had to be preserved
     */
    public native void ensureSelectionIsPreserved(Element element)
    /*-{
        var self = this;
        // Restore the selection before the element is focused.
        element.attachEvent('onbeforeactivate', function(event) {
            self.@org.xwiki.gwt.user.client.ui.internal.TextBoxImplIE6::restoreSelection(Lcom/google/gwt/user/client/Element;)(element);
        });

        // Save the selection before the element loses the focus.
        element.attachEvent('onbeforedeactivate', function(event) {
            self.@org.xwiki.gwt.user.client.ui.internal.TextBoxImplIE6::saveSelection(Lcom/google/gwt/user/client/Element;)(element);
        });
    }-*/;

    /**
     * Restores the selection inside the given element (e.g. text area, text input). If there's no saved selection the
     * caret is placed at the start.
     * 
     * @param element the element whose selection is restored.
     */
    private void restoreSelection(Element element)
    {
        setSelectionRange(element, element.getPropertyInt(CURSOR_POS), element.getPropertyInt(SELECTION_LENGTH));
    }

    /**
     * Saves the selection start and the selection length.
     * 
     * @param element the element whose selection is saved
     */
    private void saveSelection(Element element)
    {
        element.setPropertyInt(CURSOR_POS, getTextAreaCursorPos(element));
        element.setPropertyInt(SELECTION_LENGTH, getSelectionLength(element));
    }
}
