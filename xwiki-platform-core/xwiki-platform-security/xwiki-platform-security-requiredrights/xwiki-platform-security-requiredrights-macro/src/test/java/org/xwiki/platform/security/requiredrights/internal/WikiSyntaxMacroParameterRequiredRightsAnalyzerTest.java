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

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit test for {@link WikiSyntaxMacroParameterRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiSyntaxMacroParameterRequiredRightsAnalyzerTest
{
    @Mock
    private MacroRequiredRightReporter reporter;

    @Mock
    private MacroBlock macroBlock;

    @Mock
    private ParameterDescriptor parameterDescriptor;

    @InjectMockComponents
    private WikiSyntaxMacroParameterRequiredRightsAnalyzer analyzer;

    @Test
    void analyze()
    {
        String content = "content";
        this.analyzer.analyze(this.macroBlock, this.parameterDescriptor, content, this.reporter);

        verify(this.reporter).analyzeContent(this.macroBlock, content);
        verifyNoMoreInteractions(this.reporter);
    }
}
