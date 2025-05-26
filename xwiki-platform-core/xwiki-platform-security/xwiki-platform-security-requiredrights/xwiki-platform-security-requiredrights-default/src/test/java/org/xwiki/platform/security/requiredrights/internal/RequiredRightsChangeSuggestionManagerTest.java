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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link RequiredRightsChangeSuggestionManager}.
 *
 * @version $Id$
 */
@ComponentTest
class RequiredRightsChangeSuggestionManagerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final DocumentRequiredRight DOCUMENT_SCRIPT_RIGHT = new DocumentRequiredRight(Right.SCRIPT,
        EntityType.DOCUMENT);

    private static final DocumentRequiredRight WIKI_ADMIN_RIGHT = new DocumentRequiredRight(Right.ADMIN,
        EntityType.WIKI);

    private static final DocumentRequiredRight PROGRAMMING_RIGHT = new DocumentRequiredRight(Right.PROGRAM, null);

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @InjectMockComponents
    private RequiredRightsChangeSuggestionManager requiredRightsChangeSuggestionManager;

    @Test
    void testIncreaseFromScriptToAdminRight()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT));
        List<RequiredRightAnalysisResult> analysisResults = mockAnalysisResults(List.of(RequiredRight.WIKI_ADMIN));
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(new RequiredRightChangeSuggestion(true, DOCUMENT_SCRIPT_RIGHT, WIKI_ADMIN_RIGHT, false, false));

        assertEquals(expectedOperations,
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults));
    }

    @Test
    void testIncreaseFromNoRightsToScriptRight()
    {
        // Verify that just edit right isn't enough.
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(true);

        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of());
        List<RequiredRightAnalysisResult> analysisResults = mockAnalysisResults(List.of(RequiredRight.SCRIPT));
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(new RequiredRightChangeSuggestion(true, null, DOCUMENT_SCRIPT_RIGHT, false, false));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(expectedOperations, operations);
    }

    @Test
    void testIncreaseFromNoRightToScriptAndMaybeProgrammingRight()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of());
        // Verify that rights that the user has are set to true.
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.SCRIPT, DOCUMENT_REFERENCE)).thenReturn(true);
        List<RequiredRightAnalysisResult> analysisResults = mockAnalysisResults(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM);
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(new RequiredRightChangeSuggestion(true, null, DOCUMENT_SCRIPT_RIGHT, false, true),
                new RequiredRightChangeSuggestion(true, null, PROGRAMMING_RIGHT, true, false));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(expectedOperations, operations);
        verify(this.contextualAuthorizationManager).hasAccess(Right.PROGRAM, null);
    }

    @Test
    void testScriptRightNeededAndSetAndMaybeProgrammingRightNeeded()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT));
        List<RequiredRightAnalysisResult> analysisResults = mockAnalysisResults(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM);
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(new RequiredRightChangeSuggestion(true, DOCUMENT_SCRIPT_RIGHT, PROGRAMMING_RIGHT, true, false));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(expectedOperations, operations);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testProgrammingRightSetAndScriptAndMaybeProgrammingRightNeeded(boolean hasAccess)
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(PROGRAMMING_RIGHT));
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(hasAccess);
        when(this.contextualAuthorizationManager.hasAccess(Right.SCRIPT, DOCUMENT_REFERENCE)).thenReturn(true);
        List<RequiredRightAnalysisResult> analysisResults =
            List.of(mockAnalysisResult(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM),
                mockAnalysisResult(List.of(RequiredRight.SCRIPT)));
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(
                new RequiredRightChangeSuggestion(false, PROGRAMMING_RIGHT, DOCUMENT_SCRIPT_RIGHT, true, hasAccess));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(expectedOperations, operations);
    }

    @Test
    void testScriptRightSetScriptRightDefinitelyNeededProgrammingRightMaybeNeeded()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT));
        List<RequiredRightAnalysisResult> analysisResults =
            List.of(mockAnalysisResult(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM),
                mockAnalysisResult(List.of(RequiredRight.PROGRAM)));
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(new RequiredRightChangeSuggestion(true, DOCUMENT_SCRIPT_RIGHT, PROGRAMMING_RIGHT, false, false));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(expectedOperations, operations);
    }

    @Test
    void testWikiAdminRightSetAndRequired()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(WIKI_ADMIN_RIGHT));
        List<RequiredRightAnalysisResult> analysisResults =
            mockAnalysisResults(List.of(RequiredRight.WIKI_ADMIN));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(List.of(), operations);
    }

    @Test
    void testProgrammingRightSetAdminRightMaybeRequiredAndScriptRightDefinitelyRequired()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(PROGRAMMING_RIGHT));
        List<RequiredRightAnalysisResult> analysisResults =
            mockAnalysisResults(List.of(RequiredRight.SCRIPT, new RequiredRight(Right.ADMIN, EntityType.WIKI, true)));
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(new RequiredRightChangeSuggestion(false, PROGRAMMING_RIGHT, DOCUMENT_SCRIPT_RIGHT, true, false),
                new RequiredRightChangeSuggestion(false, PROGRAMMING_RIGHT, WIKI_ADMIN_RIGHT, false, false));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(expectedOperations, operations);
    }

    @Test
    void testScriptRightSetAndNotAnyRightsRequired()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT));

        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(new RequiredRightChangeSuggestion(false, DOCUMENT_SCRIPT_RIGHT, null, false, false));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, List.of());

        assertEquals(expectedOperations, operations);
    }

    @Test
    void testWithoutAnyRightsRequired()
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of());

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, List.of());

        assertEquals(List.of(), operations);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testScriptRightSetButNotActuallyRequiredAndPotentiallyRequiredAdminRight(boolean hasWikiAdmin)
    {
        DocumentRequiredRights documentRequiredRights = new DocumentRequiredRights(true, Set.of(DOCUMENT_SCRIPT_RIGHT));
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN, DOCUMENT_REFERENCE.getWikiReference()))
            .thenReturn(hasWikiAdmin);
        List<RequiredRightAnalysisResult> analysisResults =
            mockAnalysisResults(List.of(new RequiredRight(Right.ADMIN, EntityType.WIKI, true)));
        List<RequiredRightChangeSuggestion> expectedOperations =
            List.of(
                new RequiredRightChangeSuggestion(true, DOCUMENT_SCRIPT_RIGHT, WIKI_ADMIN_RIGHT, true, hasWikiAdmin),
                new RequiredRightChangeSuggestion(false, DOCUMENT_SCRIPT_RIGHT, null, true, true));

        List<RequiredRightChangeSuggestion> operations =
            this.requiredRightsChangeSuggestionManager.getSuggestedOperations(DOCUMENT_REFERENCE,
                documentRequiredRights, analysisResults);

        assertEquals(expectedOperations, operations);
    }

    private static List<RequiredRightAnalysisResult> mockAnalysisResults(List<RequiredRight> requiredRights)
    {
        return List.of(mockAnalysisResult(requiredRights));
    }

    private static RequiredRightAnalysisResult mockAnalysisResult(List<RequiredRight> requiredRights)
    {
        return new RequiredRightAnalysisResult(mock(), mock(), mock(), requiredRights);
    }
}
