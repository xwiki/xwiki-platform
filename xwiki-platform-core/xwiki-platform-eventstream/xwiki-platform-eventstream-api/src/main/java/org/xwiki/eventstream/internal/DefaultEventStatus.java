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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Default implementation for {@link EventStatus}.
 *
 * @version $Id$
 * @since 9.2RC1
 */
public class DefaultEventStatus extends DefaultEntityEvent implements EventStatus
{
    private boolean isRead;

    /**
     * Construct a DefaultEventStatus.
     * 
     * @param event the event concerned by the status
     * @param entityId the id of the entity concerned by the status
     * @param isRead either or not the entity as read the given entity
     */
    public DefaultEventStatus(Event event, String entityId, boolean isRead)
    {
        super(event, entityId);

        this.isRead = isRead;
    }

    @Override
    public boolean isRead()
    {
        return this.isRead;
    }

    /**
     * @param read either or not the entity as read the given entity
     */
    public void setRead(boolean read)
    {
        this.isRead = read;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @since 14.6RC1
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj instanceof EventStatus) {
            EventStatus otherEvent = (EventStatus) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(isRead(), otherEvent.isRead());

            return builder.build();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     * @since 14.6RC1
     */
    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(isRead());

        return builder.build();
    }

    @Override
    public String toString()
    {
        XWikiToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.appendSuper(super.toString());
        builder.append("read", isRead());

        return builder.toString();
    }
}
