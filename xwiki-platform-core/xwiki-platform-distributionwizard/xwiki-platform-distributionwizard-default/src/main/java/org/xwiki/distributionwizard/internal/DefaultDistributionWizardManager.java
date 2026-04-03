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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.distributionwizard.DistributionWizardManager;
import org.xwiki.distributionwizard.DistributionWizardStep;
import org.xwiki.extension.distribution.internal.DistributionManager;

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
            for (DistributionWizardStep step : instanceList) {
                if (!step.isStepDone()) {
                    resultList.add(step);
                }
            }
            resultList.sort(Comparator.comparingInt(DistributionWizardStep::getIndex));
            return resultList;
        } catch (ComponentLookupException e) {
            throw new DistributionWizardException("Error while loading the list of steps", e);
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
        return this.distributionManager.canDisplayDistributionWizard();
    }
}
