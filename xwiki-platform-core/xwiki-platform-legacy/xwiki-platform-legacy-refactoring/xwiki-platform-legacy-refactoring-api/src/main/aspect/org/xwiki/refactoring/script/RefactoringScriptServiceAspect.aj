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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.job.Job;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;

import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;

/**
 * Deprecated methods from RefactoringScriptService.
 * @since 10.11RC1
 * @version $Id$
 */
public privileged aspect RefactoringScriptServiceAspect
{
    @Inject
    private DocumentAccessBridge RefactoringScriptService.documentAccessBridge;

    @Inject
    private ModelContext RefactoringScriptService.modelContext;

    /**
     * @param type the type of refactoring
     * @return an id for a job to perform the specified type of refactoring
     */
    private List<String> RefactoringScriptService.generateJobId(String type)
    {
        String suffix = new Date().getTime() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        return getJobId(type, suffix);
    }

    /**
     * @param type the type of refactoring
     * @param suffix uniquely identifies the job among those of the specified type
     * @return an id for a job to perform the specified type of refactoring
     */
    private List<String> RefactoringScriptService.getJobId(String type, String suffix)
    {
        return Arrays
            .asList(RefactoringJobs.GROUP, StringUtils.removeStart(type, RefactoringJobs.GROUP_PREFIX), suffix);
    }

    private void RefactoringScriptService.setRightsProperties(AbstractCheckRightsRequest request)
    {
        request.setCheckRights(true);
        request.setUserReference(this.documentAccessBridge.getCurrentUserReference());
        request.setAuthorReference(this.documentAccessBridge.getCurrentAuthorReference());
    }

    /**
     * Creates a request to move the specified source entities to the specified destination entity (which becomes their
     * new parent).
     *
     * @param sources specifies the entities to be moved
     * @param destination specifies the place where to move the entities (their new parent entity)
     * @return the move request
     * @deprecated Use {@link RequestFactory#createMoveRequest(Collection, EntityReference)} instead
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createMoveRequest(Collection<EntityReference> sources,
        EntityReference destination)
    {
        return createMoveRequest(RefactoringJobs.MOVE, sources, destination);
    }

    /**
     * Creates a request to move the specified source entity to the specified destination entity (which becomes its new
     * parent).
     *
     * @param source specifies the entity to be moved
     * @param destination specifies the place where to move the source entity (its new parent entity)
     * @return the move request
     * @deprecated Use {@link RequestFactory#createMoveRequest(EntityReference, EntityReference)} instead
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createMoveRequest(EntityReference source, EntityReference destination)
    {
        return createMoveRequest(Arrays.asList(source), destination);
    }

    /**
     * Creates a request to rename the entity specified by the given old reference.
     *
     * @param oldReference the entity to rename
     * @param newReference the new entity reference after the rename
     * @return the rename request
     * @deprecated Use {@link RequestFactory#createRenameRequest(EntityReference, EntityReference)} instead
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createRenameRequest(EntityReference oldReference,
        EntityReference newReference)
    {
        return createMoveRequest(RefactoringJobs.RENAME, Collections.singletonList(oldReference), newReference);
    }

    /**
     * Creates a request to rename the specified entity.
     *
     * @param reference the entity to rename
     * @param newName the new entity name
     * @return the rename request
     * @deprecated Use {@link RequestFactory#createRenameRequest(EntityReference, String)} instead
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createRenameRequest(EntityReference reference, String newName)
    {
        return createRenameRequest(reference, new EntityReference(newName, reference.getType(), reference.getParent()));
    }
    /**
     * Creates a request to copy the specified source entities to the specified destination entity.
     *
     * @param sources specifies the entities to be copied
     * @param destination specifies the place where to copy the entities (becomes the parent of the copies)
     * @return the copy request
     * @deprecated Use {@link RequestFactory#createCopyRequest(Collection, EntityReference)} instead
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createCopyRequest(Collection<EntityReference> sources,
        EntityReference destination)
    {
        MoveRequest request = createMoveRequest(RefactoringJobs.COPY, sources, destination);
        request.setDeleteSource(false);
        request.setAutoRedirect(false);
        request.setUpdateParentField(false);
        return request;
    }

    /**
     * Creates a request to copy the specified source entity to the specified destination entity.
     *
     * @param source specifies the entity to be copied
     * @param destination specifies the place where to copy the source entity (becomes the parent of the copy)
     * @return the copy request
     * @deprecated Use {@link RequestFactory#createCopyRequest(EntityReference, EntityReference)} instead
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createCopyRequest(EntityReference source, EntityReference destination)
    {
        return createCopyRequest(Arrays.asList(source), destination);
    }

    /**
     * Creates a request to copy the specified entity with a different reference.
     *
     * @param sourceReference the entity to copy
     * @param copyReference the reference to use for the copy
     * @return the copy-as request
     * @deprecated Use {@link RequestFactory#createCopyAsRequest(EntityReference, EntityReference)} instead.
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createCopyAsRequest(EntityReference sourceReference,
        EntityReference copyReference)
    {
        MoveRequest request = createMoveRequest(RefactoringJobs.COPY_AS, Arrays.asList(sourceReference), copyReference);
        request.setDeleteSource(false);
        request.setAutoRedirect(false);
        request.setUpdateParentField(false);
        return request;
    }

    /**
     * Creates a request to copy the specified entity with a different name.
     *
     * @param reference the entity to copy
     * @param copyName the name of the entity copy
     * @return the copy-as request
     * @deprecated Use {@link RequestFactory#createCopyAsRequest(EntityReference, String)} instead.
     */
    @Deprecated
    public MoveRequest RefactoringScriptService.createCopyAsRequest(EntityReference reference, String copyName)
    {
        EntityReference copyReference = new EntityReference(copyName, reference.getType(), reference.getParent());
        return createCopyAsRequest(reference, copyReference);
    }

    /**
     * Creates a request to delete the specified entities.
     *
     * @param entityReferences the entities to delete
     * @return the delete request
     * @deprecated Use {@link RequestFactory#createDeleteRequest(Collection)} instead.
     */
    @Deprecated
    public EntityRequest RefactoringScriptService.createDeleteRequest(Collection<EntityReference> entityReferences)
    {
        EntityRequest request = new EntityRequest();
        initEntityRequest(request, RefactoringJobs.DELETE, entityReferences);
        return request;
    }

    /**
     * Creates a request to create the specified entities.
     *
     * @param entityReferences the entities to create
     * @return the create request
     * @since 7.4M2
     * @deprecated Use {@link RequestFactory#createCreateRequest(Collection)} instead.
     */
    @Deprecated
    public CreateRequest RefactoringScriptService.createCreateRequest(Collection<EntityReference> entityReferences)
    {
        CreateRequest request = new CreateRequest();
        initEntityRequest(request, RefactoringJobs.CREATE, entityReferences);
        // Set deep create by default, to copy (if possible) any existing hierarchy of a specified template document.
        // TODO: expose this in the create UI to advanced users to allow them to opt-out?
        request.setDeep(true);
        return request;
    }

    private void RefactoringScriptService.initEntityRequest(EntityRequest request, String type,
        Collection<EntityReference> entityReferences)
    {
        request.setId(generateJobId(type));
        request.setJobType(type);
        request.setEntityReferences(entityReferences);
        setRightsProperties(request);
    }

    private MoveRequest RefactoringScriptService.createMoveRequest(String type, Collection<EntityReference> sources,
        EntityReference destination)
    {
        MoveRequest request = new MoveRequest();
        initEntityRequest(request, type, sources);
        request.setDestination(destination);
        request.setUpdateLinks(true);
        request.setAutoRedirect(true);
        request.setUpdateParentField(true);
        return request;
    }

    /**
     * Creates a request to permanently delete a specified batch of deleted documents from the recycle bin.
     *
     * @param batchId the ID of the batch of deleted documents to permanently delete
     * @return the permanently delete request
     * @since 10.10RC1
     * @deprecated Use {@link RequestFactory#createPermanentlyDeleteRequest(String)} instead.
     */
    @Deprecated
    public PermanentlyDeleteRequest RefactoringScriptService.createPermanentlyDeleteRequest(String batchId)
    {
        PermanentlyDeleteRequest request = initializePermanentlyDeleteRequest();
        request.setBatchId(batchId);
        return request;
    }

    /**
     * Creates a request to permanently delete a specified list of deleted documents from the recycle bin.
     *
     * @param deletedDocumentIds the list of IDs of the deleted documents to permanently delete
     * @return the permanently delete request
     * @since 10.10RC1
     * @deprecated Use {@link RequestFactory#createPermanentlyDeleteRequest(List)} instead.
     */
    @Deprecated
    public PermanentlyDeleteRequest RefactoringScriptService.createPermanentlyDeleteRequest(
        List<Long> deletedDocumentIds)
    {
        PermanentlyDeleteRequest request = initializePermanentlyDeleteRequest();
        request.setDeletedDocumentIds(deletedDocumentIds);
        return request;
    }

    private PermanentlyDeleteRequest RefactoringScriptService.initializePermanentlyDeleteRequest()
    {
        PermanentlyDeleteRequest request = new PermanentlyDeleteRequest();

        request.setId(generateJobId(RefactoringJobs.PERMANENTLY_DELETE));
        request.setCheckRights(true);
        request.setUserReference(this.documentAccessBridge.getCurrentUserReference());
        request.setWikiReference(getCurrentWikiReference());

        return request;
    }


    /**
     * Creates a request to restore a specified batch of deleted documents from the recycle bin.
     *
     * @param batchId the ID of the batch of deleted documents to restore
     * @return the restore request
     * @since 9.4RC1
     * @deprecated Use {@link RequestFactory#createRestoreRequest(String)} instead.
     */
    @Deprecated
    public RestoreRequest RefactoringScriptService.createRestoreRequest(String batchId)
    {
        RestoreRequest request = initializeRestoreRequest();
        request.setBatchId(batchId);
        return request;
    }

    /**
     * Creates a request to restore a specified list of deleted documents from the recycle bin.
     *
     * @param deletedDocumentIds the list of IDs of the deleted documents to restore
     * @return the restore request
     * @since 9.4RC1
     * @deprecated Use {@link RequestFactory#createRestoreRequest(List)} instead.
     */
    @Deprecated
    public RestoreRequest RefactoringScriptService.createRestoreRequest(List<Long> deletedDocumentIds)
    {
        RestoreRequest request = initializeRestoreRequest();
        request.setDeletedDocumentIds(deletedDocumentIds);
        return request;
    }

    private RestoreRequest RefactoringScriptService.initializeRestoreRequest()
    {
        RestoreRequest request = new RestoreRequest();

        request.setId(generateJobId(RefactoringJobs.RESTORE));
        request.setCheckRights(true);
        request.setUserReference(this.documentAccessBridge.getCurrentUserReference());
        request.setWikiReference(getCurrentWikiReference());

        return request;
    }

    private WikiReference RefactoringScriptService.getCurrentWikiReference()
    {
        WikiReference result = null;

        EntityReference currentEntityReference = this.modelContext.getCurrentEntityReference();
        if (currentEntityReference != null) {
            EntityReference wikiEntityReference =
                this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI);
            if (wikiEntityReference != null) {
                result = new WikiReference(wikiEntityReference);
            }
        }

        return result;
    }

    /**
     * Schedules an asynchronous job to perform the given copy-as request.
     *
     * @param request the copy-as request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     * @deprecated Use {@link #copyAs(CopyRequest)} instead.
     */
    @Deprecated
    public Job RefactoringScriptService.copyAs(MoveRequest request)
    {
        return execute(RefactoringJobs.COPY, request);
    }

    /**
     * Schedules an asynchronous job to perform the given copy request.
     *
     * @param request the copy request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     * @deprecated Use {@link #copy(CopyRequest)} instead.
     */
    @Deprecated
    public Job RefactoringScriptService.copy(MoveRequest request)
    {
        return execute(RefactoringJobs.COPY, request);
    }
}