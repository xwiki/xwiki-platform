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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.NotificationConverter;
import org.xwiki.notifications.events.AllNotificationEvent;
import org.xwiki.notifications.events.NotificationEvent;

/**
 * Default converter for any type of NotificationEvent.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultNotificationConverter implements NotificationConverter
{
    private static final List<NotificationEvent> EVENTS = Arrays.asList(AllNotificationEvent.ALL_NOTIFICATION_EVENT);

    @Override
    public Event convert(NotificationEvent notificationEvent, String source, Object data) throws Exception
    {
        org.xwiki.eventstream.Event convertedEvent = new DefaultEvent();
        convertedEvent.setType(notificationEvent.getClass().getCanonicalName());
        convertedEvent.setApplication(source);
        convertedEvent.setBody((String) data);
        convertedEvent.setDate(new Date());
        convertedEvent.setTarget(notificationEvent.getTarget());
        return convertedEvent;
    }

    @Override
    public List<NotificationEvent> getSupportedEvents()
    {
        return EVENTS;
    }
}
