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
        for (int i = 0; i < links.getLength(); i++) {
            if (links.item(i) instanceof Element) {
                Element link = (Element) links.item(i);
                String anchorName = link.getAttribute("name");
                boolean isAnchor = !anchorName.equals("");
                if (isAnchor) {
                    Node parent = link.getParentNode();
                    Comment beforeComment =
                        document.createComment(String.format("startmacro:id|-|name=\"%s\"|-|", anchorName));
                    Comment afterComment = document.createComment("stopmacro");
                    parent.insertBefore(beforeComment, link);
                    parent.insertBefore(afterComment, link.getNextSibling());
                }
            }
        }
    }
}
