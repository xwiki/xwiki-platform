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

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.provisioning.WikiProvisioningJobRequest;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.provisioning.DefaultWikiProvisioningJobExecutor}.
 *
 * @version $Id$
 * @since 6.0M1
 */
@ComponentTest
class DefaultWikiProvisioningJobExecutorTest
{
    @InjectMockComponents
    private DefaultWikiProvisioningJobExecutor defaultWikiProvisioningJobExecutor;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private XWikiContext xcontext;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void createAndExecuteJob() throws Exception
    {
        // Mocks
        WikiProvisioningJob provisioningJob = mock(WikiProvisioningJob.class);
        this.componentManager.registerComponent(Job.class, "wikiprovisioning.test", provisioningJob);
        ExecutionContextManager executionContextManager = mock(ExecutionContextManager.class);
        this.componentManager.registerComponent(ExecutionContextManager.class, executionContextManager);
        Execution execution = mock(Execution.class);
        this.componentManager.registerComponent(Execution.class, execution);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        when(this.xcontext.getUserReference()).thenReturn(user);

        // Execute
        WikiProvisioningJob job = this.defaultWikiProvisioningJobExecutor.createAndExecuteJob("wikiid",
            "wikiprovisioning.test", "templateid");

        // Verify
        // Id of the job.
        List<String> jobId = List.of("wiki", "provisioning", "wikiprovisioning.test", "wikiid");
        verify(provisioningJob).initialize(eq(new WikiProvisioningJobRequest(jobId, "wikiid", "templateid", user)));
        Thread.sleep(100);
        verify(provisioningJob).run();

        // getJobs also works
        assertEquals(this.defaultWikiProvisioningJobExecutor.getJob(jobId), job);
    }

    @Test
    void getJobWhenNoJob() throws Exception
    {
        assertNull(this.defaultWikiProvisioningJobExecutor.getJob(List.of()));
    }
}
