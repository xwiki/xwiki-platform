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

/**
 * Manages the rooms used for realtime collaboration. Each wiki page has an associated room that users can join to edit
 * the page in realtime.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Role
public interface RoomManager
{
    /**
     * Join the collaboration room associated to the specified wiki page. A WebSocket session can be used to join only
     * one room at a time.
     * 
     * @param session the WebSocket session of the user joining the room
     * @param roomReference specifies the wiki page for which to join the collaboration room
     */
    void join(Session session, DocumentReference roomReference);

    /**
     * Leave the room the specified WebSocket session is connected to. If the given session is not connected to any
     * room, then this method does nothing.
     * 
     * @param session the WebSocket session of the user leaving the room
     */
    void leave(Session session);

    /**
     * Get the room associated to the specified wiki page.
     *
     * @param roomReference the wiki page for which to get the associated room
     * @return the room associated to the specified wiki page, or {@code null} if it doesn't exist
     */
    default Room get(DocumentReference roomReference)
    {
        return get(roomReference, false);
    }

    /**
     * Get the room associated to the specified wiki page, and optionally create it if it doesn't exist yet.
     *
     * @param roomReference the wiki page for which to get the associated room
     * @param createIfNotExists indicates whether to create the room if it doesn't exist yet
     * @return the room associated to the specified wiki page, or {@code null} if it doesn't exist and shouldn't be
     *         created
     */
    Room get(DocumentReference roomReference, boolean createIfNotExists);

    /**
     * Remove the given room if it's empty (i.e. if it doesn't have any connected clients). This method is used by rooms
     * to remove themselves when they don't have any more clients, in order to avoid keeping empty rooms in memory.
     *
     * @param room the room to remove if it's empty
     * @return {@code true} if the room has been removed, or {@code false} if it hasn't been removed because it's not
     *         empty or because the given room is not managed by this room manager
     */
    boolean removeIfEmpty(Room room);
}
