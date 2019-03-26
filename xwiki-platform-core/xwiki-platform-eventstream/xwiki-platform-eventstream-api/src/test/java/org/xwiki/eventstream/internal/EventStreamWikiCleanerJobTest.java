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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.job.JobContext;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventStreamWikiCleanerJobTest
{
    @Rule
    public MockitoComponentMockingRule<EventStreamWikiCleanerJob> mocker =
            new MockitoComponentMockingRule<>(EventStreamWikiCleanerJob.class);

    private EventStream eventStream;
    private QueryManager queryManager;

    private ObservationManager observationManager;
    private LoggerManager loggerManager;
    private JobStatusStore store;
    private Provider<Execution> executionProvider;
    private Provider<ExecutionContextManager> executionContextManagerProvider;
    private JobContext jobContext;
    private JobProgressManager progressManager;
    private Execution execution = new DefaultExecution();
    private ExecutionContextManager executionContextManager;

    @Before
    public void setUp() throws Exception
    {
        eventStream = mocker.getInstance(EventStream.class);
        queryManager = mocker.getInstance(QueryManager.class);

        observationManager = mocker.getInstance(ObservationManager.class);
        loggerManager = mocker.getInstance(LoggerManager.class);
        store = mocker.getInstance(JobStatusStore.class);
        executionProvider = mock(Provider.class);
        mocker.registerComponent(new DefaultParameterizedType(null, Provider.class, Execution.class),
                executionProvider);
        when(executionProvider.get()).thenReturn(execution);
        executionContextManagerProvider = mock(Provider.class);
        mocker.registerComponent(new DefaultParameterizedType(null, Provider.class,
                ExecutionContextManager.class), executionContextManagerProvider);
        executionContextManager = mock(ExecutionContextManager.class);
        when(executionContextManagerProvider.get()).thenReturn(executionContextManager);
        jobContext = mocker.getInstance(JobContext.class);
        progressManager = mocker.getInstance(JobProgressManager.class);
        execution.pushContext(new ExecutionContext());
    }

    @Test
    public void run() throws Exception
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
        EventStreamWikiCleanerJob job = mocker.getComponentUnderTest();
        job.initialize(request);
        job.runInternal();

        // Verify
        verify(eventStream).deleteEvent(event1);
        verify(eventStream).deleteEvent(event2);
        verify(eventStream).deleteEvent(event3);
        verify(eventStream).deleteEvent(event4);
        verify(query, times(3)).bindValue("wiki", "someWiki");
    }

    @Test
    public void getType() throws Exception
    {
        assertEquals("EventStreamWikiCleanerJob", mocker.getComponentUnderTest().getType());
    }
}
