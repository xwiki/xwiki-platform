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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 1.10
 */
@ComponentList({
    EventUserFilterPreferencesGetter.class,
})
public class EventUserFilterTest
{
    @Rule
    public final MockitoComponentMockingRule<EventUserFilter> mocker = new MockitoComponentMockingRule<>(EventUserFilter.class);

    private NotificationFilterManager notificationFilterManager;
    private EntityReferenceSerializer<String> serializer;
    private DocumentReference USER_A = new DocumentReference("xwiki", "XWiki", "UserA");
    private DocumentReference USER_B = new DocumentReference("xwiki", "XWiki", "UserB");
    private DocumentReference USER_C = new DocumentReference("xwiki", "XWiki", "UserC");
    private DocumentReference USER_D = new DocumentReference("xwiki", "XWiki", "UserD");
    private DocumentReference USER_E = new DocumentReference("xwiki", "XWiki", "UserE");
    private String SERIALIZED_USER_A = "userA";
    private String SERIALIZED_USER_B = "userB";
    private String SERIALIZED_USER_C = "userC";
    private String SERIALIZED_USER_D = "userD";
    private String SERIALIZED_USER_E = "userE";
    private DocumentReference CURRENT_USER = new DocumentReference("xwiki", "XWiki", "TestMan");

    @Before
    public void setUp() throws Exception
    {
        notificationFilterManager = mock(NotificationFilterManager.class);
        mocker.registerComponent(NotificationFilterManager.class, notificationFilterManager);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(serializer.serialize(USER_A)).thenReturn(SERIALIZED_USER_A);
        when(serializer.serialize(USER_B)).thenReturn(SERIALIZED_USER_B);
        when(serializer.serialize(USER_C)).thenReturn(SERIALIZED_USER_C);
        when(serializer.serialize(USER_D)).thenReturn(SERIALIZED_USER_D);
        when(serializer.serialize(USER_E)).thenReturn(SERIALIZED_USER_E);
    }

    private Collection<NotificationFilterPreference> mockPreferences() throws NotificationException
    {
        NotificationFilterPreference p1 = mock(NotificationFilterPreference.class);
        when(p1.isEnabled()).thenReturn(true);
        when(p1.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p1.getProperties(NotificationFilterProperty.USER)).thenReturn(Arrays.asList(SERIALIZED_USER_A,
                SERIALIZED_USER_B));
        when(p1.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p1.getFilterFormats()).thenReturn(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference p2 = mock(NotificationFilterPreference.class);
        when(p2.isEnabled()).thenReturn(true);
        when(p2.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p2.getProperties(NotificationFilterProperty.USER)).thenReturn(Arrays.asList(SERIALIZED_USER_C));
        when(p2.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p2.getFilterFormats()).thenReturn(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference p3 = mock(NotificationFilterPreference.class);
        when(p3.isEnabled()).thenReturn(false);
        when(p3.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p3.getProperties(NotificationFilterProperty.USER)).thenReturn(Arrays.asList(SERIALIZED_USER_D));
        when(p3.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p3.getFilterFormats()).thenReturn(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));

        NotificationFilterPreference p4 = mock(NotificationFilterPreference.class);
        when(p4.isEnabled()).thenReturn(true);
        when(p4.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(p4.getProperties(NotificationFilterProperty.USER)).thenReturn(Arrays.asList(SERIALIZED_USER_E));
        when(p4.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(p4.getFilterFormats()).thenReturn(Sets.newSet(NotificationFormat.EMAIL));

        return Sets.newSet(p1, p2, p3, p4);
    }

    @Test
    public void filterEvent() throws Exception
    {
        // Preferences
        Collection<NotificationFilterPreference> filterPreferences = mockPreferences();

        // Mock
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

        // Test
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event1, CURRENT_USER, filterPreferences,
                        NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event2, CURRENT_USER, filterPreferences,
                        NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event3, CURRENT_USER, filterPreferences,
                        NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event4, CURRENT_USER, filterPreferences,
                        NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event5, CURRENT_USER, filterPreferences,
                        NotificationFormat.ALERT));
    }

    @Test
    public void generateFilterExpressionWithPreferences() throws Exception
    {
        // Preferences
        Collection<NotificationFilterPreference> filterPreferences = mockPreferences();

        NotificationPreference notificationPreference = mock(NotificationPreference.class);
        when(notificationPreference.getFormat()).thenReturn(NotificationFormat.ALERT);

        // Test
        assertNull(mocker.getComponentUnderTest().filterExpression(CURRENT_USER, filterPreferences,
                notificationPreference));
    }

    @Test
    public void generateFilterExpression() throws Exception
    {
        // Preferences
        Collection<NotificationFilterPreference> filterPreferences = mockPreferences();

        // Test
        ExpressionNode node = mocker.getComponentUnderTest().filterExpression(CURRENT_USER, filterPreferences,
                NotificationFilterType.EXCLUSIVE, NotificationFormat.ALERT);

        // Verify
        assertNotNull(node);
        assertEquals("NOT (USER IN (\"userA\", \"userB\", \"userC\"))", node.toString());
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals("eventUserNotificationFilter", mocker.getComponentUnderTest().getName());
    }
}
