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
package org.xwiki.rendering.internal.macro.code.source;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Validate {@link CodeMacroSource}.
 * 
 * @version $Id$
 */
class CodeMacroSourceTest
{
    private static final MacroContentSourceReference REFERENCE1 = new MacroContentSourceReference("type1", "reference1");

    private static final MacroContentSourceReference REFERENCE2 = new MacroContentSourceReference("type2", "reference2");

    @Test
    void equalshashcode()
    {
        CodeMacroSource source = new CodeMacroSource(null, null, null);
        assertEquals(source, source);
        assertEquals(new CodeMacroSource(null, null, null), new CodeMacroSource(null, null, null));
        assertEquals(new CodeMacroSource(null, null, null).hashCode(),
            new CodeMacroSource(null, null, null).hashCode());
        assertEquals(new CodeMacroSource(REFERENCE1, "content", "language"),
            new CodeMacroSource(REFERENCE1, "content", "language"));
        assertEquals(new CodeMacroSource(REFERENCE1, "content", "language").hashCode(),
            new CodeMacroSource(REFERENCE1, "content", "language").hashCode());

        assertNotEquals(new CodeMacroSource(null, null, null), null);
        assertNotEquals(new CodeMacroSource(REFERENCE1, "content", "language"),
            new CodeMacroSource(REFERENCE2, "content", "language"));
        assertNotEquals(new CodeMacroSource(REFERENCE1, "content", "language"),
            new CodeMacroSource(REFERENCE1, "content1", "language"));
        assertNotEquals(new CodeMacroSource(REFERENCE1, "content", "language"),
            new CodeMacroSource(REFERENCE1, "content", "language1"));
    }

    @Test
    void tostring()
    {
        assertEquals("reference = [<null>], language = [<null>], content = [<null>]",
            new CodeMacroSource(null, null, null).toString());
        assertEquals("reference = [type1:reference1], language = [language], content = [content]",
            new CodeMacroSource(REFERENCE1, "content", "language").toString());
    }
}
