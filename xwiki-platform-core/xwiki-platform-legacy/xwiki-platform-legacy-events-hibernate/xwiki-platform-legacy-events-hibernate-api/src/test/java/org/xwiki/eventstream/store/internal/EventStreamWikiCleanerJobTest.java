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
package org.xwiki.eventstream.store.internal;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link EventStreamWikiCleanerJob}.
 * 
 * @version $Id$
 */
@ComponentTest
class EventStreamWikiCleanerJobTest
{
    @InjectMockComponents
    private EventStreamWikiCleanerJob job;

    @MockComponent
    private EventStream eventStream;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<Execution> executionProvider;

    private Execution execution = new DefaultExecution();

    @MockComponent
    private ExecutionContextManager executionContextManager;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(executionProvider.get()).thenReturn(execution);
        executionContextManager = mock(ExecutionContextManager.class);
        execution.pushContext(new ExecutionContext());
    }

    @Test
    void run() throws Exception
    {
        Query query = mock(Query.class);
        when(queryManager.createQuery("where event.wiki = :wiki", Query.HQL)).thenReturn(query);

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);
        Event event4 = mock(Event.class);

        when(eventStream.searchEvents(query)).thenReturn(Arrays.asList(event1, event2), Arrays.asList(event3, event4),
            Collections.emptyList());

        // Test
        EventStreamWikiCleanerJobRequest request = new EventStreamWikiCleanerJobRequest("someWiki");
        this.job.initialize(request);
        this.job.runInternal();

        // Verify
        verify(eventStream).deleteEvent(event1);
        verify(eventStream).deleteEvent(event2);
        verify(eventStream).deleteEvent(event3);
        verify(eventStream).deleteEvent(event4);
        verify(query, times(3)).bindValue("wiki", "someWiki");
    }

    @Test
    void getType() throws Exception
    {
        assertEquals("EventStreamWikiCleanerJob", this.job.getType());
    }
}
