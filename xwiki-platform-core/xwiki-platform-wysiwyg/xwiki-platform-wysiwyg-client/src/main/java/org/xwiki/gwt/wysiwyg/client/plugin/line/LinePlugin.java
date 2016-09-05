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
package org.xwiki.gwt.wysiwyg.client.plugin.line;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.KeyboardAdaptor;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Overwrites the behavior of creating new lines of text and merging existing ones.
 * 
 * @version $Id$
 */
public class LinePlugin extends AbstractPlugin implements CommandListener
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
     * A regular expression that matches a string full of whitespace.
     */
    private static final RegExp WHITESPACE = RegExp.compile("^\\s+$");

    /**
     * Collection of DOM utility methods.
     */
    protected final DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * The object used to handle keyboard events.
     */
    private final KeyboardAdaptor keyboardAdaptor = new KeyboardAdaptor()
    {
        protected void handleRepeatableKey(Event event)
        {
            LinePlugin.this.handleRepeatableKey(event);
        }
    };

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        saveRegistration(getTextArea().addKeyDownHandler(keyboardAdaptor));
        saveRegistration(getTextArea().addKeyUpHandler(keyboardAdaptor));
        saveRegistration(getTextArea().addKeyPressHandler(keyboardAdaptor));
        getTextArea().getCommandManager().addCommandListener(this);

        // Adjust the initial content of the rich text area.
        onReset();
    }

    @Override
    public void destroy()
    {
        getTextArea().getCommandManager().removeCommandListener(this);

        super.destroy();
    }

    /**
     * Handles a repeatable key press.
     * 
     * @param event the native event that was fired
     */
    protected void handleRepeatableKey(Event event)
    {
        switch (event.getKeyCode()) {
            case KeyCodes.KEY_ENTER:
                onEnter(event);
                break;
            case KeyCodes.KEY_BACKSPACE:
                onBackspace(event);
                break;
            default:
                break;
        }
    }

    @Override
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

    @Override
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
        if (leaf == null) {
            return false;
        }
        switch (leaf.getNodeType()) {
            case Node.TEXT_NODE:
                if (WHITESPACE.test(leaf.getNodeValue())) {
                    // We have to check if the whitespace is rendered in the current context. Let's wrap the text node
                    // with a SPAN element and see if it has any width.
                    Element wrapper = Element.as(leaf.getOwnerDocument().createSpanElement());
                    leaf.getParentNode().replaceChild(wrapper, leaf);
                    wrapper.appendChild(leaf);
                    // Note: We test only the width because an empty SPAN element normally has the height of the line.
                    boolean needsSpace = wrapper.getOffsetWidth() > 0;
                    // Unwrap the whitespace text node.
                    wrapper.getParentNode().replaceChild(leaf, wrapper);
                    return needsSpace;
                } else {
                    return leaf.getNodeValue().length() > 0;
                }
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
            // Skip the spaces and the BRs added by the browser before the document was loaded.
            if (!br.hasAttribute("_moz_dirty") && !SPACER.equals(br.getClassName())) {
                br.setClassName(LINE_BREAK);
            }
        }
    }

    /**
     * Replaces {@code <div class="wikimodel-emptyline"/>} with {@code <p/>}. Empty lines are used by WikiModel to
     * separate block level elements, but since the user should be able to write on these empty lines we convert them to
     * paragraphs.
     */
    protected void replaceEmptyLinesWithParagraphs()
    {
        Document document = getTextArea().getDocument();
        NodeList<com.google.gwt.dom.client.Element> divs = document.getBody().getElementsByTagName("div");
        // Since NodeList is updated when one of its nodes are detached, we store the empty lines in a separate list.
        List<Node> emptyLines = new ArrayList<Node>();
        for (int i = 0; i < divs.getLength(); i++) {
            Element div = divs.getItem(i).cast();
            if (div.hasClassName("wikimodel-emptyline")) {
                emptyLines.add(div);
            }
        }
        // Replace the empty lines with paragraphs.
        for (Node emptyLine : emptyLines) {
            emptyLine.getParentNode().replaceChild(document.createPElement(), emptyLine);
        }
    }

    /**
     * Overwrites the default rich text area behavior when the Enter key is being pressed.
     * 
     * @param event the native event that was fired
     */
    protected void onEnter(Event event)
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (!selection.isCollapsed()) {
            // Selection + Enter = Selection + Delete + Enter
            // NOTE: We cannot use Range#deleteContents because it may lead to DTD-invalid HTML. That's because it
            // operates on any DOM tree without taking care of the underlying XML syntax, (X)HTML in our case. Let's use
            // the Delete command instead which is HTML-aware.
            // NOTE: The Delete command can have side-effects like the insertion of a bogus BR tag. Be aware!
            getTextArea().getDocument().execCommand(Command.DELETE.toString(), null);
        }

        // At this point the selection should be collapsed.
        Range caret = selection.getRangeAt(0);

        Node container = null;
        // CTRL and META modifiers force the Enter key to be handled by the nearest block-level container. Otherwise we
        // look for special containers like the list item.
        if (!event.getCtrlKey() && !event.getMetaKey()) {
            // See if the caret is inside a list item.
            container = domUtils.getFirstAncestor(caret.getStartContainer(), LI);
        }
        if (container == null) {
            // Look for the nearest block-level element that contains the caret.
            container = domUtils.getNearestBlockContainer(caret.getStartContainer());
        }

        String containerName = container.getNodeName().toLowerCase();
        if (LI.equals(containerName)) {
            // Leave the default behavior for now.
            return;
        } else if (TD.equals(containerName) || TH.equals(containerName)) {
            insertLineBreak(container, caret);
        } else {
            onEnterParagraph(container, caret, event);
        }

        // Cancel the event to prevent its default behavior.
        event.xPreventDefault();

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
     * @param event the native event that was fired
     */
    protected void onEnterParagraph(Node container, Range caret, Event event)
    {
        if (event.getShiftKey()) {
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
     * Adjusts the line break position so that:
     * <ul>
     * <li>Anchors don't start or end with a line break.</li>
     * </ul>
     * See XWIKI-4193: When hitting Return at the end of the link the new line should not be a link.
     * 
     * @param container the block-level element containing the line break
     * @param br the line break
     */
    protected void adjustLineBreak(Node container, Node br)
    {
        Node anchor = domUtils.getFirstAncestor(br, "a");
        if (anchor != null) {
            // NOTE: We assume the anchor is inside the container because the anchor is an in-line element while the
            // container is a block-level element. We could test if the container is or has child the anchor but let's
            // keep things simple for now.
            // Check if the anchor starts with the given line break.
            Node firstLeaf = domUtils.getFirstLeaf(anchor);
            Node leaf = br;
            boolean startsWithLineBreak = true;
            while (leaf != firstLeaf) {
                leaf = domUtils.getPreviousLeaf(leaf);
                if (needsSpace(leaf)) {
                    startsWithLineBreak = false;
                    break;
                }
            }
            if (startsWithLineBreak) {
                // Move the line break before the anchor.
                anchor.getParentNode().insertBefore(br, anchor);
            } else {
                // Check if the anchor ends with the given line break.
                Node lastLeaf = domUtils.getLastLeaf(anchor);
                leaf = br;
                boolean endsWithLineBreak = true;
                while (leaf != lastLeaf) {
                    leaf = domUtils.getNextLeaf(leaf);
                    if (needsSpace(leaf)) {
                        endsWithLineBreak = false;
                        break;
                    }
                }
                if (endsWithLineBreak) {
                    // Move the line break after the anchor.
                    domUtils.insertAfter(br, anchor);
                }
            }
        }
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
        Node lineBreak = getTextArea().getDocument().createBRElement();
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
        caret.setStartAfter(lineBreak);

        // In case the line break was inserted at the end of a line..
        ensureLineBreakIsVisible(lineBreak, container);
    }

    /**
     * Ensures that the line created by inserting a line break is visible. This is need especially when the line break
     * is inserted at the end of an existing line because most browsers don't allow the caret to be placed on invisible
     * lines.
     * 
     * @param lineBreak the line break that was inserted
     * @param container the container (e.g. the paragraph) where the line break was inserted
     */
    protected void ensureLineBreakIsVisible(Node lineBreak, Node container)
    {
        Node lastLeaf = null;
        Node leaf = lineBreak;
        // Look if there is any visible element on the new line, taking care to remain in the current block container.
        while (leaf != null && container == domUtils.getNearestBlockContainer(leaf)) {
            lastLeaf = leaf;
            leaf = domUtils.getNextLeaf(leaf);
            if (needsSpace(leaf)) {
                return;
            }
        }

        if (lastLeaf != null) {
            // It seems there's no visible element on the new line. We should add a spacer up in the tree.
            Node ancestor = lastLeaf;
            while (ancestor.getParentNode() != container && ancestor.getNextSibling() == null) {
                ancestor = ancestor.getParentNode();
            }
            domUtils.insertAfter(getTextArea().getDocument().createBRElement(), ancestor);
        }
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
        adjustLineBreak(container, br);

        // Create a new paragraph.
        Node paragraph = getTextArea().getDocument().createPElement();

        // This is the node that will contain the caret after the split.
        Node start;

        // Split the container after the found BR.
        if (domUtils.isFlowContainer(container)) {
            start = splitContentAndWrap(container, br, paragraph);
        } else {
            start = splitAndReplace(container, br, paragraph);
            copyLineStyle(Element.as(container), Element.as(paragraph));
        }
        br.getParentNode().removeChild(br);

        // Make sure that both lines generated by the split can be edited.
        // Note: the first call is required in case we split a line that starts with invisible garbage and the caret is
        // just after this garbage. Another solution would be to enhance the isAtStart method to detect such cases but
        // this is more complex.
        Element.as(container).ensureEditable();
        Element.as(paragraph).ensureEditable();

        // Place the caret inside the new container, at the beginning.
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
        Element emptyLine = document.createPElement().cast();

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

        // Ensure the newly created empty line can be edited (i.e. the user can place the caret inside it).
        // Note: in order to have the desired effect in IE we need to call this method after the empty line is attached.
        domUtils.ensureBlockIsEditable(emptyLine);
    }

    /**
     * Overwrites the default rich text area behavior when the Backspace key is being pressed.
     * 
     * @param event the native event that was fired
     */
    protected void onBackspace(Event event)
    {
        Selection selection = getTextArea().getDocument().getSelection();
        if (!selection.isCollapsed()) {
            return;
        }
        Range caret = selection.getRangeAt(0);

        // Look for the nearest block-level element that contains the caret.
        Node container = domUtils.getNearestBlockContainer(caret.getStartContainer());
        // See if the found container is preceded by an empty line.
        if (domUtils.isBlockLevelInlineContainer(container) && isAtStart(container, caret)
            && isEmptyLine(container.getPreviousSibling())) {
            // Cancel the event to prevent its default behavior.
            event.xPreventDefault();
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
        replaceEmptyLinesWithParagraphs();
        Element.as(getTextArea().getDocument().getBody()).ensureEditable();
    }

    /**
     * Splits the given container in two parts, before the specified child and after, and replaces the container of the
     * second part (to the right of the child node) with the given replacement.
     * 
     * @param container the root of the subtree to be split
     * @param child the descendant that marks the split point
     * @param replacement the new container for the second part obtained from the split
     * @return the node resulted from splitting the child, or the replacement if the container is split at the end
     */
    private Node splitAndReplace(Node container, Node child, Node replacement)
    {
        Node start = domUtils.splitNode(container.getParentNode(), child.getParentNode(), domUtils.getNodeIndex(child));
        if (start == container.getNextSibling()) {
            start = replacement;
        }
        replacement.appendChild(Element.as(container.getNextSibling()).extractContents());
        container.getParentNode().replaceChild(replacement, container.getNextSibling());
        return start;
    }

    /**
     * Splits the content of the given container in two parts, before the specified descendant and after, and wraps the
     * in-line nodes to the right of the split with the given wrapper.
     * <p>
     * NOTE: The container itself is not split, only its content is.
     * 
     * @param container the root of the subtree to be split; the root itself is not split
     * @param descendant the descendant that marks the split point
     * @param wrapper the new container for the in-line nodes that are positioned to the right of the split point
     * @return the node resulted from splitting the descendant, or the wrapper if the descendant is a direct child of
     *         the container (in which case the split doesn't take place)
     */
    private Node splitContentAndWrap(Node container, Node descendant, Node wrapper)
    {
        Node start = wrapper;
        Node child = domUtils.getChild(container, descendant);
        // If the descendant is a direct child of the container then we don't have to split.
        if (child != descendant) {
            start = domUtils.splitNode(container, descendant.getParentNode(), domUtils.getNodeIndex(descendant));
        }
        // Insert the wrapper after the split.
        domUtils.insertAfter(wrapper, child);
        // Move all the following in-line nodes inside the wrapper.
        child = wrapper.getNextSibling();
        while (child != null && domUtils.isInline(child)) {
            wrapper.appendChild(child);
            child = wrapper.getNextSibling();
        }
        return start;
    }

    /**
     * Copy some of the CSS styles from the source line to the destination line. Call this method to ensure the
     * important line styles are preserved on the new line after splitting a line.
     * 
     * @param sourceLine the line from where to copy the styles
     * @param destinationLine the line whose style will be changed
     * @see #splitLine(Node, Range)
     */
    protected void copyLineStyle(Element sourceLine, Element destinationLine)
    {
        destinationLine.getStyle().setProperty(Style.TEXT_ALIGN.getJSName(),
            sourceLine.getStyle().getProperty(Style.TEXT_ALIGN.getJSName()));
    }
}
