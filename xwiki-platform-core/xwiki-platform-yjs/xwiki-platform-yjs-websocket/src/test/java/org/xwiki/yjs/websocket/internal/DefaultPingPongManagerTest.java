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

import java.io.IOException;

import jakarta.websocket.CloseReason;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.PongMessage;
import jakarta.websocket.RemoteEndpoint.Basic;
import jakarta.websocket.Session;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static jakarta.websocket.CloseReason.CloseCodes.GOING_AWAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPingPongManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultPingPongManagerTest
{
    @InjectMockComponents
    private DefaultPingPongManager pingPongManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private YjsEndpointConfiguration configuration;

    @Captor
    private ArgumentCaptor<MessageHandler.Whole<@NonNull PongMessage>> pongHandlerCaptor;

    @Captor
    private ArgumentCaptor<CloseReason> closeReasonCaptor;

    @BeforeComponent
    void beforeEach()
    {
        // Use a large ping interval to avoid automatic pings during the test. We trigger the pings manually.
        when(this.configuration.getPingInterval()).thenReturn(60_000L);
    }

    @Test
    void startPingStop() throws Exception
    {
        when(this.configuration.getPingMaxMissedPongs()).thenReturn(1);

        Basic aliceBasicRemote = mock(Basic.class, "alice-basic-remote");
        Session aliceSession = mock(Session.class, "alice-session");
        when(aliceSession.getId()).thenReturn("alice-session");
        when(aliceSession.getBasicRemote()).thenReturn(aliceBasicRemote);
        when(aliceSession.isOpen()).thenReturn(true);

        Basic bobBasicRemote = mock(Basic.class, "bob-basic-remote");
        Session bobSession = mock(Session.class, "bob-session");
        when(bobSession.getId()).thenReturn("bob-session");
        when(bobSession.getBasicRemote()).thenReturn(bobBasicRemote);
        when(bobSession.isOpen()).thenReturn(true);

        //
        // Ping when no session is tracked.
        //

        this.pingPongManager.pingAllSessions();

        verify(aliceBasicRemote, never()).sendPing(any());
        verify(bobBasicRemote, never()).sendPing(any());

        //
        // Start tracking Alice session and ping.
        //

        this.pingPongManager.startPinging(aliceSession);
        verify(aliceSession).addMessageHandler(eq(PongMessage.class), this.pongHandlerCaptor.capture());
        MessageHandler.Whole<@NonNull PongMessage> alicePongHandler = this.pongHandlerCaptor.getValue();

        this.pingPongManager.pingAllSessions();

        verify(aliceBasicRemote).sendPing(any());
        verify(bobBasicRemote, never()).sendPing(any());

        // Send pong message from Alice to reset the missed pongs count.
        alicePongHandler.onMessage(mock(PongMessage.class));

        //
        // Start tracking Bob session and ping.
        //

        this.pingPongManager.startPinging(bobSession);
        verify(bobSession).addMessageHandler(eq(PongMessage.class), this.pongHandlerCaptor.capture());
        MessageHandler.Whole<@NonNull PongMessage> bobPongHandler = this.pongHandlerCaptor.getAllValues().get(1);

        this.pingPongManager.pingAllSessions();
        verify(aliceBasicRemote, times(2)).sendPing(any());
        verify(bobBasicRemote).sendPing(any());

        // Send pong message from Alice to reset the missed pongs count.
        alicePongHandler.onMessage(mock(PongMessage.class));

        // Ping again and check that Bob session is closed because it reached the maximum number of missed pongs.
        this.pingPongManager.pingAllSessions();
        verify(aliceBasicRemote, times(3)).sendPing(any());
        verify(bobBasicRemote).sendPing(any());
        verify(bobSession).removeMessageHandler(bobPongHandler);
        verify(bobSession).close(this.closeReasonCaptor.capture());
        assertEquals(GOING_AWAY, this.closeReasonCaptor.getValue().getCloseCode());
        assertEquals("Client did not respond to server ping.", this.closeReasonCaptor.getValue().getReasonPhrase());

        // Send pong message from Alice to reset the missed pongs count.
        alicePongHandler.onMessage(mock(PongMessage.class));

        // Pong message from Bob should be ignored (sent too late).
        bobPongHandler.onMessage(mock(PongMessage.class));

        this.pingPongManager.pingAllSessions();
        verify(aliceBasicRemote, times(4)).sendPing(any());
        verify(bobBasicRemote).sendPing(any());

        // Send pong message from Alice to reset the missed pongs count.
        alicePongHandler.onMessage(mock(PongMessage.class));

        //
        // Bob joins back.
        //

        this.pingPongManager.startPinging(bobSession);
        verify(bobSession, times(2)).addMessageHandler(eq(PongMessage.class), this.pongHandlerCaptor.capture());
        bobPongHandler = this.pongHandlerCaptor.getAllValues().get(3);

        // Fail to send ping to Alice should close her session.
        doThrow(new IOException("Ping failed")).when(aliceBasicRemote).sendPing(any());
        this.pingPongManager.pingAllSessions();
        verify(bobBasicRemote, times(2)).sendPing(any());

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to send ping message to session [alice-session]. Cause: [IOException: Ping failed]",
            this.logCapture.getMessage(0));
        verify(aliceSession).removeMessageHandler(alicePongHandler);
        verify(aliceSession).close(this.closeReasonCaptor.capture());
        assertEquals(GOING_AWAY, this.closeReasonCaptor.getValue().getCloseCode());
        assertEquals("Failed to send ping message to the client.", this.closeReasonCaptor.getValue().getReasonPhrase());

        // Send pong message from Bob to reset the missed pongs count.
        bobPongHandler.onMessage(mock(PongMessage.class));

        //
        // Bob's session gets closed.
        //

        when(bobSession.isOpen()).thenReturn(false);
        this.pingPongManager.pingAllSessions();
        verify(aliceBasicRemote, times(5)).sendPing(any());
        verify(bobBasicRemote, times(2)).sendPing(any());
        verify(bobSession).removeMessageHandler(bobPongHandler);
        verify(bobSession).close(any());

        //
        // Alice joins back and leaves
        //

        this.pingPongManager.startPinging(aliceSession);
        verify(aliceSession, times(2)).addMessageHandler(eq(PongMessage.class), this.pongHandlerCaptor.capture());
        alicePongHandler = this.pongHandlerCaptor.getAllValues().get(4);

        this.pingPongManager.stopPinging(aliceSession);
        verify(aliceSession).removeMessageHandler(alicePongHandler);
        verify(aliceSession).close(any());

        //
        // No sessions are tracked, ping should do nothing.
        //

        this.pingPongManager.pingAllSessions();
        verify(aliceBasicRemote, times(5)).sendPing(any());
        verify(bobBasicRemote, times(2)).sendPing(any());
    }
}
