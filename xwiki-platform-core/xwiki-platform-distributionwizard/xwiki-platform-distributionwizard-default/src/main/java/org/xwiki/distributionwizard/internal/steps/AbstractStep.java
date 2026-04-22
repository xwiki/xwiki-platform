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

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.distributionwizard.DistributionWizardUIDefinition;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.skinx.RequiredSkinExtensionsRecorder;
import org.xwiki.template.TemplateManager;

import jakarta.inject.Inject;

public abstract class AbstractStep implements DistributionWizardStep
{
    protected static final String WEBJAR_NAME = "xwiki-platform-distributionwizard-webjar";

    @Inject
    private ComponentDescriptor componentDescriptor;

    @Inject
    private DistributionManager distributionManager;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private RequiredSkinExtensionsRecorder requiredSkinExtensionsRecorder;

    private DistributionWizardUIDefinition uiDefinition;

    protected DistributionJob getDistributionJob()
    {
        return this.distributionManager.getCurrentDistributionJob();
    }

    protected DistributionWizardUIDefinition createUIDefinition()
    {
        return new DistributionWizardUIDefinition(componentDescriptor.getRoleHint(), WEBJAR_NAME, "", "");
    }

    protected DistributionWizardUIDefinition renderTemplate(String templateName)
    {
        this.requiredSkinExtensionsRecorder.start();
        String html = this.templateManager.renderNoException(templateName);
        String requiredSkinExtension = this.requiredSkinExtensionsRecorder.stop();
        return new DistributionWizardUIDefinition(null, WEBJAR_NAME, html, requiredSkinExtension);
    }

    protected void invalidateUI()
    {
        this.uiDefinition = null;
    }

    @Override
    public String getHint()
    {
        return componentDescriptor.getRoleHint();
    }

    @Override
    public DistributionWizardUIDefinition getUIDefinition()
    {
        if (uiDefinition == null) {
            uiDefinition = createUIDefinition();
        }
        return uiDefinition;
    }

    @Override
    public boolean startsOnDisplay()
    {
        return false;
    }

    @Override
    public Optional<String> dependsOnPreviousStep()
    {
        return Optional.empty();
    }

    @Override
    public void processStep(Map<String, Serializable> input) throws DistributionWizardException
    {
        completeJobStep();
    }

    @Override
    public Map<String, Serializable> getStepDoneInformation() throws DistributionWizardException
    {
        return Map.of();
    }

    @Override
    public boolean isRedoable()
    {
        return false;
    }

    @Override
    public boolean needsInput()
    {
        return false;
    }

    @Override
    public boolean isSkippable()
    {
        return false;
    }

    protected void completeJobStep() throws DistributionWizardException
    {
        DistributionJob distributionJob = this.distributionManager.getCurrentDistributionJob();
        DistributionStep currentStep = distributionJob.getCurrentStep();
        if (getJobStepId().equals(currentStep.getId())) {
            currentStep.setState(DistributionStep.State.COMPLETED);
            distributionJob.getStatus().answered();
        } else {
            throw new DistributionWizardException(
                String.format("Current job step id [%s] doesn't match UI step id [%s].",
                    currentStep.getId(), getJobStepId()));
        }
    }

    protected abstract String getJobStepId();
}
