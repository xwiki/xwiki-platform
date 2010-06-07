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

import org.w3c.dom.Document;
import org.xwiki.xml.html.HTMLUtils;

import com.xpn.xwiki.wysiwyg.server.cleaner.internal.NestedAnchorsFilter;

/**
 * Test for the {@link NestedAnchorsFilterTest} class.
 * 
 * @version $Id$
 */
public class NestedAnchorsFilterTest extends AbstractHTMLFilterTest
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractHTMLFilterTest#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        filter = new NestedAnchorsFilter();
    }

    /**
     * Tests that the nested anchors cleaner leaves a document with anchors on just on level unchanged.
     * 
     * @throws Exception if the XML parsing fails
     */
    public void testFilterOneLevel() throws Exception
    {
        Document document =
            parseXHTMLFragment("<p>XWiki <!--startwikilink:http://www.xwiki.org--><span class=\"wikiexternallink\">"
                + "<a href=\"http://www.xwiki.org\">http://www.xwiki.org</a>"
                + "</span><!--stopwikilink--> .org<br /></p>");
        String before = HTMLUtils.toString(document);
        filter.filter(document, null);
        String after = HTMLUtils.toString(document);
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
            "<p><!--startwikilink:http://www.xwiki.org--><span class=\"wikiexternallink\">"
                + "<a class=\"wikimodel-freestanding\" href=\"http://www.xwiki.org\">"
                + "<span class=\"wikigeneratedlinkcontent\">";
        String anchorLabel = "http://www.<strong>xwiki</strong>.org";
        String afterAnchor = "</span></a>ssst</span><!--stopwikilink-->s</p>";
        String documentString =
            beforeAnchor + "<a href=\"http://www.xwiki.orgs\">" + anchorLabel + "</a>" + afterAnchor;

        Document document = parseXHTMLFragment(documentString);
        filter.filter(document, null);
        String actual = HTMLUtils.toString(document);

        String expected = xhtmlFragment(beforeAnchor + anchorLabel + afterAnchor);

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
            "<p><!--startwikilink:http://www.xwiki.org--><span>"
                + "<a class=\"wikimodel-freestanding\" href=\"http://www.xwiki.com\">" + "<span>";
        String anchorLabel = "http://www.xwiki.org";
        String afterAnchor = "</span></a>s </span><!--stopwikilink-->s</p>";
        String documentString =
            beforeAnchor + "<a href=\"http://www.xwiki.orgs\"><a href=\"http://www.xwiki.com\">" + anchorLabel
                + "</a>com</a>" + afterAnchor;

        Document document = parseXHTMLFragment(documentString);
        filter.filter(document, null);
        String actual = HTMLUtils.toString(document);

        String expected = xhtmlFragment(beforeAnchor + anchorLabel + "com" + afterAnchor);
        assertEquals(expected, actual);
    }
}
