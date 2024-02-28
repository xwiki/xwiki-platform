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
package org.xwiki.notifications.filters.internal;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationFilterPreferenceManager}
 *
 * @version $Id$
 * @since 10.9
 */
@ComponentTest
public class DefaultNotificationFilterPreferenceManagerTest
{
    @InjectMockComponents
    private DefaultNotificationFilterPreferenceManager filterPreferenceManager;

    private DocumentReference testUser;

    @MockComponent
    private NotificationFilterPreferenceProvider testProvider;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeEach
    public void setUp() throws Exception
    {
        testUser = new DocumentReference("wiki", "test", "user");
    }

    @Test
    void filterPreferences() throws Exception
    {
        NotificationFilterPreference filterPreference1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPreference2 = mock(NotificationFilterPreference.class);

        when(testProvider.getFilterPreferences(testUser)).thenReturn(Sets.newSet(filterPreference1, filterPreference2));

        Collection<NotificationFilterPreference> resultSet = filterPreferenceManager.getFilterPreferences(testUser);

        assertTrue(resultSet.contains(filterPreference1));
        assertTrue(resultSet.contains(filterPreference2));
        assertEquals(2, resultSet.size());
    }

    @Test
    void filterPreferencesWithFilter() throws Exception
    {
        NotificationFilterPreference filterPreference1 = mock(NotificationFilterPreference.class);
        when(filterPreference1.getFilterName()).thenReturn("someFilter");
        NotificationFilterPreference filterPreference2 = mock(NotificationFilterPreference.class);
        when(filterPreference2.getFilterName()).thenReturn("fakeFilter");

        Collection<NotificationFilterPreference> filterPreferences =
                Sets.newSet(filterPreference1, filterPreference2);

        NotificationFilter fakeFilter = mock(NotificationFilter.class);
        when(fakeFilter.getName()).thenReturn("fakeFilter");

        Collection<NotificationFilterPreference> resultSet = filterPreferenceManager
                .getFilterPreferences(filterPreferences, fakeFilter).collect(Collectors.toList());

        assertTrue(resultSet.contains(filterPreference2));
        assertEquals(1, resultSet.size());
    }

