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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.events.MailEntityAddedEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * This listener is responsible of sending mails to users who enabled live mails.
 *
 * @since 12.6
 * @version $Id$
 */
@Component
@Singleton
@Named(PrefilteringLiveNotificationEmailListener.NAME)
public class PrefilteringLiveNotificationEmailListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "Prefiltering Live Notification Email Listener";

    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    private RemoteObservationManagerContext remoteState;

    @Inject
    private PrefilteringLiveNotificationEmailManager manager;

    @Inject
    private WikiDescriptorManager wikis;

    /**
     * Constructs a new {@link PrefilteringLiveNotificationEmailListener}.
     */
    public PrefilteringLiveNotificationEmailListener()
    {
        super(NAME, new MailEntityAddedEvent(), new ApplicationReadyEvent(), new WikiReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Check if the notifications are enabled in the wiki and if the mail option for the
        // notifications is enabled.
        if (this.notificationConfiguration.isEnabled() && this.notificationConfiguration.areEmailsEnabled()) {
            if (event instanceof MailEntityAddedEvent) {
                if (!this.remoteState.isRemoteState()) {
                    // Add the event to the queue of mails to send (if the associated user enabled live mails)
                    this.manager.addEvent((EntityEvent) source);
                }
            } else if (event instanceof ApplicationReadyEvent) {
                // Load all the events for which live mail was processing was not finished and send them
                // Don't block the event thread
                this.manager.addEvents(this.wikis.getMainWikiId());
            } else if (event instanceof WikiReadyEvent) {
                // Load all the events for which live mail was processing was not finished and send them
                // Don't block the event thread
                this.manager.addEvents(((WikiReadyEvent) event).getWikiId());
            }
        }
    }
}
