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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.websocket.WebSocketContext;

import static jakarta.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE;
import static jakarta.websocket.CloseReason.CloseCodes.UNEXPECTED_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.WARN;

/**
 * Test of {@link YjsRealtimeEndpoint}.
 *
 * @version $Id$
 */
@ComponentTest
class YjsRealtimeEndpointTest
{
    private static final String DOCUMENT_REFERENCE_STR = "My.Doc";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "My", "Doc");

    @InjectMockComponents
    private YjsRealtimeEndpoint yjsRealtimeEndpoint;

    @MockComponent
    private WebSocketContext context;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(WARN);

    @Mock
    private Session session;

    @Mock
    private EndpointConfig config;

    @Mock
    private Room room;

    @Captor
    private ArgumentCaptor<CloseReason> closeReasonCaptor;

    @BeforeEach
    void setUp()
    {
        // Blindly run lambda passed to the run method.
        doAnswer(i -> {
            i.getArgument(1, Runnable.class).run();
            return null;
        }).when(this.context).run(any(), any());
        when(this.session.getPathParameters()).thenReturn(Map.of("room", DOCUMENT_REFERENCE_STR));
        when(this.session.getId()).thenReturn("session-id");
        when(this.documentReferenceResolver.resolve("My.Doc")).thenReturn(DOCUMENT_REFERENCE);
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(true);
        when(this.session.getRequestParameterMap()).thenReturn(Map.of());
    }

    @Test
    void onOpenWithoutEditRight() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(false);
        this.yjsRealtimeEndpoint.onOpen(this.session, this.config);
        verify(this.session).close(any(CloseReason.class));
    }

    @Test
    void onOpenWithEditRight() throws Exception
    {
        when(this.contextComponentManager.getInstance(Room.class)).thenReturn(this.room);
        when(this.session.getRequestParameterMap()).thenReturn(Map.of("room", List.of(DOCUMENT_REFERENCE_STR)));
        this.yjsRealtimeEndpoint.onOpen(this.session, this.config);
        verify(this.room).init(any());
        verify(this.session).addMessageHandler(eq(InputStream.class), any(MessageHandler.Whole.class));
    }

    @Test
    void onOpenMissingRoomParam() throws Exception
    {
        when(this.contextComponentManager.getInstance(Room.class)).thenReturn(this.room);
        this.yjsRealtimeEndpoint.onOpen(this.session, this.config);
        verify(this.session).close(this.closeReasonCaptor.capture());
        CloseReason closeReason = this.closeReasonCaptor.getValue();
        assertEquals(UNEXPECTED_CONDITION, closeReason.getCloseCode());
        assertEquals("The [room] query parameter is mandatory.", closeReason.getReasonPhrase());
    }

    @Test
    void onOpenMissingManyRoomParams() throws Exception
    {
        when(this.contextComponentManager.getInstance(Room.class)).thenReturn(this.room);
        when(this.session.getRequestParameterMap()).thenReturn(Map.of("room", List.of(DOCUMENT_REFERENCE_STR,
            "one too many string")));
        this.yjsRealtimeEndpoint.onOpen(this.session, this.config);
        verify(this.session).close(this.closeReasonCaptor.capture());
        CloseReason closeReason = this.closeReasonCaptor.getValue();
        assertEquals(UNEXPECTED_CONDITION, closeReason.getCloseCode());
        assertEquals("The [room] query parameter is expected only once.", closeReason.getReasonPhrase());
    }

    @Test
    void onClose() throws Exception
    {
        when(this.contextComponentManager.getInstance(Room.class)).thenReturn(this.room);
        when(this.session.getRequestParameterMap()).thenReturn(Map.of("room", List.of(DOCUMENT_REFERENCE_STR)));
        this.yjsRealtimeEndpoint.onOpen(this.session, this.config);
        this.yjsRealtimeEndpoint.onClose(this.session, new CloseReason(NORMAL_CLOSURE, "some reason"));
        verify(this.room).disconnect(this.session);
    }
}