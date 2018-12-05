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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@ComponentTest
public class RequestFactoryTest
{
    @InjectMockComponents
    private RequestFactory requestFactory;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ModelContext modelContext;

    private DocumentReference userReference = new DocumentReference("wiki", "Users", "Carol");
    private WikiReference wikiReference = new WikiReference("wiki");

    @BeforeEach
    public void configure() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(this.userReference);
        when(documentAccessBridge.getCurrentAuthorReference()).thenReturn(this.userReference);
        when(modelContext.getCurrentEntityReference()).thenReturn(this.userReference);
    }

    @Test
    public void createRestoreRequest() throws Exception
    {
        List<Long> documentIds = Arrays.asList(1L, 2L, 3L);
        RestoreRequest restoreRequest =
            requestFactory.createRestoreRequest(documentIds);
        assertEquals(documentIds, restoreRequest.getDeletedDocumentIds());
        assertTrue(StringUtils.join(restoreRequest.getId(), '/')
            .startsWith(RefactoringJobs.RESTORE));
        assertTrue(restoreRequest.isCheckRights());
        assertEquals(wikiReference, restoreRequest.getWikiReference());
    }

    @Test
    public void createRestoreRequestBatchId() throws Exception
    {
        String batchId = "batch-id";
        RestoreRequest restoreRequest =
            requestFactory.createRestoreRequest(batchId);
        assertEquals(batchId, restoreRequest.getBatchId());
        assertTrue(StringUtils.join(restoreRequest.getId(), '/')
            .startsWith(RefactoringJobs.RESTORE));
        assertTrue(restoreRequest.isCheckRights());
        assertEquals(wikiReference, restoreRequest.getWikiReference());
    }

    @Test
    public void createPermanentlyDeleteRequest() throws Exception
    {
        List<Long> documentIds = Arrays.asList(1L, 2L, 3L);
        PermanentlyDeleteRequest permanentlyDeleteRequest =
            requestFactory.createPermanentlyDeleteRequest(documentIds);
        assertEquals(documentIds, permanentlyDeleteRequest.getDeletedDocumentIds());
        assertTrue(StringUtils.join(permanentlyDeleteRequest.getId(), '/')
            .startsWith(RefactoringJobs.PERMANENTLY_DELETE));
        assertTrue(permanentlyDeleteRequest.isCheckRights());
        assertEquals(wikiReference, permanentlyDeleteRequest.getWikiReference());
    }

    @Test
    public void createPermanentlyDeleteRequestBatch() throws Exception
    {
        String batchId = "batch-id";
        PermanentlyDeleteRequest permanentlyDeleteRequest =
            requestFactory.createPermanentlyDeleteRequest(batchId);
        assertEquals(batchId, permanentlyDeleteRequest.getBatchId());
        assertTrue(StringUtils.join(permanentlyDeleteRequest.getId(), '/')
            .startsWith(RefactoringJobs.PERMANENTLY_DELETE));
        assertTrue(permanentlyDeleteRequest.isCheckRights());
        assertEquals(wikiReference, permanentlyDeleteRequest.getWikiReference());
    }

    @Test
    public void createMoveRequest() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        MoveRequest moveRequest = requestFactory.createMoveRequest(source, destination);
        assertEquals(Arrays.asList(source), moveRequest.getEntityReferences());
        assertEquals(destination, moveRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "move"), moveRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.MOVE, moveRequest.getJobType());
        assertEquals(this.userReference, moveRequest.getUserReference());
        assertFalse(moveRequest.isDeep());
        assertTrue(moveRequest.isDeleteSource());
        assertTrue(moveRequest.isUpdateLinks());
        assertTrue(moveRequest.isUpdateParentField());
        assertTrue(moveRequest.isAutoRedirect());
        assertFalse(moveRequest.isInteractive());
        assertTrue(moveRequest.isCheckRights());
        moveRequest.setUpdateParentField(false);
        assertFalse(moveRequest.isUpdateParentField());
    }

    @Test
    public void createRenameRequest() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        MoveRequest renameRequest = requestFactory.createRenameRequest(source, destination);
        
        assertEquals(Arrays.asList(source), renameRequest.getEntityReferences());
        assertEquals(destination, renameRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "rename"), renameRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.RENAME, renameRequest.getJobType());
        assertEquals(this.userReference, renameRequest.getUserReference());
        assertFalse(renameRequest.isDeep());
        assertTrue(renameRequest.isDeleteSource());
        assertTrue(renameRequest.isUpdateLinks());
        assertTrue(renameRequest.isUpdateParentField());
        assertTrue(renameRequest.isAutoRedirect());
        assertFalse(renameRequest.isInteractive());
        assertTrue(renameRequest.isCheckRights());
    }

    @Test
    public void createRenameRequestString() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        String newName = "Bob";
        MoveRequest renameRequest = requestFactory.createRenameRequest(source, newName);

        EntityReference destination = new EntityReference(newName, source.getType(), source.getParent());
        assertEquals(Arrays.asList(source), renameRequest.getEntityReferences());
        assertEquals(destination, renameRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "rename"), renameRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.RENAME, renameRequest.getJobType());
        assertEquals(this.userReference, renameRequest.getUserReference());
        assertFalse(renameRequest.isDeep());
        assertTrue(renameRequest.isDeleteSource());
        assertTrue(renameRequest.isUpdateLinks());
        assertTrue(renameRequest.isUpdateParentField());
        assertTrue(renameRequest.isAutoRedirect());
        assertFalse(renameRequest.isInteractive());
        assertTrue(renameRequest.isCheckRights());
    }

    @Test
    public void createCopyRequest() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        CopyRequest copyRequest = requestFactory.createCopyRequest(source, destination);
        assertEquals(Arrays.asList(source), copyRequest.getEntityReferences());
        assertEquals(destination, copyRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "copy"), copyRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.COPY, copyRequest.getJobType());
        assertEquals(this.userReference, copyRequest.getUserReference());
        assertFalse(copyRequest.isDeep());
        assertTrue(copyRequest.isUpdateLinks());
        assertFalse(copyRequest.isInteractive());
        assertTrue(copyRequest.isCheckRights());
    }

    @Test
    public void createCopyAsRequest() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        CopyRequest copyRequest = requestFactory.createCopyAsRequest(source, destination);
        assertEquals(Arrays.asList(source), copyRequest.getEntityReferences());
        assertEquals(destination, copyRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "copyAs"), copyRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.COPY_AS, copyRequest.getJobType());
        assertEquals(this.userReference, copyRequest.getUserReference());
        assertFalse(copyRequest.isDeep());
        assertTrue(copyRequest.isUpdateLinks());
        assertFalse(copyRequest.isInteractive());
        assertTrue(copyRequest.isCheckRights());
    }

    @Test
    public void createCopyAsRequestString() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        String newPlace = "Bob";

        CopyRequest copyRequest = requestFactory.createCopyAsRequest(source, newPlace);
        EntityReference destination = new EntityReference(newPlace, source.getType(), source.getParent());
        assertEquals(Arrays.asList(source), copyRequest.getEntityReferences());
        assertEquals(destination, copyRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "copyAs"), copyRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.COPY_AS, copyRequest.getJobType());
        assertEquals(this.userReference, copyRequest.getUserReference());
        assertFalse(copyRequest.isDeep());
        assertTrue(copyRequest.isUpdateLinks());
        assertFalse(copyRequest.isInteractive());
        assertTrue(copyRequest.isCheckRights());
    }

    @Test
    public void createCreateRequest() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        CreateRequest createRequest = requestFactory.createCreateRequest(Arrays.asList(source));
        assertEquals(Arrays.asList(source), createRequest.getEntityReferences());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "create"), createRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.CREATE, createRequest.getJobType());
        assertEquals(this.userReference, createRequest.getUserReference());
        assertTrue(createRequest.isDeep());
        assertFalse(createRequest.isInteractive());
        assertTrue(createRequest.isCheckRights());
    }

    @Test
    public void createDeleteRequest() throws Exception
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        EntityRequest deleteRequest = requestFactory.createDeleteRequest(Arrays.asList(source));
        assertEquals(Arrays.asList(source), deleteRequest.getEntityReferences());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "delete"), deleteRequest.getId().subList(0, 2));
        assertEquals(RefactoringJobs.DELETE, deleteRequest.getJobType());
        assertEquals(this.userReference, deleteRequest.getUserReference());
        assertFalse(deleteRequest.isDeep());
        assertFalse(deleteRequest.isInteractive());
        assertTrue(deleteRequest.isCheckRights());
    }
}
