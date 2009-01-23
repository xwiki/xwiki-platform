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
import org.xwiki.xml.html.HTMLCleaner;

/**
 * Test case for wysiwyg html cleaner.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class WysiwygHTMLCleanerTest extends AbstractHTMLCleanerTest
{
    /**
     * Open office html cleaner.
     */
    private HTMLCleaner cleaner;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        cleaner = (HTMLCleaner) getComponentManager().lookup(HTMLCleaner.ROLE, "wysiwyg");
    }

    /**
     * Test cleaning of html content with namespaces.
     */
    public void testNamespacesAwareFiltering()
    {
        String html = header + "<w:p>paragraph</w:p>" + footer;
        Document doc =
            cleaner.clean(new StringReader(html), Collections.singletonMap(HTMLCleaner.NAMESPACES_AWARE, "false"));
        NodeList nodes = doc.getElementsByTagName("p");
        assertEquals(1, nodes.getLength());
    }

    /**
     * Test filtering of images in html content.
     */
    public void testImageFiltering()
    {
        String html = header + "<img src=\"file://path/to/local/image.png\"/>" + footer;
        Document doc = cleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("img");
        assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startComment = image.getPreviousSibling();
        Node stopComment = image.getNextSibling();
        assertEquals(Node.COMMENT_NODE, startComment.getNodeType());
        assertTrue(startComment.getNodeValue().equals("startimage:Missing.png"));
        assertEquals("Missing.png", image.getAttribute("src"));
        assertEquals(Node.COMMENT_NODE, stopComment.getNodeType());
        assertTrue(stopComment.getNodeValue().equals("stopimage"));
    }
}
