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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.script.JobScriptService;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Provides refactoring-specific scripting APIs.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(RefactoringScriptService.ROLE_HINT)
@Singleton
@Unstable
public class RefactoringScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLE_HINT = "refactoring";

    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String REFACTORING_ERROR_KEY = String.format("scriptservice.%s.error", ROLE_HINT);

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

    /**
     * Needed for getting the current user reference.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    @Named("job")
    private ScriptService jobScriptService;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    /**
     * Creates a request to move the specified source entities to the specified destination entity (which becomes their
     * new parent).
     * 
     * @param sources specifies the entities to be moved
     * @param destination specifies the place where to move the entities (their new parent entity)
     * @return the move request
     */
    public MoveRequest createMoveRequest(Collection<EntityReference> sources, EntityReference destination)
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
     */
    public MoveRequest createMoveRequest(EntityReference source, EntityReference destination)
    {
        return createMoveRequest(Arrays.asList(source), destination);
    }

    /**
     * Creates a request to rename the entity specified by the given old reference.
     * 
     * @param oldReference the entity to rename
     * @param newReference the new entity reference after the rename
     * @return the rename request
     */
    public MoveRequest createRenameRequest(EntityReference oldReference, EntityReference newReference)
    {
        return createMoveRequest(RefactoringJobs.RENAME, Collections.singletonList(oldReference), newReference);
    }

    /**
     * Creates a request to rename the specified entity.
     * 
     * @param reference the entity to rename
     * @param newName the new entity name
     * @return the rename request
     */
    public MoveRequest createRenameRequest(EntityReference reference, String newName)
    {
        return createMoveRequest(RefactoringJobs.RENAME, Collections.singletonList(reference), new EntityReference(
            newName, reference.getType(), reference.getParent()));
    }

    private MoveRequest createMoveRequest(String type, Collection<EntityReference> sources, EntityReference destination)
    {
        MoveRequest request = new MoveRequest();
        request.setId(generateJobId(type));
        request.setInteractive(true);
        request.setJobType(type);
        request.setEntityReferences(sources);
        request.setDestination(destination);
        request.setUpdateLinks(true);
        request.setDeep(true);
        setRightsProperties(request);
        return request;
    }

    /**
     * @param type the type of refactoring
     * @return an id for a job to perform the specified type of refactoring
     */
    private List<String> generateJobId(String type)
    {
        String suffix = new Date().getTime() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        return getJobId(type, suffix);
    }

    /**
     * @param type the type of refactoring
     * @param suffix uniquely identifies the job among those of the specified type
     * @return an id for a job to perform the specified type of refactoring
     */
    private List<String> getJobId(String type, String suffix)
    {
        return Arrays.asList(ROLE_HINT, type, suffix);
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
        return move(createMoveRequest(sources, destination));
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
        return move(createMoveRequest(source, destination));
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
        return rename(createRenameRequest(oldReference, newReference));
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
        return rename(createRenameRequest(reference, newName));
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
                // We cannot convert a nested document to a terminal document and preserve its child documents at the
                // same time. If the target document has child documents they will become orphans.
                MoveRequest request = createRenameRequest(documentReference, terminalDocumentReference);
                request.setDeep(false);
                return rename(request);
            }
        }
        // The specified document is already a terminal document or cannot be converted to a terminal document.
        return null;
    }

    /**
     * Executes a refactoring request.
     * 
     * @param type the type of refactoring to execute
     * @param request the refactoring request to execute
     * @return the job that has been scheduled and that can be used to monitor the progress of the operation,
     *         {@code null} in case of failure
     */
    private Job execute(String type, EntityRequest request)
    {
        setError(null);

        // Make sure that only the PR users can change the rights and context properties from the request.
        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            setRightsProperties(request);
        }

        try {
            return this.jobExecutor.execute(type, request);
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    private <T extends EntityRequest> void setRightsProperties(T request)
    {
        request.setCheckRights(true);
        request.setUserReference(this.documentAccessBridge.getCurrentUserReference());
    }

    /**
     * Retrieve the status of a move job.
     * 
     * @param id the id of a move job
     * @return the status of the specified job
     */
    public EntityJobStatus<MoveRequest> getMoveJobStatus(String id)
    {
        return getJobStatus(getJobId(RefactoringJobs.MOVE, id));
    }

    /**
     * Retrieve the status of a rename job.
     * 
     * @param id the id of a rename job
     * @return the status of the specified job
     */
    public EntityJobStatus<MoveRequest> getRenameJobStatus(String id)
    {
        return getJobStatus(getJobId(RefactoringJobs.RENAME, id));
    }

    @SuppressWarnings("unchecked")
    private <T extends EntityRequest> EntityJobStatus<T> getJobStatus(List<String> jobId)
    {
        return (EntityJobStatus<T>) ((JobScriptService) jobScriptService).getJobStatus(jobId);
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
}
