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
package org.xwiki.refactoring.script;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.RefactoringConfiguration;
import org.xwiki.refactoring.job.AbstractCopyOrMoveRequest;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.refactoring.job.RestoreRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RefactoringScriptService}.
 * 
 * @version $Id$
 */
@ComponentTest
class RefactoringScriptServiceTest
{
    @InjectMockComponents
    private RefactoringScriptService refactoringScriptService;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private RequestFactory requestFactory;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private Execution execution;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private RefactoringConfiguration configuration;

    private final ExecutionContext executionContext = new ExecutionContext();

    @BeforeEach
    void setup()
    {
        when(defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("WebHome", EntityType.DOCUMENT));
        when(execution.getContext()).thenReturn(executionContext);
    }

    private void fillCopyOrMoveRequest(AbstractCopyOrMoveRequest copyOrMoveRequest)
    {
        this.fillEntityRequest(copyOrMoveRequest);
        copyOrMoveRequest.setDestination(new SpaceReference("wiki", "newSpace"));
    }

    private void fillEntityRequest(EntityRequest entityRequest)
    {
        entityRequest.setEntityReferences(Arrays.asList(new SpaceReference("wiki", "Space")));
    }

    @Test
    void move() throws Exception
    {
        SpaceReference source = new SpaceReference("Space", new WikiReference("math"));
        WikiReference destination = new WikiReference("code");
        MoveRequest moveRequest = new MoveRequest();
        this.fillCopyOrMoveRequest(moveRequest);
        when(this.requestFactory.createMoveRequest(source, destination)).thenReturn(moveRequest);

        this.refactoringScriptService.move(source, destination);
        verify(this.requestFactory).createMoveRequest(source, destination);
        verify(this.jobExecutor).execute(RefactoringJobs.MOVE, moveRequest);
    }

    @Test
    void moveWithoutPR()
    {
        MoveRequest request = new MoveRequest();
        request.setCheckRights(false);
        request.setUserReference(new DocumentReference("wiki", "Users", "Bob"));

        DocumentReference authorReference = mock(DocumentReference.class);
        DocumentReference userReference = mock(DocumentReference.class);
        when(this.documentAccessBridge.getCurrentAuthorReference()).thenReturn(authorReference);
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(userReference);

        this.refactoringScriptService.move(request);

        assertTrue(request.isCheckRights());
        assertSame(userReference, request.getUserReference());
        assertSame(authorReference, request.getAuthorReference());
    }

    @Test
    void moveWithPR()
    {
        MoveRequest request = new MoveRequest();
        request.setCheckRights(false);

        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        request.setUserReference(bobReference);

        when(authorization.hasAccess(Right.PROGRAM)).thenReturn(true);

        this.refactoringScriptService.move(request);

        assertFalse(request.isCheckRights());
        assertEquals(bobReference, request.getUserReference());
    }

    @Test
    void moveWithException() throws Exception
    {
        MoveRequest request = new MoveRequest();
        JobException exception = new JobException("Some error message");
        when(this.jobExecutor.execute(RefactoringJobs.MOVE, request)).thenThrow(exception);

        assertNull(this.refactoringScriptService.move(request));
        assertSame(exception, this.refactoringScriptService.getLastError());
    }

    @Test
    void rename() throws Exception
    {
        SpaceReference spaceReference =
            new SpaceReference("Alice", new SpaceReference("Users", new WikiReference("dev")));
        String newName = "Bob";
        MoveRequest moveRequest = new MoveRequest();
        this.fillCopyOrMoveRequest(moveRequest);
        when(this.requestFactory.createRenameRequest(spaceReference, newName)).thenReturn(moveRequest);

        this.refactoringScriptService.rename(spaceReference, newName);
        verify(this.requestFactory).createRenameRequest(spaceReference, newName);
        verify(this.jobExecutor).execute(RefactoringJobs.RENAME, moveRequest);
    }

    @Test
    void copy() throws Exception
    {
        SpaceReference source = new SpaceReference("Space", new WikiReference("math"));
        WikiReference destination = new WikiReference("code");
        CopyRequest copyRequest = new CopyRequest();
        this.fillCopyOrMoveRequest(copyRequest);
        when(this.requestFactory.createCopyRequest(source, destination)).thenReturn(copyRequest);

        this.refactoringScriptService.copy(source, destination);
        verify(this.requestFactory).createCopyRequest(source, destination);
        verify(this.jobExecutor).execute(RefactoringJobs.COPY, copyRequest);
    }

