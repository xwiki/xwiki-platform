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
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterProvider;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationFilterManager}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class DefaultNotificationFilterManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultNotificationFilterManager> mocker =
            new MockitoComponentMockingRule<>(DefaultNotificationFilterManager.class);

    private NotificationFilter filter11;
    private NotificationFilter filter12;
    private NotificationFilter filter21;
    private NotificationFilter filter22;

    private NotificationFilterProvider provider1;
    private NotificationFilterProvider provider2;

    @Before
    public void setUp() throws Exception
    {
        ComponentManager componentManager = mocker.registerMockComponent(ComponentManager.class);

        provider1 = mock(NotificationFilterProvider.class);
        filter11 = mock(NotificationFilter.class);
        filter12 = mock(NotificationFilter.class);
        when(provider1.getAllFilters(any(DocumentReference.class))).thenReturn(Sets.newSet(filter11, filter12));

        provider2 = mock(NotificationFilterProvider.class);
        filter21 = mock(NotificationFilter.class);
        filter22 = mock(NotificationFilter.class);
        when(provider2.getAllFilters(any(DocumentReference.class))).thenReturn(Sets.newSet(filter21, filter22));

        when(componentManager.getInstanceList(any()))
                .thenReturn(Arrays.asList(provider1, provider2));
    }

    @Test
    public void getAllFilters() throws Exception
    {
        Set<NotificationFilter> filters = mocker.getComponentUnderTest().getAllFilters(
                new DocumentReference("xwiki", "test", "user"));

        assertEquals(4, filters.size());
    }

    @Test
    public void getFilters() throws Exception
    {
        DocumentReference user = new DocumentReference("xwiki", "test", "user");

        NotificationPreference preference = mock(NotificationPreference.class);

        when(provider1.getFilters(user, preference)).thenReturn(Collections.singleton(filter11));
        when(provider2.getFilters(user, preference)).thenReturn(Collections.singleton(filter21));

        Set<NotificationFilter> filters = mocker.getComponentUnderTest().getFilters(user, preference);

        assertEquals(2, filters.size());
        assertTrue(filters.contains(filter11));
        assertTrue(filters.contains(filter21));
    }
}
