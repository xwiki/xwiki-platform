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
package org.xwiki.rendering.internal.macro.cache;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CacheMacroRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class CacheMacroRequiredRightsAnalyzerTest
{
    @InjectMockComponents
    private CacheMacroRequiredRightsAnalyzer analyzer;

    @ParameterizedTest
    @MethodSource("idValuesProvider")
    void analyze(String parameterName)
    {
        String idValue = "idValue";
        String contentValue = "contentValue";
        MacroBlock macroBlock = new MacroBlock("cache", Map.of(parameterName, idValue), contentValue, false);
        MacroRequiredRightReporter reporter = mock();

        this.analyzer.analyze(macroBlock, reporter);

        verify(reporter).analyzeContent(macroBlock, idValue);
        verify(reporter).analyzeContent(macroBlock, contentValue);
    }

    @Test
    void analyzeAllInOne()
    {
        Map<String, String> parameters = idValuesProvider().collect(Collectors.toMap(Function.identity(),
            Function.identity()));
        String content = "content";
        MacroBlock macroBlock = new MacroBlock("cache", parameters, content, false);
        MacroRequiredRightReporter reporter = mock();

        this.analyzer.analyze(macroBlock, reporter);

        verify(reporter).analyzeContent(macroBlock, content);
        for (String value : parameters.values()) {
            verify(reporter).analyzeContent(macroBlock, value);
        }
    }

    static Stream<String> idValuesProvider()
    {
        return Stream.of("id", "ID", "Id", "iD");
    }
}
