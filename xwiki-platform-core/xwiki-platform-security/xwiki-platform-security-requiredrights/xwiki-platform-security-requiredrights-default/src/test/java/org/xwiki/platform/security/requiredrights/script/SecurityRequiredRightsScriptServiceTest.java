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
package org.xwiki.platform.security.requiredrights.script;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightChangeSuggestion;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightsChangeSuggestionManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link SecurityRequiredRightsScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class SecurityRequiredRightsScriptServiceTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @MockComponent
    @Named("full")
    private RequiredRightAnalyzer<DocumentReference> requiredRightAnalyzer;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private RequiredRightsChangeSuggestionManager requiredRightsChangeSuggestionManager;

    @InjectMockComponents
    private SecurityRequiredRightsScriptService service;

    @MockComponent
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument xWikiDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.xWikiDocument);
    }

    @Test
    void analyzeDocumentReturnsResultsWhenDocumentIsAccessible() throws Exception
    {
        List<RequiredRightAnalysisResult> expectedResults = List.of(mock(RequiredRightAnalysisResult.class));
        when(this.requiredRightAnalyzer.analyze(DOCUMENT_REFERENCE)).thenReturn(expectedResults);

        List<RequiredRightAnalysisResult> results = this.service.analyzeDocument(DOCUMENT_REFERENCE);

        assertEquals(expectedResults, results);
    }

    @Test
    void analyzeDocumentThrowsAuthorizationExceptionWhenAccessDenied() throws AccessDeniedException
    {
        doThrow(new AccessDeniedException(Right.VIEW, null, DOCUMENT_REFERENCE))
            .when(this.contextualAuthorizationManager).checkAccess(Right.VIEW, DOCUMENT_REFERENCE);

        assertThrows(AuthorizationException.class, () -> this.service.analyzeDocument(DOCUMENT_REFERENCE));
        verifyNoInteractions(this.wiki);
        verifyNoInteractions(this.requiredRightAnalyzer);
    }

    @Test
    void analyzeDocumentReturnsEmptyListWhenNoRightsRequired() throws Exception
    {
        when(this.requiredRightAnalyzer.analyze(DOCUMENT_REFERENCE)).thenReturn(List.of());

        List<RequiredRightAnalysisResult> results = this.service.analyzeDocument(DOCUMENT_REFERENCE);

        assertTrue(results.isEmpty());
    }

    @Test
    void getSuggestedOperations() throws Exception
    {
        List<RequiredRightAnalysisResult> mockResult = mock();

        when(this.requiredRightAnalyzer.analyze(DOCUMENT_REFERENCE)).thenReturn(mockResult);

        List<RequiredRightChangeSuggestion> result = mock();
        Document inputDocument = mock();
        when(inputDocument.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        DocumentRequiredRights documentRequiredRights = mock();
        when(inputDocument.getRequiredRights()).thenReturn(documentRequiredRights);
        when(this.requiredRightsChangeSuggestionManager
            .getSuggestedOperations(DOCUMENT_REFERENCE, documentRequiredRights, mockResult))
            .thenReturn(result);

        assertSame(result, this.service.getSuggestedOperations(inputDocument));
    }
}
