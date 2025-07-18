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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.websocket.Session;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.component.descriptor.ComponentInstantiationStrategy.PER_LOOKUP;

/**
 * This object stored the list of currently active sessions for a room. A room is usually containing the users editing a
 * given document in realtime.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Component(roles = Room.class)
@InstantiationStrategy(PER_LOOKUP)
public class Room
{
    @Inject
    private Logger logger;

    private Runnable clearRoom;

    private final Map<Session, Long> sessionIds = new ConcurrentHashMap<>();

    /**
     * Initialize the room. This method must be call directly after instantiation.
     *
     * @param clearRoom a clear room runnable action, that needs to be called once the room becomes empty again.
     */
    public void init(Runnable clearRoom)
    {
        this.clearRoom = clearRoom;
    }

    /**
     * Disconnect a session from the room.
     *
     * @param session the session to disconnect.
     */
    public void disconnect(Session session)
    {
        Long clientID = this.sessionIds.get(session);
        if (clientID == null) {
            return;
        }
        try {
            this.broadcast(session, buildDisconnectMessage(clientID));
        } catch (IOException e) {
            this.logger.warn("Failed to build the disconnect message for client id [{}]. Cause: [{}]", clientID,
                getRootCauseMessage(e));
        }

        this.sessionIds.remove(session);
        if (this.sessionIds.isEmpty()) {
            this.clearRoom.run();
        }
    }

    private static ByteBuffer buildDisconnectMessage(Long clientID) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(4);
        codedOutputStream.writeUInt64NoTag(clientID);
        codedOutputStream.flush();
        return ByteBuffer.wrap(output.toByteArray());
    }

    /**
     * Handles a message. Reads the message and check the first integer. If its value is {@code 2}, save the second
     * integer and save it as the client id of the current session. That way, when the session is closed, the id of the
     * disconnected session is broadcasted to all other clients of the same room. Clients can then remove the client id
     * from their local awareness state (i.e., the cursor of selection of the closed session are quickly hidden instead
     * of waiting for a timeout). If the value is not {@code 2}, the message is instead broadcasted to all other
     * sessions connected to the same room. We perform this logic to have a reactive handling of awareness without
     * having to implement fully yjs. If a good yjs library for java is available someday, this logic could be improved
     * to change the awareness server side.
     *
     * @param session the session from which the message originated
     * @param stream the input steam of the received message
     */
    public void handleMessage(Session session, InputStream stream)
    {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(stream.readAllBytes());
            CodedInputStream codedInputStream = CodedInputStream.newInstance(byteBuffer.array());
            // Read the first unsigned int of the message, of it's 2 (0 being a synchronization message, and 1 an
            // awareness message)
            if (codedInputStream.readUInt64() == 2) {
                var clientId = codedInputStream.readUInt64();
                // Preserve the client id to find it back when the session closes. Nothing specific is required
                // server-side during session start. When a user joins the session they update their awareness status
                // locally, then the change is propagated to other members of the session by broadcasting the patch as a
                // usual yjs doc patch.
                this.sessionIds.put(session, clientId);
            } else {
                // Rewind the buffer so that the broadcast use a non-consumed buffer.
                byteBuffer.rewind();
                broadcast(session, byteBuffer);
            }
        } catch (IOException e) {
            this.logger.warn("Failed to read a message. Cause: [{}]", getRootCauseMessage(e));
        }
    }

    /**
     * Send a binary message to all the sessions of the room, except the session sending the message.
     *
     * @param fromSession the session sending the message
     * @param data the message to broadcast to all other sessions of the room
     */
    public void broadcast(Session fromSession, ByteBuffer data)
    {
        this.sessionIds.keySet()
            .stream()
            .filter(toSession -> !Objects.equals(fromSession.getId(), toSession.getId()))
            .forEach(toSession -> {
                try {
                    toSession.getBasicRemote().sendBinary(data);
                } catch (IOException e) {
                    // We disconnect the session in case of error
                    this.disconnect(toSession);
                }
            });
    }

    /**
     * @return an immutable copy of the currently registered session ids
     */
    public Map<Session, Long> getSessionIds()
    {
        return ImmutableMap.copyOf(this.sessionIds);
    }
}
