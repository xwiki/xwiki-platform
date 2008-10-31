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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKey;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKeyFactory;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.DefaultCommandManager;
import com.xpn.xwiki.wysiwyg.client.widget.rta.history.History;
import com.xpn.xwiki.wysiwyg.client.widget.rta.history.internal.DefaultHistory;

/**
 * Extends the rich text area provided by GWT to add support for advanced editing.
 * 
 * @version $Id$
 */
public class RichTextArea extends com.google.gwt.user.client.ui.RichTextArea implements SourcesChangeEvents, HasName
{
    /**
     * The command manager that executes commands on this rich text area.
     */
    private final CommandManager cm;

    /**
     * The history of this rich text area.
     */
    private final History history;

    /**
     * The list of listeners that are notified when the content of the rich text area changes. Change events are
     * triggered only when the content of the rich text area is changed using {@link #setHTML(String)} or
     * {@link #setText(String)}.
     */
    private final ChangeListenerCollection changeListeners = new ChangeListenerCollection();

    /**
     * The list of shortcut keys activated on this rich text area.
     */
    private final List<ShortcutKey> shortcutKeys = new ArrayList<ShortcutKey>();

    /**
     * The document template that is applied on the edited document whenever the rich text area is loaded.
     */
    private final DocumentTemplate template = new DocumentTemplate();

    /**
     * Applies the {@link #template} on the {@link #getDocument()}.
     */
    private final DocumentTemplateEnforcer templateEnforcer = new DocumentTemplateEnforcer(this);

    /**
     * The name of this rich text area. It could be used to submit the edited contents to the server.
     */
    private String name;

    /**
     * Creates a new rich text area.
     */
    public RichTextArea()
    {
        cm = new DefaultCommandManager(this);
        history = new DefaultHistory(this, 10);
    }

    /**
     * Custom constructor allowing us to inject a mock command manager. It was mainly added to be used in unit tests.
     * 
     * @param cm Custom command manager
     */
    public RichTextArea(CommandManager cm)
    {
        this.cm = cm;
        history = new DefaultHistory(this, 10);
    }

    /**
     * Activates the given shortcut key on this text area. This way the default behavior of the browser is prevented,
     * and the caller of this method can associate its own behavior with the specified shortcut key.
     * 
     * @param shortcutKey the shortcut key to activate.
     */
    public void addShortcutKey(ShortcutKey shortcutKey)
    {
        if (!shortcutKeys.contains(shortcutKey)) {
            shortcutKeys.add(shortcutKey);
        }
    }

