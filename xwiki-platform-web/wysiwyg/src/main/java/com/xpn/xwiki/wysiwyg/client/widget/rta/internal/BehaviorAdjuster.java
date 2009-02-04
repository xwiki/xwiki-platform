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
     * The CSS class name associated with BRs added at edit time to make items editable.
     */
    public static final String EMPTY_LINE = "emptyLine";

    /**
     * The class name attribute.
     */
    public static final String CLASS_NAME = "class";

    /**
     * The rich text area whose behavior is being adjusted.
     */
    private RichTextArea textArea;

    /**
     * Collection of DOM utility methods.
     */
    private DOMUtils domUtils = DOMUtils.getInstance();

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
        Event event = getTextArea().getCurrentEvent();
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyboardListener.KEY_DOWN:
                onDownArrow();
                break;
            case KeyboardListener.KEY_UP:
                onUpArrow();
                break;
            default:
                break;
        }
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
        } else if (!selection.isCollapsed()) {
            // Selection + Enter = Selection + Delete + Enter
            // NOTE: We cannot use Range#deleteContents because it may lead to DTD-invalid HTML. That's because it
            // operates on any DOM tree without taking care of the underlying XML syntax, (X)HTML in our case. Let's use
            // the Delete command instead which is HTML-aware.
            getTextArea().getCommandManager().execute(Command.DELETE);
        }

        // At this point the selection should be collapsed.
        Range range = selection.getRangeAt(0);
        Node ancestor = domUtils.getNearestBlockContainer((range.getStartContainer()));
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
        return domUtils.getFirstLeaf(container) == domUtils.getFirstLeaf(range.getStartContainer());
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
                leaf = domUtils.getLastLeaf(leaf);
            } else {
                // We are in the middle of the text.
                return false;
            }
        } else {
            leaf = domUtils.getPreviousLeaf(range.getStartContainer());
        }
        // We have to additionally test if the found BR is in the given container.
        return isBR(leaf) && container == domUtils.getNearestBlockContainer(leaf);
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
                domUtils.insertAfter(br, range.getStartContainer());
                break;
            case Node.TEXT_NODE:
                Node refNode = domUtils.splitNode(range.getStartContainer(), range.getStartOffset());
                refNode.getParentNode().insertBefore(br, refNode);
                break;
            case Node.ELEMENT_NODE:
                domUtils.insertAt(range.getStartContainer(), br, range.getStartOffset());
                break;
            default:
                break;
        }

        // Place the caret after the inserted BR.
        Node start = br.getNextSibling();
        if (start == null || start.getNodeType() != Node.TEXT_NODE) {
            start = getTextArea().getDocument().createTextNode("");
            domUtils.insertAfter(start, br);
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
                br = domUtils.getLastLeaf(br);
            } else {
                return;
            }
        } else {
            br = domUtils.getPreviousLeaf(range.getStartContainer());
        }

        // Create a new paragraph.
        Node paragraph = getTextArea().getDocument().xCreatePElement();

        // Split the container after the found BR.
        if (domUtils.isFlowContainer(container)) {
            Node child = domUtils.getChild(container, br);
            if (child != br) {
                domUtils.splitNode(container, br.getParentNode(), domUtils.getNodeIndex(br));
            }
            // Insert the created paragraph before the split.
            domUtils.insertAfter(paragraph, child);
            // Move all the in-line nodes after the split in the created paragraph.
            child = paragraph.getNextSibling();
            while (child != null && domUtils.isInline(child)) {
                paragraph.appendChild(child);
                child = paragraph.getNextSibling();
            }
        } else {
            domUtils.splitNode(container.getParentNode(), br.getParentNode(), domUtils.getNodeIndex(br));
            paragraph.appendChild(Element.as(container.getNextSibling()).extractContents());
            container.getParentNode().replaceChild(paragraph, container.getNextSibling());
        }
        br.getParentNode().removeChild(br);

        // Place the caret inside the created paragraph, at the beginning.
        Node start = domUtils.getFirstLeaf(paragraph);
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

        if (domUtils.isFlowContainer(container)) {
            // We are at the beginning of a flow container. Since it can contain block elements we insert the paragraph
            // before its first child.
            domUtils.insertAt(container, paragraph, 0);
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
        if (nextCell == null) {
            if (getTextArea().getCurrentEvent().getShiftKey()) {
                return;
            } else {
                getTextArea().getCommandManager().execute(new Command("insertrowafter"));
                nextCell = cell.getNextCell();
            }
        }

        Selection selection = getTextArea().getDocument().getSelection();
        Range range = selection.getRangeAt(0);

        // Place the caret at the beginning of the next cell.
        Node leaf = domUtils.getFirstLeaf(nextCell);
        if (leaf == nextCell || leaf.getNodeType() == Node.TEXT_NODE) {
            range.setStart(leaf, 0);
        } else {
            range.setStartBefore(leaf);
        }

        range.collapse(true);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * Overwrites the default rich text area behavior when the Down arrow key is being pressed.
     */
    protected void onDownArrow()
    {
        navigateOutsideTableCell(false);
    }

    /**
     * Overwrites the default rich text area behavior when the Up arrow key is being pressed.
     */
    protected void onUpArrow()
    {
        navigateOutsideTableCell(true);
    }

    /**
     * Inserts a paragraph before or after the table containing the selection.
     * <p>
     * We decided to use Ctrl+UpArrow for inserting a paragraph before a table and Ctrl+DownArrow for inserting a
     * paragraph after a table. Here's the rationale:
     * <ul>
     * <li>We can't reliably detect if the user can place the caret before of after a table. Our playground is the DOM
     * tree which we fully control but in the end the browser decides how to render each node. The table can have
     * previous siblings or previous nodes in the DOM tree but they may not be rendered at all (as it happens with HTML
     * garbage like empty elements) or not rendered before/after the table (as it happens with absolute positioned
     * elements). So we have to insert the paragraph each time.</li>
     * <li>We can't use the same key to insert a paragraph before and after the table because we can have a table with
     * just one empty cell. So we can't rely on the Enter key.</li>
     * <li>We can't use just the navigation keys because they would insert a paragraph before/after the table even when
     * the user can navigate outside of the table.</li>
     * </ul>
     * We can replace the Ctrl with Alt. The idea is to use the Up/Down arrow keys with a modifier. They will work form
     * any table cell.
     * 
     * @param before {@code true} to insert a paragraph before the table, {@code false} to insert a paragraph after the
     *            table
     */
    protected void navigateOutsideTableCell(boolean before)
    {
        Event event = getTextArea().getCurrentEvent();
        if (!event.getCtrlKey() || event.getAltKey() || event.getShiftKey() || event.getMetaKey()) {
            return;
        }

        Selection selection = getTextArea().getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            return;
        } else {
            selection.collapseToStart();
        }

        Range range = selection.getRangeAt(0);
        Node ancestor = domUtils.getFirstAncestor(range.getStartContainer(), "table");
        if (ancestor == null) {
            return;
        }

        event.xPreventDefault();

        Document document = getTextArea().getDocument();
        Node paragraph = document.xCreatePElement();
        paragraph.appendChild(document.createTextNode(""));

        if (before) {
            ancestor.getParentNode().insertBefore(paragraph, ancestor);
        } else {
            domUtils.insertAfter(paragraph, ancestor);
        }

        range.selectNodeContents(paragraph.getFirstChild());
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * Called before the underlying rich text area looses focus.
     */
    protected void onBeforeBlur()
    {
        // The edited content might be submitted so we have to mark the BRs that have been added to allow the user to
        // edit the empty block elements. These BRs will be removed from rich text area's HTML output on the server
        // side.
        markUnwantedBRs();
    }

    /**
     * Called each time the underlying rich text area gains the focus.
     */
    protected void onFocus()
    {
        // It seems the edited content wasn't submitted so we have to unmark the unwanted BRs in order to avoid
        // conflicts with the rich text area's history mechanism.
        unmarkUnwantedBRs();
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

    /**
     * @param leaf A DOM node which has not children.
     * @return true if the given leaf needs space on the screen in order to be rendered.
     */
    protected boolean needsSpace(Node leaf)
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
}
