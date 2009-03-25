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
package org.xwiki.officeimporter.internal.cleaner;

import java.io.StringReader;
import java.util.Collections;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLConstants;

/**
 * Test case for cleaning html images in {@link OpenOfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class ImageOpenOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * {@code <img/>} links should be wrapped in xwiki specific html elements so that they are recognized by the XHTML
     * parser.
     */
    public void testImageWrapping()
    {
        String html = header + "<img src=\"foo.png\"/>" + footer;
        HTMLCleanerConfiguration configuration = this.openOfficeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", "Import.Test"));
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html), configuration);
        NodeList nodes = doc.getElementsByTagName("img");
        assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startComment = image.getPreviousSibling();
        Node stopComment = image.getNextSibling();
        assertEquals(Node.COMMENT_NODE, startComment.getNodeType());
        assertTrue(startComment.getNodeValue().equals("startimage:foo.png"));
        assertEquals("/bridge/foo.png", image.getAttribute("src"));
        assertEquals(Node.COMMENT_NODE, stopComment.getNodeType());
        assertTrue(stopComment.getNodeValue().equals("stopimage"));
    }

    /**
     * Sometimes images are used inside links. In such cases, both the html link and the image need to be wrapped
     * properly.
     */
    public void testCompoundImageLinkWrapping()
    {
        String html = header + "<a href=\"http://www.xwiki.org\"><img src=\"foo.png\"/></a>" + footer;
        HTMLCleanerConfiguration configuration = this.openOfficeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", "Import.Test"));
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html), configuration);
        NodeList nodes = doc.getElementsByTagName("img");
        assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startImageComment = image.getPreviousSibling();
        Node stopImageComment = image.getNextSibling();
        assertEquals(Node.COMMENT_NODE, startImageComment.getNodeType());
        assertTrue(startImageComment.getNodeValue().equals("startimage:foo.png"));
        assertEquals("/bridge/foo.png", image.getAttribute("src"));
        assertEquals(Node.COMMENT_NODE, stopImageComment.getNodeType());
        assertTrue(stopImageComment.getNodeValue().equals("stopimage"));
        Element link = (Element) image.getParentNode();
        assertEquals("a", link.getNodeName());
        assertEquals("http://www.xwiki.org", link.getAttribute("href"));
        Element span = (Element) link.getParentNode();
        assertEquals("span", span.getNodeName());
        assertEquals("wikiexternallink", span.getAttribute("class"));
        Node startLinkComment = span.getPreviousSibling();
        assertEquals(Node.COMMENT_NODE, startLinkComment.getNodeType());
        assertTrue(startLinkComment.getNodeValue().startsWith("startwikilink"));
        Node stopLinkComment = span.getNextSibling();
        assertEquals(Node.COMMENT_NODE, stopLinkComment.getNodeType());
        assertTrue(stopLinkComment.getNodeValue().startsWith("stopwikilink"));
    }
}
