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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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
 * Provides a websocket endpoint to support realtime collaboration using yjs.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Component
@Singleton
@Named("yjs")
public class YjsRealtimeEndpoint extends Endpoint implements EndpointComponent
{
    private static final String ROOM_QUERY_PARAMETER = "room";

    @Inject
    private Logger logger;

    @Inject
    private WebSocketContext context;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * A map of rooms, a room corresponds to a {@link DocumentReference} and contains the set of currently active
     * {@link Session}.
     */
    private final Map<DocumentReference, Room> rooms = new ConcurrentHashMap<>();

    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
        this.context.run(session, () -> {
            try {
                List<String> room = session.getRequestParameterMap().get(ROOM_QUERY_PARAMETER);

                if (room == null || room.isEmpty()) {
                    session.close(
                        new CloseReason(UNEXPECTED_CONDITION,
                            "The [%s] query parameter is mandatory.".formatted(ROOM_QUERY_PARAMETER)));
                } else if (room.size() > 1) {
                    session.close(
                        new CloseReason(UNEXPECTED_CONDITION,
                            "The [%s] query parameter is expected only once.".formatted(ROOM_QUERY_PARAMETER)));
                } else {
                    var roomId = this.documentReferenceResolver.resolve(room.get(0));
                    if (this.contextualAuthorizationManager.hasAccess(EDIT, roomId)) {
                        startNewSession(session, roomId);
                    } else {
                        session.close(
                            new CloseReason(CANNOT_ACCEPT, "Current user is not allowed to edit the current document"));
                    }
                }
            } catch (IOException e) {
                this.logger.warn("Failed to close session. Cause: [{}]", getRootCauseMessage(e));
            }
        });
    }

    private void startNewSession(Session session, DocumentReference roomId)
    {
        var newRoom = this.rooms.computeIfAbsent(roomId, rid -> {
            try {
                Room room = this.componentManager.getInstance(Room.class);
                room.init(() -> this.rooms.remove(roomId));
                return room;
            } catch (ComponentLookupException e) {
                this.logger.warn("Failed to instantiate a new [{}]. Cause: [{}]", Room.class,
                    getRootCauseMessage(e));
                return null;
            }
        });
        this.roomsById.put(session.getId(), newRoom);
        if (newRoom != null) {
            session.addMessageHandler(InputStream.class, message -> newRoom.handleMessage(session, message));
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason)
    {
        super.onClose(session, closeReason);
        String sessionId = session.getId();
        this.roomsById.get(sessionId).disconnect(session);
        this.roomsById.remove(sessionId);
    }
}
