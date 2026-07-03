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

import com.google.protobuf.CodedInputStream;

/**
 * Defines the type of messages that can be sent/received by the WebSocket server.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public enum MessageType
{
    /**
     * Sent by the client, it tells the server: "Here's how much I've seen from each client. Send me what I'm missing.".
     */
    SYNC_STEP_1,

    /**
     * Sent by the server in response to a {@link #SYNC_STEP_1} message, it contains the updates (operations) that the
     * client is missing (you can view it as a diff between the client and server state vectors).
     */
    SYNC_STEP_2,

    /**
     * An incremental update, contains the operations that need to be performed on a Yjs document.
     */
    SYNC_UPDATE,

    /**
     * Used by the client to notify the server about changes in the client's awareness state (e.g. the user's cursor
     * position or text selection). Some clients may send this messsage even when there is no actual change, just to
     * keep the connection alive. In that case they expect the server to acknowledge the message by sending it back to
     * them (at least when they are alone in the room).
     */
    AWARENESS_UPDATE,

    /**
     * Ask for the awareness states of all the other clients in the room.
     */
    AWARENESS_QUERY,

    /**
     * Sent by the client to authenticate (identify) itself to the server when joining a room.
     */
    JOIN,

    /**
     * Sent by the server to notify the members of a room that someone has left the room.
     */
    LEAVE;

    /**
     * Extracts the type of a message from the given byte array.
     *
     * @param message the message to extract the type from
     * @return the type of the given message, or {@code null} if the message type is unknown
     * @throws IOException if the message is malformed and its type cannot be extracted
     */
    public static MessageType from(byte[] message) throws IOException
    {
        return from(CodedInputStream.newInstance(message));
    }

    /**
     * Extracts the type of a message from the given {@link CodedInputStream}.
     *
     * @param codedInputStream the stream to extract the message type from
     * @return the type of the message read from the given stream, or {@code null} if the message type is unknown
     * @throws IOException if the message is malformed and its type cannot be extracted
     */
    public static MessageType from(CodedInputStream codedInputStream) throws IOException
    {
        long messageType = codedInputStream.readUInt64();
        return switch ((int) messageType) {
            case 0 -> switch ((int) codedInputStream.readUInt64()) {
                case 0 -> SYNC_STEP_1;
                case 1 -> SYNC_STEP_2;
                case 2 -> SYNC_UPDATE;
                default -> null;
            };
            case 1 -> AWARENESS_UPDATE;
            case 2 -> JOIN;
            case 3 -> AWARENESS_QUERY;
            case 4 -> LEAVE;
            default -> null;
        };
    }
}
