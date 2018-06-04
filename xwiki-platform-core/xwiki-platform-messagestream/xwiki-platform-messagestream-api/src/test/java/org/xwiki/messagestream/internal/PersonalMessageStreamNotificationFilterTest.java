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
package org.xwiki.messagestream.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.internal.user.EventUserFilterPreferencesGetter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since
 */
public class PersonalMessageStreamNotificationFilterTest
{
    @Rule
    public MockitoComponentMockingRule<PersonalMessageStreamNotificationFilter> mocker =
            new MockitoComponentMockingRule<>(PersonalMessageStreamNotificationFilter.class);

    private EntityReferenceSerializer<String> serializer;
    private EventUserFilterPreferencesGetter preferencesGetter;

    @Before
    public void setUp() throws Exception
    {
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        preferencesGetter = mocker.getInstance(EventUserFilterPreferencesGetter.class);
    }

    @Test
    public void filterEvent() throws Exception
    {
        DocumentReference user1 = new DocumentReference("xwiki", "XWiki", "User");
        DocumentReference user2 = new DocumentReference("xwiki", "XWiki", "User2");
        when(serializer.serialize(user1)).thenReturn("xwiki:XWiki.User1");
        when(serializer.serialize(user2)).thenReturn("xwiki:XWiki.User2");

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        when(event1.getUser()).thenReturn(user1);
        when(event2.getUser()).thenReturn(user2);
        when(preferencesGetter.isUsedFollowed("xwiki:XWiki.User1", null, NotificationFormat.ALERT))
                .thenReturn(true);
        when(preferencesGetter.isUsedFollowed("xwiki:XWiki.User2", null, NotificationFormat.ALERT))
                .thenReturn(false);

        assertEquals(NotificationFilter.FilterPolicy.KEEP,
                mocker.getComponentUnderTest().filterEvent(event1, null, null, NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event2, null, null, NotificationFormat.ALERT));
    }

    @Test
    public void matchesPreference() throws Exception
    {
        NotificationPreference notificationPreference = mock(NotificationPreference.class);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, "personalMessage");
        when(notificationPreference.getProperties()).thenReturn(properties);

        assertTrue(mocker.getComponentUnderTest().matchesPreference(notificationPreference));

        NotificationPreference notificationPreference2 = mock(NotificationPreference.class);
        Map<NotificationPreferenceProperty, Object> properties2 = new HashMap<>();
        properties2.put(NotificationPreferenceProperty.EVENT_TYPE, "otherEventType");
        when(notificationPreference2.getProperties()).thenReturn(properties2);

        assertFalse(mocker.getComponentUnderTest().matchesPreference(notificationPreference2));
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals("Personal Message Stream Notification Filter", mocker.getComponentUnderTest().getName());
    }

}
