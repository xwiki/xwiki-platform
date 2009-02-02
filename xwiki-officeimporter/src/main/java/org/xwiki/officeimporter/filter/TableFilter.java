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
import org.xwiki.officeimporter.filter.common.AbstractHTMLFilter;
import org.xwiki.officeimporter.filter.common.ElementFilterCriterion;

/**
 * Presently xwiki rendering module doesn't support complex table cell items. This filter is used to rip-off or modify
 * html tables so that they can be rendered properly. The corresponding JIRA issue is located at
 * http://jira.xwiki.org/jira/browse/XWIKI-2804.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class TableFilter extends AbstractHTMLFilter
{
    /**
     * Tags that need to be removed from cell items while preserving there children.
     */
    private static final String[] REPLACE_TAGS = new String[] {TAG_P};

    /**
     * Tags that need to be completely removed from cell items.
     */
    private static final String[] STRIP_TAGS = new String[] {TAG_BR};

    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        // Clean table cell items first.
        List<Element> cells = filterDescendants(document.getDocumentElement(), TAG_TD);
        for (Element cell : cells) {
            cleanCell(cell);
        }
        // Strip off empty table rows. see http://jira.xwiki.org/jira/browse/XWIKI-3136.
        List<Element> emptyRows = filterDescendants(document.getDocumentElement(), TAG_TR, new ElementFilterCriterion()
        {
            public boolean isFiltered(Element element)
            {
                return element.getChildNodes().getLength() == 0;
            }
        });
        stripElements(emptyRows);
        // Remove problematic rowspan attributes.
        List<Element> rows = filterDescendants(document.getDocumentElement(), TAG_TR);
        for (Element row : rows) {
            List<Element> childCells = filterDescendants(row, TAG_TD);
            if (hasAttribute(childCells, ATT_ROWSPAN, true)) {
                stripAttribute(childCells, ATT_ROWSPAN);
            }
        }
    }

    /**
     * Cleans this particular table cell by stripping off / replacing those tags which are not yet supported inside
     * table cell items in xwiki 2.0 syntax.
     * 
     * @param cell The {@link Element} representing a table cell.
     */
    private void cleanCell(Element cell)
    {
        for (String stripTagName : STRIP_TAGS) {
            List<Element> stripTags = filterDescendants(cell, stripTagName);
            stripElements(stripTags);
        }
        for (String replaceTagName : REPLACE_TAGS) {
            List<Element> replaceTags = filterDescendants(cell, replaceTagName);
            replaceWithChildren(replaceTags);
        }
    }
}
