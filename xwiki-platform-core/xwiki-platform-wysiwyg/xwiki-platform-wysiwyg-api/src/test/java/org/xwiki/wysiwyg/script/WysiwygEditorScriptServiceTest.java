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
package org.xwiki.wysiwyg.script;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroIdFactory;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wysiwyg.internal.macro.MacroDescriptorUIFactory;
import org.xwiki.wysiwyg.macro.MacroDescriptorUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WysiwygEditorScriptService}.
 * 
 * @version $Id$
 * @since 17.5.0
 */
@ComponentTest
class WysiwygEditorScriptServiceTest
{
    @InjectMockComponents
    private WysiwygEditorScriptService editorScriptService;

    @MockComponent
    private MacroIdFactory macroIdFactory;

    @MockComponent
    private MacroManager macroManager;

    @MockComponent
    private MacroDescriptorUIFactory macroDescriptorUIFactory;

    @Test
    void getMacroDescriptorUI() throws ParseException, MacroLookupException
    {
        String macroIdString = "macroId";
        when(this.macroIdFactory.createMacroId(macroIdString)).thenReturn(null);
        assertNull(this.editorScriptService.getMacroDescriptorUI(macroIdString));

        MacroId macroId = mock(MacroId.class);
        when(this.macroIdFactory.createMacroId(macroIdString)).thenReturn(macroId);
        when(this.macroManager.exists(macroId, true)).thenReturn(false);
        assertNull(this.editorScriptService.getMacroDescriptorUI(macroIdString));

        when(this.macroManager.exists(macroId, true)).thenReturn(true);
        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);
        Macro macro = mock(Macro.class);
        when(macro.getDescriptor()).thenReturn(macroDescriptor);
        when(this.macroManager.getMacro(macroId)).thenReturn(macro);
        MacroDescriptorUI macroDescriptorUI = mock(MacroDescriptorUI.class);
        when(this.macroDescriptorUIFactory.buildMacroDescriptorUI(macroDescriptor)).thenReturn(macroDescriptorUI);
        assertEquals(macroDescriptorUI, this.editorScriptService.getMacroDescriptorUI(macroIdString));
    }
}
