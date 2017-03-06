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
package com.xpn.xwiki.plugin.activitystream.impl;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventStatus;

/**
 * @version $Id$
 */
public class ActivityEventStatusImpl implements ActivityEventStatus, Serializable
{
    protected ActivityEvent activityEvent;

    protected String entityId;

    protected boolean isRead;

    @Override
    public ActivityEvent getActivityEvent()
    {
        return activityEvent;
    }

    @Override
    public String getEntityId()
    {
        return entityId;
    }

    @Override
    public boolean isRead()
    {
        return isRead;
    }

    public void setActivityEvent(ActivityEvent activityEvent)
    {
        this.activityEvent = activityEvent;
    }

    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }

    public void setRead(boolean read)
    {
        isRead = read;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof ActivityEventStatusImpl) {
            ActivityEventStatusImpl o = (ActivityEventStatusImpl) other;
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
