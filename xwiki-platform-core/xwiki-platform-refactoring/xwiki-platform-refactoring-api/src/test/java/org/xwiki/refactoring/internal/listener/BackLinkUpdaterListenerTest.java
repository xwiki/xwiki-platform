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
package org.xwiki.refactoring.internal.listener;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.job.JobContext;
import org.xwiki.link.LinkStore;
import org.xwiki.link.ReadyIndicator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.RefactoringException;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.refactoring.internal.job.DeleteJob;
import org.xwiki.refactoring.internal.job.RenameJob;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BackLinkUpdaterListener}.
 * 
 * @version $Id$
 */
@ComponentTest
class BackLinkUpdaterListenerTest
{
    @InjectMockComponents
    private BackLinkUpdaterListener listener;

    @MockComponent
    private ReferenceUpdater updater;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private JobContext jobContext;

    @MockComponent
    private LinkStore linkStore;

    @Mock
    private RenameJob renameJob;

    @Mock
    private DeleteJob deleteJob;

    @Mock
    private ReadyIndicator readyIndicator;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private DocumentReference aliceReference = new DocumentReference("foo", "Users", "Alice");

    private DocumentReference bobReference = new DocumentReference("foo", "Users", "Bob");

    private DocumentReference carolReference = new DocumentReference("foo", "Users", "Carol");

    private DocumentReference denisReference = new DocumentReference("bar", "Users", "Denis");

    private DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(aliceReference, bobReference);

    private DocumentDeletedEvent documentDeletedEvent = new DocumentDeletedEvent(aliceReference);

    private MoveRequest renameRequest = new MoveRequest();

    private DeleteRequest deleteRequest = new DeleteRequest();

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.modelBridge.getBackLinkedDocuments(aliceReference))
            .thenReturn(Set.of(carolReference, denisReference));

        when(this.jobContext.getCurrentJob()).thenReturn(deleteJob);
        when(this.deleteJob.getRequest()).thenReturn(deleteRequest);
        deleteRequest.setNewBacklinkTargets(Collections.singletonMap(aliceReference, bobReference));

        when(this.linkStore.waitReady()).thenReturn(this.readyIndicator);
        when(this.readyIndicator.getProgressPercentage()).thenReturn(100);
    }

    @Test
    void onDocumentRenamedWithUpdateLinksOnFarm()
    {
        renameRequest.setUpdateLinks(true);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.renameJob.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater).update(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
        assertEquals("Waiting for the link index to be updated.", logCapture.getMessage(1));
        assertEquals("The link index is ready now, starting the update of the back-links.", logCapture.getMessage(2));
    }

    @Test
    void onDocumentRenamedWithUpdateLinksOnFarmAndNoEditRight()
    {
        renameRequest.setUpdateLinks(true);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.renameJob.hasAccess(Right.EDIT, denisReference)).thenReturn(false);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater, never()).update(eq(denisReference), any(DocumentReference.class), any());

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
        assertEquals("Waiting for the link index to be updated.", logCapture.getMessage(1));
        assertEquals("The link index is ready now, starting the update of the back-links.", logCapture.getMessage(2));
    }

    @Test
    void onDocumentRenamedWithUpdateLinksOnWiki()
    {
        renameRequest.setUpdateLinks(true);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater, never()).update(eq(denisReference), any(DocumentReference.class), any());

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
        assertEquals("Waiting for the link index to be updated.", logCapture.getMessage(1));
        assertEquals("The link index is ready now, starting the update of the back-links.", logCapture.getMessage(2));
    }

    @Test
    void onDocumentRenamedWithoutUpdateLinks()
    {
        renameRequest.setUpdateLinks(false);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.updater, never()).update(any(), any(DocumentReference.class), any());
    }

    @Test
    void onDocumentRenamedWithoutRenameJob()
    {
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);
        when(this.jobContext.getCurrentJob()).thenReturn(null);

        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater).update(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
    }

    @Test
    void onDocumentRenamedWithoutRenameJobAndNoEditRight()
    {
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(false);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);
        when(this.jobContext.getCurrentJob()).thenReturn(null);

        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.updater, never()).update(eq(carolReference), any(DocumentReference.class), any());
        verify(this.updater).update(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
    }

    @Test
    void onDocumentDeletedWithUpdateLinksOnFarm()
    {
        deleteRequest.setUpdateLinks(true);

        when(this.deleteJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.deleteJob.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater).update(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
        assertEquals("Waiting for the link index to be updated.", logCapture.getMessage(1));
        assertEquals("The link index is ready now, starting the update of the back-links.", logCapture.getMessage(2));
    }

    @Test
    void onDocumentDeletedWithUpdateLinksOnFarmOnDocWithoutTarget() throws RefactoringException
    {
        deleteRequest.setUpdateLinks(true);

        DocumentReference docReference = new DocumentReference("wiki", "Users", "Ana");
        when(this.modelBridge.getBackLinkedDocuments(docReference)).thenReturn(Set.of(aliceReference));
        when(this.deleteJob.hasAccess(Right.EDIT, docReference)).thenReturn(true);

        this.listener.onEvent(new DocumentDeletedEvent(docReference), null, null);

        verify(this.updater, never()).update(eq(aliceReference), eq(docReference),
            any(DocumentReference.class));
    }

    @Test
    void onDocumentDeleteWithUpdateLinksOnFarmAndNoEditRight()
    {
        deleteRequest.setUpdateLinks(true);

        when(this.deleteJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.deleteJob.hasAccess(Right.EDIT, denisReference)).thenReturn(false);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater, never()).update(eq(denisReference), any(DocumentReference.class), any());

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
        assertEquals("Waiting for the link index to be updated.", logCapture.getMessage(1));
        assertEquals("The link index is ready now, starting the update of the back-links.", logCapture.getMessage(2));
    }

    @Test
    void onDocumentDeletedWithoutUpdateLinks()
    {
        deleteRequest.setUpdateLinks(false);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.updater, never()).update(any(), any(DocumentReference.class), any());
    }

    @Test
    void onDocumentDeletedWithoutDeleteJob()
    {
        when(this.jobContext.getCurrentJob()).thenReturn(null);
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.updater, never()).update(any(), any(DocumentReference.class), any());
    }

    @Test
    void onDocumentDeleteWithIndexingWaitingAndInterrupt() throws ExecutionException, InterruptedException,
        TimeoutException
    {
        deleteRequest.setUpdateLinks(true);

        when(this.readyIndicator.get(anyLong(), any())).thenThrow(new TimeoutException())
            .thenThrow(new InterruptedException());

        when(this.deleteJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.deleteJob.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater).update(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
        assertEquals("Waiting for the link index to be updated.", logCapture.getMessage(1));
        assertEquals("Interrupted while waiting for the link indexing to be updated, starting the updating of "
            + "back-links nevertheless.", logCapture.getMessage(2));
    }

    @Test
    void onDocumentDeleteWithIndexingWaitingError() throws ExecutionException, InterruptedException, TimeoutException
    {
        deleteRequest.setUpdateLinks(true);

        when(this.readyIndicator.get(anyLong(), any())).thenThrow(new ExecutionException(new RuntimeException()));

        when(this.deleteJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.deleteJob.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.updater).update(carolReference, aliceReference, bobReference);
        verify(this.updater).update(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice].", logCapture.getMessage(0));
        assertEquals("Waiting for the link index to be updated.", logCapture.getMessage(1));
        assertEquals("Link indexing stopped with an exception while waiting for indexing to complete, starting "
                + "the updating of back-links nevertheless.",
            logCapture.getMessage(2));
    }
}
