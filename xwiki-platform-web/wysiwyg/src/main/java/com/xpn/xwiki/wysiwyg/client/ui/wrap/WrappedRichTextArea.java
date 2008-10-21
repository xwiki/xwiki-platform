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
package com.xpn.xwiki.wysiwyg.client.ui.wrap;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.Selection;
import com.xpn.xwiki.wysiwyg.client.ui.XShortcutKey;
import com.xpn.xwiki.wysiwyg.client.ui.XShortcutKeyFactory;
import com.xpn.xwiki.wysiwyg.client.util.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.util.Document;

public class WrappedRichTextArea extends RichTextArea
{
    private class LoadCommand implements Command
    {
        public void execute()
        {
            Document doc = (Document) IFrameElement.as(getElement()).getContentDocument().cast();
            Node head = doc.getElementsByTagName("head").getItem(0);

            // Add style sheet declarations
            LinkElement linkPrototype = doc.xCreateLinkElement();
            linkPrototype.setRel("stylesheet");
            linkPrototype.setType("text/css");
            for (String styleSheetURL : iframeConfig.getStyleSheetURLs()) {
                LinkElement link = (LinkElement) linkPrototype.cloneNode(false);
                link.setHref(styleSheetURL);
                head.appendChild(link);
            }

            // Add script declarations
            ScriptElement scriptPrototype = doc.xCreateScriptElement();
            scriptPrototype.setType("text/javascript");
            for (String scriptURL : iframeConfig.getScriptURLs()) {
                ScriptElement script = (ScriptElement) scriptPrototype.cloneNode(false);
                script.setSrc(scriptURL);
                head.appendChild(script);
            }

            // Set the class and id attributes on body
            doc.getBody().setId(iframeConfig.getBodyId());
            doc.getBody().setClassName(iframeConfig.getBodyClassName());
        }
    }

    private final List<XShortcutKey> shortcutKeys = new ArrayList<XShortcutKey>();

    private final IFrameConfig iframeConfig = new IFrameConfig();

    public void addShortcutKey(XShortcutKey shortcutKey)
    {
        if (!shortcutKeys.contains(shortcutKey)) {
            shortcutKeys.add(shortcutKey);
        }
    }

    public void removeShortcutKey(XShortcutKey shortcutKey)
    {
        shortcutKeys.remove(shortcutKey);
    }

    /**
     * @return The DOM document being edited with this rich text area.
     */
    public Document getDocument()
    {
        return IFrameElement.as(getElement()).getContentDocument().cast();
    }

    /**
     * @return The configuration object associated with the in-line frame of this rich text area.
     */
    public IFrameConfig getIFrameConfig()
    {
        return iframeConfig;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Widget#onLoad()
     */
    protected void onLoad()
    {
        DeferredCommand.addCommand(new LoadCommand());
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#onBrowserEvent(Event)
     */
    public void onBrowserEvent(Event event)
    {
        if (event.getTypeInt() == Event.ONKEYDOWN) {
            if (shortcutKeys.contains(XShortcutKeyFactory.createShortcutKey(event))) {
                event.preventDefault();
            } else if (event.getKeyCode() == KeyboardListener.KEY_ENTER && !event.getShiftKey()) {
                onEnter(event);
            }
        }
        super.onBrowserEvent(event);
    }

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
