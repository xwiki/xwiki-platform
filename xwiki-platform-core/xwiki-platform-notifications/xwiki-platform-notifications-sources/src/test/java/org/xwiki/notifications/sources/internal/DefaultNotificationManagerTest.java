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
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.internal.SimilarityCalculator;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.query.Query;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

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
    private NotificationPreferenceManager notificationPreferenceManager;
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
        notificationPreferenceManager = mocker.getInstance(NotificationPreferenceManager.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        startDate = new Date(10);

        Mockito.when(documentReferenceResolver.resolve("xwiki:XWiki.UserA")).thenReturn(userReference);
        query = Mockito.mock(Query.class);
        Mockito.when(queryGenerator.generateQuery(ArgumentMatchers.any(DocumentReference.class), ArgumentMatchers.any(NotificationFormat.class),
                ArgumentMatchers.anyBoolean(), ArgumentMatchers.nullable(Date.class),
                ArgumentMatchers.nullable(Date.class), ArgumentMatchers.nullable(List.class))).thenReturn(query);

        NotificationPreference pref1 = new NotificationPreference("create", true);
        Mockito.when(notificationPreferenceManager.getNotificationsPreferences(userReference)).thenReturn(Arrays.asList(pref1));
    }

    @Test
    public void getEventsWith2Queries() throws Exception
    {
        // Mocks
        Event event1 = Mockito.mock(Event.class);
        Event event2 = Mockito.mock(Event.class);
        Event event3 = Mockito.mock(Event.class);
        Event event4 = Mockito.mock(Event.class);
        Event event5 = Mockito.mock(Event.class);
        Event event6 = Mockito.mock(Event.class);

        DocumentReference doc1 = new DocumentReference("xwiki", "Main", "WebHome");
        Mockito.when(event1.getDocument()).thenReturn(doc1);
        DocumentReference doc2 = new DocumentReference("xwiki", "PrivateSpace", "WebHome");
        Mockito.when(event2.getDocument()).thenReturn(doc2);
        Mockito.when(event3.getDocument()).thenReturn(doc2);
        Mockito.when(event4.getDocument()).thenReturn(doc2);

        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc1)).thenReturn(true);
        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc2)).thenReturn(false);

        Mockito.when(event1.getType()).thenReturn("type1");
        Mockito.when(event2.getType()).thenReturn("type2");
        Mockito.when(event3.getType()).thenReturn("type3");
        Mockito.when(event4.getType()).thenReturn("type4");
        Mockito.when(event5.getType()).thenReturn("type5");
        Mockito.when(event6.getType()).thenReturn("type6");

        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3, event4),
                Arrays.asList(event5, event6));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(event1, results.get(0).getEvents().get(0));
        Assert.assertEquals(event5, results.get(1).getEvents().get(0));
    }

    @Test
    public void getEventsWhenNoPreferences() throws Exception
    {
        NotificationPreference pref1 = new NotificationPreference("create", false);
        Mockito.when(notificationPreferenceManager.getNotificationsPreferences(userReference)).thenReturn(Arrays.asList(pref1));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void getEventsWhenException() throws Exception
    {
        // Mocks
        NotificationException exception = new NotificationException("Error");
        Mockito.when(queryGenerator.generateQuery(ArgumentMatchers.eq(userReference), ArgumentMatchers.any(NotificationFormat.class), ArgumentMatchers
                        .eq(true), ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(), ArgumentMatchers.any(List.class))).thenThrow(exception);

        // Test
        NotificationException caughtException = null;
        try {
            mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);
        } catch (NotificationException e) {
            caughtException = e;
        }

        // Verify
        Assert.assertNotNull(caughtException);
        Assert.assertEquals("Fail to get the list of notifications.", caughtException.getMessage());
        Assert.assertEquals(exception, caughtException.getCause());
    }

    @Test
    public void getEventsCount() throws Exception
    {
        // Mocks
        Event event1 = Mockito.mock(Event.class);
        Event event2 = Mockito.mock(Event.class);
        Event event3 = Mockito.mock(Event.class);

        Mockito.when(eventStream.searchEvents(query)).thenReturn(
                Arrays.asList(event1, event2, event1, event2, event2, event2, event1, event2, event2, event2),
                Arrays.asList(event1, event2, event2, event1, event3));

        // Test
        long result = mocker.getComponentUnderTest().getEventsCount("xwiki:XWiki.UserA", true, 5);

        // Verify
        Assert.assertEquals(5, result);
        Mockito.verifyZeroInteractions(event3);
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
        Event eventAlice = Mockito.mock(Event.class);
        Event eventBob = Mockito.mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        Mockito.when(eventAlice.getDocument()).thenReturn(doc);
        Mockito.when(eventBob.getDocument()).thenReturn(doc);

        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);


        Mockito.when(eventAlice.getType()).thenReturn("update");
        Mockito.when(eventBob.getType()).thenReturn("update");


        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(eventAlice, eventBob));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(eventAlice, results.get(0).getEvents().get(0));
        Assert.assertEquals(eventBob, results.get(0).getEvents().get(1));
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
        Event eventComment = Mockito.mock(Event.class);
        Event eventUpdate = Mockito.mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        Mockito.when(eventComment.getDocument()).thenReturn(doc);
        Mockito.when(eventUpdate.getDocument()).thenReturn(doc);

        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        Mockito.when(eventComment.getType()).thenReturn("addComment");
        Mockito.when(eventUpdate.getType()).thenReturn("update");

        Mockito.when(eventComment.getGroupId()).thenReturn("g1");
        Mockito.when(eventUpdate.getGroupId()).thenReturn("g1");

        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(eventComment, eventUpdate));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 2);

        // Verify
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(eventComment, results.get(0).getEvents().get(0));
        Assert.assertEquals(eventUpdate, results.get(0).getEvents().get(1));
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
        Event event1 = Mockito.mock(Event.class);
        Event event2 = Mockito.mock(Event.class);
        Event event3 = Mockito.mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        Mockito.when(event1.getDocument()).thenReturn(doc);
        Mockito.when(event2.getDocument()).thenReturn(doc);
        Mockito.when(event3.getDocument()).thenReturn(doc);

        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        Mockito.when(event1.getType()).thenReturn("update");
        Mockito.when(event2.getType()).thenReturn("addComment");
        Mockito.when(event3.getType()).thenReturn("update");

        Mockito.when(event1.getGroupId()).thenReturn("g1");
        Mockito.when(event2.getGroupId()).thenReturn("g2");
        Mockito.when(event3.getGroupId()).thenReturn("g2");

        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 5);

        // Verify
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(event1, results.get(0).getEvents().get(0));
        Assert.assertEquals(event2, results.get(1).getEvents().get(0));
        Assert.assertEquals(event3, results.get(1).getEvents().get(1));
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
        Event event1 = Mockito.mock(Event.class);
        Event event2 = Mockito.mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        Mockito.when(event1.getDocument()).thenReturn(doc);
        Mockito.when(event2.getDocument()).thenReturn(doc);

        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        Mockito.when(event1.getType()).thenReturn("update");
        Mockito.when(event2.getType()).thenReturn("update");

        Mockito.when(event1.getGroupId()).thenReturn("g1");
        Mockito.when(event2.getGroupId()).thenReturn("g2");

        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 5);

        // Verify
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(event1, results.get(0).getEvents().get(0));
        Assert.assertEquals(event2, results.get(0).getEvents().get(1));
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
        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc1)).thenReturn(true);
        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc2)).thenReturn(true);

        // * Bob updates the page "Bike" (E1)
        Event event1 = Mockito.mock(Event.class); Mockito.when(event1.toString()).thenReturn("event1");
        Mockito.when(event1.getDocument()).thenReturn(doc1);
        Mockito.when(event1.getType()).thenReturn("update");
        Mockito.when(event1.getGroupId()).thenReturn("g1");

        // * Alice updates the page "Bike" (E2)
        Event event2 = Mockito.mock(Event.class); Mockito.when(event2.toString()).thenReturn("event2");
        Mockito.when(event2.getDocument()).thenReturn(doc1);
        Mockito.when(event2.getType()).thenReturn("update");
        Mockito.when(event2.getGroupId()).thenReturn("g2");

        // * Bob comments the page "Bike" (E3 & E4)
        Event event3 = Mockito.mock(Event.class); Mockito.when(event3.toString()).thenReturn("event3");
        Mockito.when(event3.getDocument()).thenReturn(doc1);
        Mockito.when(event3.getType()).thenReturn("addComment");
        Mockito.when(event3.getGroupId()).thenReturn("g3");
        Event event4 = Mockito.mock(Event.class); Mockito.when(event4.toString()).thenReturn("event4");
        Mockito.when(event4.getDocument()).thenReturn(doc1);
        Mockito.when(event4.getType()).thenReturn("update");
        Mockito.when(event4.getGroupId()).thenReturn("g3");

        // * Carol comments the page "Bike" (E5 & E6)
        // (note: we put the "update" event before the "addComment", because we can not guarantee the order so
        // it's good to test both)
        Event event5 = Mockito.mock(Event.class); Mockito.when(event5.toString()).thenReturn("event5");
        Mockito.when(event5.getDocument()).thenReturn(doc1);
        Mockito.when(event5.getType()).thenReturn("update");
        Mockito.when(event5.getGroupId()).thenReturn("g5");
        Event event6 = Mockito.mock(Event.class); Mockito.when(event6.toString()).thenReturn("event6");
        Mockito.when(event6.getDocument()).thenReturn(doc1);
        Mockito.when(event6.getType()).thenReturn("addComment");
        Mockito.when(event6.getGroupId()).thenReturn("g5");

        // * Dave comments the page "Guitar" (E7 & E8)
        Event event7 = Mockito.mock(Event.class); Mockito.when(event7.toString()).thenReturn("event7");
        Mockito.when(event7.getDocument()).thenReturn(doc2);
        Mockito.when(event7.getType()).thenReturn("update");
        Mockito.when(event7.getGroupId()).thenReturn("g7");
        Event event8 = Mockito.mock(Event.class); Mockito.when(event8.toString()).thenReturn("event8");
        Mockito.when(event8.getDocument()).thenReturn(doc2);
        Mockito.when(event8.getType()).thenReturn("addComment");
        Mockito.when(event8.getGroupId()).thenReturn("g7");

        // * Bob adds an annotation on page "Bike" (E9 & E10)
        Event event9 = Mockito.mock(Event.class); Mockito.when(event8.toString()).thenReturn("event9");
        Mockito.when(event9.getDocument()).thenReturn(doc1);
        Mockito.when(event9.getType()).thenReturn("update");
        Mockito.when(event9.getGroupId()).thenReturn("g9");
        Event event10 = Mockito.mock(Event.class); Mockito.when(event8.toString()).thenReturn("event10");
        Mockito.when(event10.getDocument()).thenReturn(doc1);
        Mockito.when(event10.getType()).thenReturn("addAnnotation");
        Mockito.when(event10.getGroupId()).thenReturn("g9");

        // * Alice adds an annotation on page "Bike" (E11 & E12)
        Event event11 = Mockito.mock(Event.class); Mockito.when(event8.toString()).thenReturn("event11");
        Mockito.when(event11.getDocument()).thenReturn(doc1);
        Mockito.when(event11.getType()).thenReturn("update");
        Mockito.when(event11.getGroupId()).thenReturn("g11");
        Event event12 = Mockito.mock(Event.class); Mockito.when(event8.toString()).thenReturn("event12");
        Mockito.when(event12.getDocument()).thenReturn(doc1);
        Mockito.when(event12.getType()).thenReturn("addAnnotation");
        Mockito.when(event12.getGroupId()).thenReturn("g11");

        // * Alice adds an other annotation on page "Bike" (E12 & E13)
        Event event13 = Mockito.mock(Event.class); Mockito.when(event8.toString()).thenReturn("event11");
        Mockito.when(event13.getDocument()).thenReturn(doc1);
        Mockito.when(event13.getType()).thenReturn("addAnnotation");
        Mockito.when(event13.getGroupId()).thenReturn("g13");
        Event event14 = Mockito.mock(Event.class); Mockito.when(event8.toString()).thenReturn("event12");
        Mockito.when(event14.getDocument()).thenReturn(doc1);
        Mockito.when(event14.getType()).thenReturn("update");
        Mockito.when(event14.getGroupId()).thenReturn("g13");

        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3, event4, event5, event6,
                event7, event8, event9, event10, event11, event12, event13, event14));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 50);

        // Verify
        Assert.assertEquals(4, results.size());

        // * Bob and Alice have updated the page "Bike"
        Assert.assertTrue(results.get(0).getEvents().contains(event1));
        Assert.assertTrue(results.get(0).getEvents().contains(event2));

        // * Bob and Carol have commented the page "Bike"
        Assert.assertTrue(results.get(1).getEvents().contains(event3));
        Assert.assertTrue(results.get(1).getEvents().contains(event4));
        Assert.assertTrue(results.get(1).getEvents().contains(event5));
        Assert.assertTrue(results.get(1).getEvents().contains(event6));

        // * Dave has commented the page "Guitar"
        Assert.assertTrue(results.get(2).getEvents().contains(event7));
        Assert.assertTrue(results.get(2).getEvents().contains(event8));

        // * Bob and Alice have annotated the page "Bike"
        Assert.assertTrue(results.get(3).getEvents().contains(event9));
        Assert.assertTrue(results.get(3).getEvents().contains(event10));
        Assert.assertTrue(results.get(3).getEvents().contains(event11));
        Assert.assertTrue(results.get(3).getEvents().contains(event12));
        Assert.assertTrue(results.get(3).getEvents().contains(event13));
        Assert.assertTrue(results.get(3).getEvents().contains(event14));
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
        Event event1 = Mockito.mock(Event.class); Mockito.when(event1.toString()).thenReturn("event1");
        Event event2 = Mockito.mock(Event.class); Mockito.when(event1.toString()).thenReturn("event2");
        Event event3 = Mockito.mock(Event.class); Mockito.when(event1.toString()).thenReturn("event3");

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        Mockito.when(event1.getDocument()).thenReturn(doc);
        Mockito.when(event2.getDocument()).thenReturn(doc);
        Mockito.when(event3.getDocument()).thenReturn(doc);

        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        Mockito.when(event1.getType()).thenReturn("update");
        Mockito.when(event2.getType()).thenReturn("addComment");
        Mockito.when(event3.getType()).thenReturn("addAnnotation");

        Mockito.when(event1.getGroupId()).thenReturn("g1");
        Mockito.when(event2.getGroupId()).thenReturn("g1");
        Mockito.when(event3.getGroupId()).thenReturn("g1");

        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 50);

        // Verify
        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getEvents().contains(event1));
        Assert.assertTrue(results.get(0).getEvents().contains(event2));
        Assert.assertTrue(results.get(0).getEvents().contains(event3));
    }

    @Test
    public void getEventsXWIKI14454() throws Exception
    {
        // Facts:
        // * Then Bob updates the page "Bike"
        // * Then Bob updates the page "Bike" again
        // * Then bob add a comment to the "Bike" page

        // Expected:
        // * Bob has commented the page "Bike"
        // * Bob has updated the page "Bike"

        // Mocks
        Event eventUpdate1          = Mockito.mock(Event.class);
        Event eventUpdate2          = Mockito.mock(Event.class);
        Event eventAddComment       = Mockito.mock(Event.class);
        Event eventAddCommentUpdate = Mockito.mock(Event.class);

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        Mockito.when(eventUpdate1.getDocument()).thenReturn(doc); Mockito.when(eventUpdate1.toString()).thenReturn("update1");
        Mockito.when(eventUpdate2.getDocument()).thenReturn(doc); Mockito.when(eventUpdate2.toString()).thenReturn("update2");
        Mockito.when(eventAddComment.getDocument()).thenReturn(doc); Mockito.when(eventAddComment.toString()).thenReturn("addComment");
        Mockito.when(eventAddCommentUpdate.getDocument()).thenReturn(doc); Mockito.when(eventAddCommentUpdate.toString()).thenReturn("updateComment");

        Mockito.when(authorizationManager.hasAccess(Right.VIEW, userReference, doc)).thenReturn(true);

        Mockito.when(eventUpdate1.getType()).thenReturn("update");
        Mockito.when(eventUpdate2.getType()).thenReturn("update");
        Mockito.when(eventAddComment.getType()).thenReturn("addComment");
        Mockito.when(eventAddCommentUpdate.getType()).thenReturn("update");

        Mockito.when(eventUpdate1.getGroupId()).thenReturn("g1");
        Mockito.when(eventUpdate2.getGroupId()).thenReturn("g2");
        Mockito.when(eventAddComment.getGroupId()).thenReturn("g3");
        Mockito.when(eventAddCommentUpdate.getGroupId()).thenReturn("g3");

        // They comes with inverse chronological order because of the query
        Mockito.when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(eventAddComment, eventAddCommentUpdate,
                eventUpdate2, eventUpdate1));

        // Test
        List<CompositeEvent> results
                = mocker.getComponentUnderTest().getEvents("xwiki:XWiki.UserA", true, 5);

        // Verify
        Assert.assertEquals(2, results.size());
    }
}