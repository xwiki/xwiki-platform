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
package org.xwiki.internal.web;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link PageTemplateRequiredRightsChecker}.
 *
 * @version $Id$
 * @since 17.10.2
 */
@ComponentTest
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
class PageTemplateRequiredRightsCheckerTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String SPACE_NAME = "Space";

    private static final String SUBSPACE_NAME = "SubSpace";

    private static final String LEVEL1_NAME = "Level1";

    private static final String LEVEL2_NAME = "Level2";

    private static final String LEVEL3_NAME = "Level3";

    private static final String SINGLE_SPACE_NAME = "SingleSpace";

    private static final DocumentReference TEMPLATE_REFERENCE =
        new DocumentReference(WIKI_NAME, "Templates", "MyTemplate");

    private static final DocumentReference USER_REFERENCE =
        new DocumentReference(WIKI_NAME, "XWiki", "User");

    private static final DocumentReference TARGET_DOCUMENT_REFERENCE =
        new DocumentReference(WIKI_NAME, List.of(SPACE_NAME, SUBSPACE_NAME), "Page");

    private static final SpaceReference TARGET_SPACE_REFERENCE =
        new SpaceReference(WIKI_NAME, SPACE_NAME, SUBSPACE_NAME);

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private PageTemplateRequiredRightsChecker checker;

    @MockComponent
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> xWikiContextProvider;

    @Mock
    private XWikiContext context;

    @BeforeEach
    void setUp()
    {
        when(this.context.getUserReference()).thenReturn(USER_REFERENCE);
        when(this.xWikiContextProvider.get()).thenReturn(this.context);
    }

    static Stream<Arguments> noAuthCheckTestCases()
    {
        return Stream.of(
            Arguments.of("no required rights (empty optional)", null),
            Arguments.of("empty required rights", DocumentRequiredRights.EMPTY),
            Arguments.of("not enforced", new DocumentRequiredRights(false,
                Set.of(new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT)))),
            Arguments.of("enforced with empty set", new DocumentRequiredRights(true, Set.of()))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("noAuthCheckTestCases")
    void hasRequiredRightsWithoutAuthorizationCheck(String description, DocumentRequiredRights requiredRights)
        throws Exception
    {
        when(this.documentRequiredRightsManager.getRequiredRights(TEMPLATE_REFERENCE))
            .thenReturn(Optional.ofNullable(requiredRights));

        assertTrue(this.checker.hasRequiredRights(TEMPLATE_REFERENCE, TARGET_DOCUMENT_REFERENCE));

        verifyNoInteractions(this.authorizationManager);
    }

    static Stream<Arguments> singleRightGrantedTestCases()
    {
        return Stream.of(
            Arguments.of("document scope on document reference", Right.SCRIPT, EntityType.DOCUMENT,
                TARGET_DOCUMENT_REFERENCE, TARGET_DOCUMENT_REFERENCE),
            Arguments.of("document scope on space reference", Right.SCRIPT, EntityType.DOCUMENT,
                TARGET_SPACE_REFERENCE, TARGET_SPACE_REFERENCE),
            Arguments.of("space scope on document reference", Right.EDIT, EntityType.SPACE,
                TARGET_DOCUMENT_REFERENCE, TARGET_DOCUMENT_REFERENCE.getLastSpaceReference()),
            Arguments.of("space scope on space reference", Right.EDIT, EntityType.SPACE,
                TARGET_SPACE_REFERENCE, TARGET_SPACE_REFERENCE),
            Arguments.of("wiki scope", Right.ADMIN, EntityType.WIKI,
                TARGET_DOCUMENT_REFERENCE, TARGET_DOCUMENT_REFERENCE.getWikiReference()),
            Arguments.of("null scope", Right.PROGRAM, null,
                TARGET_DOCUMENT_REFERENCE, null),
            Arguments.of("nested space reference", Right.COMMENT, EntityType.SPACE,
                new SpaceReference(WIKI_NAME, LEVEL1_NAME, LEVEL2_NAME, LEVEL3_NAME),
                new SpaceReference(WIKI_NAME, LEVEL1_NAME, LEVEL2_NAME, LEVEL3_NAME)),
            Arguments.of("single level space", Right.EDIT, EntityType.SPACE,
                new SpaceReference(WIKI_NAME, SINGLE_SPACE_NAME),
                new SpaceReference(WIKI_NAME, SINGLE_SPACE_NAME))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleRightGrantedTestCases")
    void hasRequiredRightsWithSingleRightGranted(String description, Right right, EntityType scope,
        EntityReference targetReference, EntityReference expectedScopeReference) throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights(true, Set.of(
            new DocumentRequiredRight(right, scope)
        ));
        when(this.documentRequiredRightsManager.getRequiredRights(TEMPLATE_REFERENCE))
            .thenReturn(Optional.of(requiredRights));
        when(this.authorizationManager.hasAccess(right, USER_REFERENCE, expectedScopeReference))
            .thenReturn(true);

        assertTrue(this.checker.hasRequiredRights(TEMPLATE_REFERENCE, targetReference));

        verify(this.authorizationManager).hasAccess(right, USER_REFERENCE, expectedScopeReference);
    }

    @Test
    void hasRequiredRightsWithMultipleRightsAllGranted() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights(true, new LinkedHashSet<>(List.of(
            new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT),
            new DocumentRequiredRight(Right.EDIT, EntityType.SPACE),
            new DocumentRequiredRight(Right.ADMIN, EntityType.WIKI)
        )));
        when(this.documentRequiredRightsManager.getRequiredRights(TEMPLATE_REFERENCE))
            .thenReturn(Optional.of(requiredRights));
        when(this.authorizationManager.hasAccess(Right.SCRIPT, USER_REFERENCE, TARGET_DOCUMENT_REFERENCE))
            .thenReturn(true);
        SpaceReference lastSpaceReference = TARGET_DOCUMENT_REFERENCE.getLastSpaceReference();
        when(this.authorizationManager.hasAccess(Right.EDIT, USER_REFERENCE, lastSpaceReference))
            .thenReturn(true);
        WikiReference wikiReference = TARGET_DOCUMENT_REFERENCE.getWikiReference();
        when(this.authorizationManager.hasAccess(Right.ADMIN, USER_REFERENCE, wikiReference))
            .thenReturn(true);

        assertTrue(this.checker.hasRequiredRights(TEMPLATE_REFERENCE, TARGET_DOCUMENT_REFERENCE));

        verify(this.authorizationManager).hasAccess(Right.SCRIPT, USER_REFERENCE, TARGET_DOCUMENT_REFERENCE);
        verify(this.authorizationManager).hasAccess(Right.EDIT, USER_REFERENCE, lastSpaceReference);
        verify(this.authorizationManager).hasAccess(Right.ADMIN, USER_REFERENCE, wikiReference);
    }

    @Test
    void hasRequiredRightsWithFirstRightDenied() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights(true, new LinkedHashSet<>(List.of(
            new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT),
            new DocumentRequiredRight(Right.EDIT, EntityType.SPACE)
        )));
        when(this.documentRequiredRightsManager.getRequiredRights(TEMPLATE_REFERENCE))
            .thenReturn(Optional.of(requiredRights));
        when(this.authorizationManager.hasAccess(Right.SCRIPT, USER_REFERENCE, TARGET_DOCUMENT_REFERENCE))
            .thenReturn(false);

        assertFalse(this.checker.hasRequiredRights(TEMPLATE_REFERENCE, TARGET_DOCUMENT_REFERENCE));

        verify(this.authorizationManager).hasAccess(Right.SCRIPT, USER_REFERENCE, TARGET_DOCUMENT_REFERENCE);
    }

    @Test
    void hasRequiredRightsWithSecondRightDenied() throws Exception
    {
        DocumentRequiredRights requiredRights = new DocumentRequiredRights(true, new LinkedHashSet<>(List.of(
            new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT),
            new DocumentRequiredRight(Right.EDIT, EntityType.SPACE),
            new DocumentRequiredRight(Right.ADMIN, EntityType.WIKI)
        )));
        when(this.documentRequiredRightsManager.getRequiredRights(TEMPLATE_REFERENCE))
            .thenReturn(Optional.of(requiredRights));
        when(this.authorizationManager.hasAccess(Right.SCRIPT, USER_REFERENCE, TARGET_DOCUMENT_REFERENCE))
            .thenReturn(true);
        SpaceReference lastSpaceReference = TARGET_DOCUMENT_REFERENCE.getLastSpaceReference();
        when(this.authorizationManager.hasAccess(Right.EDIT, USER_REFERENCE, lastSpaceReference))
            .thenReturn(false);

        assertFalse(this.checker.hasRequiredRights(TEMPLATE_REFERENCE, TARGET_DOCUMENT_REFERENCE));

        verify(this.authorizationManager).hasAccess(Right.SCRIPT, USER_REFERENCE, TARGET_DOCUMENT_REFERENCE);
        verify(this.authorizationManager).hasAccess(Right.EDIT, USER_REFERENCE, lastSpaceReference);
    }

    @Test
    void hasRequiredRightsWithAuthorizationException() throws Exception
    {
        when(this.documentRequiredRightsManager.getRequiredRights(TEMPLATE_REFERENCE))
            .thenThrow(new AuthorizationException("Test exception", new Exception("Root cause")));

        assertFalse(this.checker.hasRequiredRights(TEMPLATE_REFERENCE, TARGET_DOCUMENT_REFERENCE));

        verifyNoInteractions(this.authorizationManager);
        assertTrue(this.logCapture.getMessage(0).contains("There was an error getting the required rights"));
        assertTrue(this.logCapture.getMessage(0).contains(TEMPLATE_REFERENCE.toString()));
    }
}
