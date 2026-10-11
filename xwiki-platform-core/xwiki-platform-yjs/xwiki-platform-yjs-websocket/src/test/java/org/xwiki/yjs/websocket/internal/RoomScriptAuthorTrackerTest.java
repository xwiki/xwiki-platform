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
package org.xwiki.yjs.websocket.internal;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.yjs.websocket.internal.ScriptAuthorChange.ScriptLevel;
import org.xwiki.yjs.websocket.internal.event.RoomScriptAuthorChangeEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoomScriptAuthorTracker}.
 *
 * @version $Id$
 */
@ComponentTest
class RoomScriptAuthorTrackerTest
{
    @InjectMockComponents
    private RoomScriptAuthorTracker scriptAuthorTracker;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserReferenceSerializer;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @MockComponent
    private ObservationManager observation;

    DocumentReference roomReference = new DocumentReference("test", "Some", "Page");

    UserReference noScriptUser = mock(UserReference.class, "noScriptUser");

    UserReference scriptUser = mock(UserReference.class, "scriptUser");

    UserReference superUser = mock(UserReference.class, "superUser");

    @BeforeEach
    void beforeEach()
    {
        DocumentReference noScriptUserDocRef = new DocumentReference("test", "XWiki", "noScriptUser");
        when(this.documentUserReferenceSerializer.serialize(noScriptUser)).thenReturn(noScriptUserDocRef);

        DocumentReference scriptUserDocRef = new DocumentReference("test", "XWiki", "scriptUser");
        when(this.documentUserReferenceSerializer.serialize(scriptUser)).thenReturn(scriptUserDocRef);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, scriptUserDocRef, roomReference))
            .thenReturn(true);

        DocumentReference superUserDocRef = new DocumentReference("test", "XWiki", "superUser");
        when(this.documentUserReferenceSerializer.serialize(superUser)).thenReturn(superUserDocRef);
        when(this.authorizationManager.hasAccess(Right.PROGRAM, null, superUserDocRef, roomReference)).thenReturn(true);

        doAnswer(invocation -> {
            RoomScriptAuthorChangeEvent event = invocation.getArgument(0);
            ScriptAuthorChange scriptAuthorChange = invocation.getArgument(1);
            this.scriptAuthorTracker.setScriptAuthor(event.getRoomReference(), scriptAuthorChange);
            return null;
        }).when(this.observation).notify(any(RoomScriptAuthorChangeEvent.class), any(ScriptAuthorChange.class));
    }

    @Test
    void getScriptAuthor()
    {
        assertTrue(this.scriptAuthorTracker.getScriptAuthor(roomReference).isEmpty());

        this.scriptAuthorTracker.maybeSetScriptAuthor(roomReference, this.superUser);

        // Current user has less script rights than the room script author. The current user is the effective author.
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.scriptUser);
        assertTrue(this.scriptAuthorTracker.getScriptAuthor(roomReference).isEmpty());

        // Current user has the same script rights as the room script author. The current user is the effective author.
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.superUser);
        assertTrue(this.scriptAuthorTracker.getScriptAuthor(roomReference).isEmpty());

        // Lower the script level for the room.
        this.scriptAuthorTracker.maybeSetScriptAuthor(roomReference, this.scriptUser);

        // Current user has more script rights than the room script author. The room script author is the effective
        // author.
        ScriptAuthorChange scriptAuthorChange = this.scriptAuthorTracker.getScriptAuthor(roomReference).orElseThrow();
        assertEquals(this.scriptUser, scriptAuthorChange.getAuthor());
        assertEquals(ScriptLevel.SCRIPT, scriptAuthorChange.getScriptLevel());

        // The script level cannot increase.
        this.scriptAuthorTracker.maybeSetScriptAuthor(roomReference, this.superUser);

        // Current user still has more script rights than the room script author. The room script author is the
        // effective author.
        scriptAuthorChange = this.scriptAuthorTracker.getScriptAuthor(roomReference).orElseThrow();
        assertEquals(this.scriptUser, scriptAuthorChange.getAuthor());
        assertEquals(ScriptLevel.SCRIPT, scriptAuthorChange.getScriptLevel());

        // Current user has less script rights than the room script author. The current user is the effective author.
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.noScriptUser);
        assertTrue(this.scriptAuthorTracker.getScriptAuthor(roomReference).isEmpty());

        // Lower the room script level even more.
        this.scriptAuthorTracker.maybeSetScriptAuthor(roomReference, this.noScriptUser);

        // Current user has the same script rights as the room script author. The current user is the effective author.
        assertTrue(this.scriptAuthorTracker.getScriptAuthor(roomReference).isEmpty());

        // Current user has more script rights than the room script author. The room script author is the effective
        // author.
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.scriptUser);
        scriptAuthorChange = this.scriptAuthorTracker.getScriptAuthor(roomReference).orElseThrow();
        assertEquals(this.noScriptUser, scriptAuthorChange.getAuthor());
        assertEquals(ScriptLevel.NO_SCRIPT, scriptAuthorChange.getScriptLevel());

        //
        // Verify room script author cleanup.
        //

        // Get the script author again.
        scriptAuthorChange = this.scriptAuthorTracker.getScriptAuthor(roomReference).orElseThrow();
        assertEquals(this.noScriptUser, scriptAuthorChange.getAuthor());
        assertEquals(ScriptLevel.NO_SCRIPT, scriptAuthorChange.getScriptLevel());

        // The room is recreated.
        this.scriptAuthorTracker.setScriptAuthor(roomReference, null);
        assertTrue(this.scriptAuthorTracker.getScriptAuthor(roomReference).isEmpty());
    }
}
