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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.dom.Text;
import com.xpn.xwiki.wysiwyg.client.dom.TextFragment;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Applies in-line formatting to the current selection.
 * 
 * @version $Id$
 */
public class StyleExecutable extends AbstractExecutable
{
    /**
     * The name of the tag used for formatting.
     */
    private final String tagName;

    /**
     * The style name added to the formatting element.
     */
    private final String className;

    /**
     * CSS property that describes the in-line style applied.
     */
    private final String propertyName;

    /**
     * The value to be applied to {@link #propertyName}.
     */
    private final String propertyValue;

    /**
     * Whether to check if {@link #propertyName} has {@link #propertyValue} only on the current element or all his
     * ancestors.
     */
    private final boolean inheritable;

    /**
     * Flag that specifies if {@link #propertyName} can have multiple values.
     */
    private final boolean multipleValue;

    /**
     * Collection of DOM utility methods.
     */
    private DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * Creates a new instance.
     * 
     * @param tagName {@link #tagName}
     * @param className {@link #className}
     * @param propertyName {@link #propertyName}
     * @param propertyValue {@link #propertyValue}
     * @param inheritable {@link #inheritable}
     * @param multipleValue {@link #multipleValue}
     */
    public StyleExecutable(String tagName, String className, String propertyName, String propertyValue,
        boolean inheritable, boolean multipleValue)
    {
        this.tagName = tagName;
        this.className = className;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.inheritable = inheritable;
        this.multipleValue = multipleValue;
    }

    /**
     * @return {@link #tagName}
     */
    public String getTagName()
    {
        return tagName;
    }

    /**
     * @return {@link #className}
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * @return {@link #propertyName}
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * @return {@link #propertyValue}
     */
    public String getPropertyValue()
    {
        return propertyValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String parameter)
    {
        Selection selection = rta.getDocument().getSelection();
        List<Range> ranges = new ArrayList<Range>();
        boolean executed = isExecuted(rta);
        for (int i = 0; i < selection.getRangeCount(); i++) {
            ranges.add(execute(selection.getRangeAt(i), executed));
        }
        selection.removeAllRanges();
        for (Range range : ranges) {
            selection.addRange(range);
        }
        return true;
    }

    /**
     * Adds to or removes from the given range the in-line formatting.
     * 
     * @param range the target range
     * @param executed whether to add or remove the style
     * @return the given range after being processed
     */
    protected Range execute(Range range, boolean executed)
    {
        if (range.isCollapsed()) {
            switch (range.getStartContainer().getNodeType()) {
                case Node.TEXT_NODE:
                    Text text = (Text) range.getStartContainer();
                    text = execute(text, range.getStartOffset(), range.getEndOffset(), executed).getText();
                    range.selectNodeContents(text);
                    break;
                case Node.ELEMENT_NODE:
                    Text empty = (Text) range.getStartContainer().getOwnerDocument().createTextNode("");
                    domUtils.insertAt(range.getStartContainer(), empty, range.getStartOffset());
                    range.selectNodeContents(execute(empty, 0, 0, executed).getText());
                    break;
                default:
                    // Do nothing.
                    break;
            }
        } else {
            // Iterate through all the text nodes within the given range and apply the underlying style.
            TextFragment startContainer = null;
            TextFragment endContainer = null;
            List<Text> textNodes = getNonEmptyTextNodes(range);
            for (int i = 0; i < textNodes.size(); i++) {
                Text text = textNodes.get(i);
                int startIndex = 0;
                if (text == range.getStartContainer()) {
                    startIndex = range.getStartOffset();
                }
                int endIndex = text.getLength();
                if (text == range.getEndContainer()) {
                    endIndex = range.getEndOffset();
                }
                endContainer = execute(text, startIndex, endIndex, executed);
                if (startContainer == null) {
                    startContainer = endContainer;
                }
            }
            if (startContainer != null) {
                range.setEnd(endContainer.getText(), endContainer.getEndIndex());
                range.setStart(startContainer.getText(), startContainer.getStartIndex());
            }
        }
        return range;
    }

    /**
     * @param range a DOM range
     * @return the list of non empty text nodes that are completely or partially (at least one character) included in
     *         the given range
     */
    private List<Text> getNonEmptyTextNodes(Range range)
    {
        Node leaf = domUtils.getFirstLeaf(range);
        Node lastLeaf = domUtils.getLastLeaf(range);
        List<Text> textNodes = new ArrayList<Text>();
        // If the range starts at the end of a text node we have to ignore that node.
        if (isNonEmptyTextNode(leaf)
            && (leaf != range.getStartContainer() || range.getStartOffset() < leaf.getNodeValue().length())) {
            textNodes.add((Text) leaf);
        }
        while (leaf != lastLeaf) {
            leaf = domUtils.getNextLeaf(leaf);
            if (isNonEmptyTextNode(leaf)) {
                textNodes.add((Text) leaf);
            }
        }
        // If the range ends at the start of a text node then we have to ignore that node.
        int lastIndex = textNodes.size() - 1;
        if (lastIndex >= 0 && range.getEndOffset() == 0 && textNodes.get(lastIndex) == range.getEndContainer()) {
            textNodes.remove(lastIndex);
        }
        return textNodes;
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is of type {@link Node#TEXT_NODE} and it's not empty, {@code false}
     *         otherwise
     */
    private boolean isNonEmptyTextNode(Node node)
    {
        return node.getNodeType() == Node.TEXT_NODE && node.getNodeValue().length() > 0;
    }

    /**
     * Adds to or removes from the given text node the underlying style.
     * 
     * @param text the target text node
     * @param startIndex the first character to be processed
     * @param endIndex the last character to be processed
     * @param executed whether to add or remove the style
     * @return a text fragment indicating what has been processed
     */
    protected TextFragment execute(Text text, int startIndex, int endIndex, boolean executed)
    {
        return executed ? removeStyle(text, startIndex, endIndex) : addStyle(text, startIndex, endIndex);
    }

    /**
     * Formats the given text node, from the begin index to the end index.
     * 
     * @param text the text node to be formatted
     * @param firstCharIndex the first character on which we apply the style
     * @param lastCharIndex the last character on which we apply the style
     * @return a text fragment indicating what has been formatted
     */
    protected TextFragment addStyle(Text text, int firstCharIndex, int lastCharIndex)
    {
        if (matchesStyle(text)) {
            // Already styled. Skip.
            return new TextFragment(text, firstCharIndex, lastCharIndex);
        }

        Element styleElement = ((Document) text.getOwnerDocument()).xCreateElement(tagName);
        if (className != null) {
            styleElement.setClassName(className);
        }

        text.crop(firstCharIndex, lastCharIndex);
        text.getParentNode().replaceChild(styleElement, text);
        styleElement.appendChild(text);

        return new TextFragment(text, 0, text.getLength());
    }

    /**
     * Removes the underlying style from the given text node.
     * 
     * @param text the target text node
     * @param firstCharIndex the first character on which we remove the style
     * @param lastCharIndex the last character on which we remove the style
     * @return a text fragment indicating what has been unformatted
     */
    protected TextFragment removeStyle(Text text, int firstCharIndex, int lastCharIndex)
    {
        text.crop(firstCharIndex, lastCharIndex);
        Node child = text;
        Node parent = child.getParentNode();
        while (parent != null && matchesStyle(parent) && domUtils.isInline(parent) && split(parent, child)) {
            child = child.getParentNode();
            parent = child.getParentNode();
        }
        return new TextFragment(text, 0, text.getLength());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            if (!isExecuted(selection.getRangeAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param range the range to be inspected
     * @return {@code true} if this executable was executed on the given range
     */
    protected boolean isExecuted(Range range)
    {
        if (range.isCollapsed()) {
            return matchesStyle(range.getStartContainer());
        } else {
            List<Text> textNodes = getNonEmptyTextNodes(range);
            for (int i = 0; i < textNodes.size(); i++) {
                if (!matchesStyle(textNodes.get(i))) {
                    return false;
                }
            }
            return textNodes.size() > 0;
        }
    }

    /**
     * @param inputNode a DOM node
     * @return {@code true} if the given node matches the style associated with this executable, {@code false} otherwise
     */
    protected boolean matchesStyle(Node inputNode)
    {
        Node node = inputNode;
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getParentNode();
        }
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        return matchesStyle(Element.as(node));
    }

    /**
     * @param inputElement a DOM element
     * @return {@code true} if the given element matches the style associated with this executable, {@code false}
     *         otherwise
     */
    protected boolean matchesStyle(Element inputElement)
    {
        if (inheritable) {
            return matchesInheritedStyle(inputElement);
        } else {
            Node node = inputElement;
            while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                if (matchesInheritedStyle((Element) node)) {
                    return true;
                }
                node = node.getParentNode();
            }
            return false;
        }
    }

    /**
     * @param element a DOM element
     * @return {@code true} if the given element matches the style associated with this executable, without testing the
     *         ancestors of the element
     */
    protected boolean matchesInheritedStyle(Element element)
    {
        String computedValue = element.getComputedStyleProperty(propertyName);
        if (multipleValue) {
            return computedValue.toLowerCase().contains(propertyValue);
        } else {
            return propertyValue.equalsIgnoreCase(computedValue);
        }
    }

    /**
     * Splits the given parent node in two subtrees: left siblings of the given child in one side and right siblings on
     * the other side. The given child will then go up one level to the root, between the two sides.
     * 
     * @param parent the node that will be split
     * @param child the node that marks the place where the split is done
     * @return {@code true} if the split was done
     */
    private boolean split(Node parent, Node child)
    {
        assert (child.getParentNode() == parent);
        Node grandParent = parent.getParentNode();
        if (grandParent == null) {
            return false;
        }
        if (child.getPreviousSibling() != null) {
            Node leftClone = parent.cloneNode(false);
            Node leftSibling = child.getPreviousSibling();
            leftClone.appendChild(leftSibling);
            leftSibling = child.getPreviousSibling();
            while (leftSibling != null) {
                leftClone.insertBefore(leftSibling, leftClone.getFirstChild());
                leftSibling = child.getPreviousSibling();
            }
            grandParent.insertBefore(leftClone, parent);
        }
        if (child.getNextSibling() != null) {
            Node rightClone = parent.cloneNode(false);
            Node rightSibling = child.getNextSibling();
            while (rightSibling != null) {
                rightClone.appendChild(rightSibling);
                rightSibling = child.getNextSibling();
            }
            if (parent.getNextSibling() != null) {
                grandParent.insertBefore(rightClone, parent.getNextSibling());
            } else {
                grandParent.appendChild(rightClone);
            }
        }
        if (!matchesStyle(grandParent)) {
            grandParent.replaceChild(child, parent);
            return false;
        } else {
            return true;
        }
    }
}
