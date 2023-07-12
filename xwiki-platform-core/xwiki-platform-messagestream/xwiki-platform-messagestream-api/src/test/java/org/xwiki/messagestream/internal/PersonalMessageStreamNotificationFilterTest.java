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

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.filters.internal.user.EventUserFilterPreferencesGetter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.NotificationFormat.ALERT;
import static org.xwiki.notifications.filters.NotificationFilter.FilterPolicy.FILTER;
import static org.xwiki.notifications.filters.NotificationFilter.FilterPolicy.KEEP;
import static org.xwiki.notifications.filters.NotificationFilter.FilterPolicy.NO_EFFECT;
import static org.xwiki.notifications.preferences.NotificationPreferenceProperty.EVENT_TYPE;

/**
 * Test of {@link PersonalMessageStreamNotificationFilter}.
 *
 * @version $Id$
 */
@ComponentTest
class PersonalMessageStreamNotificationFilterTest
{
    @InjectMockComponents
    private PersonalMessageStreamNotificationFilter personalMessageFilter;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private EventUserFilterPreferencesGetter preferencesGetter;

    @Test
    void filterEvent()
    {
        DocumentReference user1 = new DocumentReference("xwiki", "XWiki", "User");
        DocumentReference user2 = new DocumentReference("xwiki", "XWiki", "User2");
        when(this.serializer.serialize(user1)).thenReturn("xwiki:XWiki.User1");
        when(this.serializer.serialize(user2)).thenReturn("xwiki:XWiki.User2");

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);
        when(event1.getUser()).thenReturn(user1);
        when(event2.getUser()).thenReturn(user2);
        when(event3.getUser()).thenReturn(user2);
        when(event1.getType()).thenReturn("personalMessage");
        when(event2.getType()).thenReturn("personalMessage");
        when(event3.getType()).thenReturn("otherType");
        when(this.preferencesGetter.isUserFollowed("xwiki:XWiki.User1", null, ALERT))
            .thenReturn(true);
        when(this.preferencesGetter.isUserFollowed("xwiki:XWiki.User2", null, ALERT))
            .thenReturn(false);

        assertEquals(KEEP, this.personalMessageFilter.filterEvent(event1, null, null, ALERT));
        assertEquals(FILTER, this.personalMessageFilter.filterEvent(event2, null, null, ALERT));
        assertEquals(NO_EFFECT, this.personalMessageFilter.filterEvent(event3, null, null, ALERT));
    }

    @Test
    void matchesPreference()
    {
        NotificationPreference notificationPreference = mock(NotificationPreference.class);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(EVENT_TYPE, "personalMessage");
        when(notificationPreference.getProperties()).thenReturn(properties);

        assertTrue(this.personalMessageFilter.matchesPreference(notificationPreference));

        NotificationPreference notificationPreference2 = mock(NotificationPreference.class);
        Map<NotificationPreferenceProperty, Object> properties2 = new HashMap<>();
        properties2.put(EVENT_TYPE, "otherEventType");
        when(notificationPreference2.getProperties()).thenReturn(properties2);

        assertFalse(this.personalMessageFilter.matchesPreference(notificationPreference2));
    }

    @Test
    void getName()
    {
        assertEquals("Personal Message Stream Notification Filter", this.personalMessageFilter.getName());
    }

    @Test
    void filterExpressionNull()
    {
        assertNull(this.personalMessageFilter.filterExpression(null, null, null));
    }
}
