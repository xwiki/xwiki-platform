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
package org.xwiki.rendering.macro.wikibridge;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility.WIKI;

/**
 * Test of {@link WikiMacroDescriptor} after being aspectified by {@link WikiMacroDescriptorAspect}.
 *
 * @version $Id$
 * @since 14.6RC1
 */
class WikiMacroDescriptorTest
{
    @Test
    void builderDefaultCategory()
    {
        WikiMacroDescriptor macroDescriptor = new WikiMacroDescriptor.Builder().defaultCategory("Test").build();
        assertEquals(Set.of("Test"), macroDescriptor.getDefaultCategories());
        assertEquals("Test", macroDescriptor.getDefaultCategory());
    }

    @Test
    void constructor()
    {
        ContentDescriptor contentDescriptor = mock(ContentDescriptor.class);
        WikiMacroDescriptor wikiMacroDescriptor =
            new WikiMacroDescriptor(new MacroId("macroId"), "macro name", "description", "defaultCategory", WIKI,
                contentDescriptor, List.of());
        assertEquals(new MacroId("macroId"), wikiMacroDescriptor.getId());
        assertEquals("macro name", wikiMacroDescriptor.getName());
        assertEquals("description", wikiMacroDescriptor.getDescription());
        assertEquals("defaultCategory", wikiMacroDescriptor.getDefaultCategory());
        assertEquals(Set.of("defaultCategory"), wikiMacroDescriptor.getDefaultCategories());
        assertEquals(WIKI, wikiMacroDescriptor.getVisibility());
        assertSame(contentDescriptor, wikiMacroDescriptor.getContentDescriptor());
        assertEquals(Map.of(), wikiMacroDescriptor.getParameterDescriptorMap());
    }

    @Test
    void constructorWithString()
    {
        ContentDescriptor contentDescriptor = mock(ContentDescriptor.class);
        WikiMacroDescriptor wikiMacroDescriptor =
            new WikiMacroDescriptor("macro name", "description", "defaultCategory", WIKI, contentDescriptor,
                List.of());
        assertNull(wikiMacroDescriptor.getId());
        assertEquals("macro name", wikiMacroDescriptor.getName());
        assertEquals("description", wikiMacroDescriptor.getDescription());
        assertEquals("defaultCategory", wikiMacroDescriptor.getDefaultCategory());
        assertEquals(Set.of("defaultCategory"), wikiMacroDescriptor.getDefaultCategories());
        assertEquals(WIKI, wikiMacroDescriptor.getVisibility());
        assertSame(contentDescriptor, wikiMacroDescriptor.getContentDescriptor());
        assertEquals(Map.of(), wikiMacroDescriptor.getParameterDescriptorMap());
    }
}
