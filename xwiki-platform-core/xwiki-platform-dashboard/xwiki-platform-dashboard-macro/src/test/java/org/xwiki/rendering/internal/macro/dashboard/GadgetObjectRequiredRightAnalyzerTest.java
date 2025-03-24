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
package org.xwiki.rendering.internal.macro.dashboard;

import java.util.HashMap;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.analyzer.XDOMRequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GadgetObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentList({
    VelocityDetector.class,
})
@ReferenceComponentList
@ComponentTest
class GadgetObjectRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private GadgetObjectRequiredRightAnalyzer analyzer;

    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @MockComponent
    private ContentParser contentParser;

    @MockComponent
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Mock
    private BaseObject object;

    @Mock
    private ObjectReference objectReference;

    private DocumentReference documentReference;

    @BeforeEach
    void setup() throws Exception
    {
        doReturn(this.objectReference).when(this.object).getReference();

        this.documentReference = new DocumentReference("xwiki", "Gadget", "WebHome");
        XWikiDocument xWikiDocument = new XWikiDocument(this.documentReference);
        xWikiDocument.setSyntax(Syntax.XWIKI_2_1);
        when(this.object.getOwnerDocument()).thenReturn(xWikiDocument);
        when(this.object.getDocumentReference()).thenReturn(this.documentReference);
    }

    @Test
    void checkTitleWithVelocity() throws RequiredRightsException
    {
        String title = "$services.localization.render('gadget')";
        when(this.object.getStringValue("title")).thenReturn(title);

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.object);

        assertEquals(1, analysisResults.size());
        RequiredRightAnalysisResult analysisResult = analysisResults.get(0);
        assertEquals(this.objectReference, analysisResult.getEntityReference());
        verify(this.translationMessageSupplierProvider).get("dashboard.requiredrights.gadget.title");
        verify(this.translationMessageSupplierProvider).get("dashboard.requiredrights.gadget.title.description", title);

        assertEquals(2, analysisResult.getRequiredRights().size());
        assertTrue(analysisResult.getRequiredRights().containsAll(
            List.of(RequiredRight.MAYBE_PROGRAM, RequiredRight.MAYBE_SCRIPT)));
    }

    @Test
    void checkContentWithXDOMAnalyzer() throws RequiredRightsException, MissingParserException, ParseException
    {
        String content = "{{velocity}}$services.localization.render('gadget'){{/velocity}}";
        when(this.object.getStringValue("content")).thenReturn(content);
        MacroBlock macroBlock = new MacroBlock("gadget", new HashMap<>(), content, false);
        XDOM xdom = new XDOM(List.of(macroBlock));
        when(this.contentParser.parse(content, Syntax.XWIKI_2_1, this.documentReference)).thenReturn(xdom);

        RequiredRightAnalysisResult wikiResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(xdom)).thenReturn(List.of(wikiResult));

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.object);

        verify(this.xdomRequiredRightAnalyzer).analyze(xdom);
        assertEquals(this.object.getReference(),
            xdom.getMetaData().getMetaData().get(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA));

        assertEquals(1, analysisResults.size());
        assertEquals(wikiResult, analysisResults.get(0));
    }
}
