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
import org.xwiki.extension.distribution.internal.job.step.DefaultUIDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
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

        // Step 0: A welcome message. Only if there is actually something to do
        for (DistributionStep step : steps) {
            if (step.getState() == null) {
                try {
                    steps.add(0, this.componentManager.<DistributionStep> getInstance(DistributionStep.class,
                        WelcomeDistributionStep.ID));
                } catch (ComponentLookupException e) {
                    this.logger.error("Failed to get step instance for id [{}]", WelcomeDistributionStep.ID);
                }
                break;
            }
        }

        // Create status

        S status = createNewDistributionStatus(request, steps);

        if (this.distributionManager.getDistributionExtension() != null) {
            DistributionJobStatus< ? > previousStatus = this.distributionManager.getPreviousFarmJobStatus();

            if (previousStatus != null
                && previousStatus.getDistributionExtension() != null
                && !ObjectUtils.equals(previousStatus.getDistributionExtension(),
                    this.distributionManager.getDistributionExtension())) {
                status.setDistributionExtension(previousStatus.getDistributionExtension());
                status.setDistributionExtensionUi(previousStatus.getDistributionExtensionUi());
            }

            status.setDistributionExtension(this.distributionManager.getDistributionExtension().getId());
            status.setDistributionExtensionUi(this.distributionManager.getMainUIExtensionId());
        }

        return status;
    }

    protected S getDistributionJobStatus()
    {
        return getStatus();
    }

    @Override
    protected void start() throws Exception
    {
        List<DistributionStep> steps = getDistributionJobStatus().getSteps();

        notifyPushLevelProgress(steps.size());

        try {
            for (int index = 0; index < steps.size(); ++index) {
                getDistributionJobStatus().setCurrentStateIndex(index);

                DistributionStep step = steps.get(index);

                if (step.getState() == null) {
                    DistributionQuestion question = new DistributionQuestion(step);

                    // Waiting to start
                    getStatus().ask(question);

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
