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

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({ VelocityDetector.class })
class DefaultObjectRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private DefaultObjectRequiredRightAnalyzer analyzer;

    @MockComponent
    @Named("XWiki.TestClass")
    private RequiredRightAnalyzer<BaseObject> mockAnalyzer;

    @MockComponent
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Inject
    private ContentParser contentParser;

    @Test
    void analyzeWithCustomAnalyzer() throws XWikiException, RequiredRightsException
    {
        XWikiDocument testDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject testObject = testDocument.newXObject(new DocumentReference("wiki", "XWiki", "TestClass"),
            this.oldcore.getXWikiContext());

        RequiredRightAnalysisResult mockResult = mock();
        when(this.mockAnalyzer.analyze(testObject)).thenReturn(List.of(mockResult));
        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(testObject);

        assertEquals(List.of(mockResult), results);
        verify(this.mockAnalyzer).analyze(testObject);
    }

    @Test
    void analyzeWithDefaultAnalyzer()
        throws XWikiException, RequiredRightsException, MissingParserException, ParseException
    {
        DocumentReference classReference = new DocumentReference("wiki", "XWiki", "StandardClass");
        XWikiDocument classDocument = new XWikiDocument(classReference);
        BaseClass classObject = classDocument.getXClass();
        String restrictedFieldName = "restricted";
        classObject.addTextAreaField(restrictedFieldName, "Restricted", 80, 5,
            TextAreaClass.EditorType.WYSIWYG.toString(), TextAreaClass.ContentType.WIKI_TEXT.toString(), true);
        String wikiFieldName = "wiki";
        classObject.addTextAreaField(wikiFieldName, "Wiki", 80, 5, TextAreaClass.EditorType.WYSIWYG.toString(),
            TextAreaClass.ContentType.WIKI_TEXT.toString(), false);
        String plainFieldName = "plain";
        classObject.addTextAreaField(plainFieldName, "Plain", 80, 5, TextAreaClass.EditorType.PURE_TEXT.toString(),
            TextAreaClass.ContentType.PURE_TEXT.toString(), false);
        String velocityFieldName = "velocity";
        classObject.addTextAreaField(velocityFieldName, "Velocity", 80, 5, TextAreaClass.EditorType.TEXT.toString(),
            TextAreaClass.ContentType.VELOCITY_CODE.toString(), false);
        String velocityWikiFieldName = "velocityWiki";
        classObject.addTextAreaField(velocityWikiFieldName, "Velocity Wiki", 80, 5,
            TextAreaClass.EditorType.TEXT.toString(), TextAreaClass.ContentType.VELOCITYWIKI.toString(), false);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, this.oldcore.getXWikiContext());

        XWikiDocument testDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        Syntax testSyntax = mock();
        testDocument.setSyntax(testSyntax);
        BaseObject testObject = testDocument.newXObject(classReference, this.oldcore.getXWikiContext());
        String restrictedContent = "Restricted $velocity {{groovy}}{{/groovy}}";
        testObject.setLargeStringValue(restrictedFieldName, restrictedContent);
        String wikiContent = "Wiki $velocity {{groovy}}{{/groovy}}";
        testObject.setLargeStringValue(wikiFieldName, wikiContent);
        String plainContent = "Plain $velocity {{groovy}}{{/groovy}}";
        testObject.setLargeStringValue(plainFieldName, plainContent);
        String velocityContent = "Velocity $velocity {{groovy}}{{/groovy}}";
        testObject.setLargeStringValue(velocityFieldName, velocityContent);
        String velocityWikiContent = "Velocity Wiki $velocity {{groovy}}{{/groovy}}";
        testObject.setLargeStringValue(velocityWikiFieldName, velocityWikiContent);

        XDOM wikiXDOM = new XDOM(List.of());
        when(this.contentParser.parse(wikiContent, testSyntax, testObject.getDocumentReference())).thenReturn(wikiXDOM);

        RequiredRightAnalysisResult wikiResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(wikiXDOM)).thenReturn(List.of(wikiResult));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(testObject);
        verify(this.xdomRequiredRightAnalyzer).analyze(wikiXDOM);
        verifyNoMoreInteractions(this.xdomRequiredRightAnalyzer);
        assertEquals(testObject.getField(wikiFieldName).getReference(),
            wikiXDOM.getMetaData().getMetaData().get(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA));

        assertEquals(wikiResult, results.get(0));
        assertEquals(testObject.getField(velocityFieldName).getReference(), results.get(1).getEntityReference());
        assertEquals(testObject.getField(velocityWikiFieldName).getReference(), results.get(2).getEntityReference());
    }

    @Test
    void analyzeDefaultTextArea()
        throws XWikiException, RequiredRightsException, MissingParserException, ParseException
    {
        DocumentReference classReference = new DocumentReference("wiki", "XWiki", "StandardClass");
        XWikiDocument classDocument = new XWikiDocument(classReference);
        BaseClass classObject = classDocument.getXClass();
        String wikiFieldName = "wiki";
        classObject.addTextAreaField(wikiFieldName, "Wiki", 80, 5, "---", "---", false);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, this.oldcore.getXWikiContext());

        XWikiDocument testDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        Syntax testSyntax = mock();
        testDocument.setSyntax(testSyntax);
        BaseObject testObject = testDocument.newXObject(classReference, this.oldcore.getXWikiContext());
        String wikiContent = "{{groovy}}{{/groovy}}";
        testObject.setLargeStringValue(wikiFieldName, wikiContent);

        XDOM wikiXDOM = new XDOM(List.of());
        when(this.contentParser.parse(wikiContent, testSyntax, testObject.getDocumentReference())).thenReturn(wikiXDOM);

        RequiredRightAnalysisResult wikiResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(wikiXDOM)).thenReturn(List.of(wikiResult));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(testObject);
        verify(this.xdomRequiredRightAnalyzer).analyze(wikiXDOM);
        verifyNoMoreInteractions(this.xdomRequiredRightAnalyzer);
        assertEquals(testObject.getField(wikiFieldName).getReference(),
            wikiXDOM.getMetaData().getMetaData().get(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA));

        assertEquals(wikiResult, results.get(0));
    }


    @Test
    void analyzeWithCustomAnalyzerThrowsException() throws XWikiException, RequiredRightsException
    {
        XWikiDocument testDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject testObject = testDocument.newXObject(new DocumentReference("wiki", "XWiki", "TestClass"),
            this.oldcore.getXWikiContext());

        when(this.mockAnalyzer.analyze(testObject))
            .thenThrow(new RequiredRightsException("Test exception", new Exception()));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(testObject);
        verify(this.mockAnalyzer).analyze(testObject);
        assertEquals(1, results.size());
        assertEquals(List.of(RequiredRight.MAYBE_PROGRAM), results.get(0).getRequiredRights());
        assertEquals(testObject.getReference(), results.get(0).getEntityReference());
    }
}
