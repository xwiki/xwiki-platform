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
import static org.xwiki.notifications.filters.NotificationFilter.FilterPolicy.FILTER;
import static org.xwiki.notifications.filters.NotificationFilter.FilterPolicy.NO_EFFECT;

/**
 * Test of {@link DirectMessageStreamNotificationFilter}.
 * @version $Id$
 */
@ComponentTest
class DirectMessageStreamNotificationFilterTest
{
    @InjectMockComponents
    private DirectMessageStreamNotificationFilter groupMessageFilter;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @Test
    void filterEvent()
    {
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        when(this.serializer.serialize(user)).thenReturn("xwiki:XWiki.User");

        Event event1 = mock(Event.class);
        when(event1.getType()).thenReturn("directMessage");
        when(event1.getStream()).thenReturn("xwiki:XWiki.User");
        Event event2 = mock(Event.class);
        when(event2.getType()).thenReturn("directMessage");
        when(event2.getStream()).thenReturn("xwiki:XWiki.OtherUser");
        Event event3 = mock(Event.class);
        when(event3.getType()).thenReturn("someType");
        when(event3.getStream()).thenReturn("xwiki:XWiki.OtherUser");

        assertEquals(NO_EFFECT, this.groupMessageFilter.filterEvent(event1, user, null, null));
        assertEquals(FILTER, this.groupMessageFilter.filterEvent(event2, user, null, null));
        assertEquals(NO_EFFECT, this.groupMessageFilter.filterEvent(event3, user, null, null));
    }

    @Test
    void matchesPreference()
    {
        NotificationPreference notificationPreference = mock(NotificationPreference.class);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, "directMessage");
        when(notificationPreference.getProperties()).thenReturn(properties);

        assertTrue(this.groupMessageFilter.matchesPreference(notificationPreference));

        NotificationPreference notificationPreference2 = mock(NotificationPreference.class);
        Map<NotificationPreferenceProperty, Object> properties2 = new HashMap<>();
        properties2.put(NotificationPreferenceProperty.EVENT_TYPE, "otherEventType");
        when(notificationPreference2.getProperties()).thenReturn(properties2);

        assertFalse(this.groupMessageFilter.matchesPreference(notificationPreference2));
    }

    @Test
    void getName()
    {
        assertEquals("Direct Message Stream Notification Filter", this.groupMessageFilter.getName());
    }

    @Test
    void filterExpressionNull()
    {
        assertNull(this.groupMessageFilter.filterExpression(null, null, null));
    }
}
