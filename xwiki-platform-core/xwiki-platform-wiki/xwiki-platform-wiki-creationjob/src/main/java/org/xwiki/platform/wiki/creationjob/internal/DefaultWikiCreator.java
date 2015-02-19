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
package org.xwiki.platform.wiki.creationjob.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiCreator;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;

/**
 * Default implementation for {@link org.xwiki.platform.wiki.creationjob.WikiCreator}.
 *
 * @version $Id$
 * @since 7.0M2
 */
@Component
@Singleton
public class DefaultWikiCreator implements WikiCreator
{
    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private JobStatusStore jobStatusStore;

    @Override
    public Job createWiki(WikiCreationRequest request) throws WikiCreationException
    {
        try {
            request.setId(getJobId(request.getWikiId()));
            return jobExecutor.execute(WikiCreationJob.JOB_TYPE, request);
        } catch (JobException e) {
            throw new WikiCreationException("Failed to create a new wiki.", e);
        }
    }

    @Override
    public JobStatus getJobStatus(String wikiId)
    {
        List<String> jobId = getJobId(wikiId);
        Job job = jobExecutor.getJob(jobId);
        if (job != null) {
            return job.getStatus();
        } else {
            return jobStatusStore.getJobStatus(jobId);
        }
    }

    private List<String> getJobId(String wikiId)
    {
        return Arrays.asList(WikiCreationJob.JOB_ID_PREFIX, "createandinstall", wikiId);
    }
}
