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

import org.junit.jupiter.api.BeforeEach;
import org.w3c.dom.Document;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Abstract class for all HTML cleaner tests.
 *
 * @version $Id$
 * @since 1.8M2
 */
abstract class AbstractHTMLCleaningTest extends AbstractOfficeImporterTest
{
    /**
     * Beginning of the test html document.
     */
    protected String header = "<html><head><title>Title</title></head><body>";

    /**
     * Ending of the test html document..
     */
    protected String footer = "</body></html>";

    /**
     * {@link OfficeHTMLCleaner} used for tests.
     */
    protected HTMLCleaner officeHTMLCleaner;

    /**
     * {@link WysiwygHTMLCleaner} used for tests.
     */
    protected HTMLCleaner wysiwygHTMLCleaner;

    @BeforeEach
    public void setUp() throws Exception
    {
        this.officeHTMLCleaner = this.componentManager.getInstance(HTMLCleaner.class, "openoffice");
        this.wysiwygHTMLCleaner = this.componentManager.getInstance(HTMLCleaner.class, "wysiwyg");
    }

    /**
     * Asserts that the given dirty HTML fragment equals the expected clean HTML after the cleaning process.
     *
     * @param dirtyHTML         the HTML fragment to be cleaned
     * @param expectedCleanHTML expected clean HTML
     */
    protected void assertCleanHTML(String dirtyHTML, String expectedCleanHTML)
    {
        Document document = officeHTMLCleaner.clean(new StringReader(header + dirtyHTML + footer));
        assertEquals(header + expectedCleanHTML + footer, HTMLUtils.toString(document, true, true).trim());
    }
}
