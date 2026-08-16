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
package org.xwiki.distributionwizard.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardManager;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.job.event.status.JobStatus;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Component
@Singleton
public class DefaultDistributionWizardManager implements DistributionWizardManager
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private DistributionManager distributionManager;

    @Override
    public List<DistributionWizardStep> getSteps(String wikiId) throws DistributionWizardException
    {
        try {
            List<DistributionWizardStep> instanceList = componentManager.getInstanceList(DistributionWizardStep.class);
            List<DistributionWizardStep> resultList = new ArrayList<>();
            List<String> previousStepIds = new ArrayList<>();
            for (DistributionWizardStep step : instanceList) {
                if (!step.isStepDone()) {
                    checkStepDependency(step, previousStepIds);
                    resultList.add(step);
                }
                previousStepIds.add(step.getHint());
            }
            // FIXME: handle dependent steps
            return resultList;
        } catch (ComponentLookupException e) {
            throw new DistributionWizardException("Error while loading the list of steps", e);
        }
    }

    private void checkStepDependency(DistributionWizardStep step, List<String> stepIds)
        throws DistributionWizardException
    {
        Optional<String> previousStepOpt = step.dependsOnPreviousStep();
        if (previousStepOpt.isPresent()) {
            if (!stepIds.contains(previousStepOpt.get())) {
                throw new DistributionWizardException(String.format("Step [%s] depends on [%s] but that one is "
                    + "missing.", step.getHint(), previousStepOpt.get()));
            }
        }
    }

    @Override
    public DistributionWizardStep getStep(String wikiId, String stepHint) throws DistributionWizardException
    {
        if (componentManager.hasComponent(DistributionWizardStep.class, stepHint)) {
            try {
                return componentManager.getInstance(DistributionWizardStep.class, stepHint);
            } catch (ComponentLookupException e) {
                throw new DistributionWizardException(String.format("Error while loading step [%s].", stepHint), e);
            }
        } else {
            throw new DistributionWizardException(String.format("Step [%s] does not exist.", stepHint));
        }
    }

    @Override
    public boolean shouldBeDisplayed()
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        boolean isJobDone = distributionJob != null && distributionJob.getStatus() != null
            && distributionJob.getStatus().getState() == JobStatus.State.FINISHED;
        return this.distributionManager.canDisplayDistributionWizard() && !isJobDone;
    }
}
