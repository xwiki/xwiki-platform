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

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * Transform non XHTML list into XHTML valid lists. Specifically, move &lt;ul&gt; and &lt;ol&gt; elements (and any other
 * nodes that are not allowed) nested inside a &lt;ul&gt; or &lt;ol&gt; element inside the previous &lt;li&gt; element.
 * <p>
 * For example: <code><pre>
 *   &lt;ul&gt;
 *     &lt;li&gt;item1&lt;/li&gt;
 *     &lt;ul&gt;
 *       &lt;li&gt;item2&lt;/li&gt;
 *     &lt;/ul&gt;
 *   &lt;/ul&gt;
 * </pre></code> becomes <code><pre>
 *   &lt;ul&gt;
 *     &lt;li&gt;item1
 *       &lt;ul&gt;
 *         &lt;li&gt;item2&lt;/li&gt;
 *       &lt;/ul&gt;
 *     &lt;/li&gt;
 *   &lt;/ul&gt;
 * </pre></code>
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Component("list")
public class ListFilter extends AbstractHTMLFilter
{

    /**
     * {@inheritDoc}
     * <p>
     * The {@link ListFilter} does not use any cleaningParameters passed in.
     */
    public void filter(Document document, Map<String, String> cleaningParameters)
    {
        // Iterate all lists and fix them.
        for (Element list : filterDescendants(document.getDocumentElement(), new String[] {TAG_UL, TAG_OL})) {
            filter(list);
        }
    }

    /**
     * Transforms the given list in a valid XHTML list by moving the nodes that are not allowed inside &lt;ul&gt; and
     * &lt;ol&gt; in &lt;li&gt; elements.
     * 
     * @param list the list to be filtered
     */
    private void filter(Element list)
    {
        // Iterate all the child nodes of the given list to see who's allowed and who's not allowed inside it.
        Node child = list.getFirstChild();
        Node previousListItem = null;
        while (child != null) {
            Node nextSibling = child.getNextSibling();
            if (isAllowedInsideList(child)) {
                // Save a reference to the previous list item. Note that the previous list item is not necessarily the
                // previous sibling.
                if (child.getNodeName().equalsIgnoreCase(TAG_LI)) {
                    previousListItem = child;
                }
            } else {
                if (previousListItem == null) {
                    // Create a new list item to be able to move the invalid child.
                    previousListItem = list.getOwnerDocument().createElement(TAG_LI);
                    list.insertBefore(previousListItem, child);
                    // Hide the marker of the list item to make the list look the same after it is cleaned.
                    ((Element) previousListItem).setAttribute(ATTRIBUTE_STYLE, "list-style-type: none");
                }
                // Move the child node at the end of the previous list item because it is not allowed where it is now.
                previousListItem.appendChild(child);
            }
            child = nextSibling;
        }
    }

    /**
     * Checks if a given node is allowed or not as a child of a &lt;ul&gt; or &lt;ol&gt; element.
     * 
     * @param node the node to be checked
     * @return {@code true} if the given node is allowed inside an ordered or unordered list, {@code false} otherwise
     */
    private boolean isAllowedInsideList(Node node)
    {
        return (node.getNodeType() != Node.ELEMENT_NODE || node.getNodeName().equalsIgnoreCase(TAG_LI))
            && (node.getNodeType() != Node.TEXT_NODE || node.getNodeValue().trim().length() == 0);
    }
}
