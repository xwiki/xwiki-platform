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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This filter is used to remove those tags that doesn't play any role with the representation of
 * information. This type of tags can result from other filters (like the style filter) or
 * openoffice specific formatting choices (like newlines being represented by empty paragraphs). For
 * an example, empty {@code <span>} or {@code <div>} tags will be ripped off within this filter.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class RedundancyFilter implements HTMLFilter
{
    /**
     * List of those tags which will be filtered if no attributes are present.
     */
    private final String[] attributeWiseFilteredTags = new String[] {"span", "div"};

    /**
     * List of those tags which will be filtered if no textual content is present inside them.
     */
    private final String[] contentWiseFilteredTags =
        new String[] {"em", "strong", "dfn", "code", "samp", "kbd", "var", "cite", "abbr",
        "acronym", "address", "blockquote", "q", "pre", "h1", "h2", "h3", "h4", "h5", "h6"};

    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        for (String key : attributeWiseFilteredTags) {
            filterNodesWithZeroAttributes(document.getElementsByTagName(key));
        }
        for (String key : contentWiseFilteredTags) {
            filterNodesWithEmptyTextContent(document.getElementsByTagName(key));
        }
    }

    /**
     * Scan the given list of elements and strip those elements that doesn't have any attributes
     * set. The children elements of such elements will be moved one level up.
     * 
     * @param elements List of elements to be examined.
     */
    private void filterNodesWithZeroAttributes(NodeList elements)
    {
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (!element.hasAttributes()) {
                NodeList children = element.getChildNodes();
                while (children.getLength() > 0) {
                    element.getParentNode().insertBefore(children.item(0), element);
                }
                element.getParentNode().removeChild(element);
                i--;
            }
        }
    }

    /**
     * Scan the given list of elements and strip those elements that doesn't have any textual
     * content inside them.
     * 
     * @param elements List of elements to be examined.
     */
    private void filterNodesWithEmptyTextContent(NodeList elements)
    {
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (element.getTextContent().trim().equals("")) {
                element.getParentNode().removeChild(element);
                i--;
            }
        }
    }
}
