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
package org.xwiki.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.jdom.input.DOMBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * XML Utility methods.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public final class XMLUtils
{
    /**
     * Private constructor since this is a utility class that shouldn't be instantiated (all
     * methods are static).
     */
    private XMLUtils()
    {
        // Nothing to do
    }
    /**
     * @param document the W3C Document to transform into a String
     * @return the XML as a String
     */
    public static String toString(Document document)
    {
        // Note: We don't use javax.xml.transform.Transformer since it prints our valid XHTML as HTML which is not
        // XHTML compliant. For example it transforms our "<hr/>" into "<hr>.
        DOMBuilder builder = new DOMBuilder();
        org.jdom.Document jdomDoc = builder.build(document);

        Format format = Format.getRawFormat();
        // Force newlines to use \n since otherwise the default is \n\r.
        // See http://www.jdom.org/docs/apidocs/org/jdom/output/Format.html#setLineSeparator(java.lang.String)
        format.setLineSeparator("\n");

        XMLOutputter outputter = new XMLOutputter(format);
        return outputter.outputString(jdomDoc);
    }

    /**
     * Strip the HTML envelope if it exists. Precisely this means removig the head tag and move all tags in
     * the body tag directly under the html element. This is useful for example if you wish to insert an HTML
     * fragment into an existing HTML page.
     *  
     * @param document the w3c Document to strip
     */
    public static void stripHTMLEnvelope(Document document)
    {
        org.w3c.dom.Element root = document.getDocumentElement();
        if (root.getNodeName().equalsIgnoreCase("html")) {
            // Look for a head element below the root element and for a body element
            Node bodyNode = null;
            Node headNode = null;
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equalsIgnoreCase("head")) {
                    headNode = node;
                } else if (node.getNodeName().equalsIgnoreCase("body")) {
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
}
