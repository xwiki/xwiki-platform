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

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;
import org.xwiki.xml.html.filter.ElementSelector;

/**
 * This filter is used to remove those tags that doesn't play any role with the representation of information. This type
 * of tags can result from other filters (like the style filter) or Open Office specific formatting choices (like
 * newlines being represented by empty paragraphs). For an example, empty {@code <span/>} or {@code <div/>} tags will be
 * ripped off within this filter.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/redundancy")
public class RedundancyFilter extends AbstractHTMLFilter
{
    /**
     * List of those tags which will be filtered if no attributes are present.
     */
    private static final String[] FILTERED_IF_NO_ATTRIBUTES_TAGS = new String[] {TAG_SPAN, TAG_DIV};

    /**
     * List of those tags which will be filtered if no textual content is present inside them.
     */
    private static final String[] FILTERED_IF_NO_CONTENT_TAGS = new String[] {
        TAG_EM, TAG_STRONG, TAG_DFN, TAG_CODE, TAG_SAMP, TAG_KBD, TAG_VAR, TAG_CITE, TAG_ABBR,
        TAG_ACRONYM, TAG_ADDRESS, TAG_BLOCKQUOTE, TAG_Q, TAG_PRE, TAG_H1, TAG_H2, TAG_H3, TAG_H4, TAG_H5, TAG_H6};

    @Override
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        List<Element> elementsWithNoAttributes =
            filterDescendants(document.getDocumentElement(), FILTERED_IF_NO_ATTRIBUTES_TAGS, new ElementSelector()
            {
                @Override
                public boolean isSelected(Element element)
                {
                    return !element.hasAttributes();
                }
            });
        for (Element element : elementsWithNoAttributes) {
            replaceWithChildren(element);
        }
        List<Element> elementsWithNoContent =
            filterDescendants(document.getDocumentElement(), FILTERED_IF_NO_CONTENT_TAGS, new ElementSelector()
            {
                @Override
                public boolean isSelected(Element element)
                {
                    return element.getTextContent().trim().equals("");
                }
            });
        for (Element element : elementsWithNoContent) {
            String textContent = element.getTextContent();
            if (textContent.equals("")) {
                element.getParentNode().removeChild(element);
            } else {
                element.setTextContent(textContent.replaceAll(" ", "&nbsp;"));
            }
        }
    }
}
