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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The purpose of this filter is to replace xhtml anchors like {@code<a name="name"></a>} with xwiki compatible links
 * like {@code<!--startmacro:id|-|name="name"|-|--><a name="name"></a><!--stopmacro-->}.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class AnchorFilter implements HTMLFilter
{
    /**
     * {@inheritDoc}
     */
    public void filter(Document document)
    {
        NodeList links = document.getElementsByTagName("a");
        List<Node> anchorsToRemove = new ArrayList<Node>();
        List<Node> anchorsToReplace = new ArrayList<Node>();
        for (int i = 0; i < links.getLength(); i++) {
            Node link = links.item(i);
            if (link instanceof Element) {
                if (isAnchor(link)) {
                    Node parent = link.getParentNode();
                    if (isSameAnchor(link, parent.getPreviousSibling())) {
                        // Means this anchor was a result of close-before-copy-inside operation of default html cleaner.
                        anchorsToRemove.add(link);
                    } else {
                        anchorsToReplace.add(link);
                    }
                }
            }
        }
        for (Node anchor : anchorsToRemove) {
            Node parent = anchor.getParentNode();
            while (null != anchor.getFirstChild()) {
                Node child = anchor.removeChild(anchor.getFirstChild());
                parent.insertBefore(child, anchor);
            }
            parent.removeChild(anchor);
        }
        for (Node anchor : anchorsToReplace) {
            replaceAnchor(document, anchor);
        }
    }

    /**
     * Checks whether the given node represents an html anchor.
     * 
     * @param node the {@link Node}
     * @return true if the node represents an anchor.
     */
    private boolean isAnchor(Node node)
    {
        boolean isAnchor = false;
        if (null != node && node instanceof Element) {
            Element element = (Element) node;
            isAnchor = !element.getAttribute("name").equals("");
        }
        return isAnchor;
    }

    /**
     * Checks whether the given two nodes represents the same anchor.
     * 
     * @param first first {@link Node}
     * @param second second {@link Node}
     * @return true if both of the nodes are anchors and have the same 'name' attribute.
     */
    private boolean isSameAnchor(Node first, Node second)
    {
        boolean isSameAnchor = false;
        if (isAnchor(first) && isAnchor(second)) {
            Element firstElement = (Element) first;
            Element secondElement = (Element) second;
            isSameAnchor = firstElement.getAttribute("name").equals(secondElement.getAttribute("name"));
        }
        return isSameAnchor;
    }

    /**
     * Converts the given anchor into xwiki conpatible xhtml.
     * 
     * @param document the {@link Document}.
     * @param anchor the {@link Node} which is to be converted.
     */
    private void replaceAnchor(Document document, Node anchor)
    {
        Node parent = anchor.getParentNode();
        String anchorName = ((Element) anchor).getAttribute("name");
        Comment beforeComment = document.createComment(String.format("startmacro:id|-|name=\"%s\"|-|", anchorName));
        Comment afterComment = document.createComment("stopmacro");
        parent.insertBefore(beforeComment, anchor);
        parent.insertBefore(afterComment, anchor.getNextSibling());
    }
}
