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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for cleaning html lists in {@link OfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
@ComponentTest
public class ListOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * If there are leading spaces within the content of a list item ({@code<li/>}) they should be trimmed.
     */
    @Test
    public void listItemContentLeadingSpaceTrimming()
    {
        String html = header + "<ol><li> Test</li></ol>" + footer;
        Document doc = officeHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("li");
        Node listContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, listContent.getNodeType());
        assertEquals("Test", listContent.getNodeValue());
    }
    
    /**
     * If there is a leading paragraph inside a list item, it should be replaced with it's content.
     */
    @Test
    public void listItemContentIsolatedParagraphCleaning()
    {
        String html = header + "<ol><li><p>Test</p></li></ol>" + footer;
        Document doc = officeHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("li");
        Node listContent = nodes.item(0).getFirstChild();
        assertEquals(Node.TEXT_NODE, listContent.getNodeType());
        assertEquals("Test", listContent.getNodeValue());
    }
}
