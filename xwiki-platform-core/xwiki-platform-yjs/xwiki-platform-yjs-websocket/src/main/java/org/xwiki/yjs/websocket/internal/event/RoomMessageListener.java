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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.yjs.websocket.internal.Room;
import org.xwiki.yjs.websocket.internal.RoomManager;

/**
 * Listens to {@link RoomMessageEvent} triggered on other cluster nodes and re-broadcasts the message to clients
 * connected to the current cluster node.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Named(RoomMessageListener.NAME)
@Singleton
public class RoomMessageListener extends AbstractEventListener
{
    /**
     * The unique identifier of the listener.
     */
    public static final String NAME = "org.xwiki.yjs.websocket.internal.event.RoomMessageListener";

    @Inject
    private RoomManager roomManager;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * Creates a new listener.
     */
    public RoomMessageListener()
    {
        super(NAME, new RoomMessageEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof RoomMessageEvent roomMessageEvent
            && this.remoteObservationManagerContext.isRemoteState()) {
            onRoomMessageEvent(roomMessageEvent, (byte[]) source);
        }
    }

    private void onRoomMessageEvent(RoomMessageEvent event, byte[] data)
    {
        Room room = this.roomManager.get(event.getRoomReference());
        if (room != null) {
            room.onMessage(event.getClient(), data);
        }
    }
}
