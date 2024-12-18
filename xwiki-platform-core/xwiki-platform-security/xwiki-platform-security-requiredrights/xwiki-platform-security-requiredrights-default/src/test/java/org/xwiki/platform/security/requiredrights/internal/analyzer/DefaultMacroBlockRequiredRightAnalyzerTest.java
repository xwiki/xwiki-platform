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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.script.ScriptMacro;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
}
