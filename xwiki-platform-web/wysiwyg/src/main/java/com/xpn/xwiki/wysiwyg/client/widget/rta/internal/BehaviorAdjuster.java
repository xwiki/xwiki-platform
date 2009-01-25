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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.SourcesLoadEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Event;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.dom.TableCellElement;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Adjusts the behavior of the rich text area to meet the cross browser specification.<br/>
 * The built-in WYSIWYG editor provided by all modern browsers may react differently to user input (like typing) on
 * different browsers. This class serves as a base class for browser specific behavior adjustment.
 * 
 * @version $Id$
 */
public class BehaviorAdjuster implements LoadListener
{
    /**
     * The name of the <code>&lt;br/&gt;</code> tag.
     */
    public static final String BR = "br";

    /**
     * The name of the <code>&lt;li&gt;</code> tag.
     */
    public static final String LI = "li";

    /**
     * The name of the <code>&lt;td&gt;</code> tag.
     */
    public static final String TD = "td";

    /**
     * The name of the <code>&lt;th&gt;</code> tag.
     */
    public static final String TH = "th";

    /**
     * The rich text area whose behavior is being adjusted.
     */
    private RichTextArea textArea;

    /**
     * @return The rich text area whose behavior is being adjusted.
     */
    public RichTextArea getTextArea()
    {
        return textArea;
    }

    /**
     * NOTE: We were forced to add this method because instances of this class are created using deferred binding and
     * thus we cannot pass the rich text area as a parameter to the constructor. As a consequence this method can be
     * called only once.
     * 
     * @param textArea The textArea whose behavior needs to be adjusted.
     */
    public void setTextArea(RichTextArea textArea)
    {
        if (this.textArea != null) {
            throw new IllegalStateException("Text area has already been set!");
        }
        this.textArea = textArea;
        // Workaround till GWT provides a way to detect when the rich text area has finished loading.
        if (textArea.getBasicFormatter() != null && textArea.getBasicFormatter() instanceof SourcesLoadEvents) {
            ((SourcesLoadEvents) textArea.getBasicFormatter()).addLoadListener(this);
        }
    }

    /**
     * Called by the underlying rich text are when user actions trigger browser events, before any registered listener
     * is notified.
     * 
     * @see RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     * @see RichTextArea#getCurrentEvent()
     */
    public void onBeforeBrowserEvent()
    {
        Event event = getTextArea().getCurrentEvent();
        switch (event.getTypeInt()) {
            case Event.ONBLUR:
                onBeforeBlur();
                break;
            default:
                break;
        }
    }

