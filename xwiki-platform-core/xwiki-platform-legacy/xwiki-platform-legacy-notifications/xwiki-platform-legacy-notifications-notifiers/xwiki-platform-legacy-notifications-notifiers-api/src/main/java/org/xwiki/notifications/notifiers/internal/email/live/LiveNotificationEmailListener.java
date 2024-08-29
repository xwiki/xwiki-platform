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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * This listener is responsible of starting triggers when specific events occurs in the wiki.
 *
 * @since 9.6RC1
 * @version $Id$
 * @deprecated This component is only used in case of post-filtering events. We stopped supporting those.
 */
@Component
@Singleton
@Named(LiveNotificationEmailListener.NAME)
@Deprecated(since = "15.5RC1")
public class LiveNotificationEmailListener extends AbstractEventListener
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
    private NotificationConfiguration notificationConfiguration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private RemoteObservationManagerContext remoteState;

    @Inject
    private Logger logger;

    private Thread notificationGraceTimeThread;

    /**
     * Thread used for triggering {@link LiveNotificationEmailManager#run()} when needed (ie : when an event grace time
     * has ended).
     */
    private final class NotificationGraceTimeRunnable implements Runnable
    {
        @Override
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

                liveNotificationEmailManager.run();

                nextWakeUpTime = liveNotificationEmailManager.getNextExecutionDate();
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
        // Check if the notifications are enabled in the wiki and if the mail option for the
        // notifications is enabled.
        if (!this.remoteState.isRemoteState() && this.notificationConfiguration.isEnabled()
            && this.notificationConfiguration.areEmailsEnabled()
            && !this.notificationConfiguration.isEventPrefilteringEnabled()) {
            try {
                org.xwiki.eventstream.Event eventStreamEvent = (org.xwiki.eventstream.Event) o;

                // We can’t directly store a list of RecordableEventDescriptors as some of them can be
                // dynamically defined at runtime.
                List<RecordableEventDescriptor> descriptorList =
                    this.recordableEventDescriptorManager.getRecordableEventDescriptors(true);

                // Try to match one of the given descriptors with the current event.
                for (RecordableEventDescriptor descriptor : descriptorList) {
                    // Find a descriptor that corresponds to the given event
                    if (descriptor.getEventType().equals(eventStreamEvent.getType())) {
                        // Add the event to the live notification email queue
                        this.liveNotificationEmailManager.addEvent(eventStreamEvent);

                        this.startNotificationThread();
                    }
                }

            } catch (EventStreamException e) {
                logger.warn("Unable to retrieve a full list of RecordableEventDescriptor.", e);
            }
        }
    }

    /**
     * If the notification grace time thread is not running, start it.
     */
    private synchronized void startNotificationThread()
    {
        // If the notification thread is not defined or is dead ...
        if (this.notificationGraceTimeThread == null || (!this.notificationGraceTimeThread.isAlive()
            && this.notificationGraceTimeThread.getState() != Thread.State.NEW)) {
            // ... initialize it
            this.notificationGraceTimeThread =
                new Thread(new ExecutionContextRunnable(new NotificationGraceTimeRunnable(), this.componentManager));
            this.notificationGraceTimeThread.setName("Live E-Mail notifications thread");
            this.notificationGraceTimeThread.setDaemon(true);
            this.notificationGraceTimeThread.setPriority(Thread.NORM_PRIORITY - 1);
        }

        if (!this.notificationGraceTimeThread.isAlive()) {
            this.notificationGraceTimeThread.start();
        }
    }
}
