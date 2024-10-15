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

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

import org.xwiki.job.event.status.JobStatus;

/**
 * Helper class for asking the user if the job should continue waiting for link indexing to complete.
 *
 * @version $Id$
 */
public class LinkIndexingQuestionHandler
{
    private final ExecutorService executor;

    private final int timeout;

    private final TimeUnit unit;

    private final IntSupplier progressSupplier;

    private Future<Void> questionFuture;

    private LinkIndexingQuestion question;

    private final JobStatus jobStatus;

    private final Instant startTime;

    /**
     * Constructor.
     *
     * @param timeout the time to wait before asking the user if the job should continue waiting for link indexing to
     *           complete
     * @param unit the time unit of the timeout
     * @param progressSupplier a supplier for the progress of the link indexing
     * @param jobStatus the job status to ask the question on
     * @param executor the executor to use for asking the question
     */
    public LinkIndexingQuestionHandler(int timeout, TimeUnit unit,
        IntSupplier progressSupplier, JobStatus jobStatus, ExecutorService executor)
    {
        this.executor = executor;
        this.jobStatus = jobStatus;
        this.timeout = timeout;
        this.unit = unit;
        this.progressSupplier = progressSupplier;
        this.startTime = Instant.now();
    }

    /**
     * Ask the user if the job should continue waiting for link indexing to complete if the timeout has been reached
     * and retrieve the answer if the user has given any yet. This method should be called repeatedly until either
     * it returns {@code false} or the waiting is done.
     *
     * @return if the job should continue waiting for link indexing to complete
     * @throws ExecutionException if there is an exception while the question is being asked
     * @throws InterruptedException if the thread is interrupted while the question is being asked
     */
    public boolean shallContinueWaiting() throws ExecutionException, InterruptedException
    {
        if (this.question == null && this.progressSupplier.getAsInt() < 75
            && this.startTime.plus(this.timeout, this.unit.toChronoUnit()).isBefore(Instant.now()))
        {
            this.question = new LinkIndexingQuestion();
            this.questionFuture = this.executor.submit(() -> {
                this.jobStatus.ask(this.question);
                // Make this lambda a Callable<Void> by returning a value so it can also throw exceptions.
                return null;
            });
        }

        // Technically, if questionFuture isn't null, the question cannot be null as we always set the question before
        // setting the question future. However, SonarCloud doesn't recognize this and the quality check fails
        // without the null check.
        if (this.questionFuture != null && this.questionFuture.isDone() && this.question != null) {
            this.questionFuture.get();
            this.questionFuture = null;

            return this.question.isContinueWaiting();
        }

        return true;
    }

    /**
     * Cancel any potentially pending question. This should be called when the question is no longer needed.
     */
    public void cancelPendingQuestion()
    {
        if (this.questionFuture != null && !this.questionFuture.isDone()) {
            this.jobStatus.answered();
        }
    }
}
