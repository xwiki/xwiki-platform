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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Property;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.Text;
import org.xwiki.gwt.dom.client.TextFragment;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

/**
 * Applies in-line style to the current selection.
 * 
 * @version $Id$
 */
public class InlineStyleExecutable extends AbstractSelectionExecutable
{
    /**
     * The style property used when applying style.
     */
    private final Property property;

    /**
     * Creates a new instance that uses the given style property.
     * 
     * @param rta the execution target
     * @param property the style property used when applying style
     */
    public InlineStyleExecutable(RichTextArea rta, Property property)
    {
        super(rta);
        this.property = property;
    }

    /**
     * @return the style property associated with the executable
     */
    protected Property getProperty()
    {
        return property;
    }

    @Override
    public boolean execute(String parameter)
    {
        Selection selection = rta.getDocument().getSelection();
        List<Range> ranges = new ArrayList<Range>();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            ranges.add(execute(selection.getRangeAt(i), parameter));
        }
        selection.removeAllRanges();
        for (Range range : ranges) {
            selection.addRange(range);
        }
        return true;
    }

    /**
     * Applies the underlying style {@link #property} with the given value to the specified range.
     * 
     * @param range the target range
     * @param parameter the value to set for the style {@link #property}
     * @return the given range after being processed
     */
    protected Range execute(Range range, String parameter)
    {
        Range styledRange = range;
        if (range.isCollapsed()) {
            switch (range.getStartContainer().getNodeType()) {
                case Node.TEXT_NODE:
                    Text text = (Text) range.getStartContainer();
                    text = execute(text, range.getStartOffset(), range.getEndOffset(), parameter).getText();
                    range.selectNodeContents(text);
                    break;
                case Node.ELEMENT_NODE:
                    Text empty = (Text) range.getStartContainer().getOwnerDocument().createTextNode("");
                    domUtils.insertAt(range.getStartContainer(), empty, range.getStartOffset());
                    range.selectNodeContents(execute(empty, 0, 0, parameter).getText());
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
                endContainer = execute(text, startIndex, endIndex, parameter);
                if (startContainer == null) {
                    startContainer = endContainer;
                }
            }
            if (startContainer != null) {
                // We cannot reuse the given range because it may have been invalidated by the DOM mutations.
                styledRange = ((Document) startContainer.getText().getOwnerDocument()).createRange();
                styledRange.setStart(startContainer.getText(), startContainer.getStartIndex());
                styledRange.setEnd(endContainer.getText(), endContainer.getEndIndex());
            }
        }
        return styledRange;
    }

    /**
     * @param range a DOM range
     * @return the list of non empty text nodes that are completely or partially (at least one character) included in
     *         the given range
     */
    protected List<Text> getNonEmptyTextNodes(Range range)
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
     * Applies the underlying style {@link #property} with the given value to the specified text fragment.
     * 
     * @param text the target text node
     * @param startIndex the first character to be processed
     * @param endIndex the last character to be processed
     * @param parameter the value to set for the style {@link #property}
     * @return a text fragment indicating what has been processed
     */
    protected TextFragment execute(Text text, int startIndex, int endIndex, String parameter)
    {
        // Make sure the style is applied only to the selected text.
        text.crop(startIndex, endIndex);

        // Look for the farthest in-line element ancestor without sibling nodes.
        Element ancestor = null;
        Node node = text.getParentNode();
        while (node.getChildNodes().getLength() == 1 && domUtils.isInline(node)) {
            ancestor = (Element) node;
            node = node.getParentNode();
        }

        // If we haven't found the proper ancestor, we wrap the text in a span element.
        if (ancestor == null) {
            ancestor = text.getOwnerDocument().createSpanElement().cast();
            text.getParentNode().replaceChild(ancestor, text);
            ancestor.appendChild(text);
        }

        // Apply the style.
        addStyle(ancestor, parameter);

        return new TextFragment(text, 0, text.getLength());
    }

    /**
     * Styles the given element.
     * 
     * @param element the element to be styled
     * @param parameter the value of the style property that is added
     */
    protected void addStyle(Element element, String parameter)
    {
        element.getStyle().setProperty(property.getJSName(), parameter);
    }

    @Override
    public String getParameter()
    {
        Selection selection = rta.getDocument().getSelection();
        String selectionParameter = null;
        for (int i = 0; i < selection.getRangeCount(); i++) {
            String rangeParameter = getParameter(selection.getRangeAt(i));
            if (rangeParameter == null || (selectionParameter != null && !selectionParameter.equals(rangeParameter))) {
                return null;
            }
            selectionParameter = rangeParameter;
        }
        return selectionParameter;
    }

    /**
     * @param range the range to be inspected
     * @return the value of the style {@link #property} for the given range
     */
    protected String getParameter(Range range)
    {
        if (range.isCollapsed()) {
            return getParameter(range.getStartContainer());
        } else {
            List<Text> textNodes = getNonEmptyTextNodes(range);
            String rangeParameter = null;
            for (int i = 0; i < textNodes.size(); i++) {
                String textParameter = getParameter(textNodes.get(i));
                if (textParameter == null || (rangeParameter != null && !rangeParameter.equals(textParameter))) {
                    return null;
                }
                rangeParameter = textParameter;
            }
            return rangeParameter;
        }
    }

    /**
     * @param inputNode a DOM node
     * @return the value of the style {@link #property} for the given node
     */
    protected String getParameter(Node inputNode)
    {
        Node node = inputNode;
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getParentNode();
        }
        return node == null || node.getNodeType() != Node.ELEMENT_NODE ? null : getParameter(Element.as(node));
    }

    /**
     * @param element a DOM element
     * @return the {@link #property} value taken from the given element's computed style
     */
    protected String getParameter(Element element)
    {
        if (getProperty().isInheritable()) {
            return element.getComputedStyleProperty(property.getJSName());
        } else {
            Node node = element;
            while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                String value = Element.as(node).getComputedStyleProperty(property.getJSName());
                if (!StringUtils.areEqual(getProperty().getDefaultValue(), value)) {
                    return value;
                }
                node = node.getParentNode();
            }
            return getProperty().getDefaultValue();
        }
    }
}
