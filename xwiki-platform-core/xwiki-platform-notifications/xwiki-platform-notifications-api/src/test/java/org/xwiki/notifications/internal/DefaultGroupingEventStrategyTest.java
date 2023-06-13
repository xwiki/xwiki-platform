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

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultGroupingEventStrategy} which also test {@link SimilarityCalculator}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@ComponentTest
@ComponentList({SimilarityCalculator.class})
class DefaultGroupingEventStrategyTest
{
    @InjectMockComponents
    private DefaultGroupingEventStrategy groupingEventStrategy;

    private Event createMockedEvent()
    {
        Event event = mock(Event.class);
        when(event.getDate()).thenReturn(new Date(1L));
        return event;
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
    void groupUC1() throws NotificationException
    {
        // Facts:
        // * Alice updates the page "Bike"
        // * Bob updates the page "Bike"

        // Expected:
        // * Alice and Bob have updated the page "Bike"

        // Comment:
        // Note: the 2 events have been combined
        Event eventAlice = createMockedEvent();
        Event eventBob = createMockedEvent();

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(eventAlice.getDocument()).thenReturn(doc);
        when(eventBob.getDocument()).thenReturn(doc);

        when(eventAlice.getType()).thenReturn("update");
        when(eventBob.getType()).thenReturn("update");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(eventAlice, eventBob));

        // Verify
        assertEquals(1, results.size());
        assertEquals(eventAlice, results.get(0).getEvents().get(0));
        assertEquals(eventBob, results.get(0).getEvents().get(1));
    }

    @Test
    void groupUC2() throws NotificationException
    {
        // Facts:
        // * Bob comments the page "Bike" (which actually update the page too)

        // Expected:
        // * Bob has commented the page "Bike"

        // Comment: we do not mention that Bob has updated the page "Bike", because it's actually a technical
        // implementation of the "comment" feature.

        // Mocks
        Event eventComment = createMockedEvent();
        Event eventUpdate = createMockedEvent();

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(eventComment.getDocument()).thenReturn(doc);
        when(eventUpdate.getDocument()).thenReturn(doc);


        when(eventComment.getType()).thenReturn("addComment");
        when(eventUpdate.getType()).thenReturn("update");

        when(eventComment.getGroupId()).thenReturn("g1");
        when(eventUpdate.getGroupId()).thenReturn("g1");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(eventComment, eventUpdate));

