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

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
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
import org.xwiki.notifications.filters.internal.scope.WatchedLocationState;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WatchedLocationReference}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
class WatchedLocationReferenceTest
{
    private EntityReference entityReference;

    private String serializedReference;

    private EntityReferenceResolver<String> resolver;

    private ScopeNotificationFilterLocationStateComputer stateComputer;

    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    private WatchedLocationReference watchedLocationReference;

    @BeforeEach
    void setup()
    {
        this.entityReference = mock(EntityReference.class);
        this.resolver = mock(EntityReferenceResolver.class);
        this.stateComputer = mock(ScopeNotificationFilterLocationStateComputer.class);
        this.notificationFilterPreferenceManager = mock(NotificationFilterPreferenceManager.class);
        this.serializedReference = "xwiki:XWiki.Location";
        this.watchedLocationReference = new WatchedLocationReference(this.entityReference,
            this.serializedReference,
            this.resolver,
            this.stateComputer,
            this.notificationFilterPreferenceManager);
    }

    @Test
    void isWatched() throws NotificationException
    {
        DocumentReference userReference = mock(DocumentReference.class);
        Collection<NotificationFilterPreference> filterPreferences = mock(Collection.class);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(userReference))
            .thenReturn(filterPreferences);

        when(this.stateComputer.isLocationWatchedWithAllTypesAndFormats(filterPreferences, this.entityReference))
            .thenReturn(new WatchedLocationState(WatchedLocationState.WatchedState.WATCHED, new Date()));
        assertTrue(this.watchedLocationReference.isWatched(userReference));

