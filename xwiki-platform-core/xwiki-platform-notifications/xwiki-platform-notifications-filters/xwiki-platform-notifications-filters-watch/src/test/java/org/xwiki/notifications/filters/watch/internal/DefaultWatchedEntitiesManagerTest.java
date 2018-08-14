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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 9.9RC1
 */
public class DefaultWatchedEntitiesManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultWatchedEntitiesManager> mocker =
            new MockitoComponentMockingRule<>(DefaultWatchedEntitiesManager.class);

    private NotificationFilterManager notificationFilterManager;

    @Before
    public void setUp() throws Exception
    {
        notificationFilterManager = mocker.getInstance(NotificationFilterManager.class);
    }

    @Test
    public void testWithSeveralFilterPreferences() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = new NotificationFilterPreference();
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference pref2 = new NotificationFilterPreference();
        pref2.setEventType("update");
        pref2.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref2)).thenReturn(true);

        NotificationFilterPreference pref3 = new NotificationFilterPreference();
        pref3.setFilterFormats(Sets.newSet(NotificationFormat.ALERT));
        when(watchedEntityReference.matchExactly(pref3)).thenReturn(true);

        NotificationFilterPreference pref4 = new NotificationFilterPreference();
        pref4.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref4)).thenReturn(true);
        pref4.setFilterType(NotificationFilterType.INCLUSIVE);
        pref4.setEnabled(false);
        pref4.setId(2000L);

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1, pref2, pref3, pref4));

        when(watchedEntityReference.isWatched(user)).thenReturn(false, true);

        // Test
        mocker.getComponentUnderTest().watchEntity(watchedEntityReference, user);

        // Checks
        verify(watchedEntityReference, never()).matchExactly(pref2);
        verify(watchedEntityReference, never()).matchExactly(pref3);
        verify(notificationFilterManager).setFilterPreferenceEnabled(2000L, true);
        verify(watchedEntityReference, never()).createInclusiveFilterPreference();
    }

    @Test
    public void watchWhenExclusiveFilter() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = new NotificationFilterPreference();
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId(1L);

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.isWatched(user)).thenReturn(false, true);

        // Test
        mocker.getComponentUnderTest().watchEntity(watchedEntityReference, user);

        // Checks
        verify(notificationFilterManager).deleteFilterPreference(1L);
        verify(watchedEntityReference, never()).createInclusiveFilterPreference();
    }

    @Test
    public void watchWhen2OppositeFilters() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = new NotificationFilterPreference();
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.INCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId(3L);
        NotificationFilterPreference pref2 = new NotificationFilterPreference();
        pref2.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref2)).thenReturn(true);
        pref2.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref2.setEnabled(true);
        pref2.setId(4L);

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1, pref2));

        when(watchedEntityReference.isWatched(user)).thenReturn(false, true);

        // Test
        mocker.getComponentUnderTest().watchEntity(watchedEntityReference, user);

        // Checks
        verify(notificationFilterManager).deleteFilterPreference(4L);
        verify(watchedEntityReference, never()).createInclusiveFilterPreference();
    }

    @Test
    public void watchWhenNoFilterMatch() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = new NotificationFilterPreference();
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.isWatched(user)).thenReturn(false);

        NotificationFilterPreference createdPref = mock(NotificationFilterPreference.class);
        when(watchedEntityReference.createInclusiveFilterPreference()).thenReturn(createdPref);

        // Test
        mocker.getComponentUnderTest().watchEntity(watchedEntityReference, user);

        // Checks
        verify(notificationFilterManager).saveFilterPreferences(eq(Sets.newSet(createdPref)));
    }

    @Test
    public void unwatchWhenInclusiveFilter() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = new NotificationFilterPreference();
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.INCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId(1L);

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.isWatched(user)).thenReturn(true, false);

        // Test
        mocker.getComponentUnderTest().unwatchEntity(watchedEntityReference, user);

        // Checks
        verify(notificationFilterManager).deleteFilterPreference(1L);
        verify(watchedEntityReference, never()).createExclusiveFilterPreference();
    }

    @Test
    public void unwatchWhenExclusiveFilter() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = new NotificationFilterPreference();
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref1.setEnabled(false);
        pref1.setId(1L);

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.isWatched(user)).thenReturn(true, false);

        // Test
        mocker.getComponentUnderTest().unwatchEntity(watchedEntityReference, user);

        // Checks
        verify(notificationFilterManager).setFilterPreferenceEnabled(1L, true);
        verify(watchedEntityReference, never()).createExclusiveFilterPreference();
    }

    @Test
    public void unwatchWhen2OppositeFilters() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = mock(NotificationFilterPreference.class);
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref1)).thenReturn(true);
        pref1.setFilterType(NotificationFilterType.INCLUSIVE);
        pref1.setEnabled(true);
        pref1.setId(1L);
        NotificationFilterPreference pref2 = mock(NotificationFilterPreference.class);

        pref2.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(watchedEntityReference.matchExactly(pref2)).thenReturn(true);
        pref2.setFilterType(NotificationFilterType.EXCLUSIVE);
        pref2.setEnabled(true);
        pref2.setId(2L);

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1, pref2));

        when(watchedEntityReference.isWatched(user)).thenReturn(true, false);

        // Test
        mocker.getComponentUnderTest().unwatchEntity(watchedEntityReference, user);

        // Checks
        verify(notificationFilterManager).deleteFilterPreference(1L);
        verify(watchedEntityReference, never()).createExclusiveFilterPreference();
    }

    @Test
    public void unwatchWhenNoFilterMatch() throws Exception
    {
        // Mocks
        WatchedEntityReference watchedEntityReference = mock(WatchedEntityReference.class);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Filters
        NotificationFilterPreference pref1 = new NotificationFilterPreference();
        pref1.setFilterFormats(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        when(notificationFilterManager.getFilterPreferences(user)).thenReturn(Sets.newSet(pref1));

        when(watchedEntityReference.isWatched(user)).thenReturn(true);

        NotificationFilterPreference createdPref = mock(NotificationFilterPreference.class);
        when(watchedEntityReference.createExclusiveFilterPreference()).thenReturn(createdPref);

        // Test
        mocker.getComponentUnderTest().unwatchEntity(watchedEntityReference, user);

        // Checks
        verify(notificationFilterManager).saveFilterPreferences(eq(Sets.newSet(createdPref)));
    }


}