    /**
     * Called by the underlying rich text are when user actions trigger browser events, after all the registered
     * listeners have been notified.
     * 
     * @see RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     * @see RichTextArea#getCurrentEvent()
     */
    public void onBrowserEvent()
    {
        Event event = getTextArea().getCurrentEvent();
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getTypeInt()) {
            case Event.ONKEYDOWN:
                onKeyDown();
                break;
            case Event.ONKEYPRESS:
                onKeyPress();
                break;
            case Event.ONFOCUS:
                onFocus();
                break;
            default:
                break;
        }
    }

    /**
     * Called when a KeyDown event is triggered inside the rich text area.
     */
    protected void onKeyDown()
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Called when a KeyPress event is triggered inside the rich text area.
     */
    protected void onKeyPress()
    {
        Event event = getTextArea().getCurrentEvent();
        switch (event.getKeyCode()) {
            case KeyboardListener.KEY_ENTER:
                onEnter();
                break;
            case KeyboardListener.KEY_TAB:
                onTab();
                break;
            default:
                break;
        }
    }

    /**
     * Overwrites the default rich text area behavior when the Enter key is being pressed.
     */
    protected void onEnter()
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            return;
        }
        Range range = selection.getRangeAt(0);
        Node ancestor = DOMUtils.getInstance().getNearestBlockContainer((range.getStartContainer()));
        String tagName = ancestor.getNodeName().toLowerCase();
        if (LI.equals(tagName)) {
            // Leave the default behavior for now.
        } else if (TD.equals(tagName) || TH.equals(tagName)) {
            onEnterTableCell(ancestor);
        } else {
            onEnterParagraph(ancestor);
        }
    }

    /**
     * Handles the case when Enter is pressed inside a table cell.
     * 
     * @param cell The table cell where the Enter has been pressed.
     */
    protected void onEnterTableCell(Node cell)
    {
        // Cancel the event to prevent its default behavior.
        getTextArea().getCurrentEvent().xPreventDefault();

        Selection selection = getTextArea().getDocument().getSelection();
        Range range = selection.getRangeAt(0);

        // If range ends with a BR tag then we must not delete it.
        if (!range.isCollapsed()) {
            Node leaf = DOMUtils.getInstance().getLastLeaf(range);
            if (BR.equalsIgnoreCase(leaf.getNodeName())) {
                range.setEndBefore(leaf);
            }
        }
        // Delete the text from the first range and leave the text of the other ranges untouched.
        range.deleteContents();

        onEnterParagraphOnce(cell, range);

        range.collapse(true);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * Behaves as if the caret is inside a paragraph. Precisely:
     * <ul>
     * <li>1 return key generates a line break (BR)</li>
     * <li>2 consecutive return keys generate a paragraph</li>
     * <li>3 or more consecutive return keys generate div's with class <em>wikimodel-emptyline</em> (their purpose is to
     * separate block level elements).</li>
     * </ul>
     * 
     * @param container The block level element containing the start of the first range.
     */
    protected void onEnterParagraph(Node container)
    {
        // Cancel the event to prevent its default behavior.
        getTextArea().getCurrentEvent().xPreventDefault();

        Selection selection = getTextArea().getDocument().getSelection();
        Range range = selection.getRangeAt(0);

        // If range ends with a BR tag then we must not delete it.
        if (!range.isCollapsed()) {
            Node leaf = DOMUtils.getInstance().getLastLeaf(range);
            if (BR.equalsIgnoreCase(leaf.getNodeName())) {
                range.setEndBefore(leaf);
            }
        }
        // Delete the text from the first range and leave the text of the other ranges untouched.
        range.deleteContents();

        if (isAtStart(container, range)) {
            onEnterParagraphThrice(container, range);
        } else if (isAfterBR(container, range)) {
            onEnterParagraphTwice(container, range);
        } else {
            onEnterParagraphOnce(container, range);
        }

        range.collapse(true);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * @param container A block level element containing the start of the given range.
     * @param range A DOM range.
     * @return true if the start of the given range is at the beginning of its block level container.
     */
    protected boolean isAtStart(Node container, Range range)
    {
        if (!container.hasChildNodes()) {
            return true;
        }
        if (range.getStartOffset() > 0) {
            return false;
        }
        return DOMUtils.getInstance().getFirstLeaf(container) == DOMUtils.getInstance().getFirstLeaf(
            range.getStartContainer());
    }

    /**
     * @param container A block level element containing the start of the given range.
     * @param range A DOM range.
     * @return true if the start of the given range is immediately after a BR element.
     */
    protected boolean isAfterBR(Node container, Range range)
    {
        Node leaf;
        if (range.getStartOffset() > 0) {
            if (range.getStartContainer().getNodeType() == Node.ELEMENT_NODE) {
                leaf = range.getStartContainer().getChildNodes().getItem(range.getStartOffset() - 1);
                leaf = DOMUtils.getInstance().getLastLeaf(leaf);
            } else {
                // We are in the middle of the text.
                return false;
            }
        } else {
            leaf = DOMUtils.getInstance().getPreviousLeaf(range.getStartContainer());
        }
        // We have to additionally test if the found BR is in the given container.
        return isBR(leaf) && container == DOMUtils.getInstance().getNearestBlockContainer(leaf);
    }

    /**
     * @param node A DOM node.
     * @return true if the given node is a BR element.
     */
    protected boolean isBR(Node node)
    {
        return node != null && BR.equalsIgnoreCase(node.getNodeName());
    }

    /**
     * Enter has been pressed once inside a block level element.
     * 
     * @param container The block level element.
     * @param range The caret.
     */
    protected void onEnterParagraphOnce(Node container, Range range)
    {
        // Insert the BR.
        Node br = getTextArea().getDocument().xCreateBRElement();
        switch (range.getStartContainer().getNodeType()) {
            case DOMUtils.CDATA_NODE:
            case DOMUtils.COMMENT_NODE:
                DOMUtils.getInstance().insertAfter(br, range.getStartContainer());
                break;
            case Node.TEXT_NODE:
                Node refNode = DOMUtils.getInstance().splitNode(range.getStartContainer(), range.getStartOffset());
                refNode.getParentNode().insertBefore(br, refNode);
                break;
            case Node.ELEMENT_NODE:
                DOMUtils.getInstance().insertAt(range.getStartContainer(), br, range.getStartOffset());
                break;
            default:
                break;
        }

        // Place the caret after the inserted BR.
        Node start = br.getNextSibling();
        if (start == null || start.getNodeType() != Node.TEXT_NODE) {
            start = getTextArea().getDocument().createTextNode("");
            DOMUtils.getInstance().insertAfter(start, br);
        }
        range.setStart(start, 0);
    }

    /**
     * Enter has been pressed twice inside a block level element.
     * 
     * @param container The block level element containing the start of the given range.
     * @param range The caret.
     */
    protected void onEnterParagraphTwice(Node container, Range range)
    {
        Node br;
        // Find the BR.
        if (range.getStartOffset() > 0) {
            if (range.getStartContainer().getNodeType() == Node.ELEMENT_NODE) {
                br = range.getStartContainer().getChildNodes().getItem(range.getStartOffset() - 1);
                br = DOMUtils.getInstance().getLastLeaf(br);
            } else {
                return;
            }
        } else {
            br = DOMUtils.getInstance().getPreviousLeaf(range.getStartContainer());
        }

        // Create a new paragraph.
        Node paragraph = getTextArea().getDocument().xCreatePElement();

        // Split the container after the found BR.
        if (DOMUtils.getInstance().isFlowContainer(container)) {
            Node child = DOMUtils.getInstance().getChild(container, br);
            if (child != br) {
                DOMUtils.getInstance()
                    .splitNode(container, br.getParentNode(), DOMUtils.getInstance().getNodeIndex(br));
            }
            // Insert the created paragraph before the split.
            DOMUtils.getInstance().insertAfter(paragraph, child);
            // Move all the in-line nodes after the split in the created paragraph.
            child = paragraph.getNextSibling();
            while (child != null && DOMUtils.getInstance().isInline(child)) {
                paragraph.appendChild(child);
                child = paragraph.getNextSibling();
            }
        } else {
            DOMUtils.getInstance().splitNode(container.getParentNode(), br.getParentNode(),
                DOMUtils.getInstance().getNodeIndex(br));
            paragraph.appendChild(Element.as(container.getNextSibling()).extractContents());
            container.getParentNode().replaceChild(paragraph, container.getNextSibling());
        }
        br.getParentNode().removeChild(br);

        // Place the caret inside the created paragraph, at the beginning.
        Node start = DOMUtils.getInstance().getFirstLeaf(paragraph);
        if (start == paragraph) {
            start = getTextArea().getDocument().createTextNode("");
            paragraph.appendChild(start);
        }
        if (start.getNodeType() == Node.ELEMENT_NODE) {
            range.setStartBefore(start);
        } else {
            range.setStart(start, 0);
        }
    }

    /**
     * Enter has been pressed thrice inside a block level element. We must be at the beginning of the given block so we
     * insert an empty line before it. In other words we move the block one line below.
     * 
     * @param container The block level element.
     * @param range The caret.
     */
    protected void onEnterParagraphThrice(Node container, Range range)
    {
        Document document = getTextArea().getDocument();
        // Create a new paragraph.
        Element paragraph = document.xCreatePElement().cast();

        if (DOMUtils.getInstance().isFlowContainer(container)) {
            // We are at the beginning of a flow container. Since it can contain block elements we insert the paragraph
            // before its first child.
            DOMUtils.getInstance().insertAt(container, paragraph, 0);
            // We place the caret after the inserted paragraph.
            range.setStartAfter(paragraph);
        } else {
            // Insert the paragraph before the container.
            container.getParentNode().insertBefore(paragraph, container);
        }
    }

    /**
     * Overwrites the default rich text area behavior when the Tab key is being pressed.
     */
    protected void onTab()
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            return;
        }

        // Prevent the default browser behavior.
        getTextArea().getCurrentEvent().xPreventDefault();

        // See in which context the tab key has been pressed.
        Range range = selection.getRangeAt(0);
        List<String> specialTags = Arrays.asList(new String[] {LI, TD, TH});
        Node ancestor = range.getStartContainer();
        int index = specialTags.indexOf(ancestor.getNodeName().toLowerCase());
        while (ancestor != null && index < 0) {
            ancestor = ancestor.getParentNode();
            if (ancestor != null) {
                index = specialTags.indexOf(ancestor.getNodeName().toLowerCase());
            }
        }

        // Handle the tab key depending on the context.
        switch (index) {
            case 0:
                onTabInListItem(ancestor);
                break;
            case 1:
            case 2:
                onTabInTableCell((TableCellElement) ancestor);
                break;
            default:
                onTabDefault();
                break;
        }
    }

    /**
     * Tab key has been pressed in an ordinary context. If the Shift key was not pressed then the current selection will
     * be replaced by 4 spaces. Otherwise no action will be taken.
     */
    protected void onTabDefault()
    {
        if (getTextArea().getCurrentEvent().getShiftKey()) {
            // Do nothing.
        } else {
            if (getTextArea().getCommandManager().isEnabled(Command.INSERT_HTML)) {
                getTextArea().getCommandManager().execute(Command.INSERT_HTML, "&nbsp;&nbsp;&nbsp;&nbsp;");
                getTextArea().getDocument().getSelection().collapseToEnd();
            }
        }
    }

    /**
     * Tab key has been pressed inside a list item. If the selection is collapsed at the beginning of a list item then
     * indent or outdent that list item depending on the Shift key. Otherwise use the default behavior for Tab key.
     * 
     * @param item The list item in which the tab key has been pressed.
     */
    protected void onTabInListItem(Node item)
    {
        Range range = getTextArea().getDocument().getSelection().getRangeAt(0);
        if (!range.isCollapsed() || !isAtStart(item, range)) {
            onTabDefault();
        } else {
            Command command = getTextArea().getCurrentEvent().getShiftKey() ? Command.OUTDENT : Command.INDENT;
            if (getTextArea().getCommandManager().isEnabled(command)) {
                getTextArea().getCommandManager().execute(command);
            }
        }
    }

    /**
     * Tab key has been pressed inside a table cell.
     * 
     * @param cell The table cell in which the tab key has been pressed.
     */
    protected void onTabInTableCell(TableCellElement cell)
    {
        Node nextCell = getTextArea().getCurrentEvent().getShiftKey() ? cell.getPreviousCell() : cell.getNextCell();
        if (nextCell != null) {
            Selection selection = getTextArea().getDocument().getSelection();
            Range range = selection.getRangeAt(0);

            // Place the caret at the beginning of the next cell.
            Node leaf = DOMUtils.getInstance().getFirstLeaf(nextCell);
            if (leaf == nextCell || leaf.getNodeType() == Node.TEXT_NODE) {
                range.setStart(leaf, 0);
            } else {
                range.setStartBefore(leaf);
            }

            range.collapse(true);
            selection.removeAllRanges();
            selection.addRange(range);
        }
    }

    /**
     * Called before the underlying rich text area looses focus.
     */
    protected void onBeforeBlur()
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Called each time the underlying rich text area gains the focus.
     */
    protected void onFocus()
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onError(Widget)
     */
    public void onError(Widget sender)
    {
        // Nothing to do upon load error.
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        adjustDragDrop(textArea.getDocument());
        replaceEmptyDivsWithParagraphs();
    }

    /**
     * Prevents the drag and drop default behavior by disabling the default events.
     * 
     * @param document the document in the loaded rich text area.
     */
    protected void adjustDragDrop(Document document)
    {
        // Nothing here by default. May be overridden by browser specific implementations.
    }

    /**
     * Replaces empty DIVs with paragraphs. Empty DIVs are used by Wikimodel as empty lines between block level
     * elements, but since the user should be able to write on these empty lines we convert them to paragraphs.
     */
    protected void replaceEmptyDivsWithParagraphs()
    {
        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> divs = document.getBody().getElementsByTagName("div");
        // Since NodeList is updated when one of its nodes are detached, we have to store the empty DIVs in a separate
        // list.
        List<Node> emptyDivs = new ArrayList<Node>();
        for (int i = 0; i < divs.getLength(); i++) {
            Node div = divs.getItem(i);
            if (!div.hasChildNodes()) {
                emptyDivs.add(div);
            }
        }
        // Replace the empty DIVs with paragraphs.
        for (Node div : emptyDivs) {
            div.getParentNode().replaceChild(document.xCreatePElement(), div);
        }
    }
}
