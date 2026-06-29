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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.yjs.websocket.internal.ScriptAuthorChange;

/**
 * An event triggered when the script author associated to a collaboration room changes. The code triggering the event
 * is expected to pass the following parameters:
 * <ul>
 * <li>source: the {@link ScriptAuthorChange} describing the change.</li>
 * </ul>
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public class RoomScriptAuthorChangeEvent extends AbstractRoomEvent
{
    /**
     * Triggered when the script author changes for any room.
     */
    public RoomScriptAuthorChangeEvent()
    {
        super(null);
    }

    /**
     * Triggered when the script author associated to the specified room has changed.
     *
     * @param roomReference the reference of the room whose script author has changed
     */
    public RoomScriptAuthorChangeEvent(DocumentReference roomReference)
    {
        super(roomReference);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof RoomScriptAuthorChangeEvent;
    }
}
