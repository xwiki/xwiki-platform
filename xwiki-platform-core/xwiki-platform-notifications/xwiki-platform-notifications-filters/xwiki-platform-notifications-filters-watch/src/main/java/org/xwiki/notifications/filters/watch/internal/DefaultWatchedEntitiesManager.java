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
import java.util.Optional;
import java.util.stream.Stream;

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
        Iterator<NotificationFilterPreference> filterPreferences = getAllEventsFilterPreferences(user).iterator();

        // If a filter preference concerning this entity already exists, use it
        while (filterPreferences.hasNext()) {
            NotificationFilterPreference notificationFilterPreference = filterPreferences.next();
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
    public void unwatchEntity(DocumentReference user, WatchedEntityReference entity)
            throws NotificationException
    {
        Stream<NotificationFilterPreference> prefs = getAllEventsFilterPreferences(user);
        Optional<NotificationFilterPreference> pref = prefs.filter(p -> entity.matchExactly(p)).findFirst();
        if (!pref.isPresent()) {
            throw new NotificationException("The entity is not watched.");
        }

        notificationFilterManager.deleteFilterPreference(pref.get().getFilterPreferenceName());
    }

    @Override
    public boolean isEntityWatched(DocumentReference user, WatchedEntityReference entity) throws NotificationException
    {
        return getAllEventsFilterPreferences(user).anyMatch(pref -> entity.match(pref));
    }

    @Override
    public boolean isEntityDirectlyWatched(DocumentReference user, WatchedEntityReference entity)
            throws NotificationException
    {
        return getAllEventsFilterPreferences(user).anyMatch(pref -> entity.matchExactly(pref));
    }

    private Stream<NotificationFilterPreference> getAllEventsFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        // A filter preferences object concerning all event is a filter that has no even set and that concern
        // concerns all notification formats.
        return notificationFilterManager.getFilterPreferences(user).stream().filter(
            filterPreference -> filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty()
            && filterPreference.getFilterFormats().size() == NotificationFormat.values().length
        );
    }
}
