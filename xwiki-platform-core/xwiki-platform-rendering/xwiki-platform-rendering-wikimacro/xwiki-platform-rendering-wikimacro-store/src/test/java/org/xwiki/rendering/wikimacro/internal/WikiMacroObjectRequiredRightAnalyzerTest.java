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
package org.xwiki.rendering.wikimacro.internal;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.analyzer.ObjectPropertyRequiredRightAnalyzer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_VISIBILITY_PROPERTY;

/**
 * Unit tests for {@link WikiMacroObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiMacroObjectRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private WikiMacroObjectRequiredRightAnalyzer analyzer;

    @MockComponent
    private ObjectPropertyRequiredRightAnalyzer objectPropertyRequiredRightAnalyzer;

    @Test
    void analyzeGlobal() throws RequiredRightsException
    {
        BaseObject object = mockMacroObject("Global");
        List<RequiredRightAnalysisResult> results = mockResults();
        when(this.objectPropertyRequiredRightAnalyzer.analyzeAllPropertiesAndAddObjectResult(object,
            RequiredRight.PROGRAM, "rendering.wikimacro.requiredrights.global")).thenReturn(results);

        assertSame(results, this.analyzer.analyze(object));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "Current Wiki", "unknown" })
    void analyzeWiki(String visibility) throws RequiredRightsException
    {
        BaseObject object = mockMacroObject(visibility);
        List<RequiredRightAnalysisResult> results = mockResults();
        when(this.objectPropertyRequiredRightAnalyzer.analyzeAllPropertiesAndAddObjectResult(object,
            RequiredRight.WIKI_ADMIN, "rendering.wikimacro.requiredrights.wiki")).thenReturn(results);

        assertSame(results, this.analyzer.analyze(object));
    }

    @Test
    void analyzeUser() throws RequiredRightsException
    {
        BaseObject object = mockMacroObject("Current User");
        List<RequiredRightAnalysisResult> results = mockResults();
        when(this.objectPropertyRequiredRightAnalyzer.analyzeAllProperties(object)).thenReturn(results);

        assertSame(results, this.analyzer.analyze(object));
        verify(this.objectPropertyRequiredRightAnalyzer).analyzeAllProperties(object);
        verifyNoMoreInteractions(this.objectPropertyRequiredRightAnalyzer);
    }

    private static List<RequiredRightAnalysisResult> mockResults()
    {
        RequiredRightAnalysisResult result = mock();
        return List.of(result);
    }

    private static BaseObject mockMacroObject(String visibility)
    {
        BaseObject object = mock();
        when(object.getStringValue(MACRO_VISIBILITY_PROPERTY)).thenReturn(visibility);
        return object;
    }
}
