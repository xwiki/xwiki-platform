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
import java.nio.ByteBuffer;
import java.util.List;

import jakarta.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.user.UserReference;
import org.xwiki.websocket.AbstractPartialBinaryMessageHandler;
import org.xwiki.yjs.websocket.internal.event.RoomMessageEvent;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * This object stored the list of currently active sessions for a room. A room is usually containing the users editing a
 * given document in realtime.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public class DefaultRoom extends AbstractRoom
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoom.class);

    /**
     * The room manager managing this room.
     */
    private final RoomManager roomManager;

    private final ObservationManager observationManager;

    /**
     * Creates a new room, managed by the given room manager and associated to the specified wiki page.
     *
     * @param roomManager the room manager managing this room
     * @param documentReference the wiki page associated to this room
     * @param configuration the Yjs WebSocket endpoint configuration
     * @param observationManager the observation manager to use to trigger room events
     */
    public DefaultRoom(RoomManager roomManager, DocumentReference documentReference,
        YjsEndpointConfiguration configuration, ObservationManager observationManager)
    {
        super(documentReference, configuration);
        this.roomManager = roomManager;
        this.observationManager = observationManager;
    }

    @Override
    public void join(Session session, UserReference userReference)
    {
        synchronized (this.lock) {
            if (hasClient(session)) {
                LOGGER.warn("The session [{}] is already connected to the [{}] room.", session.getId(),
                    this.documentReference);
                return;
            }

            // We don't know the client id until we receive the identification message.
            Client client = new WebSocketClient(userReference, session);
            this.clients.add(client);

            session.addMessageHandler(new AbstractPartialBinaryMessageHandler(this.configuration.getMaxMessageSize())
            {
                @Override
                public void onMessage(byte[] message)
                {
                    DefaultRoom.this.onMessage(client, message);
                }
            });
        }
    }

    @Override
    public void leave(Session session)
    {
        List<Client> failedRecipients = List.of();

        synchronized (this.lock) {
            Client client = removeClient(session);
            // Broadcast the leave message only if the user has identified himself.
            if (client != null && client.getId() != null) {
                try {
                    failedRecipients = this.broadcast(client, Message.leave(client.getId()));
                } catch (IOException e) {
                    LOGGER.warn("Failed to broadcast leave message for client [{}]. Cause: [{}]", client,
                        getRootCauseMessage(e));
                }
            }
        }

        // Disconnect the clients we couldn't send the leave message to. We have to do this:
        // * outside of the broadcast loop, to avoid ConcurrentModificationException
        // * after releasing the lock, to avoid a potential deadlock with the room manager
        failedRecipients.forEach(this::disconnectClient);
    }

    @Override
    protected void disconnectClient(Client client)
    {
        if (client instanceof WebSocketClient webSocketClient) {
            // Disconnect a client from the current cluster node, for which we have the WebSocket session.
            this.roomManager.leave(webSocketClient.getSession());
        } else {
            // Disconnect a client from a different cluster node, for which we don't have the WebSocket session.
            super.disconnectClient(client);
            this.roomManager.removeIfEmpty(this);
        }
    }

    @Override
    protected List<Client> broadcast(Client fromClient, byte[] data, boolean includeFromClient)
    {
        this.observationManager.notify(new RoomMessageEvent(new Client(fromClient), this.documentReference), data);
        return super.broadcast(fromClient, data, includeFromClient);
    }

    @Override
    protected void sendMessage(Client toClient, byte[] data) throws IOException
    {
        if (toClient instanceof WebSocketClient webSocketClient) {
            webSocketClient.getSession().getBasicRemote().sendBinary(ByteBuffer.wrap(data).asReadOnlyBuffer());
        }
    }

    private boolean hasClient(Session session)
    {
        return this.clients.stream().anyMatch(client -> client instanceof WebSocketClient webSocketClient
            && webSocketClient.getSession().equals(session));
    }

    private Client removeClient(Session session)
    {
        Client client = this.clients.stream()
            .filter(c -> c instanceof WebSocketClient webSocketClient && webSocketClient.getSession().equals(session))
            .findFirst().orElse(null);
        if (client != null) {
            this.clients.remove(client);
        }
        return client;
    }
}
