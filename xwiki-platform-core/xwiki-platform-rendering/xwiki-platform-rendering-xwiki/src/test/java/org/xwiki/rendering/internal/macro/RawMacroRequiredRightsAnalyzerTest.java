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

import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.anyString;
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

    private static final String PLAIN_VALUE = "plain";

    private static final String HTML_VALUE = "html";

    @MockComponent
    private SyntaxRegistry syntaxRegistry;

    @InjectMockComponents
    private RawMacroRequiredRightsAnalyzer analyzer;

    @BeforeEach
    void setUp() throws ParseException
    {
        when(this.syntaxRegistry.resolveSyntax(anyString())).then(invocationOnMock -> {
            String syntaxValue = invocationOnMock.getArgument(0);
            return switch (syntaxValue) {
                case PLAIN_VALUE -> Syntax.PLAIN_1_0;
                case HTML_VALUE -> Syntax.HTML_5_0;
                default -> throw new ParseException("Unknown syntax: " + syntaxValue);
            };
        });
    }

    private static Stream<Arguments> analyzeTestCases()
    {
        return Stream.of(
            Arguments.of(Map.of(SYNTAX_PARAMETER, PLAIN_VALUE), null),
            Arguments.of(Map.of(SYNTAX_PARAMETER, HTML_VALUE), MacroRequiredRight.SCRIPT),
            Arguments.of(Map.of("sYnTaX", HTML_VALUE), MacroRequiredRight.SCRIPT),
            Arguments.of(Map.of("sYntax", HTML_VALUE, SYNTAX_PARAMETER, PLAIN_VALUE), MacroRequiredRight.SCRIPT),
            Arguments.of(Map.of("Syntax", PLAIN_VALUE, "SYNTAX", HTML_VALUE), MacroRequiredRight.SCRIPT),
            Arguments.of(Map.of("syntaX", PLAIN_VALUE), null)
        );
    }

    @ParameterizedTest
    @MethodSource("analyzeTestCases")
    void analyze(Map<String, String> parameters, MacroRequiredRight expectedRight)
    {
        MacroBlock macroBlock = new MacroBlock("raw", parameters, false);

        MacroRequiredRightReporter reporter = mock();
        this.analyzer.analyze(macroBlock, reporter);

        if (expectedRight != null) {
            verify(reporter).report(macroBlock, List.of(expectedRight), "rendering.macro.rawMacroRequiredRights");
        } else {
            verifyNoInteractions(reporter);
        }
    }
}
