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
package org.xwiki.eventstream.store.internal;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.text.StringUtils;

import java.io.Serializable;

/**
 * A Legacy Event Status is the equivalent of {@link EventStatus} for {@link LegacyEvent}.
 *
 * TODO: migrate the database schema so we don't need to use this legacy class anymore.
 *
 * @since 11.1RC1
 * @version $Id$
 */
// The class must be Serializable or Hibernate won't accept it
public class LegacyEventStatus implements Serializable
{
    private LegacyEvent activityEvent;

    private String entityId;

    private boolean isRead;

    /**
     * @return the event concerned by the status
     */
    public LegacyEvent getActivityEvent()
    {
        return activityEvent;
    }

    /**
     * @return the id of the entity (a user or a group) concerned by the status
     */
    public String getEntityId()
    {
        return entityId;
    }

    /**
     * @return either or nor the event has been read by the entity
     */
    public boolean isRead()
    {
        return isRead;
    }

    /**
     * @param activityEvent the event concerned by the status
     */
    public void setActivityEvent(LegacyEvent activityEvent)
    {
        this.activityEvent = activityEvent;
    }

    /**
     * @param entityId the id of the entity (a user or a group) concerned by the status
     */
    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }

    /**
     * @param read either or nor the event has been read by the entity
     */
    public void setRead(boolean read)
    {
        isRead = read;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof LegacyEventStatus) {
            LegacyEventStatus o = (LegacyEventStatus) other;
            return StringUtils.equals(activityEvent.getEventId(), o.activityEvent.getEventId())
                    && StringUtils.equals(entityId, o.entityId) && isRead == o.isRead();
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        return hashCodeBuilder.append(activityEvent.getEventId().hashCode())
                .append(entityId.hashCode()).append(isRead).hashCode();
    }
}
