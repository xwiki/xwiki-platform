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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationFilterManager}
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class DefaultNotificationFilterManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultNotificationFilterManager> mocker =
            new MockitoComponentMockingRule<>(DefaultNotificationFilterManager.class);

    private ComponentManager componentManager;

    private WikiDescriptorManager wikiDescriptorManager;

    private DocumentReference testUser;

    private NotificationFilterPreferenceProvider testProvider;

    private ModelBridge modelBridge;

    @Before
    public void setUp() throws Exception
    {
        componentManager = mocker.registerMockComponent(ComponentManager.class);

        wikiDescriptorManager = mocker.registerMockComponent(WikiDescriptorManager.class);

        modelBridge = mocker.registerMockComponent(ModelBridge.class, "cached");

        testUser = new DocumentReference("wiki", "test", "user");

        testProvider = mock(NotificationFilterPreferenceProvider.class);
        when(componentManager.getInstanceList(NotificationFilterPreferenceProvider.class))
                .thenReturn(Collections.singletonList(testProvider));


        // Set a default comportment for the wikiDescriptorManager
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("wiki");
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWikiId");
        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("wiki", "currentWikiId"));
    }

    @Test
    public void getAllNotificationsFiltersWithSubWiki() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);
        NotificationFilter fakeFilter2 = mock(NotificationFilter.class);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("somethingElseThanwiki");

        when(componentManager.getInstanceList(NotificationFilter.class))
                .thenReturn(Arrays.asList(fakeFilter1, fakeFilter2));

        Collection<NotificationFilter> filters = mocker.getComponentUnderTest().getAllFilters(testUser, false);

        assertEquals(2, filters.size());
        assertTrue(filters.contains(fakeFilter1));
        assertTrue(filters.contains(fakeFilter2));
    }

    @Test
    public void getAllFiltersWithMainWiki() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);

        when(componentManager.getInstanceMap(NotificationFilter.class))
                .thenReturn(Collections.singletonMap("1", fakeFilter1));

        Collection<NotificationFilter> filters = mocker.getComponentUnderTest().getAllFilters(testUser, false);

        assertEquals(1, filters.size());
        assertTrue(filters.contains(fakeFilter1));
    }

    @Test
    public void getAllFiltersWithOneDisabledFilter() throws Exception
    {
        SystemUserNotificationFilter disabledFilter = mock(SystemUserNotificationFilter.class);

        when(componentManager.getInstanceMap(NotificationFilter.class))
                .thenReturn(Collections.singletonMap("1", disabledFilter));

        when(disabledFilter.getName()).thenReturn(SystemUserNotificationFilter.FILTER_NAME);
        Map<String, Boolean> filterActivations = new HashMap<>();
        filterActivations.put(SystemUserNotificationFilter.FILTER_NAME, false);

        Collection<NotificationFilter> filters = mocker.getComponentUnderTest().getEnabledFilters(
                mocker.getComponentUnderTest().getAllFilters(testUser, true), filterActivations).collect(Collectors.toList());

        assertEquals(0, filters.size());
    }

    @Test
    public void getFiltersWithMatchingFilters() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);

        NotificationPreference preference = mock(NotificationPreference.class);

        when(fakeFilter1.matchesPreference(preference)).thenReturn(true);

        Collection<NotificationFilter> filters = mocker.getComponentUnderTest().getFiltersRelatedToNotificationPreference(
                Arrays.asList(fakeFilter1), preference).collect(Collectors.toList());

        assertEquals(1, filters.size());
        assertTrue(filters.contains(fakeFilter1));
    }

    @Test
    public void getFiltersWithOneBadFilter() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);

        NotificationPreference preference = mock(NotificationPreference.class);

        when(fakeFilter1.matchesPreference(preference)).thenReturn(false);

        Collection<NotificationFilter> filters = mocker.getComponentUnderTest()
                .getFiltersRelatedToNotificationPreference(Arrays.asList(fakeFilter1), preference).collect(Collectors.toList());

        assertEquals(0, filters.size());
    }

    @Test
    public void testFilterPreferences() throws Exception
    {
        NotificationFilterPreference filterPreference1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPreference2 = mock(NotificationFilterPreference.class);

        when(testProvider.getFilterPreferences(testUser)).thenReturn(Sets.newSet(filterPreference1, filterPreference2));

        Collection<NotificationFilterPreference> resultSet = mocker.getComponentUnderTest().getFilterPreferences(testUser);

        assertTrue(resultSet.contains(filterPreference1));
        assertTrue(resultSet.contains(filterPreference2));
        assertEquals(2, resultSet.size());
    }

    @Test
    public void testFilterPreferencesWithFilter() throws Exception
    {
        NotificationFilterPreference filterPreference1 = mock(NotificationFilterPreference.class);
        when(filterPreference1.getFilterName()).thenReturn("someFilter");
        NotificationFilterPreference filterPreference2 = mock(NotificationFilterPreference.class);
        when(filterPreference2.getFilterName()).thenReturn("fakeFilter");

        Collection<NotificationFilterPreference> filterPreferences =
                Sets.newSet(filterPreference1, filterPreference2);

        NotificationFilter fakeFilter = mock(NotificationFilter.class);
        when(fakeFilter.getName()).thenReturn("fakeFilter");

        Collection<NotificationFilterPreference> resultSet = mocker.getComponentUnderTest()
                .getFilterPreferences(filterPreferences, fakeFilter).collect(Collectors.toList());

        assertTrue(resultSet.contains(filterPreference2));
        assertEquals(1, resultSet.size());
    }

    @Test
    public void testFilterPreferencesWithFilterAndFilterType() throws Exception
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

        Collection<NotificationFilterPreference> resultSet = mocker.getComponentUnderTest()
                .getFilterPreferences(filterPreferences, fakeFilter, NotificationFilterType.INCLUSIVE).collect(
                        Collectors.toList());

        assertTrue(resultSet.contains(filterPreference4));
        assertEquals(1, resultSet.size());
    }

    @Test
    public void saveFilterPreferences() throws Exception
    {
        when(componentManager.hasComponent(NotificationFilterPreferenceProvider.class, "testProvider"))
                .thenReturn(true);
        when(componentManager.getInstance(NotificationFilterPreferenceProvider.class, "testProvider"))
                .thenReturn(testProvider);

        Set<NotificationFilterPreference> testSet = new HashSet<>();
        NotificationFilterPreference testPref1 = mock(NotificationFilterPreference.class);
        when(testPref1.getProviderHint()).thenReturn("testProvider");
        testSet.add(testPref1);

        mocker.getComponentUnderTest().saveFilterPreferences(testSet);

        verify(testProvider, times(1)).saveFilterPreferences(testSet);
    }

    @Test
    public void deleteFilterPreference() throws Exception
    {
        mocker.getComponentUnderTest().deleteFilterPreference("myFilter");

        verify(testProvider, times(1)).deleteFilterPreference(eq("myFilter"));
    }

    @Test
    public void setFilterPreferenceEnabled() throws Exception
    {
        mocker.getComponentUnderTest().setFilterPreferenceEnabled("myFilter1", true);
        mocker.getComponentUnderTest().setFilterPreferenceEnabled("myFilter2", false);

        verify(testProvider, times(1)).setFilterPreferenceEnabled(
                eq("myFilter1"), eq(true));
        verify(testProvider, times(1)).setFilterPreferenceEnabled(
                eq("myFilter2"), eq(false));
    }
}
