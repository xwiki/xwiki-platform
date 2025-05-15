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
package org.xwiki.platform.security.requiredrights.rest.internal;

import java.util.List;
import java.util.ResourceBundle;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.requiredrights.rest.model.jaxb.AvailableRight;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link AvailableRightsManager}.
 *
 * @version $Id$
 */
@ComponentTest
class AvailableRightsManagerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    @InjectMockComponents
    private AvailableRightsManager availableRightsManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @BeforeEach
    void setUp()
    {
        when(this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn(USER_REFERENCE);
        ResourceBundle resources = ResourceBundle.getBundle("ApplicationResources");
        when(this.localizationManager.getTranslationPlain(anyString())).thenAnswer(invocationOnMock -> {
            String key = invocationOnMock.getArgument(0);
            return resources.getString(key);
        });
    }

    @Test
    void computeAvailableRightsAllRightsGranted()
    {
        when(this.authorizationManager.hasAccess(any(), eq(USER_REFERENCE), any())).thenReturn(true);

        List<AvailableRight> availableRights =
            this.availableRightsManager.computeAvailableRights(List.of(), DOCUMENT_REFERENCE);

        assertEquals(4, availableRights.size());
        availableRights.forEach(right -> {
            assertTrue(right.isHasRight());
            if (right.getRight().isEmpty()) {
                assertTrue(right.isDefinitelyRequiredRight());
            } else {
                assertFalse(right.isDefinitelyRequiredRight());
            }
            assertFalse(right.isMaybeRequiredRight());
        });
        assertEquals("", availableRights.get(0).getRight());
        assertEquals("script", availableRights.get(1).getRight());
        assertEquals("admin", availableRights.get(2).getRight());
        assertEquals("programming", availableRights.get(3).getRight());
        assertEquals("DOCUMENT", availableRights.get(0).getScope());
        assertEquals("DOCUMENT", availableRights.get(1).getScope());
        assertEquals("WIKI", availableRights.get(2).getScope());
        assertNull(availableRights.get(3).getScope());
        assertEquals("None", availableRights.get(0).getDisplayName());
        assertEquals("Script", availableRights.get(1).getDisplayName());
        assertEquals("Wiki Admin", availableRights.get(2).getDisplayName());
        assertEquals("Programming", availableRights.get(3).getDisplayName());

        verify(this.authorizationManager).hasAccess(Right.EDIT, USER_REFERENCE, DOCUMENT_REFERENCE);
        verify(this.authorizationManager).hasAccess(Right.SCRIPT, USER_REFERENCE, DOCUMENT_REFERENCE);
        verify(this.authorizationManager).hasAccess(Right.ADMIN, USER_REFERENCE, DOCUMENT_REFERENCE.getWikiReference());
        verify(this.authorizationManager).hasAccess(Right.PROGRAM, USER_REFERENCE, null);
        verifyNoMoreInteractions(this.authorizationManager);
    }

    @Test
    void computeAvailableRightsManualReviewNeeded()
    {
        RequiredRight requiredRight = new RequiredRight(Right.ADMIN, EntityType.WIKI, true);
        RequiredRightAnalysisResult analysisResult = mock();
        when(analysisResult.getRequiredRights()).thenReturn(List.of(requiredRight));

        List<AvailableRight> availableRights =
            this.availableRightsManager.computeAvailableRights(List.of(analysisResult), DOCUMENT_REFERENCE);

        assertEquals(4, availableRights.size());
        for (int i = 0; i < availableRights.size(); i++) {
            assertEquals(i == 0, availableRights.get(i).isDefinitelyRequiredRight());
            assertEquals(i == 2, availableRights.get(i).isMaybeRequiredRight());
            assertFalse(availableRights.get(i).isHasRight());
        }
    }

    @Test
    void computeAvailableRightsWithRequiredRights()
    {
        List<RequiredRight> requiredRights = List.of(
            new RequiredRight(Right.SCRIPT, EntityType.DOCUMENT, true),
            new RequiredRight(Right.SCRIPT, EntityType.DOCUMENT, false),
            new RequiredRight(Right.ADMIN, EntityType.WIKI, false),
            new RequiredRight(Right.ADMIN, EntityType.WIKI, true),
            new RequiredRight(Right.PROGRAM, null, true)
        );
        RequiredRightAnalysisResult analysisResult = mock();
        when(analysisResult.getRequiredRights()).thenReturn(requiredRights);
        when(this.authorizationManager.hasAccess(Right.EDIT, USER_REFERENCE, DOCUMENT_REFERENCE)).thenReturn(true);

        List<AvailableRight> availableRights =
            this.availableRightsManager.computeAvailableRights(List.of(analysisResult), DOCUMENT_REFERENCE);

        assertEquals(4, availableRights.size());
        // Maybe required rights are only indicated for rights above the maximum required right.
        for (int i = 0; i < availableRights.size(); i++) {
            assertEquals(i == 2, availableRights.get(i).isDefinitelyRequiredRight());
            assertEquals(i > 2, availableRights.get(i).isMaybeRequiredRight());
            // Only for "None" edit right is enough.
            assertEquals(i == 0, availableRights.get(i).isHasRight());
        }

        verify(this.authorizationManager).hasAccess(Right.EDIT, USER_REFERENCE, DOCUMENT_REFERENCE);
    }

    @Test
    void computeAvailableRightsWithWeirdRequiredRights()
    {
        // Verify that rights with unexpected scopes or different rights aren't considered.
        List<RequiredRight> requiredRights = List.of(
            new RequiredRight(Right.SCRIPT, EntityType.WIKI, false),
            new RequiredRight(Right.COMMENT, EntityType.DOCUMENT, false)
        );
        RequiredRightAnalysisResult analysisResult = mock();
        when(analysisResult.getRequiredRights()).thenReturn(requiredRights);

        List<AvailableRight> availableRights =
            this.availableRightsManager.computeAvailableRights(List.of(analysisResult), DOCUMENT_REFERENCE);
        assertEquals(4, availableRights.size());
        for (int i = 0; i < availableRights.size(); i++) {
            assertEquals(i == 0, availableRights.get(i).isDefinitelyRequiredRight());
            assertFalse(availableRights.get(i).isMaybeRequiredRight());
            assertFalse(availableRights.get(i).isHasRight());
        }
    }
}
