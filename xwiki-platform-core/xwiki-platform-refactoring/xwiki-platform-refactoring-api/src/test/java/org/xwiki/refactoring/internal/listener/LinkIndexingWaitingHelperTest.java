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
package org.xwiki.refactoring.internal.listener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.link.LinkStore;
import org.xwiki.refactoring.RefactoringException;
import org.xwiki.refactoring.internal.job.AbstractEntityJob;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.store.ReadyIndicator;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LinkIndexingWaitingHelper}.
 *
 * @version $Id$
 */
@ComponentTest
class LinkIndexingWaitingHelperTest
{
    protected static final String WAITING_MESSAGE = "Waiting for the link index to be updated.";

    private static final String FINISHED_WAITING_MESSAGE = "Finished waiting for the link index";

    @InjectMockComponents
    private LinkIndexingWaitingHelper helper;

    @MockComponent
    private JobContext jobContext;

    @MockComponent
    private JobProgressManager progressManager;

    @MockComponent
    private LinkStore linkStore;

    @Mock
    private ReadyIndicator readyIndicator;

    @Mock
    private EntityJobStatus<EntityRequest> jobStatus;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @BeforeEach
    public void configure()
    {
        when(this.jobContext.getCurrentJob()).thenReturn(mock());
        when(this.jobContext.getCurrentJob().getStatus()).thenReturn(this.jobStatus);
        when(this.linkStore.waitReady()).thenReturn(this.readyIndicator);
    }

    @Test
    void waitWithQuestionInsideJob() throws Exception
    {
        this.helper.waitWithQuestion(1, TimeUnit.SECONDS);

        verify(this.progressManager).pushLevelProgress(100, this.helper);
        verify(this.progressManager).popLevelProgress(this.helper);
    }

    @Test
    void waitWithQuestionOutsideJob()
    {
        when(this.jobContext.getCurrentJob()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> this.helper.waitWithQuestion(1, TimeUnit.SECONDS));
    }

    @Test
    void waitWithQuestionTimeout() throws Exception
    {
        doThrow(new TimeoutException()).doNothing().when(this.readyIndicator).get(anyLong(), any());

        when(this.readyIndicator.getProgressPercentage()).thenReturn(50);
        this.helper.waitWithQuestion(1, TimeUnit.HOURS);

        InOrder inOrder = inOrder(this.progressManager);
        inOrder.verify(this.progressManager).pushLevelProgress(100, this.helper);
        inOrder.verify(this.progressManager, times(50)).startStep(this.helper);
        inOrder.verify(this.progressManager).popLevelProgress(this.helper);
        verifyNoInteractions(this.jobStatus);
        verify(this.readyIndicator, times(2)).get(1, TimeUnit.SECONDS);
    }