        verify(this.notificationFilterPreferenceManager).getFilterPreferences(userReference);
    }

    @Test
    void isWatchedWithAllEventTypes() throws NotificationException
    {
        DocumentReference userReference = mock(DocumentReference.class);
        Collection<NotificationFilterPreference> filterPreferences = mock(Collection.class);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(userReference))
            .thenReturn(filterPreferences);

        when(this.stateComputer.isLocationWatchedWithAllTypesAndFormats(filterPreferences, this.entityReference))
            .thenReturn(new WatchedLocationState(WatchedLocationState.WatchedState.WATCHED, new Date()));
        assertTrue(this.watchedLocationReference.isWatchedWithAllEventTypes(userReference));

        verify(this.notificationFilterPreferenceManager).getFilterPreferences(userReference);
    }

    @Test
    void matchExactly()
    {
        NotificationFilterPreference filterPreference = mock(NotificationFilterPreference.class);
        assertFalse(this.watchedLocationReference.matchExactly(filterPreference));

        when(filterPreference.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(filterPreference.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.values()));
        when(filterPreference.getPageOnly()).thenReturn(this.serializedReference);
        when(this.resolver.resolve(this.serializedReference, EntityType.DOCUMENT)).thenReturn(this.entityReference);
        assertTrue(this.watchedLocationReference.matchExactly(filterPreference));

        when(filterPreference.getFilterName()).thenReturn("foo");
        assertFalse(this.watchedLocationReference.matchExactly(filterPreference));

        when(filterPreference.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(filterPreference.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.ALERT));
        assertFalse(this.watchedLocationReference.matchExactly(filterPreference));

        when(filterPreference.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.values()));
        when(filterPreference.getEventTypes()).thenReturn(Set.of("update"));
        assertFalse(this.watchedLocationReference.matchExactly(filterPreference));

        when(filterPreference.getEventTypes()).thenReturn(Collections.emptySet());
        when(this.resolver.resolve(this.serializedReference, EntityType.DOCUMENT))
            .thenReturn(mock(EntityReference.class));
        assertFalse(this.watchedLocationReference.matchExactly(filterPreference));
    }

    @Test
    void match()
    {
        NotificationFilterPreference filterPreference = mock(NotificationFilterPreference.class);
        assertFalse(this.watchedLocationReference.match(filterPreference));

        when(filterPreference.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(filterPreference.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.values()));
        when(filterPreference.getPageOnly()).thenReturn(this.serializedReference);
        when(this.resolver.resolve(this.serializedReference, EntityType.DOCUMENT)).thenReturn(this.entityReference);
        assertTrue(this.watchedLocationReference.match(filterPreference));

        when(filterPreference.getFilterName()).thenReturn("foo");
        assertFalse(this.watchedLocationReference.match(filterPreference));

        when(filterPreference.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(filterPreference.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.ALERT));
        assertTrue(this.watchedLocationReference.match(filterPreference));

        when(filterPreference.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.values()));
        when(filterPreference.getEventTypes()).thenReturn(Set.of("update"));
        assertTrue(this.watchedLocationReference.match(filterPreference));

        when(filterPreference.getEventTypes()).thenReturn(Collections.emptySet());
        when(this.resolver.resolve(this.serializedReference, EntityType.DOCUMENT))
            .thenReturn(mock(EntityReference.class));
        assertFalse(this.watchedLocationReference.match(filterPreference));
    }

    @Test
    void createInclusiveFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference = new DefaultNotificationFilterPreference();

        // Fields
        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.INCLUSIVE);
        filterPreference.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(Set.of(NotificationFormat.values()));
        filterPreference.setPageOnly(this.serializedReference);

        Date now = new Date();
        // We remove 10 seconds to be sure not having any flickering behaviour
        now = Date.from(now.toInstant().minus(10, ChronoUnit.SECONDS));

        when(entityReference.getType()).thenReturn(EntityType.DOCUMENT);
        when(this.resolver.resolve(this.serializedReference, EntityType.DOCUMENT)).thenReturn(this.entityReference);

        NotificationFilterPreference inclusiveFilterPreference =
            this.watchedLocationReference.createInclusiveFilterPreference();

        Date afterCreation = new Date();
        // We add 10 seconds to be sure not having any flickering behaviour
        afterCreation = Date.from(afterCreation.toInstant().plus(10, ChronoUnit.SECONDS));

        assertNotNull(inclusiveFilterPreference.getStartingDate());
        assertTrue(inclusiveFilterPreference.getStartingDate().after(now));
        assertTrue(inclusiveFilterPreference.getStartingDate().before(afterCreation));

        filterPreference.setStartingDate(inclusiveFilterPreference.getStartingDate());

        assertEquals(new ScopeNotificationFilterPreference(filterPreference, this.resolver), inclusiveFilterPreference);
    }

    @Test
    void createExclusiveFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference = new DefaultNotificationFilterPreference();

        // Fields
        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.EXCLUSIVE);
        filterPreference.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(Set.of(NotificationFormat.values()));
        filterPreference.setPage(this.serializedReference);

        Date now = new Date();
        // We remove 10 seconds to be sure not having any flickering behaviour
        now = Date.from(now.toInstant().minus(10, ChronoUnit.SECONDS));

        when(entityReference.getType()).thenReturn(EntityType.SPACE);
        when(this.resolver.resolve(this.serializedReference, EntityType.SPACE)).thenReturn(this.entityReference);

        NotificationFilterPreference exclusiveFilterPreference =
            this.watchedLocationReference.createExclusiveFilterPreference();

        Date afterCreation = new Date();
        // We add 10 seconds to be sure not having any flickering behaviour
        afterCreation = Date.from(afterCreation.toInstant().plus(10, ChronoUnit.SECONDS));

        assertNotNull(exclusiveFilterPreference.getStartingDate());
        assertTrue(exclusiveFilterPreference.getStartingDate().after(now));
        assertTrue(exclusiveFilterPreference.getStartingDate().before(afterCreation));

        filterPreference.setStartingDate(exclusiveFilterPreference.getStartingDate());

        assertEquals(new ScopeNotificationFilterPreference(filterPreference, this.resolver), exclusiveFilterPreference);
    }

    @Test
    void getWatchedStatus() throws NotificationException
    {
        DocumentReference userReference = mock(DocumentReference.class);
        Collection<NotificationFilterPreference> filterPreferences = mock(Collection.class);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(userReference))
            .thenReturn(filterPreferences);

        when(this.stateComputer.isLocationWatchedWithAllTypesAndFormats(filterPreferences, this.entityReference))
            .thenReturn(new WatchedLocationState());
        assertEquals(WatchedEntityReference.WatchedStatus.NOT_SET,
            this.watchedLocationReference.getWatchedStatus(userReference));

        verify(this.stateComputer).isLocationWatchedWithAllTypesAndFormats(filterPreferences, this.entityReference);

        when(this.stateComputer.isLocationWatchedWithAllTypesAndFormats(filterPreferences, this.entityReference))
            .thenReturn(new WatchedLocationState(WatchedLocationState.WatchedState.WATCHED, new Date()));
        assertEquals(WatchedEntityReference.WatchedStatus.WATCHED_FOR_ALL_EVENTS_AND_FORMATS,
            this.watchedLocationReference.getWatchedStatus(userReference));
    }
}