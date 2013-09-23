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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.step.DefaultUIDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.extension.distribution.internal.job.step.OutdatedExtensionsDistributionStep;

/**
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("distribution")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class FarmDistributionJob extends AbstractDistributionJob<DistributionRequest, FarmDistributionJobStatus>
{
    @Override
    protected List<DistributionStep> createSteps()
    {
        List<DistributionStep> steps = new ArrayList<DistributionStep>(3);

        // Step 1: Install/upgrade main wiki UI

        try {
            steps.add(this.componentManager.<DistributionStep> getInstance(DistributionStep.class,
                DefaultUIDistributionStep.ID));
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get default UI step instance");
        }

        // Step 2: Upgrade mode

        /* TODO: enabled when the UI will be a bit more usable in all in one mode
        try {
            steps.add(this.componentManager.<DistributionStep> getInstance(DistributionStep.class,
                UpgradeModeDistributionStep.ID));
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get upgrade mode step instance");
        }
        */

        // Step 3: Upgrade outdated extensions

        try {
            steps.add(this.componentManager.<DistributionStep> getInstance(DistributionStep.class,
                OutdatedExtensionsDistributionStep.ID));
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get outdated extensions step instance");
        }

        return steps;
    }

    @Override
    public DistributionJobStatus< ? > getPreviousStatus()
    {
        return this.distributionManager.getPreviousFarmJobStatus();
    }

    @Override
    public ExtensionId getUIExtensionId()
    {
        return this.distributionManager.getMainUIExtensionId();
    }

    @Override
    protected FarmDistributionJobStatus createNewDistributionStatus(DistributionRequest request,
        List<DistributionStep> steps)
    {
        return new FarmDistributionJobStatus(request, this.observationManager, this.loggerManager, steps);
    }
}
