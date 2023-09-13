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
package org.xwiki.notifications.notifiers.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.internal.DefaultEntityEvent;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupManager;
import org.xwiki.user.internal.group.UsersCache;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class UserEventDispatcherTest
{
    private static final String SUPPORTED_TYPE1 = "supportedType1";
    private static final String SUPPORTED_TYPE2 = "supportedType2";

    private static final String REMOTE_OBSERVATION_ID ="remoteObservationId";

    @InjectMockComponents
    private UserEventDispatcher dispatcher;

    @MockComponent
    private UsersCache userCache;

    @MockComponent
    private WikiDescriptorManager wikiManager;

    @MockComponent
    private UserEventManager userEventManager;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    private ExecutionContextManager ecm;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    private UserManager userManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    private EventStore events;

    @MockComponent
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @MockComponent
    private RemoteObservationManagerConfiguration remoteObservation;

    private SimpleEventQuery query;

    @BeforeEach
    void setup() throws EventStreamException
    {
        RecordableEventDescriptor descriptor1 = mock(RecordableEventDescriptor.class);
        RecordableEventDescriptor descriptor2 = mock(RecordableEventDescriptor.class);
        when(descriptor1.getEventType()).thenReturn(SUPPORTED_TYPE1);
        when(descriptor2.getEventType()).thenReturn(SUPPORTED_TYPE2);
        when(this.recordableEventDescriptorManager.getRecordableEventDescriptors(true)).thenReturn(
            List.of(descriptor1, descriptor2));
        when(this.remoteObservation.getId()).thenReturn(REMOTE_OBSERVATION_ID);

        query = new SimpleEventQuery()
            .eq(Event.FIELD_PREFILTERED, false)
            .open()
            .eq(Event.FIELD_REMOTE_OBSERVATION_ID, REMOTE_OBSERVATION_ID)
            .or()
            .eq(Event.FIELD_REMOTE_OBSERVATION_ID, null)
            .close()
            .addSort(Event.FIELD_DATE, SortableEventQuery.SortClause.Order.ASC)
            .setLimit(100)
            .not()
            .in(Event.FIELD_ID, new ArrayList<>());
    }

    @Test
    void flush() throws Exception
    {
        EventSearchResult result1 = mock(EventSearchResult.class, "result1");
        EventSearchResult result2 = mock(EventSearchResult.class, "result2");
        EventSearchResult result3 = mock(EventSearchResult.class, "result2");
        when(this.events.search(query))
            .thenReturn(result1)
            .thenReturn(result2)
            .thenReturn(result3);

        String event1Result1Id = "event1R1";
        Event event1Result1 = mock(Event.class, event1Result1Id);
        String event2Result1Id = "event2R1";
        Event event2Result1 = mock(Event.class, event2Result1Id);
        String event3Result1Id = "event3R1";
        Event event3Result1 = mock(Event.class, event3Result1Id);

        when(event1Result1.getId()).thenReturn(event1Result1Id);
        when(event2Result1.getId()).thenReturn(event2Result1Id);
        when(event3Result1.getId()).thenReturn(event3Result1Id);

        when(result1.stream()).thenReturn(Stream.of(event1Result1, event2Result1, event3Result1));
        when(result1.getSize()).thenReturn(3L);

        String event1Result2Id = "event1R2";
        Event event1Result2 = mock(Event.class, event1Result2Id);
        String event2Result2Id = "event2R2";
        Event event2Result2 = mock(Event.class, event2Result2Id);

        when(event1Result2.getId()).thenReturn(event1Result2Id);
        when(event2Result2.getId()).thenReturn(event2Result2Id);

        when(result2.stream()).thenReturn(Stream.of(event1Result2, event2Result2));
        when(result2.getSize()).thenReturn(3L);

        when(result3.getSize()).thenReturn(0L);

        // Handling of Event1R1
        when(event1Result1.getType()).thenReturn("foo");

        CompletableFuture futureE1R1 = mock(CompletableFuture.class, "futureE1R1");
        when(this.events.prefilterEvent(event1Result1)).thenReturn(futureE1R1);

        // verify(this.events).prefilterEvent(event1Result1);
        // verify(futureE1R1).join();

        // Handling of Event2R1
        when(event2Result1.getType()).thenReturn(SUPPORTED_TYPE2);
        WikiReference mainWiki = new WikiReference("xwiki");
        when(event2Result1.getWiki()).thenReturn(mainWiki);

        when(this.wikiManager.isMainWiki(mainWiki.getName())).thenReturn(true);

        DocumentReference mainUserFoo = mock(DocumentReference.class, "mainUserFoo");
        DocumentReference mainUserBar = mock(DocumentReference.class, "mainUserBar");
        when(this.userCache.getUsers(mainWiki, true)).thenReturn(List.of(mainUserFoo, mainUserBar));

        when(this.notificationConfiguration.areEmailsEnabled()).thenReturn(true);

        String mainUserFooStr = "mainUserFoo";
        String mainUserBarStr = "mainUserBar";
        when(this.entityReferenceSerializer.serialize(mainUserFoo)).thenReturn(mainUserFooStr);
        when(this.entityReferenceSerializer.serialize(mainUserBar)).thenReturn(mainUserBarStr);

        SimpleEventQuery queryStatusE2R1 = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event2Result1Id)
            .withStatus(mainUserFooStr);
        EventSearchResult searchResult0Hit = mock(EventSearchResult.class, "searchResult0Hit");
        when(searchResult0Hit.getTotalHits()).thenReturn(0L);
        when(this.events.search(queryStatusE2R1)).thenReturn(searchResult0Hit);
        when(this.userEventManager.isListening(event2Result1, mainUserFoo, NotificationFormat.ALERT)).thenReturn(true);
        // verify(this.events).saveEventStatus(new DefaultEventStatus(event2Result1, mainUserFooStr, false))

        SimpleEventQuery queryMailE2R1 = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event2Result1Id)
            .withMail(mainUserFooStr);
        when(this.events.search(queryMailE2R1)).thenReturn(searchResult0Hit);
        when(this.userEventManager.isListening(event2Result1, mainUserFoo, NotificationFormat.EMAIL)).thenReturn(true);
        // verify(this.events).saveMailEntityEvent(new DefaultEventStatus(event2Result1, mainUserFooStr))

        SimpleEventQuery queryStatusE2R1Bar = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event2Result1Id)
            .withStatus(mainUserBarStr);
        EventSearchResult searchResultHits = mock(EventSearchResult.class, "searchResultHits");
        when(searchResultHits.getTotalHits()).thenReturn(1L);
        when(this.events.search(queryStatusE2R1Bar)).thenReturn(searchResultHits);

        // verify(this.userEventManager, never()).isListening(event2Result1, mainUserBar, NotificationFormat.ALERT)
        // verify(this.events, never()).saveEventStatus(new DefaultEventStatus(event2Result1, mainUserBarStr, false))

        SimpleEventQuery queryMailE2R1Bar = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event2Result1Id)
            .withMail(mainUserBarStr);
        when(this.events.search(queryMailE2R1Bar)).thenReturn(searchResultHits);

        // verify(this.userEventManager, never()).isListening(event2Result1, mainUserBar, NotificationFormat.EMAIL)
        // verify(this.events, never()).saveMailEntityEvent(new DefaultEventStatus(event2Result1, mainUserBarStr))

        CompletableFuture futureE2R1 = mock(CompletableFuture.class, "futureE2R1");
        when(this.events.prefilterEvent(event2Result1)).thenReturn(futureE2R1);

        // verify(this.events).prefilterEvent(event2Result1);
        // verify(futureE2R1).join();

        // Handling of Event3R1
        when(event3Result1.getType()).thenReturn(SUPPORTED_TYPE1);
        when(event3Result1.getWiki()).thenReturn(mainWiki);

        SimpleEventQuery queryStatusE3R1 = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event3Result1Id)
            .withStatus(mainUserFooStr);
        when(this.events.search(queryStatusE3R1)).thenReturn(searchResult0Hit);
        when(this.userEventManager.isListening(event3Result1, mainUserFoo, NotificationFormat.ALERT)).thenReturn(false);
        // verify(this.events, never()).saveEventStatus(new DefaultEventStatus(event3Result1, mainUserFooStr, false))

        SimpleEventQuery queryMailE3R1 = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event3Result1Id)
            .withMail(mainUserFooStr);
        when(this.events.search(queryMailE3R1)).thenReturn(searchResult0Hit);
        when(this.userEventManager.isListening(event3Result1, mainUserFoo, NotificationFormat.EMAIL)).thenReturn(false);
        // verify(this.events, never()).saveMailEntityEvent(new DefaultEventStatus(event3Result1, mainUserFooStr))

        SimpleEventQuery queryStatusE3R1Bar = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event3Result1Id)
            .withStatus(mainUserBarStr);
        when(this.events.search(queryStatusE3R1Bar)).thenReturn(searchResult0Hit);
        when(this.userEventManager.isListening(event3Result1, mainUserBar, NotificationFormat.ALERT)).thenReturn(true);
        // verify(this.events).saveEventStatus(new DefaultEventStatus(event3Result1, mainUserBarStr, false))

        SimpleEventQuery queryMailE3R1Bar = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event3Result1Id)
            .withMail(mainUserBarStr);
        when(this.events.search(queryMailE3R1Bar)).thenReturn(searchResult0Hit);
        when(this.userEventManager.isListening(event3Result1, mainUserBar, NotificationFormat.EMAIL)).thenReturn(true);
        // verify(this.events).saveMailEntityEvent(new DefaultEventStatus(event3Result1, mainUserBarStr))

        CompletableFuture futureE3R1 = mock(CompletableFuture.class, "futureE3R1");
        when(this.events.prefilterEvent(event3Result1)).thenReturn(futureE3R1);

        // verify(this.events).prefilterEvent(event3Result1);
        // verify(futureE3R1).join();

        // Handling of event1R2
        when(event1Result2.getType()).thenReturn(SUPPORTED_TYPE1);
        when(event1Result2.getWiki()).thenReturn(mainWiki);

        when(event1Result2.getTarget()).thenReturn(Set.of(mainUserFooStr, mainUserBarStr));
        when(this.resolver.resolve(mainUserFooStr, mainWiki)).thenReturn(mainUserFoo);
        when(this.resolver.resolve(mainUserBarStr, mainWiki)).thenReturn(mainUserBar);

        UserReference fooUserRef = mock(UserReference.class, "fooUser");
        UserReference barUserRef = mock(UserReference.class, "barUser");

        when(this.documentReferenceUserReferenceResolver.resolve(mainUserFoo)).thenReturn(fooUserRef);
        when(this.documentReferenceUserReferenceResolver.resolve(mainUserBar)).thenReturn(barUserRef);

        when(this.userManager.exists(fooUserRef)).thenReturn(false);
        when(this.userManager.exists(barUserRef)).thenReturn(true);

        SimpleEventQuery queryStatusE1R2 = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event1Result2Id)
            .withStatus(mainUserFooStr);
        SimpleEventQuery queryMailE1R2 = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event1Result2Id)
            .withMail(mainUserFooStr);

        // verify(this.events, never()).search(queryStatusE1R2)
        // verify(this.events, never()).search(queryMailE1R2)

        SimpleEventQuery queryStatusE1R2Bar = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event1Result2Id)
            .withStatus(mainUserBarStr);
        when(this.events.search(queryStatusE1R2Bar)).thenReturn(searchResultHits);
        // verify(this.userEventManager, never()).isListening(event1Result2, mainUserBar, NotificationFormat.ALERT))
        // verify(this.events, never()).saveEventStatus(new DefaultEventStatus(event1Result2, mainUserBarStr, false))

        SimpleEventQuery queryMailE1R2Bar = new SimpleEventQuery(0, 0)
            .eq(Event.FIELD_ID, event1Result2Id)
            .withMail(mainUserBarStr);
        when(this.events.search(queryMailE1R2Bar)).thenReturn(searchResult0Hit);
        when(this.userEventManager.isListening(event1Result2, mainUserBar, NotificationFormat.EMAIL)).thenReturn(true);
        // verify(this.events).saveMailEntityEvent(new DefaultEventStatus(event1Result2, mainUserBarStr))

        assertNotEquals(queryMailE1R2Bar, queryStatusE1R2Bar);

        CompletableFuture futureE1R2 = mock(CompletableFuture.class, "futureE1R2");
        when(this.events.prefilterEvent(event1Result2)).thenReturn(futureE1R2);

        // verify(this.events).prefilterEvent(event1Result2);
        // verify(futureE1R2).join();

        // Handling of event2R2
        when(event2Result2.getType()).thenReturn("bar");
        when(event2Result2.getWiki()).thenReturn(mainWiki);
        CompletableFuture futureE2R2 = mock(CompletableFuture.class, "futureE2R2");
        when(this.events.prefilterEvent(event2Result2)).thenReturn(futureE2R2);
        // verify(this.events).prefilterEvent(event2Result2);
        // verify(futureE2R2).join();

        this.dispatcher.flush();

        verify(this.events).prefilterEvent(event1Result1);
        verify(futureE1R1).join();

        verify(this.events).saveEventStatus(new DefaultEventStatus(event2Result1, mainUserFooStr, false));
        verify(this.events).saveMailEntityEvent(new DefaultEntityEvent(event2Result1, mainUserFooStr));
        verify(this.userEventManager, never()).isListening(event2Result1, mainUserBar, NotificationFormat.ALERT);
        verify(this.events, never()).saveEventStatus(new DefaultEventStatus(event2Result1, mainUserBarStr, false));
        verify(this.userEventManager, never()).isListening(event2Result1, mainUserBar, NotificationFormat.EMAIL);
        verify(this.events, never()).saveMailEntityEvent(new DefaultEntityEvent(event2Result1, mainUserBarStr));
        verify(this.events).prefilterEvent(event2Result1);
        verify(futureE2R1).join();

        verify(this.events, never()).saveEventStatus(new DefaultEventStatus(event3Result1, mainUserFooStr, false));
        verify(this.events, never()).saveMailEntityEvent(new DefaultEntityEvent(event3Result1, mainUserFooStr));
        verify(this.events).saveEventStatus(new DefaultEventStatus(event3Result1, mainUserBarStr, false));
        verify(this.events).saveMailEntityEvent(new DefaultEntityEvent(event3Result1, mainUserBarStr));
        verify(this.events).prefilterEvent(event3Result1);
        verify(futureE3R1).join();

        verify(this.events, never()).search(queryStatusE1R2);
        verify(this.events, never()).search(queryMailE1R2);
        verify(this.userEventManager, never()).isListening(event1Result2, mainUserBar, NotificationFormat.ALERT);
        verify(this.events, never()).saveEventStatus(new DefaultEventStatus(event1Result2, mainUserBarStr, false));
        verify(this.events).saveMailEntityEvent(new DefaultEntityEvent(event1Result2, mainUserBarStr));
        verify(this.events).prefilterEvent(event1Result2);
        verify(futureE1R2).join();

        verify(this.events).prefilterEvent(event2Result2);
        verify(futureE2R2).join();

        verify(this.ecm, times(3)).pushContext(any(), eq(false));
        verify(this.ecm, times(3)).popContext();

    }
}