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
package org.xwiki.wiki.template.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.wiki.template.WikiTemplateManagerException;

/**
 * Default implementation for {@link TemplateWikiProvisionerExecutor}.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultTemplateWikiProvisionerExecutor implements TemplateWikiProvisionerExecutor, Initializable
{
    /**
     * Map of all the jobs.
     */
    private Map<String, TemplateWikiProvisioner> provisionners;

    /**
     * Job Executor.
     */
    // TODO: use JobManager instead when it support several threads
    private ExecutorService jobExecutor;

    /**
     * Component manager used to get metadata extractors.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public void initialize() throws InitializationException
    {
        this.provisionners = new HashMap<String, TemplateWikiProvisioner>();

        // Setup provisionners job thread
        BasicThreadFactory factory =
                new BasicThreadFactory.Builder().namingPattern("XWiki Template provisioner thread").daemon(true)
                        .priority(Thread.MIN_PRIORITY).build();
        this.jobExecutor = Executors.newCachedThreadPool(factory);
    }

    @Override
    public void createAndExecuteJob(String wikiId, String templateId) throws WikiTemplateManagerException
    {
        try {
            // Create the job
            TemplateWikiProvisioner job = componentManager.getInstance(Job.class, TemplateWikiProvisioner.JOBTYPE);
            // Initialize it
            job.initialize(new TemplateWikiProvisionerRequest(wikiId, templateId));
            // Put it to the list of jobs
            provisionners.put(wikiId, job);
            // Pass it to the executor
            jobExecutor.execute(new ExecutionContextRunnable(job, this.componentManager));
        } catch (ComponentLookupException e) {
            throw new WikiTemplateManagerException("Failed to lookup template provisioner job component", e);
        }
    }

    @Override
    public JobStatus getJobStatus(String wikiId) throws WikiTemplateManagerException
    {
        TemplateWikiProvisioner provisionner = provisionners.get(wikiId);
        if (provisionner == null) {
            throw new WikiTemplateManagerException(String.format("There is no provisioner for the wiki [%s].",
                    wikiId));
        }
        return provisionner.getStatus();
    }
}
