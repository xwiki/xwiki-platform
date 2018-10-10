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
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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

    private ModelBridge modelBridge;

    @Before
    public void setUp() throws Exception
    {
        componentManager = mocker.registerMockComponent(ComponentManager.class);

        wikiDescriptorManager = mocker.registerMockComponent(WikiDescriptorManager.class);

        modelBridge = mocker.registerMockComponent(ModelBridge.class, "cached");

        testUser = new DocumentReference("wiki", "test", "user");

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
}
