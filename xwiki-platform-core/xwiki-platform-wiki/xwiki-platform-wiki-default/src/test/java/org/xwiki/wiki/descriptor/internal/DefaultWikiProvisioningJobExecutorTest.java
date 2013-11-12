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
package org.xwiki.wiki.descriptor.internal;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.internal.provisioning.DefaultWikiProvisioningJobExecutor;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.provisioning.WikiProvisioningJobRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.provisioning.DefaultWikiProvisioningJobExecutor}.
 *
 * @version $Id$
 * @since 5.3M2
 */
public class DefaultWikiProvisioningJobExecutorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiProvisioningJobExecutor> mocker =
            new MockitoComponentMockingRule(DefaultWikiProvisioningJobExecutor.class);

    @Test
    public void createAndExecuteJob() throws Exception
    {
        // Mocks
        WikiProvisioningJob provisioningJob = mock(WikiProvisioningJob.class);
        mocker.registerComponent(WikiProvisioningJob.class, "test", provisioningJob);
        ExecutionContextManager executionContextManager = mock(ExecutionContextManager.class);
        mocker.registerComponent(ExecutionContextManager.class, executionContextManager);
        Execution execution = mock(Execution.class);
        mocker.registerComponent(Execution.class, execution);

        // Execute
        int jobId = mocker.getComponentUnderTest().createAndExecuteJob("wikiid", "test", "templateid");

        // Verify
        verify(provisioningJob).initialize(eq(new WikiProvisioningJobRequest("wikiid", "templateid")));
        Thread.sleep(100);
        verify(provisioningJob).run();

        JobStatus status = mock(JobStatus.class);
        when(provisioningJob.getStatus()).thenReturn(status);
        assertEquals(mocker.getComponentUnderTest().getJobStatus(jobId), status);
    }
}
