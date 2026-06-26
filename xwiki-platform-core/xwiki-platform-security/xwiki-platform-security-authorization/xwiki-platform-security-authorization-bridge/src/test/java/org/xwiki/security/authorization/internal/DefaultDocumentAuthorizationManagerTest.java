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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link DefaultDocumentAuthorizationManager}.
 *
 * @version $Id$
 */
@OldcoreTest
class DefaultDocumentAuthorizationManagerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("wiki", "space", "user");

    @InjectMockComponents
    private DefaultDocumentAuthorizationManager documentAuthorizationManager;

    @MockComponent
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension();

    @ParameterizedTest
    @MethodSource("getArguments")
    void hasRequiredRight(DocumentRequiredRights requiredRights, Right right, EntityType level, boolean expectedResult)
        throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.ofNullable(requiredRights));
        assertEquals(expectedResult,
            this.documentAuthorizationManager.hasRequiredRight(right, level, DOCUMENT_REFERENCE));
    }

    @ParameterizedTest
    @MethodSource("getArguments")
    void hasAccess(DocumentRequiredRights requiredRights, Right right, EntityType level, boolean expectedResult)
        throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.ofNullable(requiredRights));

        if (expectedResult) {
            when(this.authorizationManager.hasAccess(right, USER_REFERENCE, DOCUMENT_REFERENCE.extractReference(level)))
                .thenReturn(true)
                .thenReturn(false);
        }

        assertEquals(expectedResult,
            this.documentAuthorizationManager.hasAccess(right, level, USER_REFERENCE, DOCUMENT_REFERENCE));

        if (expectedResult) {
            assertFalse(this.documentAuthorizationManager.hasAccess(right, level, USER_REFERENCE, DOCUMENT_REFERENCE));
        } else {
            verifyNoInteractions(this.authorizationManager);
        }
    }

    @ParameterizedTest
    @MethodSource("getArguments")
    void checkAccess(DocumentRequiredRights requiredRights, Right right, EntityType level, boolean expectedResult)
        throws AuthorizationException
    {
        when(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.ofNullable(requiredRights));

        if (expectedResult) {
            this.documentAuthorizationManager.checkAccess(right, level, USER_REFERENCE, DOCUMENT_REFERENCE);

            AccessDeniedException expected = mock();
            doThrow(expected).when(this.authorizationManager)
                .checkAccess(right, USER_REFERENCE, DOCUMENT_REFERENCE.extractReference(level));
            AccessDeniedException actual = assertThrows(AccessDeniedException.class, () ->
                this.documentAuthorizationManager.checkAccess(right, level, USER_REFERENCE, DOCUMENT_REFERENCE)
            );
            assertEquals(expected, actual);
        } else {
            assertThrows(AccessDeniedException.class, () ->
                this.documentAuthorizationManager.checkAccess(right, level, USER_REFERENCE, DOCUMENT_REFERENCE)
            );

            assertEquals(1, this.logCapture.size());
            assertEquals(("[%s] right has been denied to user [%s] on entity [%s] based on required rights"
                    + " of document [%s] on level [%s]: security checkpoint")
                    .formatted(right, USER_REFERENCE, DOCUMENT_REFERENCE.extractReference(level), DOCUMENT_REFERENCE,
                        level),
                this.logCapture.getMessage(0));

            verifyNoInteractions(this.authorizationManager);
        }
    }

    static Stream<Arguments> getArguments()
    {
        return Stream.of(
            Arguments.of(
                DocumentRequiredRights.EMPTY,
                Right.PROGRAM,
                EntityType.DOCUMENT,
                true
            ),
            Arguments.of(
                null,
                Right.SCRIPT,
                EntityType.DOCUMENT,
                true
            ),
            Arguments.of(
                new DocumentRequiredRights(true, Set.of()),
                Right.ADMIN,
                EntityType.WIKI,
                false
            ),
            Arguments.of(
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.ADMIN, EntityType.SPACE))),
                Right.ADMIN,
                EntityType.WIKI,
                false
            ),
            Arguments.of(
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.ADMIN, EntityType.WIKI))),
                Right.ADMIN,
                EntityType.SPACE,
                true
            ),
            Arguments.of(
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.PROGRAM, null))),
                Right.SCRIPT,
                EntityType.DOCUMENT,
                true
            ),
            Arguments.of(
                new DocumentRequiredRights(true, Set.of(new DocumentRequiredRight(Right.ADMIN, EntityType.SPACE))),
                Right.REGISTER,
                EntityType.DOCUMENT,
                false
            )
        );
    }
}
