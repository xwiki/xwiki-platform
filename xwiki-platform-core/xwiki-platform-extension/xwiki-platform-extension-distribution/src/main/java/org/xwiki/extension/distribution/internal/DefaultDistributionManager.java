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
package org.xwiki.extension.distribution.internal;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.DistributionRequest;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.internal.core.MavenCoreExtension;
import org.xwiki.job.Job;
import org.xwiki.job.JobManager;

@Component
public class DefaultDistributionManager implements DistributionManager, Initializable
{
    /**
     * The repository with core modules provided by the platform.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private JobManager jobManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private CoreExtension distributionExtension;

    private ExtensionId uiExtensionId;

    private DistributionJobStatus previousStatus;

    private DistributionState distributionState;

    private DistributionJob distributionJob;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the current distribution
        this.distributionExtension = this.coreExtensionRepository.getEnvironmentExtension();

        // Get previous state
        this.previousStatus = (DistributionJobStatus) this.jobManager.getJobStatus("distribution");

        // Determine distribution status
        if (this.distributionExtension != null) {
            // Distribution state
            if (this.previousStatus == null) {
                this.distributionState = DistributionState.NEW;
                startJob();
            } else {
                ExtensionId previousExtensionId = this.previousStatus.getDistributionExtension();
                ExtensionId distributionExtensionId = this.distributionExtension.getId();

                if (previousExtensionId.equals(distributionExtensionId)) {
                    this.distributionState = DistributionState.SAME;
                } else if (!distributionExtensionId.getId().equals(previousExtensionId.getId())
                    && !this.distributionExtension.getFeatures().contains(previousExtensionId.getId())) {
                    this.distributionState = DistributionState.DIFFERENT;
                    startJob();
                } else {
                    int diff = distributionExtensionId.getVersion().compareTo(previousExtensionId.getVersion());
                    if (diff > 0) {
                        this.distributionState = DistributionState.UPGRADE;
                        startJob();
                    } else {
                        this.distributionState = DistributionState.DOWNGRADE;
                        startJob();
                    }
                }
            }

            // Distribution UI
            Model mavenModel = (Model) this.distributionExtension.getProperty(MavenCoreExtension.PKEY_MAVEN_MODEL);

            String uiId = mavenModel.getProperties().getProperty("xwiki.extension.distribution.ui");

            this.uiExtensionId = new ExtensionId(uiId, this.distributionExtension.getId().getVersion());
        } else {
            this.distributionState = DistributionState.NONE;
        }
    }

    private void startJob()
    {
        try {
            this.distributionJob = this.componentManager.getInstance(Job.class, "distribution");
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to create distribution job");
        }

        final DistributionRequest request = new DistributionRequest();

        Thread distributionJobThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                distributionJob.start(request);
            }
        });

        distributionJobThread.setDaemon(true);
        distributionJobThread.setName("Distribution initialization");
        distributionJobThread.start();
    }

    @Override
    public DistributionState getDistributionState()
    {
        return this.distributionState;
    }

    @Override
    public CoreExtension getDistributionExtension()
    {
        return this.distributionExtension;
    }

    @Override
    public ExtensionId getUIExtensionId()
    {
        return this.uiExtensionId;
    }

    @Override
    public DistributionJobStatus getPreviousJobStatus()
    {
        return this.previousStatus;
    }

    @Override
    public DistributionJob getJob()
    {
        return this.distributionJob;
    }
}
