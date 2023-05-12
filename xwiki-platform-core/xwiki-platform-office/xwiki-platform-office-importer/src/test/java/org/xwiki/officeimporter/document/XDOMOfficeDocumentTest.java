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
package org.xwiki.officeimporter.document;

import java.io.StringReader;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for {@link XDOMOfficeDocument}.
 * 
 * @version $Id$
 * @since 2.2.5
 */
@ComponentTest
class XDOMOfficeDocumentTest extends AbstractOfficeImporterTest
{
    /**
     * Tests how document title is extracted from the content of the imported document.
     * 
     * @throws Exception if it fails to extract the title
     */
    @Test
    void titleExtraction() throws Exception
    {
        String content = "content before title\n" + "%s Title %s\n" + "content after title.";
        XDOMOfficeDocument doc = createOfficeDocument(String.format(content, "=", "="), "xwiki/2.0");
        assertEquals("Title", doc.getTitle());

        doc = createOfficeDocument(String.format(content, "==", "=="), "xwiki/2.0");
        assertEquals("Title", doc.getTitle());

        doc = createOfficeDocument(String.format(content, "===", "==="), "xwiki/2.0");
        assertEquals("Title", doc.getTitle());

        doc = createOfficeDocument(String.format(content, "====", "===="), "xwiki/2.0");
        assertEquals("Title", doc.getTitle());

        doc = createOfficeDocument(String.format(content, "=====", "====="), "xwiki/2.0");
        assertEquals("Title", doc.getTitle());

        doc = createOfficeDocument(String.format(content, "======", "======"), "xwiki/2.0");
        assertEquals("Title", doc.getTitle());
    }

    /**
     * Creates an {@link XDOMOfficeDocument} by parsing the given content.
     * 
     * @param content the content to be parsed
     * @param syntax the syntax of the given content
     * @return the created {@link XDOMOfficeDocument}
     * @throws Exception if it fails to parse the given content
     */
    private XDOMOfficeDocument createOfficeDocument(String content, String syntax) throws Exception
    {
        Parser parser = this.componentManager.getInstance(Parser.class, syntax);
        XDOM xdom = parser.parse(new StringReader(content));
        return new XDOMOfficeDocument(xdom, Collections.emptyMap(), this.componentManager, null);
    }
}
