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
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.job.DistributionStepStatus.UpdateState;
import org.xwiki.extension.repository.InstalledExtensionRepository;

/**
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("distribution")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class FarmDistributionJob extends AbstractDistributionJob<DistributionRequest, FarmDistributionJobStatus>
{
    @Inject
    private InstalledExtensionRepository installedRepository;

    @Override
    protected List<DistributionStepStatus> createSteps()
    {
        List<DistributionStepStatus> steps = new ArrayList<DistributionStepStatus>(3);

        ExtensionId extensionUI = this.distributionManager.getMainUIExtensionId();

        // FIXME: using "xwiki" directly is cheating but there is no way to get the official main wiki at this
        // level yet. Using "xwiki" since in pratice there is no way to change the main wiki
        String namespace = "wiki:xwiki";

        // Step 1: Install/upgrade main wiki UI

        DistributionStepStatus step1 = new DistributionStepStatus("extension.mainui");
        steps.add(step1);
        // Only if the UI is not already installed
        step1.setUpdateState(UpdateState.COMPLETED);
        if (extensionUI != null) {
            InstalledExtension installedExtension =
                this.installedRepository.getInstalledExtension(extensionUI.getId(), namespace);
            if (installedExtension == null || !installedExtension.getId().getVersion().equals(extensionUI.getVersion())) {
                step1.setUpdateState(null);
            }
        }

        // Step 2: Wiki distribution mode

        // TODO: add a step to decide if distribution wyzard is farm based or wiki based

        // Step 2: Upgrade outdated extensions

        DistributionStepStatus step2 = new DistributionStepStatus("extension.outdatedextensions");
        steps.add(step2);
        step2.setUpdateState(UpdateState.COMPLETED);
        // Upgrade outdated extensions only when there is outdated extensions
        for (InstalledExtension extension : this.installedRepository.getInstalledExtensions()) {
            Collection<String> installedNamespaces = extension.getNamespaces();
            if (installedNamespaces == null) {
                if (!extension.isValid(null)) {
                    step2.setUpdateState(null);
                    break;
                }
            } else {
                for (String installedNamespace : installedNamespaces) {
                    if (!extension.isValid(installedNamespace)) {
                        step2.setUpdateState(null);
                        break;
                    }
                }
            }
        }

        return steps;
    }

    @Override
    protected FarmDistributionJobStatus createNewDistributionStatus(DistributionRequest request,
        List<DistributionStepStatus> steps)
    {
        return new FarmDistributionJobStatus(request, this.observationManager, this.loggerManager, steps);
    }
}
