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
package org.xwiki.rendering.internal.macro;

import org.junit.*;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Unit tests for {@link MacroId}.
 *
 * @version $Id$
 * @since 2.0M3
 */
public class MacroIdTest
{
    @Test
    public void testEquality()
    {
        MacroId id1 = new MacroId("id", Syntax.XWIKI_2_0);
        MacroId id2 = new MacroId("id", Syntax.XWIKI_2_0);
        MacroId id3 = new MacroId("otherid", Syntax.XWIKI_2_0);
        MacroId id4 = new MacroId("id", Syntax.XHTML_1_0);
        MacroId id5 = new MacroId("otherid", Syntax.XHTML_1_0);
        MacroId id6 = new MacroId("id");
        MacroId id7 = new MacroId("id");

        Assert.assertEquals(id2, id1);
        // Equal objects must have equal hashcode
        Assert.assertTrue(id1.hashCode() == id2.hashCode());

        Assert.assertFalse(id3 == id1);
        Assert.assertFalse(id4 == id1);
        Assert.assertFalse(id5 == id3);
        Assert.assertFalse(id6 == id1);

        Assert.assertEquals(id7, id6);
        // Equal objects must have equal hashcode
        Assert.assertTrue(id6.hashCode() == id7.hashCode());
    }
}
