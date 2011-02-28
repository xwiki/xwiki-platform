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
package org.xwiki.xml.internal.html.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.HTMLConstants;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * Wraps direct children of the Body tag with paragraphs. For example {@code a <table>...</table> b <p>c</p> d} is
 * transformed into {@link <p>a </p><table>...</table><p> b </p><p>c</p><p> d</p>}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@Component("body")
public class BodyFilter extends AbstractHTMLFilter
{
    /**
     * List of valid children elements of the BODY element in XHTML. 
     */
    private static final List<String> ALLOWED_BODY_TAGS = Arrays.asList(HTMLConstants.TAG_ADDRESS, 
        HTMLConstants.TAG_BLOCKQUOTE, HTMLConstants.TAG_DEL, HTMLConstants.TAG_DIV, HTMLConstants.TAG_FIELDSET,  
        HTMLConstants.TAG_FORM, HTMLConstants.TAG_HR, HTMLConstants.TAG_INS, HTMLConstants.TAG_NOSCRIPT, 
        HTMLConstants.TAG_P, HTMLConstants.TAG_PRE, HTMLConstants.TAG_SCRIPT, HTMLConstants.TAG_TABLE,
        HTMLConstants.TAG_H1, HTMLConstants.TAG_H2, HTMLConstants.TAG_H3, HTMLConstants.TAG_H4, HTMLConstants.TAG_H5,
        HTMLConstants.TAG_H6, HTMLConstants.TAG_DL, HTMLConstants.TAG_OL, HTMLConstants.TAG_UL);
    
    /**
     * {@inheritDoc}
     * @see AbstractHTMLFilter#filter(Document, Map)
     */
    public void filter(Document document, Map<String, String> cleaningParameters)
    {
        Node body = document.getElementsByTagName(HTMLConstants.TAG_BODY).item(0);
        Node currentNode = body.getFirstChild();
        Node markerNode = null;
        boolean containsOnlySpaces = true;
        while (currentNode != null) {
            // Note: We ignore comment nodes since there's no need to wrap them.
            if (currentNode.getNodeType() != Node.COMMENT_NODE) {
                if (!ALLOWED_BODY_TAGS.contains(currentNode.getNodeName())) {
                    
                    // Ensure that we don't wrap elements that contain only spaces or newlines.
                    containsOnlySpaces = containsOnlySpaces(currentNode);
    
                    if (markerNode == null) {
                        markerNode = currentNode;
                    } else {
                        // Do nothing, just go to the next node.
                    }
                } else if (markerNode != null) {
                    // surround all the nodes starting with the marker node with a paragraph unless there are only
                    // whitespaces or newlines.
                    if (!containsOnlySpaces) {
                        surroundWithParagraph(document, body, markerNode, currentNode);
                    }
                    markerNode = null;
                }
            }
            currentNode = currentNode.getNextSibling();
        }
        
        // If the marker is still set it means we need to wrap all elements between the marker till
        // the end of the body siblings with a paragraph.
        if (markerNode != null && !containsOnlySpaces) {
            surroundWithParagraph(document, body, markerNode, null);
        }
    }

    /**
     * @param currentNode the current node to check
     * @return false if the current node contains something other than whitespaces or newlines, true otherwise
     */
    private boolean containsOnlySpaces(Node currentNode)
    {
        boolean result = true;
        if (currentNode.getNodeType() == Node.TEXT_NODE) {
            Text textNode = (Text) currentNode;
            if (textNode.getNodeValue().trim().length() > 0) {
                result = false;
            }
        } else if (currentNode.getNodeType() != Node.COMMENT_NODE) {
            result = false;
        }
        return result;
    }
    
    /**
     * Surround passed nodes with a paragraph element.
     * 
     * @param document the document to use to create the new paragraph element
     * @param body the body under which to wrap non valid elements with paragraphs
     * @param beginNode the first node where to start the wrapping
     * @param endNode the last node where to stop the wrapping. If null then the wrapping is done till the 
     *        last element inside the body element
     */
    private void surroundWithParagraph(Document document, Node body, Node beginNode, Node endNode)
    {
        // surround all the nodes starting with the marker node with a paragraph.
        Element paragraph = document.createElement(TAG_P);
        body.insertBefore(paragraph, beginNode);
        Node child = beginNode;
        while (child != endNode) {
            Node nextChild = child.getNextSibling();
            paragraph.appendChild(body.removeChild(child));
            child = nextChild; 
        }
    }
}
