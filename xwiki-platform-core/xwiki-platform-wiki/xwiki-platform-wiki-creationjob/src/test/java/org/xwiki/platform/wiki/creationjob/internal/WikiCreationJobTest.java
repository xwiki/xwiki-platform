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

import java.util.Arrays;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.slf4j.Marker;
import org.xwiki.component.util.DefaultParameterizedType;
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
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class WikiCreationJobTest
{
    @Rule
    public MockitoComponentMockingRule<WikiCreationJob> mocker = new MockitoComponentMockingRule<>(WikiCreationJob.class);

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
    public void runInternal() throws Exception
    {
        // Mocks
        WikiCreationStep step1 = mock(WikiCreationStep.class);
        WikiCreationStep step2 = mock(WikiCreationStep.class);
        WikiCreationStep step3 = mock(WikiCreationStep.class);
        when(step1.getOrder()).thenReturn(100);
        when(step2.getOrder()).thenReturn(50);
        when(step3.getOrder()).thenReturn(75);
        
        mocker.registerComponent(WikiCreationStep.class, "step1", step1);
        mocker.registerComponent(WikiCreationStep.class, "step2", step2);
        mocker.registerComponent(WikiCreationStep.class, "step3", step3);
        
        // Test
        WikiCreationRequest request = new WikiCreationRequest();
        request.setId(Arrays.asList("myrequest"));
        mocker.getComponentUnderTest().start(request);
        
        // Verify
        InOrder inOrder = inOrder(step1, step2, step3, progressManager);
        // Verify that the steps are executed in the good order
        inOrder.verify(progressManager).pushLevelProgress(eq(3), any(Object.class));
        inOrder.verify(step2).execute(any(WikiCreationRequest.class));
        inOrder.verify(progressManager, calls(1)).stepPropress(any(Object.class));
        inOrder.verify(step3).execute(any(WikiCreationRequest.class));
        inOrder.verify(progressManager, calls(1)).stepPropress(any(Object.class));
        inOrder.verify(step1).execute(any(WikiCreationRequest.class));
        inOrder.verify(progressManager, calls(1)).stepPropress(any(Object.class));
        inOrder.verify(progressManager).popLevelProgress(any(Object.class));
    }


    @Test
    public void runInternalWithException() throws Exception
    {
        // Mocks
        WikiCreationStep step1 = mock(WikiCreationStep.class);
        mocker.registerComponent(WikiCreationStep.class, "step1", step1);
        when(step1.getOrder()).thenReturn(100);

        WikiCreationException exception = new WikiCreationException("Error in the step");
        doThrow(exception).when(step1).execute(any(WikiCreationRequest.class));
        
        // Test
        WikiCreationRequest request = new WikiCreationRequest();
        request.setId(Arrays.asList("myrequest"));
        request.setWikiId("wikiId");
        mocker.getComponentUnderTest().start(request);

        // Verify
        verify(mocker.getMockedLogger()).error(any(Marker.class), eq("Exception thrown during job execution"),
                eq(new WikiCreationException("Failed to execute creation steps on the wiki [wikiId].", exception)));
    }
    
    @Test
    public void getType() throws Exception
    {
        assertEquals("wikicreationjob", mocker.getComponentUnderTest().getType());
    }
}
