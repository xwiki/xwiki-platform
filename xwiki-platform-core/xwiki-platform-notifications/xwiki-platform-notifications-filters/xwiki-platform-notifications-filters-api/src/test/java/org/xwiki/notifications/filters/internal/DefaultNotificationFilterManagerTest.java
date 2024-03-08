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
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @MockComponent
    private ModelContext modelContext;

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
    void getAllFiltersForWiki() throws Exception
    {
        NotificationFilter fakeFilter1 = mock(NotificationFilter.class);
        NotificationFilter fakeFilter2 = mock(NotificationFilter.class);

        WikiReference currentWiki = new WikiReference("current");
        when(wikiDescriptorManager.getCurrentWikiReference()).thenReturn(currentWiki);

        WikiReference argumentWiki = new WikiReference("foo");

        when(componentManager.getInstanceList(NotificationFilter.class))
            .thenReturn(Arrays.asList(fakeFilter1, fakeFilter2));

        Collection<NotificationFilter> filters = this.filterManager.getAllFilters(argumentWiki);

        assertEquals(2, filters.size());
        assertTrue(filters.contains(fakeFilter1));
        assertTrue(filters.contains(fakeFilter2));
        verify(this.modelContext).setCurrentEntityReference(argumentWiki);
        verify(this.modelContext).setCurrentEntityReference(currentWiki);
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
    void getFiltersWithSpecificFilteringPhase() throws Exception
    {
        NotificationFilter filter1 = mock(NotificationFilter.class);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        NotificationFilter filter3 = mock(NotificationFilter.class);
        NotificationFilter filter4 = mock(NotificationFilter.class);
        NotificationFilter filter5 = mock(NotificationFilter.class);
        NotificationFilter filter6 = mock(NotificationFilter.class);
        NotificationFilter filter7 = mock(NotificationFilter.class);

        when(filter1.getName()).thenReturn("filter1");
        when(filter2.getName()).thenReturn("filter2");
        when(filter3.getName()).thenReturn("filter3");
        when(filter4.getName()).thenReturn("filter4");
        when(filter5.getName()).thenReturn("filter5");
        when(filter6.getName()).thenReturn("filter6");
        when(filter7.getName()).thenReturn("filter7");

        Map<String, Boolean> filterActivations = new HashMap<>();
        filterActivations.put("filter1", true);
        filterActivations.put("filter2", false);
        filterActivations.put("filter3", true);
        filterActivations.put("filter4", false);
        filterActivations.put("filter5", true);
        filterActivations.put("filter6", false);
        // We don't put filter7 so it should default as being considered activated

        //when(this.filterPreferencesModelBridge.getToggleableFilterActivations(testUser)).thenReturn
        // (filterActivations);
        when(filter1.getFilteringPhases())
            .thenReturn(NotificationFilter.SUPPORT_ONLY_PRE_FILTERING_PHASE);
        when(filter2.getFilteringPhases())
            .thenReturn(NotificationFilter.SUPPORT_ONLY_PRE_FILTERING_PHASE);
        when(filter3.getFilteringPhases())
            .thenReturn(NotificationFilter.SUPPORT_ONLY_POST_FILTERING_PHASE);
        when(filter4.getFilteringPhases())
            .thenReturn(NotificationFilter.SUPPORT_ONLY_POST_FILTERING_PHASE);
        when(filter5.getFilteringPhases())
            .thenReturn(NotificationFilter.SUPPORT_BOTH_FILTERING_PHASE);
        when(filter6.getFilteringPhases())
            .thenReturn(NotificationFilter.SUPPORT_BOTH_FILTERING_PHASE);
        when(filter7.getFilteringPhases())
            .thenReturn(NotificationFilter.SUPPORT_BOTH_FILTERING_PHASE);

        // Using subwiki so we manipulate a set instead of a map
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("somethingElseThanwiki");
        when(componentManager.getInstanceList(NotificationFilter.class))
            .thenReturn(Arrays.asList(filter1, filter2, filter3, filter4, filter5, filter6, filter7));

        // we request prefiltering filters that are enabled:
        // we should not return filter2/4/6 since they are not enabled,
        // nor filter3 because it's only for post-filtering
        Collection<NotificationFilter> allFilters = this.filterManager.getAllFilters(testUser, true,
            NotificationFilter.FilteringPhase.PRE_FILTERING);
        assertEquals(3, allFilters.size());
        assertEquals(new HashSet<>(Arrays.asList(filter1, filter5, filter7)), allFilters);

        // we request postfiltering filters that are enabled:
        // we should not return filter2/4/6 since they are not enabled,
        // nor filter1 because it's only for pre-filtering, nor filter5 because it's for both pre and post filtering
        allFilters = this.filterManager.getAllFilters(testUser, true, NotificationFilter.FilteringPhase.POST_FILTERING);
        assertEquals(1, allFilters.size());
        assertEquals(Collections.singleton(filter3), allFilters);

        // we request all filters that are enabled:
        // we should not return filter2/4/6 since they are not enabled.
        allFilters = this.filterManager.getAllFilters(testUser, true, null);
        assertEquals(4, allFilters.size());
        assertEquals(new HashSet<>(Arrays.asList(filter1, filter3, filter5, filter7)), allFilters);

        // we request prefiltering filters whatever if they are enabled or not
        // we should not return filter3 and 4 because they are only for post-filtering
        allFilters = this.filterManager.getAllFilters(testUser, false,
            NotificationFilter.FilteringPhase.PRE_FILTERING);
        assertEquals(5, allFilters.size());
        assertEquals(new HashSet<>(Arrays.asList(filter1, filter2, filter5, filter6, filter7)), allFilters);

        // we request postfiltering filters whatever if they are enabled or not
        // we should not return filter1/2 because they are only for pre-filtering,
        // nor filter5/6/7 because they are for both pre and post filtering
        allFilters = this.filterManager.getAllFilters(testUser, false,
            NotificationFilter.FilteringPhase.POST_FILTERING);
        assertEquals(2, allFilters.size());
        assertEquals(new HashSet<>(Arrays.asList(filter3, filter4)), allFilters);

        // we request all filters  whatever if they are enabled or not:
        // we return all filters
        allFilters = this.filterManager.getAllFilters(testUser, false, null);
        assertEquals(7, allFilters.size());
        assertEquals(new HashSet<>(Arrays.asList(filter1, filter2, filter3, filter4, filter5, filter6, filter7)),
            allFilters);
    }

}
