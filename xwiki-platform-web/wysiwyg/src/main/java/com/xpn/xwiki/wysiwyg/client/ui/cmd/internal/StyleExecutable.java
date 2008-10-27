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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Element;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.Selection;
import com.xpn.xwiki.wysiwyg.client.util.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.util.Document;
import com.xpn.xwiki.wysiwyg.client.util.TextFragment;

public class StyleExecutable extends DefaultExecutable
{
    private final String tagName;

    private final String className;

    private final String propertyName;

    private final String propertyValue;

    private final boolean inheritable;

    public StyleExecutable(String tagName, String className, String propertyName, String propertyValue,
        boolean inheritable, String command)
    {
        super(command);

        this.tagName = tagName;
        this.className = className;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.inheritable = inheritable;
    }

    public String getTagName()
    {
        return tagName;
    }

    public String getClassName()
    {
        return className;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public String getPropertyValue()
    {
        return propertyValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultExecutable#execute(Document, String)
     */
    public boolean execute(Document doc, String parameter)
    {
        boolean executed = isExecuted(doc);
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

    protected Range addStyle(Document doc, Range range)
    {
        Range newRange = doc.createRange();
        if (range.getCommonAncestorContainer().getNodeType() == Node.TEXT_NODE) {
            Text text = Text.as(range.getCommonAncestorContainer());
            addStyle(text, range.getStartOffset(), range.getEndOffset());
            newRange.selectNodeContents(text);
        } else if (range.isCollapsed()) {
            com.google.gwt.dom.client.Element leafElement =
                Element.as(range.getStartContainer().getChildNodes().getItem(range.getStartOffset()));
            assert (!leafElement.hasChildNodes());
            Text text = leafElement.getOwnerDocument().createTextNode("");
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

    protected void addStyle(Text text, int beginIndex, int endIndex)
    {
        if (beginIndex > 0) {
            String leftData = text.getData().substring(0, beginIndex);
            Text left = text.getOwnerDocument().createTextNode(leftData);
            text.getParentNode().insertBefore(left, text);
            text.setData(text.getData().substring(beginIndex));
            endIndex -= beginIndex;
            beginIndex = 0;
        }

        if (endIndex < text.getLength()) {
            String rightData = text.getData().substring(endIndex);
            Text right = text.getOwnerDocument().createTextNode(rightData);
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

    protected Range removeStyle(Document doc, Range range)
    {
        Range newRange = doc.createRange();
        if (range.getCommonAncestorContainer().getNodeType() == Node.TEXT_NODE) {
            Text text = Text.as(range.getCommonAncestorContainer());
            removeStyle(text, range.getStartOffset(), range.getEndOffset());

            TextFragment fragment = DOMUtils.getInstance().normalize(text);
            newRange.setStart(fragment.getText(), fragment.getStartIndex());
            newRange.setEnd(fragment.getText(), fragment.getEndIndex());
        } else if (range.isCollapsed()) {
            com.google.gwt.dom.client.Element leafElement =
                Element.as(range.getStartContainer().getChildNodes().getItem(range.getStartOffset()));
            assert (!leafElement.hasChildNodes());
            Text text = leafElement.getOwnerDocument().createTextNode("");
            leafElement.getParentNode().insertBefore(text, leafElement);
            removeStyle(text, 0, 0);

            TextFragment fragment = DOMUtils.getInstance().normalize(text);
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
                int lastTextOffset = DOMUtils.getInstance().getOffset(lastText);

                TextFragment firstFragment = DOMUtils.getInstance().normalize(firstText);
                newRange.setStart(firstFragment.getText(), firstFragment.getStartIndex());
                if (lastText.getParentNode() != null) {
                    TextFragment lastFragment = DOMUtils.getInstance().normalize(lastText);
                    newRange.setEnd(lastFragment.getText(), lastFragment.getEndIndex());
                } else {
                    newRange.setEnd(firstFragment.getText(), lastTextOffset + lastText.getLength());
                }
            } else {
                TextFragment firstFragment = DOMUtils.getInstance().normalize(firstText);
                newRange.setStart(firstFragment.getText(), firstFragment.getStartIndex());
                newRange.setEnd(firstFragment.getText(), firstFragment.getStartIndex());
            }
        }
        return newRange;
    }

    protected void removeStyle(Text text, int beginIndex, int endIndex)
    {
        if (beginIndex > 0) {
            String leftData = text.getData().substring(0, beginIndex);
            Text left = text.getOwnerDocument().createTextNode(leftData);
            text.getParentNode().insertBefore(left, text);
            text.setData(text.getData().substring(beginIndex));
            endIndex -= beginIndex;
            beginIndex = 0;
        }

        if (endIndex < text.getLength()) {
            String rightData = text.getData().substring(endIndex);
            Text right = text.getOwnerDocument().createTextNode(rightData);
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
     * @see DefaultExecutable#getParameter(Document)
     */
    public String getParameter(Document doc)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultExecutable#isEnabled(Document)
     */
    public boolean isEnabled(Document doc)
    {
        Selection selection = doc.getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            if (!isEnabled(selection.getRangeAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isEnabled(Range range)
    {
        // Right now this executable is not restricted. We'll add here future restrictions.
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultExecutable#isExecuted(Document)
     */
    public boolean isExecuted(Document doc)
    {
        Selection selection = doc.getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            if (!isExecuted(DOMUtils.getInstance().getTextRange(selection.getRangeAt(i)))) {
                return false;
            }
        }
        return true;
    }

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
     * @see DefaultExecutable#isSupported(Document)
     */
    public boolean isSupported(Document doc)
    {
        return true;
    }

    protected boolean matchesStyle(Node node)
    {
        if (node.getNodeType() == Node.TEXT_NODE) {
            node = node.getParentNode();
        }
        if (inheritable) {
            return propertyValue.equalsIgnoreCase(DOMUtils.getInstance().getComputedStyleProperty((Element) node,
                propertyName));
        } else {
            while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (propertyValue.equalsIgnoreCase(DOMUtils.getInstance().getComputedStyleProperty(element,
                    propertyName))) {
                    return true;
                }
                node = node.getParentNode();
            }
            return false;
        }
    }

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
