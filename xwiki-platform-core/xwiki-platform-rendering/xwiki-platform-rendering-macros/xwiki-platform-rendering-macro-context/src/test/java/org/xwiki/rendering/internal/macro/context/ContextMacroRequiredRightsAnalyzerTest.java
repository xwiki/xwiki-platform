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
package org.xwiki.rendering.internal.macro.context;

import org.junit.jupiter.api.Test;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ContextMacroRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class ContextMacroRequiredRightsAnalyzerTest
{
    @InjectMockComponents
    private ContextMacroRequiredRightsAnalyzer analyzer;

    @MockComponent
    private BeanManager beanManager;

    @Test
    void analyzeWhenContentIsNotRestricted() throws PropertyException
    {
        MacroBlock macroBlock = mock(MacroBlock.class);
        String content = "content";
        when(macroBlock.getContent()).thenReturn(content);
        MacroRequiredRightReporter reporter = mock(MacroRequiredRightReporter.class);

        doAnswer(invocation -> {
            ContextMacroParameters parameters = invocation.getArgument(0);
            parameters.setRestricted(false);
            return null;
        }).when(this.beanManager).populate(any(), any());
        this.analyzer.analyze(macroBlock, reporter);

        verify(reporter).analyzeContent(macroBlock, content);
    }

    @Test
    void analyzeWhenContentIsRestricted() throws PropertyException
    {
        MacroBlock macroBlock = mock(MacroBlock.class);
        MacroRequiredRightReporter reporter = mock(MacroRequiredRightReporter.class);

        doAnswer(invocation -> {
            ContextMacroParameters parameters = invocation.getArgument(0);
            parameters.setRestricted(true);
            return null;
        }).when(this.beanManager).populate(any(), any());

        this.analyzer.analyze(macroBlock, reporter);

        verifyNoInteractions(reporter);
    }

    @Test
    void analyzeWhenPropertyExceptionOccurs() throws PropertyException
    {
        MacroBlock macroBlock = mock(MacroBlock.class);
        MacroRequiredRightReporter reporter = mock(MacroRequiredRightReporter.class);

        doThrow(PropertyException.class).when(this.beanManager).populate(any(), any());

        this.analyzer.analyze(macroBlock, reporter);

        verifyNoInteractions(reporter);
    }
}
