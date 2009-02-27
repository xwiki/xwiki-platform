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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Test case for cleaning html tables in {@link OpenOfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class TableOpenOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * An isolated paragraph inside a table cell item should be replaced with paragraph's content.
     */
    public void testTableCellItemIsolatedParagraphCleaning()
    {
        String html = header + "<table><tr><td><p>Test</p></td></tr></table>" + footer;
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("td");
        Node cellContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, cellContent.getNodeType());
        assertEquals("Test", cellContent.getNodeValue());
    }

    /**
     * An isolated paragraph inside a table header item should be replaced with paragraph's content.
     */
    public void testTableHeaderItemIsolatedParagraphCleaning()
    {
        String html =
            header + "<table><thead><tr><th><p>Test</p></th></tr></thead>" + "<tbody><tr><td/></tr></tbody></table>"
                + footer;
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("th");
        Node cellContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, cellContent.getNodeType());
        assertEquals("Test", cellContent.getNodeValue());
    }

    /**
     * If multiple paragraphs are found inside a table cell item, they should be wrapped in an embedded document.
     */
    public void testTableCellItemMultipleParagraphWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><p>Test</p><p>Test</p></td></tr></table>",
            "td"));
    }

    /**
     * If multiple paragraphs are found inside a table header item, they should be wrapped in an embedded document.
     */
    public void testTableHeaderItemMultipleParagraphWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration(
            "<table><thead><tr><th><p>Test</p><p>Test</p></th></tr></thead>" + "<tbody><tr><td/></tr></tbody></table>",
            "th"));
    }

    /**
     * If a list is found inside a table cell item, it should be wrapped in an embedded document.
     */
    public void testTableCellItemInternalListWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><ol><li>item</li></ol></td></tr></table>",
            "td"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><ul><li>item</li></ul></td></tr></table>",
            "td"));
    }

    /**
     * If a list is found inside a table header item, it should be wrapped in an embedded document.
     */
    public void testTableHeaderItemInternalListWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration(
            "<table><thead><tr><th><ol><li>item</li></ol></th></tr></thead>" + "<tbody><tr><td/></tr></tbody></table>",
            "th"));
        assertEquals(true, checkEmbeddedDocumentGeneration(
            "<table><thead><tr><th><ul><li>item</li></ul></th></tr></thead>" + "<tbody><tr><td/></tr></tbody></table>",
            "th"));
    }

    /**
     * If a table is found inside a table cell item, it should be wrapped in an embedded document.
     */
    public void testTableCellItemInternalTableWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration(
            "<table><tr><td><table><tr><td><p>content</p></td></tr></table></td></tr></table>", "td"));
    }

    /**
     * If a table is found inside a table header item, it should be wrapped in an embedded document.
     */
    public void testTableHeaderItemInternalTableWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration(
            "<table><thead><tr><th><table><tr><td><p>content</p></td></tr></table></th></tr></thead>"
                + "<tbody><tr><td/></tr></tbody></table>", "th"));
    }

    /**
     * If an image is found inside a table cell item, it should be wrapped in an embedded document.
     */
    public void testTableCellItemInternalImageWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><img src=\"foo.png\"/></td></tr></table>",
            "td"));
    }

    /**
     * If an image is found inside a table header item, it should be wrapped in an embedded document.
     */
    public void testTableHeaderItemInternalImageWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration(
            "<table><thead><tr><th><img src=\"foo.png\"/></th></tr></thead>" + "<tbody><tr><td/></tr></tbody></table>",
            "th"));
    }

    /**
     * If a heading is found inside a table cell item, it should be wrapped in an embedded document.
     */
    public void testTableCellItemInternalHeadingWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><h1>Hi</h1></td></tr></table>", "td"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><h2>Hi</h2></td></tr></table>", "td"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><h3>Hi</h3></td></tr></table>", "td"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><h4>Hi</h4></td></tr></table>", "td"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><h5>Hi</h5></td></tr></table>", "td"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><tr><td><h6>Hi</h6></td></tr></table>", "td"));
    }

    /**
     * If a heading is found inside a table header item, it should be wrapped in an embedded document.
     */
    public void testTableHeaderItemInternalHeadingWrapping()
    {
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><thead><tr><th><h1>Hi</h1></th></tr></thead>"
            + "<tbody><tr><td/></tr></tbody></table>", "th"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><thead><tr><th><h2>Hi</h2></th></tr></thead>"
            + "<tbody><tr><td/></tr></tbody></table>", "th"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><thead><tr><th><h3>Hi</h3></th></tr></thead>"
            + "<tbody><tr><td/></tr></tbody></table>", "th"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><thead><tr><th><h4>Hi</h4></th></tr></thead>"
            + "<tbody><tr><td/></tr></tbody></table>", "th"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><thead><tr><th><h5>Hi</h5></th></tr></thead>"
            + "<tbody><tr><td/></tr></tbody></table>", "th"));
        assertEquals(true, checkEmbeddedDocumentGeneration("<table><thead><tr><th><h6>Hi</h6></th></tr></thead>"
            + "<tbody><tr><td/></tr></tbody></table>", "th"));
    }

    /**
     * Empty rows should be removed.
     */
    public void testEmptyRowRemoving()
    {
        String html = header + "<table><tbody><tr><td>cell</td></tr><tr></tr></tbody></table>" + footer;
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("tr");
        assertEquals(1, nodes.getLength());
    }

    /**
     * Utility method for checking if an embedded document is generated correctly from an html cleaning operation.
     * 
     * @param html the html input.
     * @param tagName the tag name inside which the embedded document should be generated.
     * @return true if an embedded document is generated dorrectly.
     */
    private boolean checkEmbeddedDocumentGeneration(String html, String tagName)
    {
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(header + html + footer));
        NodeList nodes = doc.getElementsByTagName(tagName);
        Node cellContent = nodes.item(0).getFirstChild();
        return ("div".equals(cellContent.getNodeName()))
            && "xwiki-document".equals(((Element) cellContent).getAttribute("class"));
    }
}