    @Test
    void copyAs() throws Exception
    {
        SpaceReference spaceReference =
            new SpaceReference("Alice", new SpaceReference("Users", new WikiReference("dev")));
        String newName = "Bob";
        CopyRequest copyRequest = new CopyRequest();
        this.fillCopyOrMoveRequest(copyRequest);

        when(this.requestFactory.createCopyAsRequest(spaceReference, newName)).thenReturn(copyRequest);
        this.refactoringScriptService.copyAs(spaceReference, newName);
        verify(this.requestFactory).createCopyAsRequest(spaceReference, newName);
        verify(this.jobExecutor).execute(RefactoringJobs.COPY_AS, copyRequest);
    }

    @Test
    void delete() throws Exception
    {
        WikiReference source = new WikiReference("math");

        DeleteRequest deleteRequest = new DeleteRequest();
        this.fillEntityRequest(deleteRequest);
        when(this.requestFactory.createDeleteRequest(Arrays.asList(source))).thenReturn(deleteRequest);

        this.refactoringScriptService.delete(source);
        verify(this.requestFactory).createDeleteRequest(Arrays.asList(source));
        verify(this.jobExecutor).execute(RefactoringJobs.DELETE, deleteRequest);
    }

    @Test
    void create() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        CreateRequest createRequest = new CreateRequest();
        this.fillEntityRequest(createRequest);
        when(this.requestFactory.createCreateRequest(Arrays.asList(documentReference))).thenReturn(createRequest);

