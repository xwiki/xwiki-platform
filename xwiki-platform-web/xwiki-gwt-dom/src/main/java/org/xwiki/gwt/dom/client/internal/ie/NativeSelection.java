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
package org.xwiki.gwt.dom.client.internal.ie;

import org.xwiki.gwt.dom.client.Document;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Wraps the selection JavaScript object provided by Internet Explorer.
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
     * @param document the DOM document for which to retrieve the selection object
     * @return the selection object associated with the given in-line frame
     */
    public static synchronized native NativeSelection getInstance(Document document)
    /*-{
        var selection = document.selection;
        selection.ownerDocument = document;
        return selection;
    }-*/;

    /**
     * Ensures that the selection of the given document is preserved when the document looses focus. This method is
     * required because IE has only one selection object per top level window. This means that when a child document
     * looses the focus its selection object will return ranges from the parent document.
     * 
     * @param document the document whose selection has to be preserved
     */
    public static native void ensureSelectionIsPreserved(Document document)
    /*-{
        // If there is a previously stored selection then restore it. We have to do this before the edited document
        // gets focused to allow users to have a different selection than the stored one (by clicking inside the edited
        // document when it doesn't have the focus).
        document.body.attachEvent('onbeforeactivate', function(event) {
            // In standards mode, the HTML element can gain focus separately from the BODY element without clearing its
            // inner selection. For instance, by using the scroll bars we move the focus from the BODY element to the
            // HTML element but the BODY element keeps its inner selection. In consequence, we don't have to restore
            // the selection if the focus comes from the HTML element.
            if (event.fromElement != document.documentElement) {
                var range = document.parentWindow.__savedRange;
                // Clear the reference to the saved range.
                document.parentWindow.__savedRange = undefined;
                try {
                    // Restore the saved range.
                    range.select();
                } catch (e) {
                    // ignore
                }
            }
        });

        // Save the selection when the edited document is about to loose focus.
        document.body.attachEvent('onbeforedeactivate', function(event) {
            // In standards mode, the HTML element can gain focus separately from the BODY element without clearing its
            // inner selection. For instance, by using the scroll bars we move the focus from the BODY element to the
            // HTML element but the BODY element keeps its inner selection. In consequence, we don't have to save the
            // selection if the focus goes to the HTML element.
            if (event.toElement != document.documentElement) {
                document.parentWindow.__savedRange = document.selection.createRange();
            }
        });
    }-*/;

    /**
     * Creates a TextRange object from the current text selection, or a ControlRange object from a control selection.
     * 
     * @return the created range object
     */
    public native NativeRange createRange()
    /*-{
        // Internet Explorer is limited to a single selection. Although each DOM document has its own selection object,
        // they all return the same range which corresponds to the unique selection. When we have nested documents, the
        // selection is in one of them, the focused one precisely. So it can happen that the range created from a
        // document points to another document. Also, when a document looses focus its selection is lost; further calls
        // to create range return the range from the focused document. In order to overcome this severe limitation of
        // Internet Explorer we have to save the selection whenever a document looses focus and restore it when it gains
        // it back. Additionally, we have to be able to work with a document's selection while it doesn't have the
        // focus. To do so, we save a reference to the current range as a property of the window object. While the
        // document doesn't have the focus we read and overwrite this range, instead of using the selection object.
        var range;
        var savedRange = this.ownerDocument.parentWindow.__savedRange;
        if (typeof(savedRange) == 'undefined') {
            // There's no saved range. Let's create a new range.
            range = this.createRange();
        } else {
            // There's a saved range. Return a clone of the saved range. 
            if (savedRange.duplicate) {
                // Text Range
                range = savedRange.duplicate();
            } else if (savedRange.item) {
                // Control Range
                range = this.ownerDocument.body.createControlRange();
                for(var i = 0; i < savedRange.length; i++) {
                    range.addElement(savedRange.item(i));
                }
            } else {
                // The saved range seems to be invalid.
                throw 'NativeSelection#createRange: Invalid saved range.';
            }
        }
        range.ownerDocument = this.ownerDocument;
        return range;
    }-*/;

    /**
     * Cancels the current selection and sets the selection type to none.
     */
    public native void empty()
    /*-{
        if (typeof(this.ownerDocument.parentWindow.__savedRange) == 'undefined') {
            this.empty();
        } else {
            // Save an invalid range.
            this.ownerDocument.parentWindow.__savedRange = {select : function(){}};
        }
    }-*/;

    /**
     * @return the document associated with this selection object
     */
    public native Document getOwnerDocument()
    /*-{
        return this.ownerDocument;
    }-*/;
}
