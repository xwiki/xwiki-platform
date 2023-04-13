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
package org.xwiki.notifications.rest.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventManager;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.rss.NotificationRSSManager;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

/**
 * Component dedicated to output events as a RSS Feed ready to be sent as a response.
 *
 * @version $Id$
 * @since 14.7
 * @since 14.4.4
 * @since 13.10.9
 */
@Component(roles = RSSFeedRenderer.class)
@Singleton
public class RSSFeedRenderer
{
    @Inject
    private NotificationRSSManager notificationRSSManager;

    @Inject
    private GroupingEventManager groupingEventManager;

    /**
     * Convert an event list to an RSS feed object using {@link NotificationRSSManager} and then render it using
     * {@link SyndFeedOutput}.
     *
     * @param eventList the list of events to be displayed in RSS feed.
     * @param userId the user for which to retrieve the notification to know how to group them.
     * @return a String ready to be sent.
     * @throws NotificationException in case of problem to render the events.
     */
    public String render(List<Event> eventList, String userId) throws NotificationException
    {
        SyndFeedOutput output = new SyndFeedOutput();
        List<CompositeEvent> compositeEvents = this.groupingEventManager.getCompositeEvents(eventList, userId, "rss");
        try {
            return output.outputString(notificationRSSManager.renderFeed(compositeEvents));
        } catch (FeedException e) {
            throw new NotificationException("Error while rendering the RSS feed of events", e);
        }
    }
}
