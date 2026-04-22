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
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.distributionwizard.DistributionWizardManager;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.distributionwizard.DistributionWizardUIDefinition;
import org.xwiki.distributionwizard.rest.DistributionWizardStepResources;
import org.xwiki.distributionwizard.rest.model.jaxb.Step;
import org.xwiki.distributionwizard.rest.model.jaxb.StepSummary;
import org.xwiki.distributionwizard.rest.model.jaxb.UIComponent;
import org.xwiki.rest.XWikiResource;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Component
@Named("org.xwiki.distributionwizard.rest.internal.DefaultDistributionWizardStepResources")
public class DefaultDistributionWizardStepResources extends XWikiResource implements DistributionWizardStepResources
{
    private static final String REQUIRED_EXTENSION_HEADER = "X-XWIKI-HTML-HEAD";

    @Inject
    private DistributionWizardManager distributionWizardManager;

    @Inject
    private StepResourceHelper stepResourceHelper;

    @Override
    public Response getStep(String wikiId, String stepId) throws Exception
    {
        DistributionWizardStep wizardStep = this.distributionWizardManager.getStep(wikiId, stepId);

        StepSummary stepSummary = this.stepResourceHelper.toStepSummary(wizardStep);
        UIComponent uiComponent = new UIComponent();
        DistributionWizardUIDefinition uiDefinition = wizardStep.getUIDefinition();
        String requiredSkinExtension = "";
        if (uiDefinition != null) {
            uiComponent.setComponent(uiDefinition.uiComponentName());
            uiComponent.setModule(uiDefinition.uiModuleName());
            uiComponent.setHtml(uiDefinition.html());
            requiredSkinExtension = uiDefinition.requiredSkinExtension();
        }

        Step step = new Step()
            .withId(stepSummary.getId())
            .withTitle(stepSummary.getTitle())
            .withDone(stepSummary.isDone())
            .withDependsOnPreviousStep(stepSummary.isDependsOnPreviousStep())
            .withNeedsInput(stepSummary.isNeedsInput())
            .withNeedsManualStart(stepSummary.isNeedsManualStart())
            .withSkippable(stepSummary.isSkippable())
            .withUiComponent(uiComponent);
        return Response.ok(step).header(REQUIRED_EXTENSION_HEADER, requiredSkinExtension).build();
    }

    // FIXME: handle exceptions
    @Override
    public void answerStep(String wikiId, String stepId, Map<String, Serializable> data) throws Exception
    {
        DistributionWizardStep wizardStep = this.distributionWizardManager.getStep(wikiId, stepId);
        if (wizardStep.needsInput()) {
            wizardStep.processStep(data);
        } else {
            throw new WebApplicationException("This step doesn't take inputs", Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void processStep(String wikiId, String stepId) throws Exception
    {
        DistributionWizardStep wizardStep = this.distributionWizardManager.getStep(wikiId, stepId);
        if (wizardStep.startsOnDisplay()) {
            wizardStep.processStep(Map.of());
        } else {
            throw new WebApplicationException("This step doesn't start manually.", Response.Status.BAD_REQUEST);
        }
    }
}
