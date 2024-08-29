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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.job.internal.AbstractInstallPlanJob;
import org.xwiki.extension.job.internal.ExtensionPlanContext;
import org.xwiki.extension.job.plan.internal.DefaultExtensionPlanTree;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.version.IncompatibleVersionConstraintException;
import org.xwiki.extension.version.Version;
import org.xwiki.job.Job;
import org.xwiki.job.JobGroupPath;
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

    private final List<Extension> foundFlavors = new CopyOnWriteArrayList<>();

    private final Map<ExtensionDependency, Boolean> validatedExtensions = new HashMap<>();

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
            this.extensionsCache.clear();

            return currentTree.get(0).getAction().getExtension();
        } catch (InstallException e) {
            this.logger.debug("Can't install extension [{}] on namespace [{}].", extensionId, namespace, e);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cheating by considering extension matching recommended version as valid. The goal is to speed up a lot the flavor
     * resolution and anyway the extension will be validated again when actually installing it so it's safe, false
     * positives should be very rare.
     * 
     * @see org.xwiki.extension.job.internal.AbstractInstallPlanJob#installMandatoryExtensionDependency(org.xwiki.extension.ExtensionDependency,
     *      java.lang.String, java.util.List, java.util.Map)
     */
    @Override
    protected void installMandatoryExtensionDependency(ExtensionDependency extensionDependency, String namespace,
        List<ModifableExtensionPlanNode> parentBranch, ExtensionPlanContext extensionContext,
        Set<String> parents) throws InstallException, IncompatibleVersionConstraintException, ResolveException
    {
        // Cheating a bit to speed up resolution:
        // Skip it when we already know it's valid
        // Skip it when it's matching recommended version
        Boolean valid = Boolean.FALSE;
        try {
            if (this.validatedExtensions.get(extensionDependency) == Boolean.TRUE
                && this.configuration.getRecomendedVersionConstraint(extensionDependency.getId()) == null) {
                super.installMandatoryExtensionDependency(extensionDependency, namespace, parentBranch,
                    extensionContext, parents);
            }

            valid = Boolean.TRUE;
        } finally {
            // Remember the extension is valid
            this.validatedExtensions.put(extensionDependency, valid);
        }
    }

    @Override
    protected void runInternal() throws Exception
    {
        // Get known flavors
        Collection<ExtensionId> knownFlavors = this.flavorManager.getKnownFlavors();
        Collection<String> knownInvalidFlavors = this.flavorManager.getKnownInvalidFlavors();

        // Get remote flavors
        IterableResult<Extension> flavors = this.flavorManager.searchFlavors(new FlavorQuery());

        this.progressManager.pushLevelProgress(knownFlavors.size() + flavors.getSize(), this);

        try {
            // Remember which flavors already been (in)validated
            Set<String> doneFlavors = new HashSet<>();
            // Add the know invalid flavors to the list of already done flavors
            doneFlavors.addAll(knownInvalidFlavors);

            String namespace = getRequest().getNamespaces().iterator().next();

            // Add known flavors
            for (ExtensionId flavorId : knownFlavors) {
                this.progressManager.startStep(this);

                // Validate and add the flavor
                validateKnownFlavor(flavorId, namespace);

                // Remember we took care of this flavor
                doneFlavors.add(flavorId.getId());

                this.progressManager.endStep(this);
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

                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void validateKnownFlavor(ExtensionId flavorId, String namespace)
    {
        if (flavorId.getVersion() != null) {
            try {
                // Get corresponding extension
                Extension flavor = this.extensionManager.resolveExtension(flavorId);

                // Filter allowed flavors on namespace
                if (this.namespaceResolver.isAllowed(flavor.getAllowedNamespaces(), namespace)) {
                    // Directly add the flavor without trying to validate it first (99% of the time it's
                    // valid or it means the distribution was broken and you probably want to know about it)
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
    }

    private NavigableSet<Version> getVersions(String flavorId)
    {
        NavigableSet<Version> versionList = new TreeSet<>();

        // Search local versions
        try {
            IterableResult<Version> localVersions = this.localExtensionRepository.resolveVersions(flavorId, 0, -1);
            for (Version version : localVersions) {
                versionList.add(version);
            }
        } catch (ResolveException e) {
            this.logger.debug("Failed to resolve local versions for extension id [{}]", flavorId, e);
        }

        // Search remote versions
        try {
            IterableResult<Version> remoteVersions = this.repositoryManager.resolveVersions(flavorId, 0, -1);

            for (Version version : remoteVersions) {
                versionList.add(version);
            }
        } catch (ResolveException e) {
            this.logger.debug("Failed to resolve remote versions for extension id [{}]", flavorId, e);
        }

        return versionList;
    }

    private Extension findValidVersion(String flavorId, String namespace)
    {
        NavigableSet<Version> versions = getVersions(flavorId);

        if (versions.isEmpty()) {
            this.logger.debug("Could not find any version for the flavor extension [{}]", flavorId);

            return null;
        }

        return findValidVersion(flavorId, namespace, versions);
    }

    private Extension findValidVersion(String flavorId, String namespace, NavigableSet<Version> versions)
    {
        this.progressManager.pushLevelProgress(versions.size(), flavorId);

        try {
            // Try more recent first
            for (Version version : versions.descendingSet()) {
                this.progressManager.startStep(flavorId);

                Extension extension = tryInstallExtension(new ExtensionId(flavorId, version), namespace);

                this.progressManager.endStep(flavorId);

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
