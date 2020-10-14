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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.ExtensionIndex;
import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.extension.index.internal.job.ExtensionIndexJob;
import org.xwiki.extension.index.internal.job.ExtensionIndexRequest;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.VersionUtils;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobStatus.State;

/**
 * The default implementation of {@link ExtensionIndex}, based on Solr.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
public class DefaultExtensionIndex implements ExtensionIndex, Initializable
{
    @Inject
    private JobExecutor jobs;

    @Inject
    private ExtensionIndexStore store;

    @Inject
    private ExtensionRepositoryManager repositories;

    @Override
    public void initialize() throws InitializationException
    {
        // Start index job
        try {
            this.jobs.execute(ExtensionIndexJob.JOB_TYPE, new ExtensionIndexRequest(true));
        } catch (JobException e) {
            throw new InitializationException("Failed to start indexing the available extensions", e);
        }
    }

    @Override
    public ExtensionIndexStatus index() throws JobException
    {
        Job job = this.jobs.getJob(ExtensionIndexRequest.JOB_ID);

        if (job == null || job.getStatus().getState() == State.FINISHED) {
            job = this.jobs.execute(ExtensionIndexJob.JOB_TYPE, new ExtensionIndexRequest(false));
        }

        return (ExtensionIndexStatus) job.getStatus();
    }

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension extension = this.store.getExtension(extensionId);

        if (extension != null) {
            return extension;
        }

        // Fallback on the registered remote repositories
        extension = this.repositories.resolve(extensionId);

        // Remember the found extension
        this.store.add(extension, false);

        return extension;
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        // Search in the index if the constraint is a unique version
        Version uniqueVersion = VersionUtils.getUniqueVersion(extensionDependency.getVersionConstraint());
        if (uniqueVersion != null) {
            Extension extension = this.store.getExtension(new ExtensionId(extensionDependency.getId(), uniqueVersion));

            if (extension != null) {
                return extension;
            }
        }

        // Fallback on the registered remote repositories
        Extension extension = this.repositories.resolve(extensionDependency);

        // Remember the found extension
        this.store.add(extension, false);

        return extension;
    }
}
