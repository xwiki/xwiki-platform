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
 *
 */
package org.xwiki.xml;

import java.io.StringReader;

import org.w3c.dom.Document;
import org.xwiki.test.AbstractXWikiComponentTestCase;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.internal.html.DefaultHTMLCleanerTest;

/**
 * Unit tests for {@link org.xwiki.xml.XMLUtils}.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class XMLUtilsTest extends AbstractXWikiComponentTestCase
{
    private HTMLCleaner cleaner;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        cleaner = (HTMLCleaner) getComponentManager().lookup(HTMLCleaner.class, "default");
    }

    public void testStripHTMLEnvelope() throws Exception
    {
        Document document =
            cleaner.clean(new StringReader("<html><head/><body><p>test1</p><p>test2</p></body></html>"));
        XMLUtils.stripHTMLEnvelope(document);
        assertEquals(DefaultHTMLCleanerTest.HEADER + "<html><p>test1</p><p>test2</p></html>\n", XMLUtils
            .toString(document));
    }
    
    public void testEscapeXMLComment()
    {
        assertEquals("-\\- ", XMLUtils.escapeXMLComment("-- "));
        assertEquals("-\\", XMLUtils.escapeXMLComment("-"));
        assertEquals("-\\-\\-\\", XMLUtils.escapeXMLComment("---"));
        assertEquals("- ", XMLUtils.escapeXMLComment("- "));
    }
    
    public void testUnescapeXMLComment()
    {
        assertEquals("", XMLUtils.unescapeXMLComment("\\"));
        assertEquals("\\", XMLUtils.unescapeXMLComment("\\\\"));
        assertEquals("--", XMLUtils.unescapeXMLComment("\\-\\-"));
        assertEquals("--", XMLUtils.unescapeXMLComment("\\-\\-\\"));
    }
}
