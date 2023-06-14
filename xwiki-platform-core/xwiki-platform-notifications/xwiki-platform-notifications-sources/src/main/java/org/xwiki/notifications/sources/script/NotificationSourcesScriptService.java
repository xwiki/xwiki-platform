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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.notifications.sources.internal.DefaultNotificationParametersFactory;
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
    private ParametrizedNotificationManager notificationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DefaultNotificationParametersFactory notificationParametersFactory;

    /**
     * @param expectedCount number of expected events
     * @return the matching events for the current user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     * @since 10.1RC1
     */
    public List<CompositeEvent> getEvents(int expectedCount) throws NotificationException
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentAccessBridge.getCurrentUserReference();
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = expectedCount;
        parameters.endDateIncluded = true;
        this.notificationParametersFactory.useUserPreferences(parameters);
        return notificationManager.getEvents(parameters);
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
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentAccessBridge.getCurrentUserReference();
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = expectedCount;
        parameters.endDate = untilDate;
        parameters.endDateIncluded = true;
        parameters.blackList = Arrays.asList(blackList);
        this.notificationParametersFactory.useUserPreferences(parameters);
        return notificationManager.getEvents(parameters);
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
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentAccessBridge.getCurrentUserReference();
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = expectedCount;
        parameters.endDate = untilDate;
        parameters.endDateIncluded = true;
        parameters.blackList = blackList;
        this.notificationParametersFactory.useUserPreferences(parameters);
        return notificationManager.getEvents(parameters);
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
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentAccessBridge.getCurrentUserReference();
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = maxCount;
        parameters.onlyUnread = true;

        return notificationManager.getEvents(parameters).size();
    }

    /**
     * Create a {@link NotificationParameters} object to be used to retrieve the notifications of the given user.
     *
     * @param parameters a map of parameters to use, see the documentation from
     * {@link DefaultNotificationParametersFactory.ParametersKey} to see the available parameters and accepted values.
     * @return an instance of {@link NotificationParameters} that can be used to retrieve events.
     * @throws NotificationException in case of errors.
     * @since 12.2
     */
    public NotificationParameters getNotificationParameters(Map<String, String> parameters) throws NotificationException
    {
        return this.notificationParametersFactory.createNotificationParametersWithStringMap(parameters);
    }
}
