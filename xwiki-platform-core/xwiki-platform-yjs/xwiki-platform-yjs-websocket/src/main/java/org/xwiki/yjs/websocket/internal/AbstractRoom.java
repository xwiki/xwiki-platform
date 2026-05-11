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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Base implementation of {@link Room}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public abstract class AbstractRoom implements Room
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRoom.class);

    /**
     * The wiki page associated to this room. Each wiki page has a corresponding room that users can join to edit in
     * realtime.
     */
    protected final DocumentReference documentReference;

    protected final YjsEndpointConfiguration configuration;

    /**
     * The lock used to make sure join, leave and message handling are atomic.
     */
    protected final Object lock = new Object();

    /**
     * The clients currently connected to this room. We use a List instead of a Set because the client objects are
     * mutable (the client ID is set only after receiving the JOIN message).
     */
    protected final List<Client> clients = new LinkedList<>();

    /**
     * The binary synchronization updates (message type 0, sync type 2) observed in this room.
     */
    private final List<byte[]> storedUpdates = new LinkedList<>();

    /**
     * The number of bytes used to record all the synchronization update messages received so far by this room.
     */
    private long historySize;

    protected AbstractRoom(DocumentReference documentReference, YjsEndpointConfiguration configuration)
    {
        this.documentReference = documentReference;
        this.configuration = configuration;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock) {
            return this.clients.isEmpty();
        }
    }

    @Override
    public void onMessage(Client fromClient, byte[] data)
    {
        List<Client> clientsToDisconnect = new LinkedList<>();

        synchronized (this.lock) {
            try {
                Message message = new Message(data);
                switch (message.getType()) {
                    case JOIN:
                        // Make sure the client is not added twice.
                        this.clients.remove(fromClient);
                        fromClient.setId(message.getClientId());
                        this.clients.add(fromClient);
                        break;
                    case LEAVE:
                        clientsToDisconnect.add(fromClient);
                        break;
                    case SYNC_STEP_1:
                        if (!sendStoredUpdates(fromClient)) {
                            // Failed to send the stored updates to the client.
                            clientsToDisconnect.add(fromClient);
                        }
                        break;
                    case SYNC_UPDATE:
                        // Record the update message in order to be able to replay it later to clients that request
                        // synchronization.
                        this.storedUpdates.add(data.clone());
                        this.historySize += data.length;
                        if (this.historySize > this.configuration.getMaxHistorySize()) {
                            LOGGER.warn("Room [{}] has reached the maximum history size of [{}] bytes. Closing.",
                                this.documentReference, this.configuration.getMaxHistorySize());
                            // Disconnect all clients.
                            clientsToDisconnect.addAll(this.clients);
                        }
                        break;
                    default:
                        // Nothing to do here. We'll just broadcast the message to the other clients, below.
                }

                if (message.getType() != MessageType.SYNC_STEP_1) {
                    // We include the sender in the broadcast for awareness messages in order to prevent Yjs client from
                    // closing the connection when there is a single user in the room. Although Yjs client replies to
                    // ping messages sent by the server, it doesn't take them into account when deciding to close the
                    // connection due to inactivity.
                    clientsToDisconnect
                        .addAll(broadcast(fromClient, data, message.getType() == MessageType.AWARENESS_UPDATE));
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read message. Cause: [{}]", getRootCauseMessage(e));
            }
        }

        // Disconnect the users we couldn't send the message to. We have to do this:
        // * outside of the broadcast loop, to avoid ConcurrentModificationException
        // * after releasing the lock, to avoid a potential deadlock with the room manager
        clientsToDisconnect.forEach(this::disconnectClient);
    }

    /**
     * Sends all stored synchronization updates to the given client, followed by an empty
     * {@link MessageType#SYNC_STEP_2} message to indicate the end of the synchronization process.
     *
     * @param toClient the client that should receive the updates
     * @return {@code true} if all updates were sent successfully, {@code false} otherwise
     */
    protected boolean sendStoredUpdates(Client toClient)
    {
        for (byte[] update : this.storedUpdates) {
            try {
                sendMessage(toClient, update);
            } catch (IOException e) {
                LOGGER.warn("Failed to replay stored update to client [{}]. Cause: [{}]", toClient,
                    getRootCauseMessage(e));
                return false;
            }
        }

        try {
            // Send an empty SYNC_STEP_2 message to indicate the end of the synchronization process.
            sendMessage(toClient, Message.emptySyncStep2());
        } catch (IOException e) {
            LOGGER.warn("Failed to send end of synchronization message to client [{}]. Cause: [{}]", toClient,
                getRootCauseMessage(e));
            return false;
        }

        return true;
    }

    /**
     * Send a binary message on behalf of the specified client to all the clients connected to the room, except for the
     * client sending the message.
     *
     * @param fromClient the client sending the message
     * @param data the message to broadcast to all other clients connected the room
     */
    protected List<Client> broadcast(Client fromClient, byte[] data)
    {
        return broadcast(fromClient, data, false);
    }

    /**
     * Send a binary message on behalf of the specified client to all the clients connected to the room.
     *
     * @param fromClient the client sending the message
     * @param data the message to broadcast to all other clients connected the room
     * @param includeFromClient whether the message should also be sent to the client sending the message
     */
    protected List<Client> broadcast(Client fromClient, byte[] data, boolean includeFromClient)
    {
        List<Client> failedRecipients = new LinkedList<>();
        Stream<Client> toClients = this.clients.stream();
        if (!includeFromClient) {
            toClients = toClients.filter(toClient -> !Objects.equals(fromClient, toClient));
        }
        toClients.forEach(toClient -> {
            try {
                sendMessage(toClient, data);
            } catch (IOException e) {
                LOGGER.warn("Failed to send message to client [{}]. Cause: [{}]", toClient, getRootCauseMessage(e));
                failedRecipients.add(toClient);
            }
        });
        return failedRecipients;
    }

    /**
     * Disconnect the given client from the room. This method is called when a client leaves the room or when we fail to
     * send a message to it.
     * 
     * @param client the client to disconnect
     */
    protected void disconnectClient(Client client)
    {
        this.clients.remove(client);
    }

    /**
     * Send a binary message to the given client.
     *
     * @param toClient the client to send the message to
     * @param data the message to send
     * @throws IOException if an error occurs while sending the message
     */
    protected abstract void sendMessage(Client toClient, byte[] data) throws IOException;
}
