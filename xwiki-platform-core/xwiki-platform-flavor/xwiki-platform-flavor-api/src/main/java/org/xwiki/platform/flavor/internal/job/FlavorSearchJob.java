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
package org.xwiki.platform.flavor.internal.job;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.job.internal.AbstractInstallPlanJob;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.platform.flavor.FlavorQuery;
import org.xwiki.platform.flavor.job.FlavorSearchRequest;

/**
 * Filter existing flavor to keep only those that can be installed on provided namespace.
 *
 * @version $Id$
 * @since 8.0RC1
 */
@Component
@Named(FlavorSearchJob.JOBTYPE)
public class FlavorSearchJob extends AbstractInstallPlanJob<FlavorSearchRequest>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "searchflavors";

    @Inject
    private FlavorManager flavorManager;

    private List<Extension> remoteFlavors = new ArrayList<>();

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected FlavorSearchRequest castRequest(Request request)
    {
        FlavorSearchRequest installRequest;
        if (request instanceof FlavorSearchRequest) {
            installRequest = (FlavorSearchRequest) request;
        } else {
            installRequest = new FlavorSearchRequest(request);
        }

        return installRequest;
    }

    @Override
    protected FlavorSearchStatus createNewStatus(FlavorSearchRequest request)
    {
        Job currentJob = this.jobContext.getCurrentJob();
        JobStatus currentJobStatus = currentJob != null ? currentJob.getStatus() : null;
        return new FlavorSearchStatus(request, this.observationManager, this.loggerManager, this.remoteFlavors,
            currentJobStatus);
    }

    /**
     * Try to install the provided extension and update the plan if it's working.
     *
     * @param extensionId the extension version to install
     * @param namespace the namespace where to install the extension
     * @return true if the installation would succeed, false otherwise
     */
    private Extension tryInstallExtension(ExtensionId extensionId, String namespace)
    {
        ModifableExtensionPlanTree currentTree = new ModifableExtensionPlanTree();

        try {
            installExtension(extensionId, namespace, currentTree);

            return currentTree.get(0).getAction().getExtension();
        } catch (InstallException e) {
            this.logger.debug("Can't install extension [{}] on namespace [{}].", extensionId, namespace, e);
        }

        return null;
    }

    @Override
    protected void runInternal() throws Exception
    {
        IterableResult<Extension> flavors = this.flavorManager.searchFlavors(new FlavorQuery());

        this.progressManager.pushLevelProgress(flavors.getSize(), this);

        try {
            for (Extension flavor : flavors) {
                this.progressManager.startStep(this);

                Extension validExtension =
                    findValidVersion(flavor.getId().getId(), getRequest().getNamespaces().iterator().next());
                if (validExtension != null) {
                    this.remoteFlavors.add(validExtension);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private Extension findValidVersion(String flavorId, String namespace)
    {
        IterableResult<Version> versions;
        try {
            versions = this.repositoryManager.resolveVersions(flavorId, 0, -1);

            if (versions.getSize() == 0) {
                this.logger.debug("Could not find any version for the flavor extension [{}]", flavorId);

                return null;
            }

            List<Version> versionList = new ArrayList<Version>(versions.getSize());
            for (Version version : versions) {
                versionList.add(version);
            }

            return findValidVersion(flavorId, namespace, versionList);
        } catch (ResolveException e) {
            this.logger.debug("Failed to resolve versions for extension id [{}]", flavorId, e);
        }

        return null;
    }

    private Extension findValidVersion(String flavorId, String namespace, List<Version> versionList)
    {
        this.progressManager.pushLevelProgress(versionList.size(), flavorId);

        try {
            for (ListIterator<Version> it = versionList.listIterator(versionList.size()); it.hasPrevious();) {
                this.progressManager.startStep(flavorId);

                Version version = it.previous();

                Extension extension = tryInstallExtension(new ExtensionId(flavorId, version), namespace);
                if (extension != null) {
                    return extension;
                }
            }
        } finally {
            this.progressManager.popLevelProgress(flavorId);
        }

        return null;
    }
}
