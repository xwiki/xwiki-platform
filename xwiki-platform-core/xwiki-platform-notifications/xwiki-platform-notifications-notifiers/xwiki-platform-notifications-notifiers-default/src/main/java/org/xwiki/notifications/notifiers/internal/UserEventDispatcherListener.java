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

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Receive events and dispatch them for each user.
 * 
 * @version $Id$
 * @since 12.1RC1
 */
@Component
@Singleton
@Named(UserEventDispatcherListener.NAME)
public class UserEventDispatcherListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.notifications.notifiers.internal.UserEventDispatcherListener";

    @Inject
    private UserEventDispatcherScheduler scheduler;

    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    private RemoteObservationManagerContext remoteState;

    /**
     * Configure the listener.
     */
    public UserEventDispatcherListener()
    {
        super(NAME, new EventStreamAddedEvent(), new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Don't do anything if notifications in general is disabled
        if (this.notificationConfiguration.isEnabled()) {
            if (event instanceof EventStreamAddedEvent) {
                if (!this.remoteState.isRemoteState()) {
                    // Make sure to wakeup dispatcher
                    this.scheduler.onEvent((org.xwiki.eventstream.Event) source);
                }
            } else if (event instanceof ApplicationReadyEvent) {
                this.scheduler.initialize();
            }
        }
    }
}
