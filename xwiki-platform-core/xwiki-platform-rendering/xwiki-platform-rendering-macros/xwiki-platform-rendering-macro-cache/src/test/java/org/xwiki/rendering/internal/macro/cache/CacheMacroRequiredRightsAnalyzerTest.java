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

import org.junit.jupiter.api.Test;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void analyze()
    {
        MacroBlock macroBlock = mock();
        String idValue = "idValue";
        String contentValue = "contentValue";
        when(macroBlock.getParameter("id")).thenReturn(idValue);
        when(macroBlock.getContent()).thenReturn(contentValue);
        MacroRequiredRightReporter reporter = mock();

        this.analyzer.analyze(macroBlock, reporter);

        verify(reporter).analyzeContent(macroBlock, idValue);
        verify(reporter).analyzeContent(macroBlock, contentValue);
    }
}
