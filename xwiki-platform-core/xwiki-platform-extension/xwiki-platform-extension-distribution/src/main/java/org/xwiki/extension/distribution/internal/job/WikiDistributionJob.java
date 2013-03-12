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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.job.step.DistributionStep;
import org.xwiki.extension.distribution.internal.job.step.DefaultUIDistributionStep;
import org.xwiki.extension.distribution.internal.job.step.OutdatedExtensionsDistributionStep;
import org.xwiki.extension.repository.InstalledExtensionRepository;

/**
 * @version $Id$
 * @since 5.0M1
 */
@Component
@Named("wikidistribution")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikiDistributionJob extends AbstractDistributionJob<WikiDistributionRequest, WikiDistributionJobStatus>
{
    @Inject
    private InstalledExtensionRepository installedRepository;

    @Override
    protected List<DistributionStep> createSteps()
    {
        List<DistributionStep> steps = new ArrayList<DistributionStep>(3);

        ExtensionId extensionUI = this.distributionManager.getMainUIExtensionId();

        String namespace = "wiki:" + getRequest().getWiki();

        // Step 1: Install/upgrade main wiki UI

        try {
            DistributionStep step1 =
                this.componentManager.getInstance(DistributionStep.class, DefaultUIDistributionStep.ID);

            steps.add(step1);
            step1.setState(DistributionStep.State.COMPLETED);
            // Only if the UI is not already installed
            if (extensionUI != null) {
                InstalledExtension installedExtension =
                    this.installedRepository.getInstalledExtension(extensionUI.getId(), namespace);
                if (installedExtension == null
                    || !installedExtension.getId().getVersion().equals(extensionUI.getVersion())) {
                    step1.setState(null);
                }
            }
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get step instance for id [{}]", DefaultUIDistributionStep.ID);
        }

        // Step 2: Upgrade outdated extensions

        try {
            DistributionStep step2 =
                this.componentManager.getInstance(DistributionStep.class, OutdatedExtensionsDistributionStep.ID);

            steps.add(step2);
            step2.setState(DistributionStep.State.COMPLETED);
            // Upgrade outdated extensions only when there is outdated extensions
            for (InstalledExtension extension : this.installedRepository.getInstalledExtensions(namespace)) {
                if (!extension.isValid(namespace)) {
                    step2.setState(null);
                    break;
                }
            }
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get step instance for id [{}]", OutdatedExtensionsDistributionStep.ID);
        }

        return steps;
    }

    @Override
    protected WikiDistributionJobStatus createNewDistributionStatus(WikiDistributionRequest request,
        List<DistributionStep> steps)
    {
        return new WikiDistributionJobStatus(request, this.observationManager, this.loggerManager, steps);
    }
}
