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

import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import org.xwiki.component.annotation.Role;

/**
 * Configuration for {@link YjsEndpoint}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Role
public interface YjsEndpointConfiguration
{
    /**
     * @see Session#getMaxIdleTimeout()
     * @return the number of milliseconds before this conversation may be closed by the container if it is inactive,
     *         i.e. no messages are either sent or received in that time
     */
    long getMaxIdleTimeout();

    /**
     * @see Session#getMaxBinaryMessageBufferSize()
     * @return the maximum length of incoming binary messages that this Session can buffer; ff the implementation
     *         receives a binary message that it cannot buffer because it is too large, it must close the session with a
     *         close code of {@link CloseReason.CloseCodes.TOO_BIG}
     */
    int getMaxBinaryMessageBufferSize();

    /**
     * @see Session#getMaxTextMessageBufferSize()
     * @return the maximum length of incoming text messages that this Session can buffer; if the implementation receives
     *         a text message that it cannot buffer because it is too large, it must close the session with a close code
     *         of {@link CloseReason.CloseCodes.TOO_BIG}
     */
    int getMaxTextMessageBufferSize();

    /**
     * @return the maximum length of incoming messages that the Yjs endpoint will accept
     */
    int getMaxMessageSize();

    /**
     * @return the interval in milliseconds between server-side WebSocket ping messages
     */
    long getPingInterval();

    /**
     * @return the maximum number of consecutive missed pong responses allowed before a WebSocket session is closed
     */
    int getPingMaxMissedPongs();

    /**
     * Without having an authoritative Y.Doc as source of truth, each room has to record the received synchronization
     * update messages in order to replay them to new clients joining the room.
     * 
     * @return the maximum number of bytes a room can use to record all the synchronization update messages it receives
     *         from connected clients
     */
    long getMaxHistorySize();
}
