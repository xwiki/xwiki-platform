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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.job.internal.AbstractInstallPlanJob;
import org.xwiki.extension.job.plan.internal.DefaultExtensionPlanTree;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.job.Job;
import org.xwiki.job.JobGroupPath;
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

    @Inject
    private ExtensionManager extensionManager;

    private List<Extension> foundFlavors = new CopyOnWriteArrayList<>();

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        // We reuse install plan stuff but blocking in this job would cause more issues than it solves
        return null;
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
    protected DefaultFlavorSearchStatus createNewStatus(FlavorSearchRequest request)
    {
        Job currentJob = this.jobContext.getCurrentJob();
        JobStatus currentJobStatus = currentJob != null ? currentJob.getStatus() : null;
        return new DefaultFlavorSearchStatus(request, this.observationManager, this.loggerManager, this.foundFlavors,
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
        DefaultExtensionPlanTree currentTree = new DefaultExtensionPlanTree();

        try {
            installExtension(extensionId, namespace, currentTree);

            // Cleanup
            this.extensionsNodeCache.clear();

            return currentTree.get(0).getAction().getExtension();
        } catch (InstallException e) {
            this.logger.debug("Can't install extension [{}] on namespace [{}].", extensionId, namespace, e);
        }

        return null;
    }

    @Override
    protected void runInternal() throws Exception
    {
        // Get known flavors
        Collection<ExtensionId> knownFlavors = this.flavorManager.getKnownFlavors();

        // Get remote flavors
        IterableResult<Extension> flavors = this.flavorManager.searchFlavors(new FlavorQuery());

        this.progressManager.pushLevelProgress(knownFlavors.size() + flavors.getSize(), this);

        try {
            Set<String> doneFlavors = new HashSet<>();

            String namespace = getRequest().getNamespaces().iterator().next();

            // Add known flavors
            for (ExtensionId flavorId : knownFlavors) {
                this.progressManager.startStep(this);

                if (flavorId.getVersion() != null) {
                    try {
                        // Get corresponding extension
                        Extension flavor = this.extensionManager.resolveExtension(flavorId);

                        // Filter allowed flavors on namespace
                        if (this.namespaceResolver.isAllowed(flavor.getAllowedNamespaces(), namespace)) {
                            // Directly add the flavor without trying to validate it first (99% of the time it's valid
                            // or it
                            // mean
                            // the distribution was broken and you probably want to know about it)
                            this.foundFlavors.add(flavor);
                        }
                    } catch (ResolveException e) {
                        this.logger.debug("Failed to resolve extension [{}]", flavorId, e);
                    }
                } else {
                    // Find a valid version of the flavor
                    Extension flavor = findValidVersion(flavorId.getId(), namespace);

                    if (flavor != null) {
                        this.foundFlavors.add(flavor);
                    }
                }

                // Remember we took care of this flavor
                doneFlavors.add(flavorId.getId());
            }

            // Add remote flavors
            for (Extension flavor : flavors) {
                this.progressManager.startStep(this);

                // Search only unknown flavors
                if (!doneFlavors.contains(flavor.getId().getId())) {
                    Extension validExtension = findValidVersion(flavor.getId().getId(), namespace);
                    if (validExtension != null) {
                        this.foundFlavors.add(validExtension);
                    }
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
