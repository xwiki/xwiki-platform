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
package org.xwiki.notifications.filters.watch;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Sets;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.filters.internal.user.EventUserFilterPreferencesGetter;
import org.xwiki.notifications.preferences.internal.UserProfileNotificationPreferenceProvider;
import org.xwiki.stability.Unstable;

/**
 * Reference to a user to watch.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Unstable
public class WatchedUserReference implements WatchedEntityReference
{
    private String userId;

    private NotificationFilterManager notificationFilterManager;

    private EventUserFilterPreferencesGetter preferencesGetter;

    /**
     * Construct a WatchedUserReference.
     * @param userId id of the user to watch.
     * @param preferencesGetter the instance of EventUserFilterPreferencesGetter
     * @param notificationFilterManager the notification filter manager
     * @since 9.10RC1
     */
    public WatchedUserReference(String userId, EventUserFilterPreferencesGetter preferencesGetter,
            NotificationFilterManager notificationFilterManager)
    {
        this.userId = userId;
        this.preferencesGetter = preferencesGetter;
        this.notificationFilterManager = notificationFilterManager;
    }

    @Override
    public boolean isWatched(DocumentReference userReference) throws NotificationException
    {
        return preferencesGetter.isUsedFollowed(userId, notificationFilterManager.getFilterPreferences(userReference),
                null);
    }

    @Override
    public boolean matchExactly(NotificationFilterPreference notificationFilterPreference)
    {
        return EventUserFilter.FILTER_NAME.equals(notificationFilterPreference.getFilterName())
                && notificationFilterPreference.getProperties(NotificationFilterProperty.USER).contains(userId)
                && notificationFilterPreference.getFilterFormats().containsAll(
                        Sets.newHashSet(NotificationFormat.values()));
    }

    @Override
    public NotificationFilterPreference createInclusiveFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference
                = new DefaultNotificationFilterPreference(Long.toString(new Date().getTime()));

        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.INCLUSIVE);
        filterPreference.setFilterName(EventUserFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(Sets.newHashSet(NotificationFormat.values()));
        filterPreference.setProviderHint(UserProfileNotificationPreferenceProvider.NAME);
        filterPreference.setActive(true);
        filterPreference.setStartingDate(new Date());

        // Properties
        Map<NotificationFilterProperty, List<String>> preferenceProperties = new HashMap<>();
        filterPreference.setPreferenceProperties(preferenceProperties);

        preferenceProperties.put(NotificationFilterProperty.EVENT_TYPE, Collections.emptyList());
        preferenceProperties.put(NotificationFilterProperty.USER, Collections.singletonList(userId));

        return filterPreference;
    }

    @Override
    public NotificationFilterPreference createExclusiveFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference
                = new DefaultNotificationFilterPreference(Long.toString(new Date().getTime()));

        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.EXCLUSIVE);
        filterPreference.setFilterName(EventUserFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(Sets.newHashSet(NotificationFormat.values()));
        filterPreference.setProviderHint(UserProfileNotificationPreferenceProvider.NAME);
        filterPreference.setActive(false);
        filterPreference.setStartingDate(new Date());

        // Properties
        Map<NotificationFilterProperty, List<String>> preferenceProperties = new HashMap<>();
        filterPreference.setPreferenceProperties(preferenceProperties);

        preferenceProperties.put(NotificationFilterProperty.EVENT_TYPE, Collections.emptyList());
        preferenceProperties.put(NotificationFilterProperty.USER, Collections.singletonList(userId));

        return filterPreference;
    }
}
