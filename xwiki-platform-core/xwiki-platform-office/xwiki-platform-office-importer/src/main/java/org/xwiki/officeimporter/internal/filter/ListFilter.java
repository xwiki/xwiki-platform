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

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * <p>
 * This filter includes a temporary fix for the JIRA: http://jira.xwiki.org/jira/browse/XWIKI-3262
 * </p>
 * <p>
 * Removes isolated paragraph items from list items.
 * </p>
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/list")
public class ListFilter extends AbstractHTMLFilter
{
    @Override
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        List<Element> listItems = filterDescendants(document.getDocumentElement(), new String[] {TAG_LI});
        for (Element listItem : listItems) {
            Node nextChild = listItem.getFirstChild();
            while (nextChild != null) {
                if (nextChild.getNodeType() == Node.TEXT_NODE) {
                    String trimmed = StringUtils.stripStart(nextChild.getTextContent(), WHITE_SPACE_CHARS);
                    nextChild.setTextContent(trimmed);
                    if (trimmed.equals("")) {
                        nextChild = nextChild.getNextSibling();
                        continue;
                    }
                } else if (nextChild.getNodeName().equals(TAG_P)) {
                    replaceWithChildren((Element) nextChild);
                }
                break;
            }
        }
    }
}
