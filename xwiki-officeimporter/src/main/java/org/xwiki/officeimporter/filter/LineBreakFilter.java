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
package org.xwiki.officeimporter.filter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Replaces {@code<br/>} elements placed in between block elements with {@code<div class="wikikmodel-emptyline"/>}.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class LineBreakFilter implements HTMLFilter
{
    /**
     * List of block element tag names.
     */
    private static final String[] BLOCK_ELEMENT_TAGS =
        new String[] {"p", "ul", "ol", "hr", "h1", "h2", "h3", "h4", "h5", "h6", "table"};

    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        NodeList lineBreaks = document.getElementsByTagName("br");
        List<Node> lineBreaksToReplace = new ArrayList<Node>();
        for (int i = 0; i < lineBreaks.getLength(); i++) {
            Node lineBreak = lineBreaks.item(i);
            Node prev = lineBreak.getPreviousSibling();
            while (prev != null && (isLineBreak(prev) || isEmptyTextNode(prev) || isCommentNode(prev))) {
                prev = prev.getPreviousSibling();
            }
            Node next = lineBreak.getNextSibling();
            while (next != null && (isLineBreak(next) || isEmptyTextNode(next) || isCommentNode(next))) {
                next = next.getNextSibling();
            }
            boolean shouldReplace = !(null == prev && null == next) && (isBlockElement(prev) || isBlockElement(next));
            if (shouldReplace) {
                lineBreaksToReplace.add(lineBreak);
            }
        }
        for (Node lineBreak : lineBreaksToReplace) {
            Node parent = lineBreak.getParentNode();
            Element element = document.createElement("div");
            element.setAttribute("class", "wikimodel-emptyline");
            parent.insertBefore(element, lineBreak);
            parent.removeChild(lineBreak);
        }
    }

    /**
     * Check whether the given node represents a block element.
     * 
     * @param element the {@link Node}.
     * @return true if the node represents a block element.
     */
    public boolean isBlockElement(Node node)
    {
        boolean isBlockElement = false;
        if (null != node) {
            for (String blockElementTag : BLOCK_ELEMENT_TAGS) {
                isBlockElement = node.getNodeName().equals(blockElementTag) ? true : isBlockElement;
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
        return null != node && node.getNodeName().equals("br");
    }
}
