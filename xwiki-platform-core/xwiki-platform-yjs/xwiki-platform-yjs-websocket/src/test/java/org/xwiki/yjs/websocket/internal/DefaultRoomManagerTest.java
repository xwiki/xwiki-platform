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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import jakarta.websocket.CloseReason;
import jakarta.websocket.RemoteEndpoint.Basic;
import jakarta.websocket.Session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.websocket.AbstractPartialBinaryMessageHandler;
import org.xwiki.yjs.websocket.internal.event.RoomMessageEvent;
import org.xwiki.yjs.websocket.internal.event.RoomMessageListener;

import com.google.protobuf.CodedOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultRoomManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultRoomManagerTest
{
    @InjectMockComponents
    private DefaultRoomManager roomManager;

    @InjectMockComponents
    private RoomMessageListener roomMessageListener;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private YjsEndpointConfiguration configuration;

    @MockComponent
    private PingPongManager pingPongManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Captor
    private ArgumentCaptor<AbstractPartialBinaryMessageHandler> messageHandlerCaptor;

    @Captor
    private ArgumentCaptor<CloseReason> closeReasonCaptor;

    private DocumentReference documentReference = new DocumentReference("xwiki", "Some", "Page");

    @Test
    void joinReceiveSendAndLeave() throws Exception
    {
        when(this.configuration.getMaxHistorySize()).thenReturn(30L);

        UserReference aliceReference = mock(UserReference.class, "alice-reference");
        Session aliceSession = mock(Session.class, "alice-session");
        when(aliceSession.getId()).thenReturn("alice-session");
        Basic aliceBasicRemote = mock(Basic.class, "alice-basic-remote");
        when(aliceSession.getBasicRemote()).thenReturn(aliceBasicRemote);

        UserReference bobReference = mock(UserReference.class, "bob-reference");
        Session bobSession = mock(Session.class, "bob-session");
        when(bobSession.getId()).thenReturn("bob-session");
        Basic bobBasicRemote = mock(Basic.class, "bob-basic-remote");
        when(bobSession.getBasicRemote()).thenReturn(bobBasicRemote);

        //
        // Alice joins the room.
        //

        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(aliceReference);
        this.roomManager.join(aliceSession, documentReference);

        verify(this.pingPongManager).startPinging(aliceSession);
        verify(aliceSession).addMessageHandler(this.messageHandlerCaptor.capture());
        AbstractPartialBinaryMessageHandler aliceMessageHandler = this.messageHandlerCaptor.getValue();

        // Try to join again.
        this.roomManager.join(aliceSession, documentReference);
        assertEquals(1, this.logCapture.size());
        assertEquals("The session [alice-session] is already connected to the [xwiki:Some.Page] room.",
            this.logCapture.getMessage(0));

        //
        // Bob joins the same room.
        //

        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(bobReference);

        // First, try to leave before joining. This shouldn't fail.
        this.roomManager.leave(bobSession);
        verify(this.pingPongManager).stopPinging(bobSession);

        // Now join the room.
        this.roomManager.join(bobSession, documentReference);

        verify(this.pingPongManager).startPinging(bobSession);
        verify(bobSession).addMessageHandler(this.messageHandlerCaptor.capture());
        AbstractPartialBinaryMessageHandler bobMessageHandler = this.messageHandlerCaptor.getValue();

        //
        // Alice and Bob present themselves.
        //

        Client alice = new Client(aliceReference, 13L);
        byte[] message = createIdMessage(alice.getId());
        aliceMessageHandler.onMessage(message);
        verify(this.observationManager).notify(new RoomMessageEvent(alice, documentReference), message);

        Client bob = new Client(bobReference, 27L);
        bobMessageHandler.onMessage(createIdMessage(bob.getId()));

        //
        // Carol joins from a different cluster node.
        //

        UserReference carolReference = mock(UserReference.class, "carol-reference");
        Client carol = new Client(carolReference, 42L);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.roomMessageListener.onEvent(new RoomMessageEvent(carol, documentReference), createIdMessage(carol.getId()),
            null);

        //
        // Alice asks for synchronization.
        //

        byte[] syncMessage = createSyncStep1Message();
        aliceMessageHandler.onMessage(syncMessage);
        // There are no recorded updates to replay at this point, so the server just ends the synchronization process by
        // sending an empty SYNC_STEP_2 message.
        verify(aliceBasicRemote).sendBinary(ByteBuffer.wrap(Message.emptySyncStep2()));
        // Sync step 1 messages are not broadcasted to other clients.
        verify(bobBasicRemote, never()).sendBinary(ByteBuffer.wrap(syncMessage));

        //
        // Alice sends a synchronization update message.
        //

        byte[] firstUpdate = createSyncUpdateMessage("one");
        aliceMessageHandler.onMessage(firstUpdate);
        // Binary update messages are not sent back to the sender.
        verify(aliceBasicRemote, never()).sendBinary(ByteBuffer.wrap(firstUpdate));
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(firstUpdate));
        verify(this.observationManager).notify(new RoomMessageEvent(alice, documentReference), firstUpdate);

        //
        // Bob sends awareness message.
        //

        byte[] awarenessMessage = createAwarenessMessage("Hi!");
        bobMessageHandler.onMessage(awarenessMessage);
        verify(aliceBasicRemote).sendBinary(ByteBuffer.wrap(awarenessMessage));
        // Awareness messages are sent back to the sender as well in order to keep the connection alive.
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(awarenessMessage));
        verify(this.observationManager).notify(new RoomMessageEvent(bob, documentReference), awarenessMessage);

        //
        // Alice sends another synchronization update message.
        //

        byte[] secondUpdate = createSyncUpdateMessage("two");
        aliceMessageHandler.onMessage(secondUpdate);
        verify(aliceBasicRemote, never()).sendBinary(ByteBuffer.wrap(secondUpdate));
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(secondUpdate));

        //
        // Carol sends a synchronization update message from her cluster node.
        //

        byte[] thirdUpdate = createSyncUpdateMessage("three");
        this.roomMessageListener.onEvent(new RoomMessageEvent(carol, documentReference), thirdUpdate, null);
        // The update is broadcasted to Alice and Bob.
        verify(aliceBasicRemote).sendBinary(ByteBuffer.wrap(thirdUpdate));
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(thirdUpdate));

        //
        // Bob asks for synchronization.
        //

        syncMessage = createSyncStep1Message();
        bobMessageHandler.onMessage(syncMessage);
        // We replay all recorded updates to Bob, including those that he has.
        verify(bobBasicRemote, times(2)).sendBinary(ByteBuffer.wrap(firstUpdate));
        verify(bobBasicRemote, times(2)).sendBinary(ByteBuffer.wrap(secondUpdate));
        verify(bobBasicRemote, times(2)).sendBinary(ByteBuffer.wrap(thirdUpdate));
        // We also end the synchronization process by sending an empty SYNC_STEP_2 message.
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(Message.emptySyncStep2()));
        // Sync step 1 messages are not broadcasted to other clients.
        verify(aliceBasicRemote, never()).sendBinary(ByteBuffer.wrap(syncMessage));

        //
        // Bob sends a synchronization update message that Alice fails to receive.
        //

        byte[] forthUpdate = createSyncUpdateMessage("four");
        doThrow(new IOException("Send failed")).when(aliceBasicRemote).sendBinary(ByteBuffer.wrap(forthUpdate));
        bobMessageHandler.onMessage(forthUpdate);
        verify(this.pingPongManager).stopPinging(aliceSession);
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(Message.leave(alice.getId())));
        verify(this.observationManager).notify(new RoomMessageEvent(alice, documentReference),
            Message.leave(alice.getId()));
        assertEquals(2, this.logCapture.size());
        assertEquals("Failed to send message to client [Client{id=13, userReference=alice-reference}]."
            + " Cause: [IOException: Send failed]", this.logCapture.getMessage(1));

        //
        // Alice joins back.
        //

        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(aliceReference);
        this.roomManager.join(aliceSession, documentReference);
        verify(this.pingPongManager, times(2)).startPinging(aliceSession);
        verify(aliceSession, times(2)).addMessageHandler(this.messageHandlerCaptor.capture());
        aliceMessageHandler = this.messageHandlerCaptor.getValue();

        //
        // Alice presents herself again and asks for synchronization but fails to receive the second update.
        //

        aliceMessageHandler.onMessage(createIdMessage(19));

        doThrow(new IOException("Send failed again")).when(aliceBasicRemote).sendBinary(ByteBuffer.wrap(secondUpdate));
        syncMessage = createSyncStep1Message();
        aliceMessageHandler.onMessage(syncMessage);
        verify(aliceBasicRemote).sendBinary(ByteBuffer.wrap(firstUpdate));
        // Only one, from the first synchronization request.
        verify(aliceBasicRemote).sendBinary(ByteBuffer.wrap(Message.emptySyncStep2()));
        verify(this.pingPongManager, times(2)).stopPinging(aliceSession);
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(Message.leave(19L)));
        assertEquals(3, this.logCapture.size());
        assertEquals("Failed to replay stored update to client [Client{id=19, userReference=alice-reference}]."
            + " Cause: [IOException: Send failed again]", this.logCapture.getMessage(2));

        //
        // Carols leaves the room from her cluster node.
        //

        message = Message.leave(carol.getId());
        this.roomMessageListener.onEvent(new RoomMessageEvent(carol, documentReference), message, null);
        verify(bobBasicRemote).sendBinary(ByteBuffer.wrap(message));

        //
        // Bob sends another synchronization update message that exceeds the maximum history size of the room.
        //

        syncMessage = createSyncUpdateMessage("five");
        bobMessageHandler.onMessage(syncMessage);
        verify(this.pingPongManager, times(2)).stopPinging(bobSession);
        assertEquals(4, this.logCapture.size());
        assertEquals("Room [xwiki:Some.Page] has reached the maximum history size of [30] bytes. Closing.",
            this.logCapture.getMessage(3));

        //
        // Bob joins back and leaves the room.
        //

        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(bobReference);
        this.roomManager.join(bobSession, documentReference);
        this.roomManager.leave(bobSession);
        verify(this.pingPongManager, times(3)).stopPinging(bobSession);
    }

    private byte[] createIdMessage(long id) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(2);
        codedOutputStream.writeUInt64NoTag(id);
        codedOutputStream.flush();
        return output.toByteArray();
    }

    private byte[] createSyncStep1Message() throws Exception
    {
        return createSyncMessage(0, "");
    }

    private byte[] createSyncUpdateMessage(String content) throws Exception
    {
        return createSyncMessage(2, content);
    }

    private byte[] createSyncMessage(long syncType, String content) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(0);
        codedOutputStream.writeUInt64NoTag(syncType);
        codedOutputStream.writeByteArrayNoTag(content.getBytes());
        codedOutputStream.flush();
        return output.toByteArray();
    }

    private byte[] createAwarenessMessage(String content) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(1);
        codedOutputStream.writeByteArrayNoTag(content.getBytes());
        codedOutputStream.flush();
        return output.toByteArray();
    }
}
