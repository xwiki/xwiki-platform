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

import java.util.List;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Property;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.dom.client.Text;
import org.xwiki.gwt.dom.client.TextFragment;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Node;

/**
 * Toggles a style property on the current selection.
 * 
 * @version $Id$
 */
public class ToggleInlineStyleExecutable extends InlineStyleExecutable
{
    /**
     * The value of the style property when the style is applied.
     */
    private final String value;

    /**
     * The tag used to toggle the style. It is a formatting tag that can be used as a shorthand in place of the style
     * property.
     */
    private final String tagName;

    /**
     * Flag indicating if this executable has been executed on the current selection. This flag determines if the style
     * is toggled on or off.
     */
    private boolean executed;

    /**
     * Creates a new executable that toggles the given style property on the current selection using the specified tag
     * name.
     * 
     * @param rta the execution target
     * @param property the style property used when detecting style
     * @param value the value of the style property when the style is applied
     * @param tagName the tag used to toggle the style
     */
    public ToggleInlineStyleExecutable(RichTextArea rta, Property property, String value, String tagName)
    {
        super(rta, property);

        this.value = value;
        this.tagName = tagName;
    }

    @Override
    public boolean execute(String parameter)
    {
        executed = isExecuted();
        return super.execute(parameter);
    }

    @Override
    protected TextFragment execute(Text text, int startIndex, int endIndex, String parameter)
    {
        return executed ? removeStyle(text, startIndex, endIndex) : addStyle(text, startIndex, endIndex);
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
        // Make sure we remove the style only from the selected text.
        text.crop(firstCharIndex, lastCharIndex);

        // Look for the element ancestor that has the underlying style.
        Node child = text;
        Node parent = child.getParentNode();
        while (parent != null && matchesStyle(parent) && domUtils.isInline(parent)) {
            domUtils.isolate(child);
            child = child.getParentNode();
            parent = child.getParentNode();
        }

        if (tagName.equalsIgnoreCase(child.getNodeName())) {
            // The style is enforced by a formatting element. We have to remove or rename it.
            Element element = (Element) child;
            if (element.hasAttributes()) {
                // We must keep the attributes. Let's rename the element.
                Element replacement = element.getOwnerDocument().createSpanElement().cast();
                JsArrayString attributes = element.getAttributeNames();
                for (int i = 0; i < attributes.length(); i++) {
                    replacement.setAttribute(attributes.get(i), element.getAttribute(attributes.get(i)));
                }
                replacement.appendChild(element.extractContents());
                element.getParentNode().replaceChild(replacement, element);
            } else {
                // We remove the element but keep its child nodes.
                element.unwrap();
            }
        } else {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                // Wrap the child with a span element.
                Node wrapper = child.getOwnerDocument().createSpanElement();
                child.getParentNode().replaceChild(wrapper, child);
                wrapper.appendChild(child);

                child = wrapper;
            }
            // The style is enforced using CSS. Let's reset the style property to its default value.
            ((Element) child).getStyle().setProperty(getProperty().getJSName(), getProperty().getDefaultValue());
        }

        return new TextFragment(text, 0, text.getLength());
    }

    /**
     * Adds the underlying style to the given text node.
     * 
     * @param text the target text node
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

        // Make sure we apply the style only to the selected text.
        text.crop(firstCharIndex, lastCharIndex);

        Element element = (Element) text.getOwnerDocument().createElement(tagName);
        text.getParentNode().replaceChild(element, text);
        element.appendChild(text);

        return new TextFragment(text, 0, text.getLength());
    }

    @Override
    public boolean isExecuted()
    {
        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            if (!isExecuted(selection.getRangeAt(i))) {
                return false;
            }
        }
        return selection.getRangeCount() > 0;
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
        if (getProperty().isInheritable()) {
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
        String computedValue = element.getComputedStyleProperty(getProperty().getJSName());
        if (getProperty().isMultipleValue()) {
            return computedValue != null && computedValue.toLowerCase().contains(value);
        } else {
            return value.equalsIgnoreCase(computedValue);
        }
    }
}
