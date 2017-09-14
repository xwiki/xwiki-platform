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

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class UsersNotificationFilterTest
{
    @Rule
    public final MockitoComponentMockingRule<UsersNotificationFilter> mocker =
            new MockitoComponentMockingRule<>(UsersNotificationFilter.class);

    private NotificationFilterManager notificationFilterManager;
    private EntityReferenceSerializer<String> serializer;

    @Before
    public void setUp() throws Exception
    {
        notificationFilterManager = mocker.getInstance(NotificationFilterManager.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
    }

    @Test
    public void test() throws Exception
    {
        Event event = mock(Event.class);
        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");
        DocumentReference userB = new DocumentReference("xwiki", "XWiki", "UserB");

        when(event.getUser()).thenReturn(userB);
        when(event.getType()).thenReturn("update");

        NotificationFilterPreference notificationFilterPreference = mock(NotificationFilterPreference.class);

        when(notificationFilterManager.getFilterPreferences(eq(userA), any(NotificationFilter.class),
                eq(NotificationFilterType.EXCLUSIVE), eq(NotificationFormat.ALERT)))
                .thenReturn(Sets.newHashSet(notificationFilterPreference));

        when(notificationFilterPreference.getFilterName()).thenReturn("usersNotificationFilter");
        when(notificationFilterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE)).thenReturn(
                Collections.singletonList("update"));
        when(notificationFilterPreference.getProperties(NotificationFilterProperty.USER)).thenReturn(
                Collections.singletonList("xwiki:XWiki.UserB"));

        when(serializer.serialize(eq(userB))).thenReturn("xwiki:XWiki.UserB");

        boolean result = mocker.getComponentUnderTest().filterEvent(event, userA, NotificationFormat.ALERT);

        assertTrue(result);

    }
}
