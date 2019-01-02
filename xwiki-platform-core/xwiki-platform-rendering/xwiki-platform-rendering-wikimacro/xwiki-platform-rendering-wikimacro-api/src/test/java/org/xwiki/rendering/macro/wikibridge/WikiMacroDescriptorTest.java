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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link WikiMacroDescriptor}.
 * 
 * @version $Id$
 * @since 2.1RC1
 */
public class WikiMacroDescriptorTest
{
    /**
     * Ensure that the parameter descriptor map returned has the same order as the order in which parameter
     * descriptors are passed to the Wiki Macro descriptor. This is useful for example to ensure that the
     * WYSIWYG editor will display the wiki macro parameter in the same order as the Wiki Macro Object order.
     */
    @Test
    public void testGetParameterDescriptorMapInCorrectOrder()
    {
        List<WikiMacroParameterDescriptor> paramDescriptors = Arrays.asList(
            new WikiMacroParameterDescriptor("id1", "description1", true),
            new WikiMacroParameterDescriptor("id2", "description2", true));
        WikiMacroDescriptor descriptor = new WikiMacroDescriptor.Builder().name("name").description("description")
            .defaultCategory("category").visibility(WikiMacroVisibility.GLOBAL)
            .contentDescriptor(new DefaultContentDescriptor()).parameterDescriptors(paramDescriptors).build();
        Map<String, ParameterDescriptor> result = descriptor.getParameterDescriptorMap();

        Iterator<String> it = result.keySet().iterator();
        assertEquals("id1", it.next());
        assertEquals("id2", it.next());
    }
}
