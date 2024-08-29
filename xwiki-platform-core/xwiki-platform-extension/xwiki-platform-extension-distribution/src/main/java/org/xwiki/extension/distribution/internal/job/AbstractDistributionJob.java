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
import java.util.Objects;
import java.util.concurrent.locks.Condition;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep.State;
import org.xwiki.extension.distribution.internal.job.step.ReportDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.WelcomeDistributionStep;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * @version $Id$
 * @since 5.0M1
 */
public abstract class AbstractDistributionJob<R extends DistributionRequest>
    extends AbstractJob<R, DistributionJobStatus> implements DistributionJob
{
    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    protected DistributionManager distributionManager;

    @Inject
    protected Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    /**
     * Condition to wait for ready state.
     */
    protected final Condition readyCondition = lock.newCondition();

    @Override
    public String getType()
    {
        return "distribution";
    }

    protected String getWiki()
    {
        return getRequest().getWiki();
    }

    protected boolean isMainWiki()
    {
        return this.wikiDescriptorManagerProvider.get().getMainWikiId().equals(getWiki());
    }

    protected DistributionJobStatus createNewDistributionStatus(DistributionRequest request,
        List<DistributionStep> steps)
    {
        return new DistributionJobStatus(request, this.observationManager, this.loggerManager, steps);
    }

    protected abstract List<DistributionStep> createSteps();

    @Override
    protected DistributionJobStatus createNewStatus(R request)
    {
        List<DistributionStep> steps = createSteps();

        if (getRequest().isInteractive()) {
            // Add Welcome step
            try {
                DistributionStep welcomeStep = this.componentManager
                    .<DistributionStep>getInstance(DistributionStep.class, WelcomeDistributionStep.ID);
                welcomeStep.setState(State.COMPLETED);

                steps.add(0, welcomeStep);
            } catch (ComponentLookupException e1) {
                this.logger.error("Failed to get step instance for id [{}]", WelcomeDistributionStep.ID);
            }

            // Add Report step
            try {
                DistributionStep welcomeStep = this.componentManager
                    .<DistributionStep>getInstance(DistributionStep.class, ReportDistributionStep.ID);
                welcomeStep.setState(State.COMPLETED);

                steps.add(welcomeStep);
            } catch (ComponentLookupException e1) {
                this.logger.error("Failed to get step instance for id [{}]", ReportDistributionStep.ID);
            }
        }

        // Create status

        DistributionJobStatus status = createNewDistributionStatus(request, steps);

        if (this.distributionManager.getDistributionExtension() != null) {
            DistributionJobStatus previousStatus = getPreviousStatus();

            if (previousStatus != null && previousStatus.getDistributionExtension() != null
                && !Objects.equals(previousStatus.getDistributionExtension(),
                    this.distributionManager.getDistributionExtension().getId())) {
                status.setPreviousDistributionExtension(previousStatus.getDistributionExtension());
                status.setPreviousDistributionExtensionUI(previousStatus.getDistributionExtensionUI());
            }

            status.setDistributionExtension(this.distributionManager.getDistributionExtension().getId());
            status.setDistributionExtensionUI(getUIExtensionId());
        }

        return status;
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
        List<DistributionStep> steps = getStatus().getSteps();

        this.progressManager.pushLevelProgress(steps.size(), this);

        // Initialize steps
        WelcomeDistributionStep welcomeStep = (WelcomeDistributionStep) getStep(steps, WelcomeDistributionStep.ID);
        ReportDistributionStep reportStep = (ReportDistributionStep) getStep(steps, ReportDistributionStep.ID);

        // Initialize steps
        steps.forEach(s -> s.initialize(this));

        // Prepare steps until reaching the first enabled step and enabled welcome/report steps if they exist
        for (DistributionStep step : steps) {
            // Prepare the step to check if there is something to do
            step.prepare();

            if (step.getState() == null) {
                // Enable Welcome and report steps if one of the steps is enabled
                if (welcomeStep != null) {
                    welcomeStep.setState(null);
                    reportStep.setState(null);
                }

                // Don't prepare following steps as it might be too early
                break;
            }
        }

        // Execute steps
        try {
            for (int index = 0; index < steps.size(); ++index) {
                this.progressManager.startStep(this);

                getStatus().setCurrentStateIndex(index);

                DistributionStep step = steps.get(index);

                // Prepare step
                step.prepare();

                // Execute step
                if (step.getState() == null) {
                    if (getRequest().isInteractive()) {
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
                    } else {
                        step.executeNonInteractive();
                    }
                }

                this.progressManager.endStep(this);
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
    public void awaitReady() throws InterruptedException
    {
        if (getStatus() == null || getStatus().getState() == JobStatus.State.RUNNING) {
            this.lock.lockInterruptibly();

            try {
                this.readyCondition.await();
            } finally {
                this.lock.unlock();
            }
        }
    }

    @Override
    public void setProperty(String key, Object value)
    {
        getStatus().setProperty(key, value);
    }

    @Override
    public Object getProperty(String key)
    {
        return getStatus().getproperty(key);
    }

    @Override
    public void removeProperty(String key)
    {
        getStatus().removeProperty(key);
    }
}
