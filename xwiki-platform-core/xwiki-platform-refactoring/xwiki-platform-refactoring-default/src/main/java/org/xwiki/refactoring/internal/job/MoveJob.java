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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.OverwriteQuestion;
import org.xwiki.security.authorization.Right;

/**
 * A job that can move entities to a new parent within the hierarchy.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(MoveJob.JOB_TYPE)
public class MoveJob extends AbstractOldCoreEntityJob<MoveRequest, EntityJobStatus<MoveRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "moveEntities";

    /**
     * Specifies whether all entities with the same name are to be overwritten on not. When {@code true} all entities
     * with the same name are overwritten. When {@code false} all entities with the same name are skipped. If
     * {@code null} then a question is asked for each entity.
     */
    private Boolean overwriteAll;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected EntityJobStatus<MoveRequest> createNewStatus(MoveRequest request)
    {
        return new EntityJobStatus<MoveRequest>(request, this.observationManager, this.loggerManager, null);
    }

    @Override
    protected void runInternal() throws Exception
    {
        if (this.request.getDestination() != null) {
            super.runInternal();
        }
    }

    @Override
    protected void process(EntityReference source)
    {
        // Perform generic checks that don't depend on the source/destination type.

        EntityReference destination = this.request.getDestination();
        if (isDescendantOrSelf(destination, source)) {
            this.logger.error("Cannot make [{}] a descendant of itself.", source);
            return;
        }

        // Dispatch the move operation based on the source entity type.

        switch (source.getType()) {
            case DOCUMENT:
                move(new DocumentReference(source), destination);
                break;
            default:
                this.logger.warn("Unsupported source entity type [{}].", source.getType());
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

    protected void move(DocumentReference source, EntityReference destination)
    {
        // Compute the reference of the destination document.

        EntityReference currentParent = isTerminal(source) ? source.getParent() : source.getParent().getParent();
        EntityReference newReference = source.removeParent(currentParent);

        EntityReference newParent = destination;
        if (destination.getType() == EntityType.DOCUMENT) {
            if (isTerminal(destination)) {
                this.logger.warn("The destination document [{}] cannot have child documents.", destination);
                return;
            } else {
                // The destination is a WebHome (nested) document so the new parent is its parent space.
                newParent = destination.getParent();
            }
        } else if (destination.getType() != EntityType.SPACE
            && (destination.getType() != EntityType.WIKI || isTerminal(source))) {
            this.logger.warn("Unsupported destination entity type [{}].", destination.getType());
            return;
        }

        newReference = newReference.appendParent(newParent);
        maybeMove(source, new DocumentReference(newReference));
    }

    protected void maybeMove(DocumentReference oldReference, DocumentReference newReference)
    {
        // Perform checks that are specific to the document source/destination type.

        if (!exists(oldReference)) {
            this.logger.warn("Skipping [{}] because it doesn't exist.", oldReference);
            return;
        }

        // The move operation is currently implemented as Copy + Delete.
        if (!hasAccess(Right.DELETE, oldReference)) {
            this.logger.warn("You are not allowed to delete [{}].", oldReference);
            return;
        }

        if (!hasAccess(Right.VIEW, newReference) || !hasAccess(Right.EDIT, newReference)
            || (exists(newReference) && !hasAccess(Right.DELETE, newReference))) {
            this.logger.warn("You don't have sufficient permissions over the destination document [{}].", newReference);
            return;
        }

        move(oldReference, newReference, this.request.isDeep());
    }

    private void move(DocumentReference oldReference, DocumentReference newReference, boolean deep)
    {
        this.progressManager.pushLevelProgress(5, this);

        try {
            // Step 1: Delete the destination document if needed.
            this.progressManager.startStep(this);
            if (exists(newReference)) {
                if (this.request.isInteractive() && !confirmOverwrite(oldReference, newReference)) {
                    this.logger.warn(
                        "Skipping [{}] because [{}] already exists and the user doesn't want to overwrite it.",
                        oldReference, newReference);
                    return;
                } else if (!delete(newReference)) {
                    return;
                }
            }
            this.progressManager.endStep(this);

            // Step 2: Copy the source document to the destination.
            this.progressManager.startStep(this);
            if (!copy(oldReference, newReference)) {
                return;
            }
            this.progressManager.endStep(this);

            // Step 3: Update the links.
            this.progressManager.startStep(this);
            if (this.request.isUpdateLinks()) {
                updateLinks(oldReference, newReference);
            }
            this.progressManager.endStep(this);

            // Step 4: Delete the source document.
            this.progressManager.startStep(this);
            delete(oldReference);
            this.progressManager.endStep(this);

            // Step 5: Move the child documents.
            this.progressManager.startStep(this);
            if (deep && !isTerminal(oldReference)) {
                moveChildren(oldReference, newReference);
            }
            this.progressManager.endStep(this);
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
                if (!question.isAskAgain()) {
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

    private void moveChildren(DocumentReference oldReference, DocumentReference newReference)
    {
        List<DocumentReference> oldChildReferences = getChildren(oldReference);

        this.progressManager.pushLevelProgress(oldChildReferences.size(), this);

        try {
            for (DocumentReference oldChildReference : oldChildReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);

                    DocumentReference newChildReference =
                        oldChildReference.replaceParent(oldReference.getParent(), newReference.getParent());
                    // We don't have to move recursively because #getChildDocuments() returns all the descendants
                    // actually (because we don't have a way to retrieve the direct child documents at the moment).
                    move(oldChildReference, newChildReference, false);

                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void updateLinks(DocumentReference oldReference, DocumentReference newReference)
    {
        // TODO: Update back links
        // TODO: Refactor links from doc content
    }

    @Override
    protected String getTargetWiki()
    {
        List<EntityReference> entityReferences = new LinkedList<>(this.request.getEntityReferences());
        entityReferences.add(this.request.getDestination());
        return getTargetWiki(entityReferences);
    }
}
