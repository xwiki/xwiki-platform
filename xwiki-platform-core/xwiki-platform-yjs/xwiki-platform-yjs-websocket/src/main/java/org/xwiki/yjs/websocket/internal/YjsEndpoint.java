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
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.websocket.EndpointComponent;
import org.xwiki.websocket.WebSocketContext;

import static jakarta.websocket.CloseReason.CloseCodes.CANNOT_ACCEPT;
import static jakarta.websocket.CloseReason.CloseCodes.UNEXPECTED_CONDITION;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.security.authorization.Right.EDIT;

/**
 * Provides a WebSocket endpoint to support realtime collaboration using yjs.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Component
@Singleton
@Named("yjs")
public class YjsEndpoint extends Endpoint implements EndpointComponent
{
    private static final String ROOM_QUERY_PARAMETER = "room";

    @Inject
    private Logger logger;

    @Inject
    private WebSocketContext context;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private RoomManager roomManager;

    @Inject
    private YjsEndpointConfiguration configuration;

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
        session.setMaxIdleTimeout(this.configuration.getMaxIdleTimeout());
        session.setMaxBinaryMessageBufferSize(this.configuration.getMaxBinaryMessageBufferSize());
        session.setMaxTextMessageBufferSize(this.configuration.getMaxTextMessageBufferSize());

        this.context.run(session, () -> {
            List<String> room = session.getRequestParameterMap().get(ROOM_QUERY_PARAMETER);

            if (room == null || room.isEmpty()) {
                closeSession(session, UNEXPECTED_CONDITION,
                    "The [%s] query parameter is mandatory.".formatted(ROOM_QUERY_PARAMETER));
            } else if (room.size() > 1) {
                closeSession(session, UNEXPECTED_CONDITION,
                    "The [%s] query parameter is expected only once.".formatted(ROOM_QUERY_PARAMETER));
            } else {
                var documentReference = this.documentReferenceResolver.resolve(room.get(0));
                maybeJoin(session, documentReference);
            }
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason)
    {
        this.roomManager.leave(session);
    }

    @Override
    public void onError(Session session, Throwable throwable)
    {
        this.logger.debug("Session closed with error.", throwable);
    }

    private void maybeJoin(Session session, DocumentReference documentReference)
    {
        if (this.contextualAuthorizationManager.hasAccess(EDIT, documentReference)) {
            try {
                this.roomManager.join(session, documentReference);
            } catch (Exception e) {
                this.logger.warn("Failed to join the room [{}]. Cause: [{}]", documentReference,
                    getRootCauseMessage(e));
                closeSession(session, UNEXPECTED_CONDITION, "An unexpected error happened while joining the room.");
            }
        } else {
            closeSession(session, CANNOT_ACCEPT, "You are not allowed to edit [%s].".formatted(documentReference));
        }
    }

    private void closeSession(Session session, CloseCode closeReason, String reasonPhrase)
    {
        try {
            session.close(new CloseReason(closeReason, reasonPhrase));
        } catch (IOException e) {
            this.logger.warn("Failed to close session. Cause: [{}]", getRootCauseMessage(e));
        }
    }
}
