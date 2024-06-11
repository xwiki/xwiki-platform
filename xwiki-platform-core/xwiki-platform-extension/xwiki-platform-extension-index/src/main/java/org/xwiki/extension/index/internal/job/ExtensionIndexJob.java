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
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Update the index from configured repositories.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Named(ExtensionIndexJob.JOB_TYPE)
public class ExtensionIndexJob extends AbstractJob<ExtensionIndexRequest, DefaultExtensionIndexStatus>
    implements GroupedJob
{
    /**
     * Type of the job.
     */
    public static final String JOB_TYPE = "extension.index";

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
    private WikiDescriptorManager wikis;

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
            //addLocalExtensions(indexedExtensions);
        }

        // 2: Gather all extensions from searchable repositories
        this.progress.startStep(this);
        if (getRequest().isRemoteExtensionsEnabled()) {
            addRemoteExtensions(indexedExtensions);
        }

        // 3: Validate latest and recommended extensions versions (only if something was updated or if update was
        // disabled)
        Map<String, Set<Namespace>> missingExtension = new HashMap<>();
        this.progress.startStep(this);
        validateLastExtensions(indexedExtensions, missingExtension);

        // 4: Validate older extensions
        this.progress.startStep(this);
        if (!missingExtension.isEmpty()) {
            validateOldExtensions(missingExtension, indexedExtensions);
        }
    }

    private void validateLastExtensions(Map<String, SortedSet<Version>> indexedExtensions,
        Map<String, Set<Namespace>> missingExtension)
    {
        this.progress.pushLevelProgress(getRequest().getNamespaces().size(), getRequest().getNamespaces());

        for (Namespace namespace : getRequest().getNamespaces()) {
            this.progress.startStep(getRequest().getNamespaces());

            validateExtensions(namespace, indexedExtensions, missingExtension);
        }

        this.progress.popLevelProgress(getRequest().getNamespaces());
    }

    private void validateOldExtensions(Map<String, Set<Namespace>> missingExtension,
        Map<String, SortedSet<Version>> indexedExtensions) throws SolrServerException, IOException
    {
        // Test older versions
        this.progress.pushLevelProgress(missingExtension.size(), missingExtension);
        for (Map.Entry<String, Set<Namespace>> entry : missingExtension.entrySet()) {
            this.progress.startStep(getRequest().getNamespaces());
            validateOlderExtensions(entry.getKey(), entry.getValue(), indexedExtensions);
        }
        this.progress.popLevelProgress(missingExtension);
    }

    private void add(ExtensionId extension, Map<String, SortedSet<Version>> extensions)
    {
        extensions.computeIfAbsent(extension.getId(), key -> new TreeSet<>()).add(extension.getVersion());
    }

    private Map<String, SortedSet<Version>> getIndexedExtensions() throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery();
        Set<ExtensionId> extensionIds = this.indexStore.searchExtensionIds(solrQuery);
        Map<String, SortedSet<Version>> extensions = new HashMap<>(extensionIds.size());
        for (ExtensionId extensionId : extensionIds) {
            add(extensionId, extensions);
        }

        return extensions;
    }

    private void validateOlderExtensions(String extensionId, Set<Namespace> namespaces,
        Map<String, SortedSet<Version>> indexedExtensions) throws SolrServerException, IOException
    {
        boolean updated = false;

        SortedSet<Version> indexedVersions = indexedExtensions.get(extensionId);

        // If the extension is already compatible on any namespace check this specific version
        Version compatibleVersion = this.indexStore.getCompatibleVersion(extensionId, null);

        // Try other versions
        for (Namespace namespace : namespaces) {
            try {
                if (compatibleVersion == null || this.indexStore
                    .isCompatible(new ExtensionId(extensionId, compatibleVersion), namespace.serialize()) == null) {

                    if (compatibleVersion != null) {
                        // Try only the compatible version

                    } else {
                        // Search for a compatible version among the available versions
                        updated |= validateOldExtension(extensionId, namespace, indexedVersions);
                    }
                }
            } catch (Exception e) {
                this.logger.error("Failed to validate extensions [{}] on namespace [{}]", extensionId, namespace, e);
            }
        }

        if (updated) {
            this.indexStore.commit();
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

    private boolean validateExtension(String extensionId, Namespace namespace, SortedSet<Version> indexedVersions,
        Map<String, Set<Namespace>> missingExtensions) throws SolrServerException, IOException
    {
        // If a core extension already exist then installing it in whatever version is impossible
        if (this.coreExtensions.exists(extensionId) || this.invalidFlavors.contains(extensionId)) {
            return false;
        }

        String namespaceString = namespace.serialize();

        // Get the stop version
        Version stopVersion = getStopVersion(extensionId, namespaceString);

        // Get recommended version
        VersionConstraint recommendedVersionConstraint = this.configuration.getRecomendedVersionConstraint(extensionId);
        Version recommendedVersion = VersionUtils.getUniqueVersion(recommendedVersionConstraint);

        // No reason for search something else if the installed version is already the recommended one
        if (recommendedVersion != null && recommendedVersion.equals(stopVersion)) {
            return false;
        }

        if (recommendedVersion != null) {
            return validateRecommendedVersion(extensionId, recommendedVersion, namespace, indexedVersions);
        } else {
            // Try latest version
            ExtensionId tryId = new ExtensionId(extensionId, indexedVersions.last());

            // Check if the version already been validated
            if (isValidated(tryId, namespaceString)) {
                return false;
            }

            // Try installing it
            Extension validExtension = tryInstall(tryId, namespace);
            if (validExtension != null) {
                addSearchableCompatibleExtension(validExtension, namespace, indexedVersions);

                return true;
            }

            // Try older versions (but push it to a future step)
            missingExtensions.computeIfAbsent(extensionId, key -> new HashSet<>()).add(namespace);

            // Explicitly mark the extension as invalid (if this extension exist in the index)
            this.indexStore.updateCompatible(tryId, namespace.serialize(), null, true);

            return true;
        }
    }

    private boolean validateRecommendedVersion(String extensionId, Version recommendedVersion, Namespace namespace,
        SortedSet<Version> indexedVersions) throws SolrServerException, IOException
    {
        ExtensionId recommendedExtensionId = new ExtensionId(extensionId, recommendedVersion);
        ExtensionId lastExtensionId = new ExtensionId(extensionId, indexedVersions.last());

        // Check if the recommended or last version already been validated
        if (isValidated(recommendedExtensionId, namespace.serialize())
            || isValidated(lastExtensionId, namespace.serialize())) {
            return false;
        }

        ExtensionId tryId;
        if (this.extensionManager.exists(recommendedExtensionId)) {
            // Try only the recommended version if it exist
            tryId = recommendedExtensionId;
        } else {
            // If the recommended version does not exist, test only the last version
            tryId = lastExtensionId;
        }

        // Try only the recommended version if it exist
        return validate(tryId, namespace, indexedVersions);
    }

    private boolean isAllowRootModications(Namespace namespace)
    {
        return namespace == null || namespace.equals(Namespace.ROOT)
            || (namespace.getType().endsWith(WikiNamespace.TYPE) && this.wikis.isMainWiki(namespace.getValue()));
    }

    private boolean isValidated(ExtensionId extensionId, String namespace) throws SolrServerException, IOException
    {
        return this.indexStore.isCompatible(extensionId, namespace) != null;
    }

    private boolean validateOldExtension(String extensionId, Namespace namespace, Collection<Version> versions,
        Version stopVersion, SortedSet<Version> indexedVersions) throws SolrServerException, IOException
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

            ExtensionId tryId = new ExtensionId(extensionId, version);

            // Check if the extension already been validated
            Boolean compatible = this.indexStore.isCompatible(tryId, namespace.serialize());
            if (compatible != null) {
                return false;
            }

            // Try installing it
            if (validate(tryId, namespace, indexedVersions)) {
                return true;
            }
        }

        return false;
    }

    private boolean validate(ExtensionId extensionId, Namespace namespace, SortedSet<Version> indexedVersions)
        throws SolrServerException, IOException
    {
        // Try installing it
        Extension validExtension = tryInstall(extensionId, namespace);
        if (validExtension != null) {
            addSearchableCompatibleExtension(validExtension, namespace, indexedVersions);

            return true;
        }

        if (this.indexStore.exists(extensionId)) {
            // Explicitly mark the extension as invalid (if this extension exist in the index)
            // TODO: index it if it does not exist ?
            this.indexStore.updateCompatible(extensionId, namespace.serialize(), null, true);

            return true;
        }

        return false;
    }

    private Extension tryInstall(ExtensionId extensionId, Namespace namespace)
    {
        InstallRequest planRequest = new InstallRequest(getRequest());
        planRequest.setId((List<String>) null);
        planRequest.setVerbose(false);
        planRequest.setStatusLogIsolated(true);

        planRequest.addExtension(extensionId);
        planRequest.setRootModificationsAllowed(isAllowRootModications(namespace));

        if (namespace != null) {
            planRequest.addNamespace(namespace.serialize());
        }

        // Run the install plan in the current thread to benefit from the shared extension session
        Job job = this.installPlanJobProvider.get();
        job.initialize(planRequest);

        // Ignore any log produced by the install plan job
        getStatus().ignoreLogs(true);
        try {
            job.run();
        } finally {
            getStatus().ignoreLogs(false);
        }

        if (job.getStatus().getError() == null) {
            // Get last element of the root tree node
            ExtensionPlanTree tree = ((ExtensionPlan) job.getStatus()).getTree();
            ExtensionPlanNode node = IterableUtils.get(tree, tree.size() - 1);

            return node.getAction().getExtension();
        }

        return null;
    }

    private void addSearchableCompatibleExtension(Extension validExtension, Namespace namespace,
        SortedSet<Version> indexedVersions) throws SolrServerException, IOException
    {
        if (!indexedVersions.contains(validExtension.getId().getVersion())) {
            this.indexStore.add(validExtension, false);

            // Copy variable stuff (recommended, ratings, etc.) from current latest version
            if (!indexedVersions.isEmpty()) {
                this.indexStore.update(validExtension.getId(), indexedVersions.last());
            }

            // Remember this extension version was added to the index
            indexedVersions.add(validExtension.getId().getVersion());
        }

        for (Version version : indexedVersions) {
            this.indexStore.updateCompatible(new ExtensionId(validExtension.getId().getId(), version),
                namespace.serialize(), version.equals(validExtension.getId().getVersion()), null);
        }
    }

    private boolean validateOldExtension(String extensionId, Namespace namespace, SortedSet<Version> indexedVersions)
        throws SolrServerException, IOException
    {
        String namespaceString = namespace.serialize();

        // Get the stop version
        Version stopVersion = getStopVersion(extensionId, namespaceString);

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
        return validateOldExtension(extensionId, namespace, versions, stopVersion, indexedVersions);
    }

    private void validateExtensions(Namespace namespace, Map<String, SortedSet<Version>> indexedExtensions,
        Map<String, Set<Namespace>> missingExtensions)
    {
        this.progress.pushLevelProgress(indexedExtensions.size(), indexedExtensions);

        for (Map.Entry<String, SortedSet<Version>> entry : indexedExtensions.entrySet()) {
            this.progress.startStep(indexedExtensions);

            String extensionId = entry.getKey();
            SortedSet<Version> indexedVersions = entry.getValue();

            try {
                if (validateExtension(extensionId, namespace, indexedVersions, missingExtensions)) {
                    // Commit right away since validating an extension can be very slow and we want to get as many
                    // extensions as possible as fast as possible in the search result
                    this.indexStore.commit();
                }
            } catch (Exception e) {
                this.logger.error("Failed to validate extension with if [{}] on namespace [{}]", extensionId, namespace,
                    e);
            }
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
        throws SolrServerException, IOException
    {
        boolean updated = false;

        Collection<ExtensionRepository> repositories = this.repositoryManager.getRepositories();
        this.progress.pushLevelProgress(repositories);
        for (ExtensionRepository repository : repositories) {
            this.progress.startStep(repositories);
            if (repository instanceof Searchable searchableRepository) {
                try {
                    updated |= addRemoteExtensions(searchableRepository, indexedExtensions);
                } catch (Exception e) {
                    this.logger.warn("Failed to get remote extension from repository [{}]: {}",
                        repository.getDescriptor(), ExceptionUtils.getRootCauseMessage(e));
                }
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
                        + this.solrUtils.toCompleteFilterQueryString(extension.getId().getId()));
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
                if (extension instanceof RemoteExtension remoteExtension) {
                    SortedSet<Version> versions = indexedExtensions.get(extension.getId().getId());
                    if (versions != null) {
                        for (Version version : versions) {
                            this.indexStore.update(new ExtensionId(extension.getId().getId(), version),
                                remoteExtension);

                            updated = true;
                        }
                    }
                }
            }

            if (result.getSize() < SEARCH_BATCH_SIZE) {
                break;
            }
        }

        return updated;
    }
}
