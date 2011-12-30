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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.DefaultExtensionDependency;
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
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;
import org.xwiki.extension.version.IncompatibleVersionConstraintException;
import org.xwiki.extension.version.VersionConstraint;

/**
 * Extension installation related task.
 * <p>
 * This task generates related events.
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
     * Used to install the extension itself depending of its type.
     */
    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

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
    private LocalExtension installExtension(ExtensionId extensionId, String namespace) throws InstallException
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
            throw new InstallException(MessageFormat.format("There is already a core extension with the id [{0}]",
                extensionId.getId()));
        }

        if (namespace != null) {
            this.logger.info("Installing extension [{}] on namespace [{}]", extensionId, namespace);
        } else {
            this.logger.info("Installing extension [{}]", extensionId);
        }

        LocalExtension previousExtension = null;

        LocalExtension localExtension =
            this.localExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
        if (localExtension != null) {
            this.logger.info("Found already installed extension with id [{}]. Checking compatibility.", extensionId);

            if (extensionId.getVersion() == null) {
                throw new InstallException(MessageFormat.format("The extension with id [{0}] is already installed",
                    extensionId.getId()));
            }

            int diff = extensionId.getVersion().compareTo(localExtension.getId().getVersion());

            if (diff == 0) {
                throw new InstallException(
                    MessageFormat.format("The extension [{0}] is already installed", extensionId));
            } else if (diff < 0) {
                throw new InstallException(MessageFormat.format("A more recent version of [{0}] is already installed",
                    extensionId.getId()));
            } else {
                // upgrade
                previousExtension = localExtension;
            }
        }

        LocalExtension installedExtension = installExtension(previousExtension, extensionId, dependency, namespace);

        return installedExtension;
    }

    // TODO: support version range

    /**
     * Install provided extension dependency.
     * 
     * @param extensionDependency the extension dependency to install
     * @param namespace the namespace where to install the extension
     * @throws InstallException error when trying to install provided extension
     */
    private void installExtensionDependency(ExtensionDependency extensionDependency, String namespace)
        throws InstallException
    {
        if (this.coreExtensionRepository.exists(extensionDependency.getId())) {
            return;
        }

        if (namespace != null) {
            this.logger.info("Installing extension dependency [{}] on namespace [{}]", extensionDependency, namespace);
        } else {
            this.logger.info("Installing extension dependency [{}]", extensionDependency);
        }

        LocalExtension previousExtension = null;

        ExtensionDependency targetDependency = extensionDependency;
        LocalExtension localExtension =
            this.localExtensionRepository.getInstalledExtension(extensionDependency.getId(), namespace);
        if (localExtension != null) {
            if (extensionDependency.getVersionConstraint().containsVersion(localExtension.getId().getVersion())) {
                return;
            }

            VersionConstraint mergedVersionContraint;
            try {
                if (localExtension.isInstalled(null)) {
                    Map<String, Collection<LocalExtension>> backwardDependencies =
                        this.localExtensionRepository.getBackwardDependencies(localExtension.getId());

                    mergedVersionContraint =
                        mergeVersionConstraints(backwardDependencies.get(null), extensionDependency.getId(),
                            extensionDependency.getVersionConstraint());
                    if (namespace != null) {
                        mergedVersionContraint =
                            mergeVersionConstraints(backwardDependencies.get(namespace), extensionDependency.getId(),
                                mergedVersionContraint);
                    }
                } else {
                    Collection<LocalExtension> backwardDependencies =
                        this.localExtensionRepository
                            .getBackwardDependencies(localExtension.getId().getId(), namespace);

                    mergedVersionContraint =
                        mergeVersionConstraints(backwardDependencies, extensionDependency.getId(),
                            extensionDependency.getVersionConstraint());
                }
            } catch (IncompatibleVersionConstraintException e) {
                throw new InstallException("Provided depency is incompatible with already installed extensions", e);
            } catch (ResolveException e) {
                throw new InstallException("Failed to resolve backward dependencies", e);
            }

            if (mergedVersionContraint != extensionDependency.getVersionConstraint()) {
                targetDependency = new DefaultExtensionDependency(extensionDependency, mergedVersionContraint);
            }
        }

        installExtension(previousExtension, targetDependency, true, namespace);
    }

    /**
     * Install provided extension.
     * 
     * @param previousExtension the previous installed version of the extension to install
     * @param targetDependency used to search the extension to install in remote repositories
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @throws InstallException error when trying to install provided extension
     */
    private void installExtension(LocalExtension previousExtension, ExtensionDependency targetDependency,
        boolean dependency, String namespace) throws InstallException
    {
        notifyPushLevelProgress(2);

        try {
            // Check is the extension is already in local repository
            Extension extension = resolveExtension(targetDependency);

            notifyStepPropress();

            try {
                installExtension(previousExtension, extension, dependency, namespace);
            } catch (Exception e) {
                throw new InstallException("Failed to install extension dependency", e);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensions the extensions containing the dependencies for which to merge the constraints
     * @param dependencyId the id of the dependency
     * @param previousMergedVersionContraint if not null it's merged with the provided extension dependencies version
     *            constraints
     * @return the merged version constraint
     * @throws IncompatibleVersionConstraintException the provided version constraint is compatible with the provided
     *             version constraint
     */
    private VersionConstraint mergeVersionConstraints(Collection< ? extends Extension> extensions, String dependencyId,
        VersionConstraint previousMergedVersionContraint) throws IncompatibleVersionConstraintException
    {
        VersionConstraint mergedVersionContraint = previousMergedVersionContraint;

        if (extensions != null) {
            for (Extension extension : extensions) {
                ExtensionDependency dependency = getDependency(extension, dependencyId);

                if (dependency != null) {
                    if (mergedVersionContraint == null) {
                        mergedVersionContraint = dependency.getVersionConstraint();
                    } else {
                        mergedVersionContraint = mergedVersionContraint.merge(dependency.getVersionConstraint());
                    }
                }
            }
        }

        return mergedVersionContraint;
    }

    /**
     * Extract extension with the provided id from the provided extension.
     * 
     * @param extension the extension
     * @param dependencyId the id of the depency
     * @return the extension dependency or null if none has been found
     */
    private ExtensionDependency getDependency(Extension extension, String dependencyId)
    {
        for (ExtensionDependency dependency : extension.getDependencies()) {
            if (dependency.getId().equals(dependencyId)) {
                return dependency;
            }
        }

        return null;
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
            // Check is the extension is already in local repository
            Extension extension = resolveExtension(extensionId);

            notifyStepPropress();

            try {
                return installExtension(previousExtension, extension, dependency, namespace);
            } catch (Exception e) {
                throw new InstallException("Failed to install extension", e);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensionId the identifier of the extension to install
     * @return the extension
     * @throws InstallException error when trying to resolve extension
     */
    private Extension resolveExtension(ExtensionId extensionId) throws InstallException
    {
        // Check is the extension is already in local repository
        Extension extension;
        try {
            extension = this.localExtensionRepository.resolve(extensionId);
        } catch (ResolveException e) {
            this.logger.debug("Can't find extension in local repository, trying to download it.", e);

            // Resolve extension
            try {
                extension = this.repositoryManager.resolve(extensionId);
            } catch (ResolveException e1) {
                throw new InstallException(MessageFormat.format("Failed to resolve extension [{0}]", extensionId), e1);
            }
        }

        return extension;
    }

    /**
     * @param extensionDependency describe the extension to install
     * @return the extension
     * @throws InstallException error when trying to resolve extension
     */
    private Extension resolveExtension(ExtensionDependency extensionDependency) throws InstallException
    {
        // Check is the extension is already in local repository
        Extension extension;
        try {
            extension = this.localExtensionRepository.resolve(extensionDependency);
        } catch (ResolveException e) {
            this.logger.debug("Can't find extension dependency in local repository, trying to download it.", e);

            // Resolve extension
            try {
                extension = this.repositoryManager.resolve(extensionDependency);
            } catch (ResolveException e1) {
                throw new InstallException(MessageFormat.format("Failed to resolve extension dependency [{0}]",
                    extensionDependency), e1);
            }
        }

        return extension;
    }

    /**
     * @param previousExtension the previous installed version of the extension to install
     * @param extension the new extension to install
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @return the newly installed local extension
     * @throws ComponentLookupException failed to find proper {@link org.xwiki.extension.handler.ExtensionHandler}
     * @throws InstallException error when trying to install provided extension
     * @throws LocalExtensionRepositoryException error when storing extension
     */
    private LocalExtension installExtension(LocalExtension previousExtension, Extension extension, boolean dependency,
        String namespace) throws ComponentLookupException, InstallException, LocalExtensionRepositoryException
    {
        for (ExtensionDependency dependencyDependency : extension.getDependencies()) {
            installExtensionDependency(dependencyDependency, namespace);
        }

        notifyPushLevelProgress(2);

        try {
            // Store extension in local repository
            LocalExtension localExtension;
            if (extension instanceof LocalExtension) {
                localExtension = (LocalExtension) extension;
            } else {
                localExtension = this.localExtensionRepository.storeExtension(extension);
            }

            notifyStepPropress();

            if (previousExtension != null) {
                this.extensionHandlerManager.upgrade(previousExtension, localExtension, namespace);

                try {
                    this.localExtensionRepository.uninstallExtension(previousExtension, namespace);
                } catch (UninstallException e) {
                    this.logger.error("Failed to uninstall extension [" + previousExtension + "]", e);
                }

                this.localExtensionRepository.installExtension(localExtension, namespace, dependency);

                this.observationManager.notify(new ExtensionUpgradedEvent(localExtension.getId()), localExtension,
                    previousExtension);
            } else {
                this.extensionHandlerManager.install(localExtension, namespace);

                this.localExtensionRepository.installExtension(localExtension, namespace, dependency);

                this.observationManager.notify(new ExtensionInstalledEvent(localExtension.getId()), localExtension,
                    previousExtension);
            }

            if (namespace != null) {
                this.logger.info("Successfully installed extension [{}] on namespace [{}]", localExtension, namespace);
            } else {
                this.logger.info("Successfully installed extension [{}]", localExtension);
            }

            return localExtension;
        } finally {
            notifyPopLevelProgress();
        }
    }
}
