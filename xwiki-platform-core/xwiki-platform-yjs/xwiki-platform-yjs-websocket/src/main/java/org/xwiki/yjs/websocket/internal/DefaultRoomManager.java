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

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.websocket.Session;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Default implementation of {@link RoomManager}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Singleton
public class DefaultRoomManager implements RoomManager
{
    @Inject
    private Logger logger;

    @Inject
    private YjsEndpointConfiguration configuration;

    @Inject
    private PingPongManager pingPongManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    /**
     * The lock used to make sure join and leave operations are atomic.
     */
    private final Object lock = new Object();

    /**
     * The available rooms. Each room corresponds to a wiki page.
     */
    private final Map<DocumentReference, Room> rooms = new HashMap<>();

    /**
     * Each session is associated to at most one room. This map is used to find the room a WebSocket session is
     * associated with.
     */
    private final Map<Session, Room> roomsBySession = new HashMap<>();

    @Override
    public void join(Session session, DocumentReference roomReference)
    {
        synchronized (this.lock) {
            if (this.roomsBySession.containsKey(session)) {
                this.logger.warn("The session [{}] is already connected to the [{}] room.", session.getId(),
                    this.roomsBySession.get(session).getDocumentReference());
                return;
            }

            this.pingPongManager.startPinging(session);
            var room = get(roomReference, true);
            this.roomsBySession.put(session, room);
            room.join(session, this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE));
        }
    }

    @Override
    public void leave(Session session)
    {
        synchronized (this.lock) {
            this.pingPongManager.stopPinging(session);
            var room = this.roomsBySession.remove(session);
            if (room != null) {
                room.leave(session);
                removeIfEmpty(room);
            }
        }
    }

    @Override
    public Room get(DocumentReference roomReference, boolean createIfNotExists)
    {
        synchronized (this.lock) {
            if (createIfNotExists) {
                return this.rooms.computeIfAbsent(roomReference, newRoomReference -> new DefaultRoom(this,
                    newRoomReference, this.configuration, this.observationManager));
            } else {
                return this.rooms.get(roomReference);
            }
        }
    }

    @Override
    public boolean removeIfEmpty(Room room)
    {
        synchronized (this.lock) {
            if (room != null && room.isEmpty()) {
                return this.rooms.remove(room.getDocumentReference(), room);
            }
            return false;
        }
    }
}
