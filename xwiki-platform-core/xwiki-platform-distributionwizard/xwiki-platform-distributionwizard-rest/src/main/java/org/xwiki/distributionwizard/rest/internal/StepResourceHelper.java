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

import org.xwiki.component.annotation.Component;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.distributionwizard.rest.model.jaxb.Step;
import org.xwiki.distributionwizard.rest.model.jaxb.UIComponent;

import jakarta.inject.Singleton;

@Component(roles = StepResourceHelper.class)
@Singleton
public class StepResourceHelper
{
    public Step toStep(DistributionWizardStep wizardStep) throws DistributionWizardException
    {
        Step step = new Step();
        step.setId(wizardStep.getHint());
        step.setHidden(wizardStep.isHidden());
        step.setIndex(wizardStep.getIndex());
        step.setOptional(wizardStep.isOptional());
        step.setTitle(wizardStep.getTitle());
        step.setDone(wizardStep.isStepDone());
        UIComponent uiComponent = new UIComponent();
        uiComponent.setComponent(wizardStep.getUIComponentName());
        uiComponent.setModule(wizardStep.getUIComponentModule());
        step.setUiComponent(uiComponent);
        return step;
    }
}
