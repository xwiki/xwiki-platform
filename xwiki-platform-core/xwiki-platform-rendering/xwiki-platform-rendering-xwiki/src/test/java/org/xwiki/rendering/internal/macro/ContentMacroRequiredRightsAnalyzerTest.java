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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.internal.macro.source.MacroContentSourceReferenceConverter;
import org.xwiki.rendering.internal.syntax.DefaultSyntaxRegistry;
import org.xwiki.rendering.internal.syntax.SyntaxConverter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ComponentTest
@ComponentList({ DefaultConverterManager.class, EnumConverter.class, ConvertUtilsConverter.class,
    SyntaxConverter.class, MacroContentSourceReferenceConverter.class, DefaultSyntaxRegistry.class })
// Adding constants for string literals wouldn't really help the readability of the test.
@SuppressWarnings("checkstyle:MultipleStringLiterals")
class ContentMacroRequiredRightsAnalyzerTest
{
    protected static final String XWIKI_2_0 = "xwiki/2.0";

    protected static final String XWIKI_2_1 = "xwiki/2.1";

    protected static final String INVALID = "invalid";

    @InjectMockComponents
    private ContentMacroRequiredRightsAnalyzer analyzer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Inject
    private SyntaxRegistry syntaxRegistry;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManager;

    @BeforeEach
    void beforeEach()
    {
        when(this.contextComponentManager.get()).thenReturn(this.componentManager);
        this.syntaxRegistry.registerSyntaxes(Syntax.XWIKI_2_0, Syntax.XWIKI_2_1);
    }

    @ParameterizedTest
    @MethodSource("provideDifferentContentSyntaxes")
    void analyzeDifferentContentSyntaxes(Map<String, String> parameters, List<Syntax> syntaxes)
    {
        String content = "Content";
        MacroBlock macroBlock = new MacroBlock("content", parameters, content, false);

        MacroRequiredRightReporter reporter = mock();

        this.analyzer.analyze(macroBlock, reporter);

        // Verify that the content is analyzed
        if (syntaxes.isEmpty()) {
            verify(reporter).analyzeContent(macroBlock, content);
        } else {
            for (Syntax syntax : syntaxes) {
                verify(reporter).analyzeContent(macroBlock, content, syntax);
            }
        }
    }

    @Test
    void analyzeWithScriptSource()
    {
        String content = "Content";
        MacroBlock macroBlock = new MacroBlock("content", Map.of("source", "script:variable"), content, false);

        MacroRequiredRightReporter reporter = mock();

        this.analyzer.analyze(macroBlock, reporter);

        verify(reporter).report(macroBlock, List.of(MacroRequiredRight.SCRIPT, MacroRequiredRight.MAYBE_PROGRAM),
            "rendering.macro.content.requiredRights.scriptSource");
    }

    @Test
    void analyzeWithStringSource()
    {
        String stringContent1 = "Content1";
        String stringContent2 = "Content2";
        MacroBlock macroBlock = new MacroBlock("content",
            Map.of("source", "string:" + stringContent1, "Source", "string:" + stringContent2,
                "SyntaX", XWIKI_2_0, "synTaX", XWIKI_2_1),
            "Content", false);

        MacroRequiredRightReporter reporter = mock();

        this.analyzer.analyze(macroBlock, reporter);

        for (Syntax syntax : List.of(Syntax.XWIKI_2_1, Syntax.XWIKI_2_0)) {
            for (String content : List.of(stringContent1, stringContent2)) {
                verify(reporter).analyzeContent(macroBlock, content, syntax);
            }
        }

        verifyNoMoreInteractions(reporter);
    }

    @Test
    void analyzeWithDifferentSourcesNoSyntax()
    {
        MacroBlock macroBlock = new MacroBlock("content", Map.of(
            "Source", "script:variable", "sourcE", "script:variable2",
            "souRCE", "string:1", "SOURCE", "string:2",
            "soUrce", "invalid:value"),
            false);

        MacroRequiredRightReporter reporter = mock();

        this.analyzer.analyze(macroBlock, reporter);

        verify(reporter, times(2))
            .report(macroBlock, List.of(MacroRequiredRight.SCRIPT, MacroRequiredRight.MAYBE_PROGRAM),
                "rendering.macro.content.requiredRights.scriptSource");

        verify(reporter).analyzeContent(macroBlock, "1");
        verify(reporter).analyzeContent(macroBlock, "2");
        verifyNoMoreInteractions(reporter);
    }

    private static Stream<Arguments> provideDifferentContentSyntaxes()
    {
        return Stream.of(
            Arguments.of(Map.of("syntax", XWIKI_2_0), List.of(Syntax.XWIKI_2_0)),
            Arguments.of(Map.of("Syntax", XWIKI_2_1), List.of(Syntax.XWIKI_2_1)),
            Arguments.of(Map.of("sYnTax", INVALID, "syntaX", XWIKI_2_1), List.of(Syntax.XWIKI_2_1)),
            Arguments.of(Map.of("syNtax", XWIKI_2_0, "synTax", XWIKI_2_1), List.of(Syntax.XWIKI_2_0,
                Syntax.XWIKI_2_1)),
            Arguments.of(Map.of("syntAX", INVALID), List.of()),
            Arguments.of(Map.of(), List.of())
        );
    }
}