        this.refactoringScriptService.create(documentReference);
        verify(this.requestFactory).createCreateRequest(Arrays.asList(documentReference));
        verify(this.jobExecutor).execute(RefactoringJobs.CREATE, createRequest);
    }

    @Test
    void convertToNestedDocumentAlreadyNested()
    {
        DocumentReference nestedDocumentReference =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");

        assertNull(this.refactoringScriptService.convertToNestedDocument(nestedDocumentReference));
    }

    @Test
    void convertToNestedDocument() throws Exception
    {
        DocumentReference terminalDocumentReference = new DocumentReference("code", "Model", "Entity");
        DocumentReference nestedDocumentReference =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        MoveRequest moveRequest = new MoveRequest();
        this.fillCopyOrMoveRequest(moveRequest);
        when(this.requestFactory.createRenameRequest(terminalDocumentReference, nestedDocumentReference))
            .thenReturn(moveRequest);

        this.refactoringScriptService.convertToNestedDocument(terminalDocumentReference);
        verify(this.requestFactory).createRenameRequest(terminalDocumentReference, nestedDocumentReference);
        verify(this.jobExecutor).execute(RefactoringJobs.RENAME, moveRequest);
    }

    @Test
    void convertToTerminalDocumentAlreadyTerminal()
    {
        DocumentReference terminalDocumentReference = new DocumentReference("code", "Model", "Entity");
        assertNull(this.refactoringScriptService.convertToTerminalDocument(terminalDocumentReference));

        DocumentReference rootDocumentReference = new DocumentReference("wiki", "Space", "WebHome");
        assertNull(this.refactoringScriptService.convertToTerminalDocument(rootDocumentReference));
    }

    @Test
    void convertToTerminalDocument() throws Exception
    {
        DocumentReference terminalDocumentReference = new DocumentReference("code", "Model", "Entity");
        DocumentReference nestedDocumentReference =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");

        MoveRequest moveRequest = new MoveRequest();
        this.fillCopyOrMoveRequest(moveRequest);
        when(this.requestFactory.createRenameRequest(nestedDocumentReference, terminalDocumentReference))
            .thenReturn(moveRequest);

        this.refactoringScriptService.convertToTerminalDocument(nestedDocumentReference);
        verify(this.requestFactory).createRenameRequest(nestedDocumentReference, terminalDocumentReference);
        verify(this.jobExecutor).execute(RefactoringJobs.RENAME, moveRequest);
    }

    @Test
    void restore() throws Exception
    {
        List<Long> documentIds = Arrays.asList(1L, 2L, 3L);
        RestoreRequest restoreRequest = new RestoreRequest();
        restoreRequest.setDeletedDocumentIds(documentIds);
        when(this.requestFactory.createRestoreRequest(documentIds)).thenReturn(restoreRequest);

        this.refactoringScriptService.restore(documentIds);
        verify(this.requestFactory).createRestoreRequest(documentIds);
        verify(this.jobExecutor).execute(RefactoringJobs.RESTORE, restoreRequest);
    }

    @Test
    void restoreBatch() throws Exception
    {
        String batchid = "mybatch-id";
        RestoreRequest restoreRequest = new RestoreRequest();
        restoreRequest.setBatchId(batchid);
        when(this.requestFactory.createRestoreRequest(batchid)).thenReturn(restoreRequest);

        this.refactoringScriptService.restore(batchid);
        verify(this.requestFactory).createRestoreRequest(batchid);
        verify(this.jobExecutor).execute(RefactoringJobs.RESTORE, restoreRequest);
    }

    @Test
    void permanentlyDelete() throws Exception
    {
        List<Long> documentIds = Arrays.asList(1L, 2L, 3L);
        PermanentlyDeleteRequest permanentlyDeleteRequest = new PermanentlyDeleteRequest();
        permanentlyDeleteRequest.setDeletedDocumentIds(documentIds);
        when(this.requestFactory.createPermanentlyDeleteRequest(documentIds)).thenReturn(permanentlyDeleteRequest);

        this.refactoringScriptService.permanentlyDelete(documentIds);
        verify(this.requestFactory).createPermanentlyDeleteRequest(documentIds);
        verify(this.jobExecutor).execute(RefactoringJobs.PERMANENTLY_DELETE, permanentlyDeleteRequest);
    }

    @Test
    void permanentlyDeleteBatch() throws Exception
    {
        String batchid = "mybatch-id";
        PermanentlyDeleteRequest permanentlyDeleteRequest = new PermanentlyDeleteRequest();
        permanentlyDeleteRequest.setBatchId(batchid);
        when(this.requestFactory.createPermanentlyDeleteRequest(batchid)).thenReturn(permanentlyDeleteRequest);

        this.refactoringScriptService.permanentlyDelete(batchid);
        verify(this.requestFactory).createPermanentlyDeleteRequest(batchid);
        verify(this.jobExecutor).execute(RefactoringJobs.PERMANENTLY_DELETE, permanentlyDeleteRequest);
    }

    @Test
    void changeDocumentAuthor() throws Exception
    {
        DocumentReference alice = new DocumentReference("dev", "Users", "Alice");
        DocumentReference bob = new DocumentReference("test", "Users", "Bob");

        ReplaceUserRequest request = mock(ReplaceUserRequest.class);
        when(this.requestFactory.createReplaceUserRequest(alice, bob)).thenReturn(request);

        Job job = mock(Job.class);
        when(this.jobExecutor.execute(RefactoringJobs.REPLACE_USER, request)).thenReturn(job);

        assertSame(job, this.refactoringScriptService.changeDocumentAuthor(alice, bob));

        verify(request).setReplaceDocumentAuthor(true);
        verify(request).setReplaceDocumentContentAuthor(true);
    }

    @Test
    void isRecycleBinSkippingAllowedWhenAdvancedUserAndRecycleBinSkippingIsActivated()
    {
        when(this.documentAccessBridge.isAdvancedUser()).thenReturn(true);
        when(this.configuration.isRecycleBinSkippingActivated()).thenReturn(true);
        boolean actual = this.refactoringScriptService.isRecycleBinSkippingAllowed();
        assertTrue(actual);
    }

    @Test
    void isRecycleBinSkippingAllowedWhenAdvancedUserAndRecycleBinSkippingDeactivated()
    {
        when(this.documentAccessBridge.isAdvancedUser()).thenReturn(true);
        when(this.configuration.isRecycleBinSkippingActivated()).thenReturn(false);
        boolean actual = this.refactoringScriptService.isRecycleBinSkippingAllowed();
        assertFalse(actual);
    }

    @Test
    void isRecycleBinSkippingAllowedWhenSimpleUserAndRecycleBinSkippingDeactivated()
    {
        when(this.configuration.isRecycleBinSkippingActivated()).thenReturn(false);
        boolean actual = this.refactoringScriptService.isRecycleBinSkippingAllowed();
        assertFalse(actual);
    }

    @Test
    void isRecycleBinSkippingAllowedWhenSimpleUser()
    {
        when(this.documentAccessBridge.isAdvancedUser()).thenReturn(false);
        when(this.configuration.isRecycleBinSkippingActivated()).thenReturn(true);
        boolean actual = this.refactoringScriptService.isRecycleBinSkippingAllowed();
        assertFalse(actual);
    }
}
