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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.jodconverter.core.util.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.distributionwizard.DistributionWizardManager;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.distributionwizard.rest.DistributionWizardStepsResources;
import org.xwiki.distributionwizard.rest.model.jaxb.Step;
import org.xwiki.distributionwizard.rest.model.jaxb.Steps;
import org.xwiki.rest.XWikiResource;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Component
@Named("org.xwiki.distributionwizard.rest.internal.DefaultDistributionWizardStepsResources")
public class DefaultDistributionWizardStepsResources extends XWikiResource implements DistributionWizardStepsResources
{
    private static final String REQUIRED_EXTENSION_HEADER = "X-XWIKI-HTML-HEAD";

    @Inject
    private DistributionWizardManager distributionWizardManager;

    @Inject
    private StepResourceHelper stepResourceHelper;

    @Override
    public Response getSteps(String wikiId) throws Exception
    {
        List<DistributionWizardStep> wizardSteps = this.distributionWizardManager.getSteps(wikiId);
        StringBuilder requiredExtensions = new StringBuilder();
        Steps steps = new Steps();
        List<Step> stepList = new ArrayList<>();
        for (DistributionWizardStep wizardStep : wizardSteps) {
            Step step = this.stepResourceHelper.toStep(wizardStep);
            stepList.add(step);
            if (StringUtils.isNotBlank(step.getUiComponent().getRequiredSkinExtensions())) {
                requiredExtensions.append(step.getUiComponent().getRequiredSkinExtensions());
                requiredExtensions.append(' ');
            }
        }

        steps.withStep(stepList);
        return Response.ok(steps).header(REQUIRED_EXTENSION_HEADER, requiredExtensions.toString()).build();
    }
}
