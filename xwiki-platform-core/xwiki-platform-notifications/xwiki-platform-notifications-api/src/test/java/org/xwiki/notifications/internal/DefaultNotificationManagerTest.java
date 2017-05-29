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
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.query.Query;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
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
    private QueryGenerator queryGenerator;
    private DocumentAccessBridge documentAccessBridge;
    private DocumentReferenceResolver<String> documentReferenceResolver;
    private ModelBridge modelBridge;
    private AuthorizationManager authorizationManager;

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "UserA");
    private Query query;
    private Date startDate;

    @Before
    public void setUp() throws Exception
    {
        eventStream = mocker.getInstance(EventStream.class);
        queryGenerator = mocker.getInstance(QueryGenerator.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        modelBridge = mocker.getInstance(ModelBridge.class, "cached");
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        startDate = new Date(10);

        when(documentReferenceResolver.resolve("xwiki:XWiki.UserA")).thenReturn(userReference);
        query = mock(Query.class);
        when(queryGenerator.generateQuery(any(DocumentReference.class), anyBoolean(), nullable(Date.class), nullable(List.class)))
                .thenReturn(query);


        when(modelBridge.getUserStartDate(userReference)).thenReturn(startDate);

        NotificationPreference pref1 = new NotificationPreference("create", null, true);
        when(modelBridge.getNotificationsPreferences(userReference)).thenReturn(Arrays.asList(pref1));
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
        when(queryGenerator.generateQuery(eq(userReference), eq(true), isNull(), any(List.class))).thenThrow(exception);

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
        // * Bob adds an annotation on page "Bike" (E9 & E10)
        // * Alice adds an annotation on page "Bike" (E11 & E12)
        // * Alice adds an other annotation on page "Bike" (E12 & E13)


        // Expected:
        // * Bob and Alice have updated the page "Bike"
        // * Bob and Carol have commented the page "Bike"
        // * Dave has commented the page "Guitar"
        // * Bob and Alice have annotated the page "Bike"

        // Comment: it's only a mix of other use cases to make sure we have the expected results.

        // Mocks
        DocumentReference doc1 = new DocumentReference("xwiki", "Main", "Bike");
        DocumentReference doc2 = new DocumentReference("xwiki", "Main", "Guitar");
        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc1)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc2)).thenReturn(true);

        // * Bob updates the page "Bike" (E1)
        Event event1 = mock(Event.class); when(event1.toString()).thenReturn("event1");
        when(event1.getDocument()).thenReturn(doc1);
        when(event1.getType()).thenReturn("update");
        when(event1.getGroupId()).thenReturn("g1");

        // * Alice updates the page "Bike" (E2)
        Event event2 = mock(Event.class); when(event2.toString()).thenReturn("event2");
        when(event2.getDocument()).thenReturn(doc1);
        when(event2.getType()).thenReturn("update");
        when(event2.getGroupId()).thenReturn("g2");

        // * Bob comments the page "Bike" (E3 & E4)
        Event event3 = mock(Event.class); when(event3.toString()).thenReturn("event3");
        when(event3.getDocument()).thenReturn(doc1);
        when(event3.getType()).thenReturn("addComment");
        when(event3.getGroupId()).thenReturn("g3");
        Event event4 = mock(Event.class); when(event4.toString()).thenReturn("event4");
        when(event4.getDocument()).thenReturn(doc1);
        when(event4.getType()).thenReturn("update");
        when(event4.getGroupId()).thenReturn("g3");

        // * Carol comments the page "Bike" (E5 & E6)
        // (note: we put the "update" event before the "addComment", because we can not guarantee the order so
        // it's good to test both)
        Event event5 = mock(Event.class); when(event5.toString()).thenReturn("event5");
        when(event5.getDocument()).thenReturn(doc1);
        when(event5.getType()).thenReturn("update");
        when(event5.getGroupId()).thenReturn("g5");
        Event event6 = mock(Event.class); when(event6.toString()).thenReturn("event6");
        when(event6.getDocument()).thenReturn(doc1);
        when(event6.getType()).thenReturn("addComment");
        when(event6.getGroupId()).thenReturn("g5");

        // * Dave comments the page "Guitar" (E7 & E8)
        Event event7 = mock(Event.class); when(event7.toString()).thenReturn("event7");
        when(event7.getDocument()).thenReturn(doc2);
        when(event7.getType()).thenReturn("update");
        when(event7.getGroupId()).thenReturn("g7");
        Event event8 = mock(Event.class); when(event8.toString()).thenReturn("event8");
        when(event8.getDocument()).thenReturn(doc2);
        when(event8.getType()).thenReturn("addComment");
        when(event8.getGroupId()).thenReturn("g7");

        // * Bob adds an annotation on page "Bike" (E9 & E10)
        Event event9 = mock(Event.class); when(event8.toString()).thenReturn("event9");
        when(event9.getDocument()).thenReturn(doc1);
        when(event9.getType()).thenReturn("update");
        when(event9.getGroupId()).thenReturn("g9");
        Event event10 = mock(Event.class); when(event8.toString()).thenReturn("event10");
        when(event10.getDocument()).thenReturn(doc1);
        when(event10.getType()).thenReturn("addAnnotation");
        when(event10.getGroupId()).thenReturn("g9");

        // * Alice adds an annotation on page "Bike" (E11 & E12)
        Event event11 = mock(Event.class); when(event8.toString()).thenReturn("event11");
        when(event11.getDocument()).thenReturn(doc1);
        when(event11.getType()).thenReturn("update");
        when(event11.getGroupId()).thenReturn("g11");
        Event event12 = mock(Event.class); when(event8.toString()).thenReturn("event12");
        when(event12.getDocument()).thenReturn(doc1);
        when(event12.getType()).thenReturn("addAnnotation");
        when(event12.getGroupId()).thenReturn("g11");

        // * Alice adds an other annotation on page "Bike" (E12 & E13)
        Event event13 = mock(Event.class); when(event8.toString()).thenReturn("event11");
        when(event13.getDocument()).thenReturn(doc1);
        when(event13.getType()).thenReturn("addAnnotation");
        when(event13.getGroupId()).thenReturn("g13");
        Event event14 = mock(Event.class); when(event8.toString()).thenReturn("event12");
        when(event14.getDocument()).thenReturn(doc1);
        when(event14.getType()).thenReturn("update");
        when(event14.getGroupId()).thenReturn("g13");

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3, event4, event5, event6,
                event7, event8, event9, event10, event11, event12, event13, event14));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 50);

        // Verify
        assertEquals(4, results.size());

        // * Bob and Alice have updated the page "Bike"
        assertTrue(results.get(0).getEvents().contains(event1));
        assertTrue(results.get(0).getEvents().contains(event2));

        // * Bob and Carol have commented the page "Bike"
        assertTrue(results.get(1).getEvents().contains(event3));
        assertTrue(results.get(1).getEvents().contains(event4));
        assertTrue(results.get(1).getEvents().contains(event5));
        assertTrue(results.get(1).getEvents().contains(event6));

        // * Dave has commented the page "Guitar"
        assertTrue(results.get(2).getEvents().contains(event7));
        assertTrue(results.get(2).getEvents().contains(event8));

        // * Bob and Alice have annotated the page "Bike"
        assertTrue(results.get(3).getEvents().contains(event9));
        assertTrue(results.get(3).getEvents().contains(event10));
        assertTrue(results.get(3).getEvents().contains(event11));
        assertTrue(results.get(3).getEvents().contains(event12));
        assertTrue(results.get(3).getEvents().contains(event13));
        assertTrue(results.get(3).getEvents().contains(event14));
    }

    @Test
    public void getEvents1Update2Events() throws Exception
    {
        // Facts:
        // * Bob comment and annotate the page "Bike" in the same time

        // Expected:
        // * Bob has commented the page "Bike"
        // * Bob has annotated the page "Bike"

        // Mocks
        Event event1 = mock(Event.class); when(event1.toString()).thenReturn("event1");
        Event event2 = mock(Event.class); when(event1.toString()).thenReturn("event2");
        Event event3 = mock(Event.class); when(event1.toString()).thenReturn("event3");

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(event1.getDocument()).thenReturn(doc);
        when(event2.getDocument()).thenReturn(doc);
        when(event3.getDocument()).thenReturn(doc);

        when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        when(event1.getType()).thenReturn("update");
        when(event2.getType()).thenReturn("addComment");
        when(event3.getType()).thenReturn("addAnnotation");

        when(event1.getGroupId()).thenReturn("g1");
        when(event2.getGroupId()).thenReturn("g1");
        when(event3.getGroupId()).thenReturn("g1");

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 50);

        // Verify
        assertEquals(1, results.size());
        assertTrue(results.get(0).getEvents().contains(event1));
        assertTrue(results.get(0).getEvents().contains(event2));
        assertTrue(results.get(0).getEvents().contains(event3));
    }
}