        // Verify
        assertEquals(1, results.size());
        assertEquals(eventComment, results.get(0).getEvents().get(0));
        assertEquals(eventUpdate, results.get(0).getEvents().get(1));
    }

    @Test
    void groupUC3() throws Exception
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
        Event event1 = createMockedEvent();
        Event event2 = createMockedEvent();
        Event event3 = createMockedEvent();

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(event1.getDocument()).thenReturn(doc);
        when(event2.getDocument()).thenReturn(doc);
        when(event3.getDocument()).thenReturn(doc);

        when(event1.getType()).thenReturn("update");
        when(event2.getType()).thenReturn("addComment");
        when(event3.getType()).thenReturn("update");

        when(event1.getGroupId()).thenReturn("g1");
        when(event2.getGroupId()).thenReturn("g2");
        when(event3.getGroupId()).thenReturn("g2");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(event1, event2, event3));

        // Verify
        assertEquals(2, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
        assertEquals(event2, results.get(1).getEvents().get(0));
        assertEquals(event3, results.get(1).getEvents().get(1));
    }

    @Test
    void groupUC4() throws Exception
    {
        // Facts:
        // * Bob updates the page "Bike"
        // * Then Bob updates the page "Bike" again

        // Expected:
        // * Bob has updated the page "Bike"

        // Comment: we don't show 2 events, only one is interesting

        // Mocks
        Event event1 = createMockedEvent();
        Event event2 = createMockedEvent();

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(event1.getDocument()).thenReturn(doc);
        when(event2.getDocument()).thenReturn(doc);

        when(event1.getType()).thenReturn("update");
        when(event2.getType()).thenReturn("update");

        when(event1.getGroupId()).thenReturn("g1");
        when(event2.getGroupId()).thenReturn("g2");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(event1, event2));

        // Verify
        assertEquals(1, results.size());
        assertEquals(event1, results.get(0).getEvents().get(0));
        assertEquals(event2, results.get(0).getEvents().get(1));
    }

    @Test
    void groupUC5() throws Exception
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

        // * Bob updates the page "Bike" (E1)
        Event event1 = createMockedEvent();
        when(event1.toString()).thenReturn("event1");
        when(event1.getDocument()).thenReturn(doc1);
        when(event1.getType()).thenReturn("update");
        when(event1.getGroupId()).thenReturn("g1");

        // * Alice updates the page "Bike" (E2)
        Event event2 = createMockedEvent();
        when(event2.toString()).thenReturn("event2");
        when(event2.getDocument()).thenReturn(doc1);
        when(event2.getType()).thenReturn("update");
        when(event2.getGroupId()).thenReturn("g2");

        // * Bob comments the page "Bike" (E3 & E4)
        Event event3 = createMockedEvent();
        when(event3.toString()).thenReturn("event3");
        when(event3.getDocument()).thenReturn(doc1);
        when(event3.getType()).thenReturn("addComment");
        when(event3.getGroupId()).thenReturn("g3");
        Event event4 = createMockedEvent();
        when(event4.toString()).thenReturn("event4");
        when(event4.getDocument()).thenReturn(doc1);
        when(event4.getType()).thenReturn("update");
        when(event4.getGroupId()).thenReturn("g3");

        // * Carol comments the page "Bike" (E5 & E6)
        // (note: we put the "update" event before the "addComment", because we can not guarantee the order so
        // it's good to test both)
        Event event5 = createMockedEvent();
        when(event5.toString()).thenReturn("event5");
        when(event5.getDocument()).thenReturn(doc1);
        when(event5.getType()).thenReturn("update");
        when(event5.getGroupId()).thenReturn("g5");
        Event event6 = createMockedEvent();
        when(event6.toString()).thenReturn("event6");
        when(event6.getDocument()).thenReturn(doc1);
        when(event6.getType()).thenReturn("addComment");
        when(event6.getGroupId()).thenReturn("g5");

        // * Dave comments the page "Guitar" (E7 & E8)
        Event event7 = createMockedEvent();
        when(event7.toString()).thenReturn("event7");
        when(event7.getDocument()).thenReturn(doc2);
        when(event7.getType()).thenReturn("update");
        when(event7.getGroupId()).thenReturn("g7");
        Event event8 = createMockedEvent();
        when(event8.toString()).thenReturn("event8");
        when(event8.getDocument()).thenReturn(doc2);
        when(event8.getType()).thenReturn("addComment");
        when(event8.getGroupId()).thenReturn("g7");

        // * Bob adds an annotation on page "Bike" (E9 & E10)
        Event event9 = createMockedEvent();
        when(event8.toString()).thenReturn("event9");
        when(event9.getDocument()).thenReturn(doc1);
        when(event9.getType()).thenReturn("update");
        when(event9.getGroupId()).thenReturn("g9");
        Event event10 = createMockedEvent();
        when(event8.toString()).thenReturn("event10");
        when(event10.getDocument()).thenReturn(doc1);
        when(event10.getType()).thenReturn("addAnnotation");
        when(event10.getGroupId()).thenReturn("g9");

        // * Alice adds an annotation on page "Bike" (E11 & E12)
        Event event11 = createMockedEvent();
        when(event8.toString()).thenReturn("event11");
        when(event11.getDocument()).thenReturn(doc1);
        when(event11.getType()).thenReturn("update");
        when(event11.getGroupId()).thenReturn("g11");
        Event event12 = createMockedEvent();
        when(event8.toString()).thenReturn("event12");
        when(event12.getDocument()).thenReturn(doc1);
        when(event12.getType()).thenReturn("addAnnotation");
        when(event12.getGroupId()).thenReturn("g11");

        // * Alice adds an other annotation on page "Bike" (E12 & E13)
        Event event13 = createMockedEvent();
        when(event8.toString()).thenReturn("event11");
        when(event13.getDocument()).thenReturn(doc1);
        when(event13.getType()).thenReturn("addAnnotation");
        when(event13.getGroupId()).thenReturn("g13");
        Event event14 = createMockedEvent();
        when(event8.toString()).thenReturn("event12");
        when(event14.getDocument()).thenReturn(doc1);
        when(event14.getType()).thenReturn("update");
        when(event14.getGroupId()).thenReturn("g13");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(
            event1,
            event2,
            event3,
            event4,
            event5,
            event6,
            event7,
            event8,
            event9,
            event10,
            event11,
            event12,
            event13,
            event14
        ));

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
    void groupUpdate2Events() throws Exception
    {
        // Facts:
        // * Bob comment and annotate the page "Bike" in the same time

        // Expected:
        // * Bob has commented the page "Bike"
        // * Bob has annotated the page "Bike"

        // Mocks
        Event event1 = createMockedEvent();
        when(event1.toString()).thenReturn("event1");
        Event event2 = createMockedEvent();
        when(event1.toString()).thenReturn("event2");
        Event event3 = createMockedEvent();
        when(event1.toString()).thenReturn("event3");

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(event1.getDocument()).thenReturn(doc);
        when(event2.getDocument()).thenReturn(doc);
        when(event3.getDocument()).thenReturn(doc);

        when(event1.getType()).thenReturn("update");
        when(event2.getType()).thenReturn("addComment");
        when(event3.getType()).thenReturn("addAnnotation");

        when(event1.getGroupId()).thenReturn("g1");
        when(event2.getGroupId()).thenReturn("g1");
        when(event3.getGroupId()).thenReturn("g1");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(event1, event2, event3));

        // Verify
        assertEquals(1, results.size());
        assertTrue(results.get(0).getEvents().contains(event1));
        assertTrue(results.get(0).getEvents().contains(event2));
        assertTrue(results.get(0).getEvents().contains(event3));
    }

    @Test
    void groupXWIKI14454() throws Exception
    {
        // Facts:
        // * Then Bob updates the page "Bike"
        // * Then Bob updates the page "Bike" again
        // * Then bob add a comment to the "Bike" page

        // Expected:
        // * Bob has commented the page "Bike"
        // * Bob has updated the page "Bike"

        // Mocks
        Event eventUpdate1 = createMockedEvent();
        Event eventUpdate2 = createMockedEvent();
        Event eventAddComment = createMockedEvent();
        Event eventAddCommentUpdate = createMockedEvent();

        DocumentReference doc = new DocumentReference("xwiki", "Main", "Bike");
        when(eventUpdate1.getDocument()).thenReturn(doc);
        when(eventUpdate1.toString()).thenReturn("update1");
        when(eventUpdate2.getDocument()).thenReturn(doc);
        when(eventUpdate2.toString()).thenReturn("update2");
        when(eventAddComment.getDocument()).thenReturn(doc);
        when(eventAddComment.toString()).thenReturn("addComment");
        when(eventAddCommentUpdate.getDocument()).thenReturn(doc);
        when(eventAddCommentUpdate.toString()).thenReturn("updateComment");

        when(eventUpdate1.getType()).thenReturn("update");
        when(eventUpdate2.getType()).thenReturn("update");
        when(eventAddComment.getType()).thenReturn("addComment");
        when(eventAddCommentUpdate.getType()).thenReturn("update");

        when(eventUpdate1.getGroupId()).thenReturn("g1");
        when(eventUpdate2.getGroupId()).thenReturn("g2");
        when(eventAddComment.getGroupId()).thenReturn("g3");
        when(eventAddCommentUpdate.getGroupId()).thenReturn("g3");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(
            eventUpdate1,
            eventUpdate2,
            eventAddComment,
            eventAddCommentUpdate
        ));

        // Verify
        assertEquals(2, results.size());
    }

    @Test
    void groupXWIKI14719() throws Exception
    {
        DocumentReference userA = new DocumentReference("xwiki", "XWiki", "UserA");

        // Example taken from a real case
        Event event0 =
            createMockedEvent("update", userA, userA, new Date(1510567729000L), "1997830249-1510567729000-Puhs4MSa");
        Event event1 =
            createMockedEvent("update", userA, userA, new Date(1510567724000L), "1997830249-1510567724000-aCjmsmSh");
        Event event2 =
            createMockedEvent("update", userA, userA, new Date(1510567718000L), "1997830249-1510567718000-hEErMBp9");
        Event event3 =
            createMockedEvent("update", userA, userA, new Date(1510567717000L), "1997830249-1510567718000-hEErMBp9");
        Event event4 =
            createMockedEvent("update", userA, userA, new Date(1510567715000L), "1997830249-1510567715000-B723WWBC");
        Event event5 =
            createMockedEvent("update", userA, userA, new Date(1510567715000L), "1997830249-1510567715000-B723WWBC");
        Event event6 =
            createMockedEvent("update", userA, userA, new Date(1510567714000L), "1997830249-1510567714000-SHPmruCG");
        Event event7 =
            createMockedEvent("update", userA, userA, new Date(1510567712000L), "1997830249-1510567712000-Fy19J0v1");
        Event event8 =
            createMockedEvent("update", userA, userA, new Date(1510567711000L), "1997830249-1510567711000-zDfFnZbD");
        Event event9 =
            createMockedEvent("update", userA, userA, new Date(1510567711000L), "1997830249-1510567711000-zDfFnZbD");

        // Test
        List<CompositeEvent> results = this.groupingEventStrategy.group(List.of(
            event1,
            event2,
            event3,
            event4,
            event5,
            event6,
            event7,
            event8,
            event9
        ));

        assertEquals(1, results.size());
    }
}