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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMacroBlockRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class DefaultMacroBlockRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private DefaultMacroBlockRequiredRightAnalyzer analyzer;

    @MockComponent
    @Named("macro/testmacro")
    private RequiredRightAnalyzer<MacroBlock> mockMacroAnalyzer;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

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
}
