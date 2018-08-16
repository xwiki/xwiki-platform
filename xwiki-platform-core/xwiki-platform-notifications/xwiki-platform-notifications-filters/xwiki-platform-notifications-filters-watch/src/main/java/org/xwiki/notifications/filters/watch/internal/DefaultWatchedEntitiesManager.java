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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.notifications.preferences.internal.XWikiEventTypesEnabler;
import org.xwiki.text.StringUtils;

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

    @Inject
    private XWikiEventTypesEnabler xwikiEventTypesEnabler;

    @Override
    public void watchEntity(WatchedEntityReference entity, DocumentReference user) throws NotificationException
    {
        handleEntity(entity, user, true);
    }

    @Override
    public void unwatchEntity(WatchedEntityReference entity, DocumentReference user)
            throws NotificationException
    {
        handleEntity(entity, user, false);
    }

    @Override
    public Collection<String> getWatchedUsers(DocumentReference user) throws NotificationException
    {
        Collection<NotificationFilterPreference> filterPreferences
            = notificationFilterManager.getFilterPreferences(user);

        Set<String> results = new HashSet<>();
        Iterator<NotificationFilterPreference> iterator =
            filterPreferences.stream().filter(
                pref -> pref.isEnabled() && pref.getFilterName().equals(EventUserFilter.FILTER_NAME)
                        && pref.getFilterType() == NotificationFilterType.INCLUSIVE
            ).iterator();
        while (iterator.hasNext()) {
            NotificationFilterPreference preference = iterator.next();
            results.add(preference.getUser());
        }
        return results;
    }

    private void handleEntity(WatchedEntityReference entity, DocumentReference user, boolean shouldBeWatched)
            throws NotificationException
    {
        if (entityIsAlreadyInDesiredState(entity, user, shouldBeWatched)) {
            return;
        }

        // If the notifications for the XWiki app (create, update, delete, addComment) are not enabled but autowatch is
        // on, then we need to enable the notifications in the user preferences.
        // We do that because it has no sense for a user to use the "AutoWatch" feature when the notifications are not
        // enabled.
        // Moreover, it makes the notifications feature discoverable. It means that, by default, all pages where the
        // user has made a contribution will generate notifications. That's probably what users expect from a
        // notification area.
        xwikiEventTypesEnabler.ensureXWikiNotificationsAreEnabled(user);

        Iterator<NotificationFilterPreference> filterPreferences = getAllEventsFilterPreferences(user).iterator();

        boolean thereIsAMatch = false;

        // Look if an existing filter match the entity
        while (filterPreferences.hasNext()) {
            NotificationFilterPreference notificationFilterPreference = filterPreferences.next();
            if (entity.matchExactly(notificationFilterPreference)) {
                thereIsAMatch = true;

                if (notificationFilterPreference.getFilterType() == NotificationFilterType.INCLUSIVE
                        && notificationFilterPreference.isEnabled() != shouldBeWatched) {
                    enableOrDeleteFilter(shouldBeWatched, notificationFilterPreference);
                } else if (notificationFilterPreference.getFilterType() == NotificationFilterType.EXCLUSIVE
                        && notificationFilterPreference.isEnabled() == shouldBeWatched) {
                    enableOrDeleteFilter(!shouldBeWatched, notificationFilterPreference);
                }
            }
        }

        // But it might been still unwatched because of an other filter!
        if (!thereIsAMatch || entity.isWatched(user) != shouldBeWatched) {
            notificationFilterManager.saveFilterPreferences(
                    Sets.newHashSet(createFilterPreference(entity, shouldBeWatched)));
        }
    }

    private boolean entityIsAlreadyInDesiredState(WatchedEntityReference entity, DocumentReference user,
            boolean desiredState) throws NotificationException
    {
        // If the notifications are enabled and the entity is already in the desired state, then we have nothing to do
        return !xwikiEventTypesEnabler.isNotificationDisabled(user) && entity.isWatched(user) == desiredState;
    }

    private void enableOrDeleteFilter(boolean enable,
            NotificationFilterPreference notificationFilterPreference) throws NotificationException
    {
        if (enable) {
            notificationFilterManager.setFilterPreferenceEnabled(
                    notificationFilterPreference.getId(),
                    true);
        } else  {
            // Delete this filter instead of just disabling it, because we don't want to let remaining
            // filters
            notificationFilterManager.deleteFilterPreference(
                    notificationFilterPreference.getId());
        }
    }

    private Stream<NotificationFilterPreference> getAllEventsFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        // A filter preferences object concerning all event is a filter that has no even set and that concern
        // concerns all notification formats.
        return notificationFilterManager.getFilterPreferences(user).stream().filter(
            filterPreference -> StringUtils.isBlank(filterPreference.getEventType())
            && filterPreference.getNotificationFormats().size() == NotificationFormat.values().length
        );
    }

    private NotificationFilterPreference createFilterPreference(WatchedEntityReference entity, boolean shouldBeWatched)
    {
        return shouldBeWatched ? entity.createInclusiveFilterPreference()
                : entity.createExclusiveFilterPreference();
    }
}
