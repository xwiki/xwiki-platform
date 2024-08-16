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
package org.xwiki.websocket.internal;

import jakarta.websocket.CloseReason;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint.Basic;
import jakarta.websocket.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.websocket.WebSocketContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DynamicEchoEndpoint}.
 * 
 * @version $Id$
 */
@ComponentTest
class DynamicEchoEndpointTest
{
    @InjectMockComponents
    private DynamicEchoEndpoint echoEndPoint;

    @MockComponent
    private DocumentAccessBridge bridge;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    protected WebSocketContext context;

    @Captor
    ArgumentCaptor<MessageHandler.Whole<String>> messageHandlerCaptor;

    @Captor
    ArgumentCaptor<CloseReason> closeReasonCaptor;

    @Mock
    Session session;

    @BeforeEach
    void setup()
    {
        Basic basicRemote = mock(Basic.class);
        when(this.session.getBasicRemote()).thenReturn(basicRemote);
    }

    @Test
    void onOpenUnauthenticated() throws Exception
    {
        setContext("test", null);

        this.echoEndPoint.onOpen(this.session, null);

        verify(this.session).close(this.closeReasonCaptor.capture());

        CloseReason reason = this.closeReasonCaptor.getValue();
        assertEquals(CloseReason.CloseCodes.CANNOT_ACCEPT, reason.getCloseCode());
        assertEquals("We don't accept connections from guest users. Please login first.", reason.getReasonPhrase());

        verify(this.session, never()).getBasicRemote();
        verify(this.session, never()).getAsyncRemote();
    }

    @Test
    void onOpen() throws Exception
    {
        setContext("test", new DocumentReference("test", "Users", "Alice"));

        this.echoEndPoint.onOpen(this.session, null);

        verify(this.session).addMessageHandler(this.messageHandlerCaptor.capture());
        this.messageHandlerCaptor.getValue().onMessage("Hi there!");

        verify(this.session.getBasicRemote()).sendObject("[test] test:Users.Alice -> Hi there!");
    }

    private void setContext(String wiki, DocumentReference userReference)
    {
        doAnswer(invocation -> {
            when(this.bridge.getCurrentUserReference()).thenReturn(userReference);
            when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference(wiki));

            invocation.getArgument(1, Runnable.class).run();

            when(this.bridge.getCurrentUserReference()).thenReturn(null);
            when(this.modelContext.getCurrentEntityReference()).thenReturn(null);

            return null;
        }).when(this.context).run(any(Session.class), any(Runnable.class));
    }
}