    /**
     * Deactivates the specified shortcut key for this rich text area.
     * 
     * @param shortcutKey The shortcut key to be deactivated.
     */
    public void removeShortcutKey(ShortcutKey shortcutKey)
    {
        shortcutKeys.remove(shortcutKey);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesChangeEvents#addChangeListener(ChangeListener)
     */
    public void addChangeListener(ChangeListener listener)
    {
        changeListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesChangeEvents#removeChangeListener(ChangeListener)
     */
    public void removeChangeListener(ChangeListener listener)
    {
        changeListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasName#setName(String)
     */
    public void setName(String name)
    {
        if (!name.equals(this.name)) {
            this.name = name;
            changeListeners.fireChange(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasName#getName()
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return The DOM document being edited with this rich text area.
     */
    public Document getDocument()
    {
        return IFrameElement.as(getElement()).getContentDocument().cast();
    }

    /**
     * @return The document template that is applied on the edited document when the rich text area is loading.
     */
    public DocumentTemplate getDocumentTemplate()
    {
        return template;
    }

    /**
     * @return the {@link CommandManager} associated with this instance.
     */
    public CommandManager getCommandManager()
    {
        return cm;
    }

    /**
     * @return The history of this rich text area.
     */
    public History getHistory()
    {
        return history;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#onLoad()
     */
    protected void onLoad()
    {
        DeferredCommand.addCommand(templateEnforcer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#setHTML(String)
     */
    public void setHTML(String html)
    {
        super.setHTML(html);
        changeListeners.fireChange(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#setText(String)
     */
    public void setText(String text)
    {
        super.setText(text);
        changeListeners.fireChange(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#onBrowserEvent(Event)
     */
    public void onBrowserEvent(Event event)
    {
        if (event.getTypeInt() == Event.ONKEYDOWN) {
            if (shortcutKeys.contains(ShortcutKeyFactory.createShortcutKey(event))) {
                event.preventDefault();
            } else if (event.getKeyCode() == KeyboardListener.KEY_ENTER && !event.getShiftKey()) {
                onEnter(event);
            }
        }
        super.onBrowserEvent(event);
    }

    /**
     * Handles the return key.<br/>
     * TODO: Move this code inside a command!!!
     * 
     * @param event keyboard event.
     */
    public void onEnter(Event event)
    {
        Selection selection = getDocument().getSelection();
        if (selection.getRangeCount() == 0) {
            // We might not have an implementation of Selection for the current browser.
            // This should be removed in the future.
            return;
        }
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
                    event.preventDefault();

                    // Delete the text from the first range and leave the text of the other ranges untouched.
                    range.deleteContents();

                    // Create the line break and insert it before the current range.
                    Element br = getDocument().xCreateBRElement();
                    Node refNode = range.getStartContainer();
                    if (refNode.hasChildNodes()) {
                        refNode = refNode.getChildNodes().getItem(range.getStartOffset());
                    }
                    refNode.getParentNode().insertBefore(br, refNode);

                    // Update the current range
                    selection.removeAllRanges();
                    range = getDocument().createRange();
                    range.setStartBefore(refNode);
                    range.setEndBefore(refNode);
                    selection.addRange(range);
                } else {
                    // The selection starts inside a non-empty paragraph so we leave the default behavior.
                }
            }
        } else {
            // We are not inside a paragraph so we change the default behavior.
            event.preventDefault();

            // Save the start container and offset to know later where to split the text.
            Node startContainer = range.getStartContainer();
            int startOffset = range.getStartOffset();

            // Delete the text from the first range and leave the text of the other ranges untouched.
            range.deleteContents();
            // Reset the selection. We're going to move the cursor inside the new paragraph.
            selection.removeAllRanges();

            // Split the DOM subtree that has the previously found ancestor as root.
            Node splitRightNode;
            if (startContainer.getNodeType() == Node.TEXT_NODE) {
                Text right = Text.as(startContainer);

                if (startOffset > 0) {
                    // Split the text node
                    String leftData = right.getData().substring(0, startOffset);
                    String rightData = right.getData().substring(startOffset);
                    right.setData(rightData);
                    Text left = getDocument().createTextNode(leftData);
                    right.getParentNode().insertBefore(left, right);
                }

                // Split-up the rest of the subtree, till we reach the previously found ancestor.
                splitRightNode = split(right, ancestor);
            } else {
                splitRightNode = split(startContainer.getChildNodes().getItem(startOffset), ancestor);
            }

            // Wrap left in-line siblings in a paragraph.
            Element leftParagraph = getDocument().xCreatePElement();
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
            Element rightParagraph = getDocument().xCreatePElement();
            ancestor.replaceChild(rightParagraph, splitRightNode);
            rightParagraph.appendChild(splitRightNode);
            Node rightSibling = rightParagraph.getNextSibling();
            while (rightSibling != null && DOMUtils.getInstance().isInline(rightSibling)) {
                rightParagraph.appendChild(rightSibling);
                rightSibling = rightParagraph.getNextSibling();
            }

            // Create the new range and move the cursor inside the new paragraph.
            range = getDocument().createRange();
            range.selectNodeContents(DOMUtils.getInstance().getFirstLeaf(rightParagraph));
            range.collapse(true);
            selection.addRange(range);
        }
    }

    /**
     * Splits a DOM subtree.<br/>
     * TODO: Move this code inside a command!!!
     * 
     * @param rightChild
     * @param ancestor
     * @return
     */
    private Node split(Node rightChild, Node ancestor)
    {
        // Split-up the rest of the subtree, till we reach the given ancestor.
        while (rightChild.getParentNode() != ancestor) {
            Node rightSubtree = rightChild.getParentNode();
            // If we have left siblings then we split
            if (rightChild.getPreviousSibling() != null) {
                Node leftSubtree = rightSubtree.cloneNode(false);
                rightSubtree.getParentNode().insertBefore(leftSubtree, rightSubtree);
                // Move left siblings in left subtree
                while (rightSubtree.getFirstChild() != rightChild) {
                    leftSubtree.appendChild(rightSubtree.getFirstChild());
                }
            }
            rightChild = rightSubtree;
        }
        return rightChild;
    }
}
