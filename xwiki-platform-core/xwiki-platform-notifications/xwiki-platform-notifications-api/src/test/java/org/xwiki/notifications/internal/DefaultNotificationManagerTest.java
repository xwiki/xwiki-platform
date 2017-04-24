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
package org.xwiki.notifications.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentList(SimilarityCalculator.class)
public class DefaultNotificationManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultNotificationManager> mocker =
            new MockitoComponentMockingRule<>(DefaultNotificationManager.class);

    private EventStream eventStream;
    private QueryManager queryManager;
    private DocumentAccessBridge documentAccessBridge;
    private DocumentReferenceResolver<String> documentReferenceResolver;
    private ModelBridge modelBridge;
    private EntityReferenceSerializer<String> serializer;
    private ConfigurationSource userPreferencesSource;
    private AuthorizationManager authorizationManager;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");
    private Query query;
    private Date startDate;

    @Before
    public void setUp() throws Exception
    {
        eventStream = mocker.getInstance(EventStream.class);
        queryManager = mocker.getInstance(QueryManager.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        modelBridge = mocker.getInstance(ModelBridge.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        userPreferencesSource = mocker.getInstance(ConfigurationSource.class, "user");
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        startDate = new Date(10);

        when(documentReferenceResolver.resolve("xwiki:XWiki.UserA")).thenReturn(userReference);
        query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);

        when(modelBridge.getUserStartDate(userReference)).thenReturn(startDate);
        when(serializer.serialize(userReference)).thenReturn("xwiki:XWiki.UserA");

        NotificationPreference pref1 = new NotificationPreference("create", null, true);
        when(modelBridge.getNotificationsPreferences(userReference)).thenReturn(Arrays.asList(pref1));

        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(0);
    }

    @Test
    public void getEventsWith2Queries() throws Exception
    {
        // Mocks
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);
        Event event4 = mock(Event.class);
        Event event5 = mock(Event.class);
        Event event6 = mock(Event.class);

        DocumentReference doc1 = new DocumentReference("xwiki", "Main", "WebHome");
        when(event1.getDocument()).thenReturn(doc1);
        DocumentReference doc2 = new DocumentReference("xwiki", "PrivateSpace", "WebHome");
        when(event2.getDocument()).thenReturn(doc2);
        when(event3.getDocument()).thenReturn(doc2);
        when(event4.getDocument()).thenReturn(doc2);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc1)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc2)).thenReturn(false);

        when(event1.getType()).thenReturn("type1");
        when(event2.getType()).thenReturn("type2");
        when(event3.getType()).thenReturn("type3");
        when(event4.getType()).thenReturn("type4");
        when(event5.getType()).thenReturn("type5");
        when(event6.getType()).thenReturn("type6");

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3, event4),
                Arrays.asList(event5, event6));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        assertEquals(2, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
        assertEquals(event5, results.get(1).getEvents().get(0));
    }

    @Test
    public void getEventsWhenNoPreferences() throws Exception
    {
        NotificationPreference pref1 = new NotificationPreference("create", null, false);
        when(modelBridge.getNotificationsPreferences(userReference)).thenReturn(Arrays.asList(pref1));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        assertEquals(0, results.size());
    }

    @Test
    public void getEventsWhenException() throws Exception
    {
        // Mocks
        NotificationException exception = new NotificationException("Error");
        when(modelBridge.getNotificationsPreferences(userReference)).thenThrow(exception);

        // Test
        NotificationException caughtException = null;
        try {
            mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);
        } catch (NotificationException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Fail to get the list of notifications.", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

    @Test
    public void getEventsCount() throws Exception
    {
        // Mocks
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);

        when(eventStream.searchEvents(query)).thenReturn(
                Arrays.asList(event1, event2, event1, event2, event2, event2, event1, event2, event2, event2),
                Arrays.asList(event1, event2, event2, event1, event3));

        // Test
        long result = mocker.getComponentUnderTest().getEventsCount("xwiki:XWiki.UserA", true, 5);

        // Verify
        assertEquals(5, result);
        verifyZeroInteractions(event3);
    }

    @Test
    public void getEventsUC1() throws Exception
    {
        // Facts:
        // * Alice updates the page "Bike"
        // * Bob updates the page "Bike"

        // Expected:
        // * Alice and Bob have updated the page "Bike"

        // Comment:
        // Note: the 2 events have been combined

        // Mocks
        Event eventAlice = mock(Event.class);
        Event eventBob = mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(eventAlice.getDocument()).thenReturn(doc);
        when(eventBob.getDocument()).thenReturn(doc);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);


        when(eventAlice.getType()).thenReturn("update");
        when(eventBob.getType()).thenReturn("update");


        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(eventAlice, eventBob));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        assertEquals(1, results.size());
        assertEquals(eventAlice, results.get(0).getEvents().get(0));
        assertEquals(eventBob, results.get(0).getEvents().get(1));
    }

    @Test
    public void getEventsUC2() throws Exception
    {
        // Facts:
        // * Bob comments the page "Bike" (which actually update the page too)

        // Expected:
        // * Bob has commented the page "Bike"

        // Comment: we do not mention that Bob has updated the page "Bike", because it's actually a technical
        // implementation of the "comment" feature.

        // Mocks
        Event eventComment = mock(Event.class);
        Event eventUpdate = mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(eventComment.getDocument()).thenReturn(doc);
        when(eventUpdate.getDocument()).thenReturn(doc);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        when(eventComment.getType()).thenReturn("addComment");
        when(eventUpdate.getType()).thenReturn("update");

        when(eventComment.getGroupId()).thenReturn("g1");
        when(eventUpdate.getGroupId()).thenReturn("g1");

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(eventComment, eventUpdate));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        assertEquals(1, results.size());
        assertEquals(eventComment, results.get(0).getEvents().get(0));
        assertEquals(eventUpdate, results.get(0).getEvents().get(1));
    }

    @Test
    public void getEventsUC3() throws Exception
    {
        // Facts:
        // * Alice updates the page "Bike"
        // * Bob comments the page "Bike"

        // Expected:
        // * Alice has updated the page "Bike"
        // * Bob has commented the page "Bike"

        // Comment: same as UC2 but we make sure we don't lose the event concerning Alice

        // Note: the UC4 described in https://jira.xwiki.org/browse/XWIKI-14114 is actually similar to that one
        // because we don't care of the event' user in our tests.

        // Mocks
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(event1.getDocument()).thenReturn(doc);
        when(event2.getDocument()).thenReturn(doc);
        when(event3.getDocument()).thenReturn(doc);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        when(event1.getType()).thenReturn("update");
        when(event2.getType()).thenReturn("addComment");
        when(event3.getType()).thenReturn("update");

        when(event1.getGroupId()).thenReturn("g1");
        when(event2.getGroupId()).thenReturn("g2");
        when(event3.getGroupId()).thenReturn("g2");

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 5);

        // Verify
        assertEquals(2, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
        assertEquals(event2, results.get(1).getEvents().get(0));
        assertEquals(event3, results.get(1).getEvents().get(1));
    }

    @Test
    public void getEventsUC5() throws Exception
    {
        // Facts:
        // * Bob updates the page "Bike"
        // * Then Bob updates the page "Bike" again

        // Expected:
        // * Bob has updated the page "Bike"

        // Comment: we don't show 2 events, only one is interesting

        // Mocks
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(event1.getDocument()).thenReturn(doc);
        when(event2.getDocument()).thenReturn(doc);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        when(event1.getType()).thenReturn("update");
        when(event2.getType()).thenReturn("update");

        when(event1.getGroupId()).thenReturn("g1");
        when(event2.getGroupId()).thenReturn("g2");

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 5);

        // Verify
        assertEquals(1, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
        assertEquals(event2, results.get(0).getEvents().get(1));
    }

    @Test
    public void getEventsUC6() throws Exception
    {
        // Facts:
        // * Bob updates the page "Bike" (E1)
        // * Alice updates the page "Bike" (E2)
        // * Bob comments the page "Bike" (E3 & E4)
        // * Carol comments the page "Bike" (E5 & E6)
        // * Dave comments the page "Guitar" (E7 & E8)

        // Expected:
        // * Bob and Alice have updated the page "Bike"
        // * Bob and Carol have commented the page "Bike"
        // * Dave has commented the page "Guitar"

        // Comment: it's only a mix of other use cases to make sure we have the expected results.

        // Mocks
        Event event1 = mock(Event.class); when(event1.toString()).thenReturn("event1");
        Event event2 = mock(Event.class); when(event2.toString()).thenReturn("event2");
        Event event3 = mock(Event.class); when(event3.toString()).thenReturn("event3");
        Event event4 = mock(Event.class); when(event4.toString()).thenReturn("event4");
        Event event5 = mock(Event.class); when(event5.toString()).thenReturn("event5");
        Event event6 = mock(Event.class); when(event6.toString()).thenReturn("event6");
        Event event7 = mock(Event.class); when(event7.toString()).thenReturn("event7");
        Event event8 = mock(Event.class); when(event8.toString()).thenReturn("event8");

        DocumentReference doc1 = new DocumentReference("xwiki", "Main", "Bike");
        when(event1.getDocument()).thenReturn(doc1);
        when(event2.getDocument()).thenReturn(doc1);
        when(event3.getDocument()).thenReturn(doc1);
        when(event4.getDocument()).thenReturn(doc1);
        when(event5.getDocument()).thenReturn(doc1);
        when(event6.getDocument()).thenReturn(doc1);
        DocumentReference doc2 = new DocumentReference("xwiki", "Main", "Guitar");
        when(event7.getDocument()).thenReturn(doc2);
        when(event8.getDocument()).thenReturn(doc2);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc1)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc2)).thenReturn(true);

        when(event1.getType()).thenReturn("update");
        when(event2.getType()).thenReturn("update");
        when(event3.getType()).thenReturn("addComment");
        when(event4.getType()).thenReturn("update");
        when(event5.getType()).thenReturn("update");
        when(event6.getType()).thenReturn("addComment");
        when(event7.getType()).thenReturn("update");
        when(event8.getType()).thenReturn("addComment");

        when(event1.getGroupId()).thenReturn("g1");
        when(event2.getGroupId()).thenReturn("g2");
        when(event3.getGroupId()).thenReturn("g3");
        when(event4.getGroupId()).thenReturn("g3");
        when(event5.getGroupId()).thenReturn("g5");
        when(event6.getGroupId()).thenReturn("g5");
        when(event7.getGroupId()).thenReturn("g7");
        when(event8.getGroupId()).thenReturn("g7");

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3, event4, event5, event6,
                event7, event8));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 50);

        // Verify
        assertEquals(3, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
        assertEquals(event2, results.get(0).getEvents().get(1));
        assertEquals(event3, results.get(1).getEvents().get(0));
        assertEquals(event4, results.get(1).getEvents().get(1));
        assertEquals(event5, results.get(1).getEvents().get(2));
        assertEquals(event6, results.get(1).getEvents().get(3));
        assertEquals(event7, results.get(2).getEvents().get(0));
        assertEquals(event8, results.get(2).getEvents().get(1));
    }

    @Test
    public void generateQuery() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWhenHiddenDocsAreEnabled() throws Exception
    {
        // Mock
        when(userPreferencesSource.getProperty("displayHiddenDocuments", 0)).thenReturn(1);

        // Test
        mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWithNotOnlyUnread() throws Exception
    {
        // Test
        mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", false, 2);

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.hidden <> true " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
    }

    @Test
    public void generateQueryWithUntilDate() throws Exception
    {
        Date untilDate = new Date();

        // Test
        mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2, untilDate,
                Collections.emptyList());

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
        verify(query).bindValue("endDate", untilDate);
    }

    @Test
    public void generateQueryWithUntilDateAndBlackList() throws Exception
    {
        Date untilDate = new Date();

        // Test
        mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2, untilDate,
                Arrays.asList("event1", "event2"));

        // Verify
        verify(queryManager).createQuery(
                "where event.date >= :startDate AND event.user <> :user AND (event.type IN (:types))" +
                        " AND event.id NOT IN (:blackList) AND event.date <= :endDate AND event.hidden <> true AND " +
                        "(event not in (select status.activityEvent from ActivityEventStatusImpl status " +
                        "where status.activityEvent = event and status.entityId = :user and status.read = true)) " +
                        "order by event.date DESC", Query.HQL);
        verify(query).bindValue("user", "xwiki:XWiki.UserA");
        verify(query).bindValue(eq("types"), eq(Arrays.asList("create")));
        verify(query).bindValue("startDate", startDate);
        verify(query).bindValue("endDate", untilDate);
        verify(query).bindValue("blackList", Arrays.asList("event1", "event2"));
    }
}

