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
package org.xwiki.refactoring.internal.job;

import java.util.LinkedList;
import java.util.List;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.EndFoldEvent;
import org.xwiki.refactoring.internal.event.AbstractEntityCopyOrRenameEvent;
import org.xwiki.refactoring.job.AbstractCopyOrMoveRequest;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.OverwriteQuestion;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.security.authorization.Right;

/**
 * Defines the common methods for both copy and move jobs since they are both related to moving entities.
 * 
 * @param <T> the type of the request associated to the job
 * @since 10.11RC1
 * @version $Id$
 */
public abstract class AbstractCopyOrMoveJob<T extends AbstractCopyOrMoveRequest>
    extends AbstractEntityJobWithChecks<T, EntityJobStatus<T>>
{
    /**
     * Specifies whether all entities with the same name are to be overwritten on not. When {@code true} all entities
     * with the same name are overwritten. When {@code false} all entities with the same name are skipped. If
     * {@code null} then a question is asked for each entity.
     */
    private Boolean overwriteAll;

    @Override
    protected void runInternal() throws Exception
    {
        this.progressManager.pushLevelProgress(3, this);

        try {
            this.progressManager.startStep(this);
            BeginFoldEvent beginEvent = createBeginEvent();
            this.observationManager.notify(beginEvent, this, this.getRequest());
            if (beginEvent instanceof CancelableEvent && ((CancelableEvent) beginEvent).isCanceled()) {
                return;
            }
            this.progressManager.endStep(this);

            this.progressManager.startStep(this);
            if (this.request.getDestination() != null) {
                super.runInternal();
            }
            this.progressManager.endStep(this);

            this.progressManager.startStep(this);
            EndFoldEvent endEvent = createEndEvent();
            this.observationManager.notify(endEvent, this, this.getRequest());
            this.progressManager.endStep(this);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    /**
     * If true, we check that the source and destination entityReference are of the same type. And we process them using
     * that information.
     * 
     * @return false by default.
     */
    protected boolean processOnlySameSourceDestinationTypes()
    {
        return false;
    }

    @Override
    protected void process(EntityReference source)
    {
        // Perform generic checks that don't depend on the source/destination type.

        EntityReference destination = this.request.getDestination();

        if (processOnlySameSourceDestinationTypes()) {
            if (source.getType() != destination.getType()) {
                this.logger.error("You cannot change the entity type (from [{}] to [{}]).", source.getType(),
                    destination.getType());
                return;
            }
        }

        if (isDescendantOrSelf(destination, source)) {
            this.logger.error("Cannot make [{}] a descendant of itself.", source);
            return;
        }

        if (source.getParent() != null && source.getParent().equals(destination)) {
            this.logger.error("Cannot move [{}] into [{}], it's already there.", source, destination);
            return;
        }

        // Dispatch the move operation based on the source entity type.

        switch (source.getType()) {
            case DOCUMENT:
                try {
                    process(new DocumentReference(source), destination);
                } catch (Exception e) {
                    this.logger.error("Failed to copy or move document with reference [{}]", source, e);
                }
                break;
            case SPACE:
                process(new SpaceReference(source), destination);
                break;
            default:
                this.logger.error("Unsupported source entity type [{}].", source.getType());
        }
    }

    private boolean isDescendantOrSelf(EntityReference alice, EntityReference bob)
    {
        EntityReference parent = alice;
        while (parent != null && !parent.equals(bob)) {
            parent = parent.getParent();
        }
        return parent != null;
    }

    protected void process(DocumentReference source, EntityReference destination) throws Exception
    {
        if (processOnlySameSourceDestinationTypes()) {
            // We know the destination is a document (see above).
            DocumentReference destinationDocumentReference = new DocumentReference(destination);
            this.process(source, destinationDocumentReference);
        } else {
            if (this.request.isDeep() && isSpaceHomeReference(source)) {
                process(source.getLastSpaceReference(), destination);
            } else if (destination.getType() == EntityType.SPACE) {
                maybePerformRefactoring(source,
                    new DocumentReference(source.getName(), new SpaceReference(destination)));
            } else if (destination.getType() == EntityType.DOCUMENT
                && isSpaceHomeReference(new DocumentReference(destination))) {
                maybePerformRefactoring(source,
                    new DocumentReference(source.getName(), new SpaceReference(destination.getParent())));
            } else {
                this.logger.error("Unsupported destination entity type [{}] for a document.", destination.getType());
            }
        }
    }

    protected void process(DocumentReference source, DocumentReference destination) throws Exception
    {
        if (this.request.isDeep() && isSpaceHomeReference(source)) {
            if (isSpaceHomeReference(destination)) {
                // Rename an entire space.
                process(source.getLastSpaceReference(), destination.getLastSpaceReference());
            } else {
                this.logger.error("You cannot transform a non-terminal document [{}] into a terminal document [{}]"
                    + " and preserve its child documents at the same time.", source, destination);
            }
        } else {
            maybePerformRefactoring(source, destination);
        }
    }

    protected void process(SpaceReference source, EntityReference destination)
    {
        if (processOnlySameSourceDestinationTypes()) {
            // We know the destination is a space (see above).
            process(source, new SpaceReference(destination));
        } else {
            if (destination.getType() == EntityType.SPACE || destination.getType() == EntityType.WIKI) {
                process(source, new SpaceReference(source.getName(), destination));
            } else if (destination.getType() == EntityType.DOCUMENT
                && isSpaceHomeReference(new DocumentReference(destination))) {
                process(source, new SpaceReference(source.getName(), destination.getParent()));
            } else {
                this.logger.error("Unsupported destination entity type [{}] for a space.", destination.getType());
            }
        }
    }

    protected void process(final SpaceReference source, final SpaceReference destination)
    {
        visitDocuments(source, new Visitor<DocumentReference>()
        {
            @Override
            public void visit(DocumentReference oldChildReference)
            {
                DocumentReference newChildReference = oldChildReference.replaceParent(source, destination);
                try {
                    maybePerformRefactoring(oldChildReference, newChildReference);
                } catch (Exception e) {
                    logger.error("Failed to perform the refactoring from document with reference [{}] to [{}]",
                        oldChildReference, newChildReference, e);
                }
            }
        });
    }

    protected boolean checkAllRights(DocumentReference oldReference, DocumentReference newReference) throws Exception
    {
        if (!hasAccess(Right.VIEW, oldReference)) {
            this.logger.error("You don't have sufficient permissions over the source document [{}].", oldReference);
            return false;
        } else if (!hasAccess(Right.EDIT, newReference)
            || (this.modelBridge.exists(newReference) && !hasAccess(Right.DELETE, newReference))) {
            this.logger.error("You don't have sufficient permissions over the destination document [{}].",
                newReference);
            return false;
        }
        return true;
    }

    protected void maybePerformRefactoring(DocumentReference oldReference, DocumentReference newReference)
        throws Exception
    {
        // Perform checks that are specific to the document source/destination type.

        EntitySelection entitySelection = this.getConcernedEntitiesEntitySelection(oldReference);
        if (entitySelection == null) {
            this.logger.info("Skipping [{}] because it does not match any entity selection.", oldReference);
        } else if (!entitySelection.isSelected()) {
            this.logger.info("Skipping [{}] because it has been unselected.", oldReference);
        } else if (!this.modelBridge.exists(oldReference)) {
            this.logger.warn("Skipping [{}] because it doesn't exist.", oldReference);
        } else if (this.checkAllRights(oldReference, newReference)) {
            performRefactoring(oldReference, newReference);
        }
    }

    protected abstract void performRefactoring(DocumentReference oldReference, DocumentReference newReference);

    protected boolean copyOrMove(DocumentReference oldReference, DocumentReference newReference,
        AbstractEntityCopyOrRenameEvent<?> beforeEvent, AbstractEntityCopyOrRenameEvent<?> afterEvent) throws Exception
    {
        this.progressManager.pushLevelProgress(5, this);

        try {
            // Step 1: Send before event.
            this.progressManager.startStep(this);
            this.observationManager.notify(beforeEvent, this, this.getRequest());
            if (beforeEvent.isCanceled()) {
                return false;
            }
            this.progressManager.endStep(this);

            // Step 2: Delete the destination document if needed.
            this.progressManager.startStep(this);
            if (this.modelBridge.exists(newReference)) {
                if (this.request.isInteractive() && !this.modelBridge.canOverwriteSilently(newReference)
                    && !confirmOverwrite(oldReference, newReference)) {
                    this.logger.warn(
                        "Skipping [{}] because [{}] already exists and the user doesn't want to overwrite it.",
                        oldReference, newReference);
                    return false;
                }
            }
            this.progressManager.endStep(this);

            // Step 3: Copy the source document to the destination.
            this.progressManager.startStep(this);
            if (!this.atomicOperation(oldReference, newReference)) {
                return false;
            }
            this.progressManager.endStep(this);

            // Step 4: Update the destination document based on the source document parameters.
            this.progressManager.startStep(this);
            this.modelBridge.update(newReference, this.request.getEntityParameters(oldReference));
            this.progressManager.endStep(this);

            // Step 5: Send after event.
            this.progressManager.startStep(this);
            this.observationManager.notify(afterEvent, this, this.getRequest());
            this.progressManager.endStep(this);

            return true;
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private boolean confirmOverwrite(EntityReference source, EntityReference destination)
    {
        if (this.overwriteAll == null) {
            OverwriteQuestion question = new OverwriteQuestion(source, destination);
            try {
                this.status.ask(question);
                if (this.status.isCanceled()) {
                    return false;
                } else if (!question.isAskAgain()) {
                    // Use the same answer for the following overwrite questions.
                    this.overwriteAll = question.isOverwrite();
                }
                return question.isOverwrite();
            } catch (InterruptedException e) {
                this.logger.warn("Overwrite question has been interrupted.");
                return false;
            }
        } else {
            return this.overwriteAll;
        }
    }

    @Override
    protected EntityReference getCommonParent()
    {
        List<EntityReference> entityReferences = new LinkedList<>(this.request.getEntityReferences());
        entityReferences.add(this.request.getDestination());
        return getCommonParent(entityReferences);
    }

    /**
     * Atomic operation to perform: should be a rename for Rename/Move and copy for Copy.
     * @param source the source reference
     * @param target the target reference
     * @return {@code true} if the operation worked well.
     */
    protected abstract boolean atomicOperation(DocumentReference source, DocumentReference target);
    protected abstract BeginFoldEvent createBeginEvent();
    protected abstract EndFoldEvent createEndEvent();
}
