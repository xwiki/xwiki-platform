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
package org.xwiki.wiki.internal.provisioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.job.Job;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.provisioning.WikiProvisioningJobException;
import org.xwiki.wiki.provisioning.WikiProvisioningJobExecutor;
import org.xwiki.wiki.provisioning.WikiProvisioningJobRequest;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link org.xwiki.wiki.provisioning.WikiProvisioningJobExecutor}.
 *
 * @since 6.0M1
 * @version $Id$
 * @deprecated since 7.0M2
 */
@Deprecated
@Component
@Singleton
public class DefaultWikiProvisioningJobExecutor implements WikiProvisioningJobExecutor, Initializable
{
    /**
     * List of all the jobs.
     */
    private Map<List<String>, WikiProvisioningJob> jobs;

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

    /**
     * Provider to get the xwiki context.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void initialize() throws InitializationException
    {
        this.jobs = new HashMap<List<String>, WikiProvisioningJob>();

        // Setup jobs thread
        BasicThreadFactory factory =
                new BasicThreadFactory.Builder().namingPattern("XWiki provisioning thread").daemon(true)
                        .priority(Thread.MIN_PRIORITY).build();
        this.jobExecutor = Executors.newCachedThreadPool(factory);
    }

    @Override
    public WikiProvisioningJob createAndExecuteJob(String wikiId, String provisioningJobName, Object parameter) throws
            WikiProvisioningJobException
    {
        try {
            // Get the context
            XWikiContext xcontext = xcontextProvider.get();
            // Create the job
            WikiProvisioningJob job = componentManager.getInstance(Job.class, provisioningJobName);
            // Id of the new job
            List<String> jobId = new ArrayList<String>();
            jobId.add("wiki");
            jobId.add("provisioning");
            jobId.add(provisioningJobName);
            jobId.add(wikiId);
            // Initialize it
            job.initialize(new WikiProvisioningJobRequest(jobId, wikiId, parameter, xcontext.getUserReference()));
            // Add it to the list of jobs
            jobs.put(jobId, job);
            // Pass it to the executor
            jobExecutor.execute(job);
            // Return the job
            return job;
        } catch (ComponentLookupException e) {
            throw new WikiProvisioningJobException(
                    String.format("Failed to lookup provisioning job component for role [%s]", provisioningJobName), e);
        }
    }

    @Override
    public WikiProvisioningJob getJob(List<String> jobId) throws WikiProvisioningJobException
    {
        WikiProvisioningJob job = jobs.get(jobId);
        return job;
    }
}
