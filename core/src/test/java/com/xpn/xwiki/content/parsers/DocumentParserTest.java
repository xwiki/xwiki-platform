/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.content.parsers;

import junit.framework.TestCase;
import com.xpn.xwiki.content.Link;

/**
 * Unit tests for {@link DocumentParser}.
 *
 * @version $Id: $
 */
public class DocumentParserTest extends TestCase
{
    public void testParseLinksWhenDocumentWithNoLinks() throws Exception
    {
        DocumentParser parser = new DocumentParser();

        ParsingResultCollection result = parser.parseLinks("No links in there");
        assertFalse(result.hasInvalidElements());
        assertTrue(result.getValidElements().isEmpty());
    }

    public void testParseLinks() throws Exception
    {
        DocumentParser parser = new DocumentParser();

        ParsingResultCollection result = parser.parseLinks("This is [link1]. This is [link2].");
        assertFalse(result.hasInvalidElements());
        assertEquals(2, result.getValidElements().size());
        assertEquals("link1", ((Link) result.getValidElements().get(0)).getPage());
        assertEquals("link2", ((Link) result.getValidElements().get(1)).getPage());
    }
}
