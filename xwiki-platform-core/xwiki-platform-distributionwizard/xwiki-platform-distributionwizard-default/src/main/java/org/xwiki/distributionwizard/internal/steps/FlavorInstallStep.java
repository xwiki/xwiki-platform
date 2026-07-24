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
package org.xwiki.distributionwizard.internal.steps;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import org.xwiki.component.annotation.Component;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardUIDefinition;
import org.xwiki.distributionwizard.internal.FlavorHelper;
import org.xwiki.extension.distribution.internal.job.step.FlavorDistributionStep;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Component
@Singleton
@Named("FlavorInstallStep")
@Priority(30)
public class FlavorInstallStep extends AbstractStep
{
    @Inject
    private FlavorHelper flavorHelper;

    @Override
    public Optional<String> dependsOnPreviousStep()
    {
        return Optional.of("FlavorChoiceStep");
    }

    @Override
    public boolean startsOnDisplay()
    {
        return true;
    }

    @Override
    public void processStep(Map<String, Serializable> input) throws DistributionWizardException
    {
        this.invalidateUI();
        if (!this.flavorHelper.isNoFlavorSelected()) {
            try {
                Job installJob = this.flavorHelper.startSelectedFlavorInstallation();
                getDistributionJob().setProperty("installJobStatus", installJob.getStatus());
                completeJobStep();
            } catch (JobException e) {
                throw new DistributionWizardException("Error while starting installation job", e);
            }
        }
    }

    @Override
    protected String getJobStepId()
    {
        return FlavorDistributionStep.ID;
    }

    @Override
    public boolean isStepDone() throws DistributionWizardException
    {
        return this.flavorHelper.isNoFlavorSelected() || this.flavorHelper.isFlavorInstalled();
    }

    @Override
    public Map<String, Serializable> getStepDoneInformation() throws DistributionWizardException
    {
        if (flavorHelper.isFlavorInstalled()) {
            return Map.of("flavorInstalled", flavorHelper.getInstalledFlavor().getId());
        } else {
            return Map.of("flavor.selected", "noFlavor");
        }
    }

    @Override
    protected DistributionWizardUIDefinition createUIDefinition()
    {
        return renderTemplate("flavorinstallstep.vm");
    }
}
