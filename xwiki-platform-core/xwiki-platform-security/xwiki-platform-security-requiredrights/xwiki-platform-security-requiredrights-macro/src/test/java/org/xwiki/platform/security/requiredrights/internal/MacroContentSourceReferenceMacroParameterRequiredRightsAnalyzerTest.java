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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.properties.converter.Converter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link MacroContentSourceReferenceMacroParameterRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class MacroContentSourceReferenceMacroParameterRequiredRightsAnalyzerTest
{
    private static final String MACRO_ID = "macroId";

    private static final String PARAMETER_ID = "parameterId";

    @MockComponent
    private Converter<MacroContentSourceReference> macroContentSourceReferenceConverter;

    @Mock
    private MacroRequiredRightReporter reporter;

    @Mock
    private MacroBlock macroBlock;

    @Mock
    private ParameterDescriptor parameterDescriptor;

    @InjectMockComponents
    private MacroContentSourceReferenceMacroParameterRequiredRightsAnalyzer analyzer;

    @BeforeEach
    void setUp()
    {
        when(this.macroBlock.getId()).thenReturn(MACRO_ID);
        when(this.parameterDescriptor.getId()).thenReturn(PARAMETER_ID);
        when(this.macroContentSourceReferenceConverter.convert(eq(MacroContentSourceReference.class), anyString()))
            .then(invocation -> {
                String[] values = StringUtils.split(invocation.getArgument(1), ":", 2);
                return new MacroContentSourceReference(values[0], values[1]);
            });
    }

    @Test
    void analyzeReportsScriptRightForScriptType()
    {
        String value = "script:someScript";

        this.analyzer.analyze(this.macroBlock, this.parameterDescriptor, value, this.reporter);

        verify(this.reporter).report(this.macroBlock, List.of(MacroRequiredRight.SCRIPT),
            "security.requiredrights.macro.scriptContentSource",
            PARAMETER_ID, MACRO_ID);
    }

    @Test
    void analyzeDoesNotReportScriptRightForNonScriptType()
    {
        String value = "other:someContent";

        this.analyzer.analyze(this.macroBlock, this.parameterDescriptor, value, this.reporter);

        verifyNoInteractions(this.reporter);
    }
}
