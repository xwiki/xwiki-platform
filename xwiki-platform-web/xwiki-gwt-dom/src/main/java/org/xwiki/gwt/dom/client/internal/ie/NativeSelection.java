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
     * NOTE: If there's no selected range in the given document then the returned selection object corresponds to the
     * parent document. As a consequence, ranges obtained through this selection object using the native API are from
     * the parent document. {@link #ensureSelectionIsPreserved(Document)} tries to fix this, but we have to return a
     * non-null selection object even if it's not the right one, as long as we set the {@code ownerDocument} property to
     * the right value.
     * 
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
        var restoreSelectionHandler = function(event) {
            // In standards mode, the HTML element can gain focus separately from the BODY element without clearing its
            // inner selection. For instance, by using the scroll bars we move the focus from the BODY element to the
            // HTML element but the BODY element keeps its inner selection. In consequence, we don't have to restore
            // the selection if the focus comes from the HTML element.
            //
            // Also, IE renders some elements as boxes (elements with fixed width or height, or with
            // display:inline-block for instance) which can be edited by double clicking. Although this boxes are inside
            // the edited document, the BODY element looses the focus when this elements are edited. There's no need to
            // restore the selection when the focus comes from such an inner node.
            if (!event.fromElement || event.fromElement.ownerDocument != document) {
                var range = document.parentWindow.__xwe_savedRange;
                // Clear the reference to the saved range.
                document.parentWindow.__xwe_savedRange = undefined;
                try {
                    // Try to restore the saved range as it is, preserving its type.
                    range.select();
                } catch (e) {
                    try {
                        // Try to restore the saved range as a text range since it may be a control range with an
                        // invalid element. This can happen if an invalid control range is selected while the parent
                        // window is not focused. See NativeRange#select() and ControlRange#add(Element).
                        range.execCommand('SelectAll');
                    } catch(e) {
                        // ignore
                    }
                }
            }
        };
        document.body.attachEvent('onbeforeactivate', restoreSelectionHandler);
        // 'onbeforeactivate' event is not fired when the parent window is focused from code (which happens for instance
        // when we click on the HTML element).
        document.parentWindow.attachEvent('onfocus', restoreSelectionHandler);

        // Save the selection when the edited document is about to loose focus.
        document.body.attachEvent('onbeforedeactivate', function(event) {
            // In standards mode, the HTML element can gain focus separately from the BODY element without clearing its
            // inner selection. For instance, by using the scroll bars we move the focus from the BODY element to the
            // HTML element but the BODY element keeps its inner selection. In consequence, we don't have to save the
            // selection if the focus goes to the HTML element.
            //
            // Also, IE renders some elements as boxes (elements with fixed width or height, or with
            // display:inline-block for instance) which can be edited by double clicking. Although this boxes are inside
            // the edited document, the BODY element looses the focus when this elements are edited. There's no need to
            // save the selection when the focus goes to such an inner node.
            if (!event.toElement || event.toElement.ownerDocument != document) {
                document.parentWindow.__xwe_savedRange = document.selection.createRange();
                document.parentWindow.__xwe_savedRange.ownerDocument = document;
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
        var savedRange = this.ownerDocument.parentWindow.__xwe_savedRange;
        if (typeof(savedRange) == 'undefined') {
            // There's no saved range. Default on current range.
            var currentRange = this.createRange();
            currentRange.ownerDocument = this.ownerDocument;
            // Ranges unsupported by the native selection are cached (ranges that start or end inside a hidden element).
            var cachedRange = this.ownerDocument.parentWindow.__xwe_cachedRange;
            var cachedRangeWitness = this.ownerDocument.parentWindow.__xwe_cachedRangeWitness;
            // If there's a cached range and the witness equals the current range then return the cached range.
            if (typeof(cachedRange) != 'undefined' && @org.xwiki.gwt.dom.client.internal.ie.NativeRange::areEqual(Lorg/xwiki/gwt/dom/client/internal/ie/NativeRange;Lorg/xwiki/gwt/dom/client/internal/ie/NativeRange;)(currentRange, cachedRangeWitness)) {
                return @org.xwiki.gwt.dom.client.internal.ie.NativeRange::duplicate(Lorg/xwiki/gwt/dom/client/internal/ie/NativeRange;)(cachedRange);
            } else {
                // Reset the cache.
                this.ownerDocument.parentWindow.__xwe_cachedRange = undefined;
                this.ownerDocument.parentWindow.__xwe_cachedRangeWitness = undefined;
                return currentRange;
            }
        } else {
            // There's a saved range. Return a clone of the saved range.
            return @org.xwiki.gwt.dom.client.internal.ie.NativeRange::duplicate(Lorg/xwiki/gwt/dom/client/internal/ie/NativeRange;)(savedRange);
        }
    }-*/;

    /**
     * Cancels the current selection and sets the selection type to none.
     */
    public native void empty()
    /*-{
        if (typeof(this.ownerDocument.parentWindow.__xwe_savedRange) == 'undefined') {
            try {
                // Try canceling the current selection. This throws an exception if the parent window is hidden.
                this.empty();
            } catch(e) {
                // Do nothing.
            }
        } else {
            // Save an invalid range.
            this.ownerDocument.parentWindow.__xwe_savedRange = {select : function(){}};
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
