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
package com.xpn.xwiki.web;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UndeleteAction}.
 *
 * @version $Id$
 */
public class UndeleteActionTest
{
    /**
     * A component manager that allows us to register mock components.
     */
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    /**
     * The object being tested.
     */
    private UndeleteAction undeleteAction = new UndeleteAction();

    /**
     * A mock {@link XWikiContext};
     */
    private XWikiContext context = mock(XWikiContext.class);

    private XWikiRequest request = mock(XWikiRequest.class);

    /**
     * A mock {@link XWiki};
     */
    private XWiki xwiki = mock(XWiki.class);

    /**
     * A mock {@link XWikiDocument};
     */
    private XWikiDocument document = mock(XWikiDocument.class);

    private XWikiRightService rightsService = mock(XWikiRightService.class);

    private RefactoringScriptService refactoringScriptService = mock(RefactoringScriptService.class);

    private RequestFactory requestFactory = mock(RequestFactory.class);

    private JobExecutor jobExecutor;

    private Job job = mock(Job.class);

    private RestoreRequest jobRequest;

    @Before
    public void setUp() throws Exception
    {
        mocker.registerMockComponent(CSRFToken.class);
        mocker.registerComponent(ScriptService.class, "refactoring", refactoringScriptService);
        Utils.setComponentManager(mocker);

        when(context.getRequest()).thenReturn(request);

        when(context.getWiki()).thenReturn(xwiki);

        when(xwiki.getRightService()).thenReturn(rightsService);

        when(context.getDoc()).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Main", "DeletedDocument"));

        jobExecutor = mocker.registerMockComponent(JobExecutor.class);
        when(jobExecutor.execute(anyString(), any())).thenReturn(job);

        jobRequest = mock(RestoreRequest.class);
        when(refactoringScriptService.getRequestFactory()).thenReturn(requestFactory);
        when(requestFactory.createRestoreRequest(any(List.class))).thenReturn(jobRequest);
        when(requestFactory.createRestoreRequest(anyString())).thenReturn(jobRequest);
    }

    /**
     * Launches a RestoreJob with the current deleted document ID.
     */
    @Test
    public void restoreSingleDocument() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        long id = 13;

        when(request.getParameter("id")).thenReturn(String.valueOf(id));

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getId()).thenReturn(id);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        when(rightsService.hasAccessLevel(any(), any(), any(), any())).thenReturn(true);

        assertFalse(undeleteAction.action(context));

        verify(requestFactory).createRestoreRequest(Arrays.asList(id));
        verify(jobExecutor).execute(RefactoringJobs.RESTORE, jobRequest);
        verify(job).join();
    }

    @Test
    public void missingCSRFToken() throws Exception
    {
        // Valid Deleted document ID.
        long id = 13;

        when(request.getParameter("id")).thenReturn(String.valueOf(id));

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        // Invalid CSRF token.
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(false);

        assertFalse(undeleteAction.action(context));

        // Verify that the resubmission URL was retrieved to be used in the redirect.
        verify(csrfToken).getResubmissionURL();
    }

    /**
     * When the recycle bin is disabled or when the deleted document ID is invalid, the document should not be restored.
     */
    @Test
    public void recycleBinDisabledOrInvalidId() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        long id = 13;

        when(request.getParameter("id")).thenReturn(String.valueOf(id));

        // null is returned when the ID is invalid or the Recycle Bin is disabled.
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(null);

        assertFalse(undeleteAction.action(context));

        // Verify that we never get this far.
        verify(requestFactory, never()).createRestoreRequest(Arrays.asList(id));
    }

    /**
     * Show the "restore" UI with the option to include the batch when restoring and to see the contents of the batch of
     * the current deleted document.
     */
    @Test
    public void showBatch() throws Exception
    {
        long id = 13;

        when(request.getParameter("id")).thenReturn(String.valueOf(id));

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getId()).thenReturn(id);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        when(request.getParameter("showBatch")).thenReturn("true");

        when(rightsService.hasAccessLevel(any(), any(), any(), any())).thenReturn(true);

        assertTrue(undeleteAction.action(context));
        // Render the "restore" template.
        assertEquals("restore", undeleteAction.render(context));

        // Just make sure that we stop to the display, since the "confirm=true" parameter was not passed.
        verify(requestFactory, never()).createRestoreRequest(Arrays.asList(id));
    }

    /**
     * Launches a RestoreJob with the batchId of the current deleted document.
     */
    @Test
    public void restoreBatch() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        long id = 13;
        String batchId = "abc123";

        when(request.getParameter("id")).thenReturn(String.valueOf(id));

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getId()).thenReturn(id);
        when(deletedDocument.getBatchId()).thenReturn(batchId);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        // Go through the screen showing the option to include the batch and displaying its contents.
        when(request.getParameter("showBatch")).thenReturn("true");

        // Option to include the entire batch when restoring is enabled.
        when(request.getParameter("includeBatch")).thenReturn("true");

        // Confirmation button pressed.
        when(request.getParameter("confirm")).thenReturn("true");

        when(rightsService.hasAccessLevel(any(), any(), any(), any())).thenReturn(true);

        assertFalse(undeleteAction.action(context));

        verify(requestFactory).createRestoreRequest(batchId);
        verify(jobExecutor).execute(RefactoringJobs.RESTORE, jobRequest);
        verify(job).join();
    }

    /**
     * When trying to restore, rights are checked on the current deleted document, regardless if single or batch
     * restore.
     */
    @Test
    public void notAllowedToRestoreSinglePage() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        long id = 13;

        when(request.getParameter("id")).thenReturn(String.valueOf(id));

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getId()).thenReturn(id);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        when(rightsService.hasAccessLevel(any(), any(), any(), any())).thenReturn(false);

        assertTrue(undeleteAction.action(context));
        // Render the "accessdenied" template.
        assertEquals("accessdenied", undeleteAction.render(context));

        // Just make sure we don`t go any further.
        verify(requestFactory, never()).createRestoreRequest(Arrays.asList(id));
    }

    /**
     * When trying to restore, rights are checked on the current deleted document, regardless if single or batch
     * restore.
     */
    @Test
    public void notAllowedToRestoreBatch() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        long id = 13;
        String batchId = "abc123";

        when(request.getParameter("id")).thenReturn(String.valueOf(id));

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getId()).thenReturn(id);
        when(deletedDocument.getBatchId()).thenReturn(batchId);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        // Go through the screen showing the option to include the batch and displaying its contents.
        when(request.getParameter("showBatch")).thenReturn("true");

        // Option to include the entire batch when restoring is enabled.
        when(request.getParameter("includeBatch")).thenReturn("true");

        // Confirmation button pressed.
        when(request.getParameter("confirm")).thenReturn("true");

        // No rights to restore the page when checking from the Action. The job will check individual rights.
        when(rightsService.hasAccessLevel(any(), any(), any(), any())).thenReturn(false);

        assertTrue(undeleteAction.action(context));
        // Render the "accessdenied" template.
        assertEquals("accessdenied", undeleteAction.render(context));

        // Just make sure we don`t go any further.
        verify(requestFactory, never()).createRestoreRequest(batchId);
    }
}
