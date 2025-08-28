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
package org.xwiki.security.authorization.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link DocumentRequiredRightsChecker}.
 *
 * @version $Id$
 */
@ComponentTest
class DocumentRequiredRightsCheckerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "Space", "Page");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    @InjectMockComponents
    private DocumentRequiredRightsChecker documentRequiredRightsChecker;

    @MockComponent
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @Test
    void withSpaceReference() throws AuthorizationException
    {
        assertTrue(this.documentRequiredRightsChecker.hasRequiredRights(USER_REFERENCE,
            DOCUMENT_REFERENCE.getLastSpaceReference()));
        verifyNoInteractions(this.documentRequiredRightsManager);
        verifyNoInteractions(this.authorizationManager);
    }

    @Test
    void withNullReference() throws AuthorizationException
    {
        assertTrue(this.documentRequiredRightsChecker.hasRequiredRights(USER_REFERENCE, null));
        verifyNoInteractions(this.documentRequiredRightsManager);
        verifyNoInteractions(this.authorizationManager);
    }

    @Test
    void withEmptyRequiredRights() throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.of(DocumentRequiredRights.EMPTY));
        assertTrue(this.documentRequiredRightsChecker.hasRequiredRights(USER_REFERENCE, DOCUMENT_REFERENCE));
        verifyNoInteractions(this.authorizationManager);
    }

    @Test
    void withNoRequiredRights() throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.empty());
        assertTrue(this.documentRequiredRightsChecker.hasRequiredRights(USER_REFERENCE, DOCUMENT_REFERENCE));
        verifyNoInteractions(this.authorizationManager);
    }

    @Test
    void withDeniedRequiredRight() throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.of(new DocumentRequiredRights(true, new LinkedHashSet<>(
                List.of(
                    new DocumentRequiredRight(Right.PROGRAM, null),
                    new DocumentRequiredRight(Right.ADMIN, EntityType.WIKI),
                    new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT)
                )
            ))));

        when(this.authorizationManager.hasAccess(Right.PROGRAM, USER_REFERENCE, null))
            .thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.ADMIN, USER_REFERENCE, DOCUMENT_REFERENCE.getWikiReference()))
            .thenReturn(true);

        AttachmentReference attachmentReference = new AttachmentReference("test.png", DOCUMENT_REFERENCE);

        assertFalse(this.documentRequiredRightsChecker.hasRequiredRights(USER_REFERENCE, attachmentReference));

        verify(this.authorizationManager).hasAccess(Right.PROGRAM, USER_REFERENCE, null);
        verify(this.authorizationManager).hasAccess(Right.ADMIN, USER_REFERENCE, DOCUMENT_REFERENCE.getWikiReference());
        verify(this.authorizationManager).hasAccess(Right.SCRIPT, USER_REFERENCE, DOCUMENT_REFERENCE);
    }

    @Test
    void withEnforcedEmptyRequiredRights() throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.of(new DocumentRequiredRights(true, Set.of())));
        assertTrue(this.documentRequiredRightsChecker.hasRequiredRights(USER_REFERENCE, DOCUMENT_REFERENCE));
        verifyNoInteractions(this.authorizationManager);
    }

    @Test
    void withGrantedRequiredRight() throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.of(new DocumentRequiredRights(true, Set.of(
                new DocumentRequiredRight(Right.SCRIPT, EntityType.SPACE)
            ))));
        SpaceReference spaceReference = DOCUMENT_REFERENCE.getLastSpaceReference();
        when(this.authorizationManager.hasAccess(Right.SCRIPT, USER_REFERENCE, spaceReference)).thenReturn(true);
        assertTrue(this.documentRequiredRightsChecker.hasRequiredRights(USER_REFERENCE, DOCUMENT_REFERENCE));
        verify(this.authorizationManager).hasAccess(Right.SCRIPT, USER_REFERENCE, spaceReference);
    }
}
