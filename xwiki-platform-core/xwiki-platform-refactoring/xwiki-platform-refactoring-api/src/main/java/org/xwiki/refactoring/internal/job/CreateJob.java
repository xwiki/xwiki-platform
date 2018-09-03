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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.OverwriteQuestion;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.security.authorization.Right;

/**
 * A job that can create entities (optionally, from a template).
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named(RefactoringJobs.CREATE)
public class CreateJob extends AbstractEntityJob<CreateRequest, EntityJobStatus<CreateRequest>>
{
    private Boolean overwriteAll;

    @Override
    public String getType()
    {
        return RefactoringJobs.CREATE;
    }

    @Override
    protected void process(EntityReference entityReference)
    {
        // Dispatch the create operation based on the entity type.

        switch (entityReference.getType()) {
            case DOCUMENT:
                process(new DocumentReference(entityReference));
                break;
            case SPACE:
                process(new SpaceReference(entityReference));
                break;
            default:
                this.logger.error("Unsupported entity type [{}].", entityReference.getType());
        }
    }

    private void process(DocumentReference documentReference)
    {
        // The template, if specified, must be a document.
        DocumentReference templateDocumentReference = null;
        if (this.request.getTemplateReference() != null
            && this.request.getTemplateReference().extractReference(EntityType.DOCUMENT) != null) {
            templateDocumentReference =
                new DocumentReference(this.request.getTemplateReference().extractReference(EntityType.DOCUMENT));
        }

        progressManager.pushLevelProgress(3, this);

        try {
            progressManager.startStep(this, "Main document");

            // Handle the target document creation first
            maybeCreate(documentReference, templateDocumentReference);

            progressManager.startStep(this, "Template children");

            // If both the target and template documents are non-terminal and also the operation is deep, handle
            // possible children of the template document.
            if (this.request.isDeep() && isSpaceHomeReference(documentReference) && templateDocumentReference != null
                && isSpaceHomeReference(templateDocumentReference)) {
                process(documentReference.getLastSpaceReference());
            }

            progressManager.startStep(this, "Remove lock from the main document");

            // Remove any existing lock of the user that started this job on the target document.
            this.modelBridge.removeLock(documentReference);
        } finally {
            // Done, go back to parent progress level
            this.progressManager.popLevelProgress(this);
        }
    }

    private void process(final SpaceReference newSpaceReference)
    {
        // The template, if specified, must be a space.
        SpaceReference extractedSpaceReference = null;
        if (this.request.getTemplateReference() != null
            && this.request.getTemplateReference().extractReference(EntityType.SPACE) != null) {
            extractedSpaceReference =
                new SpaceReference(this.request.getTemplateReference().extractReference(EntityType.SPACE));
        }
        final SpaceReference templateSpaceReference = extractedSpaceReference;

        if (templateSpaceReference != null) {
            // Space from template space.
            visitDocuments(templateSpaceReference, new Visitor<DocumentReference>()
            {
                @Override
                public void visit(final DocumentReference templateDocumentReference)
                {
                    DocumentReference newDocumentReference =
                        templateDocumentReference.replaceParent(templateSpaceReference, newSpaceReference);
                    maybeCreate(newDocumentReference, templateDocumentReference);
                }
            });
        } else {
            // Empty space (webhome document).
            DocumentReference newSpaceWebHomeReference = new DocumentReference("WebHome", newSpaceReference);
            maybeCreate(newSpaceWebHomeReference, null);
        }
    }

    private void maybeCreate(DocumentReference newDocumentReference, DocumentReference templateDocumentReference)
    {
        if (request.getSkippedEntities().contains(newDocumentReference)) {
            this.logger.debug("Skipping creation of document [{}], as specified in the request.", newDocumentReference);
        } else if (this.modelBridge.exists(newDocumentReference)
            && (!this.request.isInteractive() || !confirmOverwrite(newDocumentReference, templateDocumentReference))) {
            this.logger.warn("Skipping creation of document [{}] because it already exists.", newDocumentReference);
        } else if (!hasAccess(Right.EDIT, newDocumentReference)) {
            this.logger.error("You are not allowed to create the document [{}].", newDocumentReference);
        } else if (templateDocumentReference == null) {
            // If no template is specified, then we are just creating an empty document.
            this.modelBridge.create(newDocumentReference);
        } else if (!hasAccess(Right.VIEW, templateDocumentReference)) {
            this.logger.error("You are not allowed to view the template document [{}].", templateDocumentReference);
        } else if (!this.modelBridge.exists(templateDocumentReference)) {
            // Should generally not happen, but you never know.
            this.logger.error("Template document [{}] does not exist.", templateDocumentReference);
        } else {
            this.modelBridge.copy(templateDocumentReference, newDocumentReference);
        }
    }

    /*
     * TODO: Refactor since the same code is used in {@link MoveJob}.
     */
    private boolean confirmOverwrite(DocumentReference newDocReference, DocumentReference templateDocumentReference)
    {
        if (this.overwriteAll == null) {
            OverwriteQuestion question = new OverwriteQuestion(templateDocumentReference, newDocReference);
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
}
