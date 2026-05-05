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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.websocket.WebSocketContext;

import static jakarta.websocket.CloseReason.CloseCodes.CANNOT_ACCEPT;
import static jakarta.websocket.CloseReason.CloseCodes.UNEXPECTED_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.EDIT;

/**
 * Unit tests for {@link YjsEndpoint}.
 *
 * @version $Id$
 */
@ComponentTest
class YjsEndpointTest
{
    @InjectMockComponents
    private YjsEndpoint endpoint;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private WebSocketContext context;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private RoomManager roomManager;

    @MockComponent
    private YjsEndpointConfiguration configuration;

    @Mock
    private Session session;

    @Mock
    private EndpointConfig endpointConfig;

    private DocumentReference documentReference = new DocumentReference("test", "Some", "Page");

    @Captor
    private ArgumentCaptor<CloseReason> closeReasonCaptor;

    private Map<String, List<String>> requestParameters = new HashMap<>();

    @BeforeEach
    void beforeEach()
    {
        when(this.configuration.getMaxIdleTimeout()).thenReturn(75_000L);
        when(this.configuration.getMaxBinaryMessageBufferSize()).thenReturn(8_192);
        when(this.configuration.getMaxTextMessageBufferSize()).thenReturn(8_192);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(this.context).run(eq(session), any(Runnable.class));

        when(this.session.getRequestParameterMap()).thenReturn(this.requestParameters);

        when(this.documentReferenceResolver.resolve("Some.Page")).thenReturn(this.documentReference);
    }

    @Test
    void onOpen() throws Exception
    {
        this.requestParameters.put("room", List.of("Some.Page"));
        when(this.contextualAuthorizationManager.hasAccess(EDIT, this.documentReference)).thenReturn(true);

        this.endpoint.onOpen(this.session, this.endpointConfig);

        verify(this.roomManager).join(this.session, this.documentReference);

        verify(this.session).setMaxIdleTimeout(75_000L);
        verify(this.session).setMaxBinaryMessageBufferSize(8_192);
        verify(this.session).setMaxTextMessageBufferSize(8_192);
        verify(this.session, never()).close(any());
    }

    @Test
    void onOpenWhenRoomNotSpecified() throws Exception
    {
        this.endpoint.onOpen(this.session, this.endpointConfig);

        verifySessionClosed(UNEXPECTED_CONDITION, "The [room] query parameter is mandatory.");
        verify(this.roomManager, never()).join(any(), any());
    }

    @Test
    void onOpenWhenRoomSpecifiedMultipleTimes() throws Exception
    {
        this.requestParameters.put("room", List.of("Space.Page", "Space.OtherPage"));

        this.endpoint.onOpen(this.session, this.endpointConfig);

        verifySessionClosed(UNEXPECTED_CONDITION, "The [room] query parameter is expected only once.");
        verify(this.roomManager, never()).join(any(), any());
    }

    @Test
    void onOpenUnauthorized() throws Exception
    {
        this.requestParameters.put("room", List.of("Some.Page"));
        when(this.contextualAuthorizationManager.hasAccess(EDIT, this.documentReference)).thenReturn(false);

        this.endpoint.onOpen(this.session, this.endpointConfig);

        verifySessionClosed(CANNOT_ACCEPT, "You are not allowed to edit [test:Some.Page].");
        verify(this.roomManager, never()).join(any(), any());
    }

    @Test
    void onOpenWhenJoinThrows() throws Exception
    {
        this.requestParameters.put("room", List.of("Some.Page"));
        when(this.contextualAuthorizationManager.hasAccess(EDIT, this.documentReference)).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(this.roomManager).join(this.session, this.documentReference);

        this.endpoint.onOpen(this.session, this.endpointConfig);

        verifySessionClosed(UNEXPECTED_CONDITION, "An unexpected error happened while joining the room.");
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to join the room [test:Some.Page]. Cause: [RuntimeException: boom]",
            this.logCapture.getMessage(0));
    }

    @Test
    void onClose()
    {
        CloseReason closeReason = new CloseReason(UNEXPECTED_CONDITION, "closed");

        this.endpoint.onClose(this.session, closeReason);

        verify(this.roomManager).leave(this.session);
    }

    private CloseReason captureCloseReason() throws IOException
    {
        verify(this.session).close(this.closeReasonCaptor.capture());
        return this.closeReasonCaptor.getValue();
    }

    private void verifySessionClosed(CloseReason.CloseCode expectedCloseCode, String expectedReasonPhrase)
        throws IOException
    {
        CloseReason closeReason = captureCloseReason();
        assertEquals(expectedCloseCode, closeReason.getCloseCode());
        assertEquals(expectedReasonPhrase, closeReason.getReasonPhrase());
    }
}
