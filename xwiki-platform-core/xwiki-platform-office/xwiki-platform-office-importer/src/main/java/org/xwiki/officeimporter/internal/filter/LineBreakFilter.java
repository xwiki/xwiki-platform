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
package org.xwiki.officeimporter.internal.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;
import org.xwiki.xml.html.filter.ElementSelector;

/**
 * Replaces {@code<br/>} elements placed in between block elements with {@code<div class="wikikmodel-emptyline"/>}.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/linebreak")
public class LineBreakFilter extends AbstractHTMLFilter
{
    /**
     * List of block element tag names.
     */
    private static final String[] BLOCK_ELEMENT_TAGS =
        new String[] {TAG_P, TAG_UL, TAG_OL, TAG_H1, TAG_H2, TAG_H3, TAG_H4, TAG_H5, TAG_H6, TAG_TABLE};

    /**
     * Sort the block elements tag name array.
     */
    static {
        Arrays.sort(BLOCK_ELEMENT_TAGS);
    }

    @Override
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        List<Element> lineBreaksToReplace =
            filterDescendants(document.getDocumentElement(), new String[] {TAG_BR}, new ElementSelector()
            {
                @Override
                public boolean isSelected(Element element)
                {
                    Node prev = findPreviousNode(element);
                    Node next = findNextNode(element);
                    return !(null == prev && null == next) && (isBlockElement(prev) || isBlockElement(next));
                }
            });
        for (Element lineBreak : lineBreaksToReplace) {
            Node parent = lineBreak.getParentNode();
            Element element = document.createElement(TAG_DIV);
            element.setAttribute(ATTRIBUTE_CLASS, "wikimodel-emptyline");
            parent.insertBefore(element, lineBreak);
            parent.removeChild(lineBreak);
        }
    }

    /**
     * Finds the previous sibling of the given element which is not a {@code <br/>}, an empty text node or a comment
     * node.
     *
     * @param element the element to be analysed.
     * @return previous sibling of the given element which is not a html line-break, an empty text node or a comment
     *         node.
     */
    private Node findPreviousNode(Element element)
    {
        Node prev = element.getPreviousSibling();
        while (prev != null && (isLineBreak(prev) || isEmptyTextNode(prev) || isCommentNode(prev))) {
            prev = prev.getPreviousSibling();
        }
        return prev;
    }

    /**
     * Finds the next sibling of the given element which is not a {@code <br/>}, an empty text node or a comment node.
     *
     * @param element the element to be analysed.
     * @return next sibling of the given element which is not a html line-break, an empty text node or a comment node.
     */
    private Node findNextNode(Element element)
    {
        Node next = element.getNextSibling();
        while (next != null && (isLineBreak(next) || isEmptyTextNode(next) || isCommentNode(next))) {
            next = next.getNextSibling();
        }
        return next;
    }

    /**
     * Check whether the given node represents a block element.
     * 
     * @param node the node to be checked.
     * @return true if the node represents a block element.
     */
    private boolean isBlockElement(Node node)
    {
        boolean isBlockElement = false;
        if (null != node) {
            for (String blockElement : BLOCK_ELEMENT_TAGS) {
                isBlockElement = blockElement.equals(node.getNodeName()) ? true : isBlockElement;
            }
        }
        return isBlockElement;
    }

    /**
     * Checks if a node represents empty text content (white space).
     * 
     * @param node the {@link Node}.
     * @return true if the node represents white space.
     */
    private boolean isEmptyTextNode(Node node)
    {
        return null != node && node.getNodeType() == Node.TEXT_NODE && node.getTextContent().trim().equals("");
    }

    /**
     * Checks if a node represents an html comment.
     * 
     * @param node the {@link Node}.
     * @return true if the node is a comment node.
     */
    private boolean isCommentNode(Node node)
    {
        return null != node && node.getNodeType() == Node.COMMENT_NODE;
    }

    /**
     * Checks if a node represents an html line break.
     * 
     * @param node the {@link Node}
     * @return true of the node represents a line break.
     */
    private boolean isLineBreak(Node node)
    {
        return null != node && node.getNodeName().equals(TAG_BR);
    }
}
