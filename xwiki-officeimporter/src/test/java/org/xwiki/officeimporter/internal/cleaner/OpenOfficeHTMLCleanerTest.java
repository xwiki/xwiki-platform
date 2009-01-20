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
import org.xwiki.officeimporter.internal.MockDocumentAccessBridge;
import org.xwiki.xml.html.HTMLCleaner;

import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Test case for default open office html cleaner.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OpenOfficeHTMLCleanerTest extends AbstractXWikiComponentTestCase
{
    /**
     * Beginning of the test html document.
     */
    private String header = "<html><head><title>Title</title></head><body>";

    /**
     * Beginning of the test html document, which has a {@code <style> tag.}
     */
    private String headerWithStyles =
        "<html><head><style type=\"text/css\">h1 {color:red} p {color:blue} </style><title>Title</title></head><body>";

    /**
     * Ending of the test html document..
     */
    private String footer = "</body></html>";

    /**
     * Open office html cleaner.
     */
    private HTMLCleaner cleaner;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        getComponentManager().registerComponentDescriptor(MockDocumentAccessBridge.getComponentDescriptor());
        super.setUp();
        cleaner = (HTMLCleaner) getComponentManager().lookup(HTMLCleaner.ROLE, "openoffice");
    }

    /**
     * Test most basic cleaning.
     */
    public void testBasicCleaning()
    {
        String html = header + footer;
        Document doc = cleaner.clean(new StringReader(html));
        assertNotNull(doc.getDoctype());
    }

    /**
     * Test stripping of {@code <style>} and {@code <script>} tags.
     */
    public void testTagStripping()
    {
        String html = headerWithStyles + footer;
        Document doc = cleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("style");
        assertEquals(0, nodes.getLength());
        html = header + "<script type=\"text/javascript\">document.write(\"Hello World!\")</script>" + footer;
        doc = cleaner.clean(new StringReader(html));
        nodes = doc.getElementsByTagName("script");
        assertEquals(0, nodes.getLength());
    }

    /**
     * Test stripping of redundant tags.
     */
    public void testRedundantTagFiltering()
    {
        // <span> & <div> tags without attributes should be stripped off.
        String htmlTemplate = header + "<p>Test%sRedundant%sFiltering</p>" + footer;
        String[] attributeWiseFilteredTags = new String[] {"span", "div"};
        for (String tag : attributeWiseFilteredTags) {
            String startTag = "<" + tag + ">";
            String endTag = "</" + tag + ">";
            String html = String.format(htmlTemplate, startTag, endTag);
            Document doc = cleaner.clean(new StringReader(html));
            NodeList nodes = doc.getElementsByTagName(tag);
            assertEquals(0, nodes.getLength());
        }
        // Tags that usually contain textual information like <strong>, <code>, <em> etc. etc.
        // should be filtered if they do not contain any textual content.
        htmlTemplate = header + "<p>Test%sRedundant%s%s%sFiltering</p>" + footer;
        String[] contentWiseFilteredTags =
            new String[] {"em", "strong", "dfn", "code", "samp", "kbd", "var", "cite", "abbr", "acronym", "address",
            "blockquote", "q", "pre", "h1", "h2", "h3", "h4", "h5", "h6"};
        for (String tag : contentWiseFilteredTags) {
            String startTag = "<" + tag + ">";
            String endTag = "</" + tag + ">";
            String html = String.format(htmlTemplate, startTag, endTag, startTag, endTag);
            Document doc = cleaner.clean(new StringReader(html));
            NodeList nodes = doc.getElementsByTagName(tag);
            assertEquals(1, nodes.getLength());
        }
    }
    
    /**
     * Test filtering of {@code<p><br/></p>} elements.
     */
    public void testEmptyParagraphFiltering() {
        String html = header + "<p><br/></p><p><br/></p><p><br/></p><p><br/></p>" + footer;
        Document doc = cleaner.clean(new StringReader(html));
        NodeList paras = doc.getElementsByTagName("p");
        assertEquals(0, paras.getLength());
        NodeList breaks = doc.getElementsByTagName("br");
        assertEquals(3, breaks.getLength());
    }

    /**
     * Test filtering of html links.
     */
    public void testLinkFiltering()
    {
        String html = header + "<a href=\"http://www.xwiki.org\">xwiki</a>" + footer;
        Document doc = cleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("a");
        assertEquals(1, nodes.getLength());
        Node link = nodes.item(0);
        Element span = (Element) link.getParentNode();
        assertEquals("span", span.getNodeName());
        assertEquals("wikiexternallink", span.getAttribute("class"));
        Node startComment = span.getPreviousSibling();
        assertEquals(Node.COMMENT_NODE, startComment.getNodeType());
        assertTrue(startComment.getNodeValue().startsWith("startwikilink"));
        Node stopComment = span.getNextSibling();
        assertEquals(Node.COMMENT_NODE, stopComment.getNodeType());
        assertTrue(stopComment.getNodeValue().startsWith("stopwikilink"));
    }

    /**
     * Test filtering of html lists.
     */
    public void testListFiltering()
    {
        // Leading spaces inside list items are not allowed.
        String html = header + "<ol><li> Test</li></ol>" + footer;
        Document doc = cleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("li");
        Node listContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, listContent.getNodeType());
        assertEquals("Test", listContent.getNodeValue());
        // Paragraphs inside list items are not allowed.
        html = header + "<ol><li><p>Test</p></li></ol>" + footer;
        doc = cleaner.clean(new StringReader(html));
        listContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, listContent.getNodeType());
        assertEquals("Test", listContent.getNodeValue());
        // Leading space plus a starting paragraph.
        html = header + "<ol><li> <p>Test</p></li></ol>" + footer;
        doc = cleaner.clean(new StringReader(html));
        listContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, listContent.getNodeType());
        assertEquals("Test", listContent.getNodeValue());
    }

    /**
     * Test filtering of html tables.
     */
    public void testTableFiltering()
    {
        // Leading or trailing spaces inside cell items are not allowed.
        String html = header + "<table><tr><td> Test </td></tr></table>" + footer;
        Document doc = cleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("td");
        Node cellContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, cellContent.getNodeType());
        assertEquals("Test", cellContent.getNodeValue());
        // Paragraphs are not allowed inside cell items.
        html = header + "<table><tr><td> <p>Test</p> </td></tr></table>" + footer;
        doc = cleaner.clean(new StringReader(html));
        nodes = doc.getElementsByTagName("td");
        cellContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, cellContent.getNodeType());
        assertEquals("Test", cellContent.getNodeValue());
        // Line breaks are not allowed inside cell items.
        html = header + "<table><tr><td><br/><p><br/>Test</p> </td></tr></table>" + footer;
        doc = cleaner.clean(new StringReader(html));
        nodes = doc.getElementsByTagName("td");
        cellContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, cellContent.getNodeType());
        assertEquals("Test", cellContent.getNodeValue());
    }

    /**
     * Test filtering of html image links.
     */
    public void testImageFiltering()
    {
        String html = header + "<img src=\"foo.png\"/>" + footer;
        Document doc = cleaner.clean(new StringReader(html), Collections.singletonMap("targetDocument", "Import.Test"));
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
     * Test handling of image links.
     */
    public void testImageLinks()
    {
        String html = header + "<a href=\"http://www.xwiki.org\"><img src=\"foo.png\"/></a>" + footer;
        Document doc = cleaner.clean(new StringReader(html), Collections.singletonMap("targetDocument", "Import.Test"));
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
    
    /**
     * Test filtering of {@code<br/>} elements placed in between block elements.
     */
    public void testLineBreakFiltering() {
        String html = header + "<p>para1</p><br/><br/><p>para2</p>" + footer;
        Document doc = cleaner.clean(new StringReader(html), Collections.singletonMap("targetDocument", "Import.Test"));
        NodeList lineBreaks = doc.getElementsByTagName("br");
        assertEquals(0, lineBreaks.getLength());
        NodeList divs = doc.getElementsByTagName("div");
        assertEquals(2, divs.getLength());
    }
}
