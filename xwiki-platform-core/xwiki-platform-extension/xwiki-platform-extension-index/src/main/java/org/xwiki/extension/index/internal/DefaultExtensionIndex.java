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
package org.xwiki.extension.index.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionNotFoundException;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.ExtensionIndex;
import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.extension.index.internal.job.ExtensionIndexJob;
import org.xwiki.extension.index.internal.job.ExtensionIndexRequest;
import org.xwiki.extension.repository.AbstractAdvancedSearchableExtensionRepository;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.internal.RepositoryUtils;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.VersionUtils;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus.State;

/**
 * The default implementation of {@link ExtensionIndex}, based on Solr.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Singleton
public class DefaultExtensionIndex extends AbstractAdvancedSearchableExtensionRepository
    implements ExtensionIndex, Initializable
{
    private static final String ID = "index";

    @Inject
    private JobExecutor jobs;

    @Inject
    private JobStatusStore jobStore;

    @Inject
    private ExtensionIndexStore store;

    @Inject
    private ExtensionRepositoryManager repositories;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        setDescriptor(new DefaultExtensionRepositoryDescriptor(ID, ID, null));
    }

    @Override
    public ExtensionIndexStatus getStatus(Namespace namespace)
    {
        List<String> id;
        if (namespace != null) {
            id = ExtensionIndexRequest.getId(namespace);
        } else {
            id = ExtensionIndexRequest.JOB_ID;
        }

        // Try running jobs
        Job job = this.jobs.getJob(id);

        if (job != null) {
            return (ExtensionIndexStatus) job.getStatus();
        }

        // Try serialized jobs
        return (ExtensionIndexStatus) this.jobStore.getJobStatus(id);
    }

    @Override
    public ExtensionIndexStatus index(Namespace namespace) throws JobException
    {
        Job job = this.jobs.getJob(ExtensionIndexRequest.getId(namespace));

        if (job == null || job.getStatus().getState() == State.FINISHED) {
            job = this.jobs.execute(ExtensionIndexJob.JOB_TYPE,
                new ExtensionIndexRequest(false, true, true, Arrays.asList(namespace)));
        }

        return (ExtensionIndexStatus) job.getStatus();
    }

    // ExtensionRepository

    @Override
    public IterableResult<Extension> search(ExtensionQuery query) throws SearchException
    {
        return this.store.search(query);
    }

    private SolrExtension getSolrExtension(ExtensionId extensionId)
    {
        try {
            return this.store.getSolrExtension(extensionId);
        } catch (Exception e) {
            this.logger.warn("Failed to get the extension [{}] from the index: {}", extensionId,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return null;
    }

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension extension = getSolrExtension(extensionId);

        if (extension != null) {
            return extension;
        }

        // Fallback on the registered remote repositories
        extension = this.repositories.resolve(extensionId);

        // Remember the found extension
        cacheExtension(extension);

        return extension;
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        // Search in the index if the constraint is a unique version
        Version uniqueVersion = VersionUtils.getUniqueVersion(extensionDependency.getVersionConstraint());
        if (uniqueVersion != null) {
            ExtensionId extensionId = new ExtensionId(extensionDependency.getId(), uniqueVersion);

            Extension extension = getSolrExtension(extensionId);

            if (extension != null) {
                return extension;
            }
        }

        // Fallback on the registered remote repositories
        Extension extension = this.repositories.resolve(extensionDependency);

        // Remember the found extension
        cacheExtension(extension);

        return extension;
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        try {
            return this.store.exists(extensionId);
        } catch (Exception e) {
            this.logger.error("Failed to check existance of extension [{}]", extensionId, e);

            return false;
        }
    }

    private void cacheExtension(Extension extension)
    {
        try {
            this.store.add(extension, false);
        } catch (Exception e) {
            this.logger.warn("Failed to add the extension [{}] to the index: {}", extension.getId(),
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        Collection<Version> versions;
        try {
            versions = this.store.getExtensionVersions(id);
        } catch (Exception e) {
            throw new ResolveException("Failed to search for exetnsion versions", e);
        }

        if (versions == null) {
            throw new ExtensionNotFoundException("Can't find extension with id [" + id + "]");
        }

        if (nb == 0 || offset >= versions.size()) {
            return new CollectionIterableResult<>(versions.size(), offset, Collections.<Version>emptyList());
        }

        return RepositoryUtils.getIterableResult(offset, nb, versions);
    }

    @Override
    public boolean isFilterable()
    {
        return true;
    }

    @Override
    public boolean isSortable()
    {
        return true;
    }
}
