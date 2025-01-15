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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.internal.analyzer.ObjectPropertyRequiredRightAnalyzer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_DEFAULT_VALUE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_TYPE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_TYPE_WIKI;

/**
 * Unit tests for {@link WikiMacroParameterObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiMacroParameterObjectRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private WikiMacroParameterObjectRequiredRightAnalyzer analyzer;

    @MockComponent
    private ObjectPropertyRequiredRightAnalyzer propertyRequiredRightAnalyzer;

    @Mock
    private BaseObject baseObject;

    @Mock
    private PropertyInterface propertyInterface;

    @ParameterizedTest
    @ValueSource(strings = { PARAMETER_TYPE_WIKI, "java.util.List<org.xwiki.rendering.block.Block>" })
    void analyzeWithWikiParameterType(String type) throws Exception
    {
        when(this.baseObject.getStringValue(PARAMETER_TYPE_PROPERTY)).thenReturn(type);
        String defaultValue = "wiki content";
        when(this.baseObject.getStringValue(PARAMETER_DEFAULT_VALUE_PROPERTY)).thenReturn(defaultValue);
        when(this.baseObject.getField(PARAMETER_DEFAULT_VALUE_PROPERTY)).thenReturn(this.propertyInterface);
        RequiredRightAnalysisResult allPropertiesResult = mock();
        when(this.propertyRequiredRightAnalyzer.analyzeAllProperties(this.baseObject))
            .thenReturn(List.of(allPropertiesResult));
        RequiredRightAnalysisResult wikiContentResult = mock();
        when(this.propertyRequiredRightAnalyzer.analyzeWikiContent(this.baseObject, defaultValue,
            this.propertyInterface.getReference())).thenReturn(List.of(wikiContentResult));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.baseObject);

        assertEquals(List.of(allPropertiesResult, wikiContentResult), results);
        verify(this.propertyRequiredRightAnalyzer).analyzeAllProperties(this.baseObject);
        verify(this.propertyRequiredRightAnalyzer).analyzeWikiContent(this.baseObject, defaultValue,
            this.propertyInterface.getReference());
    }

    @ParameterizedTest
    @ValueSource(strings = { "java.util.List", "unknown" })
    @NullSource
    void analyzeWithNonWikiParameterType(String parameterType) throws Exception
    {
        when(this.baseObject.getStringValue(PARAMETER_TYPE_PROPERTY)).thenReturn(parameterType);
        RequiredRightAnalysisResult result = mock();
        when(this.propertyRequiredRightAnalyzer.analyzeAllProperties(this.baseObject)).thenReturn(List.of(result));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.baseObject);

        assertEquals(List.of(result), results);
        verify(this.propertyRequiredRightAnalyzer).analyzeAllProperties(this.baseObject);
        verify(this.propertyRequiredRightAnalyzer, never()).analyzeWikiContent(any(), any(), any());
        verify(this.baseObject, never()).getStringValue(PARAMETER_DEFAULT_VALUE_PROPERTY);
    }
}
