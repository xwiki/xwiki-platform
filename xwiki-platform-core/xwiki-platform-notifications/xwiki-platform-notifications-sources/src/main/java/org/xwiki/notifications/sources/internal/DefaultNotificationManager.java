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
package org.xwiki.notifications.sources.internal;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.sources.NewNotificationManager;
import org.xwiki.notifications.sources.NotificationManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link NotificationManager}.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultNotificationManager implements NotificationManager
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private NewNotificationManager newNotificationManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public List<CompositeEvent> getEvents(String userId, int expectedCount)
            throws NotificationException
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentReferenceResolver.resolve(userId);
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = expectedCount;
        return getEvents(parameters);
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, int count, Date untilDate, List<String> blackList)
            throws NotificationException
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentReferenceResolver.resolve(userId);
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = count;
        parameters.endDate = untilDate;
        parameters.blackList = blackList;
        return getEvents(parameters);
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, int expectedCount, Date untilDate, Date fromDate,
            List<String> blackList) throws NotificationException
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentReferenceResolver.resolve(userId);
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = expectedCount;
        parameters.endDate = untilDate;
        parameters.fromDate = fromDate;
        parameters.blackList = blackList;
        return getEvents(parameters);
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, NotificationFormat format, int expectedCount, Date untilDate,
            Date fromDate, List<String> blackList) throws NotificationException
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = documentReferenceResolver.resolve(userId);
        parameters.format = format;
        parameters.expectedCount = expectedCount;
        parameters.endDate = untilDate;
        parameters.fromDate = fromDate;
        parameters.blackList = blackList;
        return getEvents(parameters);
    }

    @Override
    public long getEventsCount(String userId, int maxCount) throws NotificationException
    {
        return getEvents(userId, maxCount).size();
    }

    private List<CompositeEvent> getEvents(NotificationParameters parameters)
            throws NotificationException
    {
        parameters.preferences = notificationPreferenceManager.getPreferences(parameters.user, true,
                parameters.format);
        parameters.filters = notificationFilterManager.getAllFilters(
                parameters.user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId()));
        parameters.filterPreferences = notificationFilterManager.getFilterPreferences(parameters.user);
        parameters.filterActivations = notificationFilterManager.getToggeableFilterActivations(parameters.user);

        return newNotificationManager.getEvents(parameters);
    }

    @Override
    public List<NotificationPreference> getPreferences() throws NotificationException
    {
        return notificationPreferenceManager.getAllPreferences(
                documentAccessBridge.getCurrentUserReference());
    }

    @Override
    public List<NotificationPreference> getPreferences(String userId) throws NotificationException
    {
        return notificationPreferenceManager.getAllPreferences(documentReferenceResolver.resolve(userId));
    }

    @Override
    public void setStartDate(String userId, Date startDate) throws NotificationException
    {
        notificationPreferenceManager.setStartDateForUser(documentReferenceResolver.resolve(userId), startDate);
    }
}
