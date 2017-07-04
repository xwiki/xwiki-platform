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
package org.xwiki.notifications.internal.email;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.Event;

/**
 * This listener is responsible of starting triggers when specific events occurs in the wiki.
 *
 * @since 9.6-RC1
 * @version $Id$
 */
@Singleton
@Named(LiveNotificationEmailListener.NAME)
public class LiveNotificationEmailListener extends AbstractEventListener implements Initializable
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "Live Notification Email Listener";

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private LiveNotificationEmailManager liveNotificationEmailManager;

    @Inject
    private Logger logger;

    private NotificationGraceTimeThread notificationGraceTimeThread;

    private class NotificationGraceTimeThread extends Thread
    {
        private LiveNotificationEmailManager emailManager;

        public NotificationGraceTimeThread(LiveNotificationEmailManager emailManager)
        {
            this.emailManager = emailManager;
            this.setDaemon(true);
            this.setPriority(NORM_PRIORITY);
        }

        public void run()
        {
            DateTime nextWakeUpTime = DateTime.now();

            // If the nextWakeUpTime is null, the thead will die, but will be restarted by
            // LiveNotificationEmailListener#onEvent(Event, Object, Object) as soon as a new event is caught.
            while (nextWakeUpTime != null) {
                if (nextWakeUpTime.isAfterNow()) {
                    try {
                        Thread.sleep(nextWakeUpTime.getMillis() - DateTime.now().getMillis());
                    } catch (IllegalArgumentException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            return;
                        }
                    }
                }

                this.emailManager.run();

                nextWakeUpTime = this.emailManager.getNextExecutionDate();
            }
        }
    }

    /**
     * Constructs a new {@link LiveNotificationEmailListener}.
     */
    public LiveNotificationEmailListener()
    {
        super(NAME, new EventStreamAddedEvent());
    }

    @Override
    public void onEvent(Event event, Object o, Object o1)
    {
        try {
            EventStreamAddedEvent eventStreamEvent = (EventStreamAddedEvent) event;

            // We can’t directly store a list of RecordableEventDescriptors as some of them can be
            // dynamically defined at runtime.
            List<RecordableEventDescriptor> descriptorList =
                    this.recordableEventDescriptorManager.getRecordableEventDescriptors(true);

            // Try to match one of the given descriptors with the current event.
            for (RecordableEventDescriptor descriptor : descriptorList) {
                // Find a descriptor that corresponds to the given event
                if (descriptor.getEventType().equals(eventStreamEvent.getEvent().getType())) {
                    // Add the event to the live notification email queue
                    this.liveNotificationEmailManager.addEvent(eventStreamEvent.getEvent());

                    // If the notification grace time thread is not running, start it
                    if (!this.notificationGraceTimeThread.isAlive()) {
                        this.notificationGraceTimeThread.start();
                    }
                }
            }
        } catch (EventStreamException e) {
            logger.warn("Unable to retrieve a full list of RecordableEventDescriptor.", e);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize the local instance of NotificationGraceTimeThread
        this.notificationGraceTimeThread = new NotificationGraceTimeThread(this.liveNotificationEmailManager);
    }
}
