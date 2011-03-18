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
package org.xwiki.extension.task.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionInstalled;
import org.xwiki.extension.event.ExtensionUpgraded;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.task.InstallRequest;
import org.xwiki.observation.ObservationManager;

@Component("install")
public class InstallTask extends AbstractTask<InstallRequest>
{
    @Requirement
    private ExtensionRepositoryManager repositoryManager;

    @Requirement
    private CoreExtensionRepository coreExtensionRepository;

    @Requirement
    private LocalExtensionRepository localExtensionRepository;

    @Requirement
    private VersionManager versionManager;

    @Requirement
    private ExtensionHandlerManager extensionHandlerManager;

    @Requirement
    private ObservationManager observationManager;

    @Override
    protected void start() throws Exception
    {
        for (ExtensionId extensionId : getRequest().getExtensions()) {
            if (getRequest().hasNamespaces()) {
                for (String namespace : getRequest().getNamespaces()) {
                    installExtension(extensionId, namespace);
                }
            } else {
                installExtension(extensionId, null);
            }
        }
    }

    public LocalExtension installExtension(ExtensionId extensionId, String namespace) throws InstallException
    {
        return installExtension(extensionId, false, namespace);
    }

    private LocalExtension installExtension(ExtensionId extensionId, boolean dependency, String namespace)
        throws InstallException
    {
        if (this.coreExtensionRepository.exists(extensionId.getId())) {
            throw new InstallException("[" + extensionId.getId() + "]: core extension");
        }

        LocalExtension previousExtension = null;

        LocalExtension localExtension =
            this.localExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
        if (localExtension != null) {
            int diff =
                this.versionManager.compareVersions(extensionId.getVersion(), localExtension.getId().getVersion());

            if (diff == 0) {
                throw new InstallException("[" + extensionId.getId() + "]: already installed");
            } else if (diff < 0) {
                throw new InstallException("[" + extensionId.getId() + "]: a more recent version is already installed");
            } else {
                // upgrade
                previousExtension = localExtension;
            }
        }

        return installExtension(previousExtension, extensionId, dependency, namespace);
    }

    // TODO: support version range
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

    private LocalExtension installExtension(LocalExtension previousExtension, ExtensionId extensionId,
        boolean dependency, String namespace) throws InstallException
    {
        // Resolve extension
        Extension remoteExtension;
        try {
            remoteExtension = this.repositoryManager.resolve(extensionId);
        } catch (ResolveException e) {
            throw new InstallException("Failed to resolve extension [" + extensionId + "]", e);
        }

        if (remoteExtension == null) {
            throw new InstallException("Failed to resolve extension [" + extensionId + "]");
        }

        try {
            return installExtension(previousExtension, remoteExtension, dependency, namespace);
        } catch (Exception e) {
            throw new InstallException("Failed to install extension", e);
        }
    }

    private LocalExtension installExtension(LocalExtension previousExtension, Extension remoteExtension,
        boolean dependency, String namespace) throws ComponentLookupException, InstallException
    {
        for (ExtensionDependency dependencyDependency : remoteExtension.getDependencies()) {
            installExtensionDependency(dependencyDependency, namespace);
        }

        // Store extension in local repository
        LocalExtension localExtension =
            this.localExtensionRepository.installExtension(remoteExtension, previousExtension != null
                ? previousExtension.isDependency() : dependency, namespace);

        if (previousExtension != null) {
            this.extensionHandlerManager.upgrade(previousExtension, localExtension, namespace);

            try {
                this.localExtensionRepository.uninstallExtension(previousExtension, namespace);
            } catch (UninstallException e) {
                // TODO should probably log something here even if that should not happen since we are sure this
                // extension exists
            }

            this.observationManager.notify(new ExtensionUpgraded(localExtension.getId()), localExtension,
                previousExtension);
        } else {
            this.extensionHandlerManager.install(localExtension, namespace);

            this.observationManager.notify(new ExtensionInstalled(localExtension.getId()), localExtension,
                previousExtension);
        }

        return localExtension;
    }
}
