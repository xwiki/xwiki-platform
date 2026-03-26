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
package org.xwiki.distributionwizard.rest.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.distributionwizard.DistributionWizardManager;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.distributionwizard.rest.DistributionWizardStepResources;
import org.xwiki.distributionwizard.rest.DistributionWizardStepsResources;
import org.xwiki.distributionwizard.rest.model.jaxb.Step;
import org.xwiki.distributionwizard.rest.model.jaxb.Steps;
import org.xwiki.distributionwizard.rest.model.jaxb.UIComponent;
import org.xwiki.rest.XWikiResource;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Component
@Named("org.xwiki.distributionwizard.rest.internal.DefaultDistributionWizardStepResources")
public class DefaultDistributionWizardStepResources extends XWikiResource implements DistributionWizardStepResources
{
    @Inject
    private DistributionWizardManager distributionWizardManager;

    @Inject
    private StepResourceHelper stepResourceHelper;

    @Override
    public Step getStep(String wikiId, String stepId) throws Exception
    {
        DistributionWizardStep wizardStep = this.distributionWizardManager.getStep(wikiId, stepId);
        return this.stepResourceHelper.toStep(wizardStep);
    }

    @Override
    public void answerStep(String wikiId, String stepId, Map<String, Serializable> data) throws Exception
    {
        DistributionWizardStep wizardStep = this.distributionWizardManager.getStep(wikiId, stepId);
        wizardStep.handleAnswer(data);
    }
}
