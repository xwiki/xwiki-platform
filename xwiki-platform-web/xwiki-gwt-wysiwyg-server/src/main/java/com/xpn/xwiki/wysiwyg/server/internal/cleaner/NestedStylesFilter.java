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
package com.xpn.xwiki.wysiwyg.server.internal.cleaner;

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
 * Merges the values of the {@code style} attribute from nested in-line HTML elements and applies the result directly to
 * the inner text to overcome the fact that some wiki syntaxes (e.g. xwiki/2.0) don't support nested custom in-line
 * parameters and the renderer they use doesn't know how to merge the style parameter.
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
        Arrays.asList(new String[] {"background-color", "color", "font-family", "font-size", "font-style",
            "font-weight", "text-decoration", "vertical-align"});

    /**
     * The list of most important HTML elements that can have both in-line and block-level content, as specified by the
     * XHTML 1.0 strict DTD.
     */
    private static final List<String> HTML_FLOW_CONTAINERS =
        Arrays.asList(new String[] {"body", "li", "td", "th", "dd", "div", "blockquote"});

    /**
     * The list of most important block-level HTML elements that can have only in-line content, as specified by the
     * XHTML 1.0 strict DTD.
     */
    private static final List<String> HTML_BLOCK_LEVEL_INLINE_CONTAINERS =
        Arrays.asList(new String[] {"p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "dt", "address"});

    /**
     * The list of most important block-level HTML elements that can have only special content, or no content at all, as
     * specified by the XHTML 1.0 strict DTD.
     */
    private static final List<String> HTML_SPECIAL_BLOCK_LEVEL_ELEMENTS =
        Arrays.asList(new String[] {"hr", "ul", "ol", "dl", "table", "tbody", "thead", "tfoot", "tr", "form"});

    /**
     * The object used to parse the style attribute.
     */
    private final CSSOMParser cssParser = new CSSOMParser();

    /**
     * The stack of styled ancestors.
     */
    private final Stack<Element> styledAncestors = new Stack<Element>();

    /**
     * Flag indicating if the merged style is applied by wrapping the text nodes or by updating the style of the last
     * styled ancestor. We wrap all the text nodes in a nested style group, except the first one.
     */
    private boolean wrapTextNodes;

    /**
     * {@inheritDoc}
     * 
     * @see HTMLFilter#filter(Document, Map)
     */
    public void filter(Document document, Map<String, String> parameters)
    {
        styledAncestors.clear();
        wrapTextNodes = false;
        iterate(document, new Visitor()
        {
            public void enter(Node node)
            {
                NestedStylesFilter.this.enter(node);
            }

            public void leave(Node node)
            {
                NestedStylesFilter.this.leave(node);
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
     * Enters the given node.
     * 
     * @param node a DOM node
     */
    private void enter(Node node)
    {
        if (isStyled(node)) {
            styledAncestors.push((Element) node);
        }
    }

    /**
     * Leaves the given node.
     * 
     * @param node a DOM node
     */
    private void leave(Node node)
    {
        if (!styledAncestors.isEmpty()) {
            if (styledAncestors.peek().equals(node)) {
                styledAncestors.pop();
                if (styledAncestors.isEmpty()) {
                    // We're leaving a nested style group.
                    wrapTextNodes = false;
                }
            } else if (node.getNodeType() == Node.TEXT_NODE && (wrapTextNodes || styledAncestors.size() > 1)) {
                mergeStyles(node, styledAncestors, wrapTextNodes);
                // We're inside a nested style group.
                wrapTextNodes = true;
            }
        }
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is an in-line HTML element whose style attribute contains at least one of
     *         the {@link #MERGED_PROPERTIES}, {@code false} otherwise
     */
    private boolean isStyled(Node node)
    {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        Element element = (Element) node;
        if (!element.hasAttribute(STYLE_ATTRIBUTE) || isBlock(element)) {
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
     * @param element an HTML element
     * @return {@code true} if the given element is a block-level element as specified in the XHTML 1.0 strict DTD,
     *         {@code false} otherwise
     */
    private boolean isBlock(Element element)
    {
        String tagName = element.getTagName().toLowerCase();
        return HTML_FLOW_CONTAINERS.contains(tagName) || HTML_BLOCK_LEVEL_INLINE_CONTAINERS.contains(tagName)
            || HTML_SPECIAL_BLOCK_LEVEL_ELEMENTS.contains(tagName);
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
     * @param wrapNode flag indicating if the merged style should be applied by wrapping the given node or by updating
     *            the style of the last styled ancestor
     */
    private void mergeStyles(Node node, Stack<Element> styledAncestors, boolean wrapNode)
    {
        // Merge the style attributes from all the ancestors.
        StringBuilder mergedStyleAttribute = new StringBuilder();
        // CSSStyleDeclaration implementation uses currently a list to store the declared properties and properties that
        // appear multiple times are not merged. As a consequence, when querying the value of a property only its first
        // occurence is taken into account. We iterate starting from the top styled ancestor to give priority to the
        // most recent styled ancestors.
        for (int i = styledAncestors.size() - 1; i >= 0; i--) {
            Element ancestor = styledAncestors.get(i);
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
        if (wrapNode) {
            Element wrapper = styledAncestors.peek().getOwnerDocument().createElement("span");
            node.getParentNode().replaceChild(wrapper, node);
            wrapper.appendChild(node);
            wrapper.setAttribute(STYLE_ATTRIBUTE, filteredStyle.getCssText());
        } else {
            styledAncestors.peek().setAttribute(STYLE_ATTRIBUTE, filteredStyle.getCssText());
        }
    }
}
