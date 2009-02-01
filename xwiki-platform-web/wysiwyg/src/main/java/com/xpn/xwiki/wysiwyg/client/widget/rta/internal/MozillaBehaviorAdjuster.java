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

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Event;
import com.xpn.xwiki.wysiwyg.client.dom.Range;

/**
 * Adjusts the behavior of the rich text area in Mozilla based browsers, like Firefox.
 * 
 * @version $Id$
 */
public class MozillaBehaviorAdjuster extends BehaviorAdjuster
{
    /**
     * The CSS class name associated with BRs we have to add inside empty block elements to make them editable in
     * Mozilla.
     */
    public static final String EMPTY_LINE = "emptyLine";

    /**
     * The class name attribute.
     */
    public static final String CLASS_NAME = "class";

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
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which causes the caret to be rendered on the same line after you press
     * Enter, if the new line doesn't have any visible contents. Once you start typing the caret moves below, but it
     * looks strange before you type. We fixed the bug by adding a BR at the end of the new line.
     * 
     * @see BehaviorAdjuster#onEnterParagraphOnce(Node, Range)
     */
    protected void onEnterParagraphOnce(Node container, Range range)
    {
        super.onEnterParagraphOnce(container, range);

        // Start container should be a text node.
        Node lastLeaf;
        Node leaf = range.getStartContainer();
        // Look if there is any visible element on the new line, taking care to remain in the current block container.
        do {
            if (needsSpace(leaf)) {
                return;
            }
            lastLeaf = leaf;
            leaf = DOMUtils.getInstance().getNextLeaf(leaf);
        } while (leaf != null && container == DOMUtils.getInstance().getNearestBlockContainer(leaf));
        // It seems there's no visible element on the new line. We should add one.
        DOMUtils.getInstance().insertAfter(getTextArea().getDocument().xCreateBRElement(), lastLeaf);
    }

    /**
     * @param leaf A DOM node which has not children.
     * @return true if the given leaf needs space on the screen in order to be rendered.
     */
    private boolean needsSpace(Node leaf)
    {
        switch (leaf.getNodeType()) {
            case Node.TEXT_NODE:
                return leaf.getNodeValue().length() > 0;
            case Node.ELEMENT_NODE:
                Element element = Element.as(leaf);
                return BR.equalsIgnoreCase(element.getTagName()) || element.getOffsetHeight() > 0
                    || element.getOffsetWidth() > 0;
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which makes empty paragraphs invisible. We add a BR to the created
     * paragraph if it's empty.
     * 
     * @see BehaviorAdjuster#onEnterParagraphTwice(Node, Range)
     */
    protected void onEnterParagraphTwice(Node container, Range range)
    {
        super.onEnterParagraphTwice(container, range);

        // The start point of the range should have been placed inside the new paragraph.
        Node paragraph = DOMUtils.getInstance().getNearestBlockContainer(range.getStartContainer());
        // Look if there is any visible element on the new line, taking care to remain in the current block container.
        Node leaf = DOMUtils.getInstance().getFirstLeaf(paragraph);
        do {
            if (needsSpace(leaf)) {
                return;
            }
            leaf = DOMUtils.getInstance().getNextLeaf(leaf);
        } while (leaf != null && paragraph == DOMUtils.getInstance().getNearestBlockContainer(leaf));
        // It seems there's no visible element inside the newly created paragraph. We should add one.
        paragraph.appendChild(getTextArea().getDocument().xCreateBRElement());
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which makes empty paragraphs invisible. We add a BR to the newly
     * created paragraph.
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
        }
        paragraph.appendChild(getTextArea().getDocument().xCreateBRElement());
    }

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which makes empty paragraphs invisible. We add a BR to the newly
     * created paragraph.
     * 
     * @see BehaviorAdjuster#replaceEmptyDivsWithParagraphs()
     */
    protected void replaceEmptyDivsWithParagraphs()
    {
        super.replaceEmptyDivsWithParagraphs();

        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> paragraphs = document.getBody().getElementsByTagName("p");
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Node paragraph = paragraphs.getItem(i);
            if (!paragraph.hasChildNodes()) {
                // The user cannot place the caret inside an empty paragraph in Firefox. The workaround to make an empty
                // paragraph editable is to append a BR.
                paragraph.appendChild(document.xCreateBRElement());
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#onBeforeBlur()
     */
    protected void onBeforeBlur()
    {
        super.onBeforeBlur();
        // The edited content might be submitted so we have to mark the BRs that have been added to allow the user to
        // edit the empty block elements. These BRs will be removed from rich text area's HTML output on the server
        // side.
        markUnwantedBRs();
    }

    /**
     * Marks the BRs that generate empty lines. These BRs were added to overcome a Mozilla bug that prevents us from
     * typing inside an empty block level element.
     */
    protected void markUnwantedBRs()
    {
        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> brs = document.getBody().getElementsByTagName(BR);
        for (int i = 0; i < brs.getLength(); i++) {
            Element br = brs.getItem(i).cast();
            Node container = DOMUtils.getInstance().getNearestBlockContainer(br);
            Node leaf = DOMUtils.getInstance().getNextLeaf(br);
            boolean emptyLine = true;
            // Look if there is any visible element on the new line, taking care to remain in the current block
            // container.
            while (leaf != null && container == DOMUtils.getInstance().getNearestBlockContainer(leaf)) {
                if (needsSpace(leaf)) {
                    emptyLine = false;
                    break;
                }
                leaf = DOMUtils.getInstance().getNextLeaf(leaf);
            }
            if (emptyLine) {
                br.setClassName(EMPTY_LINE);
            } else {
                br.removeAttribute(CLASS_NAME);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#onFocus()
     */
    protected void onFocus()
    {
        super.onFocus();
        // It seems the edited content wasn't submitted so we have to unmark the unwanted BRs in order to avoid
        // conflicts with the rich text area's history mechanism.
        unmarkUnwantedBRs();
    }

    /**
     * @see #markUnwantedBRs()
     */
    protected void unmarkUnwantedBRs()
    {
        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> brs = document.getBody().getElementsByTagName(BR);
        for (int i = 0; i < brs.getLength(); i++) {
            Element br = (Element) brs.getItem(i);
            if (EMPTY_LINE.equals(br.getClassName())) {
                br.removeAttribute(CLASS_NAME);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see BehaviorAdjuster#navigateOutsideTableCell(boolean)
     */
    protected void navigateOutsideTableCell(boolean before)
    {
        super.navigateOutsideTableCell(before);

        Event event = getTextArea().getCurrentEvent();
        if (!event.isCancelled()) {
            return;
        }

        Document document = getTextArea().getDocument();
        document.getSelection().getRangeAt(0).getStartContainer().getParentNode().appendChild(
            document.xCreateBRElement());
    }
}
