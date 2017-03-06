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
package org.xwiki.eventstream.internal;

import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;

/**
 * @version $Id$
 */
public class DefaultEventStatus implements EventStatus
{
    private Event event;

    private String entityId;

    private boolean isRead;

    public DefaultEventStatus(Event event, String entityId, boolean isRead)
    {
        this.event = event;
        this.entityId = entityId;
        this.isRead = isRead;
    }

    @Override
    public Event getEvent()
    {
        return null;
    }

    @Override
    public String getEntityId()
    {
        return null;
    }

    @Override
    public boolean isRead()
    {
        return false;
    }

    public void setEvent(Event event)
    {
        this.event = event;
    }

    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }

    public void setRead(boolean read)
    {
        isRead = read;
    }
}
