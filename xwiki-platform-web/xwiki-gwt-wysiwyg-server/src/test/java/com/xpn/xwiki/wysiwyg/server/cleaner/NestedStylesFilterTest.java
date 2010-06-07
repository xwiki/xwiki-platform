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

import com.xpn.xwiki.wysiwyg.server.cleaner.internal.NestedStylesFilter;

/**
 * Unit tests for {@link NestedStylesFilter}.
 * 
 * @version $Id$
 */
public class NestedStylesFilterTest extends AbstractHTMLFilterTest
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

        filter = new NestedStylesFilter();
    }

    /**
     * Tests that nested styles are merged.
     * 
     * @throws Exception if parsing the XHTML fails
     */
    public void testMergeStyles() throws Exception
    {
        Document document =
            parseXHTMLFragment("<p>12<span style=\"font-family:Courier New;" + " font-size:24pt\">"
                + "34<span style=\"color:red; background-color:yellow\">56</span>78</span>90</p>");

        filter.filter(document, null);

        String expected =
            "<p>12<span style=\"font-family:Courier New; font-size:24pt\">"
                + "<span style=\"font-family: Courier New; font-size: 24pt\">34</span>"
                + "<span style=\"color:red; background-color:yellow\">"
                + "<span style=\"background-color: yellow; color: red; font-family: Courier New; font-size: 24pt\">"
                + "56</span></span><span style=\"font-family: Courier New; font-size: 24pt\">78</span></span>90</p>";
        String actual = HTMLUtils.toString(document);
        assertEquals(xhtmlFragment(expected), actual);
    }
}
