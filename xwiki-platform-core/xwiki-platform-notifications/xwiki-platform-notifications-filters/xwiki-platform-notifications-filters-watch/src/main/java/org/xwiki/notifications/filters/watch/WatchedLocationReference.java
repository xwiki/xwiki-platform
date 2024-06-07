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

import java.util.Date;
import java.util.Set;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterLocationStateComputer;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;

/**
 * Reference of a location to watch.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class WatchedLocationReference implements WatchedEntityReference
{
    private static final Set<NotificationFormat> ALL_NOTIFICATION_FORMATS = Set.of(NotificationFormat.values());

    private EntityReference entityReference;

    private String serializedReference;

    private EntityReferenceResolver<String> resolver;

    private ScopeNotificationFilterLocationStateComputer stateComputer;

    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    /**
     * Construct a WatchedLocationReference.
     * @param entityReference the reference of the location to watch
     * @param serializedReference the serialized reference of the location to watch
     * @param resolver the default entity reference resolver
     * @param stateComputer the default ScopeNotificationFilterLocationStateComputer
     * @param notificationFilterPreferenceManager the notification filter preference manager
     * @since 10.9
     */
    public WatchedLocationReference(EntityReference entityReference, String serializedReference,
            EntityReferenceResolver<String> resolver,
            ScopeNotificationFilterLocationStateComputer stateComputer,
            NotificationFilterPreferenceManager notificationFilterPreferenceManager)
    {
        this.entityReference = entityReference;
        this.serializedReference = serializedReference;
        this.resolver = resolver;
        this.stateComputer = stateComputer;
        this.notificationFilterPreferenceManager = notificationFilterPreferenceManager;
    }

    @Override
    public boolean isWatched(DocumentReference userReference) throws NotificationException
    {
        return stateComputer.isLocationWatched(notificationFilterPreferenceManager.getFilterPreferences(userReference),
                this.entityReference);
    }

    @Override
    public boolean isWatchedWithAllEventTypes(DocumentReference userReference) throws NotificationException
    {
        return stateComputer.isLocationWatchedWithAllEventTypes(
            notificationFilterPreferenceManager.getFilterPreferences(userReference),
            this.entityReference);
    }

    @Override
    public boolean matchExactly(NotificationFilterPreference notificationFilterPreference)
    {
        if (ScopeNotificationFilter.FILTER_NAME.equals(notificationFilterPreference.getFilterName())
            && notificationFilterPreference.getEventTypes().isEmpty()
            && notificationFilterPreference.getNotificationFormats().containsAll(
                Set.of(NotificationFormat.values()))) {
            ScopeNotificationFilterPreference scope
                    = new ScopeNotificationFilterPreference(notificationFilterPreference, resolver);
            return entityReference.equals(scope.getScopeReference());
        }

        return false;
    }

    @Override
    public boolean match(NotificationFilterPreference notificationFilterPreference)
    {
        if (ScopeNotificationFilter.FILTER_NAME.equals(notificationFilterPreference.getFilterName())) {
            ScopeNotificationFilterPreference scope
                    = new ScopeNotificationFilterPreference(notificationFilterPreference, resolver);
            return entityReference.equals(scope.getScopeReference());
        }

        return false;
    }

    @Override
    public NotificationFilterPreference createInclusiveFilterPreference()
    {
        return new ScopeNotificationFilterPreference(createFilterPreference(), resolver);
    }

    @Override
    public NotificationFilterPreference createExclusiveFilterPreference()
    {
        DefaultNotificationFilterPreference preference = createFilterPreference();
        preference.setFilterType(NotificationFilterType.EXCLUSIVE);
        return new ScopeNotificationFilterPreference(preference, resolver);
    }

    private DefaultNotificationFilterPreference createFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference = new DefaultNotificationFilterPreference();

        // Fields
        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.INCLUSIVE);
        filterPreference.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(ALL_NOTIFICATION_FORMATS);
        filterPreference.setStartingDate(new Date());

        // Properties

        // Scope value
        switch (entityReference.getType()) {
            case WIKI:
                filterPreference.setWiki(serializedReference);
                break;
            case SPACE:
                filterPreference.setPage(serializedReference);
                break;
            case DOCUMENT:
                filterPreference.setPageOnly(serializedReference);
                break;
            default:
                break;
        }

        return filterPreference;
    }

    @Override
    public String toString()
    {
        return serializedReference;
    }
}
