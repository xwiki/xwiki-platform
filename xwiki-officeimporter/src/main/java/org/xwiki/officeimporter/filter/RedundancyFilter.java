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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.officeimporter.filter.common.AbstractHTMLFilter;

/**
 * This filter is used to remove those tags that doesn't play any role with the representation of
 * information. This type of tags can result from other filters (like the style filter) or
 * Open Office specific formatting choices (like newlines being represented by empty paragraphs). For
 * an example, empty {@code <span>} or {@code <div>} tags will be ripped off within this filter.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class RedundancyFilter extends AbstractHTMLFilter
{
    /**
     * List of those tags which will be filtered if no attributes are present.
     */
    private static final String[] ATTRIBUTES_FILTERED_TAGS = new String[] {"span", "div"};

    /**
     * List of those tags which will be filtered if no textual content is present inside them.
     */
    private static final String[] CONTENT_FILTERED_TAGS =
        new String[] {"em", "strong", "dfn", "code", "samp", "kbd", "var", "cite", "abbr",
        "acronym", "address", "blockquote", "q", "pre", "h1", "h2", "h3", "h4", "h5", "h6"};
    
    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        for (String key : ATTRIBUTES_FILTERED_TAGS) {
            filterNodesWithZeroAttributes(filterDescendants(document.getDocumentElement(), key));
            
        }
        for (String key : CONTENT_FILTERED_TAGS) {
            filterNodesWithEmptyTextContent(filterDescendants(document.getDocumentElement(), key));
        }
    }

    /**
     * Scan the given list of elements and strip those elements that doesn't have any attributes
     * set. The children elements of such elements will be moved one level up.
     * 
     * @param textElements List of elements to be examined.
     */
    private void filterNodesWithZeroAttributes(List<Element> textElements)
    {
        for (Element textElement : textElements) {
            if (!textElement.hasAttributes()) {
                NodeList children = textElement.getChildNodes();
                while (children.getLength() > 0) {
                    textElement.getParentNode().insertBefore(children.item(0), textElement);
                }
                textElement.getParentNode().removeChild(textElement);
            }
        }
    }

    /**
     * Scan the given list of elements and strip those elements that doesn't have any textual
     * content inside them.
     * 
     * @param textElements List of elements to be examined.
     */
    private void filterNodesWithEmptyTextContent(List<Element> textElements)
    {
        for (Element textElement : textElements) {
            Node parent = textElement.getParentNode();
            String textContent = textElement.getTextContent();
            if (textContent.equals("")) {
                parent.removeChild(textElement);
            } else if (textContent.trim().equals("")) {
                textElement.setTextContent(textContent.replaceAll(" ", "&nbsp;"));
            }
        }
    }
}
