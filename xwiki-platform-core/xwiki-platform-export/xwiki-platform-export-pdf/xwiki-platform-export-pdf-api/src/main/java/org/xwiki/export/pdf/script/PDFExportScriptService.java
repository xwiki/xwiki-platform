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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.export.pdf.internal.job.PDFExportJob;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobRequestFactory;
import org.xwiki.export.script.ExportScriptService;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Scripting API to export documents as PDF.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5RC1
 */
@Component
@Named(PDFExportScriptService.ROLE_HINT)
@Singleton
@Unstable
public class PDFExportScriptService implements ScriptService
{
    /**
     * The role hint of this script service.
     */
    public static final String ROLE_HINT = ExportScriptService.ROLE_HINT + ".pdf";

    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String PDF_EXPORT_ERROR_KEY = String.format("scriptservice.%s.error", ROLE_HINT);

    /**
     * Used to create and initialize PDF export requests.
     */
    @Inject
    private PDFExportJobRequestFactory requestFactory;

    /**
     * Used to check user rights.
     */
    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * Used to execute the PDF export jobs.
     */
    @Inject
    private JobExecutor jobExecutor;

    /**
     * Provides access to the current context, used to store and retrieve caught exceptions.
     */
    @Inject
    private Execution execution;

    /**
     * @return a new PDF export request, initialized based on the current HTTP request
     */
    public PDFExportJobRequest createRequest()
    {
        setError(null);

        try {
            return this.requestFactory.createRequest();
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Schedule the execution of the given PDF export request.
     * 
     * @param request the PDF export request to execute
     * @return the asynchronous background job that has been scheduled to execute the given PDF export request
     */
    public Job execute(PDFExportJobRequest request)
    {
        setError(null);

        // Make sure that only the PR users can change the rights and context properties from the request.
        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            this.requestFactory.setRightsProperties(request);
        }

        try {
            return this.jobExecutor.execute(PDFExportJob.JOB_TYPE, request);
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(PDF_EXPORT_ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(PDF_EXPORT_ERROR_KEY, e);
    }
}
