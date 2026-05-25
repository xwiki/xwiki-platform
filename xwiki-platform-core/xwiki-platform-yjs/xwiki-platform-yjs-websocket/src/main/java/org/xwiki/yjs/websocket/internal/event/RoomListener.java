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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.yjs.websocket.internal.Message;
import org.xwiki.yjs.websocket.internal.MessageType;
import org.xwiki.yjs.websocket.internal.Room;
import org.xwiki.yjs.websocket.internal.RoomManager;
import org.xwiki.yjs.websocket.internal.RoomScriptAuthorTracker;
import org.xwiki.yjs.websocket.internal.ScriptAuthorChange;

/**
 * Listens to {@link RoomMessageEvent} triggered by Yjs collaboration rooms and:
 * <ul>
 * <li>if the event is remote (originates from a different cluster node), re-broadcasts the message to clients connected
 * to the current cluster node</li>
 * <li>updates the script author associated to the room.</li>
 * </ul>
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Named(RoomListener.NAME)
@Singleton
public class RoomListener extends AbstractEventListener
{
    /**
     * The unique identifier of the listener.
     */
    public static final String NAME = "org.xwiki.yjs.websocket.internal.event.RoomListener";

    @Inject
    private Logger logger;

    @Inject
    private RoomManager roomManager;

    @Inject
    private RoomScriptAuthorTracker roomScriptAuthorTracker;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * Creates a new listener.
     */
    public RoomListener()
    {
        super(NAME, new RoomMessageEvent(), new RoomScriptAuthorChangeEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        boolean isRemote = this.remoteObservationManagerContext.isRemoteState();
        if (event instanceof RoomMessageEvent roomMessageEvent) {
            onRoomMessageEvent(roomMessageEvent, (byte[]) source, isRemote);
        } else if (event instanceof RoomScriptAuthorChangeEvent roomScriptAuthorChangeEvent) {
            ScriptAuthorChange scriptAuthorChange = (ScriptAuthorChange) source;
            this.roomScriptAuthorTracker.setScriptAuthor(roomScriptAuthorChangeEvent.getRoomReference(),
                scriptAuthorChange);
        }
    }

    private void onRoomMessageEvent(RoomMessageEvent event, byte[] data, boolean isRemote)
    {
        if (isRemote) {
            // Re-broadcast the message to clients connected to the current cluster node.
            Room room = this.roomManager.get(event.getRoomReference(), true);
            room.onMessage(event.getClient(), data);
        } else if (getMessageType(data) == MessageType.SYNC_UPDATE) {
            // Update the script author only when the content is updated (not when users join, leave or change their
            // awareness state like the cursor position). We do this only for local events because we need the XWiki
            // context to determine access rights for the message sender, and the XWiki context is initialized based
            // on the WebSocket session, which is not shared across cluster nodes. We have the WebSocket session only
            // for local clients, that are connected directly to the current cluster node.
            this.roomScriptAuthorTracker.maybeSetScriptAuthor(event.getRoomReference(),
                event.getClient().getUserReference());
        }
    }

    private MessageType getMessageType(byte[] data)
    {
        try {
            Message message = new Message(data);
            return message.getType();
        } catch (Exception e) {
            this.logger.warn("Failed to determine the message type. Root cause: [{}].",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
