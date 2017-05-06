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
import org.mockito.Mockito;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

    private JobExecutor jobExecutor;

    private Job job = mock(Job.class);

    private RestoreRequest jobRequest;

    @Before
    public void setUp() throws Exception
    {
        mocker.registerMockComponent(CSRFToken.class);
        mocker.registerComponent(ScriptService.class, "refactoring", refactoringScriptService);
        Utils.setComponentManager(mocker);

        when(context.getRequest()).thenReturn(mock(XWikiRequest.class));

        when(context.getWiki()).thenReturn(xwiki);

        when(xwiki.getRightService()).thenReturn(rightsService);

        when(context.getDoc()).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Main", "DeletedDocument"));

        jobExecutor = mocker.registerMockComponent(JobExecutor.class);
        when(jobExecutor.execute(anyString(), any())).thenReturn(job);

        jobRequest = mock(RestoreRequest.class);
        when(refactoringScriptService.createRestoreRequest(any(List.class))).thenReturn(jobRequest);
    }

    @Test
    public void missingCSRFToken() throws Exception
    {
        assertFalse(undeleteAction.action(context));

        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        verify(csrfToken).isTokenValid(null);
    }

    /**
     * @see "XWIKI-9421: Attachment version is incremented when a document is restored from recycle bin"
     */
    @Test
    public void restore() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        when(xwiki.hasRecycleBin(context)).thenReturn(true);

        long id = 13;

        when(context.getRequest().getParameter("id")).thenReturn(String.valueOf(id));

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);
        when(deletedDocument.getId()).thenReturn(id);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        when(rightsService.hasAccessLevel(any(), any(), any(), any())).thenReturn(true);

        when(xwiki.exists(any(DocumentReference.class), any(XWikiContext.class))).thenReturn(false);

        assertFalse(undeleteAction.action(context));

        verify(refactoringScriptService).createRestoreRequest(Arrays.asList(id));
        verify(jobExecutor).execute(RefactoringJobs.RESTORE, jobRequest);
        verify(job).join();
    }

    /**
     * When the recycle bin is disabled, the document should not be restored.
     */
    @Test
    public void testRecycleBinDisabled() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        when(xwiki.hasRecycleBin(context)).thenReturn(false);

        when(context.getRequest().getParameter("id")).thenReturn("13");

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);

        when(xwiki.exists(any(DocumentReference.class), any(XWikiContext.class))).thenReturn(false);

        assertFalse(undeleteAction.action(context));

        verify(xwiki, Mockito.never()).restoreFromRecycleBin(document, 13, "Restored from recycle bin", context);
    }

    /**
     * When the location where to restore the document already exists, don`t override.
     */
    @Test
    public void testDocumentAlreadyExists() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        when(xwiki.hasRecycleBin(context)).thenReturn(true);

        when(context.getRequest().getParameter("id")).thenReturn("13");

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(xwiki.getDeletedDocument(anyLong(), any(XWikiContext.class))).thenReturn(deletedDocument);

        when(deletedDocument.getLocale()).thenReturn(Locale.ROOT);

        when(xwiki.exists(any(DocumentReference.class), any(XWikiContext.class))).thenReturn(true);

        assertFalse(undeleteAction.action(context));

        verify(xwiki, Mockito.never()).restoreFromRecycleBin(document, 13, "Restored from recycle bin", context);
    }

    /**
     * @see "XWIKI-9567: Cannot restore document translations from recycle bin"
     */
    @Test
    public void testRestoringTranslation() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        when(xwiki.hasRecycleBin(context)).thenReturn(true);

        when(context.getRequest().getParameter("id")).thenReturn("13");

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(xwiki.getDeletedDocument(any(), any(), anyInt(), any(XWikiContext.class))).thenReturn(
            deletedDocument);

        // Document to restore is a translation.
        when(deletedDocument.getLocale()).thenReturn(new Locale("ro"));

        DocumentReference translationDocumentReference =
            new DocumentReference(document.getDocumentReference(), deletedDocument.getLocale());
        when(xwiki.exists(translationDocumentReference, context)).thenReturn(false);

        assertFalse(undeleteAction.action(context));

        // Make sure that the main document is not checked for existence, but the translated document which we actually
        // want to restore.
        verify(xwiki, Mockito.never()).exists(document.getDocumentReference(), context);
        verify(xwiki).exists(translationDocumentReference, context);

        verify(xwiki).restoreFromRecycleBin(document, 13, "Restored from recycle bin", context);
    }
}
