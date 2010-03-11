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

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

/**
 * Test case for cleaning html images in {@link OpenOfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class ImageOpenOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * Mock document access bridge.
     */
    private DocumentAccessBridge mockDAB;
    
    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        mockDAB = getComponentManager().lookup(DocumentAccessBridge.class);        
    }

    /**
     * {@code <img/>} links should be wrapped in xwiki specific html elements so that they are recognized by the XHTML
     * parser.
     */
    @org.junit.Test
    public void testImageWrapping() throws Exception
    {
        String html = header + "<img src=\"foo.png\"/>" + footer;
        HTMLCleanerConfiguration configuration = this.openOfficeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", "Import.Test"));

        this.mockery.checking(new Expectations() {{
            allowing(mockDAB).getAttachmentURL("Import.Test", "foo.png");
            will(returnValue("/bridge/foo.png"));
        }});
        
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html), configuration);

        NodeList nodes = doc.getElementsByTagName("img");
        Assert.assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startComment = image.getPreviousSibling();
        Node stopComment = image.getNextSibling();
        Assert.assertEquals(Node.COMMENT_NODE, startComment.getNodeType());
        Assert.assertTrue(startComment.getNodeValue().equals("startimage:foo.png"));
        Assert.assertEquals("/bridge/foo.png", image.getAttribute("src"));
        Assert.assertEquals(Node.COMMENT_NODE, stopComment.getNodeType());
        Assert.assertTrue(stopComment.getNodeValue().equals("stopimage"));
    }

    /**
     * Sometimes images are used inside links. In such cases, both the html link and the image need to be wrapped
     * properly.
     */
    @org.junit.Test
    public void testCompoundImageLinkWrapping()
    {
        String html = header + "<a href=\"http://www.xwiki.org\"><img src=\"foo.png\"/></a>" + footer;
        HTMLCleanerConfiguration configuration = this.openOfficeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", "Import.Test"));

        this.mockery.checking(new Expectations() {{
            allowing(mockDAB).getAttachmentURL("Import.Test", "foo.png");
            will(returnValue("/bridge/foo.png"));
        }});
        
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html), configuration);

        NodeList nodes = doc.getElementsByTagName("img");
        Assert.assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Node startImageComment = image.getPreviousSibling();
        Node stopImageComment = image.getNextSibling();
        Assert.assertEquals(Node.COMMENT_NODE, startImageComment.getNodeType());
        Assert.assertTrue(startImageComment.getNodeValue().equals("startimage:foo.png"));
        Assert.assertEquals("/bridge/foo.png", image.getAttribute("src"));
        Assert.assertEquals(Node.COMMENT_NODE, stopImageComment.getNodeType());
        Assert.assertTrue(stopImageComment.getNodeValue().equals("stopimage"));
        Element link = (Element) image.getParentNode();
        Assert.assertEquals("a", link.getNodeName());
        Assert.assertEquals("http://www.xwiki.org", link.getAttribute("href"));
        Element span = (Element) link.getParentNode();
        Assert.assertEquals("span", span.getNodeName());
        Assert.assertEquals("wikiexternallink", span.getAttribute("class"));
        Node startLinkComment = span.getPreviousSibling();
        Assert.assertEquals(Node.COMMENT_NODE, startLinkComment.getNodeType());
        Assert.assertTrue(startLinkComment.getNodeValue().startsWith("startwikilink"));
        Node stopLinkComment = span.getNextSibling();
        Assert.assertEquals(Node.COMMENT_NODE, stopLinkComment.getNodeType());
        Assert.assertTrue(stopLinkComment.getNodeValue().startsWith("stopwikilink"));
    }

    /**
     * OpenOffice 3.2 server generates relative paths for embedded images. These relative paths should be cleaned and
     * the image name extracted.
     */
    @org.junit.Test
    public void testRelativePathCleaning()
    {
        String html = header + "<img src=\"../../some/path/foo.png\"/>" + footer;
        HTMLCleanerConfiguration configuration = this.openOfficeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", "Import.Test"));

        this.mockery.checking(new Expectations() {{
            allowing(mockDAB).getAttachmentURL("Import.Test", "foo.png");
            will(returnValue("/bridge/foo.png"));
        }});
        
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html), configuration);
        
        NodeList nodes = doc.getElementsByTagName("img");
        Assert.assertEquals(1, nodes.getLength());
        Element image = (Element) nodes.item(0);
        Assert.assertEquals("/bridge/foo.png", image.getAttribute("src"));
    }
}