    @Test
    void waitWithQuestionUserStopped() throws Exception
    {
        doAnswer(invocationOnMock -> {
            // Give the thread that runs the lambda a chance to run.
            Thread.yield();
            throw new TimeoutException();
        }).when(this.readyIndicator).get(anyLong(), any());

        when(this.readyIndicator.getProgressPercentage()).thenReturn(42);

        // Immediately ask the user if they want to continue waiting.
        this.helper.waitWithQuestion(0, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(this.progressManager);
        inOrder.verify(this.progressManager).pushLevelProgress(100, this.helper);
        inOrder.verify(this.progressManager, times(42)).startStep(this.helper);
        inOrder.verify(this.progressManager).popLevelProgress(this.helper);
        verify(this.jobStatus).ask(any(LinkIndexingQuestion.class));
        verify(this.readyIndicator, atLeastOnce()).get(1, TimeUnit.SECONDS);
        assertEquals("Not waiting for the link index to be updated anymore as the user asked to stop waiting.",
            this.logCapture.getMessage(0));
    }

    @Test
    void waitWithQuestionInterruptedException() throws Exception
    {
        doThrow(new InterruptedException()).when(this.readyIndicator).get(anyLong(), any());

        assertThrows(InterruptedException.class, () -> this.helper.waitWithQuestion(1, TimeUnit.SECONDS));
        verify(this.progressManager).popLevelProgress(this.helper);
    }

    @Test
    void waitWithQuestionExecutionException() throws Exception
    {
        Exception expectedException = new Exception();
        doThrow(new ExecutionException(expectedException)).when(this.readyIndicator).get(anyLong(), any());

        RefactoringException actualException =
            assertThrows(RefactoringException.class, () -> this.helper.waitWithQuestion(1, TimeUnit.SECONDS));
        assertSame(expectedException, actualException.getCause());
        verify(this.progressManager).popLevelProgress(this.helper);
    }

    @Test
    void maybeWaitForLinkIndexingWithLogWithoutJob()
    {
        when(this.jobContext.getCurrentJob()).thenReturn(null);

        this.helper.maybeWaitForLinkIndexingWithLog(1, TimeUnit.SECONDS);

        verifyNoInteractions(this.progressManager);
    }

    @Test
    void maybeWaitForLinkIndexingWithLogWithGenericJob()
    {
        this.helper.maybeWaitForLinkIndexingWithLog(1, TimeUnit.SECONDS);

        verify(this.progressManager).pushLevelProgress(100, this.helper);
        verify(this.progressManager).popLevelProgress(this.helper);

        assertEquals(WAITING_MESSAGE, this.logCapture.getMessage(0));
        assertEquals(FINISHED_WAITING_MESSAGE, this.logCapture.getMessage(1));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void maybeWaitForLinkIndexingWithLogWithEntityJob(boolean shallWait)
    {
        AbstractEntityJob<EntityRequest, EntityJobStatus<EntityRequest>> entityJob = mock();
        when(this.jobContext.getCurrentJob()).thenReturn(entityJob);
        when(entityJob.getStatus()).thenReturn(this.jobStatus);
        EntityRequest request = mock();
        when(entityJob.getRequest()).thenReturn(request);
        when(request.isWaitForIndexing()).thenReturn(shallWait);

        this.helper.maybeWaitForLinkIndexingWithLog(1, TimeUnit.SECONDS);

        if (shallWait) {
            verify(this.progressManager).pushLevelProgress(100, this.helper);
            verify(this.progressManager).popLevelProgress(this.helper);

            assertEquals(WAITING_MESSAGE, this.logCapture.getMessage(0));
            assertEquals(FINISHED_WAITING_MESSAGE, this.logCapture.getMessage(1));
        } else {
            verifyNoInteractions(this.progressManager);
        }
    }

    @Test
    void maybeWaitForLinkIndexingWithLogExecutionException() throws Exception
    {
        String exceptionMessage = "Test error.";
        Exception expectedException = new Exception(exceptionMessage);
        doThrow(new ExecutionException(expectedException)).when(this.readyIndicator).get(anyLong(), any());

        this.helper.maybeWaitForLinkIndexingWithLog(1, TimeUnit.SECONDS);

        assertEquals(WAITING_MESSAGE, this.logCapture.getMessage(0));
        assertEquals("Failed to wait for the link index to be updated: [Exception: %s], continuing nevertheless."
            .formatted(exceptionMessage), this.logCapture.getMessage(1));
    }

    @Test
    void maybeWaitForLinkIndexingWithLogInterruptedException() throws Exception
    {
        String exceptionMessage = "Test interruption.";
        Exception expectedException = new InterruptedException(exceptionMessage);
        doThrow(expectedException).when(this.readyIndicator).get(anyLong(), any());

        this.helper.maybeWaitForLinkIndexingWithLog(1, TimeUnit.SECONDS);

        assertEquals(WAITING_MESSAGE, this.logCapture.getMessage(0));
        assertEquals("Interrupted while waiting for link indexing: [InterruptedException: %s], continuing nevertheless."
            .formatted(exceptionMessage), this.logCapture.getMessage(1));
    }
}
