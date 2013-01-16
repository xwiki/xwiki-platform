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
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.AbstractExtensionJob;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.job.Request;

/**
 * Make sure the provided XAR extension properly is registered in the installed extensions index.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named(RepairXarJob.JOBTYPE)
public class RepairXarJob extends AbstractExtensionJob<InstallRequest>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "repairxar";

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

    /**
     * Used to resolve extensions to install.
     */
    @Inject
    protected ExtensionRepositoryManager repositoryManager;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected InstallRequest castRequest(Request request)
    {
        InstallRequest installRequest;
        if (request instanceof InstallRequest) {
            installRequest = (InstallRequest) request;
        } else {
            installRequest = new InstallRequest(request);
        }

        return installRequest;
    }

    @Override
    protected void start() throws Exception
    {
        notifyPushLevelProgress(getRequest().getExtensions().size());

        try {
            for (ExtensionId extensionId : getRequest().getExtensions()) {
                if (getRequest().getNamespaces() != null) {
                    notifyPushLevelProgress(getRequest().getNamespaces().size());

                    try {
                        for (String namespace : getRequest().getNamespaces()) {
                            repairExtension(extensionId, namespace, false);

                            notifyStepPropress();
                        }
                    } finally {
                        notifyPopLevelProgress();
                    }
                } else {
                    repairExtension(extensionId, null, false);
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
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
            notifyPushLevelProgress(2);

            try {
                Extension extension = this.repositoryManager.resolve(extensionId);

                notifyStepPropress();

                if (extension.getType().equals(XarExtensionHandler.TYPE)) {
                    localExtension = this.localExtensionRepository.storeExtension(extension);
                }
            } catch (ResolveException e) {
                throw new InstallException("Failed to find extension", e);
            } catch (LocalExtensionRepositoryException e) {
                throw new InstallException("Failed save extension in local reposiory", e);
            } finally {
                notifyPopLevelProgress();
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
     * @throws InstallException failed to repair extension
     */
    private void repairExtension(ExtensionId extensionId, String namespace, boolean dependency) throws InstallException
    {
        this.logger.info("Repairing extension [{}] on namespace [{}]", extensionId, namespace);

        if (this.installedRepository.getInstalledExtension(extensionId.getId(), namespace) != null) {
            this.logger.info("Extension [{}] already installed on namespace [{}]", extensionId.getId(), namespace);

            return;
        }

        notifyPushLevelProgress(2);

        try {
            LocalExtension localExtension = getLocalXARExtension(extensionId);

            notifyStepPropress();

            if (localExtension != null) {
                repairExtension(localExtension, namespace, dependency);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param localExtension the local extension to install
     * @param namespace the namespace where to install extension
     * @param dependency indicate of the extension is installed as a dependency of another
     * @throws InstallException failed to repair extension
     */
    private void repairExtension(LocalExtension localExtension, String namespace, boolean dependency)
        throws InstallException
    {
        notifyPushLevelProgress(2);

        try {
            Collection< ? extends ExtensionDependency> dependencies = localExtension.getDependencies();

            if (!dependencies.isEmpty()) {
                notifyPushLevelProgress(dependencies.size());

                try {
                    for (ExtensionDependency extensionDependency : dependencies) {
                        if (extensionDependency.getVersionConstraint().getVersion() == null) {
                            this.logger.warn("Can't repair extension dependency [{}] with version range ([{}])"
                                + " since there is no way to know what has been installed",
                                extensionDependency.getId(), extensionDependency.getVersionConstraint());
                        } else {
                            try {
                                repairExtension(new ExtensionId(extensionDependency.getId(), extensionDependency
                                    .getVersionConstraint().getVersion()), namespace, true);
                            } catch (InstallException e) {
                                this.logger.warn("Failed to repair dependency [{}]", extensionDependency, e);
                            }
                        }

                        notifyStepPropress();
                    }
                } finally {
                    notifyPopLevelProgress();
                }
            }

            notifyStepPropress();

            this.installedRepository.installExtension(localExtension, namespace, dependency);
        } finally {
            notifyPopLevelProgress();
        }

        this.logger.info("Successfully Repaired extension [{}] on namespace [{}]", localExtension, namespace);
    }
}
