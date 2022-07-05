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
package org.xwiki.export.pdf.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.export.pdf.internal.job.PDFExportJob;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobRequestFactory;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PDFExportScriptService}.
 * 
 * @version $Id$
 * @since 14.4.3
 * @since 14.5.1
 * @since 14.6RC1
 */
@ComponentTest
class PDFExportScriptServiceTest
{
    @InjectMockComponents
    private PDFExportScriptService service;

    @MockComponent
    private PDFExportJobRequestFactory requestFactory;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private Execution execution;

    @Mock
    private PDFExportJobRequest request;

    @Mock
    private Job job;

    @BeforeEach
    void configure()
    {
        ExecutionContext context = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(context);
    }

    @Test
    void createRequest() throws Exception
    {
        when(this.requestFactory.createRequest()).thenReturn(this.request);
        assertSame(this.request, this.service.createRequest());
    }

    @Test
    void createRequestWithException() throws Exception
    {
        Exception exception = new Exception();
        when(this.requestFactory.createRequest()).thenThrow(exception);
        assertNull(this.service.createRequest());
        assertSame(exception, this.service.getLastError());
    }

    @Test
    void executeWithoutPR() throws Exception
    {
        when(this.jobExecutor.execute(PDFExportJob.JOB_TYPE, this.request)).thenReturn(this.job);

        assertSame(job, this.service.execute(this.request));

        verify(this.requestFactory).setRightsProperties(this.request);
    }

    @Test
    void executeWithPR() throws Exception
    {
        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(true);
        when(this.jobExecutor.execute(PDFExportJob.JOB_TYPE, this.request)).thenReturn(this.job);

        assertSame(job, this.service.execute(this.request));

        verify(this.requestFactory, never()).setRightsProperties(this.request);
    }

    @Test
    void executeWithException() throws Exception
    {
        JobException exception = new JobException("Failed!");
        when(this.jobExecutor.execute(PDFExportJob.JOB_TYPE, this.request)).thenThrow(exception);

        assertNull(this.service.execute(this.request));
        assertSame(exception, this.service.getLastError());
    }
}
