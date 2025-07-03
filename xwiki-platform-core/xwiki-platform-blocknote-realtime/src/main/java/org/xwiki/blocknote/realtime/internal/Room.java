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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.Session;

import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 * @since x.y.z
 */
public class Room
{
    private final DocumentReference reference;

    private final Runnable clearRoom;

    // FIXME: can probably be a threadsafe set.
    private final Map<Session, Object> sessionIds = new ConcurrentHashMap<>();

    public Room(DocumentReference reference, Runnable clearRoom)
    {
        this.reference = reference;
        this.clearRoom = clearRoom;
    }

    public void disconnect(Session session)
    {
        this.sessionIds.remove(session);
        this.sessionIds.forEach((s, o) -> { });

        // TODO: replace left with a structure message
        this.broadcast(session.getId(), "%s left".formatted(session.getId()));

        if (this.sessionIds.isEmpty()) {
            // TODO: check if really thead safe, what's happening is another user joins the session at the same time
            // the last one is leaving?
            this.clearRoom.run();
        }
    }

    public void register(Session sessionId)
    {
        this.sessionIds.put(sessionId, new Object());
    }

    // todo: allow to send two kinds of message 1) user left 2) a message with a payload
    // todo: check how to customize the client y-websocket to handle this.
    public void broadcast(String sessionId, String message)
    {
        this.sessionIds.keySet().forEach(session -> {
            if(!Objects.equals(session.getId(), sessionId)) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    // TODO: add logger handling.
                }
            }
        });
    }
}
