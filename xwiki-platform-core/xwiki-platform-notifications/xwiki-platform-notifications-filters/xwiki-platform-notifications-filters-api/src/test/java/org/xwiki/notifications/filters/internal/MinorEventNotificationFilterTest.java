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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.minor.MinorEventAlertNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MinorEventNotificationFilterTest
{
    @Rule
    public final MockitoComponentMockingRule<MinorEventAlertNotificationFilter> mocker =
            new MockitoComponentMockingRule<>(MinorEventAlertNotificationFilter.class);

    @Test
    public void filterEvent() throws Exception
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
                mocker.getComponentUnderTest().filterEvent(event1, randomUser, NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event2, randomUser, NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event3, randomUser, NotificationFormat.ALERT));
    }

    @Test
    public void filterExpression() throws Exception
    {
        NotificationPreference fakePreference = mock(NotificationPreference.class);

        DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "UserA");
        assertNull(mocker.getComponentUnderTest().filterExpression(randomUser, fakePreference));
        assertEquals("NOT ((TYPE = \"update\" AND NOT (DOCUMENT_VERSION ENDS WITH \".1\")))",
                mocker.getComponentUnderTest().filterExpression(randomUser, NotificationFilterType.EXCLUSIVE,
                        NotificationFormat.ALERT).toString());
    }

    @Test
    public void matchesPreference() throws Exception
    {
        assertFalse(mocker.getComponentUnderTest().matchesPreference(mock(NotificationPreference.class)));
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals(MinorEventAlertNotificationFilter.FILTER_NAME, mocker.getComponentUnderTest().getName());
    }
}
