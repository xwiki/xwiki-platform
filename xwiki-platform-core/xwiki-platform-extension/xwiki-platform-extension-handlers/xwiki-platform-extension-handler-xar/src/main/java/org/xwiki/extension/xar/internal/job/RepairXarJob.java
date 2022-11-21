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
package org.xwiki.extension.xar.internal.job;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.event.ExtensionInstallFailedEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionInstallingEvent;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.AbstractExtensionJob;
import org.xwiki.extension.job.internal.ExtensionPlanContext;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.logging.marker.BeginTranslationMarker;
import org.xwiki.logging.marker.EndTranslationMarker;

/**
 * Make sure the provided XAR extension properly is registered in the installed extensions index.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named(RepairXarJob.JOBTYPE)
public class RepairXarJob extends AbstractExtensionJob<InstallRequest, DefaultJobStatus<InstallRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "repairxar";

    private static final BeginTranslationMarker LOG_REPAIR_BEGIN =
        new BeginTranslationMarker("extension.xar.log.repair.begin");

    private static final BeginTranslationMarker LOG_REPAIR_NAMESPACE_BEGIN =
        new BeginTranslationMarker("extension.xar.log.repair.begin.namespace");

    private static final EndTranslationMarker LOG_REPAIR_END = new EndTranslationMarker("extension.xar.log.repair.end");

    private static final EndTranslationMarker LOG_REPAIR_END_NAMESPACE =
        new EndTranslationMarker("extension.xar.log.repair.end.namespace");

    /**
     * Used to resolve extensions to install.
     */
    @Inject
    protected ExtensionRepositoryManager repositoryManager;

    /**
     * Used to set a local extension as installed.
     */
    @Inject
    private InstalledExtensionRepository installedRepository;

    /**
     * Used to store downloaded extensions.
     */
    @Inject
    private LocalExtensionRepository localRepository;

    @Inject
    private CoreExtensionRepository coreRepository;

    @Inject
    private ExtensionManagerConfiguration configuration;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        this.progressManager.pushLevelProgress(getRequest().getExtensions().size(), this);

        try {
            for (ExtensionId extensionId : getRequest().getExtensions()) {
                this.progressManager.startStep(this);

                if (getRequest().getNamespaces() != null) {
                    this.progressManager.pushLevelProgress(getRequest().getNamespaces().size(), this);

                    try {
                        for (String namespace : getRequest().getNamespaces()) {
                            this.progressManager.startStep(this);

                            repairExtension(extensionId, namespace, false, new ExtensionPlanContext());

                            this.progressManager.endStep(this);
                        }
                    } finally {
                        this.progressManager.popLevelProgress(this);
                    }
                } else {
                    repairExtension(extensionId, null, false, new ExtensionPlanContext());
                }

                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    /**
     * @param extensionId the extension unique identifier
     * @return the stored local extension
     * @throws InstallException failed to store extension
     */
    private LocalExtension getLocalXARExtension(ExtensionId extensionId) throws InstallException
    {
        LocalExtension localExtension = this.localRepository.getLocalExtension(extensionId);

        if (localExtension == null) {
            this.progressManager.pushLevelProgress(2, this);

            try {
                this.progressManager.startStep(this);

                Extension extension = this.repositoryManager.resolve(extensionId);

                this.progressManager.endStep(this);

                this.progressManager.startStep(this);

                if (XarExtensionHandler.TYPE.equals(extension.getType())) {
                    localExtension = this.localExtensionRepository.storeExtension(extension);
                }

                this.progressManager.endStep(this);
            } catch (ResolveException e) {
                throw new InstallException("Failed to find extension", e);
            } catch (LocalExtensionRepositoryException e) {
                throw new InstallException("Failed save extension in local repository", e);
            } finally {
                this.progressManager.popLevelProgress(this);
            }
        } else if (!localExtension.getType().equals(XarExtensionHandler.TYPE)) {
            localExtension = null;
        }

        return localExtension;
    }

    /**
     * @param extensionId the unique extension identifier
     * @param namespace the namespace where to install extension
     * @param dependency indicate of the extension is installed as a dependency of another
     * @param extensionContext the current extension context
     * @throws InstallException failed to repair extension
     */
    private void repairExtension(ExtensionId extensionId, String namespace, boolean dependency,
        ExtensionPlanContext extensionContext) throws InstallException
    {
        if (this.installedRepository.getInstalledExtension(extensionId.getId(), namespace) != null) {
            this.logger.debug("Extension [{}] already installed on namespace [{}]", extensionId.getId(), namespace);

            return;
        }

        if (getRequest().isVerbose()) {
            if (namespace != null) {
                this.logger.info(LOG_REPAIR_NAMESPACE_BEGIN, "Repairing XAR extension [{}] on namespace [{}]",
                    extensionId, namespace);
            } else {
                this.logger.info(LOG_REPAIR_BEGIN, "Repairing XAR extension [{}] on all namespaces", extensionId);
            }
        }

        this.progressManager.pushLevelProgress(2, this);

        try {
            this.progressManager.startStep(this);
            LocalExtension localExtension = getLocalXARExtension(extensionId);
            this.progressManager.endStep(this);

            this.progressManager.startStep(this);
            if (localExtension != null) {
                repairExtension(localExtension, namespace, dependency, extensionContext);
            }
        } finally {
            if (getRequest().isVerbose()) {
                if (namespace != null) {
                    this.logger.info(LOG_REPAIR_END_NAMESPACE, "Done repairing XAR extension [{}] on namespace [{}]",
                        extensionId, namespace);
                } else {
                    this.logger.info(LOG_REPAIR_END, "Done repairing XAR extension [{}] on all namespaces",
                        extensionId);
                }
            }

            this.progressManager.popLevelProgress(this);
        }
    }

    /**
     * @param localExtension the local extension to install
     * @param namespace the namespace where to install extension
     * @param dependency indicate of the extension is installed as a dependency of another
     * @param extensionContext the current extension context
     * @throws InstallException failed to repair extension
     */
    private void repairExtension(LocalExtension localExtension, String namespace, boolean dependency,
        ExtensionPlanContext extensionContext) throws InstallException
    {
        this.progressManager.pushLevelProgress(2, this);

        this.observationManager.notify(new ExtensionInstallingEvent(localExtension.getId(), namespace), localExtension);

        InstalledExtension installedExtension = null;
        try {
            this.progressManager.startStep(this);

            Collection<? extends ExtensionDependency> dependencies = localExtension.getDependencies();

            if (!dependencies.isEmpty()) {
                this.progressManager.pushLevelProgress(dependencies.size(), dependencies);

                try {
                    for (ExtensionDependency extensionDependency : dependencies) {
                        this.progressManager.startStep(dependencies);

                        // Is ignored
                        if (this.configuration.isIgnoredDependency(extensionDependency)) {
                            continue;
                        }

                        // Replace with managed dependency if any
                        ExtensionDependency resolvedDependency =
                            extensionContext.getDependency(extensionDependency, localExtension);

                        // Check for excludes
                        if (!extensionContext.isExcluded(resolvedDependency)) {
                            repairDependency(resolvedDependency, namespace,
                                new ExtensionPlanContext(extensionContext, localExtension));
                        }

                        this.progressManager.endStep(dependencies);
                    }
                } finally {
                    this.progressManager.popLevelProgress(dependencies);
                }
            }

            this.progressManager.endStep(this);

            this.progressManager.startStep(this);

            installedExtension = this.installedRepository.installExtension(localExtension, namespace, dependency);
        } finally {
            if (installedExtension != null) {
                this.observationManager.notify(new ExtensionInstalledEvent(installedExtension.getId(), namespace),
                    installedExtension);
            } else {
                this.observationManager.notify(new ExtensionInstallFailedEvent(localExtension.getId(), namespace),
                    localExtension);
            }

            this.progressManager.popLevelProgress(this);
        }
    }

    /**
     * @param extensionDependency the extension dependency to install
     * @param namespace the namespace where to install extension
     * @param extensionContext the current extension context
     */
    private void repairDependency(ExtensionDependency extensionDependency, String namespace,
        ExtensionPlanContext extensionContext)
    {
        // Skip core extensions
        if (this.coreRepository.getCoreExtension(extensionDependency.getId()) == null) {
            // TODO: take into account managed dependencies
            if (extensionDependency.getVersionConstraint().getVersion() == null) {
                this.logger.warn(
                    "Can't repair extension dependency [{}] with version range ([{}])"
                        + " since there is no way to know what has been installed",
                    extensionDependency.getId(), extensionDependency.getVersionConstraint());

            } else {
                try {
                    repairExtension(
                        new ExtensionId(extensionDependency.getId(),
                            extensionDependency.getVersionConstraint().getVersion()),
                        namespace, true, new ExtensionPlanContext(extensionContext, extensionDependency));
                } catch (InstallException e) {
                    this.logger.warn("Failed to repair dependency [{}]", extensionDependency, e);
                }
            }
        }
    }
}
