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
package org.xwiki.notifications.script;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.CompositeEventStatusManager;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationManager;
import org.xwiki.notifications.NotificationRenderer;
import org.xwiki.notifications.internal.ModelBridge;
import org.xwiki.notifications.rss.NotificationRSSManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

/**
 * Script services for the notifications.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
@Named("notification")
@Unstable
public class NotificationScriptService implements ScriptService
{
    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    private NotificationManager notificationManager;

    @Inject
    private NotificationRenderer notificationRenderer;

    @Inject
    private NotificationRSSManager notificationRSSManager;

    @Inject
    private EventStatusManager eventStatusManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private CompositeEventStatusManager compositeEventStatusManager;

    @Inject
    private ModelBridge modelBridge;

    /**
     * @param onyUnread either or not to return only unread events
     * @param expectedCount number of expected events
     * @return the matching events for the current user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     */
    public List<CompositeEvent> getEvents(boolean onyUnread, int expectedCount) throws NotificationException
    {
        return notificationManager.getEvents(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                onyUnread,
                expectedCount
        );
    }

    /**
     * @param onyUnread either or not to return only unread events
     * @param expectedCount number of expected events
     * @param untilDate do not return events happened after this date
     * @param blackList array of events id to exclude from the search
     * @return the matching events for the current user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     */
    public List<CompositeEvent> getEvents(boolean onyUnread, int expectedCount, Date untilDate, String[] blackList)
            throws NotificationException
    {
        return notificationManager.getEvents(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                onyUnread,
                expectedCount,
                untilDate,
                Arrays.asList(blackList)
        );
    }

    /**
     * @param onyUnread either or not to return only unread events
     * @param expectedCount number of expected events
     * @param untilDate do not return events happened after this date
     * @param blackList list of events id to exclude from the search
     * @return the matching events for the current user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     */
    public List<CompositeEvent> getEvents(boolean onyUnread, int expectedCount, Date untilDate, List<String> blackList)
            throws NotificationException
    {
        return notificationManager.getEvents(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                onyUnread,
                expectedCount,
                untilDate,
                blackList
        );
    }

    /**
     * Return the number of events to display as notifications concerning the current user.
     *
     * @param onlyUnread either if only unread events should be counted or all events
     * @param maxCount maximum number of events to count
     * @return the list of events to display as notifications
     * @throws NotificationException if an error happens
     */
    public long getEventsCount(boolean onlyUnread, int maxCount) throws NotificationException
    {
        return notificationManager.getEventsCount(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                onlyUnread,
                maxCount
        );
    }

    /**
     * Generate a rendering Block for a given event to display as notification.
     * @param event the event to render
     * @return a rendering block ready to display the event
     * 
     * @throws NotificationException if an error happens
     */
    public Block render(CompositeEvent event) throws NotificationException
    {
        return notificationRenderer.render(event);
    }

    /**
     * Get the list of statuses concerning the given events and the current user.
     *
     * @param events a list of events
     * @return the list of statuses corresponding to each pair or event/entity
     *
     * @throws Exception if an error occurs
     */
    public List<EventStatus> getEventStatuses(List<Event> events) throws Exception
    {
        return eventStatusManager.getEventStatus(events,
                Arrays.asList(entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference())));
    }

    /**
     * Get the list of statuses concerning the given composite events and the current user.
     *
     * @param compositeEvents a list of composite events
     * @return the list of statuses corresponding to each pair or event/entity
     *
     * @throws Exception if an error occurs
     * @since 9.4RC1
     */
    public List<CompositeEventStatus> getCompositeEventStatuses(List<CompositeEvent> compositeEvents) throws Exception
    {
        return compositeEventStatusManager.getCompositeEventStatuses(compositeEvents,
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()));
    }

    /**
     * Get the status of the module.
     * 
     * @return true if the notification module is enabled in the platform configuration
     */
    public boolean isEnabled()
    {
        return notificationConfiguration.isEnabled();
    }

    /**
     * Get the status of the module.
     *
     * @return true if the notification module can send emails
     * @since 9.5RC1
     */
    public boolean areEmailsEnabled()
    {
        return notificationConfiguration.areEmailsEnabled();
    }

    /**
     * Save a status for the current user.
     * @param eventId id of the event
     * @param isRead either or not the current user has read the given event
     * @throws Exception if an error occurs
     */
    public void saveEventStatus(String eventId, boolean isRead) throws Exception
    {
        DefaultEvent event = new DefaultEvent();
        event.setId(eventId);
        String userId = entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference());
        eventStatusManager.saveEventStatus(new DefaultEventStatus(event, userId, isRead));
    }

    /**
     * Set the start date for the current user.
     *
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error occurs
     */
    public void setStartDate(Date startDate) throws NotificationException
    {
        modelBridge.setStartDateForUser(documentAccessBridge.getCurrentUserReference(), startDate);
    }

    /**
     * Set the start date for the given user.
     *
     * @param userId id of the user
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error occurs
     */
    public void setStartDate(String userId, Date startDate) throws NotificationException
    {
        notificationManager.setStartDate(userId, startDate);
    }

    /**
     * Get the RSS notifications feed of the given user.
     *
     * @param entryNumber number of entries to get
     * @param onlyUnread if only unread events should be returned
     * @return the notifications RSS feed
     * @throws NotificationException if an error occurs
     * @since 9.6RC1
     */
    public String getFeed(int entryNumber, boolean onlyUnread) throws NotificationException
    {
        String userId = entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference());
        return this.getFeed(userId, entryNumber, onlyUnread);
    }

    /**
     * Get the RSS notifications feed of the given user.
     *
     * @param userId id of the user
     * @param entryNumber number of entries to get
     * @param onlyUnread if only unread events should be returned
     * @return the notifications RSS feed
     * @throws NotificationException if an error occurs
     * @since 9.6RC1
     */
    public String getFeed(String userId, int entryNumber, boolean onlyUnread) throws NotificationException
    {
        SyndFeedOutput output = new SyndFeedOutput();
        try {
            return output.outputString(this.notificationRSSManager.renderFeed(
                    this.notificationManager.getEvents(userId, onlyUnread, entryNumber)));
        } catch (FeedException e) {
            throw new NotificationException("Unable to render RSS feed", e);
        }
    }
}
