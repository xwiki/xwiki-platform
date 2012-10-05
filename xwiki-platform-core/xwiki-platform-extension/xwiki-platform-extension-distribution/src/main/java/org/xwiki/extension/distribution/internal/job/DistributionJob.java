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

import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionStepStatus.UpdateState;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.internal.AbstractJobStatus;

/**
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("distribution")
public class DistributionJob extends AbstractJob<DistributionRequest>
{
    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    private DistributionManager distributionManager;

    @Inject
    private InstalledExtensionRepository installedRepository;

    @Override
    public String getType()
    {
        return "distribution";
    }

    @Override
    protected AbstractJobStatus<DistributionRequest> createNewStatus(DistributionRequest request)
    {
        // TODO: make steps components automatically discovered so that any module can add custom steps

        List<DistributionStepStatus> steps = new ArrayList<DistributionStepStatus>(3);

        ExtensionId extensionUI = this.distributionManager.getUIExtensionId();

        // Step 1: Install/upgrade main wiki UI

        // Only if the UI is not already installed
        if (this.installedRepository.getInstalledExtension(extensionUI) == null) {
            steps.add(new DistributionStepStatus("extension.mainui"));
        }

        // Step 2: Upgrade outdated extensions

        // Upgrade outdated extensions only when there is outdated extensions
        for (InstalledExtension extension : this.installedRepository.getInstalledExtensions()) {
            Collection<String> namespaces = extension.getNamespaces();
            if (namespaces == null) {
                if (!extension.isValid(null)) {
                    steps.add(new DistributionStepStatus("extension.outdatedextensions"));
                    break;
                }
            } else {
                for (String namespace : namespaces) {
                    if (!extension.isValid(namespace)) {
                        steps.add(new DistributionStepStatus("extension.outdatedextensions"));
                        break;
                    }
                }
            }
        }

        // Step 0: A welcome message. Only if there is actually something to do
        if (!steps.isEmpty()) {
            steps.add(0, new DistributionStepStatus("welcome"));
        }

        // Create status

        DistributionJobStatus status =
            new DistributionJobStatus(request, this.observationManager, this.loggerManager, steps);

        if (this.distributionManager.getDistributionExtension() != null) {
            DistributionJobStatus previousStatus = this.distributionManager.getPreviousJobStatus();

            if (previousStatus != null
                && previousStatus.getDistributionExtension() != null
                && !ObjectUtils.equals(previousStatus.getDistributionExtension(),
                    this.distributionManager.getDistributionExtension())) {
                status.setDistributionExtension(previousStatus.getDistributionExtension());
                status.setDistributionExtensionUi(previousStatus.getDistributionExtensionUi());
            }

            status.setDistributionExtension(this.distributionManager.getDistributionExtension().getId());
            status.setDistributionExtensionUi(extensionUI);
        }

        return status;
    }

    /**
     * @return the distribution job status
     */
    protected DistributionJobStatus getDistributionJobStatus()
    {
        return (DistributionJobStatus) getStatus();
    }

    @Override
    protected void start() throws Exception
    {
        List<DistributionStepStatus> steps = getDistributionJobStatus().getSteps();

        notifyPushLevelProgress(steps.size());

        try {
            for (int index = 0; index < steps.size(); ++index) {
                getDistributionJobStatus().setCurrentStateIndex(index);

                DistributionStepStatus step = steps.get(index);

                if (step.getUpdateState() == null) {
                    DistributionQuestion question = new DistributionQuestion(step.getStepId());

                    // Waiting to start
                    getStatus().ask(question);

                    if (question.isSave()) {
                        switch (question.getAction()) {
                            case CANCEL_STEP:
                                step.setUpdateState(UpdateState.CANCELED);
                                break;
                            case COMPLETE_STEP:
                                step.setUpdateState(UpdateState.COMPLETED);
                                break;
                            case CANCEL:
                                for (; index < steps.size(); ++index) {
                                    steps.get(index).setUpdateState(UpdateState.CANCELED);
                                }
                            case SKIP:
                                index = steps.size() - 1;
                                break;
                            default:
                                break;
                        }
                    }
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }
}
