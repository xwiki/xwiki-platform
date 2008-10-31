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
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Applies in-line formatting to the current selection.
 * 
 * @version $Id$
 */
public class StyleExecutable implements Executable
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
     * Creates a new instance.
     * 
     * @param tagName {@link #tagName}
     * @param className {@link #className}
     * @param propertyName {@link #propertyName}
     * @param propertyValue {@link #propertyValue}
     * @param inheritable {@link #inheritable}
     */
    public StyleExecutable(String tagName, String className, String propertyName, String propertyValue,
        boolean inheritable)
    {
        this.tagName = tagName;
        this.className = className;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.inheritable = inheritable;
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
     * @see Executable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String parameter)
    {
        Document doc = rta.getDocument();
        boolean executed = isExecuted(rta);
        Selection selection = doc.getSelection();
        List<Range> ranges = new ArrayList<Range>();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            if (executed) {
                ranges.add(removeStyle(doc, DOMUtils.getInstance().getTextRange(selection.getRangeAt(i))));
            } else {
                ranges.add(addStyle(doc, DOMUtils.getInstance().getTextRange(selection.getRangeAt(i))));
            }
        }
        selection.removeAllRanges();
        for (Range range : ranges) {
            selection.addRange(range);
        }
        return true;
    }

    /**
     * Adds in-line formatting to the given range from the specified document.
     * 
     * @param doc The document who hosts the range.
     * @param range The range on which the formatting will be applied.
     * @return The new range corresponding to the formatted text.
     */
    protected Range addStyle(Document doc, Range range)
    {
        Range newRange = doc.createRange();
        if (range.getCommonAncestorContainer().getNodeType() == Node.TEXT_NODE) {
            Text text = Text.as(range.getCommonAncestorContainer());
            addStyle(text, range.getStartOffset(), range.getEndOffset());
            newRange.selectNodeContents(text);
        } else if (range.isCollapsed()) {
            Element leafElement = Element.as(range.getStartContainer().getChildNodes().getItem(range.getStartOffset()));
            assert (!leafElement.hasChildNodes());
            Text text = leafElement.getOwnerDocument().createTextNode("").cast();
            leafElement.getParentNode().insertBefore(text, leafElement);
            addStyle(text, 0, 0);
            newRange.selectNodeContents(text);
        } else {
            Text firstText = Text.as(range.getStartContainer());
            Text lastText = Text.as(range.getEndContainer());
            if (!matchesStyle(firstText)) {
                addStyle(firstText, range.getStartOffset(), firstText.getLength());
                newRange.setStart(firstText, 0);
            } else {
                newRange.setStart(firstText, range.getStartOffset());
            }

            Node node = DOMUtils.getInstance().getNextLeaf(firstText);
            while (node != null && node != lastText) {
                if (node.getNodeType() == Node.TEXT_NODE && !matchesStyle(node)) {
                    Text text = Text.as(node);
                    addStyle(text, 0, text.getLength());
                }
                node = DOMUtils.getInstance().getNextLeaf(node);
            }

            if (node == lastText && !matchesStyle(lastText)) {
                addStyle(lastText, 0, range.getEndOffset());
                newRange.setEnd(lastText, lastText.getLength());
            } else {
                newRange.setEnd(lastText, range.getEndOffset());
            }
        }
        return newRange;
    }

    /**
     * Formats the given text node, from the begin index to the end index.
     * 
     * @param text The text node to be formatted.
     * @param firstCharIndex The first character on which we apply the style.
     * @param lastCharIndex The last character on which we apply the style.
     */
    protected void addStyle(Text text, int firstCharIndex, int lastCharIndex)
    {
        int beginIndex = firstCharIndex;
        int endIndex = lastCharIndex;
        if (beginIndex > 0) {
            String leftData = text.getData().substring(0, beginIndex);
            Text left = text.getOwnerDocument().createTextNode(leftData).cast();
            text.getParentNode().insertBefore(left, text);
            text.setData(text.getData().substring(beginIndex));
            endIndex -= beginIndex;
            beginIndex = 0;
        }

        if (endIndex < text.getLength()) {
            String rightData = text.getData().substring(endIndex);
            Text right = text.getOwnerDocument().createTextNode(rightData).cast();
            if (text.getNextSibling() != null) {
                text.getParentNode().insertBefore(right, text.getNextSibling());
            } else {
                text.getParentNode().appendChild(right);
            }
            text.setData(text.getData().substring(beginIndex, endIndex));
        }

        com.google.gwt.dom.client.Element styleElement = ((Document) text.getOwnerDocument()).xCreateElement(tagName);
        if (className != null) {
            styleElement.setClassName(className);
        }

        Node ancestor = text;
        while (ancestor.getParentNode() != null && ancestor.getPreviousSibling() == null
            && ancestor.getNextSibling() == null && DOMUtils.getInstance().isInline(ancestor.getParentNode())) {
            ancestor = ancestor.getParentNode();
        }
        ancestor.getParentNode().replaceChild(styleElement, ancestor);
        styleElement.appendChild(ancestor);
    }

    /**
     * Removes in-line formatting on the given range from the specified document.
     * 
     * @param doc The document who hosts the range.
     * @param range The range from which the formatting will be removed.
     * @return The new range corresponding to the text without the formatting.
     */
    protected Range removeStyle(Document doc, Range range)
    {
        Range newRange = doc.createRange();
        if (range.getCommonAncestorContainer().getNodeType() == Node.TEXT_NODE) {
            Text text = Text.as(range.getCommonAncestorContainer());
            removeStyle(text, range.getStartOffset(), range.getEndOffset());

            TextFragment fragment = text.normalize();
            newRange.setStart(fragment.getText(), fragment.getStartIndex());
            newRange.setEnd(fragment.getText(), fragment.getEndIndex());
        } else if (range.isCollapsed()) {
            Element leafElement = Element.as(range.getStartContainer().getChildNodes().getItem(range.getStartOffset()));
            assert (!leafElement.hasChildNodes());
            Text text = leafElement.getOwnerDocument().createTextNode("").cast();
            leafElement.getParentNode().insertBefore(text, leafElement);
            removeStyle(text, 0, 0);

            TextFragment fragment = text.normalize();
            newRange.setStart(fragment.getText(), fragment.getStartIndex());
            newRange.setEnd(fragment.getText(), fragment.getEndIndex());
        } else {
            Text firstText = Text.as(range.getStartContainer());
            Text lastText = Text.as(range.getEndContainer());
            removeStyle(firstText, range.getStartOffset(), firstText.getLength());

            Node node = DOMUtils.getInstance().getNextLeaf(firstText);
            while (node != null && node != lastText) {
                if (node.getNodeType() == Node.TEXT_NODE) {
                    Text text = Text.as(node);
                    removeStyle(text, 0, text.getLength());
                }
                node = DOMUtils.getInstance().getNextLeaf(node);
            }

            if (node == lastText) {
                removeStyle(lastText, 0, range.getEndOffset());
                int lastTextOffset = lastText.getOffset();

                TextFragment firstFragment = firstText.normalize();
                newRange.setStart(firstFragment.getText(), firstFragment.getStartIndex());
                if (lastText.getParentNode() != null) {
                    TextFragment lastFragment = lastText.normalize();
                    newRange.setEnd(lastFragment.getText(), lastFragment.getEndIndex());
                } else {
                    newRange.setEnd(firstFragment.getText(), lastTextOffset + lastText.getLength());
                }
            } else {
                TextFragment firstFragment = firstText.normalize();
                newRange.setStart(firstFragment.getText(), firstFragment.getStartIndex());
                newRange.setEnd(firstFragment.getText(), firstFragment.getStartIndex());
            }
        }
        return newRange;
    }

    /**
     * Removes in-line formatting on the given text node, from the begin index to the end index.
     * 
     * @param text The text node whose formatting will be removed.
     * @param firstCharIndex The first character on which we remove the style
     * @param lastCharIndex The last character on which we remove the style.
     */
    protected void removeStyle(Text text, int firstCharIndex, int lastCharIndex)
    {
        int beginIndex = firstCharIndex;
        int endIndex = lastCharIndex;
        if (beginIndex > 0) {
            String leftData = text.getData().substring(0, beginIndex);
            Text left = text.getOwnerDocument().createTextNode(leftData).cast();
            text.getParentNode().insertBefore(left, text);
            text.setData(text.getData().substring(beginIndex));
            endIndex -= beginIndex;
            beginIndex = 0;
        }

        if (endIndex < text.getLength()) {
            String rightData = text.getData().substring(endIndex);
            Text right = text.getOwnerDocument().createTextNode(rightData).cast();
            if (text.getNextSibling() != null) {
                text.getParentNode().insertBefore(right, text.getNextSibling());
            } else {
                text.getParentNode().appendChild(right);
            }
            text.setData(text.getData().substring(beginIndex, endIndex));
        }

        Node child = text;
        Node parent = child.getParentNode();
        while (parent != null && matchesStyle(parent) && DOMUtils.getInstance().isInline(parent)
            && split(parent, child)) {
            child = child.getParentNode();
            parent = child.getParentNode();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            if (!isEnabled(selection.getRangeAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param range The range to be inspected.
     * @return true if this executable can be execute on the given range.
     */
    private boolean isEnabled(Range range)
    {
        // Right now this executable is not restricted. We'll add here future restrictions.
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            if (!isExecuted(DOMUtils.getInstance().getTextRange(selection.getRangeAt(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param range The range to be inspected.
     * @return true if this executable was executed on the given range.
     */
    private boolean isExecuted(Range range)
    {
        if (range.isCollapsed() || range.getCommonAncestorContainer().getNodeType() == Node.TEXT_NODE
            || range.getStartContainer() == range.getEndContainer()) {
            return matchesStyle(range.getCommonAncestorContainer());
        } else {
            Node node = range.getStartContainer();
            if (!matchesStyle(node)) {
                return false;
            }
            while (node != range.getEndContainer()) {
                node = DOMUtils.getInstance().getNextLeaf(node);
                if (!matchesStyle(node)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        return true;
    }

    /**
     * @param inputNode A DOM node.
     * @return true if the given node matches the style associated with this executable.
     */
    protected boolean matchesStyle(Node inputNode)
    {
        Node node = inputNode;
        if (node.getNodeType() == Node.TEXT_NODE) {
            node = node.getParentNode();
        }
        if (inheritable) {
            return propertyValue.equalsIgnoreCase(Element.as(node).getComputedStyleProperty(propertyName));
        } else {
            while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (propertyValue.equalsIgnoreCase(element.getComputedStyleProperty(propertyName))) {
                    return true;
                }
                node = node.getParentNode();
            }
            return false;
        }
    }

    /**
     * Splits the given parent node in two subtrees: left siblings of the given child in one side and right siblings on
     * the other side. The given child will then go up one level to the root, between the two sides.
     * 
     * @param parent The node that will be split.
     * @param child The node that marks the place where the split is done.
     * @return true if the split was done.
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
