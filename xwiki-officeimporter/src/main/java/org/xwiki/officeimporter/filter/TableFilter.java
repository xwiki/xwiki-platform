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

import java.util.Arrays;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Presently xwiki rendering module doesn't support complex table cell items. This filter is used to
 * rip-off or modify html tables so that they can be rendered properly. The corresponding JIRA issue
 * is located at http://jira.xwiki.org/jira/browse/XWIKI-2804.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class TableFilter implements HTMLFilter
{
    /**
     * Tags that need to be removed from cell items while preserving there children.
     */
    private static final String[] FILTER_TAGS = new String[] {"p"};

    /**
     * Tags that need to be completely removed from cell items.
     */
    private static final String[] REMOVE_TAGS = new String[] {"br"};

    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        NodeList cellItems = document.getElementsByTagName("td");
        for (int i = 0; i < cellItems.getLength(); i++) {
            Node cellItem = cellItems.item(i);
            cleanNode(cellItem);
        }
    }

    /**
     * Cleans this particular node.
     * 
     * @param node Node to be cleaned.
     * @return True if this node was ripped off.
     */
    private boolean cleanNode(Node node)
    {
        Node parent = node.getParentNode();
        NodeList children = node.getChildNodes();
        boolean removed = false;
        if (node.getNodeType() == Node.TEXT_NODE) {
            String trimmedContent = node.getTextContent().trim();
            if (trimmedContent.equals("")) {
                parent.removeChild(node);
                removed = true;
            } else {
                node.setTextContent(trimmedContent);
            }
        } else if (Arrays.binarySearch(FILTER_TAGS, node.getNodeName()) >= 0) {
            while (children.getLength() > 0) {
                Node child = children.item(0);
                parent.insertBefore(children.item(0), node);
                cleanNode(child);
            }
            parent.removeChild(node);
            removed = true;
        } else if (Arrays.binarySearch(REMOVE_TAGS, node.getNodeName()) >= 0) {
            parent.removeChild(node);
            removed = true;
        } else {
            for (int i = 0; i < children.getLength(); i++) {
                if (cleanNode(children.item(i))) {
                    --i;
                }
            }
        }
        return removed;
    }
}
