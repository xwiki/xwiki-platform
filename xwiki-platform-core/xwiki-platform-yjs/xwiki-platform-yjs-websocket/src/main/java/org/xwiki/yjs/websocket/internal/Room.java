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

import jakarta.websocket.Session;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

/**
 * Each wiki page has a corresponding room that users can join to edit the page in realtime.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Role
public interface Room
{
    /**
     * @return the reference of the wiki page associated to this room
     */
    DocumentReference getDocumentReference();

    /**
     * Join this room. Messages received through the given WebSocket session will be forwarded to the all the other
     * members of the room. Messages forwarded from other members of the room will be sent to the given WebSocket
     * session.
     *
     * @param session the WebSocket session to use to forward messages to and from this room.
     * @param userReference the user joining the room
     */
    void join(Session session, UserReference userReference);

    /**
     * Leave this room.
     * 
     * @param session the WebSocket session to disconnect from this room
     */
    void leave(Session session);

    /**
     * @return {@code true} if this room is empty (i.e. no user is currently connected to it) or {@code false}
     *         otherwise.
     */
    boolean isEmpty();

    /**
     * Handle the given message received from the specified client which is supposed to be connected to this room.
     *
     * @param fromClient the client that sent the message (is must be connected to this room)
     * @param data the message data (must be a valid Yjs message)
     */
    void onMessage(Client fromClient, byte[] data);
}
