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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateRecycleBinStore;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UndeleteAction}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class UndeleteActionTest
{
    private static final DocumentReference DELETED_REFERENCE =
        new DocumentReference("xwiki", "Main", "DeletedDocument");

    private static final long ID = 13;

    @MockComponent
    private RequestFactory requestFactory;

    @MockComponent
    private CSRFToken csrfToken;

    @Mock
    private RefactoringScriptService refactoringScriptService;

    @MockComponent
    private JobExecutor jobExecutor;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Mock
    private XWikiRequest request;

    @Mock
    private Job job;

    @Mock
    private RestoreRequest jobRequest;

    @Mock
    private XWikiDeletedDocument deletedDocument;

    @Mock
    private XWikiHibernateRecycleBinStore recycleBinStore;

    /**
     * The object being tested.
     */
    private UndeleteAction undeleteAction = new UndeleteAction();

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(this.request);
        this.oldcore.getSpyXWiki().setRecycleBinStore(this.recycleBinStore);
        XWikiDocument contextDocument = mock(XWikiDocument.class);
        when(contextDocument.getDocumentReference()).thenReturn(DELETED_REFERENCE);
        this.oldcore.getXWikiContext().setDoc(contextDocument);

        when(this.jobExecutor.execute(anyString(), any())).thenReturn(this.job);

        this.componentManager.registerComponent(ScriptService.class, "refactoring", this.refactoringScriptService);
        when(this.refactoringScriptService.getRequestFactory()).thenReturn(this.requestFactory);
        when(this.requestFactory.createRestoreRequest(any(List.class))).thenReturn(this.jobRequest);
        when(this.requestFactory.createRestoreRequest(anyString())).thenReturn(this.jobRequest);

        when(this.request.getParameter("id")).thenReturn(String.valueOf(ID));

        when(this.deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getId()).thenReturn(ID);
        when(deletedDocument.getDocumentReference()).thenReturn(DELETED_REFERENCE);
        doReturn(this.deletedDocument).when(this.oldcore.getSpyXWiki()).getDeletedDocument(anyLong(),
            any(XWikiContext.class));
    }

    /**
     * Launches a RestoreJob with the current deleted document ID.
     */
    @Test
    void restoreSingleDocument() throws Exception
    {
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);

        when(this.recycleBinStore.hasAccess(any(), any(), any()))
            .thenReturn(true);
        assertFalse(this.undeleteAction.action(this.oldcore.getXWikiContext()));

        verify(this.requestFactory).createRestoreRequest(Arrays.asList(ID));
        verify(jobExecutor).execute(RefactoringJobs.RESTORE, jobRequest);
        verify(job).join();
    }

    @Test
    void restoreSingleDocumentWhenDeleter() throws Exception
    {
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);

        when(this.recycleBinStore.hasAccess(any(), any(), any()))
            .thenReturn(true);

        assertFalse(this.undeleteAction.action(this.oldcore.getXWikiContext()));

        verify(this.requestFactory).createRestoreRequest(Arrays.asList(ID));
        verify(jobExecutor).execute(RefactoringJobs.RESTORE, jobRequest);
        verify(job).join();
    }

    @Test
    void missingCSRFToken() throws Exception
    {
        // Invalid CSRF token.
        when(this.csrfToken.isTokenValid(null)).thenReturn(false);

        assertFalse(this.undeleteAction.action(this.oldcore.getXWikiContext()));

        // Verify that the resubmission URL was retrieved to be used in the redirect.
        verify(this.csrfToken).getResubmissionURL();
    }

    /**
     * When the recycle bin is disabled or when the deleted document ID is invalid, the document should not be restored.
     */
    @Test
    void recycleBinDisabledOrInvalidId() throws Exception
    {
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);

        // null is returned when the ID is invalid or the Recycle Bin is disabled.
        doReturn(null).when(this.oldcore.getSpyXWiki()).getDeletedDocument(anyLong(), any(XWikiContext.class));

        assertFalse(this.undeleteAction.action(this.oldcore.getXWikiContext()));

        // Verify that we never get this far.
        verify(this.requestFactory, never()).createRestoreRequest(Arrays.asList(ID));
    }

    /**
     * Show the "restore" UI with the option to include the batch when restoring and to see the contents of the batch of
     * the current deleted document.
     */
    @Test
    void showBatch() throws Exception
    {
        when(this.request.getParameter("showBatch")).thenReturn("true");

        when(this.recycleBinStore.hasAccess(any(), any(), any()))
            .thenReturn(true);
        assertTrue(this.undeleteAction.action(this.oldcore.getXWikiContext()));
        // Render the "restore" template.
        assertEquals("restore", undeleteAction.render(this.oldcore.getXWikiContext()));

        // Just make sure that we stop to the display, since the "confirm=true" parameter was not passed.
        verify(this.requestFactory, never()).createRestoreRequest(Arrays.asList(ID));
    }

    /**
     * Launches a RestoreJob with the batchId of the current deleted document.
     */
    @Test
    void restoreBatch() throws Exception
    {
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);

        String batchId = "abc123";

        when(deletedDocument.getBatchId()).thenReturn(batchId);

        // Go through the screen showing the option to include the batch and displaying its contents.
        when(this.request.getParameter("showBatch")).thenReturn("true");

        // Option to include the entire batch when restoring is enabled.
        when(this.request.getParameter("includeBatch")).thenReturn("true");

        // Confirmation button pressed.
        when(this.request.getParameter("confirm")).thenReturn("true");

        when(this.recycleBinStore.hasAccess(any(), any(), any()))
            .thenReturn(true);

        assertFalse(this.undeleteAction.action(this.oldcore.getXWikiContext()));

        verify(this.requestFactory).createRestoreRequest(batchId);
        verify(jobExecutor).execute(RefactoringJobs.RESTORE, jobRequest);
        verify(job).join();
    }

    /**
     * When trying to restore, rights are checked on the current deleted document, regardless if single or batch
     * restore.
     */
    @Test
    void notAllowedToRestoreSinglePage() throws Exception
    {
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);

        when(this.oldcore.getMockRightService().hasAccessLevel(any(), any(), any(), any())).thenReturn(false);

        assertTrue(this.undeleteAction.action(this.oldcore.getXWikiContext()));
        // Render the "accessdenied" template.
        assertEquals("accessdenied", undeleteAction.render(this.oldcore.getXWikiContext()));

        // Just make sure we don`t go any further.
        verify(this.requestFactory, never()).createRestoreRequest(Arrays.asList(ID));
    }

    /**
     * When trying to restore, rights are checked on the current deleted document, regardless if single or batch
     * restore.
     */
    @Test
    void notAllowedToRestoreBatch() throws Exception
    {
        when(this.csrfToken.isTokenValid(null)).thenReturn(true);

        String batchId = "abc123";

        when(this.deletedDocument.getBatchId()).thenReturn(batchId);

        // Go through the screen showing the option to include the batch and displaying its contents.
        when(this.request.getParameter("showBatch")).thenReturn("true");

        // Option to include the entire batch when restoring is enabled.
        when(this.request.getParameter("includeBatch")).thenReturn("true");

        // Confirmation button pressed.
        when(this.request.getParameter("confirm")).thenReturn("true");

        // No rights to restore the page when checking from the Action. The job will check individual rights.
        when(this.oldcore.getMockRightService().hasAccessLevel(any(), any(), any(), any())).thenReturn(false);

        assertTrue(this.undeleteAction.action(this.oldcore.getXWikiContext()));
        // Render the "accessdenied" template.
        assertEquals("accessdenied", undeleteAction.render(this.oldcore.getXWikiContext()));

        // Just make sure we don`t go any further.
        verify(this.requestFactory, never()).createRestoreRequest(batchId);
    }
}
