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
package org.xwiki.platform.security.requiredrights.internal.display;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.rendering.internal.parser.plain.PlainTextStreamParser;
import org.xwiki.rendering.internal.renderer.event.EventBlockRenderer;
import org.xwiki.rendering.internal.renderer.event.EventRenderer;
import org.xwiki.rendering.internal.renderer.event.EventRendererFactory;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MacroDisplayerProvider}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    EventBlockRenderer.class,
    EventRendererFactory.class,
    EventRenderer.class,
    PlainTextBlockParser.class,
    PlainTextStreamParser.class
})
class MacroDisplayerProviderTest
{
    @Inject
    @Named("event/1.0")
    private BlockRenderer eventRenderer;

    @InjectMockComponents
    private MacroDisplayerProvider provider;

    @MockComponent
    private MacroManager macroManager;

    @MockComponent
    private MacroContentParser macroContentParser;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @BeforeEach
    void setUp()
    {
        when(this.renderingContext.getDefaultSyntax()).thenReturn(Syntax.XWIKI_2_1);
    }

    @Test
    void getWithMacroDescriptor() throws MacroLookupException
    {
        String existingName = "existingParameterName";
        String nonExistingParameterName = "nonExistingParameterName";

        MacroDescriptor macroDescriptor = mock();
        ParameterDescriptor existingParameterDescriptor = mock();
        when(existingParameterDescriptor.getName()).thenReturn("Name from Macro Descriptor");
        when(existingParameterDescriptor.getDescription()).thenReturn("Parameter description from Macro Descriptor");
        when(macroDescriptor.getParameterDescriptorMap()).thenReturn(
            Map.of(existingName, existingParameterDescriptor)
        );
        ContentDescriptor contentDescriptor = mock();
        when(contentDescriptor.getDescription()).thenReturn("Content description from Macro Descriptor");
        when(macroDescriptor.getContentDescriptor()).thenReturn(contentDescriptor);
        Macro<?> macro = mock();
        when(macro.getDescriptor()).thenReturn(macroDescriptor);

        Syntax mockSyntax = mock();
        when(this.macroContentParser.getCurrentSyntax(any())).thenReturn(mockSyntax);

        String macroId = "testMacro";
        doReturn(macro).when(this.macroManager).getMacro(new MacroId(macroId, mockSyntax));

        String macroContent = "#TestMacro";

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(existingName, "existingValue");
        parameters.put(nonExistingParameterName, "nonExistingValue");
        MacroBlock macroBlock = new MacroBlock(macroId, parameters, macroContent, false);

        Block resultBlock = this.provider.get(macroBlock).get();

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.eventRenderer.render(resultBlock, printer);

        String expectedOutput = "beginDefinitionList [[class]=[xform]]\n"
            + "beginDefinitionTerm\n"
            + "onWord [Name]\n"
            + "onSpace\n"
            + "onWord [from]\n"
            + "onSpace\n"
            + "onWord [Macro]\n"
            + "onSpace\n"
            + "onWord [Descriptor]\n"
            + "beginFormat [NONE] [[class]=[xHint]]\n"
            + "onWord [Parameter]\n"
            + "onSpace\n"
            + "onWord [description]\n"
            + "onSpace\n"
            + "onWord [from]\n"
            + "onSpace\n"
            + "onWord [Macro]\n"
            + "onSpace\n"
            + "onWord [Descriptor]\n"
            + "endFormat [NONE] [[class]=[xHint]]\n"
            + "endDefinitionTerm\n"
            + "beginDefinitionDescription\n"
            + "beginGroup [[class]=[code box]]\n"
            + "onWord [existingValue]\n"
            + "endGroup [[class]=[code box]]\n"
            + "endDefinitionDescription\n"
            + "beginDefinitionTerm\n"
            + "onWord [nonExistingParameterName]\n"
            + "endDefinitionTerm\n"
            + "beginDefinitionDescription\n"
            + "beginGroup [[class]=[code box]]\n"
            + "onWord [nonExistingValue]\n"
            + "endGroup [[class]=[code box]]\n"
            + "endDefinitionDescription\n"
            + "beginDefinitionTerm\n"
            + "onWord [Content]\n"
            + "beginFormat [NONE] [[class]=[xHint]]\n"
            + "onWord [Content]\n"
            + "onSpace\n"
            + "onWord [description]\n"
            + "onSpace\n"
            + "onWord [from]\n"
            + "onSpace\n"
            + "onWord [Macro]\n"
            + "onSpace\n"
            + "onWord [Descriptor]\n"
            + "endFormat [NONE] [[class]=[xHint]]\n"
            + "endDefinitionTerm\n"
            + "beginDefinitionDescription\n"
            + "beginGroup [[class]=[code box]]\n"
            + "onSpecialSymbol [#]\n"
            + "onWord [TestMacro]\n"
            + "endGroup [[class]=[code box]]\n"
            + "endDefinitionDescription\n"
            + "endDefinitionList [[class]=[xform]]\n";
        assertEquals(expectedOutput, printer.toString());
    }

