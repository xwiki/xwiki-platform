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
package org.xwiki.notifications.sources.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PreferenceDateNotificationFilter}.
 *
 * @version $Id$
 */
@ComponentTest
public class PreferenceDateNotificationFilterTest
{
    @InjectMockComponents
    private PreferenceDateNotificationFilter notificationFilter;

    @Test
    void shouldFilterDefault()
    {
        assertFalse(this.notificationFilter.shouldFilter(mock(Event.class), Collections.emptyList()));
    }

    @Test
    void shouldFilterEvent()
    {
        Event event = mock(Event.class);
        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);
        String eventType = "myEventType";
        when(event.getType()).thenReturn(eventType);

        // pref1 will be discarded
        when(pref1.getProperties()).thenReturn(Collections.emptyMap());
        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, eventType));

        when(event.getDate()).thenReturn(new Date(41));
        when(pref2.getStartDate()).thenReturn(new Date(42));
        assertTrue(this.notificationFilter.shouldFilter(event, Arrays.asList(pref1, pref2)));
    }

    @Test
    void shouldNotFilterWrongType()
    {
        Event event = mock(Event.class);
        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);
        String eventType = "myEventType";
        when(event.getType()).thenReturn(eventType);

        when(pref1.getProperties()).thenReturn(Collections.emptyMap());
        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "anotherType"));
        assertFalse(this.notificationFilter.shouldFilter(event, Arrays.asList(pref1, pref2)));
    }

    @Test
    void shouldNotFilterWrongDate()
    {
        Event event = mock(Event.class);
        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);
        String eventType = "myEventType";
        when(event.getType()).thenReturn(eventType);

        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, eventType));
        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, eventType));

        when(event.getDate()).thenReturn(new Date(40));
        when(pref2.getStartDate()).thenReturn(new Date(0));
        when(pref2.getStartDate()).thenReturn(new Date(39));
        assertFalse(this.notificationFilter.shouldFilter(event, Arrays.asList(pref1, pref2)));
    }

    @Test
    void shouldFilterEventSameDate()
    {
        Event event = mock(Event.class);
        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);
        String eventType = "myEventType";
        when(event.getType()).thenReturn(eventType);

        // pref1 will be discarded
        when(pref1.getProperties()).thenReturn(Collections.emptyMap());
        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, eventType));

        when(event.getDate()).thenReturn(new Date(42));
        when(pref2.getStartDate()).thenReturn(new Date(42));
        assertFalse(this.notificationFilter.shouldFilter(event, Arrays.asList(pref1, pref2)));
    }
}
