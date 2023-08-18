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

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.internal.SimilarityCalculator;

/**
 * This manager contains a queue of events that just happened in a wiki and that are waiting to be sent to the users
 * that subscribed to those events.
 *
 * @since 9.6RC1
 * @version $Id$
 * @deprecated This component is only used in case of post-filtering events. We stopped supporting those.
 */
@Component(roles = LiveNotificationEmailManager.class)
@Singleton
@Deprecated(since = "15.5RC1")
public class LiveNotificationEmailManager implements Initializable
{
    @Inject
    private SimilarityCalculator similarityCalculator;

    @Inject
    private LiveNotificationEmailSender liveNotificationEmailSender;

    @Inject
    private NotificationConfiguration notificationConfiguration;

    /**
     * Number of minutes during which a composite event can stay in the event map before being sent.
     */
    private int graceTime = 10;

    /**
     * Represents an element of the queue.
     */
    private class QueueElement
    {
        private CompositeEvent event;

        private DateTime date;

        QueueElement(CompositeEvent event, DateTime date)
        {
            this.event = event;
            this.date = date;
        }
    }

    private Queue<QueueElement> queue = new ConcurrentLinkedDeque<>();

    /**
     * Add an event that has to be sent to the queue.
     * If a composite event exists in the queue matching the event, the two will merge. Else, a new composite event
     * is created.
     *
     * @param event the event that has to be sent in X minutes
     */
    public void addEvent(Event event)
    {
        Iterator<QueueElement> it = queue.iterator();
        while (it.hasNext()) {
            QueueElement element = it.next();
            // Compute the similarity between the event and the composite event in the map
            int similarity = similarityCalculator.computeSimilarity(event, element.event.getEvents().get(0));

            // If we can merge the event in the composite event
            if (similarity > SimilarityCalculator.NO_SIMILARITY
                && element.event.getSimilarityBetweenEvents() <= similarity) {
                try {
                    element.event.add(event, similarity);
                    return;
                } catch (NotificationException e) {
                    // If the addition process has failed, try with another CompositeEvent or, in last resort,
                    // create a new one.
                }
            }
        }

        // If no composite event has been found, create a new one
        this.queue.add(new QueueElement(new CompositeEvent(event), DateTime.now().plusMinutes(this.graceTime)));
    }

    /**
     * Go through the internal queue and sends the events that have to be sent now. Once an event is sent, it is
     * removed from the internal queue.
     */
    public void run()
    {
        Iterator<QueueElement> it = this.queue.iterator();
        while (it.hasNext()) {
            QueueElement element = it.next();

            if (element.date.isBeforeNow()) {
                // Send the mail
                this.liveNotificationEmailSender.sendEmails(element.event);
                it.remove();
            } else {
                // As soon as we hit an element which has its date older than now, we know that every other element
                // will have an older date, and therefore it’s useless to go through them.
                return;
            }
        }
    }

    /**
     * @return the next date for which an event contained in the queue will have to be sent. If the queue is empty,
     * returns null.
     */
    public DateTime getNextExecutionDate()
    {
        if (!this.queue.isEmpty()) {
            return this.queue.peek().date;
        } else {
            return null;
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Load the grace time present in the wiki configuration
        this.graceTime = this.notificationConfiguration.liveNotificationsGraceTime();
    }
}
