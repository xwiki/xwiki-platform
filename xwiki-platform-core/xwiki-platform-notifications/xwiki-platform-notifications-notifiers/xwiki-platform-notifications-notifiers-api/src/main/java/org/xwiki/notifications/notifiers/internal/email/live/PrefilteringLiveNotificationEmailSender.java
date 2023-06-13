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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Session;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.mail.CompositeMailListener;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Dispatch events to users with live email notifications enabled.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component(roles = PrefilteringLiveNotificationEmailSender.class)
@Singleton
public class PrefilteringLiveNotificationEmailSender
{
    /**
     * The local reference of the template to use to produce live mails.
     */
    public static final LocalDocumentReference TEMPLATE =
        new LocalDocumentReference(Arrays.asList("XWiki", "Notifications"), "MailTemplate");

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @Inject
    private MailSender mailSender;

    @Inject
    private Provider<PrefilteringMimeMessageIterator> liveMimeMessageIteratorProvider;

    /**
     * @param eventsToSend the event from which to produce mail for the passed users
     */
    public void sendMails(Map<DocumentReference, List<Event>> eventsToSend)
    {
        PrefilteringMimeMessageIterator liveNotificationMessageIterator = this.liveMimeMessageIteratorProvider.get();
        liveNotificationMessageIterator.initialize(eventsToSend, new HashMap<>(), TEMPLATE);

        Session session = this.sessionFactory.create(Collections.emptyMap());
        MailListener mailListener = mailListenerProvider.get();

        // Pass it to the message sender to send it asynchronously.
        this.mailSender.sendAsynchronously(liveNotificationMessageIterator, session,
            new CompositeMailListener(mailListener, liveNotificationMessageIterator.getMailListener()));
    }
}
