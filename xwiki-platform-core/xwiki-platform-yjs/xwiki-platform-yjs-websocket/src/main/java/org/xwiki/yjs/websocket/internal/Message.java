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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

/**
 * Represents a message sent/received by the WebSocket server.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public class Message
{
    private final byte[] bytes;

    private final MessageType type;

    private final Long clientId;

    /**
     * Creates a new message by parsing the given byte array.
     *
     * @param bytes the byte array to parse
     * @throws IOException if the given byte array is malformed and cannot be parsed as a message
     */
    public Message(byte[] bytes) throws IOException
    {
        this.bytes = bytes;
        CodedInputStream codedInputStream = CodedInputStream.newInstance(bytes);
        this.type = MessageType.from(codedInputStream);
        this.clientId = type == MessageType.JOIN ? codedInputStream.readUInt64() : null;
    }

    /**
     * @return the raw bytes of this message, as received from the client or as sent to the client
     */
    public byte[] getBytes()
    {
        return this.bytes;
    }

    /**
     * @return the type of this message
     */
    public MessageType getType()
    {
        return this.type;
    }

    /**
     * @return the client ID contained in this message, or {@code null} if this is not an authentication message
     */
    public Long getClientId()
    {
        return this.clientId;
    }

    /**
     * Builds a leave message for the given client ID.
     *
     * @param clientID the ID of the client that left the room
     * @return a message that can be used to notify the remaining clients in the room that the client with the given ID
     *         has left the room
     * @throws IOException if an error occurs while building the leave message
     */
    public static byte[] leave(Long clientID) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        // Write message type first.
        codedOutputStream.writeUInt64NoTag(4);
        codedOutputStream.writeUInt64NoTag(clientID);
        codedOutputStream.flush();
        return output.toByteArray();
    }

    /**
     * An empty SYNC_STEP_2 message looks like this: [0, 1, 2, 0, 0].
     * <table>
     * <tr>
     * <th>Byte</th>
     * <th>Value</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td>0</td>
     * <td>Message type: synchronization message</td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>1</td>
     * <td>Synchronization step: step 2</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>2</td>
     * <td>Length of the update payload: 2 bytes</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>0</td>
     * <td>0 clients/structs</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>0</td>
     * <td>0 deletes</td>
     * </table>
     * 
     * @return an empty SYNC_STEP_2 message, which is used to indicate the end of the synchronization process
     * @throws IOException if an error occurs while building the message
     */
    public static byte[] emptySyncStep2() throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        // This is a synchronization message.
        codedOutputStream.writeUInt64NoTag(0);
        // This is step 2 of the synchronization process.
        codedOutputStream.writeUInt64NoTag(1);
        // The length of the update payload is 2 bytes.
        // See https://www.palanikannan.com/blogs/yjs-snapshots-part-2-sync#the-wire-format-byte-by-byte .
        codedOutputStream.writeUInt64NoTag(2);
        // 0 clients/structs.
        codedOutputStream.writeUInt64NoTag(0);
        // 0 deletes.
        codedOutputStream.writeUInt64NoTag(0);
        codedOutputStream.flush();
        return output.toByteArray();
    }
}
