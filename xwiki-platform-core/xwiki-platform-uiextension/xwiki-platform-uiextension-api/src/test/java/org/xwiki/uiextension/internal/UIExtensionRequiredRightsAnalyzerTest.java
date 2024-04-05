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
package org.xwiki.uiextension.internal;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UIExtensionRequiredRightsAnalyzer}.
 */
@ComponentTest
class UIExtensionRequiredRightsAnalyzerTest
{
    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationBlockSupplierProvider;

    @MockComponent
    @Named("stringCode")
    private BlockSupplierProvider<String> stringBlockSupplierProvider;

    @MockComponent
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @MockComponent
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @MockComponent
    private ContentParser contentParser;

    @MockComponent
    private VelocityDetector velocityDetector;

    @InjectMockComponents
    private UIExtensionRequiredRightsAnalyzer analyzer;

    @Mock
    private XWikiDocument ownerDocument;

    @Mock
    private BaseObject baseObject;

    @Mock
    private BaseObjectReference objectReference;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xdomRequiredRightAnalyzer.analyze(any())).thenReturn(List.of());
        when(this.contentParser.parse(any(), any(), any())).thenReturn(new XDOM(List.of()));
        when(this.baseObject.getOwnerDocument()).thenReturn(this.ownerDocument);
        when(this.baseObject.getReference()).thenReturn(this.objectReference);
        when(this.baseObject.getStringValue(WikiUIExtensionConstants.EXTENSION_POINT_ID_PROPERTY)).thenReturn("");
        when(this.baseObject.getStringValue(WikiUIExtensionConstants.SCOPE_PROPERTY)).thenReturn("user");
    }

    @ParameterizedTest
    @EnumSource
    void analyzeScope(WikiComponentScope scope) throws Exception
    {
        when(this.baseObject.getStringValue(WikiUIExtensionConstants.SCOPE_PROPERTY))
            .thenReturn(scope.toString().toLowerCase());

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.baseObject);

        if (scope != WikiComponentScope.USER) {
            assertEquals(1, analysisResults.size());

            assertEquals(this.objectReference, analysisResults.get(0).getEntityReference());
            if (scope == WikiComponentScope.WIKI) {
                assertEquals(List.of(RequiredRight.WIKI_ADMIN), analysisResults.get(0).getRequiredRights());
            } else if (scope == WikiComponentScope.GLOBAL) {
                assertEquals(List.of(RequiredRight.PROGRAM), analysisResults.get(0).getRequiredRights());
            }

            verify(this.translationBlockSupplierProvider)
                .get("uiextension.requiredrights." + scope.toString().toLowerCase());
            verify(this.objectBlockSupplierProvider).get(this.baseObject);
        } else {
            assertEquals(0, analysisResults.size());
        }
    }

    @Test
    void analyzeAdminOnlyExtensionPoints() throws Exception
    {
        String extensionPoint = "org.xwiki.plaftorm.drawer.header";
        when(this.baseObject.getStringValue(WikiUIExtensionConstants.EXTENSION_POINT_ID_PROPERTY))
            .thenReturn(extensionPoint);
        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.baseObject);
        assertEquals(1, analysisResults.size());
        assertEquals(this.objectReference, analysisResults.get(0).getEntityReference());
        assertEquals(List.of(RequiredRight.WIKI_ADMIN), analysisResults.get(0).getRequiredRights());
        verify(this.translationBlockSupplierProvider).get("uiextension.requiredrights.adminOnly", extensionPoint);
        verify(this.objectBlockSupplierProvider).get(this.baseObject);
    }

    @Test
    void analyzeParameters() throws Exception
    {
        String parameters = "$velocity";
        when(this.baseObject.getStringValue(WikiUIExtensionConstants.PARAMETERS_PROPERTY)).thenReturn(parameters);
        when(this.velocityDetector.containsVelocityScript(anyString())).thenReturn(true);

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.baseObject);

        assertEquals(1, analysisResults.size());

        assertEquals(this.objectReference, analysisResults.get(0).getEntityReference());
        assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, analysisResults.get(0).getRequiredRights());

        verify(this.translationBlockSupplierProvider).get("uiextension.requiredrights.parameters");
        verify(this.stringBlockSupplierProvider).get(parameters);
    }

    @Test
    void analyzeContent() throws Exception
    {
        when(this.baseObject.getStringValue(WikiUIExtensionConstants.CONTENT_PROPERTY)).thenReturn("content");
        List<RequiredRightAnalysisResult> xdomResult = List.of(new RequiredRightAnalysisResult(mock(), mock(), mock(),
            List.of(RequiredRight.PROGRAM)));
        when(this.xdomRequiredRightAnalyzer.analyze(any())).thenReturn(xdomResult);
        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.baseObject);
        assertEquals(xdomResult, analysisResults);
    }

    @Test
    void analyzeContentWithError() throws Exception
    {
        String message = "error";
        String content = "content value";
        when(this.baseObject.getStringValue(WikiUIExtensionConstants.CONTENT_PROPERTY)).thenReturn(content);
        when(this.contentParser.parse(eq(content), any(), any())).thenThrow(new WikiComponentException(message));

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.baseObject);
        assertEquals(1, analysisResults.size());
        assertEquals(this.objectReference, analysisResults.get(0).getEntityReference());
        assertEquals(List.of(RequiredRight.MAYBE_PROGRAM), analysisResults.get(0).getRequiredRights());
        verify(this.translationBlockSupplierProvider).get("uiextension.requiredrights.contentError",
            "WikiComponentException: " + message);
        verify(this.stringBlockSupplierProvider).get(content);
    }
}
