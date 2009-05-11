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
package org.xwiki.xml.html;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * HTML Utility methods.
 * 
 * @version $Id$
 * @since 1.8.3
 */
public final class HTMLUtils
{
    /**
     * Private constructor since this is a utility class that shouldn't be instantiated (all methods are static).
     */
    private HTMLUtils()
    {
        // Nothing to do
    }

    /**
     * Strip the HTML envelope if it exists. Precisely this means removing the head tag and move all tags in the body
     * tag directly under the html element. This is useful for example if you wish to insert an HTML fragment into an
     * existing HTML page.
     * 
     * @param document the w3c Document to strip
     */
    public static void stripHTMLEnvelope(Document document)
    {
        org.w3c.dom.Element root = document.getDocumentElement();
        if (root.getNodeName().equalsIgnoreCase(HTMLConstants.TAG_HTML)) {
            // Look for a head element below the root element and for a body element
            Node bodyNode = null;
            Node headNode = null;
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equalsIgnoreCase(HTMLConstants.TAG_HEAD)) {
                    headNode = node;
                } else if (node.getNodeName().equalsIgnoreCase(HTMLConstants.TAG_BODY)) {
                    bodyNode = node;
                }
            }

            if (headNode != null) {
                root.removeChild(headNode);
            }

            if (bodyNode != null) {
                // Move all children of body node under the root element
                NodeList bodyChildrenNodes = bodyNode.getChildNodes();
                while (bodyChildrenNodes.getLength() > 0) {
                    root.insertBefore(bodyChildrenNodes.item(0), null);
                }
                root.removeChild(bodyNode);
            }
        }
    }

    /**
     * Remove the first element inside a parent element and copy the element's children 
     * in the parent.
     * 
     * @param document the w3c document from which to remove the top level paragraph
     * @param parentTagName the name of the parent tag to look under
     * @param elementTagName the name of the first element to remove
     */
    public static void stripFirstElementInside(Document document, String parentTagName, String elementTagName)
    {
        NodeList parentNodes = document.getElementsByTagName(parentTagName);
        if (parentNodes.getLength() > 0) {
            Node parentNode = parentNodes.item(0);
            // Look for a p element below the first parent element
            Node pNode = parentNode.getFirstChild();
            if (elementTagName.equalsIgnoreCase(pNode.getNodeName())) {
                // Move all children of p node under the root element
                NodeList pChildrenNodes = pNode.getChildNodes();
                while (pChildrenNodes.getLength() > 0) {
                    parentNode.insertBefore(pChildrenNodes.item(0), null);
                }
                parentNode.removeChild(pNode);
            }
        }
    }
}
