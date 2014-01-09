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
package org.xwiki.wiki.template.script;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WikiTemplateManagerScriptTest
{
    @Rule
    public MockitoComponentMockingRule<WikiTemplateManagerScript> mocker =
            new MockitoComponentMockingRule(WikiTemplateManagerScript.class);

    private WikiTemplateManager wikiTemplateManager;

    private WikiDescriptorManager wikiDescriptorManager;

    private AuthorizationManager authorizationManager;

    private Provider<XWikiContext> xcontextProvider;

    private Execution execution;

    @Before
    public void setUp() throws Exception
    {
        wikiTemplateManager = mocker.getInstance(WikiTemplateManager.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        authorizationManager = mocker.getInstance(AuthorizationManager.class);
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        execution = mocker.getInstance(Execution.class);
    }

    @Test
    public void getWikiProvisioningJobStatus() throws Exception
    {
        WikiProvisioningJob job = mock(WikiProvisioningJob.class);
        when(wikiTemplateManager.getWikiProvisioningJob(anyList())).thenReturn(job);
        JobStatus status = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(status);

        List<String> jobId = new ArrayList<String>();
        JobStatus result = mocker.getComponentUnderTest().getWikiProvisioningJobStatus(jobId);

        assertEquals(status, result);
    }

    @Test
    public void getWikiProvisioningJobStatusWithBadId() throws Exception
    {
        List<String> jobId = new ArrayList<String>();
        JobStatus result = mocker.getComponentUnderTest().getWikiProvisioningJobStatus(jobId);

        assertEquals(null, result);
    }

    @Test
    public void getWikiProvisioningJobStatusWithException() throws Exception
    {
        when(wikiTemplateManager.getWikiProvisioningJob(anyList())).thenThrow(new WikiTemplateManagerException("test"));

        List<String> jobId = new ArrayList<String>();
        JobStatus result = mocker.getComponentUnderTest().getWikiProvisioningJobStatus(jobId);

        assertEquals(null, result);
    }


}
