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

import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @version $Id$
 * @since 9.9RC1
 */
@ComponentTest
class DefaultWatchedEntitiesManagerTest
{
    @InjectMockComponents
    private DefaultWatchedEntitiesManager watchedEntitiesManager;
    
    @MockComponent
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;
    
    @Test
    void testWithSeveralFilterPreferences() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        DefaultNotificationFilterPreference pref2 = new DefaultNotificationFilterPreference();
        pref2.setEventTypes(Sets.newSet("update"));
        pref2.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.match(pref2)).thenReturn(true);
        pref2.setFilterType(NotificationFilterType.INCLUSIVE);
        pref2.setEnabled(true);
        pref2.setId("pref2");

        DefaultNotificationFilterPreference pref3 = new DefaultNotificationFilterPreference();
        pref3.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT));
        when(watchedEntityReference.match(pref3)).thenReturn(true);
        pref3.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref3.setEnabled(true);
        pref3.setId("pref3");

        DefaultNotificationFilterPreference pref4 = new DefaultNotificationFilterPreference();
        pref4.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref4)).thenReturn(true);
        pref4.setFilterType(NotificationFilterType.INCLUSIVE);
        pref4.setEnabled(false);
        pref4.setId("pref4");

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef))
            .thenReturn(Sets.newSet(pref1, pref2, pref3, pref4));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(
                WatchedEntityReference.WatchedStatus.NOT_SET,
                WatchedEntityReference.WatchedStatus.WATCHED_FOR_ALL_EVENTS_AND_FORMATS);

        // Test
        watchedEntitiesManager.watchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager, never()).setFilterPreferenceEnabled(userDocRef, "pref2", false);
        verify(notificationFilterPreferenceManager).setFilterPreferenceEnabled(userDocRef, "pref3", false);
        verify(notificationFilterPreferenceManager).setFilterPreferenceEnabled(userDocRef, "pref4", true);
        verify(watchedEntityReference, never()).createInclusiveFilterPreference();
    }

    @Test
    void watchWhenExclusiveFilter() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId("pref1");

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(
                WatchedEntityReference.WatchedStatus.NOT_SET,
                WatchedEntityReference.WatchedStatus.WATCHED_FOR_ALL_EVENTS_AND_FORMATS);

        // Test
        watchedEntitiesManager.watchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager).deleteFilterPreference(userDocRef, "pref1");
        verify(watchedEntityReference, never()).createInclusiveFilterPreference();
    }

    @Test
    void watchWhen2OppositeFilters() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.INCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId("pref1");
        DefaultNotificationFilterPreference pref2 = new DefaultNotificationFilterPreference();
        pref2.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref2)).thenReturn(true);
        pref2.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref2.setEnabled(true);
        pref2.setId("pref2");

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef))
            .thenReturn(Sets.newSet(pref1, pref2));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(
                WatchedEntityReference.WatchedStatus.NOT_SET,
                WatchedEntityReference.WatchedStatus.WATCHED_FOR_ALL_EVENTS_AND_FORMATS);

        // Test
        watchedEntitiesManager.watchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager).deleteFilterPreference(userDocRef, "pref2");
        verify(watchedEntityReference, never()).createInclusiveFilterPreference();
    }

    @Test
    void watchWhenNoFilterMatch() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(WatchedEntityReference.WatchedStatus.NOT_SET);

        DefaultNotificationFilterPreference createdPref = mock(DefaultNotificationFilterPreference.class);
        when(watchedEntityReference.createInclusiveFilterPreference()).thenReturn(createdPref);

        when(watchedEntityReference.createExclusiveFilterPreference()).thenReturn(createdPref);
        when(createdPref.getId()).thenReturn("aabbccdd");

        doAnswer(invocationOnMock -> {
            Set set = (Set) invocationOnMock.getArgument(1);
            DefaultNotificationFilterPreference prefToSave = (DefaultNotificationFilterPreference) set.toArray()[0];
            assertEquals("aabbccdd", prefToSave.getId());
            return null;
        }).when(notificationFilterPreferenceManager).saveFilterPreferences(eq(userDocRef), anySet());

        // Test
        watchedEntitiesManager.watchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager).saveFilterPreferences(eq(userDocRef), anySet());
    }

    @Test
    void unwatchWhenInclusiveFilter() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.INCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId("pref1");

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(
                WatchedEntityReference.WatchedStatus.WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS,
                WatchedEntityReference.WatchedStatus.BLOCKED_FOR_ALL_EVENTS_AND_FORMATS);

        // Test
        watchedEntitiesManager.unwatchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager).deleteFilterPreference(userDocRef, "pref1");
        verify(watchedEntityReference, never()).createExclusiveFilterPreference();
    }

    @Test
    void unwatchWhenExclusiveFilter() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref1.setEnabled(false);
        pref1.setId("pref1");

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(
                WatchedEntityReference.WatchedStatus.WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS,
                WatchedEntityReference.WatchedStatus.BLOCKED_FOR_ALL_EVENTS_AND_FORMATS);

        // Test
        watchedEntitiesManager.unwatchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager).setFilterPreferenceEnabled(userDocRef, "pref1", true);
        verify(watchedEntityReference, never()).createExclusiveFilterPreference();
    }

    @Test
    void unwatchWhen2OppositeFilters() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.INCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId("pref1");
        DefaultNotificationFilterPreference pref2 = new DefaultNotificationFilterPreference();

        pref2.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref2)).thenReturn(true);
        pref2.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref2.setEnabled(true);
        pref2.setId("pref2");

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef)).thenReturn(Sets.newSet(pref1, pref2));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(
                WatchedEntityReference.WatchedStatus.WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS,
                WatchedEntityReference.WatchedStatus.BLOCKED_FOR_ALL_EVENTS_AND_FORMATS);

        // Test
        watchedEntitiesManager.unwatchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager).deleteFilterPreference(userDocRef, "pref1");
        verify(watchedEntityReference, never()).createExclusiveFilterPreference();
    }

    @Test
    void unwatchWhenNoFilterMatch() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference userDocRef = new DocumentReference("xwiki", "XWiki", "User");
        UserReference userRef = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userRef);
        when(this.userReferenceSerializer.serialize(userRef)).thenReturn(userDocRef);

        // Filters
        DefaultNotificationFilterPreference pref1 = new DefaultNotificationFilterPreference();
        pref1.setNotificationFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        when(notificationFilterPreferenceManager.getFilterPreferences(userDocRef)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.getWatchedStatus(userRef))
            .thenReturn(
                WatchedEntityReference.WatchedStatus.WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS);

        DefaultNotificationFilterPreference createdPref = mock(DefaultNotificationFilterPreference.class);
        when(watchedEntityReference.createExclusiveFilterPreference()).thenReturn(createdPref);
        when(createdPref.getId()).thenReturn("aabbccdd");

        doAnswer(invocationOnMock -> {
            Set set = (Set) invocationOnMock.getArgument(1);
            DefaultNotificationFilterPreference prefToSave = (DefaultNotificationFilterPreference) set.toArray()[0];
            assertEquals("aabbccdd", prefToSave.getId());
            return null;
        }).when(notificationFilterPreferenceManager).saveFilterPreferences(eq(userDocRef), anySet());

        // Test
        watchedEntitiesManager.unwatchEntity(watchedEntityReference, userDocRef);

        // Checks
        verify(notificationFilterPreferenceManager).saveFilterPreferences(eq(userDocRef), anySet());
    }
}
