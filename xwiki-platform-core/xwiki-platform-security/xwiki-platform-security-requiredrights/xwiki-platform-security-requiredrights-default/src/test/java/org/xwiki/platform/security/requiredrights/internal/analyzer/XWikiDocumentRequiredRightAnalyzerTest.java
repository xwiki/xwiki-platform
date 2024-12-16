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
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiDocumentRequiredRightAnalyzer}.
 * 
 * @version $Id$
 */
@ComponentTest
class XWikiDocumentRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private XWikiDocumentRequiredRightAnalyzer analyzer;

    @MockComponent
    private DocumentContextExecutor documentContextExecutor;

    @MockComponent
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @MockComponent
    private RequiredRightAnalyzer<BaseObject> objectRequiredRightAnalyzer;

    @MockComponent
    private RequiredRightAnalyzer<BaseClass> baseClassRequiredRightAnalyzer;

    @MockComponent
    private VelocityDetector velocityDetector;

    @Test
    void analyze() throws Exception
    {
        when(this.documentContextExecutor.call(any(), any())).thenAnswer(invocation -> {
            Callable<List<RequiredRightAnalysisResult>> callable = invocation.getArgument(0);
            return callable.call();
        });

        XWikiDocument document = mock();
        when(document.getTitle()).thenReturn("title");
        DocumentReference mockDocumentReference = mock();
        when(document.getDocumentReferenceWithLocale()).thenReturn(mockDocumentReference);
        BaseObject object = mock();
        when(document.getXObjects()).thenReturn(Map.of(mock(), List.of(object)));

        BaseClass baseClass = mock();
        when(document.getXClass()).thenReturn(baseClass);

        XDOM xdom = mock();
        when(document.getXDOM()).thenReturn(xdom);
        RequiredRightAnalysisResult xdomResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(xdom)).thenReturn(List.of(xdomResult));

        RequiredRightAnalysisResult objectResult = mock();
        when(this.objectRequiredRightAnalyzer.analyze(object)).thenReturn(List.of(objectResult));

        RequiredRightAnalysisResult baseClassResult = mock();
        when(this.baseClassRequiredRightAnalyzer.analyze(baseClass)).thenReturn(List.of(baseClassResult));

        assertEquals(List.of(xdomResult, baseClassResult, objectResult), this.analyzer.analyze(document));
    }

    @Test
    void analyzeTitle() throws Exception
    {
        when(this.documentContextExecutor.call(any(), any())).thenAnswer(invocation -> {
            Callable<List<RequiredRightAnalysisResult>> callable = invocation.getArgument(0);
            return callable.call();
        });

        XWikiDocument document = mock();
        String title = "#set($foo = 'bar')";
        when(document.getTitle()).thenReturn(title);
        DocumentReference mockDocumentReference = mock();
        when(document.getDocumentReferenceWithLocale()).thenReturn(mockDocumentReference);
        when(document.getXObjects()).thenReturn(Map.of());

        XDOM xdom = mock();
        when(document.getXDOM()).thenReturn(xdom);
        when(this.xdomRequiredRightAnalyzer.analyze(xdom)).thenReturn(List.of());

        when(this.velocityDetector.containsVelocityScript(title)).thenReturn(true);

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(document);
        assertEquals(1, analysisResults.size());
        RequiredRightAnalysisResult analysisResult = analysisResults.get(0);
        assertEquals(mockDocumentReference, analysisResult.getEntityReference());
        assertEquals(List.of(RequiredRight.MAYBE_SCRIPT, RequiredRight.MAYBE_PROGRAM),
            analysisResult.getRequiredRights());
    }
}
