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
package org.xwiki.extension.job.internal;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * Extension installation related task.
 * <p>
 * This task is taking care of discovering automatically if the extension need to be upgraded instead of installed. It
 * also generated related events.
 * 
 * @version $Id$
 */
@Component
@Named("install")
public class InstallJob extends AbstractJob<InstallRequest>
{
    /**
     * Used to resolve extensions to install.
     */
    @Inject
    private ExtensionRepositoryManager repositoryManager;

    /**
     * Used to check if extension or its dependencies are already core extensions.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * Used to manipulate local extension repository.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * Used to compare version of upgraded extensions.
     */
    @Inject
    private VersionManager versionManager;

    /**
     * Used to install the extension itself depending of its type.
     */
    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.internal.AbstractJob#start()
     */
    @Override
    protected void start() throws Exception
    {
        List<ExtensionId> extensions = getRequest().getExtensions();

        notifyPushLevelProgress(extensions.size());

        try {
            for (ExtensionId extensionId : extensions) {
                if (getRequest().hasNamespaces()) {
                    List<String> namespaces = getRequest().getNamespaces();

                    notifyPushLevelProgress(namespaces.size());

                    try {
                        for (String namespace : namespaces) {
                            installExtension(extensionId, namespace);

                            notifyStepPropress();
                        }
                    } finally {
                        notifyPopLevelProgress();
                    }
                } else {
                    installExtension(extensionId, null);
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Install provided extension.
     * 
     * @param extensionId the identifier of the extension to install
     * @param namespace the namespace where to install the extension
     * @return the newly installed local extension
     * @throws InstallException error when trying to install provided extension
     */
    public LocalExtension installExtension(ExtensionId extensionId, String namespace) throws InstallException
    {
        return installExtension(extensionId, false, namespace);
    }

    /**
     * Install provided extension.
     * 
     * @param extensionId the identifier of the extension to install
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @return the newly installed local extension
     * @throws InstallException error when trying to install provided extension
     */
    private LocalExtension installExtension(ExtensionId extensionId, boolean dependency, String namespace)
        throws InstallException
    {
        if (this.coreExtensionRepository.exists(extensionId.getId())) {
            throw new InstallException(MessageFormat.format("[{0}]: core extension", extensionId.getId()));
        }

        if (namespace != null) {
            this.logger.info("Installing extension [{0}] on namespace [{1}]", extensionId, namespace);
        } else {
            this.logger.info("Installing extension [{0}]", extensionId);
        }

        LocalExtension previousExtension = null;

        LocalExtension localExtension =
            this.localExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
        if (localExtension != null) {
            int diff =
                this.versionManager.compareVersions(extensionId.getVersion(), localExtension.getId().getVersion());

            if (diff == 0) {
                throw new InstallException(MessageFormat.format("[{0}]: already installed", extensionId.getId()));
            } else if (diff < 0) {
                throw new InstallException(MessageFormat.format("[{0}]: a more recent version is already installed",
                    extensionId.getId()));
            } else {
                // upgrade
                previousExtension = localExtension;
            }
        }

        return installExtension(previousExtension, extensionId, dependency, namespace);
    }

    // TODO: support version range

    /**
     * Install provided extension dependency.
     * 
     * @param extensionDependency the extension dependency to install
     * @param namespace the namespace where to install the extension
     * @return the newly installed local extension
     * @throws InstallException error when trying to install provided extension
     */
    private LocalExtension installExtensionDependency(ExtensionDependency extensionDependency, String namespace)
        throws InstallException
    {
        if (this.coreExtensionRepository.exists(extensionDependency.getId())) {
            return null;
        }

        LocalExtension previousExtension = null;

        LocalExtension localExtension =
            this.localExtensionRepository.getInstalledExtension(extensionDependency.getId(), namespace);
        if (localExtension != null) {
            int diff =
                this.versionManager.compareVersions(extensionDependency.getVersion(), localExtension.getId()
                    .getVersion());

            if (diff > 0) {
                // upgrade
                previousExtension = localExtension;
            } else {
                return null;
            }
        }

        return installExtension(previousExtension,
            new ExtensionId(extensionDependency.getId(), extensionDependency.getVersion()), true, namespace);
    }

    /**
     * Install provided extension.
     * 
     * @param previousExtension the previous installed version of the extension to install
     * @param extensionId the identifier of the extension to install
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @return the newly installed local extension
     * @throws InstallException error when trying to install provided extension
     */
    private LocalExtension installExtension(LocalExtension previousExtension, ExtensionId extensionId,
        boolean dependency, String namespace) throws InstallException
    {
        notifyPushLevelProgress(2);

        try {
            // Resolve extension
            Extension remoteExtension;
            try {
                remoteExtension = this.repositoryManager.resolve(extensionId);
            } catch (ResolveException e) {
                throw new InstallException(MessageFormat.format("Failed to resolve extension [{0}]", extensionId), e);
            }

            notifyStepPropress();

            try {
                return installExtension(previousExtension, remoteExtension, dependency, namespace);
            } catch (Exception e) {
                throw new InstallException("Failed to install extension", e);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param previousExtension the previous installed version of the extension to install
     * @param remoteExtension the new extension to install
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @return the newly installed local extension
     * @throws ComponentLookupException failed to find proper {@link org.xwiki.extension.handler.ExtensionHandler}
     * @throws InstallException error when trying to install provided extension
     */
    private LocalExtension installExtension(LocalExtension previousExtension, Extension remoteExtension,
        boolean dependency, String namespace) throws ComponentLookupException, InstallException
    {
        for (ExtensionDependency dependencyDependency : remoteExtension.getDependencies()) {
            installExtensionDependency(dependencyDependency, namespace);
        }

        notifyPushLevelProgress(2);

        try {
            // Store extension in local repository
            LocalExtension localExtension =
                this.localExtensionRepository.installExtension(remoteExtension, previousExtension != null
                    ? previousExtension.isDependency() : dependency, namespace);

            notifyStepPropress();

            if (previousExtension != null) {
                this.extensionHandlerManager.upgrade(previousExtension, localExtension, namespace);

                try {
                    this.localExtensionRepository.uninstallExtension(previousExtension, namespace);
                } catch (UninstallException e) {
                    this.logger.error("Failed to uninstall extension [" + previousExtension + "]", e);
                }

                this.observationManager.notify(new ExtensionUpgradedEvent(localExtension.getId()), localExtension,
                    previousExtension);
            } else {
                this.extensionHandlerManager.install(localExtension, namespace);

                this.observationManager.notify(new ExtensionInstalledEvent(localExtension.getId()), localExtension,
                    previousExtension);
            }

            return localExtension;
        } finally {
            notifyPopLevelProgress();
        }
    }
}
