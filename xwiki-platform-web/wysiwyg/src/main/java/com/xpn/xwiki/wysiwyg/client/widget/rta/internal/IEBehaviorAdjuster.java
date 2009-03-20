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

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Adjusts the behavior of the rich text area in Internet Explorer browsers.
 * 
 * @version $Id$
 */
public class IEBehaviorAdjuster extends BehaviorAdjuster
{
    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        super.onLoad(sender);
        ensureSelectionIsPreserved(getTextArea().getDocument());
    }

    /**
     * Ensures that the selection of the given document is preserved when the document looses focus. This method is
     * required because IE has only one selection object per top level window. This means that when a child document
     * looses the focus its selection object will return ranges from the parent document.
     * 
     * @param document the document whose selection has to be preserved
     */
    private native void ensureSelectionIsPreserved(Document document)
    /*-{
        // If there is a previously stored selection then restore it. We have to do this before the edited document
        // gets focused to allow users to have a different selection than the stored one (by clicking inside the edited
        // document when it doesn't have the focus).
        document.body.attachEvent('onbeforeactivate', function(event) {
            // Save the bookmark locally to prevent any interference.
            var bookmark = document.body.__bookmark;
            // Reset the bookmark to prevent redundant calls to this function.
            document.body.__bookmark = null;
            switch (typeof(bookmark)) {
                case 'string':
                    // The bookmark is an opaque string that can be used with moveToBookmark to recreate the original
                    // text range.
                    var textRange = document.body.createTextRange();
                    textRange.moveToBookmark(bookmark);
                    textRange.select();
                    break;
                case 'object':
                    // The bookmark is a reference to the element previously selected.
                    var controlRange = document.body.createControlRange();
                    controlRange.addElement(bookmark);
                    controlRange.select();
                    break;
            }
        });

        // Save the selection when the edited document is about to loose focus.
        document.body.attachEvent('onbeforedeactivate', function(event) {
            document.body.__bookmark = null;
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
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#adjustDragDrop(Document)
     */
    public native void adjustDragDrop(Document document)
    /*-{
        // block default drag and drop mechanism to not allow content to be dropped on this document 
        document.body.attachEvent("ondrop", function(event) {
            event.returnValue = false;
        });
        // block dragging from this object too, because default behaviour is to cut & paste and 
        // we loose content from the editor 
        document.body.attachEvent("ondrag", function(event) {
            event.returnValue = false;
        });
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#onKeyDown()
     */
    protected void onKeyDown()
    {
        Event event = getTextArea().getCurrentEvent();
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyboardListener.KEY_TAB:
                // IE moves the focus when Tab key is down and thus the key press event doesn't get fired. If we block
                // the key down event then IE doesn't fire key press. We are forced to apply out custom behavior for tab
                // key now, on key down, and not later, on key press.
                onTab();
                break;
            default:
                super.onKeyDown();
                break;
        }
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a IE bug which makes empty paragraphs invisible. Setting the inner HTML to the empty
     * string seems to do the trick.
     * 
     * @see BehaviorAdjuster#onEnterParagraphThrice(Node, Range)
     */
    protected void onEnterParagraphThrice(Node container, Range range)
    {
        super.onEnterParagraphThrice(container, range);

        Node paragraph;
        if (DOMUtils.getInstance().isFlowContainer(container)) {
            paragraph = container.getFirstChild();
        } else {
            paragraph = container.getPreviousSibling();

            if (!container.hasChildNodes()) {
                // If the caret is inside an empty block level container and we insert a new paragraph before then the
                // caret doesn't remain in its place. We have to reset the caret.
                container.appendChild(container.getOwnerDocument().createTextNode(""));
                range.selectNodeContents(container.getFirstChild());
            }
        }
        // Empty paragraphs are not displayed in IE. Strangely, setting the inner HTML to the empty string
        // forces IE to render the empty paragraphs. Appending an empty text node doesn't help.
        Element.as(paragraph).setInnerHTML("");
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a IE bug which makes empty paragraphs invisible. Setting the inner HTML to the empty
     * string seems to do the trick.
     * 
     * @see BehaviorAdjuster#replaceEmptyDivsWithParagraphs()
     */
    protected void replaceEmptyDivsWithParagraphs()
    {
        super.replaceEmptyDivsWithParagraphs();

        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> paragraphs = document.getBody().getElementsByTagName("p");
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = paragraphs.getItem(i).cast();
            if (!paragraph.hasChildNodes()) {
                // Empty paragraphs are not displayed in IE. Strangely, setting the inner HTML to the empty string
                // forces IE to render the empty paragraphs. Appending an empty text node doesn't help.
                paragraph.setInnerHTML("");
            }
        }
    }
}
