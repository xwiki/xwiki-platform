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
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.job.AbstractCopyOrMoveRequest;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RefactoringScriptService}.
 * 
 * @version $Id$
 */
@ComponentTest
public class RefactoringScriptServiceTest
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

    private ExecutionContext executionContext = new ExecutionContext();

    @BeforeEach
    public void setup()
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
    public void move() throws Exception
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
    public void moveWithoutPR() throws Exception
    {
        MoveRequest request = new MoveRequest();
        request.setCheckRights(false);
        request.setUserReference(new DocumentReference("wiki", "Users", "Bob"));

        this.refactoringScriptService.move(request);

        verify(this.requestFactory).setRightsProperties(request);
    }

    @Test
    public void moveWithPR() throws Exception
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
    public void moveWithException() throws Exception
    {
        MoveRequest request = new MoveRequest();
        JobException exception = new JobException("Some error message");
        when(this.jobExecutor.execute(RefactoringJobs.MOVE, request)).thenThrow(exception);

        assertNull(this.refactoringScriptService.move(request));
        assertSame(exception, this.refactoringScriptService.getLastError());
    }

    @Test
    public void rename() throws Exception
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
    public void copy() throws Exception
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
    public void copyAs() throws Exception
    {
        SpaceReference spaceReference =
            new SpaceReference("Alice", new SpaceReference("Users", new WikiReference("dev")));
        String newName = "Bob";
        CopyRequest copyRequest = new CopyRequest();
        this.fillCopyOrMoveRequest(copyRequest);

        when(this.requestFactory.createCopyAsRequest(spaceReference, newName)).thenReturn(copyRequest);
        this.refactoringScriptService.copyAs(spaceReference, newName);
        verify(this.requestFactory).createCopyAsRequest(spaceReference, newName);
        verify(this.jobExecutor).execute(RefactoringJobs.COPY, copyRequest);
    }

    @Test
    public void delete() throws Exception
    {
        WikiReference source = new WikiReference("math");

        EntityRequest deleteRequest = new EntityRequest();
        this.fillEntityRequest(deleteRequest);
        when(this.requestFactory.createDeleteRequest(Arrays.asList(source))).thenReturn(deleteRequest);

        this.refactoringScriptService.delete(source);
        verify(this.requestFactory).createDeleteRequest(Arrays.asList(source));
        verify(this.jobExecutor).execute(RefactoringJobs.DELETE, deleteRequest);
    }

    @Test
    public void create() throws Exception
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
    public void convertToNestedDocumentAlreadyNested() throws Exception
    {
        DocumentReference nestedDocumentReference =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");

        assertNull(this.refactoringScriptService.convertToNestedDocument(nestedDocumentReference));
    }

    @Test
    public void convertToNestedDocument() throws Exception
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
    public void convertToTerminalDocumentAlreadyTerminal() throws Exception
    {
        DocumentReference terminalDocumentReference = new DocumentReference("code", "Model", "Entity");
        assertNull(this.refactoringScriptService.convertToTerminalDocument(terminalDocumentReference));

        DocumentReference rootDocumentReference = new DocumentReference("wiki", "Space", "WebHome");
        assertNull(this.refactoringScriptService.convertToTerminalDocument(rootDocumentReference));
    }

    @Test
    public void convertToTerminalDocument() throws Exception
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
    public void restore() throws Exception
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
    public void restoreBatch() throws Exception
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
    public void permanentlyDelete() throws Exception
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
    public void permanentlyDeleteBatch() throws Exception
    {
        String batchid = "mybatch-id";
        PermanentlyDeleteRequest permanentlyDeleteRequest = new PermanentlyDeleteRequest();
        permanentlyDeleteRequest.setBatchId(batchid);
        when(this.requestFactory.createPermanentlyDeleteRequest(batchid)).thenReturn(permanentlyDeleteRequest);

        this.refactoringScriptService.permanentlyDelete(batchid);
        verify(this.requestFactory).createPermanentlyDeleteRequest(batchid);
        verify(this.jobExecutor).execute(RefactoringJobs.PERMANENTLY_DELETE, permanentlyDeleteRequest);
    }
}
