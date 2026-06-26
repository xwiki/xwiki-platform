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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.phase.Disposable;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Component in charge of scheduling and stopping indexing jobs.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component(roles = ExtensionIndexJobScheduler.class)
@Singleton
public class ExtensionIndexJobScheduler implements Disposable
{
    @Inject
    private JobExecutor jobs;

    @Inject
    private JobStatusStore jobStore;

    @Inject
    private ExtensionManagerConfiguration configuration;

    @Inject
    private WikiDescriptorManager wikis;

    @Inject
    private Logger logger;

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ExtensionIndexRequest scheduledRequest =
        new ExtensionIndexRequest(true, true, Arrays.asList(Namespace.ROOT));

    private ExtensionIndexRequest currentRequest;

    private boolean extensionAdded;

    private volatile boolean disposed;

    /**
     * Indicate that the instance is starting.
     */
    public void start()
    {
        // Add main wiki
        this.scheduledRequest.addNamespace(new WikiNamespace(this.wikis.getMainWikiId()));

        // Trigger a first indexing
        executeFirstJob();
    }

    /**
     * Indicate that a namespace start to be ready for manipulation.
     * 
     * @param namespace the namespace to initialize
     */
    public void initialize(Namespace namespace)
    {
        this.scheduledRequest.addNamespace(namespace);

        // Start an analysis of the namespace only if something changed
        if (this.extensionAdded) {
            executeJob(new ExtensionIndexRequest(false, false, Arrays.asList(namespace)));
        }
    }

    /**
     * Start a new indexing process or return the status of the currently running one.
     * 
     * @param namespace the namespace for which to validate the extensions
     * @return the status of the running indexing process
     * @throws JobException when failing to start indexing
     */
    public ExtensionIndexStatus index(Namespace namespace) throws JobException
    {
        // Check if the namespace's job is already running
        Job job;
        if (this.currentRequest != null && this.currentRequest.getNamespaces().contains(namespace)) {
            job = this.jobs.getJob(ExtensionIndexRequest.getId(namespace));
        } else {
            job = null;
        }

        // If not start a new one
        if (job == null || job.getStatus().getState() == State.FINISHED) {
            ExtensionIndexRequest request = new ExtensionIndexRequest(false, true, Arrays.asList(namespace));

            // Also analyze root namespace for main wiki
            if (namespace.getType().equals(WikiNamespace.TYPE) && this.wikis.isMainWiki(namespace.getValue())) {
                request.addNamespace(Namespace.ROOT);
            }

            job = this.jobs.execute(ExtensionIndexJob.JOB_TYPE, request);
        }

        return (ExtensionIndexStatus) job.getStatus();
    }

    /**
     * @param namespace the namespace for which the validation was executed
     * @return the status of the currently running or last indexing process
     */
    public ExtensionIndexStatus getStatus(Namespace namespace)
    {
        List<String> id = ExtensionIndexRequest.getId(namespace);

        // Try running jobs

        Job job = this.jobs.getJob(id);

        // If no specific job try scheduled job
        if (job == null && this.scheduledRequest.getNamespaces().contains(namespace)) {
            job = this.jobs.getJob(this.scheduledRequest.getId());
        }

        if (job != null) {
            return (ExtensionIndexStatus) job.getStatus();
        }

        // Try serialized jobs

        ExtensionIndexStatus namespaceStatus = (ExtensionIndexStatus) this.jobStore.getJobStatus(id);
        ExtensionIndexStatus scheduledStatus = !id.equals(this.scheduledRequest.getId())
            ? (ExtensionIndexStatus) this.jobStore.getJobStatus(this.scheduledRequest.getId()) : null;

        if (hasAStartDate(namespaceStatus)
            && (scheduledStatus == null || namespaceStatus.getStartDate().after(scheduledStatus.getStartDate())
                || !((ExtensionIndexRequest) scheduledStatus.getRequest()).getNamespaces().contains(namespace))) {
            return namespaceStatus;
        }

        return scheduledStatus;
    }

    private boolean hasAStartDate(ExtensionIndexStatus status)
    {
        return status != null && status.getStartDate() != null;
    }

    /**
     * @param namespace the new namespace to analyze
     */
    public void add(Namespace namespace)
    {
        // Update the scheduled request
        this.scheduledRequest.addNamespace(namespace);

        // Queue an analysis of the namespace
        executeJob(new ExtensionIndexRequest(false, false, Arrays.asList(namespace)));
    }

    /**
     * @param namespace the namespace to stop analysing
     */
    public void remove(Namespace namespace)
    {
        // Update the scheduled request
        this.scheduledRequest.removeNamespace(namespace);

        // TODO: remove the namespace from the index
    }

    private void executeFirstJob()
    {
        this.scheduler.execute(this::runFirstJob);
    }

    private void executeJob(ExtensionIndexRequest request)
    {
        this.scheduler.execute(() -> this.runJob(request));
    }

    private void runFirstJob()
    {
        // Run the first job
        runScheduledJob();

        if (!this.disposed) {
            // Disable local extension loading (it will be done dynamically)
            this.scheduledRequest.setLocalExtensionsEnabled(false);

            // Start scheduling following jobs
            this.scheduler.scheduleWithFixedDelay(this::runScheduledJob, this.configuration.getIndexInterval(),
                this.configuration.getIndexInterval(), TimeUnit.SECONDS);
        }
    }

    private void runScheduledJob()
    {
        if (!this.disposed) {
            Job job = runJob(this.scheduledRequest);

            if (job != null) {
                ExtensionIndexStatus status = (ExtensionIndexStatus) job.getStatus();

                // Remember updates
                this.extensionAdded |= status.isExtensionAdded();
            }
        }
    }

    private Job runJob(ExtensionIndexRequest request)
    {
        this.currentRequest = request;

        try {
            Job job = this.jobs.execute(ExtensionIndexJob.JOB_TYPE, request);
            job.join();

            return job;
        } catch (Exception e) {
            // Ignore the error if the scheduler was disposed (we assume it's a consequence of the shutdown)
            if (!this.disposed) {
                this.logger.error("Failed to execute job", e);
            }
        } finally {
            this.currentRequest = null;
        }

        return null;
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;

        // Stop the scheduling
        this.scheduler.shutdownNow();
    }
}
