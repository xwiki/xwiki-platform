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
package org.xwiki.notifications.page;

import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.stability.Unstable;

/**
 * Generic event for page-defined notifications.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Unstable
public class PageNotificationEvent implements RecordableEvent
{
    private String eventName;
    private PageNotificationEventDescriptor descriptor;

    /**
     * Creates a PageNotificationEvent.
     *
     * @param descriptor the event descriptor associated to the event
     */
    public PageNotificationEvent(PageNotificationEventDescriptor descriptor)
    {
        this.descriptor = descriptor;
        this.eventName = descriptor.getEventName();
    }

    /**
     * Creates a {@link PageNotificationEvent} with a blank eventName and a null descriptor.
     */
    public PageNotificationEvent()
    {
    }

    /**
     * @return the event descriptor associated to the event
     */
    public PageNotificationEventDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent != null && otherEvent instanceof PageNotificationEvent;
    }
}
