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
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RawMacroRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class RawMacroRequiredRightsAnalyzerTest
{
    private static final String SYNTAX_PARAMETER = "syntax";

    @MockComponent
    private SyntaxRegistry syntaxRegistry;

    @InjectMockComponents
    private RawMacroRequiredRightsAnalyzer analyzer;

    private static Stream<Arguments> analyzeTestCases()
    {
        return Stream.of(
            Arguments.of("plain", Syntax.PLAIN_1_0, null),
            Arguments.of("html", Syntax.HTML_5_0, MacroRequiredRight.SCRIPT)
        );
    }

    @ParameterizedTest
    @MethodSource("analyzeTestCases")
    void analyze(String syntaxValue, Syntax expectedSyntax, MacroRequiredRight expectedRight) throws ParseException
    {
        when(this.syntaxRegistry.resolveSyntax(syntaxValue)).thenReturn(expectedSyntax);

        MacroBlock macroBlock = mock();
        when(macroBlock.getParameter(SYNTAX_PARAMETER)).thenReturn(syntaxValue);

        MacroRequiredRightReporter reporter = mock();
        this.analyzer.analyze(macroBlock, reporter);

        verify(this.syntaxRegistry).resolveSyntax(syntaxValue);
        if (expectedRight != null) {
            verify(reporter).report(macroBlock, List.of(expectedRight), "rendering.macro.rawMacroRequiredRights");
        } else {
            verifyNoInteractions(reporter);
        }
    }
}
