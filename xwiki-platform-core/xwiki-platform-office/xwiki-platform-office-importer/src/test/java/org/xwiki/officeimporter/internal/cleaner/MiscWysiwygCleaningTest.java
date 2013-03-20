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
import org.junit.Assert;
import org.junit.Test;

/**
 * Miscellaneous cleaning tests for {@link WysiwygHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class MiscWysiwygCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * Test cleaning of HTML paragraphs with namespaces specified.
     */
    @Test
    public void testParagraphsWithNamespaces()
    {
        String html = header + "<w:p>paragraph</w:p>" + footer;
        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap(HTMLCleanerConfiguration.NAMESPACES_AWARE, "false"));
        Document doc = wysiwygHTMLCleaner.clean(new StringReader(html), configuration);
        NodeList nodes = doc.getElementsByTagName("p");
        Assert.assertEquals(1, nodes.getLength());
    }

    /**
     * The source of the images in copy pasted HTML content should be replaces with 'Missing.png' since they can't be
     * uploaded automatically.
     */
    @Test
    public void testImageFiltering()
    {        
        String html = header + "<img src=\"file://path/to/local/image.png\"/>" + footer;
        Document doc = wysiwygHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("img");
        Assert.assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startComment = image.getPreviousSibling();
        Node stopComment = image.getNextSibling();
        Assert.assertEquals(Node.COMMENT_NODE, startComment.getNodeType());
        Assert.assertEquals("startimage:false|-|attach|-|Missing.png", startComment.getNodeValue());
        Assert.assertEquals("Missing.png", image.getAttribute("src"));
        Assert.assertEquals(Node.COMMENT_NODE, stopComment.getNodeType());
        Assert.assertTrue(stopComment.getNodeValue().equals("stopimage"));
    }
}
