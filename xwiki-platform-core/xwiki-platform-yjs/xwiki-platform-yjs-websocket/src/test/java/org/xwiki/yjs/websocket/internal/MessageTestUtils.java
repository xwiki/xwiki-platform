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

import com.google.protobuf.CodedOutputStream;

/**
 * Utility class to create Yjs messages for tests.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public class MessageTestUtils
{
    /**
     * Creates a message used to identify the specified client.
     *
     * @param id the client id
     * @return the message used to identify the specified client
     * @throws Exception if an error occurs while creating the message
     */
    public static byte[] createIdMessage(long id) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(2);
        codedOutputStream.writeUInt64NoTag(id);
        codedOutputStream.flush();
        return output.toByteArray();
    }

    /**
     * @return the message used to start the synchronization of a Yjs document
     * @throws Exception if an error occurs while creating the message
     */
    public static byte[] createSyncStep1Message() throws Exception
    {
        return createSyncMessage(0, "");
    }

    /**
     * Creates a message used to update a Yjs document.
     *
     * @param content the content of the update message
     * @return the message used to update a Yjs document
     * @throws Exception if an error occurs while creating the message
     */
    public static byte[] createSyncUpdateMessage(String content) throws Exception
    {
        return createSyncMessage(2, content);
    }

    private static byte[] createSyncMessage(long syncType, String content) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(0);
        codedOutputStream.writeUInt64NoTag(syncType);
        codedOutputStream.writeByteArrayNoTag(content.getBytes());
        codedOutputStream.flush();
        return output.toByteArray();
    }

    /**
     * Creates a message used to update the awareness information of a Yjs document.
     *
     * @param content the content of the awareness message
     * @return the message used to update the awareness information of a Yjs document
     * @throws Exception if an error occurs while creating the message
     */
    public static byte[] createAwarenessMessage(String content) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(output);
        codedOutputStream.writeUInt64NoTag(1);
        codedOutputStream.writeByteArrayNoTag(content.getBytes());
        codedOutputStream.flush();
        return output.toByteArray();
    }
}
