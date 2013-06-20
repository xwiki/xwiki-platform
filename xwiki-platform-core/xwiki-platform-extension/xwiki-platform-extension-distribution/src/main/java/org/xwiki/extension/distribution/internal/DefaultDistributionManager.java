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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.DistributionRequest;
import org.xwiki.extension.distribution.internal.job.FarmDistributionJob;
import org.xwiki.extension.distribution.internal.job.FarmDistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.WikiDistributionJob;
import org.xwiki.extension.distribution.internal.job.WikiDistributionJobStatus;
import org.xwiki.extension.distribution.internal.job.step.UpgradeModeDistributionStep.UpgradeMode;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.internal.core.MavenCoreExtension;
import org.xwiki.job.Job;
import org.xwiki.job.JobManager;
import org.xwiki.job.internal.JobStatusStorage;
import org.xwiki.logging.LoggerManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.rightsmanager.RightsManager;

/**
 * Default {@link DistributionManager} implementation.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
public class DefaultDistributionManager implements DistributionManager, Initializable
{
    private static final String JOBID = "distribution";

    /**
     * The repository with core modules provided by the platform.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private JobManager jobManager;

    /**
     * Used to lookup components dynamically.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Used to get the Execution Context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to create a new Execution Context from scratch.
     */
    @Inject
    private ExecutionContextManager executionContextManager;

    /**
     * Used to send extensions installation and upgrade related events.
     */
    @Inject
    protected Provider<ObservationManager> observationManagerProvider;

    /**
     * Used to isolate job related log.
     */
    @Inject
    protected Provider<LoggerManager> loggerManagerProvider;

    /**
     * Used to check various rights.
     */
    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Used to access current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * USed to manipulated jobs statuses.
     */
    @Inject
    private JobStatusStorage jobStatusStorage;

    @Inject
    private Logger logger;

    private CoreExtension distributionExtension;

    private ExtensionId mainUIExtensionId;

    private ExtensionId wikiUIExtensionId;

    private FarmDistributionJob farmDistributionJob;

    private Map<String, WikiDistributionJob> wikiDistributionJobs =
        new ConcurrentHashMap<String, WikiDistributionJob>();

    @Override
    public void initialize() throws InitializationException
    {
        // Get the current distribution
        this.distributionExtension = this.coreExtensionRepository.getEnvironmentExtension();

        // Determine distribution status
        if (this.distributionExtension != null) {
            // Distribution UI
            Model mavenModel = (Model) this.distributionExtension.getProperty(MavenCoreExtension.PKEY_MAVEN_MODEL);

            String mainUIId = mavenModel.getProperties().getProperty("xwiki.extension.distribution.ui");

            if (mainUIId != null) {
                this.mainUIExtensionId = new ExtensionId(mainUIId, this.distributionExtension.getId().getVersion());
            }

            String wikiUIId = mavenModel.getProperties().getProperty("xwiki.extension.distribution.wikiui");

            if (wikiUIId != null) {
                this.wikiUIExtensionId = new ExtensionId(wikiUIId, this.distributionExtension.getId().getVersion());
            }
        }
    }

    private List<String> getFarmJobId()
    {
        return Arrays.asList(JOBID);
    }

    @Override
    public FarmDistributionJob startFarmJob()
    {
        try {
            this.farmDistributionJob = this.componentManager.getInstance(Job.class, "distribution");

            final DistributionRequest request = new DistributionRequest();
            request.setId(getFarmJobId());
            // FIXME: this is sheeting but there is no API to get the main wiki name at this level
            request.setWiki("xwiki");

            Thread distributionJobThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Create a clean Execution Context
                    ExecutionContext context = new ExecutionContext();

                    try {
                        executionContextManager.initialize(context);
                    } catch (ExecutionContextException e) {
                        throw new RuntimeException("Failed to initialize farm distribution job execution context", e);
                    }

                    farmDistributionJob.start(request);
                }
            });

            distributionJobThread.setDaemon(true);
            distributionJobThread.setName("Farm distribution initialization");
            distributionJobThread.start();

            return this.farmDistributionJob;
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to create farm distribution job", e);
        }

        return null;
    }

    private List<String> getWikiJobId(String wiki)
    {
        return Arrays.asList(JOBID, "wiki", wiki);
    }

    @Override
    public WikiDistributionJob startWikiJob(String wiki)
    {
        try {
            WikiDistributionJob wikiJob = this.componentManager.getInstance(Job.class, "wikidistribution");
            this.wikiDistributionJobs.put(wiki, wikiJob);

            final DistributionRequest request = new DistributionRequest();
            request.setId(getWikiJobId(wiki));
            request.setWiki(wiki);

            Thread distributionJobThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Create a clean Execution Context
                    ExecutionContext context = new ExecutionContext();

                    try {
                        executionContextManager.initialize(context);
                    } catch (ExecutionContextException e) {
                        throw new RuntimeException("Failed to initialize wiki distribution job execution context", e);
                    }

                    wikiDistributionJobs.get(request.getWiki()).start(request);
                }
            });

            distributionJobThread.setDaemon(true);
            distributionJobThread.setName("Distribution initialization of wiki [" + wiki + "]");
            distributionJobThread.start();

            return wikiJob;
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to create distribution job for wiki [" + wiki + "]", e);
        }

        return null;
    }

    private DistributionState getDistributionState(DistributionJobStatus< ? > previousStatus)
    {
        return DistributionJobStatus.getDistributionState(
            previousStatus != null ? previousStatus.getDistributionExtension() : null,
            this.distributionExtension != null ? this.distributionExtension.getId() : null);
    }

    @Override
    public DistributionState getFarmDistributionState()
    {
        return getDistributionState(getPreviousFarmJobStatus());
    }

    @Override
    public DistributionState getWikiDistributionState(String wiki)
    {
        return getDistributionState(getPreviousWikiJobStatus(wiki));
    }

    @Override
    public CoreExtension getDistributionExtension()
    {
        return this.distributionExtension;
    }

    @Override
    public ExtensionId getMainUIExtensionId()
    {
        return this.mainUIExtensionId;
    }

    @Override
    public ExtensionId getWikiUIExtensionId()
    {
        return this.wikiUIExtensionId;
    }

    @Override
    public FarmDistributionJobStatus getPreviousFarmJobStatus()
    {
        DistributionJobStatus<DistributionRequest> jobStatus =
            (DistributionJobStatus<DistributionRequest>) this.jobManager.getJobStatus(getFarmJobId());

        FarmDistributionJobStatus farmJobStatus;
        if (jobStatus != null) {
            if (jobStatus instanceof FarmDistributionJobStatus) {
                farmJobStatus = (FarmDistributionJobStatus) jobStatus;
            } else {
                // RETRO-COMPATIBILITY: the status used to be a DistributionJobStatus
                farmJobStatus =
                    new FarmDistributionJobStatus(jobStatus, this.observationManagerProvider.get(),
                        this.loggerManagerProvider.get());
            }
        } else {
            farmJobStatus = null;
        }

        return farmJobStatus;
    }

    @Override
    public WikiDistributionJobStatus getPreviousWikiJobStatus(String wiki)
    {
        return (WikiDistributionJobStatus) this.jobManager.getJobStatus(getWikiJobId(wiki));
    }

    @Override
    public UpgradeMode getUpgradeMode()
    {
        if (this.farmDistributionJob != null) {
            FarmDistributionJobStatus status = this.farmDistributionJob.getStatus();

            if (status != null) {
                return status.getUpgradeMode();
            }
        }

        return UpgradeMode.WIKI;
    }

    @Override
    public FarmDistributionJob getFarmJob()
    {
        return this.farmDistributionJob;
    }

    @Override
    public WikiDistributionJob getWikiJob(String wiki)
    {
        return this.wikiDistributionJobs.get(wiki);
    }

    @Override
    public DistributionJob getCurrentDistributionJob()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.isMainWiki() ? getFarmJob() : getWikiJob(xcontext.getDatabase());
    }

    @Override
    public boolean canDisplayDistributionWizard()
    {
        XWikiContext xcontext = xcontextProvider.get();

        DocumentReference currentUser = xcontext.getUserReference();

        if (currentUser != null) {
            return this.authorizationManager.hasAccess(Right.ADMIN, currentUser,
                new WikiReference(xcontext.getDatabase()));
        }

        if (xcontext.isMainWiki()) {
            // If there is no user on main wiki let guest access distribution wizard
            try {
                return RightsManager.getInstance().countAllGlobalUsersOrGroups(true, null, xcontext) == 0;
            } catch (XWikiException e) {
                this.logger.error("Failed to count global users", e);
            }
        }

        return false;
    }

    @Override
    public void deletePreviousWikiJobStatus(String wiki)
    {
        this.jobStatusStorage.remove(getWikiJobId(wiki));
        this.wikiDistributionJobs.remove(wiki);
    }

    @Override
    public void copyPreviousWikiJobStatus(String sourceWiki, String targetWiki)
    {
        WikiDistributionJobStatus sourceStatus = getPreviousWikiJobStatus(sourceWiki);

        if (sourceStatus != null) {
            WikiDistributionJobStatus targetStatus =
                new WikiDistributionJobStatus(sourceStatus, this.observationManagerProvider.get(),
                    this.loggerManagerProvider.get());

            this.jobStatusStorage.store(targetStatus);
        }
    }
}
