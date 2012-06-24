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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * <p>
 * This filter includes a temporary fix for the JIRA: http://jira.xwiki.org/jira/browse/XWIKI-3091
 * </p>
 * <p>
 * Replaces xhtml anchors like {@code<a name="name"></a>} with xwiki compatible anchors like
 * {@code<!--startmacro:id|-|name="name"|-|--><a name="name"></a><!--stopmacro-->}.
 * </p>
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/anchor")
public class AnchorFilter extends AbstractHTMLFilter
{
    @Override
    public void filter(Document document, Map<String, String> cleaningParameters)
    {
        List<Element> links = filterDescendants(document.getDocumentElement(), new String[] {TAG_A});
        List<Element> anchorsToRemove = new ArrayList<Element>();
        List<Element> anchorsTofix = new ArrayList<Element>();
        for (Element link : links) {
            if (isAnchor(link)) {
                // OO server generates html content like:
                // <body><a name="table1"><h1>Sheet 1: <em>Hello</em></h1></a><body>
                // And the html cleaner converts this invalid xhtml into following xhtml:
                // <p><a name="table1"/></p><h1><a name=\"table1\">Sheet 1: <em>Hello</em></a></h1>
                // We need to make sure that the duplicate anchor inside the <h1> tag is removed
                Node heading = link.getParentNode();
                Node paragraph = (heading != null) ? heading.getPreviousSibling() : null;
                Node originalAnchor =
                    (paragraph instanceof Element) ? ((Element) paragraph).getElementsByTagName(TAG_A).item(0) : null;
                if (isSameAnchor(link, originalAnchor)) {
                    // Means this anchor was a result of close-before-copy-inside operation of default html cleaner.
                    anchorsToRemove.add(link);
                } else {
                    anchorsTofix.add(link);
                }
            }
        }
        for (Element anchor : anchorsToRemove) {
            replaceWithChildren(anchor);
        }
        for (Element anchor : anchorsTofix) {
            fixAnchor(document, anchor);
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
            isAnchor = !element.getAttribute(ATTRIBUTE_NAME).equals("");
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
            isSameAnchor = firstElement.getAttribute(ATTRIBUTE_NAME).equals(secondElement.getAttribute(ATTRIBUTE_NAME));
        }
        return isSameAnchor;
    }

    /**
     * Converts the given anchor into xwiki conpatible xhtml.
     * 
     * @param document the {@link Document}.
     * @param anchor the {@link Node} which is to be converted.
     */
    private void fixAnchor(Document document, Node anchor)
    {
        Node parent = anchor.getParentNode();
        String anchorName = ((Element) anchor).getAttribute(ATTRIBUTE_NAME);
        Comment beforeComment = document.createComment(String.format("startmacro:id|-|name=\"%s\"|-|", anchorName));
        Comment afterComment = document.createComment("stopmacro");
        parent.insertBefore(beforeComment, anchor);
        parent.insertBefore(afterComment, anchor.getNextSibling());
    }
}
