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

import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.Event;

/**
 * Default implementation for {@link EntityEvent}.
 *
 * @version $Id$
 * @since 12.6RC1
 */
public class DefaultEntityEvent implements EntityEvent
{
    private Event event;

    private String entityId;

    /**
     * Construct a DefaultEventStatus.
     * 
     * @param event the event concerned by the status
     * @param entityId the id of the entity concerned by the status
     */
    public DefaultEntityEvent(Event event, String entityId)
    {
        this.event = event;
        this.entityId = entityId;
    }

    @Override
    public Event getEvent()
    {
        return event;
    }

    @Override
    public String getEntityId()
    {
        return entityId;
    }

    /**
     * @param event the event concerned by the status
     */
    public void setEvent(Event event)
    {
        this.event = event;
    }

    /**
     * @param entityId the id of the entity concerned by the status
     */
    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }
}
