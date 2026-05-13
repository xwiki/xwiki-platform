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
package org.xwiki.notifications.filters.internal.user;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 1.10
 */
@ComponentTest
@ComponentList({
    EventUserFilterPreferencesGetter.class,
})
class EventUserFilterTest
{
    @InjectMockComponents
    private EventUserFilter eventUserFilter;

    @MockComponent
    private NotificationFilterManager notificationFilterManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    private static final DocumentReference USER_A = new DocumentReference("xwiki", "XWiki", "UserA");

    private static final DocumentReference USER_B = new DocumentReference("xwiki", "XWiki", "UserB");

    private static final DocumentReference USER_C = new DocumentReference("xwiki", "XWiki", "UserC");

    private static final DocumentReference USER_D = new DocumentReference("xwiki", "XWiki", "UserD");

    private static final DocumentReference USER_E = new DocumentReference("xwiki", "XWiki", "UserE");

    private static final String SERIALIZED_USER_A = "userA";

    private static final String SERIALIZED_USER_B = "userB";

    private static final String SERIALIZED_USER_C = "userC";

    private static final String SERIALIZED_USER_D = "userD";

    private static final String SERIALIZED_USER_E = "userE";

    private static final DocumentReference CURRENT_USER = new DocumentReference("xwiki", "XWiki", "TestMan");

    @BeforeEach
    void setUp()
    {
        when(this.serializer.serialize(USER_A)).thenReturn(SERIALIZED_USER_A);
        when(this.serializer.serialize(USER_B)).thenReturn(SERIALIZED_USER_B);
        when(this.serializer.serialize(USER_C)).thenReturn(SERIALIZED_USER_C);
        when(this.serializer.serialize(USER_D)).thenReturn(SERIALIZED_USER_D);
        when(this.serializer.serialize(USER_E)).thenReturn(SERIALIZED_USER_E);
    }

    private Collection<NotificationFilterPreference> mockPreferences() throws NotificationException
    {
        NotificationFilterPreference p1 = mock(NotificationFilterPreference.class);
        when(p1.isEnabled()).thenReturn(true);
        when(p1.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p1.getUser()).thenReturn(SERIALIZED_USER_A);
        when(p1.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p1.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference p1bis = mock(NotificationFilterPreference.class);
        when(p1bis.isEnabled()).thenReturn(true);
        when(p1bis.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p1bis.getUser()).thenReturn(SERIALIZED_USER_B);
        when(p1bis.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p1bis.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference p2 = mock(NotificationFilterPreference.class);
        when(p2.isEnabled()).thenReturn(true);
        when(p2.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p2.getUser()).thenReturn(SERIALIZED_USER_C);
        when(p2.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p2.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference p3 = mock(NotificationFilterPreference.class);
        when(p3.isEnabled()).thenReturn(false);
        when(p3.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p3.getUser()).thenReturn(SERIALIZED_USER_D);
        when(p3.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p3.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference p4 = mock(NotificationFilterPreference.class);
        when(p4.isEnabled()).thenReturn(true);
        when(p4.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p4.getUser()).thenReturn(SERIALIZED_USER_E);
        when(p4.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p4.getNotificationFormats()).thenReturn(Set.of(NotificationFormat.EMAIL));

        return new LinkedHashSet<>(List.of(p1, p1bis, p2, p3, p4));
    }

    @Test
    void filterEvent() throws Exception
    {
        Collection<NotificationFilterPreference> filterPreferences = mockPreferences();

        Event event1 = mock(Event.class);
        when(event1.getUser()).thenReturn(USER_A);

        Event event2 = mock(Event.class);
        when(event2.getUser()).thenReturn(USER_B);

        Event event3 = mock(Event.class);
        when(event3.getUser()).thenReturn(USER_C);

        Event event4 = mock(Event.class);
        when(event4.getUser()).thenReturn(USER_D);

        Event event5 = mock(Event.class);
        when(event5.getUser()).thenReturn(USER_E);

        assertEquals(NotificationFilter.FilterPolicy.FILTER,
            this.eventUserFilter.filterEvent(event1, CURRENT_USER, filterPreferences, NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
            this.eventUserFilter.filterEvent(event2, CURRENT_USER, filterPreferences, NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
            this.eventUserFilter.filterEvent(event3, CURRENT_USER, filterPreferences, NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.eventUserFilter.filterEvent(event4, CURRENT_USER, filterPreferences, NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.eventUserFilter.filterEvent(event5, CURRENT_USER, filterPreferences, NotificationFormat.ALERT));
    }

    @Test
    void generateFilterExpressionWithPreferences() throws Exception
    {
        Collection<NotificationFilterPreference> filterPreferences = mockPreferences();

        NotificationPreference notificationPreference = mock(NotificationPreference.class);
        when(notificationPreference.getFormat()).thenReturn(NotificationFormat.ALERT);

        assertNull(this.eventUserFilter.filterExpression(CURRENT_USER, filterPreferences, notificationPreference));
    }

    @Test
    void generateFilterExpression() throws Exception
    {
        Collection<NotificationFilterPreference> filterPreferences = mockPreferences();

        ExpressionNode node = this.eventUserFilter.filterExpression(CURRENT_USER, filterPreferences,
            NotificationFilterType.EXCLUSIVE, NotificationFormat.ALERT);

        assertNotNull(node);
        assertEquals("NOT (USER IN (\"userA\", \"userB\", \"userC\"))", node.toString());
    }

    @Test
    void getName()
    {
        assertEquals("eventUserNotificationFilter", this.eventUserFilter.getName());
    }
}
