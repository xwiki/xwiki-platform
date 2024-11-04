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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.xwiki.job.event.status.JobStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LinkIndexingQuestionHandler}.
 *
 * @version $Id$
 */
class LinkIndexingQuestionHandlerTest
{
    private JobStatus jobStatus;

    private AtomicInteger progress;

    @BeforeEach
    void setUp()
    {
        this.jobStatus = mock();
        this.progress = new AtomicInteger(0);
    }

    @Test
    void shallContinueWaitingBeforeTimeout() throws ExecutionException, InterruptedException
    {
        ExecutorService mockExecutor = mock();
        LinkIndexingQuestionHandler handler =
            new LinkIndexingQuestionHandler(1, TimeUnit.HOURS, this.progress::get, this.jobStatus, mockExecutor);

        this.progress.set(50);
        assertTrue(handler.shallContinueWaiting());
        verifyNoInteractions(mockExecutor);
        verifyNoInteractions(this.jobStatus);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shallContinueWaitingAfterTimeoutBeforeUserResponse(boolean shallWait) throws Exception
    {
        ExecutorService mockExecutor = mock();
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        when(mockExecutor.submit(ArgumentMatchers.<Callable<Void>>any())).thenReturn(completableFuture);
        LinkIndexingQuestionHandler handler =
            new LinkIndexingQuestionHandler(0, TimeUnit.SECONDS, this.progress::get, this.jobStatus, mockExecutor);

        this.progress.set(50);
        assertTrue(handler.shallContinueWaiting());
        // Capture the argument to the mock executor
        ArgumentCaptor<Callable<Void>> captor = ArgumentCaptor.captor();
        verify(mockExecutor).submit(captor.capture());
        // Run the task
        captor.getValue().call();
        // Capture the question
        ArgumentCaptor<LinkIndexingQuestion> questionCaptor = ArgumentCaptor.captor();
        verify(this.jobStatus).ask(questionCaptor.capture());

        // Set the user response
        questionCaptor.getValue().setContinueWaiting(shallWait);

        // While the question has been answered, the future is not yet completed, and thus we should still continue
        // waiting.
        assertTrue(handler.shallContinueWaiting());

        // Complete the future to simulate the user response.
        completableFuture.complete(null);

        // Check the result
        assertEquals(shallWait, handler.shallContinueWaiting());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shallContinueWaitingAfterTimeoutAndUserResponse(boolean shallWait) throws InterruptedException,
        ExecutionException
    {
        ExecutorService mockExecutor = mock();
        when(mockExecutor.submit(ArgumentMatchers.<Callable<Void>>any())).thenAnswer(invocation -> {
            Callable<Void> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        doAnswer(invocation -> {
            LinkIndexingQuestion question = invocation.getArgument(0);
            question.setContinueWaiting(shallWait);
            return null;
        }).when(this.jobStatus).ask(any(LinkIndexingQuestion.class));

        LinkIndexingQuestionHandler handler =
            new LinkIndexingQuestionHandler(0, TimeUnit.SECONDS, this.progress::get, this.jobStatus, mockExecutor);

        this.progress.set(50);
        assertEquals(shallWait, handler.shallContinueWaiting());
        verify(this.jobStatus).ask(any(LinkIndexingQuestion.class));
        if (shallWait) {
            assertTrue(handler.shallContinueWaiting());
        }
    }

    @ParameterizedTest
    @ValueSource(classes = { InterruptedException.class, ExecutionException.class })
    void shallContinueWaitingAfterTimeoutAndUserResponseWithException(Class<? extends Exception> exceptionClass)
        throws Exception
    {
        ExecutorService mockExecutor = mock();
        Future<Void> mockFuture = mock();
        when(mockExecutor.submit(ArgumentMatchers.<Callable<Void>>any())).thenReturn(mockFuture);
        when(mockFuture.isDone()).thenReturn(true);
        Exception expectedException = mock(exceptionClass);
        when(mockFuture.get()).thenThrow(expectedException);

        LinkIndexingQuestionHandler handler =
            new LinkIndexingQuestionHandler(0, TimeUnit.SECONDS, this.progress::get, this.jobStatus, mockExecutor);

        this.progress.set(50);
        Exception actualException = assertThrows(exceptionClass, handler::shallContinueWaiting);
        assertSame(expectedException, actualException);
    }

    @Test
    void shallContinueIfProgressIsHigherThan75() throws ExecutionException, InterruptedException
    {
        ExecutorService mockExecutor = mock();
        LinkIndexingQuestionHandler handler =
            new LinkIndexingQuestionHandler(0, TimeUnit.SECONDS, this.progress::get, this.jobStatus, mockExecutor);

        this.progress.set(75);
        assertTrue(handler.shallContinueWaiting());
        verifyNoInteractions(this.jobStatus);
        verifyNoInteractions(mockExecutor);
    }

    @Test
    void cancelPendingQuestionWithNoPendingQuestion()
    {
        ExecutorService mockExecutor = mock();
        LinkIndexingQuestionHandler handler =
            new LinkIndexingQuestionHandler(0, TimeUnit.SECONDS, this.progress::get, this.jobStatus, mockExecutor);

        handler.cancelPendingQuestion();
        verifyNoInteractions(this.jobStatus);
        verifyNoInteractions(mockExecutor);
    }

    @Test
    void cancelPendingQuestion() throws ExecutionException, InterruptedException
    {
        ExecutorService mockExecutor = mock();
        when(mockExecutor.submit(ArgumentMatchers.<Callable<Void>>any())).thenReturn(new CompletableFuture<>());
        LinkIndexingQuestionHandler handler =
            new LinkIndexingQuestionHandler(0, TimeUnit.SECONDS, this.progress::get, this.jobStatus, mockExecutor);

        this.progress.set(50);
        handler.shallContinueWaiting();
        handler.cancelPendingQuestion();
        verify(this.jobStatus).answered();
    }
}
