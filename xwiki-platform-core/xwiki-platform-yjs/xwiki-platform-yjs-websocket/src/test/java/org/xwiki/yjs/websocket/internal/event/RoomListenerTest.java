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
package org.xwiki.yjs.websocket.internal.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.yjs.websocket.internal.Client;
import org.xwiki.yjs.websocket.internal.MessageTestUtils;
import org.xwiki.yjs.websocket.internal.Room;
import org.xwiki.yjs.websocket.internal.RoomManager;
import org.xwiki.yjs.websocket.internal.RoomScriptAuthorTracker;
import org.xwiki.yjs.websocket.internal.ScriptAuthorChange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoomListener}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@ComponentTest
class RoomListenerTest
{
    @InjectMockComponents
    private RoomListener roomListener;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private RoomManager roomManager;

    @MockComponent
    private RoomScriptAuthorTracker roomScriptAuthorTracker;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    private DocumentReference roomReference = new DocumentReference("test", "Some", "Room");

    @Mock(name = "alice")
    private UserReference alice;

    private RoomMessageEvent roomMessageEvent;

    @Mock
    private Room room;

    @BeforeEach
    void beforeEach()
    {
        this.roomMessageEvent = new RoomMessageEvent(new Client(alice), roomReference);
    }

    @Test
    void onScriptAuthorChange()
    {
        RoomScriptAuthorChangeEvent event = new RoomScriptAuthorChangeEvent(roomReference);
        ScriptAuthorChange scriptAuthorChange = mock(ScriptAuthorChange.class);

        this.roomListener.onEvent(event, scriptAuthorChange, null);

        verify(this.roomScriptAuthorTracker).setScriptAuthor(roomReference, scriptAuthorChange);
    }

    @Test
    void onLocalMessage() throws Exception
    {
        // Only local update messages should trigger the update of the script author.
        byte[] awarenessMessage = MessageTestUtils.createAwarenessMessage("one");
        this.roomListener.onEvent(this.roomMessageEvent, awarenessMessage, null);
        verify(this.roomScriptAuthorTracker, never()).maybeSetScriptAuthor(roomReference, alice);

        // Invalid local message.
        this.roomListener.onEvent(this.roomMessageEvent, new byte[0], null);
        verify(this.roomScriptAuthorTracker, never()).maybeSetScriptAuthor(roomReference, alice);
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to determine the message type. Root cause: [InvalidProtocolBufferException:"
            + " While parsing a protocol message, the input ended unexpectedly in the middle of a field.  This could"
            + " mean either that the input has been truncated or that an embedded message misreported its own length.].",
            this.logCapture.getMessage(0));

        // Valid local update message.
        byte[] updateMessage = MessageTestUtils.createSyncUpdateMessage("two");
        this.roomListener.onEvent(this.roomMessageEvent, updateMessage, null);
        verify(this.roomScriptAuthorTracker).maybeSetScriptAuthor(roomReference, alice);
    }

    @Test
    void onRemoteMessage()
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        when(this.roomManager.get(roomReference, true)).thenReturn(this.room);

        // Should re-broadcast the message to the room if it exists.
        byte[] message = "remote".getBytes();
        this.roomListener.onEvent(this.roomMessageEvent, message, null);
        verify(this.room).onMessage(this.roomMessageEvent.getClient(), message);
    }
}
