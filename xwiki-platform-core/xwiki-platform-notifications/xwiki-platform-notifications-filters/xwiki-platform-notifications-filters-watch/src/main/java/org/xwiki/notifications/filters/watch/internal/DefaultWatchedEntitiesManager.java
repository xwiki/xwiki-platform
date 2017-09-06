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
package org.xwiki.notifications.filters.watch.internal;

import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.compress.utils.Sets;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;

/**
 * Default implementation of {@link WatchedEntitiesManager}.
 *
 * @version $Id$
 * @since 9.8R1
 */
@Component
@Singleton
public class DefaultWatchedEntitiesManager implements WatchedEntitiesManager
{
    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Override
    public void watchEntity(DocumentReference user, WatchedEntityReference entity) throws NotificationException
    {
        Set<NotificationFilterPreference> filterPreferences = getAllEventsFilterPreferences(user);

        // If a filter preference concerning this entity already exists, use it
        for (NotificationFilterPreference notificationFilterPreference : filterPreferences) {
            if (entity.matchExactly(notificationFilterPreference)) {
                // If the filter preference is not enabled, then enable it
                if (!notificationFilterPreference.isEnabled()) {
                    notificationFilterManager.setFilterPreferenceEnabled(notificationFilterPreference.getFilterName(),
                            true);
                }
                // In any case, we have nothing else to do
                return;
            }
        }

        // If we reach this line, it means we need to create a new filter preference
        notificationFilterManager.saveFilterPreferences(Sets.newHashSet(entity.createFilterPreference()));
    }

    @Override
    public boolean isEntityWatched(DocumentReference user, WatchedEntityReference entity) throws NotificationException
    {
        Set<NotificationFilterPreference> prefs = getAllEventsFilterPreferences(user);
        return prefs.stream().anyMatch(pref -> entity.match(pref));
    }

    private Set<NotificationFilterPreference> getAllEventsFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        Set<NotificationFilterPreference> filterPreferences = notificationFilterManager.getFilterPreferences(user);
        Iterator<NotificationFilterPreference> it = filterPreferences.iterator();
        while (it.hasNext()) {
            NotificationFilterPreference filterPreference = it.next();
            if (!filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty()) {
                it.remove();
            }
            if (!filterPreference.getFilterFormats().equals(NotificationFormat.values().length)) {
                it.remove();
            }
        }
        return filterPreferences;
    }
}
