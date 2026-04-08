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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.distributionwizard.DistributionWizardUIDefinition;
import org.xwiki.distributionwizard.rest.model.jaxb.Step;
import org.xwiki.distributionwizard.rest.model.jaxb.StepSummary;
import org.xwiki.distributionwizard.rest.model.jaxb.UIComponent;

import jakarta.inject.Singleton;

@Component(roles = StepResourceHelper.class)
@Singleton
public class StepResourceHelper
{
    public StepSummary toStepSummary(DistributionWizardStep wizardStep) throws DistributionWizardException
    {
        StepSummary stepSummary = new StepSummary();
        stepSummary.setId(wizardStep.getHint());
        stepSummary.setHidden(wizardStep.isHidden());
        stepSummary.setOptional(wizardStep.isOptional());
        stepSummary.setTitle(wizardStep.getTitle());
        stepSummary.setDone(wizardStep.isStepDone());
        stepSummary.setOriginalIndex(wizardStep.getIndex());
        return stepSummary;
    }
}
