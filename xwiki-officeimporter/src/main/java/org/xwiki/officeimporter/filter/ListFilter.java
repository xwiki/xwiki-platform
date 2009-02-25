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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * Presently xwiki rendering module doesn't support complex list items. Because of this reason this
 * temporary filter is used to rip off any complex formatting elements present in html lists. The
 * JIRA issue is located at http://jira.xwiki.org/jira/browse/XWIKI-2812.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class ListFilter extends AbstractHTMLFilter
{
    /**
     * {@inheritDoc}
     */
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        NodeList listItems = document.getElementsByTagName("li");
        for (int i = 0; i < listItems.getLength(); i++) {
            Node listItem = listItems.item(i);
            Node counter = listItem.getFirstChild();
            while (counter != null) {
                if (counter.getNodeType() == Node.TEXT_NODE) {
                    String trimmed = StringUtils.stripStart(counter.getTextContent(), WHITE_SPACE_CHARS);
                    counter.setTextContent(trimmed);
                    if (trimmed.equals("")) {
                        counter = counter.getNextSibling();
                        continue;
                    }
                } else if (counter.getNodeName().equals("p")) {
                    NodeList children = counter.getChildNodes();
                    while (children.getLength() > 0) {
                        listItem.insertBefore(children.item(0), counter);
                    }
                    listItem.removeChild(counter);
                }
                break;
            }
        }
    }
}
