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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.NotificationConverter;
import org.xwiki.notifications.events.AllNotificationEvent;
import org.xwiki.notifications.events.NotificationEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Store the notification inside the event stream.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("NotificationEventListener")
public class NotificationEventListener extends AbstractEventListener
{
    @Inject
    private EventStream eventStream;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Construct a NotificationEventListener.
     */
    public NotificationEventListener()
    {
        super("NotificationEventListener", AllNotificationEvent.ALL_NOTIFICATION_EVENT);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String eventType = event.getClass().getCanonicalName();
        try {
            // Lookup the correct converter
            NotificationConverter converter;
            if (componentManager.hasComponent(NotificationConverter.class, eventType)) {
                converter = componentManager.getInstance(NotificationConverter.class, eventType);
            } else {
                // Fallback to the default converter
                converter = componentManager.getInstance(NotificationConverter.class);
            }

            // Convert the event
            org.xwiki.eventstream.Event convertedEvent
                    = converter.convert((NotificationEvent) event, (String) source, data);

            // Save the event in the event stream
            // TODO: handle the audience
            eventStream.addEvent(convertedEvent);
        } catch (ComponentLookupException e) {
            logger.warn("Failed to lookup a converter for the event [{}].", eventType, e);
        } catch (Exception e) {
            logger.warn("Failed to convert the event [{}].", eventType, e);
        }
    }
}
