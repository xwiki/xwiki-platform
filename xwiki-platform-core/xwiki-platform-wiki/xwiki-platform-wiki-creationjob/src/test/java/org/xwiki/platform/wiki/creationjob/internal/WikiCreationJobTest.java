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
package org.xwiki.platform.wiki.creationjob.internal;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.job.JobContext;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiCreationStep;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class WikiCreationJobTest
{
    @InjectMockComponents
    private WikiCreationJob wikiCreationJob;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private LoggerManager loggerManager;

    @MockComponent
    private JobStatusStore store;

    @MockComponent
    private Provider<Execution> executionProvider;

    @MockComponent
    private Provider<ExecutionContextManager> executionContextManagerProvider;

    @MockComponent
    private JobContext jobContext;

    @MockComponent
    private JobProgressManager progressManager;

    @MockComponent
    private Logger logger;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private final Execution execution = new DefaultExecution();

    private ExecutionContextManager executionContextManager;

    @BeforeEach
    void setUp()
    {
        this.executionContextManager = mock(ExecutionContextManager.class);
        when(this.executionProvider.get()).thenReturn(this.execution);
        when(this.executionContextManagerProvider.get()).thenReturn(this.executionContextManager);
        this.execution.pushContext(new ExecutionContext());
    }

    @Test
    void runInternal() throws Exception
    {
        // Mocks
        WikiCreationStep step1 = mock(WikiCreationStep.class);
        WikiCreationStep step2 = mock(WikiCreationStep.class);
        WikiCreationStep step3 = mock(WikiCreationStep.class);
        when(step1.getOrder()).thenReturn(100);
        when(step2.getOrder()).thenReturn(50);
        when(step3.getOrder()).thenReturn(75);

        this.componentManager.registerComponent(WikiCreationStep.class, "step1", step1);
        this.componentManager.registerComponent(WikiCreationStep.class, "step2", step2);
        this.componentManager.registerComponent(WikiCreationStep.class, "step3", step3);

        // Test
        WikiCreationRequest request = new WikiCreationRequest();
        request.setId(List.of("myrequest"));
        this.wikiCreationJob.start(request);

        // Verify
        InOrder inOrder = inOrder(step1, step2, step3, this.progressManager);
        // Verify that the steps are executed in the good order
        inOrder.verify(this.progressManager).pushLevelProgress(eq(3), any(Object.class));
        inOrder.verify(this.progressManager, calls(1)).startStep(any(Object.class));
        inOrder.verify(step2).execute(any(WikiCreationRequest.class));
        inOrder.verify(this.progressManager, calls(1)).startStep(any(Object.class));
        inOrder.verify(step3).execute(any(WikiCreationRequest.class));
        inOrder.verify(this.progressManager, calls(1)).startStep(any(Object.class));
        inOrder.verify(step1).execute(any(WikiCreationRequest.class));
        inOrder.verify(this.progressManager).popLevelProgress(any(Object.class));
    }

    @Test
    void runInternalWithException() throws Exception
    {
        // Mocks
        WikiCreationStep step1 = mock(WikiCreationStep.class);
        this.componentManager.registerComponent(WikiCreationStep.class, "step1", step1);
        when(step1.getOrder()).thenReturn(100);

        WikiCreationException exception = new WikiCreationException("Error in the step");
        doThrow(exception).when(step1).execute(any(WikiCreationRequest.class));

        // Test
        WikiCreationRequest request = new WikiCreationRequest();
        request.setId(List.of("myrequest"));
        request.setWikiId("wikiId");
        this.wikiCreationJob.start(request);

        // Verify
        verify(this.logger).error(any(Marker.class), eq("Exception thrown during job execution"),
            eq(new WikiCreationException("Failed to execute creation steps on the wiki [wikiId].", exception)));
    }

    @Test
    void getType()
    {
        assertEquals("wikicreationjob", this.wikiCreationJob.getType());
    }
}
