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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.platform.security.requiredrights.MacroParameterRequiredRightsAnalyzer;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightsAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.script.ScriptMacro;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link DefaultMacroBlockRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(DefaultMacroRequiredRightReporter.class)
class DefaultMacroBlockRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private DefaultMacroBlockRequiredRightAnalyzer analyzer;

    @MockComponent
    @Named("testmacro")
    private RequiredRightAnalyzer<MacroBlock> mockMacroAnalyzer;

    @MockComponent
    @Named(ScriptMacroAnalyzer.ID)
    private RequiredRightAnalyzer<MacroBlock> scriptMacroAnalyzer;

    @MockComponent
    private MacroRequiredRightsAnalyzer defaultMacroRequiredRightsAnalyzer;

    @MockComponent
    private MacroManager macroManager;

    @MockComponent
    private MacroContentParser macroContentParser;

    @MockComponent
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @MockComponent
    private MacroParameterRequiredRightsAnalyzer<List<Block>> listBlockParameterAnalyzer;

    @Test
    void analyzeWithCustomAnalyzer() throws Exception
    {
        MacroBlock block = mock();
        when(block.getId()).thenReturn("testmacro");
        RequiredRightAnalysisResult mockResult = mock();
        when(this.mockMacroAnalyzer.analyze(block)).thenReturn(List.of(mockResult));

        List<RequiredRightAnalysisResult> result = this.analyzer.analyze(block);

        verify(this.mockMacroAnalyzer).analyze(block);
        assertEquals(List.of(mockResult), result);
    }

    @Test
    void analyzeWithScriptMacroAnalyzer() throws Exception
    {
        String scriptMacroName = "myScript";

        MacroBlock block = mock();
        when(block.getId()).thenReturn(scriptMacroName);

        // Create a fake syntax to create the macro id.
        Syntax syntax = mock();
        when(block.getSyntaxMetadata()).thenReturn(Optional.of(syntax));
        MacroId macroId = new MacroId(scriptMacroName, syntax);

        // Mock a script macro.
        Macro<?> scriptMacro = mock(withSettings().extraInterfaces(ScriptMacro.class));
        // Use doReturn() as Mockito has problems with generics.
        doReturn(scriptMacro).when(this.macroManager).getMacro(macroId);
        // Mock the actual analysis.
        RequiredRightAnalysisResult mockResult = mock();
        when(this.scriptMacroAnalyzer.analyze(same(block))).thenReturn(List.of(mockResult));

        List<RequiredRightAnalysisResult> result = this.analyzer.analyze(block);

        // Ensure that the script macro analyzer was called.
        verify(this.scriptMacroAnalyzer).analyze(same(block));
        assertEquals(List.of(mockResult), result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void analyzeContentRecursively(boolean isWikiContent) throws Exception
    {
        String macroName = "wikimacro";
        String testContent = "TEST_CONTENT";
        Map<String, String> parameters = Map.of("key", "parameter");
        MacroBlock block = new MacroBlock(macroName, parameters, testContent, false);

        // Create an XDOM block that acts as root.
        MetaData metaDataOuter = new MetaData();
        metaDataOuter.addMetaData(MetaData.SOURCE, "xwiki:Space.Page");

        // Create a fake syntax to create the macro id.
        Syntax syntax = mock();
        metaDataOuter.addMetaData(MetaData.SYNTAX, syntax);
        MacroId macroId = new MacroId(macroName, syntax);
        new XDOM(List.of(block), metaDataOuter);

        // Mock the macro.
        Macro<?> macro = mock();
        doReturn(macro).when(this.macroManager).getMacro(macroId);
        // Mock a macro descriptor that says that the macro content is wiki syntax.
        MacroDescriptor macroDescriptor = mock();
        when(macro.getDescriptor()).thenReturn(macroDescriptor);
        ContentDescriptor contentDescriptor = mock();
        when(contentDescriptor.getType()).thenReturn(isWikiContent ? Block.LIST_BLOCK_TYPE : String.class);
        when(macroDescriptor.getContentDescriptor()).thenReturn(contentDescriptor);

        XDOM xdom = mock();
        when(xdom.getMetaData()).thenReturn(metaDataOuter);
        when(this.macroContentParser.parse(eq(testContent), any(), any(), eq(false), eq(metaDataOuter), eq(false)))
            .thenReturn(xdom);

        RequiredRightAnalysisResult mockResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(xdom)).thenReturn(List.of(mockResult));

        List<RequiredRightAnalysisResult> result = this.analyzer.analyze(block);

        if (isWikiContent) {
            verify(this.macroContentParser).parse(eq(testContent), isNull(Syntax.class), any(), eq(false),
                eq(metaDataOuter), eq(false));
            verify(this.xdomRequiredRightAnalyzer).analyze(xdom);
            assertEquals(List.of(mockResult), result);
        } else {
            verifyNoInteractions(this.macroContentParser);
            verifyNoInteractions(this.xdomRequiredRightAnalyzer);
            assertEquals(List.of(), result);
        }
    }

    @Test
    void analyzeDefaultMacro()
    {
        String macroName = "default";

        MacroBlock block = mock();
        when(block.getId()).thenReturn(macroName);

        doAnswer(invocationOnMock -> {
            MacroBlock macroBlock = invocationOnMock.getArgument(0);
            MacroRequiredRightReporter reporter = invocationOnMock.getArgument(1);
            reporter.report(macroBlock, List.of(MacroRequiredRight.PROGRAM), "test.summary");
            return null;
        }).when(this.defaultMacroRequiredRightsAnalyzer).analyze(eq(block), any());

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(block);
        assertEquals(1, results.size());
        RequiredRightAnalysisResult result = results.get(0);
        assertEquals(List.of(RequiredRight.PROGRAM), result.getRequiredRights());
    }

    @Test
    void analyzeWikiParameters() throws Exception
    {
        String macroName = "wikiParameter";

        String wikiValue = "wikiValue";
        String wikiValue2 = "WikiValue";
        String wikiDisplay = "WikiDisplay";
        List<String> wikiValues = List.of(wikiValue, wikiValue2, wikiDisplay);

        // Use a LinkedHashMap to ensure that the order of the parameters is preserved.
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("wiki", wikiValue);
        parameters.put("Wiki", wikiValue2);
        parameters.put("wikiDisplaY", wikiDisplay);
        parameters.put("string", "String");
        parameters.put("other", "other value");
        MacroBlock block = new MacroBlock(macroName, parameters, false);

        // Create a fake syntax to create the macro id.
        Syntax syntax = mock();
        MacroId macroId = new MacroId(macroName, syntax);

        // Mock the macro.
        Macro<?> macro = mock();
        doReturn(macro).when(this.macroManager).getMacro(argThat(macroIdArg -> macroName.equals(macroIdArg.getId())));

        // Mock the macro descriptor.
        MacroDescriptor macroDescriptor = mock();
        when(macro.getDescriptor()).thenReturn(macroDescriptor);
        when(macroDescriptor.getId()).thenReturn(macroId);
        Map<String, ParameterDescriptor> parameterDescriptorMap = Map.of(
            "wiki", getParameterDescriptor("wiki", Block.LIST_BLOCK_TYPE, String.class),
            "wikidisplay", getParameterDescriptor("wikiDisplay", String.class, Block.LIST_BLOCK_TYPE),
            "string", getParameterDescriptor("string", String.class, String.class));
        when(macroDescriptor.getParameterDescriptorMap()).thenReturn(parameterDescriptorMap);
        when(macroDescriptor.getContentDescriptor()).thenReturn(new DefaultContentDescriptor(false));

        doAnswer(invocationOnMock -> {
            MacroBlock macroBlock = invocationOnMock.getArgument(0);
            ParameterDescriptor parameterDescriptor = invocationOnMock.getArgument(1);
            String value = invocationOnMock.getArgument(2);
            MacroRequiredRightReporter reporter = invocationOnMock.getArgument(3);
            assertSame(parameterDescriptorMap.get(parameterDescriptor.getId().toLowerCase()), parameterDescriptor);
            reporter.analyzeContent(macroBlock, value);
            return null;
        }).when(this.listBlockParameterAnalyzer).analyze(eq(block), any(), any(), any());

        // Stub the macro content parser and the XDOM analyzer to simply pass on the analyzed content as mock name.
        // That way, we can easily verify that all parameters were analyzed.
        when(this.macroContentParser.parse(any(), any(), any(), anyBoolean(), any(), anyBoolean()))
            .then(invocationOnMock -> {
                String content = invocationOnMock.getArgument(0);
                return mock(XDOM.class, content);
            });

        when(this.xdomRequiredRightAnalyzer.analyze(any())).then(invocationOnMock -> {
            XDOM xdom = invocationOnMock.getArgument(0);
            return List.of(mock(RequiredRightAnalysisResult.class, getMockName(xdom)));
        });

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(block);

        List<String> analyzedValues = results.stream().map(this::getMockName).toList();

        assertEquals(wikiValues, analyzedValues);
    }

    private static ParameterDescriptor getParameterDescriptor(String id, Type parameterType, Type displayType)
    {
        ParameterDescriptor wikiParameterDescriptor = mock();
        when(wikiParameterDescriptor.getParameterType()).thenReturn(parameterType);
        when(wikiParameterDescriptor.getDisplayType()).thenReturn(displayType);
        when(wikiParameterDescriptor.getId()).thenReturn(id);
        return wikiParameterDescriptor;
    }

    private String getMockName(Object mockObject)
    {
        return mockingDetails(mockObject).getMockCreationSettings().getMockName().toString();
    }
}
