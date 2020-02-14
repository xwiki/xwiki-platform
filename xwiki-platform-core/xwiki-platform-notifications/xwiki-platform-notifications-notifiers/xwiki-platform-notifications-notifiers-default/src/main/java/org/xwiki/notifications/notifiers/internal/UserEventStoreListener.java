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
package org.xwiki.notifications.notifiers.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Receive events and dispatch them for each user.
 * 
 * @version $Id$
 * @since 12.1RC1
 */
@Component
@Singleton
@Named(UserEventStoreListener.NAME)
public class UserEventStoreListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.notifications.notifiers.internal.UserEventStore";

    @Inject
    private EventStatusManager statuses;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Logger logger;

    /**
     * Configure the listener.
     */
    public UserEventStoreListener()
    {
        super(NAME, new UserNotificationEvent(NotificationFormat.ALERT));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        UserNotificationEvent userEvent = (UserNotificationEvent) event;
        org.xwiki.eventstream.Event streamEvent = (org.xwiki.eventstream.Event) source;

        // Store the corresponding status
        try {
            String userId = this.entityReferenceSerializer.serialize(userEvent.getUserReference());

            this.statuses.saveEventStatus(new DefaultEventStatus(streamEvent, userId, false));
        } catch (Exception e) {
            this.logger.error("Failed to store the status of the event [{}] for the user [{}]", streamEvent.getId(),
                userEvent.getUserReference(), e);
        }
    }
}
