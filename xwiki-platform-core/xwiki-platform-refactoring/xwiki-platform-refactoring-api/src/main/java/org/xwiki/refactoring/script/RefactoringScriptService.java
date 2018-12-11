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
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Provides refactoring-specific scripting APIs.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(RefactoringJobs.GROUP)
@Singleton
public class RefactoringScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String REFACTORING_ERROR_KEY = String.format("scriptservice.%s.error", RefactoringJobs.GROUP);

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to execute refactoring jobs.
     */
    @Inject
    private JobExecutor jobExecutor;

    /**
     * Used to check user rights.
     */
    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private RequestFactory requestFactory;

    /**
     * @return the standard request factory for creating the different refactoring requests.
     * @since 10.11RC1
     */
    public RequestFactory getRequestFactory()
    {
        return this.requestFactory;
    }

    /**
     * Schedules an asynchronous job to perform the given move request.
     *
     * @param request the move request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job move(MoveRequest request)
    {
        return execute(RefactoringJobs.MOVE, request);
    }

    /**
     * Schedules an asynchronous job to move the specified source entities to the specified destination entity (which
     * becomes their new parent).
     *
     * @param sources specifies the entities to be moved
     * @param destination specifies the place where to move the entities (their new parent entity)
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job move(Collection<EntityReference> sources, EntityReference destination)
    {
        return move(getRequestFactory().createMoveRequest(sources, destination));
    }

    /**
     * Schedules an asynchronous job to move the specified source entity to the specified destination entity (which
     * becomes its new parent).
     *
     * @param source specifies the entity to be moved
     * @param destination specifies the place where to move the entity (its new parent entity)
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job move(EntityReference source, EntityReference destination)
    {
        return move(getRequestFactory().createMoveRequest(source, destination));
    }

    /**
     * Schedules an asynchronous job to perform the given rename request.
     *
     * @param request the rename request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job rename(MoveRequest request)
    {
        return execute(RefactoringJobs.RENAME, request);
    }

    /**
     * Schedules an asynchronous job to rename the specified entity.
     *
     * @param oldReference the entity to rename
     * @param newReference the new entity reference after the rename
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job rename(EntityReference oldReference, EntityReference newReference)
    {
        return rename(getRequestFactory().createRenameRequest(oldReference, newReference));
    }

    /**
     * Schedules an asynchronous job to rename the specified entity.
     *
     * @param reference the entity to rename
     * @param newName the new entity name
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job rename(EntityReference reference, String newName)
    {
        return rename(getRequestFactory().createRenameRequest(reference, newName));
    }

    /**
     * Schedules an asynchronous job to convert the specified terminal document to a nested document (that can have
     * child documents). E.g. the document {@code Space1.Space2.Name} is converted to {@code Space1.Space2.Name.WebHome}
     * .
     *
     * @param documentReference the terminal document to convert to a nested document (that can have child documents)
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job convertToNestedDocument(DocumentReference documentReference)
    {
        String defaultDocName = this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
        if (!documentReference.getName().equals(defaultDocName)) {
            SpaceReference spaceReference =
                new SpaceReference(documentReference.getName(), documentReference.getParent());
            return rename(documentReference, new DocumentReference(defaultDocName, spaceReference));
        }
        // The specified document is already a nested document.
        return null;
    }

    /**
     * Schedules an asynchronous job to convert the specified nested document to a terminal document (that can't have
     * child documents). E.g. the document {@code One.Two.WebHome} is converted to {@code One.Two} .
     *
     * @param documentReference the nested document to convert to a terminal document (that can't have child documents)
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job convertToTerminalDocument(DocumentReference documentReference)
    {
        if (documentReference.getName().equals(
            this.defaultEntityReferenceProvider.getDefaultReference(documentReference.getType()).getName())) {
            EntityReference parentReference = documentReference.getParent();
            if (parentReference.getParent().getType() == EntityType.SPACE) {
                // There has to be at least 2 levels of nested spaces in order to be able to convert a nested document
                // into a terminal document. We cannot convert a root document like Main.WebHome into a terminal
                // document.
                DocumentReference terminalDocumentReference =
                    new DocumentReference(parentReference.getName(), new SpaceReference(parentReference.getParent()));
                return rename(documentReference, terminalDocumentReference);
            }
        }
        // The specified document is already a terminal document or cannot be converted to a terminal document.
        return null;
    }

    /**
     * Schedules an asynchronous job to perform the given copy request.
     *
     * @param request the copy request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     * @since 10.11RC1
     */
    public Job copy(CopyRequest request)
    {
        return execute(RefactoringJobs.COPY, request);
    }

    /**
     * Schedules an asynchronous job to copy the specified source entities to the specified destination entity.
     *
     * @param sources specifies the entities to be copied
     * @param destination specifies the place where to copy the entities
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job copy(Collection<EntityReference> sources, EntityReference destination)
    {
        return copy(getRequestFactory().createCopyRequest(sources, destination));
    }

    /**
     * Schedules an asynchronous job to copy the specified source entity to the specified destination entity.
     *
     * @param source specifies the entity to be copied
     * @param destination specifies the place where to copy the entity
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job copy(EntityReference source, EntityReference destination)
    {
        return copy(getRequestFactory().createCopyRequest(source, destination));
    }

    /**
     * Schedules an asynchronous job to perform the given copy-as request.
     *
     * @param request the copy-as request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job copyAs(CopyRequest request)
    {
        return execute(RefactoringJobs.COPY_AS, request);
    }

    /**
     * Schedules an asynchronous job to copy the specified entity with a different reference.
     *
     * @param sourceReference the entity to copy
     * @param copyReference the reference to use for the copy
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job copyAs(EntityReference sourceReference, EntityReference copyReference)
    {
        return copyAs(getRequestFactory().createCopyAsRequest(sourceReference, copyReference));
    }

    /**
     * Schedules an asynchronous job to copy the specified entity with a different name.
     *
     * @param reference the entity to copy
     * @param copyName the name to use for the copy
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job copyAs(EntityReference reference, String copyName)
    {
        return copyAs(getRequestFactory().createCopyAsRequest(reference, copyName));
    }

    /**
     * Schedules an asynchronous job to perform the given delete request.
     *
     * @param request the delete request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job delete(EntityRequest request)
    {
        return execute(RefactoringJobs.DELETE, request);
    }

    /**
     * Schedules an asynchronous job to delete the specified entities.
     *
     * @param entityReferences the entities to delete
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job delete(Collection<EntityReference> entityReferences)
    {
        return delete(getRequestFactory().createDeleteRequest(entityReferences));
    }

    /**
     * Schedules an asynchronous job to delete the specified entity.
     *
     * @param entityReference the entity to delete
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    public Job delete(EntityReference entityReference)
    {
        return delete(Arrays.asList(entityReference));
    }

    /**
     * Schedules an asynchronous job to perform the given create request.
     *
     * @param request the create request to perform
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     * @since 7.4M2
     */
    public Job create(CreateRequest request)
    {
        return execute(RefactoringJobs.CREATE, request);
    }

    /**
     * Schedules an asynchronous job to create the specified entities.
     *
     * @param entityReferences the entities to create
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     * @since 7.4M2
     */
    public Job create(Collection<EntityReference> entityReferences)
    {
        return create(getRequestFactory().createCreateRequest(entityReferences));
    }

    /**
     * Schedules an asynchronous job to create the specified entity.
     *
     * @param entityReference the entity to create
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     * @since 7.4M2
     */
    public Job create(EntityReference entityReference)
    {
        return create(Arrays.asList(entityReference));
    }

    /**
     * Executes a refactoring request.
     *
     * @param type the type of refactoring to execute
     * @param request the refactoring request to execute
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    private Job execute(String type, AbstractCheckRightsRequest request)
    {
        setError(null);

        // Make sure that only the PR users can change the rights and context properties from the request.
        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            getRequestFactory().setRightsProperties(request);
        }

        try {
            return this.jobExecutor.execute(type, request);
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(REFACTORING_ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(REFACTORING_ERROR_KEY, e);
    }

    /**
     * Schedules an asynchronous job for restoring the specified entities.
     *
     * @param restoreRequest the information of the restore to do
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *  {@code null} in case of failure
     *  @since 10.10RC1
     */
    public Job restore(RestoreRequest restoreRequest)
    {
        return this.execute(RefactoringJobs.RESTORE, restoreRequest);
    }

    /**
     * Schedules an asynchronous job for restoring the specified the batchId.
     *
     * @param batchId the batchId corresponding to the entities to restore
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *  {@code null} in case of failure
     *  @since 10.10RC1
     */
    public Job restore(String batchId)
    {
        return this.restore(getRequestFactory().createRestoreRequest(batchId));
    }

    /**
     * Schedules an asynchronous job for restoring the specified deleted documents.
     *
     * @param deletedDocumentIds the ids of the deleted documents to restore
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *  {@code null} in case of failure
     *  @since 10.10RC1
     */
    public Job restore(List<Long> deletedDocumentIds)
    {
        return this.restore(getRequestFactory().createRestoreRequest(deletedDocumentIds));
    }

    /**
     * Schedules an asynchronous job for permanently deleting entities.
     *
     * @param request the request to run.
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *  {@code null} in case of failure
     *  @since 10.10RC1
     */
    public Job permanentlyDelete(PermanentlyDeleteRequest request)
    {
        return this.execute(RefactoringJobs.PERMANENTLY_DELETE, request);
    }

    /**
     * Schedules an asynchronous job for permanently deleting entities.
     *
     * @param batchId the batchId with the entities to permanently delete
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *  {@code null} in case of failure
     *  @since 10.10RC1
     */
    public Job permanentlyDelete(String batchId)
    {
        return this.permanentlyDelete(getRequestFactory().createPermanentlyDeleteRequest(batchId));
    }

    /**
     * Schedules an asynchronous job for permanently deleting entities.
     *
     * @param deletedDocumentIds the list of document ids to delete
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *  {@code null} in case of failure
     *
     * @since 10.10RC1
     */
    public Job permanentlyDelete(List<Long> deletedDocumentIds)
    {
        return this.permanentlyDelete(getRequestFactory().createPermanentlyDeleteRequest(deletedDocumentIds));
    }
}
