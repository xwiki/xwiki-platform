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
package com.xpn.xwiki.wysiwyg.client.plugin.line;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;

/**
 * Overwrites the behavior of creating new lines of text and merging existing ones.
 * 
 * @version $Id$
 */
public class LinePlugin extends AbstractPlugin implements KeyboardListener, CommandListener
{
    /**
     * The command that stores the value of the rich text area in an HTML form field.
     */
    public static final Command SUBMIT = new Command("submit");

    /**
     * The command that notifies us when the content of the rich text area has been reset.
     */
    public static final Command RESET = new Command("reset");

    /**
     * The CSS class name associated with BRs added at edit time to make items like empty block-level elements editable.
     */
    public static final String SPACER = "spacer";

    /**
     * The CSS class name associated with BRs that are present in the rich text area's HTML input and which have to be
     * kept even when they are placed at the end of a block-level element. We need to mark this initial BRs so they are
     * not mistaken with the {@link #SPACER} BRs we add during the editing.
     */
    public static final String LINE_BREAK = "lineBreak";

    /**
     * The class name attribute.
     */
    public static final String CLASS_NAME = "class";

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
     * Collection of DOM utility methods.
     */
    protected final DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * Flag used to avoid handling both KeyDown and KeyPress events. This flag is needed because of the inconsistencies
     * between browsers regarding keyboard events. For instance IE doesn't generate the KeyPress event for backspace key
     * and generates multiple KeyDown events while a key is hold down. On the contrary, FF generates the KeyPress event
     * for the backspace key and generates just one KeyDown event while a key is hold down. FF generates multiple
     * KeyPress events when a key is hold down.
     */
    private boolean ignoreNextKeyPress;

    /**
     * Flag used to prevent the default browser behavior for the KeyPress event when the KeyDown event has been
     * canceled. This is needed only in functional tests where keyboard events (KeyDown, KeyPress, KeyUp) are triggered
     * independently and thus canceling KeyDown doesn't prevent the default KeyPress behavior. Without this flag, and
     * because we have to handle the KeyDown event besides the KeyPress in order to overcome cross-browser
     * inconsistencies, simulating keyboard typing in functional tests would trigger our custom behavior but also the
     * default browser behavior.
     */
    private boolean cancelNextKeyPress;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        getTextArea().addKeyboardListener(this);
        getTextArea().getCommandManager().addCommandListener(this);

