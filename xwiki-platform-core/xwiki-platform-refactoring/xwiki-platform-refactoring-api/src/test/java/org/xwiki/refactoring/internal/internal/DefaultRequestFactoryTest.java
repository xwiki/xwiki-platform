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
package org.xwiki.refactoring.internal.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.internal.script.DefaultRequestFactory;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultRequestFactoryTest
{
    @InjectMockComponents
    private DefaultRequestFactory requestFactory;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    private DocumentReference userReference = new DocumentReference("wiki", "Users", "Carol");

    private WikiReference wikiReference = new WikiReference("wiki");

    @BeforeEach
    void configure()
    {
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(this.userReference);
        when(documentAccessBridge.getCurrentAuthorReference()).thenReturn(this.userReference);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn(this.wikiReference.getName());
    }

    @Test
    void createRestoreRequest()
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
    void createRestoreRequestBatchId()
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
    void createPermanentlyDeleteRequest()
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
    void createPermanentlyDeleteRequestBatch()
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
    void createMoveRequest()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        MoveRequest moveRequest = requestFactory.createMoveRequest(source, destination);
        assertEquals(Arrays.asList(source), moveRequest.getEntityReferences());
        assertEquals(destination, moveRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "move"), moveRequest.getId().subList(0, 2));
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
    void createRenameRequest()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        MoveRequest renameRequest = requestFactory.createRenameRequest(source, destination);
        
        assertEquals(Arrays.asList(source), renameRequest.getEntityReferences());
        assertEquals(destination, renameRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "rename"), renameRequest.getId().subList(0, 2));
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
    void createRenameRequestString()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        String newName = "Bob";
        MoveRequest renameRequest = requestFactory.createRenameRequest(source, newName);

        EntityReference destination = new EntityReference(newName, source.getType(), source.getParent());
        assertEquals(Arrays.asList(source), renameRequest.getEntityReferences());
        assertEquals(destination, renameRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "rename"), renameRequest.getId().subList(0, 2));
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
    void createCopyRequest()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        CopyRequest copyRequest = requestFactory.createCopyRequest(source, destination);
        assertEquals(Arrays.asList(source), copyRequest.getEntityReferences());
        assertEquals(destination.getParent(), copyRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "copy"), copyRequest.getId().subList(0, 2));
        assertEquals(this.userReference, copyRequest.getUserReference());
        assertFalse(copyRequest.isDeep());
        assertTrue(copyRequest.isUpdateLinks());
        assertFalse(copyRequest.isInteractive());
        assertTrue(copyRequest.isCheckRights());
    }

    @Test
    void createCopyAsRequest()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        DocumentReference destination =
            new DocumentReference("code", Arrays.asList("Model", "Entity"), "WebHome");
        CopyRequest copyRequest = requestFactory.createCopyAsRequest(source, destination);
        assertEquals(Arrays.asList(source), copyRequest.getEntityReferences());
        assertEquals(destination, copyRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "copyAs"), copyRequest.getId().subList(0, 2));
        assertEquals(this.userReference, copyRequest.getUserReference());
        assertFalse(copyRequest.isDeep());
        assertTrue(copyRequest.isUpdateLinks());
        assertFalse(copyRequest.isInteractive());
        assertTrue(copyRequest.isCheckRights());
    }

    @Test
    void createCopyAsRequestString()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        String newPlace = "Bob";

        CopyRequest copyRequest = requestFactory.createCopyAsRequest(source, newPlace);
        EntityReference destination = new EntityReference(newPlace, source.getType(), source.getParent());
        assertEquals(Arrays.asList(source), copyRequest.getEntityReferences());
        assertEquals(destination, copyRequest.getDestination());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "copyAs"), copyRequest.getId().subList(0, 2));
        assertEquals(this.userReference, copyRequest.getUserReference());
        assertFalse(copyRequest.isDeep());
        assertTrue(copyRequest.isUpdateLinks());
        assertFalse(copyRequest.isInteractive());
        assertTrue(copyRequest.isCheckRights());
    }

    @Test
    void createCreateRequest()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        CreateRequest createRequest = requestFactory.createCreateRequest(Arrays.asList(source));
        assertEquals(Arrays.asList(source), createRequest.getEntityReferences());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "create"), createRequest.getId().subList(0, 2));
        assertEquals(this.userReference, createRequest.getUserReference());
        assertTrue(createRequest.isDeep());
        assertFalse(createRequest.isInteractive());
        assertTrue(createRequest.isCheckRights());
    }

    @Test
    void createDeleteRequest()
    {
        DocumentReference source = new DocumentReference("code", "Model", "Entity");
        EntityRequest deleteRequest = requestFactory.createDeleteRequest(Arrays.asList(source));
        assertEquals(Arrays.asList(source), deleteRequest.getEntityReferences());
        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "delete"), deleteRequest.getId().subList(0, 2));
        assertEquals(this.userReference, deleteRequest.getUserReference());
        assertFalse(deleteRequest.isDeep());
        assertFalse(deleteRequest.isInteractive());
        assertTrue(deleteRequest.isCheckRights());
    }

    @Test
    void createReplaceUserRequestWithLocalUser()
    {
        DocumentReference alice = new DocumentReference("dev", "Users", "Alice");
        DocumentReference bob = new DocumentReference("test", "Users", "Bob");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("test");

        ReplaceUserRequest request = this.requestFactory.createReplaceUserRequest(alice, bob);

        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "replaceUser"), request.getId().subList(0, 2));
        assertEquals(alice, request.getOldUserReference());
        assertEquals(bob, request.getNewUserReference());

        assertTrue(request.isCheckRights());
        assertEquals(this.userReference, request.getUserReference());
        assertEquals(this.userReference, request.getAuthorReference());
        assertFalse(request.isInteractive());

        assertEquals(Collections.singleton(alice.getWikiReference()), request.getEntityReferences());
    }

    @Test
    void createReplaceUserRequestWithGlobalUser() throws Exception
    {
        DocumentReference bob = new DocumentReference("test", "Users", "Bob");
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("test");
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("test", "dev"));

        ReplaceUserRequest request = this.requestFactory.createReplaceUserRequest(null, bob);

        assertEquals(Arrays.asList(RefactoringJobs.GROUP, "replaceUser"), request.getId().subList(0, 2));
        assertNull(request.getOldUserReference());
        assertEquals(bob, request.getNewUserReference());

        assertTrue(request.isCheckRights());
        assertEquals(this.userReference, request.getUserReference());
        assertEquals(this.userReference, request.getAuthorReference());
        assertFalse(request.isInteractive());

        assertEquals(new HashSet<>(Arrays.asList(bob.getWikiReference(), new WikiReference("dev"))),
            request.getEntityReferences());
    }
}
