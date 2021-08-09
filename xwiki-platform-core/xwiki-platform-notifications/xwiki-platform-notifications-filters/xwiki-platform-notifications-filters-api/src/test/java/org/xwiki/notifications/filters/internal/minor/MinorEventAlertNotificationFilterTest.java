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
package org.xwiki.notifications.filters.internal.minor;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MinorEventAlertNotificationFilter}.
 */
@ComponentTest
class MinorEventAlertNotificationFilterTest
{
    @InjectMockComponents
    private MinorEventAlertNotificationFilter filter;

    @Test
    void filterEvent()
    {
        DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "UserA");
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);
        when(event1.getType()).thenReturn("update");
        when(event1.getDocumentVersion()).thenReturn("2.12");
        when(event2.getType()).thenReturn("addComment");
        when(event2.getDocumentVersion()).thenReturn("2.12");
        when(event3.getType()).thenReturn("update");
        when(event3.getDocumentVersion()).thenReturn("2.1");
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                this.filter.filterEvent(event1, randomUser, Collections.emptyList(),
                        NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.filter.filterEvent(event1, randomUser, Collections.emptyList(),
                NotificationFormat.EMAIL));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                this.filter.filterEvent(event2, randomUser, Collections.emptyList(),
                        NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                this.filter.filterEvent(event3, randomUser, Collections.emptyList(),
                        NotificationFormat.ALERT));
    }

    @Test
    void filterExpression()
    {
        NotificationPreference fakePreference = mock(NotificationPreference.class);

        DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "UserA");
        assertNull(this.filter.filterExpression(randomUser, Collections.emptyList(), fakePreference));
        assertEquals("NOT ((TYPE = \"update\" AND NOT (DOCUMENT_VERSION ENDS WITH \".1\")))",
                this.filter.filterExpression(randomUser, Collections.emptyList(),
                        NotificationFilterType.EXCLUSIVE,
                        NotificationFormat.ALERT).toString());
    }

    @Test
    void filterExpressionWithWrongParameters()
    {
        DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "UserA");
        assertNull(this.filter.filterExpression(randomUser, Collections.emptyList(),
                        NotificationFilterType.INCLUSIVE, NotificationFormat.ALERT));
        assertNull(this.filter.filterExpression(randomUser, Collections.emptyList(),
                NotificationFilterType.EXCLUSIVE, NotificationFormat.EMAIL));
    }

    @Test
    void matchesPreference()
    {
        assertFalse(this.filter.matchesPreference(mock(NotificationPreference.class)));
    }

    @Test
    void getName()
    {
        assertEquals(MinorEventAlertNotificationFilter.FILTER_NAME, this.filter.getName());
    }

    @Test
    void getFormats()
    {
        assertEquals(1, this.filter.getFormats().size());
        assertTrue(this.filter.getFormats().contains(NotificationFormat.ALERT));
    }
}
