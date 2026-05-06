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
package org.xwiki.yjs.websocket.internal.event;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.yjs.websocket.internal.Client;

/**
 * Event triggered when a message is received in a room.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public class RoomMessageEvent implements Event, Serializable
{
    /**
     * The client that sent the message.
     */
    private final Client client;

    /**
     * The reference of the room where the message was sent.
     */
    private final DocumentReference roomReference;

    /**
     * A new message was sent by the given client in the specified room.
     *
     * @param client the client that sent the message
     * @param roomReference the reference of the room where the message was sent
     */
    public RoomMessageEvent(Client client, DocumentReference roomReference)
    {
        this.client = client;
        this.roomReference = roomReference;
    }

    /**
     * Listen to all room message events (any client and any room).
     */
    public RoomMessageEvent()
    {
        this(null, null);
    }

    /**
     * @return the client that sent the message
     */
    public Client getClient()
    {
        return this.client;
    }

    /**
     * @return the reference of the room where the message was sent
     */
    public DocumentReference getRoomReference()
    {
        return this.roomReference;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof RoomMessageEvent event) {
            return (this.client == null || this.client.equals(event.client))
                && (this.roomReference == null || this.roomReference.equals(event.roomReference));
        }
        return false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RoomMessageEvent other = (RoomMessageEvent) obj;
        return new EqualsBuilder().append(this.client, other.client).append(this.roomReference, other.roomReference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(this.client).append(this.roomReference).toHashCode();
    }
}
