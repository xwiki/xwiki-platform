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
 * This filter is used to rip-off or modify HTML tables so that they can be rendered properly.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/table")
public class TableFilter extends AbstractHTMLFilter
{
    @Override
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
        // Strip off empty table rows. See http://jira.xwiki.org/jira/browse/XWIKI-3136.
        List<Element> emptyRows =
            filterDescendants(document.getDocumentElement(), new String[] {TAG_TR}, new ElementSelector()
            {
                @Override
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
