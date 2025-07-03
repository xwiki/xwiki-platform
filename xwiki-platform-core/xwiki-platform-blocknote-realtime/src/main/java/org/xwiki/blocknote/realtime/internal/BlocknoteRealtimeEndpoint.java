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
package org.xwiki.blocknote.realtime.internal;

import java.io.IOException;
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
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.websocket.EndpointComponent;
import org.xwiki.websocket.WebSocketContext;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.security.authorization.Right.EDIT;

/**
 * @version $Id$
 * @since x.y.z
 */
@Component
@Singleton
@Named("blocknote")
public class BlocknoteRealtimeEndpoint extends Endpoint implements EndpointComponent
{
    // The client side keeps the connection alive by sending a PING message from time to time, using a timer
    // (setTimeout). The browsers are slowing down timers used by inactive tabs / windows (that don't have
    // the user focus). This is called timer throttling and can go up to 1 minute, which means inactive browser tabs
    // won't be able to send PING messages more often than every minute. For this reason, we set the session idle
    // timeout a little bit higher than the timer throttling value to make sure the WebSocket connection is not closed
    // in background tabs.
    // See https://developer.chrome.com/blog/timer-throttling-in-chrome-88/
    private static final long TIMEOUT_MILLISECONDS = 65000;

    @Inject
    private Logger logger;

    @Inject
    private WebSocketContext context;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private final Map<DocumentReference, Room> rooms = new ConcurrentHashMap<>();

    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
        // Close the session if we don't receive any message from the user in TIMEOUT_MILLISECONDS.
        session.setMaxIdleTimeout(TIMEOUT_MILLISECONDS);
        this.logger.info("BlocknoteRealtimeEndpoint opened. session [{}], config [{}]", session, config);
        this.context.run(session, () -> {
            var roomId = this.documentReferenceResolver.resolve(session.getPathParameters().get("room"));
            // TODO: remove the "true", it's a bypass to test with websocat
            if (true || this.contextualAuthorizationManager.hasAccess(EDIT, roomId)) {
                var newRoom = this.rooms.computeIfAbsent(roomId, rid -> new Room(rid, () -> this.rooms.remove(rid)));
                newRoom.register(session);
                this.roomsById.put(session.getId(), newRoom);
                // TODO: the String is not working with yjs, we need to move to a binary model, need to RTFM.
                session.addMessageHandler(String.class, message -> newRoom.broadcast(session.getId(), message));
            } else {
                try {
                    session.close(
                        new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Current user is not allowed to "
                            + "edit the current document"));
                } catch (IOException e) {
                    this.logger.warn("Failed to close session. Cause: [{}]", getRootCauseMessage(e));
                }
            }
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason)
    {
        super.onClose(session, closeReason);
        this.logger.info("BlocknoteRealtimeEndpoint opened. session [{}], close reason [{}]", session, closeReason);
        this.roomsById.get(session.getId()).disconnect(session);
    }
}
