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

import java.net.URL;
import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.job.PDFExportJob;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobRequestFactory;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
    @Named("chrome")
    private PDFPrinter<URL> pdfPrinter;

    @MockComponent
    private PDFExportConfiguration configuration;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private Execution execution;

    /**
     * Used to return script-safe objects.
     */
    @MockComponent
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider scriptSafeProvider;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

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

    @Test
    void isServerSidePrintingAvailable()
    {
        assertFalse(this.service.isServerSidePrintingAvailable());
        assertNull(this.service.getLastError());

        when(this.pdfPrinter.isAvailable()).thenReturn(true);
        assertTrue(this.service.isServerSidePrintingAvailable());
        assertNull(this.service.getLastError());

        when(this.pdfPrinter.isAvailable()).thenThrow(new RuntimeException("Failed to initialize."));
        assertFalse(this.service.isServerSidePrintingAvailable());
        assertEquals("Failed to initialize.", this.service.getLastError().getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getConfiguration()
    {
        PDFExportConfiguration safeConfig = mock(PDFExportConfiguration.class, "safe");
        when(this.scriptSafeProvider.get(this.configuration)).thenReturn(safeConfig);
        assertEquals(safeConfig, this.service.getConfiguration());
        assertNull(this.service.getLastError());

        when(this.authorization.hasAccess(Right.ADMIN)).thenReturn(true);
        assertEquals(this.configuration, this.service.getConfiguration());
        assertNull(this.service.getLastError());

        when(this.authorization.hasAccess(Right.ADMIN)).thenReturn(false);
        when(this.scriptSafeProvider.get(this.configuration)).thenThrow(new RuntimeException("Failed!"));
        assertNull(this.service.getConfiguration());
        assertEquals("Failed!", this.service.getLastError().getMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    void isEnabled() throws Exception
    {
        DocumentReference firstTemplate = new DocumentReference("test", "First", "Template");
        DocumentReference secondTemplate = new DocumentReference("test", "Second", "Template");

        PDFExportConfiguration safeConfig = mock(PDFExportConfiguration.class, "safe");
        when(this.scriptSafeProvider.get(this.configuration)).thenReturn(safeConfig);
        when(safeConfig.getTemplates()).thenReturn(Arrays.asList(firstTemplate, secondTemplate));

        assertFalse(this.service.isEnabled());

        when(this.documentAccessBridge.exists(secondTemplate)).thenReturn(true);

        assertTrue(this.service.isEnabled());
    }
}
