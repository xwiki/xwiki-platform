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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_DEFAULT_VALUE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.PARAMETER_DESCRIPTION_PROPERTY;
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
    protected static final String DEFAULT_VALUE = "wiki content";

    protected static final String DESCRIPTION = "wiki description";

    private static final EntityReference DEFAULT_VALUE_REFERENCE = mock();

    private static final EntityReference DESCRIPTION_REFERENCE = mock();

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @InjectMockComponents
    private WikiMacroParameterObjectRequiredRightAnalyzer analyzer;

    @MockComponent
    private ContentParser contentParser;

    @MockComponent
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Mock
    private BaseObject baseObject;

    @Mock
    private PropertyInterface defaultValueProperty;

    @Mock
    private PropertyInterface descriptionProperty;

    @Mock
    private XWikiDocument document;

    private final XDOM descriptionXDOM = new XDOM(List.of(new WordBlock("description")));

    private final XDOM defaultXDOM = new XDOM(List.of(new WordBlock("default")));

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.baseObject.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.baseObject.getOwnerDocument()).thenReturn(this.document);
        when(this.document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        when(this.baseObject.getStringValue(PARAMETER_DEFAULT_VALUE_PROPERTY)).thenReturn(DEFAULT_VALUE);
        when(this.baseObject.getField(PARAMETER_DEFAULT_VALUE_PROPERTY)).thenReturn(this.defaultValueProperty);
        when(this.defaultValueProperty.getReference()).thenReturn(DEFAULT_VALUE_REFERENCE);

        when(this.baseObject.getStringValue(PARAMETER_DESCRIPTION_PROPERTY)).thenReturn(DESCRIPTION);
        when(this.baseObject.getField(PARAMETER_DESCRIPTION_PROPERTY)).thenReturn(this.descriptionProperty);
        when(this.descriptionProperty.getReference()).thenReturn(DESCRIPTION_REFERENCE);

        when(this.contentParser.parse(DEFAULT_VALUE, Syntax.XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(this.defaultXDOM);
        when(this.contentParser.parse(DESCRIPTION, Syntax.XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(this.descriptionXDOM);
    }

    @ParameterizedTest
    @ValueSource(strings = { PARAMETER_TYPE_WIKI, "java.util.List<org.xwiki.rendering.block.Block>" })
    void analyzeWithWikiParameterType(String type) throws Exception
    {
        when(this.baseObject.getStringValue(PARAMETER_TYPE_PROPERTY)).thenReturn(type);

        RequiredRightAnalysisResult defaultValueResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(this.defaultXDOM)).thenReturn(List.of(defaultValueResult));
        RequiredRightAnalysisResult descriptionResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(this.descriptionXDOM)).thenReturn(List.of(descriptionResult));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.baseObject);

        assertEquals(List.of(descriptionResult, defaultValueResult), results);
        assertEquals(DESCRIPTION_REFERENCE, this.descriptionXDOM.getMetaData().getMetaData("entityReference"));
        assertEquals(DEFAULT_VALUE_REFERENCE, this.defaultXDOM.getMetaData().getMetaData("entityReference"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "java.util.List", "unknown" })
    @NullSource
    void analyzeWithNonWikiParameterType(String parameterType) throws Exception
    {
        when(this.baseObject.getStringValue(PARAMETER_TYPE_PROPERTY)).thenReturn(parameterType);
        RequiredRightAnalysisResult descriptionResult = mock();
        when(this.xdomRequiredRightAnalyzer.analyze(this.descriptionXDOM)).thenReturn(List.of(descriptionResult));

        List<RequiredRightAnalysisResult> results = this.analyzer.analyze(this.baseObject);

        assertEquals(List.of(descriptionResult), results);

        verify(this.xdomRequiredRightAnalyzer, never()).analyze(this.defaultXDOM);
        verify(this.contentParser, never()).parse(eq(DEFAULT_VALUE), any(), any());
        verify(this.contentParser).parse(eq(DESCRIPTION), any(), any());
    }
}

