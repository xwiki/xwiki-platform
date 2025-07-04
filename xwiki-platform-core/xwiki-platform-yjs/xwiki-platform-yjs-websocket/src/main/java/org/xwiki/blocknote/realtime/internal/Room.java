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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.Session;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

/**
 * @version $Id$
 * @since 17.6.0RC1
 */
public class Room
{
    private final Runnable clearRoom;

    private final Map<Session, Long> sessionIds = new ConcurrentHashMap<>();

    public Room(Runnable clearRoom)
    {
        this.clearRoom = clearRoom;
    }

    public void disconnect(Session session)
    {
        Long clientID = this.sessionIds.get(session);
        if(clientID == null) {
            return;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        try {
            codedOutputStream.writeUInt64NoTag(4);
            codedOutputStream.writeUInt64NoTag(clientID);
            codedOutputStream.flush();
            this.broadcast(session, ByteBuffer.wrap(output.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.sessionIds.remove(session);
        if (this.sessionIds.isEmpty()) {
            // TODO: check if really thead safe, what's happening is another user joins the session at the same time
            // the last one is leaving?
            this.clearRoom.run();
        }
    }

    public void register(Session sessionId)
    {
        // TODO: remove this method
//        this.sessionIds.put(sessionId, -1L);
    }

    // todo: allow to send two kinds of message 1) user left 2) a message with a payload
    // todo: check how to customize the client y-websocket to handle this.
    public void handleMessage(Session session, InputStream message)
    {
        try {
            ByteBuffer wrap = ByteBuffer.wrap(message.readAllBytes());
            CodedInputStream codedInputStream = CodedInputStream.newInstance(wrap.array());
            var messageId = codedInputStream.readUInt64();
            if(messageId == 2) {
                var clientId = codedInputStream.readUInt64();
                this.sessionIds.put(session, clientId);
            } else {
                wrap.rewind();
                broadcast(session, wrap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(Session session, ByteBuffer wrap) throws IOException
    {
        for (Session session2 : this.sessionIds.keySet()) {
            if (!Objects.equals(session.getId(), session2.getId())) {
                try {
                    session2.getBasicRemote().sendBinary(wrap);
                } catch (IOException e) {
                    // We disconnect the session in case of error
                    this.disconnect(session2);
                }
            }
        }
    }
}
