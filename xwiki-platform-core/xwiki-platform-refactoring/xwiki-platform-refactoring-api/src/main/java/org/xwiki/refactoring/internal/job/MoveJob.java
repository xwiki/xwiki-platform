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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.OverwriteQuestion;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * A job that can move entities to a new parent within the hierarchy.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(RefactoringJobs.MOVE)
public class MoveJob extends AbstractEntityJob<MoveRequest, EntityJobStatus<MoveRequest>>
{
    /**
     * Specifies whether all entities with the same name are to be overwritten on not. When {@code true} all entities
     * with the same name are overwritten. When {@code false} all entities with the same name are skipped. If
     * {@code null} then a question is asked for each entity.
     */
    private Boolean overwriteAll;

    /**
     * The component used to refactor document links after a document is rename or moved.
     */
    @Inject
    private LinkRefactoring linkRefactoring;
    
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public String getType()
    {
        return RefactoringJobs.MOVE;
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

        if (source.getParent() != null && source.getParent().equals(destination)) {
            this.logger.error("Cannot move [{}] into [{}], it's already there.", source, destination);
            return;
        }

        // Dispatch the move operation based on the source entity type.

        switch (source.getType()) {
            case DOCUMENT:
                process(new DocumentReference(source), destination);
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

    protected void process(DocumentReference source, EntityReference destination)
    {
        if (this.request.isDeep() && isSpaceHomeReference(source)) {
            process(source.getLastSpaceReference(), destination);
        } else if (destination.getType() == EntityType.SPACE) {
            maybeMove(source, new DocumentReference(source.getName(), new SpaceReference(destination)));
        } else if (destination.getType() == EntityType.DOCUMENT
            && isSpaceHomeReference(new DocumentReference(destination))) {
            maybeMove(source, new DocumentReference(source.getName(), new SpaceReference(destination.getParent())));
        } else {
            this.logger.error("Unsupported destination entity type [{}] for a document.", destination.getType());
        }
    }

    protected void process(SpaceReference source, EntityReference destination)
    {
        if (destination.getType() == EntityType.SPACE || destination.getType() == EntityType.WIKI) {
            process(source, new SpaceReference(source.getName(), destination));
        } else if (destination.getType() == EntityType.DOCUMENT
            && isSpaceHomeReference(new DocumentReference(destination))) {
            process(source, new SpaceReference(source.getName(), destination.getParent()));
        } else {
            this.logger.error("Unsupported destination entity type [{}] for a space.", destination.getType());
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
                if (MoveJob.this.hasAccess(Right.VIEW, newChildReference)) {
                    maybeMove(oldChildReference, newChildReference);
                }
            }
        });
    }

    private boolean checkAllRights(DocumentReference oldReference, DocumentReference newReference)
    {
        if (this.request.isDeleteSource() && !hasAccess(Right.DELETE, oldReference)) {
            // The move operation is implemented as Copy + Delete.
            this.logger.error("You are not allowed to delete [{}].", oldReference);
            return false;
        } else if (!hasAccess(Right.VIEW, oldReference)) {
            this.logger.error("You don't have sufficient permissions over the source document [{}].",
                oldReference);
            return false;
        } else if (!hasAccess(Right.VIEW, newReference) || !hasAccess(Right.EDIT, newReference)
            || (this.modelBridge.exists(newReference) && !hasAccess(Right.DELETE, newReference)))
        {
            this.logger.error("You don't have sufficient permissions over the destination document [{}].",
                newReference);
            return false;
        }
        return true;
    }

    protected void maybeMove(DocumentReference oldReference, DocumentReference newReference)
    {
        // Perform checks that are specific to the document source/destination type.

        if (!this.modelBridge.exists(oldReference)) {
            this.logger.warn("Skipping [{}] because it doesn't exist.", oldReference);
        } else if (this.checkAllRights(oldReference, newReference)) {
            move(oldReference, newReference);
        }
    }

