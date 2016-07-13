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
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.provisioning.WikiProvisioningJobRequest;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.provisioning.DefaultWikiProvisioningJobExecutor}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultWikiProvisioningJobExecutorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiProvisioningJobExecutor> mocker =
            new MockitoComponentMockingRule(DefaultWikiProvisioningJobExecutor.class);

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
    }

    @Test
    public void createAndExecuteJob() throws Exception
    {
        // Mocks
        WikiProvisioningJob provisioningJob = mock(WikiProvisioningJob.class);
        mocker.registerComponent(Job.class, "wikiprovisioning.test", provisioningJob);
        ExecutionContextManager executionContextManager = mock(ExecutionContextManager.class);
        mocker.registerComponent(ExecutionContextManager.class, executionContextManager);
        Execution execution = mock(Execution.class);
        mocker.registerComponent(Execution.class, execution);
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        when(xcontext.getUserReference()).thenReturn(user);

        // Execute
        WikiProvisioningJob job = mocker.getComponentUnderTest().createAndExecuteJob("wikiid", "wikiprovisioning.test",
                "templateid");

        // Verify
        // Id of the job.
        List<String> jobId = new ArrayList<String>();
        jobId.add("wiki");
        jobId.add("provisioning");
        jobId.add("wikiprovisioning.test");
        jobId.add("wikiid");
        verify(provisioningJob).initialize(eq(new WikiProvisioningJobRequest(jobId, "wikiid", "templateid", user)));
        Thread.sleep(100);
        verify(provisioningJob).run();

        // getJobs also works
        assertEquals(mocker.getComponentUnderTest().getJob(jobId), job);
    }

    @Test
    public void getJobWhenNoJob() throws Exception
    {
        List<String> jobId = new ArrayList<String>();
        assertNull(mocker.getComponentUnderTest().getJob(jobId));
    }

}
