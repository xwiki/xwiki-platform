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
package org.xwiki.extension.index.internal.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallPlanJob;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.extension.job.plan.ExtensionPlanTree;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.repository.search.Searchable;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.Version.Type;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.internal.VersionUtils;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.Job;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.search.solr.SolrUtils;

/**
 * Update the index from configured repositories.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Named(ExtensionIndexJob.JOB_TYPE)
public class ExtensionIndexJob extends AbstractJob<ExtensionIndexRequest, DefaultExtensionIndexStatus>
    implements GroupedJob
{
    /**
     * Type of the job.
     */
    public static final String JOB_TYPE = "extendion.index";

    private static final int SEARCH_BATCH_SIZE = 100;

    private static final JobGroupPath GROUP_PATH = new JobGroupPath(JOB_TYPE, null);

    @Inject
    private ExtensionIndexStore indexStore;

    @Inject
    private ExtensionRepositoryManager repositoryManager;

    @Inject
    private LocalExtensionRepository localExtensions;

    @Inject
    private CoreExtensionRepository coreExtensions;

    @Inject
    private JobProgressManager progress;

    @Inject
    private ExtensionContext extensionContext;

    @Inject
    private ExtensionManagerConfiguration configuration;

    @Inject
    private InstalledExtensionRepository installedExtensions;

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    private FlavorManager flavors;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    @Named(InstallPlanJob.JOBTYPE)
    private Provider<Job> installPlanJobProvider;

    private Collection<String> invalidFlavors;

    @Override
    public JobGroupPath getGroupPath()
    {
        return GROUP_PATH;
    }

    @Override
    protected void jobStarting()
    {
        // Reduce the priority of this thread since it's not a critical task and it might be expensive
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);

        // Start an extension session so that things like the Maven cache are shared with the whole process
        this.extensionContext.pushSession();

        super.jobStarting();
    }

    @Override
    protected void jobFinished(Throwable error)
    {
        try {
            super.jobFinished(error);
        } finally {
            // We don't need the extension session anymore
            this.extensionContext.popSession();

            // Restore normal priority
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected DefaultExtensionIndexStatus createNewStatus(ExtensionIndexRequest request)
    {
        return new DefaultExtensionIndexStatus(request, this.observationManager, this.loggerManager);
    }

    @Override
    protected void runInternal() throws Exception
    {
        // Gather known invalid flavor so that we skip them
        this.invalidFlavors = this.flavors.getKnownInvalidFlavors();

        // Get indexed extensions
        Map<String, SortedSet<Version>> indexedExtensions = getIndexedExtensions();

        this.progress.pushLevelProgress(4, this);

        // 1: Add local extensions
        this.progress.startStep(this);
        if (getRequest().isLocalExtensionsEnabled()) {
            addLocalExtensions(indexedExtensions);
        }

        // 2: Gather all extensions from searchable repositories
        this.progress.startStep(this);
        if (getRequest().isRemoteExtensionsEnabled()) {
            addRemoteExtensions(indexedExtensions);
        }

        // 3: Validate latest and recommended extensions versions (only if something was updated or if update was
        // disabled)
        Set<String> missingExtension = new HashSet<>();
        this.progress.startStep(this);
        // if (getStatus().isUpdated() || (!getRequest().isLocalExtensionsEnabled() &&
        // !getRequest().isRemoteExtensionsEnabled())) {
        validateRecommendedExtensions(indexedExtensions, missingExtension);
        // }

        // 4: Validate older extensions
        this.progress.startStep(this);
        if (!missingExtension.isEmpty()) {
            validateOldExtensions(missingExtension, indexedExtensions);
        }
    }

    private void validateRecommendedExtensions(Map<String, SortedSet<Version>> indexedExtensions,
        Set<String> missingExtension)
    {
        this.progress.pushLevelProgress(getRequest().getNamespaces().size(), getRequest().getNamespaces());
        for (Namespace namespace : getRequest().getNamespaces()) {
            this.progress.startStep(getRequest().getNamespaces());
            validateExtensions(namespace, indexedExtensions, missingExtension);
        }
        this.progress.popLevelProgress(getRequest().getNamespaces());
    }

    private void validateOldExtensions(Set<String> missingExtension, Map<String, SortedSet<Version>> indexedExtensions)
    {
        // Test older versions
        this.progress.pushLevelProgress(getRequest().getNamespaces().size(), getRequest().getNamespaces());
        for (Namespace namespace : getRequest().getNamespaces()) {
            this.progress.startStep(getRequest().getNamespaces());
            validateOlderExtensions(namespace, missingExtension, indexedExtensions);
        }
        this.progress.popLevelProgress(getRequest().getNamespaces());
    }

    private void add(ExtensionId extension, Map<String, SortedSet<Version>> extensions)
    {
        extensions.computeIfAbsent(extension.getId(), key -> new TreeSet<>()).add(extension.getVersion());
    }

    private Map<String, SortedSet<Version>> getIndexedExtensions() throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();
        Set<ExtensionId> extensoinIds = this.indexStore.searchExtensionIds(solrQuery);
        Map<String, SortedSet<Version>> extensions = new HashMap<>(extensoinIds.size());
        for (ExtensionId extensionId : extensoinIds) {
            add(extensionId, extensions);
        }

        return extensions;
    }

    private void validateExtensions(Namespace namespace, Map<String, SortedSet<Version>> indexedExtensions,
        Set<String> missingExtensions)
    {
        try {
            // Validate namespace
            validateExtensions(namespace, false, indexedExtensions, missingExtensions);
        } catch (Exception e) {
            this.logger.error("Failed to validate extensions on namespace [{}]", namespace, e);
        }
    }

    private void validateOlderExtensions(Namespace namespace, Set<String> missingExtensions,
        Map<String, SortedSet<Version>> indexedExtensions)
    {
        try {
            // Validate namespace
            validateOlderExtensions(namespace, false, missingExtensions, indexedExtensions);
        } catch (Exception e) {
            this.logger.error("Failed to validate extensions on namespace [{}]", namespace, e);
        }
    }

    private Version getStopVersion(String extensionId, String namespace)
    {
        // Get the already installed version
        InstalledExtension installedExtension = this.installedExtensions.getInstalledExtension(extensionId, namespace);
        Version stopVersion;
        if (installedExtension != null) {
            stopVersion = installedExtension.getId().getVersion();
        } else {
            stopVersion = null;
        }

        return stopVersion;
    }

    private Extension getValid(String extensionId, Namespace namespace, boolean allowRootModications,
        SortedSet<Version> indexedVersions, Set<String> missingExtensions)
    {
        // If a core extension already exist then installing it in whatever version is impossible
        if (this.coreExtensions.exists(extensionId) || this.invalidFlavors.contains(extensionId)) {
            return null;
        }

        String namespaceString = namespace.serialize();

        // Get the stop version
        Version stopVersion = getStopVersion(extensionId, namespaceString);

        // Get recommended version
        VersionConstraint recommendedVersionConstraint = this.configuration.getRecomendedVersionConstraint(extensionId);
        Version recommendedVersion = VersionUtils.getUniqueVersion(recommendedVersionConstraint);

        // No reason for search something else if the installed version is already the recommended one
        if (recommendedVersion != null && recommendedVersion.equals(stopVersion)) {
            return null;
        }

        // Try recommended version
        if (recommendedVersion != null) {
            ExtensionId recommendedExtensionId = new ExtensionId(extensionId, recommendedVersion);

            if (this.extensionManager.exists(recommendedExtensionId)) {
                // Try only the recommended version if it exist
                return tryInstall(recommendedExtensionId, namespaceString, allowRootModications);
            } else {
                // If the recommended version does not exist, test only the last version
                return tryInstall(new ExtensionId(extensionId, indexedVersions.last()), namespaceString,
                    allowRootModications);
            }
        }

        // Try latest version
        Extension validExtension =
            tryInstall(new ExtensionId(extensionId, indexedVersions.last()), namespaceString, allowRootModications);
        if (validExtension != null) {
            return validExtension;
        }

        // Try older versions
        missingExtensions.add(extensionId);

        return null;
    }

    private Extension getValid(String extensionId, String namespace, boolean allowRootModications,
        Collection<Version> versions, Version stopVersion)
    {
        List<Version> versionList;
        if (versions instanceof List) {
            versionList = (List<Version>) versions;
        } else {
            versionList = new ArrayList<>(versions);
        }
        // Need to invert the list
        for (ListIterator<Version> it = versionList.listIterator(versionList.size()); it.hasPrevious();) {
            Version version = it.previous();

            // Don't try lower than the stop version
            if (stopVersion != null && stopVersion.compareTo(version) >= 0) {
                break;
            }

            Extension validExtension =
                tryInstall(new ExtensionId(extensionId, version), namespace, allowRootModications);
            if (validExtension != null) {
                return validExtension;
            }
        }

        return null;
    }

    private Extension tryInstall(ExtensionId extensionId, String namespace, boolean allowRootMdofications)
    {
        // Check if it already been validated
        Boolean compatible = this.indexStore.isCompatible(extensionId, namespace);
        if (compatible != null) {
            return ;
        }

        InstallRequest planRequest = new InstallRequest(getRequest());
        planRequest.setId((List<String>) null);
        planRequest.setVerbose(false);
        planRequest.setStatusLogIsolated(true);

        planRequest.addExtension(extensionId);
        planRequest.setRootModificationsAllowed(allowRootMdofications);

        if (namespace != null) {
            planRequest.addNamespace(namespace);
        }

        // Run the install plan in the current thread to benefit from the shared extension session
        Job job = this.installPlanJobProvider.get();
        job.initialize(planRequest);
        job.run();

        if (job.getStatus().getError() == null) {
            // Get last element of the root tree node
            ExtensionPlanTree tree = ((ExtensionPlan) job.getStatus()).getTree();
            ExtensionPlanNode node = IterableUtils.get(tree, tree.size() - 1);

            return node.getAction().getExtension();
        }

        return null;
    }

    private void addValidExtension(Extension validExtension, Namespace namespace, SortedSet<Version> indexedVersions)
        throws SolrServerException, IOException
    {
        if (!indexedVersions.contains(validExtension.getId().getVersion())) {
            this.indexStore.add(validExtension, false);

            // Remember this extension version was added to the index
            indexedVersions.add(validExtension.getId().getVersion());
        }

        for (Version version : indexedVersions) {
            this.indexStore.updateCompatible(new ExtensionId(validExtension.getId().getId(), version), namespace,
                version.equals(validExtension.getId().getVersion()));
        }
    }

    private void validateOlderExtensions(Namespace namespace, boolean allowRootMdofications,
        Set<String> missingExtension, Map<String, SortedSet<Version>> indexedExtensions)
        throws SolrServerException, IOException
    {
        boolean updated = false;

        for (String extensionId : missingExtension) {
            SortedSet<Version> indexedVersions = indexedExtensions.get(extensionId);

            Extension validExtension = getOldValid(extensionId, namespace, allowRootMdofications, indexedVersions);

            if (validExtension != null) {
                addValidExtension(validExtension, namespace, indexedVersions);

                updated = true;
            }
        }

        if (updated) {
            this.indexStore.commit();
        }
    }

    private Extension getOldValid(String extensionId, Namespace namespace, boolean allowRootModications,
        SortedSet<Version> indexedVersions) throws SolrServerException, IOException
    {
        String namespaceString = namespace.serialize();

        // Get the stop version
        Version stopVersion = getStopVersion(extensionId, namespaceString);

        // Get version valid in other namespaces
        Version compatibleVersion = this.indexStore.getCompatibleVersion(extensionId);
        if (compatibleVersion != null) {
            // Assume if this one is not compatible, none is
            return tryInstall(new ExtensionId(extensionId, compatibleVersion), namespaceString, allowRootModications);
        }

        // Get available versions
        Collection<Version> versions = new TreeSet<>();
        for (LocalExtension localExtension : this.localExtensions.getLocalExtensionVersions(extensionId)) {
            versions.add(localExtension.getId().getVersion());
        }
        try {
            CollectionUtils.addAll(versions, this.repositoryManager.resolveVersions(extensionId, 0, -1));
        } catch (ResolveException e) {
            this.logger.debug("Failed to get available version on remote repositories", e);
        }
        // The last version already been tested
        versions.remove(indexedVersions.last());

        // Search for a compatible version among the available versions
        return getValid(extensionId, namespaceString, allowRootModications, versions, stopVersion);
    }

    private void validateExtensions(Namespace namespace, boolean allowRootMdofications,
        Map<String, SortedSet<Version>> indexedExtensions, Set<String> missingExtensions)
        throws SolrServerException, IOException
    {
        boolean updated = false;

        this.progress.pushLevelProgress(indexedExtensions.size(), indexedExtensions);

        //for (Map.Entry<String, SortedSet<Version>> entry : indexedExtensions.entrySet()) {
            this.progress.startStep(indexedExtensions);

            //String extensionId = entry.getKey();
            String extensionId = "org.xwiki.contrib.oidc:oidc-authenticator";
            //SortedSet<Version> indexedVersions = entry.getValue();
            SortedSet<Version> indexedVersions = indexedExtensions.get("org.xwiki.contrib.oidc:oidc-authenticator");

            Extension validExtension =
                getValid(extensionId, namespace, allowRootMdofications, indexedVersions, missingExtensions);

            if (validExtension != null) {
                addValidExtension(validExtension, namespace, indexedVersions);

                updated = true;
            }
        //}

        if (updated) {
            this.indexStore.commit();
        }

        this.progress.popLevelProgress(indexedExtensions);
    }

    private void addLocalExtensions(Map<String, SortedSet<Version>> indexedExtensions)
        throws SearchException, SolrServerException, IOException
    {
        boolean updated = false;

        IterableResult<Extension> extensions = this.localExtensions.search("", 0, -1);

        // Add the extensions
        for (Extension extension : extensions) {
            if (!this.invalidFlavors.contains(extension.getId().getId())) {
                // TODO: support beta and snapshots versions too ?
                if (extension.getId().getVersion().getType() == Type.STABLE
                    && !this.indexStore.exists(extension.getId(), true)) {
                    this.indexStore.add(extension, true);

                    updated = true;
                    getStatus().setExtensionAdded(true);

                    add(extension.getId(), indexedExtensions);
                }
            }
        }

        if (updated) {
            this.indexStore.commit();
        }
    }

    private void addRemoteExtensions(Map<String, SortedSet<Version>> indexedExtensions)
        throws SearchException, SolrServerException, IOException
    {
        boolean updated = false;

        Collection<ExtensionRepository> repositories = this.repositoryManager.getRepositories();
        this.progress.pushLevelProgress(repositories);
        for (ExtensionRepository repository : repositories) {
            this.progress.startStep(repositories);
            if (repository instanceof Searchable) {
                updated |= addRemoteExtensions((Searchable) repository, indexedExtensions);
            }
        }
        this.progress.popLevelProgress(repositories);

        if (updated) {
            this.indexStore.commit();
        }
    }

    private boolean addRemoteExtensions(Searchable searchableRepository,
        Map<String, SortedSet<Version>> indexedExtensions) throws SearchException, SolrServerException, IOException
    {
        boolean updated = false;

        for (int offset = 0; true; offset += SEARCH_BATCH_SIZE) {
            // Get remote extensions ids
            IterableResult<Extension> result = searchableRepository.search("", offset, SEARCH_BATCH_SIZE);

            for (Extension extension : result) {
                if (!this.invalidFlavors.contains(extension.getId().getId())
                    && !this.coreExtensions.exists(extension.getId()) && !this.localExtensions.exists(extension.getId())
                    && !this.indexStore.exists(extension.getId())) {
                    // TODO: Resolve the complete extension but it very expensive...

                    // Add the extension to the index
                    this.indexStore.add(extension, true);

                    add(extension.getId(), indexedExtensions);

                    // Make sure only one version is tagged as "last"
                    SolrQuery solrQuery = new SolrQuery();
                    solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID + ':'
                        + this.solrUtils.toFilterQueryString(extension.getId().getId()));
                    solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST + ':' + true);
                    for (ExtensionId extensionid : this.indexStore.searchExtensionIds(solrQuery)) {
                        boolean last =
                            indexedExtensions.get(extension.getId().getId()).last().equals(extensionid.getVersion());
                        if (!extensionid.getVersion().equals(extension.getId().getVersion())) {
                            // Update the "last" flag of the already indexed extensions
                            this.indexStore.updateLast(extensionid, last);
                        } else if (!last) {
                            // The new extension is actually not the last one (maybe some local extension is more
                            // recent)
                            this.indexStore.updateLast(extensionid, false);

                        }
                    }

                    updated = true;
                    getStatus().setExtensionAdded(true);
                }

                // Update recommended and rating
                if (extension instanceof RemoteExtension && this.indexStore.exists(extension.getId())) {
                    this.indexStore.update((RemoteExtension) extension);

                    updated = true;
                }
            }

            if (result.getSize() < SEARCH_BATCH_SIZE) {
                break;
            }
        }

        return updated;
    }
}
