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
package com.xpn.xwiki.wysiwyg.server.cleaner;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xwiki.xml.html.HTMLUtils;

import com.xpn.xwiki.wysiwyg.server.cleaner.internal.NestedAnchorsFilter;

/**
 * Test for the {@link NestedAnchorsFilterTest} class.
 * 
 * @version $Id$
 */
public class NestedAnchorsFilterTest extends TestCase
{
    /**
     * The string with the XML prologue and the xhtml DTD.
     */
    private static final String PROLOGUE_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    /**
     * The filter under test.
     */
    private NestedAnchorsFilter filter = new NestedAnchorsFilter();

    /**
     * Tests that the nested anchors cleaner leaves a document with anchors on just on level unchanged.
     * 
     * @throws Exception if the XML parsing fails
     */
    public void testFilterOneLevel() throws Exception
    {
        String documentString =
            PROLOGUE_DTD + "<html><head></head><body><p>XWiki <!--startwikilink:http://www.xwiki.org-->"
                + "<span class=\"wikiexternallink\"><a href=\"http://www.xwiki.org\">http://www.xwiki.org</a></span>"
                + "<!--stopwikilink--> .org<br /></p></body></html>\n";
        Document doc = prepareDocument(documentString);

        String before = HTMLUtils.toString(doc);
        filter.filter(doc, null);

        String after = HTMLUtils.toString(doc);

        assertEquals(before, after);
    }

    /**
     * Tests that two nesting levels for anchors are removed.
     * 
     * @throws Exception if something goes wrong parsing XHTML
     */
    public void testFilterTwoLevels() throws Exception
    {
        String beforeAnchor =
            "<html><head></head><body><p><!--startwikilink:http://www.xwiki.org--><span class=\"wikiexternallink\">"
                + "<a class=\"wikimodel-freestanding\" href=\"http://www.xwiki.org\">"
                + "<span class=\"wikigeneratedlinkcontent\">";
        String anchorLabel = "http://www.<strong>xwiki</strong>.org";
        String afterAnchor = "</span></a>ssst</span><!--stopwikilink-->s</p></body></html>\n";
        String documentString =
            PROLOGUE_DTD + beforeAnchor + "<a href=\"http://www.xwiki.orgs\">" + anchorLabel + "</a>" + afterAnchor;

        Document doc = prepareDocument(documentString);
        filter.filter(doc, null);
        String actual = HTMLUtils.toString(doc);

        String expected = PROLOGUE_DTD + beforeAnchor + anchorLabel + afterAnchor;

        assertEquals(expected, actual);
    }

    /**
     * Tests that three levels nesting of anchors are cleaned correctly.
     * 
     * @throws Exception if something goes wrong parsing XHTML
     */
    public void testFilterThreeLevels() throws Exception
    {
        String beforeAnchor =
            "<html><head></head><body><p><!--startwikilink:http://www.xwiki.org--><span>"
                + "<a class=\"wikimodel-freestanding\" href=\"http://www.xwiki.com\">" + "<span>";
        String anchorLabel = "http://www.xwiki.org";
        String afterAnchor = "</span></a>s </span><!--stopwikilink-->s</p></body></html>\n";
        String documentString =
            PROLOGUE_DTD + beforeAnchor + "<a href=\"http://www.xwiki.orgs\"><a href=\"http://www.xwiki.com\">"
                + anchorLabel + "</a>com</a>" + afterAnchor;

        Document doc = prepareDocument(documentString);
        filter.filter(doc, null);
        String actual = HTMLUtils.toString(doc);

        String expected = PROLOGUE_DTD + beforeAnchor + anchorLabel + "com" + afterAnchor;
        assertEquals(expected, actual);
    }

    /**
     * Creates a {@link org.w3c.dom.Document} from the passed string.
     * 
     * @param stringSource the string value of the document
     * @return the {@link org.w3c.dom.Document} corresponding to the passed string
     * @throws Exception if parsing of the {@code stringSOurce} fails
     */
    private Document prepareDocument(String stringSource) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(stringSource));
        return builder.parse(inputSource);
    }
}
