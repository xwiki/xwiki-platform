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
            if (event.fromElement == document.documentElement) {
                return;
            }
            var range = 
    @org.xwiki.gwt.dom.client.internal.ie.NativeSelection::createRange(Lorg/xwiki/gwt/dom/client/Document;)(document);
            // Reset the bookmark to prevent redundant calls to this function.
            // NOTE: We don't use "null" but "undefined" to reset the bookmark because typeof(bookmark) returns
            // "object" when the bookmark is null but defined (previously had a non null value).
            document.body.__bookmark = undefined;
            range.select();
        });

        // Save the selection when the edited document is about to loose focus.
        document.body.attachEvent('onbeforedeactivate', function(event) {
            // In standards mode, the HTML element can gain focus separately from the BODY element without clearing its
            // inner selection. For instance, by using the scroll bars we move the focus from the BODY element to the
            // HTML element but the BODY element keeps its inner selection. In consequence, we don't have to save the
            // selection if the focus goes to the HTML element.
            if (event.toElement == document.documentElement) {
                return;
            }
            document.body.__bookmark = undefined;
            var range = document.selection.createRange();
            // Check the type of the range and if the range is inside the edited document.
            if (range.getBookmark && range.parentElement().ownerDocument == document) {
                // Text range.
                document.body.__bookmark = range.getBookmark();
            } else if (range.item && range.length > 0 && range.item(0).ownerDocument == document) {
                // Control range.
                document.body.__bookmark = range.item(0);
            }
        });
    }-*/;

    /**
     * This is a utility method needed in order to create a range in a static environment, because
     * "only static references to overlay types are allowed from JSNI".
     * 
     * @param document a DOM document for which to retrieve the current range
     * @return the current range of the given document
     */
    public static NativeRange createRange(Document document)
    {
        return getInstance(document).createRange();
    }

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
        // focus. To do so, we save the selection in a book mark stored as an attribute of that document's body. While
        // the document doesn't have the focus we read and overwrite this book mark, instead of using the selection
        // object.
        var bookmark = this.ownerDocument.body.__bookmark;
        switch (typeof(bookmark)) {
            case 'undefined':
                // There's no saved range. Let's use the selection object to create a new range.
                var range = this.createRange();
                range.ownerDocument = this.ownerDocument;
                return range;
            case 'string':
                // There's a saved text range. The bookmark is an opaque string that can be used with moveToBookmark to
                // recreate the original text range.
                var textRange = this.ownerDocument.body.createTextRange();
                textRange.ownerDocument = this.ownerDocument;
                try {
                    textRange.moveToBookmark(bookmark);
                } catch(e) {
                    // The document could have been changed in the mean time, leaving the bookmark invalid. Fallback on
                    // a default text range.
                    textRange.moveToElementText(this.ownerDocument.body);
                    textRange.collapse();
                }
                return textRange;
            case 'object':
                // There's a saved control range. The bookmark is a reference to the element previously selected.
                var controlRange = this.ownerDocument.body.createControlRange();
                try {
                    controlRange.addElement(bookmark);
                } catch(e) {
                    // The element could have been detached in the mean time. Fallback on a default text range.
                    controlRange = this.ownerDocument.body.createTextRange();
                    controlRange.moveToElementText(this.ownerDocument.body);
                    controlRange.collapse();
                }
                controlRange.ownerDocument = this.ownerDocument;
                return controlRange;
            default:
                // There seems to be a saved range, but the type of the bookmark is not supported.
                throw 'NativeSelection#createRange: Unsupported bookmark.';
        }
    }-*/;

    /**
     * Cancels the current selection and sets the selection type to none.
     */
    public native void empty()
    /*-{
        if (typeof(this.ownerDocument.body.__bookmark) == 'undefined') {
            this.empty();
        } else {
            // Set the bookmark to an unsupported value.
            this.ownerDocument.body.__bookmark = false;
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
