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
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventManager;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.query.QueryException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.group.GroupManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultParametrizedNotificationManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultParametrizedNotificationManagerTest
{
    @InjectMockComponents
    private DefaultParametrizedNotificationManager defaultParametrizedNotificationManager;

    @MockComponent
    private RecordableEventDescriptorHelper recordableEventDescriptorHelper;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    private EventSearcher eventSearcher;

    @MockComponent
    private GroupingEventManager groupingEventManager;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");

    @BeforeEach
    public void setUp() throws Exception
    {
        NotificationPreference pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(pref1.isNotificationEnabled()).thenReturn(true);

        when(recordableEventDescriptorHelper.hasDescriptor(anyString(), any(DocumentReference.class))).thenReturn(true);

        // We consider a grouping algorithm, where each event creates a composite.
        doAnswer(invocationOnMock -> {
            List<CompositeEvent> compositeEvents = invocationOnMock.getArgument(0);
            List<Event> events = invocationOnMock.getArgument(1);
            for (Event event : events) {
                boolean added = false;
                for (CompositeEvent compositeEvent : compositeEvents) {
                    if (compositeEvent.getEvents().contains(event)) {
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    CompositeEvent compositeEvent = new CompositeEvent(event);
                    compositeEvents.add(compositeEvent);
                }
            }
            return null;
        }).when(this.groupingEventManager).augmentCompositeEvents(any(), any(), any(), eq("alert"));
    }

    private Event createMockedEvent()
    {
        Event event = mock(Event.class);
        when(event.getDate()).thenReturn(new Date(1L));
        return event;
    }

    @Test
    void getEventsWhenNoPreferences() throws Exception
    {
        NotificationPreference pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(pref1.isNotificationEnabled()).thenReturn(false);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = new DocumentReference("xwiki", "XWiki", "UserA");
        parameters.expectedCount = 2;
        parameters.preferences = Arrays.asList(pref1);
        List<CompositeEvent> results = this.defaultParametrizedNotificationManager.getEvents(parameters);

        // Verify
        assertEquals(0, results.size());
    }

    @Test
    void getEventsWith2Queries() throws Exception
    {
        // Mocks
        Event event1 = createMockedEvent();
        Event event2 = createMockedEvent();
        Event event3 = createMockedEvent();
        Event event4 = createMockedEvent();
        Event event5 = createMockedEvent();
        Event event6 = createMockedEvent();

        DocumentReference doc1 = new DocumentReference("xwiki", "Main", "WebHome");
        when(event1.getDocument()).thenReturn(doc1);
        DocumentReference doc2 = new DocumentReference("xwiki", "PrivateSpace", "WebHome");
        when(event2.getDocument()).thenReturn(doc2);
        when(event3.getDocument()).thenReturn(doc2);
        when(event4.getDocument()).thenReturn(doc2);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc1)).thenReturn(true);
        when(contextualAuthorizationManager.hasAccess(Right.VIEW, doc1)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc2)).thenReturn(false);

        when(event1.getType()).thenReturn("type1");
        when(event2.getType()).thenReturn("type2");
        when(event3.getType()).thenReturn("type3");
        when(event4.getType()).thenReturn("type4");
        when(event5.getType()).thenReturn("type5");
        when(event6.getType()).thenReturn("type6");

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = new DocumentReference("xwiki", "XWiki", "UserA");
        parameters.expectedCount = 2;

        when(this.eventSearcher.searchEvents(0, 4, parameters)).thenReturn(List.of(
            event1,
            event2,
            event3,
            event4,
            event5,
            event6
        ));
        List<CompositeEvent> results = this.defaultParametrizedNotificationManager.getEvents(parameters);

        // Verify
        assertEquals(2, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
        assertEquals(event5, results.get(1).getEvents().get(0));
    }

    @Test
    void getEventsWhenException() throws Exception
    {
        // Mocks
        QueryException exception = new QueryException("Error", null, null);
        when(this.eventSearcher.searchEvents(anyInt(), anyInt(), any())).thenThrow(exception);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = new DocumentReference("xwiki", "XWiki", "UserA");
        parameters.expectedCount = 2;

        NotificationException notificationException = assertThrows(NotificationException.class,
            () -> this.defaultParametrizedNotificationManager.getEvents(parameters));

        assertEquals("Fail to get the list of notifications.", notificationException.getMessage());
        assertEquals(exception, notificationException.getCause());
    }

    @Test
    void getEventsCount() throws Exception
    {
        // Mocks
        Event event1 = createMockedEvent();
        Event event2 = createMockedEvent();
        Event event3 = createMockedEvent();
        Event event4 = createMockedEvent();
        Event event5 = createMockedEvent();
        Event event6 = createMockedEvent();

        when(recordableEventDescriptorHelper.hasDescriptor(isNull(), any(DocumentReference.class))).thenReturn(true);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = new DocumentReference("xwiki", "XWiki", "UserA");
        parameters.expectedCount = 5;

        when(this.eventSearcher.searchEvents(anyInt(), anyInt(), any()))
            .thenReturn(
                List.of(event1, event2, event1, event4, event2, event4, event1, event2, event2, event3))
            .thenReturn(List.of(event5, event2, event6, event1, event3));

        long result = this.defaultParametrizedNotificationManager.getEvents(parameters).size();

        // Verify
        assertEquals(5, result);
        verifyNoInteractions(event6);
    }

    private Event createMockedEvent(String type, DocumentReference user, DocumentReference doc, Date date,
        String groupId)
    {
        Event event = mock(Event.class);
        when(event.getDate()).thenReturn(date);
        when(event.getDocument()).thenReturn(doc);
        when(event.getUser()).thenReturn(user);
        when(event.getType()).thenReturn(type);
        when(event.getGroupId()).thenReturn(groupId);

        when(event.toString()).thenReturn(String.format("[%s] Event [%s] on document [%s] by [%s] on [%s]", groupId,
            type, doc, user, date.toString()));

        return event;
    }

    @Test
    void getEventsXWIKI15151() throws Exception
    {
        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");

        // Example taken from a real case
        Event event1 = createMockedEvent("update", userA, userA, new Date(1510567729000L), "id1");
        Event event2 = createMockedEvent("update", userA, userA, new Date(1510567729000L), "id2");

        when(authorizationManager.hasAccess(eq(Right.VIEW), eq(userReference), any(DocumentReference.class)))
            .thenReturn(true);
        when(contextualAuthorizationManager.hasAccess(eq(Right.VIEW), any(DocumentReference.class))).thenReturn(true);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userA;
        parameters.expectedCount = 5;
        parameters.format = NotificationFormat.ALERT;

        when(this.eventSearcher.searchEvents(0, 10, parameters)).thenReturn(List.of(
            event1,
            event2
        ));

        // Filter 1
        NotificationFilter filter1 = mock(NotificationFilter.class);
        when(filter1.getPriority()).thenReturn(1);
        when(filter1.filterEvent(eq(event1), any(DocumentReference.class), anyCollection(),
            any(NotificationFormat.class))).thenReturn(NotificationFilter.FilterPolicy.FILTER);
        when(filter1.filterEvent(eq(event2), any(DocumentReference.class), anyCollection(),
            any(NotificationFormat.class))).thenReturn(NotificationFilter.FilterPolicy.KEEP);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        when(filter2.getPriority()).thenReturn(2);
        when(filter2.filterEvent(eq(event1), any(DocumentReference.class), anyCollection(),
            any(NotificationFormat.class))).thenReturn(NotificationFilter.FilterPolicy.KEEP);
        when(filter2.filterEvent(eq(event2), any(DocumentReference.class), anyCollection(),
            any(NotificationFormat.class))).thenReturn(NotificationFilter.FilterPolicy.FILTER);
        parameters.filters = Arrays.asList(filter1, filter2);
        when(filter1.compareTo(filter2)).thenReturn(1);
        when(filter2.compareTo(filter1)).thenReturn(-1);

        List<CompositeEvent> results = this.defaultParametrizedNotificationManager.getEvents(parameters);

        assertEquals(1, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
    }

    @Test
    void getEventsThatHaveNoDescriptor() throws Exception
    {
        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");

        // Example taken from a real case
        Event event1 = createMockedEvent("customThing", userA, userA, new Date(1510567729000L), "id1");
        Event event2 = createMockedEvent("update", userA, userA, new Date(1510567729000L), "id2");

        when(authorizationManager.hasAccess(eq(Right.VIEW), eq(userReference), any(DocumentReference.class)))
            .thenReturn(true);
        when(contextualAuthorizationManager.hasAccess(eq(Right.VIEW), any(DocumentReference.class))).thenReturn(true);

        when(recordableEventDescriptorHelper.hasDescriptor(eq("customThing"), eq(userA))).thenReturn(false);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = userA;
        parameters.expectedCount = 5;
        parameters.format = NotificationFormat.ALERT;

        when(this.eventSearcher.searchEvents(0, 10, parameters)).thenReturn(List.of(
            event1,
            event2
        ));

        List<CompositeEvent> results = this.defaultParametrizedNotificationManager.getEvents(parameters);

        assertEquals(1, results.size());
        assertEquals(event2, results.get(0).getEvents().get(0));
    }

    @Test
    void getEventsWhenCurrentUserNotAuthorized() throws Exception
    {
        Event event = createMockedEvent();

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(event.getDocument()).thenReturn(doc);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);
        when(contextualAuthorizationManager.hasAccess(Right.VIEW, doc)).thenReturn(false);

        when(event.getType()).thenReturn("update");

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = this.userReference;
        parameters.expectedCount = 1;
        when(this.eventSearcher.searchEvents(0, 2, parameters)).thenReturn(List.of(
            event
        ));

        List<CompositeEvent> results = this.defaultParametrizedNotificationManager.getEvents(parameters);

        // Verify
        assertEquals(0, results.size());
        verify(contextualAuthorizationManager).hasAccess(Right.VIEW, doc);
    }

    @Test
    void getEventsFilterOnTarget() throws Exception
    {
        Event event = createMockedEvent();
        when(event.getType()).thenReturn("update");
        when(event.getTarget()).thenReturn(new HashSet<>(Arrays.asList("Foo.bar")));

        when(authorizationManager.hasAccess(eq(Right.VIEW), eq(userReference), any())).thenReturn(true);
        when(contextualAuthorizationManager.hasAccess(eq(Right.VIEW), any())).thenReturn(true);
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = this.userReference;
        parameters.expectedCount = 1;
        when(serializer.serialize(this.userReference)).thenReturn("XWiki.UserA");
        when(this.eventSearcher.searchEvents(0, 2, parameters)).thenReturn(List.of(
            event
        ));

        // the current user is not targeted by the event: we don't get any result.
        // We also check that we tried to look in the groups.
        List<CompositeEvent> results = this.defaultParametrizedNotificationManager.getEvents(parameters);
        assertTrue(results.isEmpty());
        verify(this.groupManager, times(1)).getGroups(this.userReference, null, true);

        // the current user is targeted explicitely by the event: we get a result.
        // We also check that we don't perform any check in groups in that case
        when(event.getTarget()).thenReturn(new HashSet<>(Arrays.asList("Foo.bar", "XWiki.UserA")));
        results = this.defaultParametrizedNotificationManager.getEvents(parameters);
        assertEquals(1, results.size());
        verify(this.groupManager, times(1)).getGroups(this.userReference, null, true);

        DocumentReference groupReference = mock(DocumentReference.class);
        when(this.groupManager.getGroups(this.userReference, null, true)).thenReturn(Arrays.asList(groupReference));
        when(this.serializer.serialize(groupReference)).thenReturn("Foo.bar");

        // the current user is targeted by the event, but this time through a group: we get a result.
        // We also check that we did perform a new check in groups
        when(event.getTarget()).thenReturn(new HashSet<>(Arrays.asList("Foo.bar")));
        results = this.defaultParametrizedNotificationManager.getEvents(parameters);
        assertEquals(1, results.size());
        verify(this.groupManager, times(2)).getGroups(this.userReference, null, true);

        // the current user is targeted explicitely by the event, but also through a group: we should still get a single
        // result
        // We also check that we don't perform any supplementary check in groups in that case
        when(event.getTarget()).thenReturn(new HashSet<>(Arrays.asList("Foo.bar", "XWiki.UserA")));
        results = this.defaultParametrizedNotificationManager.getEvents(parameters);
        assertEquals(1, results.size());
        verify(this.groupManager, times(2)).getGroups(this.userReference, null, true);
    }
}