    /**
     * Test that get works also when there is no macro descriptor for the given macro.
     */
    @Test
    void getWithoutMacroDescriptor() throws MacroLookupException
    {
        String macroId = "testMacro";
        String macroContent = "#TestMacro";

        MacroBlock macroBlock = new MacroBlock(macroId, Map.of("parameterName", "parameterValue"), macroContent, false);

        when(this.macroManager.getMacro(any())).thenThrow(new MacroLookupException(""));

        Block resultBlock = this.provider.get(macroBlock).get();

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.eventRenderer.render(resultBlock, printer);

        String expectedOutput = "beginDefinitionList [[class]=[xform]]\n"
            + "beginDefinitionTerm\n"
            + "onWord [parameterName]\n"
            + "endDefinitionTerm\n"
            + "beginDefinitionDescription\n"
            + "beginGroup [[class]=[code box]]\n"
            + "onWord [parameterValue]\n"
            + "endGroup [[class]=[code box]]\n"
            + "endDefinitionDescription\n"
            + "beginDefinitionTerm\n"
            + "onWord [Content]\n"
            + "endDefinitionTerm\n"
            + "beginDefinitionDescription\n"
            + "beginGroup [[class]=[code box]]\n"
            + "onSpecialSymbol [#]\n"
            + "onWord [TestMacro]\n"
            + "endGroup [[class]=[code box]]\n"
            + "endDefinitionDescription\n"
            + "endDefinitionList [[class]=[xform]]\n";

        assertEquals(expectedOutput, printer.toString());
    }

    /**
     * Test that get uses translations.
     */
    @Test
    void getWithTranslations() throws MacroLookupException
    {
        String macroId = "testMacro";
        String macroContent = "#TestMacro";

        MacroBlock macroBlock = new MacroBlock(macroId, Map.of("parameterName", "parameterValue"), macroContent, false);

        when(this.macroManager.getMacro(any())).thenThrow(new MacroLookupException(""));

        when(this.localizationManager.getTranslationPlain("rendering.macro.testMacro.parameter.parameterName.name"))
            .thenReturn("TranslatedParameter");
        when(this.localizationManager.getTranslationPlain("rendering.macro.testMacro.parameter.parameterName.description"))
            .thenReturn("Translated parameter description.");
        when(this.localizationManager.getTranslationPlain("rendering.macroContent")).thenReturn("TranslatedContent");
        when(this.localizationManager.getTranslationPlain("rendering.macro.testMacro.content.description"))
            .thenReturn("Translated content description.");

        Block resultBlock = this.provider.get(macroBlock).get();

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.eventRenderer.render(resultBlock, printer);

        String expectedOutput = "beginDefinitionList [[class]=[xform]]\n"
            + "beginDefinitionTerm\n"
            + "onWord [TranslatedParameter]\n"
            + "beginFormat [NONE] [[class]=[xHint]]\n"
            + "onWord [Translated]\n"
            + "onSpace\n"
            + "onWord [parameter]\n"
            + "onSpace\n"
            + "onWord [description]\n"
            + "onSpecialSymbol [.]\n"
            + "endFormat [NONE] [[class]=[xHint]]\n"
            + "endDefinitionTerm\n"
            + "beginDefinitionDescription\n"
            + "beginGroup [[class]=[code box]]\n"
            + "onWord [parameterValue]\n"
            + "endGroup [[class]=[code box]]\n"
            + "endDefinitionDescription\n"
            + "beginDefinitionTerm\n"
            + "onWord [TranslatedContent]\n"
            + "beginFormat [NONE] [[class]=[xHint]]\n"
            + "onWord [Translated]\n"
            + "onSpace\n"
            + "onWord [content]\n"
            + "onSpace\n"
            + "onWord [description]\n"
            + "onSpecialSymbol [.]\n"
            + "endFormat [NONE] [[class]=[xHint]]\n"
            + "endDefinitionTerm\n"
            + "beginDefinitionDescription\n"
            + "beginGroup [[class]=[code box]]\n"
            + "onSpecialSymbol [#]\n"
            + "onWord [TestMacro]\n"
            + "endGroup [[class]=[code box]]\n"
            + "endDefinitionDescription\n"
            + "endDefinitionList [[class]=[xform]]\n";

        assertEquals(expectedOutput, printer.toString());
    }
}
