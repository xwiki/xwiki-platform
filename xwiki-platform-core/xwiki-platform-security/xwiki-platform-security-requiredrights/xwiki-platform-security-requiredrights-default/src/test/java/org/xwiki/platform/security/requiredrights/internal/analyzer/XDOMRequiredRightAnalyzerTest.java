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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XDOMRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class XDOMRequiredRightAnalyzerTest
{
    @MockComponent
    private RequiredRightAnalyzer<MacroBlock> defaultMacroBlockRequiredRightAnalyzer;

    @InjectMockComponents
    private XDOMRequiredRightAnalyzer analyzer;

    @Test
    void analyze() throws RequiredRightsException
    {
        MacroBlock macroBlock = new MacroBlock("macro", Map.of(), false);
        XDOM xdom = new XDOM(List.of(macroBlock));
        List<RequiredRightAnalysisResult> expected = List.of(mock(RequiredRightAnalysisResult.class));
        when(this.defaultMacroBlockRequiredRightAnalyzer.analyze(macroBlock)).thenReturn(expected);

        assertEquals(expected, this.analyzer.analyze(xdom));
    }
}
