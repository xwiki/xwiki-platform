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
import org.w3c.dom.NodeList;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for filtering {@code<p><br/></p>} elements in {@link OfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
@ComponentTest
public class EmptyLineParagraphOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * The first {@code<p><br/></p>} element in a sequence of such elements should be removed.
     */
    @Test
    public void removeFirstEmptyLineParagraph()
    {
        String html = header + "<p><br/></p>" + footer;
        Document doc = officeHTMLCleaner.clean(new StringReader(html));
        NodeList paras = doc.getElementsByTagName("p");
        assertEquals(0, paras.getLength());
        NodeList breaks = doc.getElementsByTagName("br");
        assertEquals(0, breaks.getLength());
    }

    /**
     * In a sequence of {@code<p><br/></p>} elements, while the first element is removed, rest will be replaced by
     * {@code<br/>} elements.
     */
    @Test
    public void replaceAdditionalEmptyLineParagraphs()
    {
        String html = header + "<p><br/></p><p><br/></p><p><br/></p><p><br/></p>" + footer;
        Document doc = officeHTMLCleaner.clean(new StringReader(html));
        NodeList breaks = doc.getElementsByTagName("br");
        assertEquals(3, breaks.getLength());
    }
}
