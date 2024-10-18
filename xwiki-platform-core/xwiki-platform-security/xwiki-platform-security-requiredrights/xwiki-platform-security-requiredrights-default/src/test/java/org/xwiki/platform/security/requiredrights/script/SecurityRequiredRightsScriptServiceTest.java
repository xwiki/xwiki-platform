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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
// Dependencies are required for mocking.
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@ComponentTest
class SecurityRequiredRightsScriptServiceTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final DocumentRequiredRight DOCUMENT_SCRIPT_RIGHT = new DocumentRequiredRight(Right.SCRIPT,
        EntityType.DOCUMENT);

    private static final DocumentRequiredRight WIKI_ADMIN_RIGHT = new DocumentRequiredRight(Right.ADMIN,
        EntityType.WIKI);

    private static final DocumentRequiredRight PROGRAMMING_RIGHT = new DocumentRequiredRight(Right.PROGRAM, null);

    @MockComponent
    private RequiredRightAnalyzer<XWikiDocument> requiredRightAnalyzer;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

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
        when(this.requiredRightAnalyzer.analyze(this.xWikiDocument)).thenReturn(expectedResults);

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
    void analyzeDocumentThrowsRequiredRightsExceptionWhenDocumentLoadingFails() throws Exception
    {
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenThrow(new XWikiException());

        assertThrows(RequiredRightsException.class, () -> this.service.analyzeDocument(DOCUMENT_REFERENCE));
        verifyNoInteractions(this.requiredRightAnalyzer);
    }

    @Test
    void analyzeDocumentReturnsEmptyListWhenNoRightsRequired() throws Exception
    {
        when(this.requiredRightAnalyzer.analyze(this.xWikiDocument)).thenReturn(List.of());

        List<RequiredRightAnalysisResult> results = this.service.analyzeDocument(DOCUMENT_REFERENCE);

        assertTrue(results.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideTestCasesForGetSuggestedOperations")
    void getSuggestedOperationsTest(DocumentRequiredRights documentRequiredRights,
        List<RequiredRightAnalysisResult> analysisResults,
        List<SecurityRequiredRightsScriptService.RightOperation> expectedOperations) throws Exception
    {
        Document document = mock();
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(document.getRequiredRights()).thenReturn(documentRequiredRights);
        when(this.requiredRightAnalyzer.analyze(this.xWikiDocument)).thenReturn(analysisResults);

        List<SecurityRequiredRightsScriptService.RightOperation> operations =
            this.service.getSuggestedOperations(document);

        assertEquals(expectedOperations, operations);
    }

    private static Stream<Arguments> provideTestCasesForGetSuggestedOperations()
    {
        List<Arguments> arguments = new ArrayList<>();
        // Test increase from script to admin right.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT)),
            List.of(new RequiredRightAnalysisResult(mock(), mock(), mock(), List.of(RequiredRight.WIKI_ADMIN))),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(true, DOCUMENT_SCRIPT_RIGHT, WIKI_ADMIN_RIGHT,
                    false)
            )
        ));

        // Test increase from no rights to script right.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of()),
            List.of(new RequiredRightAnalysisResult(mock(), mock(), mock(), List.of(RequiredRight.SCRIPT))),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(true, null, DOCUMENT_SCRIPT_RIGHT, false)
            )
        ));

        // Test increase from no right to script and maybe programming right. Both should be suggested.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of()),
            List.of(new RequiredRightAnalysisResult(mock(), mock(), mock(), RequiredRight.SCRIPT_AND_MAYBE_PROGRAM)),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(true, null, DOCUMENT_SCRIPT_RIGHT, false),
                new SecurityRequiredRightsScriptService.RightOperation(true, null, PROGRAMMING_RIGHT, true)
            )
        ));

        // Test with script right needed and set and maybe programming right needed. A possible increase should be
        // suggested.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT)),
            List.of(new RequiredRightAnalysisResult(mock(), mock(), mock(), RequiredRight.SCRIPT_AND_MAYBE_PROGRAM)),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(true, DOCUMENT_SCRIPT_RIGHT, PROGRAMMING_RIGHT,
                    true)
            )
        ));

        // Test with programming right set and script and maybe programming right needed. A possible decrease should
        // be suggested.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(PROGRAMMING_RIGHT)),
            List.of(
                new RequiredRightAnalysisResult(mock(), mock(), mock(), RequiredRight.SCRIPT_AND_MAYBE_PROGRAM),
                new RequiredRightAnalysisResult(mock(), mock(), mock(), List.of(RequiredRight.SCRIPT))
            ),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(false, PROGRAMMING_RIGHT, DOCUMENT_SCRIPT_RIGHT,
                    true)
            )
        ));

        // Test with script right set, script right definitely needed, programming right maybe needed and an
        // additional result where programming right is definitely needed. An increase to programming right should be
        // suggested without manual review.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT)),
            List.of(
                new RequiredRightAnalysisResult(mock(), mock(), mock(), RequiredRight.SCRIPT_AND_MAYBE_PROGRAM),
                new RequiredRightAnalysisResult(mock(), mock(), mock(), List.of(RequiredRight.PROGRAM))
            ),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(true, DOCUMENT_SCRIPT_RIGHT, PROGRAMMING_RIGHT,
                    false)
            )
        ));

        // Test with wiki admin right set and required. No operation should be suggested.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(WIKI_ADMIN_RIGHT)),
            List.of(new RequiredRightAnalysisResult(mock(), mock(), mock(), List.of(RequiredRight.WIKI_ADMIN))),
            List.<SecurityRequiredRightsScriptService.RightOperation>of()
        ));

        // Test with programming right set, admin right maybe required and script right definitely required. Two
        // options for a possible decrease should be suggested.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(PROGRAMMING_RIGHT)),
            List.of(
                new RequiredRightAnalysisResult(mock(), mock(), mock(),
                    List.of(
                        RequiredRight.SCRIPT,
                        new RequiredRight(Right.ADMIN, EntityType.WIKI, true)))
            ),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(false, PROGRAMMING_RIGHT, DOCUMENT_SCRIPT_RIGHT,
                    true),
                new SecurityRequiredRightsScriptService.RightOperation(false, PROGRAMMING_RIGHT, WIKI_ADMIN_RIGHT,
                    false)
            )
        ));

        // Test with script right set and not any rights required. Removing script right should be suggested.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT)),
            List.<RequiredRightAnalysisResult>of(),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(false, DOCUMENT_SCRIPT_RIGHT, null, false)
            )
        ));

        // Test without any rights required.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of()),
            List.<RequiredRightAnalysisResult>of(),
            List.<SecurityRequiredRightsScriptService.RightOperation>of()
        ));

        // Test with script right set but not actually required and a potentially required admin right - both the
        // removal of script and the addition of admin right should be suggested.
        arguments.add(Arguments.of(
            new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT)),
            List.of(
                new RequiredRightAnalysisResult(mock(), mock(), mock(),
                    List.of(new RequiredRight(Right.ADMIN, EntityType.WIKI, true)))
            ),
            List.of(
                new SecurityRequiredRightsScriptService.RightOperation(true, DOCUMENT_SCRIPT_RIGHT, WIKI_ADMIN_RIGHT,
                    true),
                new SecurityRequiredRightsScriptService.RightOperation(false, DOCUMENT_SCRIPT_RIGHT, null, true)
            )
        ));

        return arguments.stream();
    }

    @Test
    void getSuggestedOperationsThrowsRequiredRightsExceptionWhenDocumentLoadingFails() throws Exception
    {
        Document document = mock(Document.class);
        when(document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenThrow(new XWikiException());

        assertThrows(RequiredRightsException.class, () -> this.service.getSuggestedOperations(document));
    }
}
