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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallPlanJob;
import org.xwiki.extension.job.plan.ExtensionPlan;
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
    private ExtensionRepositoryManager repositories;

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
    @Named(InstallPlanJob.JOBTYPE)
    private Provider<Job> installPlanJobProvider;

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
        this.progress.pushLevelProgress(4, this);

        // 1: Add local extensions
        this.progress.startStep(this);
        if (getRequest().isLocalExtensionsEnabled()) {
            addLocalExtensions();
        }

        // 2: Gather all extension from searchable repositories
        this.progress.startStep(this);
        if (getRequest().isRemoteExtensionsEnabled()) {
            addRemoteExtensions();
        }

        // 3: Update versions
        this.progress.startStep(this);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.addFilterQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_LAST + ':' + true);
        Set<ExtensionId> extensoinIds = this.indexStore.searchExtensionIds(solrQuery);
        Map<String, SortedSet<Version>> extensions = new HashMap<>(extensoinIds.size());
        for (ExtensionId extensionId : extensoinIds) {
            extensions.computeIfAbsent(extensionId.getId(), key -> new TreeSet<>()).add(extensionId.getVersion());
        }
        if (getRequest().isRemoteExtensionsEnabled()) {
            updateVersions(extensions);
        }

        // 4: Validate extensions (only if something was updated or if update was disabled)
        this.progress.startStep(this);
        if (getStatus().isUpdated() || !getRequest().isRemoteExtensionsEnabled()) {
            this.progress.pushLevelProgress(getRequest().getNamespaces().size(), getRequest().getNamespaces());
            for (Namespace namespace : getRequest().getNamespaces()) {
                this.progress.startStep(getRequest().getNamespaces());
                validateExtensions(namespace, extensions);
            }
            this.progress.popLevelProgress(getRequest().getNamespaces());
        }

        this.progress.popLevelProgress(this);
    }

    private void validateExtensions(Namespace namespace, Map<String, SortedSet<Version>> extensions)
    {
        try {
            // Validate namespace
            validateExtensions(namespace.serialize(), false, extensions);
        } catch (Exception e) {
            this.logger.error("Failed to validate extensions on namespace [{}]", namespace, e);
        }
    }

    private Extension getValid(String extensionId, String namespace, boolean allowRootModications,
        SortedSet<Version> indexedVersions) throws SolrServerException, IOException
    {
        // If a core extension already exist then installing it in whatever version is impossible
        if (this.coreExtensions.exists(extensionId)) {
            return null;
        }

        // Get the already installed version
        InstalledExtension installedExtension = this.installedExtensions.getInstalledExtension(extensionId, namespace);
        Version stopVersion;
        if (installedExtension != null) {
            stopVersion = installedExtension.getId().getVersion();
        } else {
            stopVersion = null;
        }

        // Get recommended version
        VersionConstraint recommendedVersionConstraint = this.configuration.getRecomendedVersionConstraint(extensionId);
        Version recommendedVersion = VersionUtils.getUniqueVersion(recommendedVersionConstraint);

        // No reason for search something else if the installed version is already the recommended one
        if (recommendedVersion != null && recommendedVersion.equals(stopVersion)) {
            return null;
        }

        // Get available versions
        Collection<Version> versions = this.indexStore.getExtensionVersions(extensionId);
        if (versions == null) {
            versions = indexedVersions;
        }

        // Try recommended version
        if (recommendedVersion != null && versions.contains(recommendedVersion)) {
            // Try only the recommended version if it exist
            return tryInstall(new ExtensionId(extensionId, recommendedVersion), namespace, allowRootModications);
        }

        // Try others
        return getValid(extensionId, namespace, allowRootModications, versions, stopVersion);
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

        return job.getStatus().getError() == null
            ? ((ExtensionPlan) job.getStatus()).getTree().iterator().next().getAction().getExtension() : null;
    }

    private void validateExtensions(String namespace, boolean allowRootMdofications,
        Map<String, SortedSet<Version>> extensions) throws SolrServerException, IOException
    {
        boolean updated = false;

        for (Map.Entry<String, SortedSet<Version>> entry : extensions.entrySet()) {
            String extensionId = entry.getKey();
            SortedSet<Version> indexedVersions = entry.getValue();

            Extension validExtension = getValid(extensionId, namespace, allowRootMdofications, indexedVersions);

            if (validExtension != null) {
                Collection<Version> versions = entry.getValue();

                if (!versions.contains(validExtension.getId().getVersion())) {
                    this.indexStore.add(validExtension, false);

                    // Remember this extension version was added to the index
                    versions.add(validExtension.getId().getVersion());
                }

                for (Version version : versions) {
                    this.indexStore.updateCompatible(new ExtensionId(extensionId, version), namespace,
                        version.equals(validExtension.getId().getVersion()));
                }

                updated = true;
            }
        }

        if (updated) {
            this.indexStore.commit();
        }
    }

    private Map<String, SortedSet<Version>> updateVersions(Map<String, SortedSet<Version>> extensions)
        throws SolrServerException, IOException
    {
        boolean updated = false;
        for (Map.Entry<String, SortedSet<Version>> entry : extensions.entrySet()) {
            updated |= updateVersions(entry.getKey(), entry.getValue());
        }

        if (updated) {
            this.indexStore.commit();
        }

        return extensions;
    }

    private boolean updateVersions(String id, SortedSet<Version> storedVersions)
    {
        // Get all available versions
        IterableResult<Version> versions;
        try {
            versions = this.repositories.resolveVersions(id, 0, -1);
        } catch (ResolveException e) {
            return false;
        }

        // TODO: remove indexed extensions not part of the resolved versions ?

        List<String> newVersions = new ArrayList<>(versions.getSize());
        for (Version version : versions) {
            if (version.getType() == Type.STABLE) {
                newVersions.add(version.getValue());
            }
        }

        boolean updated = false;

        for (Iterator<Version> it = storedVersions.iterator(); it.hasNext();) {
            Version version = it.next();

            ExtensionId extensionId = new ExtensionId(id, version);
            try {
                // FIXME: update versions only if needed
                this.indexStore.update(extensionId, !it.hasNext(), newVersions);

                updated = true;
            } catch (Exception e) {
                this.logger.error("Failed to update the extension [{}]", extensionId, e);
            }
        }

        return updated;
    }

    private void addLocalExtensions() throws SearchException, SolrServerException, IOException
    {
        boolean updated = false;

        IterableResult<Extension> extensions = this.localExtensions.search("", 0, -1);

        // Add the extensions
        for (Extension extension : extensions) {
            // TODO: support beta and snapshots versions too ?
            if (extension.getId().getVersion().getType() == Type.STABLE
                && !this.indexStore.exists(extension.getId(), true)) {
                this.indexStore.add(extension, true);

                updated = true;
                getStatus().setUpdated(true);
            }
        }

        if (updated) {
            this.indexStore.commit();
        }
    }

    private void addRemoteExtensions() throws SearchException, ResolveException, SolrServerException, IOException
    {
        boolean updated = false;

        for (ExtensionRepository repository : this.repositories.getRepositories()) {
            if (repository instanceof Searchable) {
                updated |= addRemoteExtensions((Searchable) repository);
            }
        }

        if (updated) {
            this.indexStore.commit();
        }
    }

    private boolean addRemoteExtensions(Searchable searchableRepository)
        throws SearchException, ResolveException, SolrServerException, IOException
    {
        boolean updated = false;

        for (int offset = 0; true; offset += SEARCH_BATCH_SIZE) {
            // Get remote extensions ids
            IterableResult<Extension> result = searchableRepository.search("", offset, SEARCH_BATCH_SIZE);

            for (Extension extension : result) {
                if (!this.coreExtensions.exists(extension.getId()) && !this.localExtensions.exists(extension.getId())
                    && !this.indexStore.exists(extension.getId())) {
                    // Resolve the complete extension (search result is not necessarily complete)
                    Extension completeExtension = this.repositories.resolve(extension.getId());

                    // Add the extension to the index
                    this.indexStore.add(completeExtension, true);

                    updated = true;
                    getStatus().setUpdated(true);
                }

                // Update recommended and rating
                if (extension instanceof RemoteExtension) {
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
