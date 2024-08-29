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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.Session;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.internal.email.NotificationUserIterator;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Sends live email notifications regarding a given composite event against every user that have set their preferences
 * to receive live email notifications for this kind of events.
 *
 * @since 9.6RC1
 * @version $Id$
 * @deprecated This component is only used in case of post-filtering events. We stopped supporting those.
 */
@Component(roles = LiveNotificationEmailSender.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Deprecated(since = "15.5RC1")
public class LiveNotificationEmailSender
{
    @Inject
    private MailSender mailSender;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @Inject
    private Provider<NotificationUserIterator> notificationUserIteratorProvider;

    @Inject
    private Provider<LiveMimeMessageIterator> liveMimeMessageIteratorProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Send live notification e-mails regarding the given event for the users that are concerned by this event and that
     * have enabled live notifications.
     * 
     * @param event the event that should be sent to the users
     */
    public void sendEmails(CompositeEvent event)
    {
        DocumentReference templateReference = new DocumentReference(this.wikiDescriptorManager.getCurrentWikiId(),
            Arrays.asList("XWiki", "Notifications"), "MailTemplate");

        // Get a list of users that have enabled the live e-mail notifications.
        NotificationUserIterator notificationUserIterator = this.notificationUserIteratorProvider.get();
        notificationUserIterator.initialize(NotificationEmailInterval.LIVE);

        LiveMimeMessageIterator liveNotificationMessageIterator = this.liveMimeMessageIteratorProvider.get();
        liveNotificationMessageIterator.initialize(notificationUserIterator, new HashMap<>(), event, templateReference);

        Session session = this.sessionFactory.create(Collections.emptyMap());
        MailListener mailListener = this.mailListenerProvider.get();

        // Pass it to the message sender to send it asynchronously.
        this.mailSender.sendAsynchronously(liveNotificationMessageIterator, session, mailListener);
    }
}
