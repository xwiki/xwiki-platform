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

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterLocationStateComputer;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.scope.WatchedLocationState;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Reference of a location to watch.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class WatchedLocationReference implements WatchedEntityReference
{
    private static final Set<NotificationFormat> ALL_NOTIFICATION_FORMATS = Set.of(NotificationFormat.values());

    private final ComponentManager componentManager;

    private final EntityReference entityReference;

    private final String serializedReference;

    private final EntityReferenceResolver<String> resolver;

    private final ScopeNotificationFilterLocationStateComputer stateComputer;

    private final NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    private final UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    /**
     * Construct a WatchedLocationReference.
     * @param entityReference the reference of the location to watch
     * @param componentManager the component manager for loading needed components
     */
    public WatchedLocationReference(EntityReference entityReference, ComponentManager componentManager)
        throws ComponentLookupException
    {
        this.entityReference = entityReference;
        this.componentManager = componentManager;
        EntityReferenceSerializer<String> serializer;
        this.resolver = componentManager.getInstance(
            new DefaultParameterizedType(null, EntityReferenceResolver.class, String.class));
        serializer = componentManager.getInstance(
            new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class));
        this.stateComputer = componentManager.getInstance(ScopeNotificationFilterLocationStateComputer.class);
        this.notificationFilterPreferenceManager =
            componentManager.getInstance(NotificationFilterPreferenceManager.class);
        this.userReferenceSerializer = componentManager.getInstance(
            new DefaultParameterizedType(null, UserReferenceSerializer.class, DocumentReference.class), "document");

        this.serializedReference = serializer.serialize(entityReference);
    }

    @Override
    public boolean isWatched(DocumentReference userReference) throws NotificationException
    {
        // FIXME: it doesn't feel correct...
        // Fix is with introducing a deprecated stateComputer.isWatched
        return isWatchedWithAllEventTypes(userReference);
    }

    @Override
    public boolean isWatchedWithAllEventTypes(DocumentReference userReference) throws NotificationException
    {
        return getWatchedStatus(userReference) == WatchedStatus.WATCHED_FOR_ALL_EVENTS_AND_FORMATS;
    }

    @Override
    public WatchedStatus getWatchedStatus(UserReference userReference) throws NotificationException
    {
        DocumentReference userDocReference = this.userReferenceSerializer.serialize(userReference);
        return getWatchedStatus(userDocReference);
    }

    /**
     * Retrieve the specific watched status of an entity for the given user.
     *
     * @param userReference the user for whom to check if the entity is watched or not
     * @return the specific watched status of the entity by the given user
     * @throws NotificationException in case of errors
     * @since 15.5RC1
     */
    public WatchedStatus getWatchedStatus(DocumentReference userReference) throws NotificationException
    {
        Collection<NotificationFilterPreference> filterPreferences =
            notificationFilterPreferenceManager.getFilterPreferences(userReference);
        WatchedLocationState locationWatched = stateComputer.isLocationWatchedWithAllTypesAndFormats(filterPreferences,
            this.entityReference);
        return switch (locationWatched.getState()) {
            case CUSTOM -> WatchedStatus.CUSTOM;
            case WATCHED -> WatchedStatus.WATCHED_FOR_ALL_EVENTS_AND_FORMATS;
            case WATCHED_BY_ANCESTOR -> WatchedStatus.WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS;
            case WATCHED_WITH_CHILDREN -> WatchedStatus.WATCHED_WITH_CHILDREN_FOR_ALL_EVENTS_AND_FORMATS;
            case BLOCKED -> WatchedStatus.BLOCKED_FOR_ALL_EVENTS_AND_FORMATS;
            case BLOCKED_BY_ANCESTOR -> WatchedStatus.BLOCKED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS;
            case BLOCKED_WITH_CHILDREN -> WatchedStatus.BLOCKED_WITH_CHILDREN_FOR_ALL_EVENTS_AND_FORMATS;
            default -> WatchedStatus.NOT_SET;
        };
    }

    @Override
    public Optional<Pair<EntityReference, WatchedStatus>> getFirstFilteredAncestor(UserReference userReference)
        throws NotificationException
    {
        Optional<Pair<EntityReference, WatchedStatus>> result = Optional.empty();

        WatchedStatus watchedStatus = getWatchedStatus(userReference);
        if (watchedStatus.isBlocked() || watchedStatus.isWatched()) {
            EntityReference currentReference = entityReference;
            WatchedStatus parentWatchedStatus = null;
            do {
                EntityReference parent = currentReference.getParent();
                if (parent != null) {
                    WatchedLocationReference parentWatchedLocationReference =
                        null;
                    try {
                        parentWatchedLocationReference = new WatchedLocationReference(parent, componentManager);
                    } catch (ComponentLookupException e) {
                        throw new NotificationException("Error when creating a new reference", e);
                    }
                    parentWatchedStatus = parentWatchedLocationReference.getWatchedStatus(userReference);
                }
                currentReference = parent;
            } while (currentReference != null && !keepAncestor(parentWatchedStatus));
            if (currentReference != null) {
                result = Optional.of(
                    Pair.of(currentReference, parentWatchedStatus));
            }
        }

        return result;
    }

    private boolean keepAncestor(WatchedStatus ancestorWatchedStatus)
    {
        return (ancestorWatchedStatus.isWatched() || ancestorWatchedStatus.isBlocked())
            && !(ancestorWatchedStatus == WatchedStatus.WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS
            || ancestorWatchedStatus == WatchedStatus.BLOCKED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS);
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
