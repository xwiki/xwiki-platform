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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.job.JobContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.internal.ModelBridge;
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
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    private LinkRefactoring linkRefactoring;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private JobContext jobContext;

    @Mock
    private RenameJob renameJob;

    @Mock
    private DeleteJob deleteJob;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

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
        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("foo", "bar"));
        when(this.modelBridge.getBackLinkedReferences(aliceReference, "foo")).thenReturn(Arrays.asList(carolReference));
        when(this.modelBridge.getBackLinkedReferences(aliceReference, "bar")).thenReturn(Arrays.asList(denisReference));

        when(this.jobContext.getCurrentJob()).thenReturn(deleteJob);
        when(this.deleteJob.getRequest()).thenReturn(deleteRequest);
        deleteRequest.setNewBacklinkTargets(Collections.singletonMap(aliceReference, bobReference));
    }

    @Test
    void onDocumentRenamedWithUpdateLinksOnFarm()
    {
        renameRequest.setUpdateLinks(true);
        renameRequest.setUpdateLinksOnFarm(true);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.renameJob.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring).renameLinks(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [bar].", logCapture.getMessage(1));
    }

    @Test
    void onDocumentRenamedWithUpdateLinksOnFarmAndNoEditRight()
    {
        renameRequest.setUpdateLinks(true);
        renameRequest.setUpdateLinksOnFarm(true);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.renameJob.hasAccess(Right.EDIT, denisReference)).thenReturn(false);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring, never()).renameLinks(eq(denisReference), any(DocumentReference.class), any());

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [bar].", logCapture.getMessage(1));
    }

    @Test
    void onDocumentRenamedWithUpdateLinksOnWiki()
    {
        renameRequest.setUpdateLinks(true);
        renameRequest.setUpdateLinksOnFarm(false);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring, never()).renameLinks(eq(denisReference), any(DocumentReference.class), any());

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
    }

    @Test
    void onDocumentRenamedWithoutUpdateLinks()
    {
        renameRequest.setUpdateLinks(false);
        renameRequest.setUpdateLinksOnFarm(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring, never()).renameLinks(any(), any(DocumentReference.class), any());
    }

    @Test
    void onDocumentRenamedWithoutRenameJob()
    {
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring).renameLinks(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [bar].", logCapture.getMessage(1));
    }

    @Test
    void onDocumentRenamedWithoutRenameJobAndNoEditRight()
    {
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(false);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.linkRefactoring, never()).renameLinks(eq(carolReference), any(DocumentReference.class), any());
        verify(this.linkRefactoring).renameLinks(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [bar].", logCapture.getMessage(1));
    }

    @Test
    void onDocumentDeletedWithUpdateLinksOnFarm()
    {
        deleteRequest.setUpdateLinks(true);
        deleteRequest.setUpdateLinksOnFarm(true);

        when(this.deleteJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.deleteJob.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring).renameLinks(denisReference, aliceReference, bobReference);

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [bar].", logCapture.getMessage(1));
    }

    @Test
    void onDocumentDeletedWithUpdateLinksOnFarmOnDocWithoutTarget()
    {
        deleteRequest.setUpdateLinks(true);
        deleteRequest.setUpdateLinksOnFarm(true);

        DocumentReference docReference = new DocumentReference("wiki", "Users", "Ana");
        when(this.modelBridge.getBackLinkedReferences(docReference, "wiki")).thenReturn(Arrays.asList(aliceReference));
        when(this.deleteJob.hasAccess(Right.EDIT, docReference)).thenReturn(true);

        this.listener.onEvent(new DocumentDeletedEvent(docReference), null, null);

        verify(this.linkRefactoring, never()).renameLinks(eq(aliceReference), eq(docReference),
            any(DocumentReference.class));
    }

    @Test
    void onDocumentDeleteWithUpdateLinksOnFarmAndNoEditRight()
    {
        deleteRequest.setUpdateLinks(true);
        deleteRequest.setUpdateLinksOnFarm(true);

        when(this.deleteJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.deleteJob.hasAccess(Right.EDIT, denisReference)).thenReturn(false);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring, never()).renameLinks(eq(denisReference), any(DocumentReference.class), any());

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [bar].", logCapture.getMessage(1));
    }

    @Test
    void onDocumentDeleteWithUpdateLinksOnWiki()
    {
        deleteRequest.setUpdateLinks(true);
        deleteRequest.setUpdateLinksOnFarm(false);

        when(this.deleteJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring, never()).renameLinks(eq(denisReference), any(DocumentReference.class), any());

        assertEquals("Updating the back-links for document [foo:Users.Alice] in wiki [foo].", logCapture.getMessage(0));
    }

    @Test
    void onDocumentDeletedWithoutUpdateLinks()
    {
        deleteRequest.setUpdateLinks(false);
        deleteRequest.setUpdateLinksOnFarm(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.linkRefactoring, never()).renameLinks(any(), any(DocumentReference.class), any());
    }

    @Test
    void onDocumentDeletedWithoutDeleteJob()
    {
        when(this.jobContext.getCurrentJob()).thenReturn(null);
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.linkRefactoring, never()).renameLinks(any(), any(DocumentReference.class), any());
    }
}
