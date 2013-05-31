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

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep.State;
import org.xwiki.extension.distribution.internal.job.step.UpgradeModeDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.WelcomeDistributionStep;
import org.xwiki.job.internal.AbstractJob;

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

        // Create status

        S status = createNewDistributionStatus(request, steps);

        if (this.distributionManager.getDistributionExtension() != null) {
            DistributionJobStatus< ? > previousStatus = getPreviousStatus();

            if (previousStatus != null
                && previousStatus.getDistributionExtension() != null
                && !ObjectUtils.equals(previousStatus.getDistributionExtension(),
                    this.distributionManager.getDistributionExtension())) {
                status.setDistributionExtension(previousStatus.getDistributionExtension());
                status.setDistributionExtensionUI(previousStatus.getDistributionExtensionUI());
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
    protected void start() throws Exception
    {
        List<DistributionStep> steps = getDistributionJobStatus().getSteps();

        notifyPushLevelProgress(steps.size());

        // Initialize steps
        WelcomeDistributionStep welcomeStep = (WelcomeDistributionStep) getStep(steps, WelcomeDistributionStep.ID);
        UpgradeModeDistributionStep upgrademodeStep =
            (UpgradeModeDistributionStep) getStep(steps, UpgradeModeDistributionStep.ID);

        for (DistributionStep step : steps) {
            step.initialize(this);

            // Enable Welcome step if one of the steps is enabled
            if (step.getState() == null) {
                if (welcomeStep != null) {
                    welcomeStep.setState(null);
                }

                // TODO: find some better rule
                // CANCELED by default, will be enabled only if it's enabled in the status or if another step is
                if (upgrademodeStep != null && upgrademodeStep.getState() == State.CANCELED) {
                    upgrademodeStep.setState(null);
                }
            }
        }

        // Execute steps
        try {
            for (int index = 0; index < steps.size(); ++index) {
                getDistributionJobStatus().setCurrentStateIndex(index);

                DistributionStep step = steps.get(index);

                step.prepare();

                if (step.getState() == null) {
                    DistributionQuestion question = new DistributionQuestion(step);

                    // Waiting to start
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
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    // DistributionJob

    @Override
    public DistributionStep getCurrentStep()
    {
        return getStatus().getCurrentStep();
    }
}
