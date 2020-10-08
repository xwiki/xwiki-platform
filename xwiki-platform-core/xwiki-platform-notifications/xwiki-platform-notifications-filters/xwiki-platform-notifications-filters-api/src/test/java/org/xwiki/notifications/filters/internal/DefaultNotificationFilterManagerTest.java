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
import java.util.stream.Collectors;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationFilterManager}
 *
 * @version $Id$
 * @since 9.7RC1
 */
@ComponentTest
public class DefaultNotificationFilterManagerTest
{
    @InjectMockComponents
    private DefaultNotificationFilterManager filterManager;

    @MockComponent
    private ComponentManager componentManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private DocumentReference testUser;

    @MockComponent
    @Named("cached")
    private ModelBridge modelBridge;

    @BeforeEach
    void setUp() throws Exception
    {
        testUser = new DocumentReference("wiki", "test", "user");

        // Set a default comportment for the wikiDescriptorManager
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("wiki");
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentWikiId");
        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("wiki", "currentWikiId"));
    }

    @Test
    void getAllNotificationsFiltersWithSubWiki() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);
        NotificationFilter fakeFilter2 = mock(NotificationFilter.class);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("somethingElseThanwiki");

        when(componentManager.getInstanceList(NotificationFilter.class))
                .thenReturn(Arrays.asList(fakeFilter1, fakeFilter2));

        Collection<NotificationFilter> filters = this.filterManager.getAllFilters(testUser, false);

        assertEquals(2, filters.size());
        assertTrue(filters.contains(fakeFilter1));
        assertTrue(filters.contains(fakeFilter2));
    }

    @Test
    void getAllFiltersWithMainWiki() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);

        when(componentManager.getInstanceMap(NotificationFilter.class))
                .thenReturn(Collections.singletonMap("1", fakeFilter1));

        Collection<NotificationFilter> filters = this.filterManager.getAllFilters(testUser, false);

        assertEquals(1, filters.size());
        assertTrue(filters.contains(fakeFilter1));
    }

    @Test
    void getAllFiltersWithOneDisabledFilter() throws Exception
    {
        SystemUserNotificationFilter disabledFilter = mock(SystemUserNotificationFilter.class);

        when(componentManager.getInstanceMap(NotificationFilter.class))
                .thenReturn(Collections.singletonMap("1", disabledFilter));

        when(disabledFilter.getName()).thenReturn(SystemUserNotificationFilter.FILTER_NAME);
        Map<String, Boolean> filterActivations = new HashMap<>();
        filterActivations.put(SystemUserNotificationFilter.FILTER_NAME, false);

        Collection<NotificationFilter> filters = this.filterManager.getEnabledFilters(
                this.filterManager.getAllFilters(testUser, true), filterActivations).collect(Collectors.toList());

        assertEquals(0, filters.size());
    }

    @Test
    void getFiltersWithMatchingFilters() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);

        NotificationPreference preference = mock(NotificationPreference.class);

        when(fakeFilter1.matchesPreference(preference)).thenReturn(true);

        Collection<NotificationFilter> filters = this.filterManager.getFiltersRelatedToNotificationPreference(
                Arrays.asList(fakeFilter1), preference).collect(Collectors.toList());

        assertEquals(1, filters.size());
        assertTrue(filters.contains(fakeFilter1));
    }

    @Test
    void getFiltersWithOneBadFilter() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);

        NotificationPreference preference = mock(NotificationPreference.class);

        when(fakeFilter1.matchesPreference(preference)).thenReturn(false);

        Collection<NotificationFilter> filters = this.filterManager
            .getFiltersRelatedToNotificationPreference(Arrays.asList(fakeFilter1), preference)
            .collect(Collectors.toList());

        assertEquals(0, filters.size());
    }

    @Test
    void getFiltersWithSpecificFilteringMoment() throws Exception
    {
        NotificationFilter filter1 = mock(NotificationFilter.class);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        NotificationFilter filter3 = mock(NotificationFilter.class);
        NotificationFilter filter4 = mock(NotificationFilter.class);

        when(filter1.getName()).thenReturn("filter1");
        when(filter2.getName()).thenReturn("filter2");
        when(filter3.getName()).thenReturn("filter3");
        when(filter4.getName()).thenReturn("filter4");

        Map<String, Boolean> filterActivations = new HashMap<>();
        filterActivations.put("filter1", true);
        filterActivations.put("filter2", false);
        filterActivations.put("filter3", true);
        filterActivations.put("filter4", true);

        when(this.modelBridge.getToggeableFilterActivations(testUser)).thenReturn(filterActivations);
        when(filter1.getFilteringMoment()).thenReturn(NotificationFilter.FilteringMoment.BOTH);
        when(filter2.getFilteringMoment()).thenReturn(NotificationFilter.FilteringMoment.ONLY_PREFILTERING);
        when(filter3.getFilteringMoment()).thenReturn(NotificationFilter.FilteringMoment.ONLY_POSTFILTERING);
        when(filter4.getFilteringMoment()).thenReturn(NotificationFilter.FilteringMoment.ONLY_PREFILTERING);

        // Using subwiki so we manipulate a set instead of a map
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("somethingElseThanwiki");
        when(componentManager.getInstanceList(NotificationFilter.class))
            .thenReturn(Arrays.asList(filter1, filter2, filter3, filter4));

        Collection<NotificationFilter> allFilters = this.filterManager.getAllFilters(testUser, true,
            new HashSet<>(
                Arrays.asList(NotificationFilter.FilteringMoment.BOTH,
                    NotificationFilter.FilteringMoment.ONLY_PREFILTERING)));
        assertEquals(2, allFilters.size());
        assertEquals(new HashSet<>(Arrays.asList(filter1, filter4)), allFilters);

        allFilters = this.filterManager.getAllFilters(testUser, false,
            new HashSet<>(
                Arrays.asList(NotificationFilter.FilteringMoment.BOTH,
                    NotificationFilter.FilteringMoment.ONLY_PREFILTERING)));
        assertEquals(3, allFilters.size());
        assertEquals(new HashSet<>(Arrays.asList(filter1, filter2, filter4)), allFilters);

        allFilters = this.filterManager.getAllFilters(testUser, true, Collections.singleton(
            NotificationFilter.FilteringMoment.ONLY_POSTFILTERING));
        assertEquals(1, allFilters.size());
        assertEquals(Collections.singleton(filter3), allFilters);
    }

}
