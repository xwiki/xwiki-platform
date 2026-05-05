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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.user.UserReference;

/**
 * Represents a client connected to the Yjs WebSocket endpoint.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public class WebSocketClient extends Client
{
    /**
     * The WebSocket session associated to this client.
     */
    private final transient Session session;

    /**
     * Creates a new WebSocket client with the given user reference and WebSocket session.
     * 
     * @param userReference the user reference associated to this client
     * @param session the WebSocket session associated to this client
     */
    public WebSocketClient(UserReference userReference, Session session)
    {
        super(userReference);
        this.session = session;
    }

    /**
     * @return the WebSocket session associated to this client
     */
    public Session getSession()
    {
        return this.session;
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
        WebSocketClient other = (WebSocketClient) obj;
        return new EqualsBuilder().append(getId(), other.getId()).append(getUserReference(), other.getUserReference())
            .append(this.session, other.session).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(getId()).append(getUserReference()).append(this.session).toHashCode();
    }
}
