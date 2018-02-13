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
package org.xwiki.notifications.sources.script;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.sources.NotificationManager;
import org.xwiki.script.service.ScriptService;

/**
 * Script service for the notification sources.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Named("notification.sources")
@Singleton
public class NotificationSourcesScriptService implements ScriptService
{
    @Inject
    private NotificationManager notificationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * @param expectedCount number of expected events
     * @return the matching events for the current user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     * @since 10.1RC1
     */
    public List<CompositeEvent> getEvents(int expectedCount) throws NotificationException
    {
        return notificationManager.getEvents(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                expectedCount
        );
    }

    /**
     * @param expectedCount number of expected events
     * @param untilDate do not return events happened after this date
     * @param blackList array of events id to exclude from the search
     * @return the matching events for the current user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     * @since 10.1RC1
     */
    public List<CompositeEvent> getEvents(int expectedCount, Date untilDate, String[] blackList)
            throws NotificationException
    {
        return notificationManager.getEvents(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                expectedCount,
                untilDate,
                Arrays.asList(blackList)
        );
    }

    /**
     * @param expectedCount number of expected events
     * @param untilDate do not return events happened after this date
     * @param blackList list of events id to exclude from the search
     * @return the matching events for the current user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     */
    public List<CompositeEvent> getEvents(int expectedCount, Date untilDate, List<String> blackList)
            throws NotificationException
    {
        return notificationManager.getEvents(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                expectedCount,
                untilDate,
                blackList
        );
    }

    /**
     * Return the number of events to display as notifications concerning the current user.
     *
     * @param maxCount maximum number of events to count
     * @return the list of events to display as notifications
     * @throws NotificationException if an error happens
     */
    public long getEventsCount(int maxCount) throws NotificationException
    {
        return notificationManager.getEventsCount(
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()),
                maxCount
        );
    }
}
