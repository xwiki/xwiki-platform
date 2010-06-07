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
package com.xpn.xwiki.wysiwyg.server.cleaner.internal;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;
import org.xwiki.xml.html.filter.HTMLFilter;

import com.steadystate.css.parser.CSSOMParser;

/**
 * Merges the values of the {@code style} attribute from nested elements and applies the result directly to the inner
 * text to overcome the fact that some wiki syntaxes (e.g. xwiki/2.0) don't support nested custom parameters and the
 * renderer they use doesn't know how to merge the style parameter.
 * 
 * @version $Id$
 */
public class NestedStylesFilter implements HTMLFilter
{
    /**
     * The interface used to iterate a DOM document.
     */
    private static interface Visitor
    {
        /**
         * Enters the given node.
         * 
         * @param node a DOM node
         */
        void enter(Node node);

        /**
         * Leaves the given node.
         * 
         * @param node a DOM node
         */
        void leave(Node node);
    }

    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(NestedStylesFilter.class);

    /**
     * The name of the style attribute.
     */
    private static final String STYLE_ATTRIBUTE = "style";

    /**
     * The list of CSS properties that are merged and applied directly to the inner text.
     */
    private static final List<String> MERGED_PROPERTIES =
        Arrays.asList(new String[] {"background-color", "color", "font-family", "font-size"});

    /**
     * The object used to parse the style attribute.
     */
    private final CSSOMParser cssParser = new CSSOMParser();

    /**
     * {@inheritDoc}
     * 
     * @see HTMLFilter#filter(Document, Map)
     */
    public void filter(Document document, Map<String, String> parameters)
    {
        final Stack<Element> styledAncestors = new Stack<Element>();
        iterate(document, new Visitor()
        {
            public void enter(Node node)
            {
                if (isStyled(node)) {
                    styledAncestors.push((Element) node);
                }
            }

            public void leave(Node node)
            {
                if (!styledAncestors.isEmpty()) {
                    if (styledAncestors.peek().equals(node)) {
                        styledAncestors.pop();
                    } else if (node.getNodeType() == Node.TEXT_NODE) {
                        mergeStyles(node, styledAncestors);
                    }
                }
            }
        });
    }

    /**
     * Iterates the given document in depth-first pre-order and calls the visitor whenever we enter or leave a node.
     * 
     * @param document the document to be iterated
     * @param visitor the object to be notified whenever we enter or leave a node
     */
    private void iterate(Document document, Visitor visitor)
    {
        Node node = document;
        while (node != null) {
            visitor.enter(node);
            if (node.hasChildNodes()) {
                node = node.getFirstChild();
            } else {
                // Note that we notify the visitor only after we move to the next node to allow the visitor to replace
                // the visited node with a subtree.
                while (node != null && node.getNextSibling() == null) {
                    Node toLeave = node;
                    node = node.getParentNode();
                    visitor.leave(toLeave);
                }
                if (node != null) {
                    Node toLeave = node;
                    node = node.getNextSibling();
                    visitor.leave(toLeave);
                }
            }
        }
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is an element whose style attribute contains at least one of the
     *         {@link #MERGED_PROPERTIES}, {@code false} otherwise
     */
    private boolean isStyled(Node node)
    {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        Element element = (Element) node;
        if (!element.hasAttribute(STYLE_ATTRIBUTE)) {
            return false;
        }
        CSSStyleDeclaration style = parseStyleAttribute(element.getAttribute(STYLE_ATTRIBUTE));
        if (style != null) {
            for (String property : MERGED_PROPERTIES) {
                if (style.getPropertyCSSValue(property) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Parses the value of a style attribute.
     * 
     * @param value the value of the style attribute to be parsed
     * @return the object representation of the CSS contained in the style attribute
     */
    private CSSStyleDeclaration parseStyleAttribute(String value)
    {
        try {
            return cssParser.parseStyleDeclaration(new InputSource(new StringReader(value)));
        } catch (Exception e) {
            LOG.error("Failed to parse the style attribute: " + value, e);
            return null;
        }
    }

    /**
     * Merges the given style declarations and applies the result on the given node.
     * 
     * @param node the node to be styled
     * @param styledAncestors the list of element ancestors for which {@link #isStyled(Node)} returns {@code true}
     */
    private void mergeStyles(Node node, Stack<Element> styledAncestors)
    {
        Element wrapper = styledAncestors.peek().getOwnerDocument().createElement("span");
        node.getParentNode().replaceChild(wrapper, node);
        wrapper.appendChild(node);

        // Merge the style attributes from all the ancestors.
        StringBuilder mergedStyleAttribute = new StringBuilder();
        for (Element ancestor : styledAncestors) {
            if (mergedStyleAttribute.length() > 0
                && mergedStyleAttribute.charAt(mergedStyleAttribute.length() - 1) != ';') {
                mergedStyleAttribute.append(';');
            }
            mergedStyleAttribute.append(ancestor.getAttribute(STYLE_ATTRIBUTE));
        }

        // Parse the merged style attribute.
        CSSStyleDeclaration mergedStyle = parseStyleAttribute(mergedStyleAttribute.toString());

        // Keep only the MERGED_PROPERTIES.
        CSSStyleDeclaration filteredStyle = parseStyleAttribute("");
        for (String property : MERGED_PROPERTIES) {
            CSSValue value = mergedStyle.getPropertyCSSValue(property);
            if (value != null) {
                filteredStyle.setProperty(property, value.getCssText(), mergedStyle.getPropertyPriority(property));
            }
        }

        // Apply the style.
        wrapper.setAttribute(STYLE_ATTRIBUTE, filteredStyle.getCssText());
    }
}
