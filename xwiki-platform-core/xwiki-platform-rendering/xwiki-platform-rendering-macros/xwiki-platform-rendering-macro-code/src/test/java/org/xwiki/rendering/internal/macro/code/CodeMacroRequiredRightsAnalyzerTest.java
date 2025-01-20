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
package org.xwiki.rendering.internal.macro.code;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.code.CodeMacroParameters;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.xwiki.rendering.macro.source.MacroContentSourceReference.TYPE_SCRIPT;
import static org.xwiki.rendering.macro.source.MacroContentSourceReference.TYPE_STRING;

/**
 * Unit test for {@link CodeMacroRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class CodeMacroRequiredRightsAnalyzerTest
{
    @InjectMockComponents
    private CodeMacroRequiredRightsAnalyzer analyzer;

    @MockComponent
    private BeanManager beanManager;

    @Mock
    private MacroRequiredRightReporter reporter;

    @Mock
    private MacroBlock macroBlock;

    private void setupMock(MacroContentSourceReference source) throws PropertyException
    {
        doAnswer(invocation -> {
            CodeMacroParameters params = invocation.getArgument(0);
            params.setSource(source);
            return null;
        }).when(this.beanManager).populate(any(), anyMap());
    }

    @Test
    void analyzeWithScriptSource() throws PropertyException
    {
        setupMock(new MacroContentSourceReference(TYPE_SCRIPT, "script"));

        this.analyzer.analyze(this.macroBlock, this.reporter);

        verify(this.reporter).report(this.macroBlock, List.of(MacroRequiredRight.SCRIPT),
            "rendering.macro.code.requiredRights.scriptSource");
    }

    @ParameterizedTest
    @MethodSource("provideSourceReferences")
    @NullSource
    void analyzeWithSource(MacroContentSourceReference source) throws PropertyException
    {
        setupMock(source);

        this.analyzer.analyze(this.macroBlock, this.reporter);

        verifyNoInteractions(this.reporter);
    }

    private static Stream<MacroContentSourceReference> provideSourceReferences()
    {
        return Stream.of(
            new MacroContentSourceReference(TYPE_STRING, "non-script")
        );
    }

    @Test
    void analyzeWithPropertyException() throws PropertyException
    {
        doThrow(PropertyException.class).when(this.beanManager).populate(any(), anyMap());

        this.analyzer.analyze(this.macroBlock, this.reporter);

        verifyNoInteractions(this.reporter);
    }
}
