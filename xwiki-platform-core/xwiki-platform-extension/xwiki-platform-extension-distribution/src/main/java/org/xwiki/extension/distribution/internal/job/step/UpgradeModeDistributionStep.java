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
package org.xwiki.extension.distribution.internal.job.step;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;

@Component
@Named(UpgradeModeDistributionStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class UpgradeModeDistributionStep extends AbstractDistributionStep
{
    public static final String ID = "upgrademode";

    public enum UpgradeMode
    {
        WIKI,
        ALLINONE
    }

    private UpgradeMode upgradeMode = UpgradeMode.WIKI;

    public UpgradeModeDistributionStep()
    {
        super(ID);
    }

    @Override
    public void initialize(DistributionJob distributionJob)
    {
        // TODO: find some better rule
        // CANCELED by default, will be enabled only if it's enabled in the status or if another step is
        setState(State.CANCELED);

        super.initialize(distributionJob);

        // Initialize upgrade mode with saved one
        DistributionJobStatus< ? > previousStatus = this.distributionJob.getPreviousStatus();

        if (previousStatus != null) {
            UpgradeModeDistributionStep previousStep =
                (UpgradeModeDistributionStep) previousStatus.getStep(UpgradeModeDistributionStep.ID);

            if (previousStep != null) {
                setUpgradeMode(previousStep.getUpgradeMode());
            }
        }
    }

    @Override
    public void prepare()
    {
        // Nothing to do
    }

    public UpgradeMode getUpgradeMode()
    {
        return this.upgradeMode;
    }

    public void setUpgradeMode(UpgradeMode upgradeMode)
    {
        this.upgradeMode = upgradeMode;
    }
}
