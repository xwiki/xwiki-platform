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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;

/**
 * Adjusts the behavior of the rich text area in Mozilla based browsers, like Firefox.
 * 
 * @version $Id$
 */
public class MozillaBehaviorAdjuster extends BehaviorAdjuster
{
    public void onEnter()
    {
        if (getTextArea().getCurrentEvent().getShiftKey()) {
            // Keep the default behavior for soft return.
            return;
        }

        Selection selection = getTextArea().getDocument().getSelection();
        // We only take care of the first range.
        // The other ranges will be removed from the selection but their text will remain untouched.
        Range range = selection.getRangeAt(0);

        Node ancestor = range.getStartContainer();
        while (ancestor != null && DOMUtils.getInstance().isInline(ancestor)) {
            ancestor = ancestor.getParentNode();
        }
        if (ancestor == null) {
            // This shouln't happen!
            return;
        }

        String display = DOMUtils.getInstance().getDisplay(ancestor);
        if ("list-item".equalsIgnoreCase(display)) {
            // ignore
        } else if ("block".equalsIgnoreCase(display) && !"body".equalsIgnoreCase(ancestor.getNodeName())) {
            if ("p".equalsIgnoreCase(ancestor.getNodeName())) {
                Element paragraph = Element.as(ancestor);
                if (paragraph.getInnerText().length() == 0) {
                    // We are inside an empty paragraph. We'll behave as if the use pressed Shift+Return.
                    getTextArea().getCurrentEvent().preventDefault();

                    // Delete the text from the first range and leave the text of the other ranges untouched.
                    range.deleteContents();

                    // Create the line break and insert it before the current range.
                    Element br = getTextArea().getDocument().xCreateBRElement();
                    Node refNode = range.getStartContainer();
                    if (refNode.hasChildNodes()) {
                        refNode = refNode.getChildNodes().getItem(range.getStartOffset());
                    }
                    refNode.getParentNode().insertBefore(br, refNode);

                    // Update the current range
                    selection.removeAllRanges();
                    range = getTextArea().getDocument().createRange();
                    range.setStartBefore(refNode);
                    range.setEndBefore(refNode);
                    selection.addRange(range);
                } else {
                    // The selection starts inside a non-empty paragraph so we leave the default behavior.
                }
            }
        } else {
            // We are not inside a paragraph so we change the default behavior.
            getTextArea().getCurrentEvent().preventDefault();

            // Save the start container and offset to know later where to split the text.
            Node startContainer = range.getStartContainer();
            int startOffset = range.getStartOffset();

            // Delete the text from the first range and leave the text of the other ranges untouched.
            range.deleteContents();
            // Reset the selection. We're going to move the cursor inside the new paragraph.
            selection.removeAllRanges();

            // Split the DOM subtree that has the previously found ancestor as root.
            DOMUtils.getInstance().splitNode(ancestor, startContainer, startOffset);

            // Wrap left in-line siblings in a paragraph.
            Element leftParagraph = getTextArea().getDocument().xCreatePElement();
            Node splitRightNode = DOMUtils.getInstance().getChild(ancestor, startContainer).getNextSibling();
            Node leftSibling = splitRightNode.getPreviousSibling();
            if (leftSibling != null && DOMUtils.getInstance().isInline(leftSibling)) {
                leftParagraph.appendChild(leftSibling);
                leftSibling = splitRightNode.getPreviousSibling();
                while (leftSibling != null && DOMUtils.getInstance().isInline(leftSibling)) {
                    leftParagraph.insertBefore(leftSibling, leftParagraph.getFirstChild());
                    leftSibling = splitRightNode.getPreviousSibling();
                }
                ancestor.insertBefore(leftParagraph, splitRightNode);
            }

            // Wrap right in-line siblings in a paragraph.
            Element rightParagraph = getTextArea().getDocument().xCreatePElement();
            ancestor.replaceChild(rightParagraph, splitRightNode);
            rightParagraph.appendChild(splitRightNode);
            Node rightSibling = rightParagraph.getNextSibling();
            while (rightSibling != null && DOMUtils.getInstance().isInline(rightSibling)) {
                rightParagraph.appendChild(rightSibling);
                rightSibling = rightParagraph.getNextSibling();
            }

            // Create the new range and move the cursor inside the new paragraph.
            range = getTextArea().getDocument().createRange();
            range.selectNodeContents(DOMUtils.getInstance().getFirstLeaf(rightParagraph));
            range.collapse(true);
            selection.addRange(range);
        }
    }

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
}
