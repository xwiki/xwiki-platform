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
package org.xwiki.xml.html;

import java.io.StringReader;

import org.w3c.dom.Document;
import org.xwiki.test.AbstractXWikiComponentTestCase;
import org.xwiki.xml.internal.html.DefaultHTMLCleanerTest;

/**
 * Unit tests for {@link org.xwiki.xml.html.HTMLUtils}.
 * 
 * @version $Id$
 * @since 1.8.3
 */
public class HTMLUtilsTest extends AbstractXWikiComponentTestCase
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
            cleaner.clean(new StringReader("<html><head><body><p>test1</p><p>test2</p></body></html>"));
        HTMLUtils.stripHTMLEnvelope(document);
        assertEquals(DefaultHTMLCleanerTest.HEADER + "<html><p>test1</p><p>test2</p></html>\n", 
            HTMLUtils.toString(document));
    }
    
    public void testStripTopLevelParagraph() throws Exception
    {
        Document document = cleaner.clean(new StringReader("<html><head /><body><p>test</p></body></html>"));
        HTMLUtils.stripFirstElementInside(document, "body", "p");
        assertEquals(DefaultHTMLCleanerTest.HEADER + "<html><head></head><body>test</body></html>\n", 
            HTMLUtils.toString(document));
    }
}
