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
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;
import org.xwiki.xml.html.filter.ElementSelector;

/**
 * Presently xwiki rendering module doesn't support complex table cell items. This filter is used to rip-off or modify
 * html tables so that they can be rendered properly. The corresponding JIRA issue is located at
 * http://jira.xwiki.org/jira/browse/XWIKI-2804.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/table")
public class TableFilter extends AbstractHTMLFilter
{
    /**
     * If these tags are found within a cell item (td), cell's content will be considered as an embedded document.
     */
    private static final String[] FILTER_TAGS =
        new String[] {TAG_P, TAG_H1, TAG_H2, TAG_H3, TAG_H4, TAG_H5, TAG_H6, TAG_BR, TAG_UL, TAG_OL, TAG_IMG, TAG_TABLE};

    /**
     * {@inheritDoc}
     */
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        // Remove isolated paragraphs inside cell items / table header items.
        List<Element> tableCells = filterDescendants(document.getDocumentElement(), new String[] {TAG_TD, TAG_TH});
        for (Element cell : tableCells) {
            List<Element> paragraphs = filterChildren(cell, TAG_P);
            if (paragraphs.size() == 1) {
                replaceWithChildren(paragraphs.get(0));
            }
        }
        // Filter resulting cell content.
        tableCells = filterDescendants(document.getDocumentElement(), new String[] {TAG_TD, TAG_TH});
        for (Element cell : tableCells) {
            List<Element> dangerTags = filterDescendants(cell, FILTER_TAGS);
            if (!dangerTags.isEmpty()) {
                Element div = document.createElement(TAG_DIV);
                div.setAttribute(ATTRIBUTE_CLASS, "xwiki-document");
                moveChildren(cell, div);
                cell.appendChild(div);
            }
        }
        // Strip off empty table rows. see http://jira.xwiki.org/jira/browse/XWIKI-3136.
        List<Element> emptyRows =
            filterDescendants(document.getDocumentElement(), new String[] {TAG_TR}, new ElementSelector()
            {
                public boolean isSelected(Element element)
                {
                    return element.getChildNodes().getLength() == 0;
                }
            });
        for (Element emptyRow : emptyRows) {
            emptyRow.getParentNode().removeChild(emptyRow);
        }
        // Remove problematic rowspan attributes.
        List<Element> rows = filterDescendants(document.getDocumentElement(), new String[] {TAG_TR});
        for (Element row : rows) {
            List<Element> childCells = filterDescendants(row, new String[] {TAG_TD});
            if (hasAttribute(childCells, ATTRIBUTE_ROWSPAN, true)) {
                for (Element childCell : childCells) {
                    childCell.removeAttribute(ATTRIBUTE_ROWSPAN);
                }
            }
        }
    }
}
