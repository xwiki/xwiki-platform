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
package org.xwiki.rendering.parser;

import junit.framework.TestCase;

/**
 * Unit tests for {@link Syntax}.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class SyntaxTest extends TestCase
{
    public void testEquality()
    {
        Syntax syntax1 = new Syntax(SyntaxType.XWIKI, "1.0");
        Syntax syntax2 = new Syntax(SyntaxType.XWIKI, "1.0");

        assertEquals(syntax2.hashCode(), syntax1.hashCode());
        assertEquals(syntax2, syntax1);
    }

    public void testNonEquality()
    {
        Syntax syntax1 = new Syntax(SyntaxType.XWIKI, "1.0");
        Syntax syntax2 = new Syntax(SyntaxType.XWIKI, "2.0");
        Syntax syntax3 = new Syntax(SyntaxType.CONFLUENCE, "1.0");

        assertFalse(syntax2.equals(syntax1));
        assertFalse(syntax3.equals(syntax1));
    }

    public void testToString()
    {
        Syntax syntax = new Syntax(SyntaxType.XWIKI, "1.0");
        assertEquals("XWiki 1.0", syntax.toString());
        assertEquals("xwiki/1.0", syntax.toIdString());
    }
}