    @Test
    void filterPreferencesWithFilterAndFilterType() throws Exception
    {
        NotificationFilterPreference filterPreference1 = mock(NotificationFilterPreference.class);
        when(filterPreference1.getFilterName()).thenReturn("someFilter");
        when(filterPreference1.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        NotificationFilterPreference filterPreference2 = mock(NotificationFilterPreference.class);
        when(filterPreference2.getFilterName()).thenReturn("fakeFilter");
        when(filterPreference2.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        NotificationFilterPreference filterPreference3 = mock(NotificationFilterPreference.class);
        when(filterPreference3.getFilterName()).thenReturn("someFilter");
        when(filterPreference3.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);
        NotificationFilterPreference filterPreference4 = mock(NotificationFilterPreference.class);
        when(filterPreference4.getFilterName()).thenReturn("fakeFilter");
        when(filterPreference4.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);

        Collection<NotificationFilterPreference> filterPreferences
                = Sets.newSet(filterPreference1, filterPreference2, filterPreference3, filterPreference4);

        NotificationFilter fakeFilter = mock(NotificationFilter.class);
        when(fakeFilter.getName()).thenReturn("fakeFilter");

        Collection<NotificationFilterPreference> resultSet = filterPreferenceManager
                .getFilterPreferences(filterPreferences, fakeFilter, NotificationFilterType.INCLUSIVE).collect(
                        Collectors.toList());

        assertTrue(resultSet.contains(filterPreference4));
        assertEquals(1, resultSet.size());
    }

    @Test
    void deleteFilterPreference() throws Exception
    {
        filterPreferenceManager.deleteFilterPreference(testUser, "myFilter");

        verify(testProvider).deleteFilterPreferences(testUser, Set.of("myFilter"));
    }

    @Test
    void deleteFilterPreferenceProviderException() throws Exception
    {
        String providerName1 = "providerName1";
        String providerName2 = "providerName2";

        NotificationFilterPreferenceProvider provider1 =
            componentManager.registerMockComponent(NotificationFilterPreferenceProvider.class, providerName1);
        NotificationFilterPreferenceProvider provider2 =
            componentManager.registerMockComponent(NotificationFilterPreferenceProvider.class, providerName2);

        String filterId = "filterId";
        doThrow(new NotificationException("error provider1")).when(provider1).deleteFilterPreferences(testUser,
            Set.of(filterId));
        filterPreferenceManager.deleteFilterPreference(testUser, filterId);
        verify(testProvider).deleteFilterPreferences(testUser, Set.of(filterId));
        verify(provider1).deleteFilterPreferences(testUser, Set.of(filterId));
        verify(provider2).deleteFilterPreferences(testUser, Set.of(filterId));

        doThrow(new NotificationException("error provider2")).when(provider2).deleteFilterPreferences(testUser,
            Set.of(filterId));
        filterPreferenceManager.deleteFilterPreference(testUser, filterId);
        verify(testProvider, times(2)).deleteFilterPreferences(testUser, Set.of(filterId));
        verify(provider1, times(2)).deleteFilterPreferences(testUser, Set.of(filterId));
        verify(provider2, times(2)).deleteFilterPreferences(testUser, Set.of(filterId));

        doThrow(new NotificationException("error testprovider")).when(testProvider).deleteFilterPreferences(testUser,
            Set.of(filterId));
        NotificationException notificationException = assertThrows(NotificationException.class,
                () -> filterPreferenceManager.deleteFilterPreference(testUser, filterId));
        assertEquals("Error when trying to remove filter preferences [filterId] for user [wiki:test.user] - "
            + "All providers called failed, see exceptions: [NotificationException: error testprovider,"
            + "NotificationException: error provider1,NotificationException: error provider2].",
            notificationException.getMessage());
    }

    @Test
    void setFilterPreferenceEnabled() throws Exception
    {
        filterPreferenceManager.setFilterPreferenceEnabled(testUser,"myFilter1", true);
        filterPreferenceManager.setFilterPreferenceEnabled(testUser,"myFilter2", false);
        filterPreferenceManager.setFilterPreferenceEnabled(new WikiReference("foo"), "myFilter3", true);
        filterPreferenceManager.setFilterPreferenceEnabled(new WikiReference("foo"), "myFilter4", false);

        verify(testProvider).setFilterPreferenceEnabled(testUser, "myFilter1", true);
        verify(testProvider).setFilterPreferenceEnabled(testUser, "myFilter2", false);
        verify(testProvider).setFilterPreferenceEnabled(new WikiReference("foo"), "myFilter3", true);
        verify(testProvider).setFilterPreferenceEnabled(new WikiReference("foo"), "myFilter4", false);
    }

    @Test
    void setStartDateForUser() throws Exception
    {
        NotificationFilterPreferenceProvider provider1 =
            componentManager.registerMockComponent(NotificationFilterPreferenceProvider.class, "provider1");
        NotificationFilterPreferenceProvider provider2 =
            componentManager.registerMockComponent(NotificationFilterPreferenceProvider.class, "provider2");

        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        Date date = new Date();

        // Test
        filterPreferenceManager.setStartDateForUser(user, date);

        // Checks
        verify(provider1).setStartDateForUser(eq(user), eq(date));
        verify(provider2).setStartDateForUser(eq(user), eq(date));
    }

    @Test
    void saveFilterPreferences() throws Exception
    {
        DocumentReference user = new DocumentReference("subwiki", "XWiki", "Foo");
        NotificationFilterPreference pref1 = mock(NotificationFilterPreference.class, "pref1");
        NotificationFilterPreference pref2 = mock(NotificationFilterPreference.class, "pref2");
        NotificationFilterPreference pref3 = mock(NotificationFilterPreference.class, "pref3");

        String providerName1 = "providerName1";
        String providerName2 = "providerName2";

        NotificationFilterPreferenceProvider provider1 =
            componentManager.registerMockComponent(NotificationFilterPreferenceProvider.class, providerName1);
        NotificationFilterPreferenceProvider provider2 =
            componentManager.registerMockComponent(NotificationFilterPreferenceProvider.class, providerName2);

        when(pref1.getProviderHint()).thenReturn(providerName1);
        when(pref2.getProviderHint()).thenReturn(providerName2);
        when(pref3.getProviderHint()).thenReturn(providerName1);

        this.filterPreferenceManager.saveFilterPreferences(user, Set.of(pref1, pref2, pref3));
        verify(provider1).saveFilterPreferences(user, Set.of(pref1, pref3));
        verify(provider2).saveFilterPreferences(user, Set.of(pref2));
    }
}
