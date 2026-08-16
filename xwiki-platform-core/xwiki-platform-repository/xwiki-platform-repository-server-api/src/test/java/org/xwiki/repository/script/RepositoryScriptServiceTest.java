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
package org.xwiki.repository.script;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.ExtensionId;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.internal.job.XIPExportJob;
import org.xwiki.repository.job.XIPExportJobRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RepositoryScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class RepositoryScriptServiceTest
{
    @InjectMockComponents
    private RepositoryScriptService repositoryScriptService;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private Execution execution;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext xcontext;

    private final ExecutionContext executionContext = new ExecutionContext();

    private ExtensionId extensionId = new ExtensionId("org.xwiki.contrib:my-extension", "1.0");

    private String xwikiVersion = "18.5.0";

    @BeforeEach
    void setup()
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.contextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void exportExtension() throws JobException
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Alice");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Bob");
        when(this.xcontext.getUserReference()).thenReturn(userReference);
        when(this.xcontext.getAuthorReference()).thenReturn(authorReference);

        Job expectedJob = mock(Job.class);
        when(this.jobExecutor.execute(eq(XIPExportJob.JOB_TYPE), any(XIPExportJobRequest.class)))
            .thenReturn(expectedJob);

        Job result = this.repositoryScriptService.exportExtension(extensionId, xwikiVersion);

        assertEquals(expectedJob, result);
        assertNull(this.repositoryScriptService.getLastError());

        ArgumentCaptor<XIPExportJobRequest> requestCaptor = ArgumentCaptor.forClass(XIPExportJobRequest.class);
        verify(this.jobExecutor).execute(eq(XIPExportJob.JOB_TYPE), requestCaptor.capture());
        XIPExportJobRequest request = requestCaptor.getValue();

        assertEquals(extensionId, request.getExtension());
        assertEquals(xwikiVersion, request.getXWikiVersion());
        assertEquals(
            List.of(new ExtensionId("org.xwiki.platform:xwiki-platform-distribution-war-dependencies", xwikiVersion)),
            request.getCoreExtensions());
        assertTrue(request.isCheckRights());
        assertEquals(userReference, request.getUserReference());
        assertEquals(authorReference, request.getAuthorReference());
        List<String> jobId = request.getId();
        assertEquals(3, jobId.size());
        assertEquals("repository", jobId.get(0));
        assertEquals("xip", jobId.get(1));
    }

    @Test
    void exportExtensionWhenJobExecutionFails() throws JobException
    {
        JobException expectedException = new JobException("Failed to start XIP export job");
        when(this.jobExecutor.execute(eq(XIPExportJob.JOB_TYPE), any(XIPExportJobRequest.class)))
            .thenThrow(expectedException);

        assertNull(this.repositoryScriptService.exportExtension(extensionId, xwikiVersion));
        assertEquals(expectedException, this.repositoryScriptService.getLastError());
    }
}
