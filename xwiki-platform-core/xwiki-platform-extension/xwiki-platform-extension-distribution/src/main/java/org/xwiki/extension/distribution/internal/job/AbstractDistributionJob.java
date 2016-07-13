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
package org.xwiki.extension.distribution.internal.job;

import java.util.List;
import java.util.concurrent.locks.Condition;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep.State;
import org.xwiki.extension.distribution.internal.job.step.ReportDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.WelcomeDistributionStep;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.event.status.JobStatus;

/**
 * @version $Id$
 * @since 5.0M1
 */
public abstract class AbstractDistributionJob<R extends DistributionRequest, S extends DistributionJobStatus<R>>
    extends AbstractJob<R, S> implements DistributionJob
{
    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    protected DistributionManager distributionManager;

    /**
     * Condition to wait for ready state.
     */
    protected final Condition readyCondition = lock.newCondition();

    @Override
    public String getType()
    {
        return "distribution";
    }

    protected abstract S createNewDistributionStatus(R request, List<DistributionStep> steps);

    protected abstract List<DistributionStep> createSteps();

    @Override
    protected S createNewStatus(R request)
    {
        List<DistributionStep> steps = createSteps();

        // Add Welcome step
        try {
            DistributionStep welcomeStep =
                this.componentManager
                    .<DistributionStep> getInstance(DistributionStep.class, WelcomeDistributionStep.ID);
            welcomeStep.setState(State.COMPLETED);

            steps.add(0, welcomeStep);
        } catch (ComponentLookupException e1) {
            this.logger.error("Failed to get step instance for id [{}]", WelcomeDistributionStep.ID);
        }

        // Add Report step
        try {
            DistributionStep welcomeStep =
                this.componentManager.<DistributionStep> getInstance(DistributionStep.class, ReportDistributionStep.ID);
            welcomeStep.setState(State.COMPLETED);

            steps.add(welcomeStep);
        } catch (ComponentLookupException e1) {
            this.logger.error("Failed to get step instance for id [{}]", ReportDistributionStep.ID);
        }

        // Create status

        S status = createNewDistributionStatus(request, steps);

        if (this.distributionManager.getDistributionExtension() != null) {
            DistributionJobStatus< ? > previousStatus = getPreviousStatus();

            if (previousStatus != null
                && previousStatus.getDistributionExtension() != null
                && !ObjectUtils.equals(previousStatus.getDistributionExtension(),
                    this.distributionManager.getDistributionExtension())) {
                status.setPreviousDistributionExtension(previousStatus.getDistributionExtension());
                status.setPreviousDistributionExtensionUI(previousStatus.getDistributionExtensionUI());
            }

            status.setDistributionExtension(this.distributionManager.getDistributionExtension().getId());
            status.setDistributionExtensionUI(getUIExtensionId());
        }

        return status;
    }

    protected S getDistributionJobStatus()
    {
        return getStatus();
    }

    protected DistributionStep getStep(List<DistributionStep> steps, String stepId)
    {
        for (DistributionStep step : steps) {
            if (step.getId().equals(stepId)) {
                return step;
            }
        }

        return null;
    }

    @Override
    protected void runInternal() throws Exception
    {
        List<DistributionStep> steps = getDistributionJobStatus().getSteps();

        this.progressManager.pushLevelProgress(steps.size(), this);

        // Initialize steps
        WelcomeDistributionStep welcomeStep = (WelcomeDistributionStep) getStep(steps, WelcomeDistributionStep.ID);
        ReportDistributionStep reportStep = (ReportDistributionStep) getStep(steps, ReportDistributionStep.ID);

        for (DistributionStep step : steps) {
            step.initialize(this);

            // Enable Welcome step if one of the steps is enabled
            if (step.getState() == null) {
                if (welcomeStep != null) {
                    welcomeStep.setState(null);
                }
                if (reportStep != null) {
                    reportStep.setState(null);
                }
            }
        }

        // Execute steps
        try {
            for (int index = 0; index < steps.size(); ++index) {
                this.progressManager.startStep(this);

                getDistributionJobStatus().setCurrentStateIndex(index);

                DistributionStep step = steps.get(index);

                step.prepare();

                if (step.getState() == null) {
                    DistributionQuestion question = new DistributionQuestion(step);

                    // Waiting to start
                    signalReady();
                    getStatus().ask(question);

                    if (question.getAction() != null) {
                        switch (question.getAction()) {
                            case CANCEL:
                                for (; index < steps.size(); ++index) {
                                    steps.get(index).setState(DistributionStep.State.CANCELED);
                                }
                            case SKIP:
                                index = steps.size() - 1;
                                break;
                            default:
                                break;
                        }
                    }

                    // Save the status so that we remember the answer even if the DW is stopped before the end
                    this.store.storeAsync(this.status);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    // DistributionJob

    @Override
    public DistributionStep getCurrentStep()
    {
        return getStatus().getCurrentStep();
    }

    @Override
    protected void jobFinished(Throwable exception)
    {
        super.jobFinished(exception);

        signalReady();
    }

    private void signalReady()
    {
        this.lock.lock();

        try {
            this.readyCondition.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void awaitReady()
    {
        if (getStatus() == null || getStatus().getState() == JobStatus.State.RUNNING) {
            try {
                this.lock.lockInterruptibly();

                try {
                    this.readyCondition.await();
                } finally {
                    this.lock.unlock();
                }
            } catch (InterruptedException e) {
                this.logger.warn("The distribution job has been interrupted");
            }
        }
    }
}
