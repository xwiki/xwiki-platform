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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroIdFactory;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wysiwyg.internal.macro.MacroDescriptorUIFactory;
import org.xwiki.wysiwyg.macro.MacroDescriptorUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WysiwygEditorScriptService}.
 * 
 * @version $Id$
 * @since 17.5.0RC1
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

    @MockComponent
    @Named("html/5.0")
    private Parser htmlParser;
    
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

    @Test
    void getMacroParametersFromHTML() throws ParseException
    {
        String macroIdString = "macroId/syntax";
        String htmlFragment = "some html";
        when(this.macroIdFactory.createMacroId(macroIdString)).thenReturn(null);
        assertTrue(this.editorScriptService.getMacroParametersFromHTML(macroIdString, htmlFragment).isEmpty());

        MacroId macroId = mock(MacroId.class);
        when(this.macroIdFactory.createMacroId(macroIdString)).thenReturn(macroId);
        XDOM xdom = mock(XDOM.class);
        when(this.htmlParser.parse(any())).then(invocationOnMock -> {
            StringReader reader = invocationOnMock.getArgument(0);
            String line = new BufferedReader(reader).readLine();
            assertEquals("<html><body>some html</body></html>", line);
            return xdom;
        });
        MacroBlock macroBlock = mock(MacroBlock.class);
        when(xdom.getFirstBlock(any(), eq(Block.Axes.DESCENDANT))).then(invocationOnMock -> {
            assertInstanceOf(MacroBlockMatcher.class, invocationOnMock.getArgument(0));
            return macroBlock;
        });
        when(macroBlock.getParameters()).thenReturn(Map.of(
            "paramFoo", "valueFoo",
            "paramBar", "valueBar"
        ));
        when(macroBlock.getContent()).thenReturn("the content of macro");
        assertEquals(Map.of(
            "paramFoo", "valueFoo",
            "paramBar", "valueBar",
            "$content", "the content of macro"
        ), this.editorScriptService.getMacroParametersFromHTML(macroIdString, htmlFragment));
    }
}
