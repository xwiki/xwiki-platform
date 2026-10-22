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
package org.xwiki.rendering.macro.wikibridge.binding;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link WikiMacroBinding}.
 *
 * @version $Id$
 */
class WikiMacroBindingTest
{
    @Test
    void constructorExposesTheValuesUnderTheirBindingNames()
    {
        MacroDescriptor descriptor = mock(MacroDescriptor.class);
        WikiMacroParameters parameters = new WikiMacroParameters();
        MacroTransformationContext context = new MacroTransformationContext();

        WikiMacroBinding binding = new WikiMacroBinding(descriptor, parameters, "content", context);

        assertSame(descriptor, binding.getDescriptor());
        assertSame(parameters, binding.getParameters());
        assertEquals("content", binding.getContent());
        assertSame(context, binding.getContext());
        assertNull(binding.getResult());

        assertSame(descriptor, binding.get("descriptor"));
        assertSame(parameters, binding.get("parameters"));
        assertEquals("content", binding.get("content"));
        assertSame(context, binding.get("context"));
    }

    @Test
    void setResult()
    {
        WikiMacroBinding binding = new WikiMacroBinding(null, null, null, null);
        binding.setResult("result");

        assertEquals("result", binding.getResult());
        assertEquals("result", binding.get("result"));
    }

    @Test
    void replaceAll()
    {
        WikiMacroBinding binding = new WikiMacroBinding(null, null, "old", null);
        binding.put("extra", "value");
        binding.setResult("oldResult");

        WikiMacroParameters parameters = new WikiMacroParameters();
        WikiMacroBinding replacement = new WikiMacroBinding(null, parameters, "new", null);

        binding.replaceAll(replacement);

        assertEquals(replacement, binding);
        assertEquals("new", binding.getContent());
        assertSame(parameters, binding.getParameters());
        assertNull(binding.getResult());
        assertNull(binding.get("extra"));
    }
}
