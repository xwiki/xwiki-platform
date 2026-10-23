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
package org.xwiki.edit.internal;

import java.util.List;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EditorRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class EditorRequiredRightsAnalyzerTest
{
    private static final DocumentReference EDITOR_REFERENCE = new DocumentReference("wiki", "Space", "Editor");

    private static final String CODE = "{{velocity}}code{{/velocity}}";

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

    @InjectMockComponents
    private EditorRequiredRightsAnalyzer analyzer;

    private final BaseObject editorObject = mock();

    private final BaseObjectReference objectReference = mock();

    @BeforeEach
    void setUp()
    {
        XWikiDocument ownerDocument = mock();
        when(ownerDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(this.editorObject.getOwnerDocument()).thenReturn(ownerDocument);
        when(this.editorObject.getReference()).thenReturn(this.objectReference);
        when(this.editorObject.getDocumentReference()).thenReturn(EDITOR_REFERENCE);
        when(this.editorObject.getStringValue("scope")).thenReturn("user");
    }

    @ParameterizedTest
    @CsvSource({ "wiki, wiki", "global, global", "invalid, wiki" })
    void analyzeScope(String scope, String expectedScope) throws Exception
    {
        RequiredRight requiredRight = "global".equals(expectedScope) ? RequiredRight.PROGRAM : RequiredRight.WIKI_ADMIN;
        when(this.editorObject.getStringValue("scope")).thenReturn(scope);

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.editorObject);

        assertEquals(1, results.size());
        assertEquals(this.objectReference, results.get(0).getEntityReference());
        assertEquals(List.of(requiredRight), results.get(0).getRequiredRights());
        verify(this.translationBlockSupplierProvider).get("platform.edit.editor.requiredrights." + expectedScope);
        verify(this.objectBlockSupplierProvider).get(this.editorObject);
    }

    @Test
    void analyzeUserScopeWithoutCode() throws Exception
    {
        assertEquals(List.of(), this.analyzer.analyze(this.editorObject));
        verify(this.contentParser, never()).parse(any(), any(), any());
    }

    @Test
    void analyzeCode() throws Exception
    {
        when(this.editorObject.getStringValue("code")).thenReturn(CODE);
        XDOM xdom = new XDOM(List.of());
        when(this.contentParser.parse(CODE, Syntax.XWIKI_2_1, EDITOR_REFERENCE)).thenReturn(xdom);
        RequiredRightAnalysisResult codeResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(xdom)).thenReturn(List.of(codeResult));

        assertEquals(List.of(codeResult), this.analyzer.analyze(this.editorObject));
        assertEquals(this.objectReference, xdom.getMetaData().getMetaData("entityReference"));
    }

    @Test
    void analyzeCodeWithParseError() throws Exception
    {
        when(this.editorObject.getStringValue("code")).thenReturn(CODE);
        when(this.contentParser.parse(any(), any(), any())).thenThrow(new WikiComponentException("Parse failure"));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.editorObject);

        assertEquals(1, results.size());
        assertEquals(List.of(RequiredRight.MAYBE_PROGRAM), results.get(0).getRequiredRights());
        verify(this.translationBlockSupplierProvider).get("platform.edit.editor.requiredrights.codeError",
            "WikiComponentException: Parse failure");
        verify(this.stringBlockSupplierProvider).get(CODE);
        assertEquals(this.objectReference, results.get(0).getEntityReference());
    }
}
