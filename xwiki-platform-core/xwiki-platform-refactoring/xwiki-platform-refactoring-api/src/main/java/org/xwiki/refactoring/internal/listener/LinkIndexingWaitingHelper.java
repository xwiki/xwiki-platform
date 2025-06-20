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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.link.LinkStore;
import org.xwiki.refactoring.RefactoringException;
import org.xwiki.refactoring.internal.job.AbstractEntityJob;
import org.xwiki.store.ReadyIndicator;

/**
 * Helper class for waiting on the link indexing.
 *
 * @version $Id$
 */
@Component(roles = LinkIndexingWaitingHelper.class)
@Singleton
public class LinkIndexingWaitingHelper implements Initializable, Disposable
{
    private ExecutorService executor;

    @Inject
    private JobContext jobContext;

    @Inject
    private JobProgressManager progressManager;

    // Use a Provider to avoid early initialization of the link store.
    @Inject
    private Provider<LinkStore> linkStore;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.executor.shutdown();
    }

    /**
     * Wait for link indexing to complete, asking the user after the given timeout if they want to continue waiting.
     * <p>
     *     This method only waits inside a job, and, in the case of an entity job, only if the request specifies that
     *     indexing should be waited for. Exceptions are caught and logged, but not rethrown.
     * </p>
     *
     * @param timeout the maximum time to wait
     * @param timeUnit the unit of the timeout value
     */
    public void maybeWaitForLinkIndexingWithLog(int timeout, TimeUnit timeUnit)
    {
        Job currentJob = this.jobContext.getCurrentJob();
        if (currentJob != null && (!(currentJob instanceof AbstractEntityJob<?, ?> entityJob)
            || entityJob.getRequest().isWaitForIndexing()))
        {
            // We're inside a job, so some waiting should be okay. Wait for the indexing of the link store to finish.
            try {
                this.logger.info("Waiting for the link index to be updated.");
                waitWithQuestion(timeout, timeUnit);
                this.logger.info("Finished waiting for the link index");
            } catch (InterruptedException e) {
                this.logger.warn(
                    "Interrupted while waiting for link indexing: [{}], continuing nevertheless.",
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full interrupted exception:", e);
                Thread.currentThread().interrupt();
            } catch (RefactoringException e) {
                this.logger.warn(
                    "Failed to wait for the link index to be updated: [{}], continuing nevertheless.",
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full exception:", e);
            }
        }
    }

    /**
     * Wait for link indexing to complete, asking the user after the given timeout if they want to continue waiting.
     *
     * @param timeout the timeout after which the question shall be asked
     * @param unit the unit of the timeout value
     * @throws InterruptedException if the thread got interrupted while waiting
     * @throws RefactoringException if any exception occurred while waiting, this would usually indicate that
     *     indexing was terminated
     */
    public void waitWithQuestion(int timeout, TimeUnit unit) throws InterruptedException, RefactoringException
    {
        if (this.jobContext.getCurrentJob() == null) {
            throw new IllegalStateException("This method must be used inside a job.");
        }

        this.progressManager.pushLevelProgress(100, this);
        ReadyIndicator readyIndicator = this.linkStore.get().waitReady();
        try {
            waitOnReadyIndicatorWithProgress(readyIndicator, timeout, unit);
        } catch (ExecutionException e) {
            throw new RefactoringException("Error while waiting for the link index to be updated.", e.getCause());
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void waitOnReadyIndicatorWithProgress(ReadyIndicator readyIndicator, int timeout, TimeUnit unit)
        throws ExecutionException, InterruptedException
    {
        int percent = 0;
        JobStatus jobStatus = this.jobContext.getCurrentJob().getStatus();
        LinkIndexingQuestionHandler questionHandler = new LinkIndexingQuestionHandler(timeout, unit,
            readyIndicator::getProgressPercentage, jobStatus, this.executor);

        try {
            while (true) {
                try {
                    readyIndicator.get(1, TimeUnit.SECONDS);
                    return;
                } catch (TimeoutException e) {
                    for (; percent < readyIndicator.getProgressPercentage(); ++percent) {
                        this.progressManager.startStep(this);
                    }

                    if (!questionHandler.shallContinueWaiting()) {
                        this.logger.info(
                            "Not waiting for the link index to be updated anymore as the user asked to stop waiting.");
                        break;
                    }
                }
            }
        } finally {
            questionHandler.cancelPendingQuestion();
        }
    }
}