    private void move(DocumentReference oldReference, DocumentReference newReference)
    {
        this.progressManager.pushLevelProgress(7, this);

        try {
            // Step 1: Delete the destination document if needed.
            this.progressManager.startStep(this);
            if (this.modelBridge.exists(newReference)) {
                if (this.request.isInteractive() && !confirmOverwrite(oldReference, newReference)) {
                    this.logger.warn(
                        "Skipping [{}] because [{}] already exists and the user doesn't want to overwrite it.",
                        oldReference, newReference);
                    return;
                } else if (!this.modelBridge.delete(newReference)) {
                    return;
                }
            }
            this.progressManager.endStep(this);

            // Step 2: Copy the source document to the destination.
            this.progressManager.startStep(this);
            if (!this.modelBridge.copy(oldReference, newReference)) {
                return;
            }
            this.progressManager.endStep(this);

            // Step 3: Update the destination document based on the source document parameters.
            this.progressManager.startStep(this);
            this.modelBridge.update(newReference, this.request.getEntityParameters(oldReference));
            this.progressManager.endStep(this);

            // Step 4 + 5: Update other documents that might be affected by this move.
            updateDocuments(oldReference, newReference);

            // Step 6: Delete the source document.
            this.progressManager.startStep(this);
            if (this.request.isDeleteSource()) {
                this.modelBridge.delete(oldReference);
            }
            this.progressManager.endStep(this);

            // Step 7: Create an automatic redirect.
            this.progressManager.startStep(this);
            if (this.request.isDeleteSource() && this.request.isAutoRedirect()) {
                this.modelBridge.createRedirect(oldReference, newReference);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void updateDocuments(DocumentReference oldReference, DocumentReference newReference)
    {
        // Step 3: Update the links.
        this.progressManager.startStep(this);
        if (this.request.isUpdateLinks()) {
            updateLinks(oldReference, newReference);
        }
        this.progressManager.endStep(this);

        // Step 4: (legacy) Preserve existing parent-child relationships by updating the parent field of documents
        // having the moved document as parent.
        this.progressManager.startStep(this);
        if (this.request.isUpdateParentField()) {
            this.modelBridge.updateParentField(oldReference, newReference);
        }
        this.progressManager.endStep(this);
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

    private void updateLinks(DocumentReference oldReference, DocumentReference newReference)
    {
        this.progressManager.pushLevelProgress(2, this);

        try {
            // Step 1: Update the links that target the old reference to point to the new reference.
            this.progressManager.startStep(this);
            if (this.request.isDeleteSource()) {
                updateBackLinks(oldReference, newReference);
            }
            this.progressManager.endStep(this);

            // Step 2: Update the relative links from the document content.
            this.progressManager.startStep(this);
            this.linkRefactoring.updateRelativeLinks(oldReference, newReference);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void updateBackLinks(DocumentReference oldReference, DocumentReference newReference)
    {
        Collection<String> wikiIds = Collections.singleton(oldReference.getWikiReference().getName());
        if (this.request.isUpdateLinksOnFarm()) {
            try {
                wikiIds = this.wikiDescriptorManager.getAllIds();
            } catch (WikiManagerException e) {
                this.logger.error("Failed to retrieve the list of wikis.", e);
            }
        }
        boolean popLevelProgress = false;
        try {
            if (wikiIds.size() > 0) {
                this.progressManager.pushLevelProgress(wikiIds.size(), this);
                popLevelProgress = true;
            }
            for (String wikiId : wikiIds) {
                this.progressManager.startStep(this);
                updateBackLinks(oldReference, newReference, wikiId);
                this.progressManager.endStep(this);
            }
        } finally {
            if (popLevelProgress) {
                this.progressManager.popLevelProgress(this);
            }
        }
    }

    private void updateBackLinks(DocumentReference oldReference, DocumentReference newReference, String wikiId)
    {
        this.logger.info("Updating the back-links for document [{}] in wiki [{}].", oldReference, wikiId);
        List<DocumentReference> backlinkDocumentReferences =
            this.modelBridge.getBackLinkedReferences(oldReference, wikiId);
        this.progressManager.pushLevelProgress(backlinkDocumentReferences.size(), this);

        try {
            for (DocumentReference backlinkDocumentReference : backlinkDocumentReferences) {
                this.progressManager.startStep(this);
                if (hasAccess(Right.EDIT, backlinkDocumentReference)) {
                    this.linkRefactoring.renameLinks(backlinkDocumentReference, oldReference, newReference);
                }
                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    @Override
    protected EntityReference getCommonParent()
    {
        if (this.request.isUpdateLinksOnFarm()) {
            return null;
        } else {
            List<EntityReference> entityReferences = new LinkedList<>(this.request.getEntityReferences());
            entityReferences.add(this.request.getDestination());
            return getCommonParent(entityReferences);
        }
    }
}
