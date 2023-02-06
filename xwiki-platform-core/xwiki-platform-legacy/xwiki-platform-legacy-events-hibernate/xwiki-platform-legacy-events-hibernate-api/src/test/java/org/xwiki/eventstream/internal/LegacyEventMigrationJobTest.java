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
package org.xwiki.eventstream.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link LegacyEventMigrationJob}.
 *
 * @version $Id$
 */
@ComponentTest
public class LegacyEventMigrationJobTest
{
    @InjectMockComponents
    private LegacyEventMigrationJob migrationJob;

    @MockComponent
    private EventStreamConfiguration configuration;

    @MockComponent
    private EventStream eventStream;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private ComponentManager componentManager;

    @MockComponent
    private JobProgressManager progressManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void runInternalNoStoreOrDisabled() throws Exception
    {
        when(this.configuration.isEventStoreEnabled()).thenReturn(false);
        this.migrationJob.runInternal();
        assertEquals("New event store system is disabled", logCapture.getMessage(0));
        verify(this.componentManager, never()).getInstance(any(), any());
        verify(this.eventStream, never()).countEvents();

        when(this.configuration.isEventStoreEnabled()).thenReturn(true);
        when(this.configuration.getEventStore()).thenReturn("");
        this.migrationJob.runInternal();
        assertEquals("New event store system is disabled", logCapture.getMessage(1));
        verify(this.componentManager, never()).getInstance(any(), any());
        verify(this.eventStream, never()).countEvents();

        when(this.configuration.getEventStore()).thenReturn("myStore");
        when(this.componentManager.getInstance(EventStore.class, "myStore"))
            .thenThrow(new ComponentLookupException("Error when looking for myStore"));

        this.migrationJob.runInternal();
        assertEquals("Failed to get the configured event store [myStore]", logCapture.getMessage(2));
        verify(this.componentManager).getInstance(EventStore.class, "myStore");
        verify(this.eventStream, never()).countEvents();
    }

    @Test
    void runInternal() throws Exception
    {
        when(this.configuration.isEventStoreEnabled()).thenReturn(true);
        when(this.configuration.getEventStore()).thenReturn("myStore");
        EventStore eventStore = mock(EventStore.class);

        when(this.componentManager.getInstance(EventStore.class, "myStore")).thenReturn(eventStore);

        // Batch size is 100, so should be processed in 2 steps.
        when(this.eventStream.countEvents()).thenReturn(105L);

        LegacyEventMigrationRequest request = new LegacyEventMigrationRequest(new Date(42),
            Arrays.asList("myId", "42"));
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("WHERE event.date >= :since ORDER BY event.date desc", Query.HQL)).thenReturn(query);
        Query statusQuery = mock(Query.class);
        when(this.queryManager.createQuery("select eventStatus.entityId from LegacyEventStatus eventStatus "
            + "where eventStatus.activityEvent.id = :eventId", Query.HQL)).thenReturn(statusQuery);

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);
        Event event4 = mock(Event.class);

        when(this.eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2, event3, event4));

        // event1 is already present: it won't be migrated
        when(event1.getId()).thenReturn("event1");
        when(eventStore.getEvent("event1")).thenReturn(Optional.of(event1));

        when(event2.getId()).thenReturn("event2");
        when(eventStore.getEvent("event2")).thenReturn(Optional.empty());

        // By default we migrate event without any wiki.
        when(event2.getWiki()).thenReturn(null);

        when(event3.getId()).thenReturn("event3");
        when(eventStore.getEvent("event3")).thenReturn(Optional.empty());

        // The wiki of event3 does not exist anymore: the event won't be migrated
        WikiReference event3Wiki = new WikiReference("foo");
        when(event3.getWiki()).thenReturn(event3Wiki);
        when(this.wikiDescriptorManager.exists("foo")).thenReturn(false);

        when(event4.getId()).thenReturn("event4");
        when(eventStore.getEvent("event4")).thenReturn(Optional.empty());

        WikiReference event4Wiki = new WikiReference("bar");
        when(event4.getWiki()).thenReturn(event4Wiki);
        when(this.wikiDescriptorManager.exists("bar")).thenReturn(true);

        CompletableFuture<Event> future = mock(CompletableFuture.class);
        when(eventStore.saveEvent(event4)).thenReturn(future);

        this.migrationJob.initialize(request);
        this.migrationJob.runInternal();

        InOrder progressManagerOrder = inOrder(this.progressManager);
        progressManagerOrder.verify(this.progressManager).pushLevelProgress(eq(2), same(this.migrationJob));
        progressManagerOrder.verify(this.progressManager).startStep(same(this.migrationJob));
        verify(query).bindValue("since", new Date(42));
        verify(query).setOffset(100);
        verify(eventStore, never()).saveEvent(event1);
        verify(eventStore).saveEvent(event2);
        verify(eventStore, never()).saveEvent(event3);
        verify(eventStore).saveEvent(event4);
        verify(future).get();
        progressManagerOrder.verify(this.progressManager).popLevelProgress(same(this.migrationJob));
    }
}