        // Adjust the initial content of the rich text area.
        onReset();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        getTextArea().removeKeyboardListener(this);
        getTextArea().getCommandManager().removeCommandListener(this);

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        if (sender == getTextArea()) {
            ignoreNextKeyPress = true;
            handleRepeatableKey(keyCode, modifiers);
            cancelNextKeyPress = getTextArea().getCurrentEvent().isCancelled();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        if (sender == getTextArea()) {
            if (!ignoreNextKeyPress) {
                handleRepeatableKey(keyCode, modifiers);
            } else if (cancelNextKeyPress) {
                getTextArea().getCurrentEvent().xPreventDefault();
            }
            ignoreNextKeyPress = false;
            cancelNextKeyPress = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        ignoreNextKeyPress = false;
        cancelNextKeyPress = false;
    }

    /**
     * Handles a repeatable key press.
     * 
     * @param keyCode the Unicode character that was generated by the keyboard action
     * @param modifiers the modifier keys pressed when the event occurred
     */
    protected void handleRepeatableKey(int keyCode, int modifiers)
    {
        switch (keyCode) {
            case KeyboardListener.KEY_ENTER:
                onEnter(modifiers);
                break;
            case KeyboardListener.KEY_BACKSPACE:
                onBackspace();
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        if (SUBMIT.equals(command)) {
            // The edited content might be submitted so we have to mark the BRs that have been added to allow the user
            // to edit the empty block elements. These BRs will be removed from rich text area's HTML output on the
            // server side.
            markSpacers();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if (SUBMIT.equals(command)) {
            // Revert the changes made on before submit command in order avoid conflicts with the rich text area's
            // history mechanism.
            unMarkSpacers();
        } else if (RESET.equals(command)) {
            onReset();
        }
    }

    /**
     * Marks the BRs that have been added as spacers during the editing. These BRs were added to overcome a Mozilla bug
     * that prevents us from typing inside an empty block level element.
     */
    protected void markSpacers()
    {
        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> brs = document.getBody().getElementsByTagName(BR);
        for (int i = 0; i < brs.getLength(); i++) {
            Element br = brs.getItem(i).cast();
            // Ignore the BRs that have been there from the beginning.
            if (LINE_BREAK.equals(br.getClassName())) {
                continue;
            }
            Node container = domUtils.getNearestBlockContainer(br);
            Node leaf = domUtils.getNextLeaf(br);
            boolean emptyLine = true;
            // Look if there is any visible element on the new line, taking care to remain in the current block
            // container.
            while (leaf != null && container == domUtils.getNearestBlockContainer(leaf)) {
                if (needsSpace(leaf)) {
                    emptyLine = false;
                    break;
                }
                leaf = domUtils.getNextLeaf(leaf);
            }
            if (emptyLine) {
                br.setClassName(SPACER);
            } else {
                br.removeAttribute(CLASS_NAME);
            }
        }
    }

    /**
     * @see #markSpacers()
     */
    protected void unMarkSpacers()
    {
        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> brs = document.getBody().getElementsByTagName(BR);
        for (int i = 0; i < brs.getLength(); i++) {
            Element br = (Element) brs.getItem(i);
            if (SPACER.equals(br.getClassName())) {
                br.removeAttribute(CLASS_NAME);
            }
        }
    }

    /**
     * @param leaf a DOM node which has not children
     * @return {@code true} if the given leaf needs space on the screen in order to be rendered, {@code false} otherwise
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
     * Marks the initial line breaks so they are not mistaken as {@link #SPACER}.
     */
    protected void markInitialLineBreaks()
    {
        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> brs = document.getBody().getElementsByTagName(BR);
        for (int i = 0; i < brs.getLength(); i++) {
            Element br = (Element) brs.getItem(i);
            // Skip the BRs added by the browser before the document was loaded.
            if (!br.hasAttribute("_moz_dirty")) {
                br.setClassName(LINE_BREAK);
            }
        }
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
     * Overwrites the default rich text area behavior when the Enter key is being pressed.
     * 
     * @param modifiers the modifier keys pressed when the event occurred
     */
    protected void onEnter(int modifiers)
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (!selection.isCollapsed()) {
            // Selection + Enter = Selection + Delete + Enter
            // NOTE: We cannot use Range#deleteContents because it may lead to DTD-invalid HTML. That's because it
            // operates on any DOM tree without taking care of the underlying XML syntax, (X)HTML in our case. Let's use
            // the Delete command instead which is HTML-aware.
            getTextArea().getDocument().execCommand(Command.DELETE.toString(), null);
        }

        // At this point the selection should be collapsed.
        Range caret = selection.getRangeAt(0);

        Node container = null;
        // CTRL and META modifiers force the Enter key to be handled by the nearest block-level container. Otherwise we
        // look for special containers like the list item.
        if (modifiers != KeyboardListener.MODIFIER_CTRL && modifiers != KeyboardListener.MODIFIER_META) {
            // See if the caret is inside a list item.
            container = domUtils.getFirstAncestor(caret.getStartContainer(), LI);
        }
        if (container == null) {
            // Look for the nearest block-level element that contains the caret.
            container = domUtils.getNearestBlockContainer((caret.getStartContainer()));
        }

        String containerName = container.getNodeName().toLowerCase();
        if (LI.equals(containerName)) {
            // Leave the default behavior for now.
            return;
        } else if (TD.equals(containerName) || TH.equals(containerName)) {
            insertLineBreak(container, caret);
        } else {
            onEnterParagraph(container, caret, modifiers);
        }

        // Cancel the event to prevent its default behavior.
        getTextArea().getCurrentEvent().xPreventDefault();

        // Update the caret.
        caret.collapse(true);
        selection.removeAllRanges();
        selection.addRange(caret);
    }

    /**
     * Behaves as if the caret is inside a paragraph. Precisely:
     * <ul>
     * <li>SHIFT+Enter generates a line break</li>
     * <li>Enter at the beginning of the line inserts an empty line before</li>
     * <li>Enter anywhere else splits the current block and generates a new paragraph.</li>
     * </ul>
     * 
     * @param container a block-level element containing the caret
     * @param caret the position of the caret inside the document
     * @param modifiers the modifier keys pressed when the event occurred
     */
    protected void onEnterParagraph(Node container, Range caret, int modifiers)
    {
        if (modifiers == KeyboardListener.MODIFIER_SHIFT) {
            insertLineBreak(container, caret);
        } else if (isAtStart(container, caret)) {
            insertEmptyLine(container, caret);
        } else {
            if (!isAfterLineBreak(container, caret)) {
                insertLineBreak(container, caret);
            }
            splitLine(container, caret);
        }
    }

    /**
     * @param container a block level element containing the caret
     * @param caret the position of the caret inside the document
     * @return {@code true} if the caret is at the beginning of its block level container, {@code false} otherwise
     */
    protected boolean isAtStart(Node container, Range caret)
    {
        if (!container.hasChildNodes()) {
            return true;
        }
        if (caret.getStartOffset() > 0) {
            return false;
        }
        return domUtils.getFirstLeaf(container) == domUtils.getFirstLeaf(caret.getStartContainer());
    }

    /**
     * @param container a block level element containing the caret
     * @param caret the position of the caret in the document
     * @return {@code true} if the caret is immediately after a line break inside the given container, {@code false}
     *         otherwise
     */
    protected boolean isAfterLineBreak(Node container, Range caret)
    {
        Node leaf;
        if (caret.getStartOffset() > 0) {
            if (caret.getStartContainer().getNodeType() == Node.ELEMENT_NODE) {
                leaf = caret.getStartContainer().getChildNodes().getItem(caret.getStartOffset() - 1);
                leaf = domUtils.getLastLeaf(leaf);
            } else {
                // We are in the middle of the text.
                return false;
            }
        } else {
            leaf = domUtils.getPreviousLeaf(caret.getStartContainer());
        }
        // We have to additionally test if the found line break is in the given container.
        return isLineBreak(leaf) && container == domUtils.getNearestBlockContainer(leaf);
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node marks a line break, {@code false} otherwise
     */
    protected boolean isLineBreak(Node node)
    {
        return node != null && BR.equalsIgnoreCase(node.getNodeName());
    }

    /**
     * Inserts a line break at the specified position in the document.
     * 
     * @param container a block-level element containing the caret
     * @param caret the place where to insert the line break
     */
    protected void insertLineBreak(Node container, Range caret)
    {
        // Insert the line break.
        Node lineBreak = getTextArea().getDocument().xCreateBRElement();
        switch (caret.getStartContainer().getNodeType()) {
            case DOMUtils.CDATA_NODE:
            case DOMUtils.COMMENT_NODE:
                domUtils.insertAfter(lineBreak, caret.getStartContainer());
                break;
            case Node.TEXT_NODE:
                Node refNode = domUtils.splitNode(caret.getStartContainer(), caret.getStartOffset());
                refNode.getParentNode().insertBefore(lineBreak, refNode);
                break;
            case Node.ELEMENT_NODE:
                domUtils.insertAt(caret.getStartContainer(), lineBreak, caret.getStartOffset());
                break;
            default:
                break;
        }

        // Place the caret after the inserted line break.
        Node start = lineBreak.getNextSibling();
        if (start == null || start.getNodeType() != Node.TEXT_NODE) {
            start = getTextArea().getDocument().createTextNode("");
            domUtils.insertAfter(start, lineBreak);
        }
        caret.setStart(start, 0);
    }

    /**
     * Splits a line after a line break.
     * 
     * @param container a block-level element containing the caret
     * @param caret the position of the caret in the document
     */
    protected void splitLine(Node container, Range caret)
    {
        Node br;
        // Find the BR.
        if (caret.getStartOffset() > 0) {
            if (caret.getStartContainer().getNodeType() == Node.ELEMENT_NODE) {
                br = caret.getStartContainer().getChildNodes().getItem(caret.getStartOffset() - 1);
                br = domUtils.getLastLeaf(br);
            } else {
                return;
            }
        } else {
            br = domUtils.getPreviousLeaf(caret.getStartContainer());
        }

        // Create a new paragraph.
        Node paragraph = getTextArea().getDocument().xCreatePElement();

        // This is the node that will contain the caret after the split.
        Node start;

        // Split the container after the found BR.
        if (domUtils.isFlowContainer(container)) {
            Node child = domUtils.getChild(container, br);
            if (child != br) {
                start = domUtils.splitNode(container, br.getParentNode(), domUtils.getNodeIndex(br));
            } else {
                start = paragraph;
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
            start = domUtils.splitNode(container.getParentNode(), br.getParentNode(), domUtils.getNodeIndex(br));
            if (start == container.getNextSibling()) {
                start = paragraph;
            }
            paragraph.appendChild(Element.as(container.getNextSibling()).extractContents());
            container.getParentNode().replaceChild(paragraph, container.getNextSibling());
        }
        br.getParentNode().removeChild(br);

        // Place the caret inside the new container, at the beginning.
        if (!start.hasChildNodes()) {
            start.appendChild(getTextArea().getDocument().createTextNode(""));
        }
        if (start.getFirstChild().getNodeType() == Node.TEXT_NODE) {
            start = start.getFirstChild();
        }
        caret.setStart(start, 0);
    }

    /**
     * Inserts an empty line before the block containing the caret. This is useful when the caret is at the beginning of
     * a block and we want to move that block down by one line; by pressing Enter we can do that.
     * 
     * @param container a block-level element containing the caret
     * @param caret the place where to insert the empty line
     */
    protected void insertEmptyLine(Node container, Range caret)
    {
        Document document = getTextArea().getDocument();
        // Create a new empty line.
        Element emptyLine = document.xCreatePElement().cast();

        if (domUtils.isFlowContainer(container)) {
            // We are at the beginning of a flow container. Since it can contain block elements we insert the empty line
            // before its first child.
            domUtils.insertAt(container, emptyLine, 0);
            // We place the caret after the inserted empty line.
            caret.setStartAfter(emptyLine);
        } else {
            // Insert the empty line before the container.
            container.getParentNode().insertBefore(emptyLine, container);
        }
    }

    /**
     * Overwrites the default rich text area behavior when the Backspace key is being pressed.
     */
    protected void onBackspace()
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (!selection.isCollapsed()) {
            return;
        }
        Range caret = selection.getRangeAt(0);

        // Look for the nearest block-level element that contains the caret.
        Node container = domUtils.getNearestBlockContainer((caret.getStartContainer()));
        // See if the found container is preceded by an empty line.
        if (domUtils.isBlockLevelInlineContainer(container) && isAtStart(container, caret)
            && isEmptyLine(container.getPreviousSibling())) {
            // Cancel the event to prevent its default behavior.
            getTextArea().getCurrentEvent().xPreventDefault();
            // Remove the empty line.
            container.getParentNode().removeChild(container.getPreviousSibling());
        }
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node represents an empty line
     */
    protected boolean isEmptyLine(Node node)
    {
        // Test of the given node is a paragraph.
        if (node == null || !"p".equalsIgnoreCase(node.getNodeName())) {
            return false;
        }
        // Test if the paragraph has visible child nodes.
        Node child = node.getFirstChild();
        while (child != null) {
            if ((child.getNodeType() == Node.ELEMENT_NODE && ((Element) child).getOffsetWidth() > 0)
                || !StringUtils.isEmpty(child.getNodeValue())) {
                return false;
            }
            child = child.getNextSibling();
        }
        return true;
    }

    /**
     * Called after the content of the rich text area has been reset.
     */
    protected void onReset()
    {
        markInitialLineBreaks();
        replaceEmptyDivsWithParagraphs();
    }
}
