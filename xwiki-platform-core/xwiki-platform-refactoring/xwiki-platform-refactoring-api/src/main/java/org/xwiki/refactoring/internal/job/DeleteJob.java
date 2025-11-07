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
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.RefactoringConfiguration;
import org.xwiki.refactoring.batch.BatchOperationExecutor;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.security.authorization.Right;

/**
 * A job that can delete entities.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(RefactoringJobs.DELETE)
public class DeleteJob extends AbstractEntityJobWithChecks<DeleteRequest, EntityJobStatus<DeleteRequest>>
{
    @Inject
    private BatchOperationExecutor batchOperationExecutor;

    @Inject
    private RefactoringConfiguration configuration;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    protected DeleteRequest castRequest(Request request)
    {
        return new DeleteRequest(request);
    }

    @Override
    public String getType()
    {
        return RefactoringJobs.DELETE;
    }

    @Override
    protected void getEntities(Collection<EntityReference> entityReferences)
    {
        // Collect the list of concerned entities.
        super.getEntities(entityReferences);

        // Allow others to exclude concerned entities.
        this.notifyDocumentsDeleting();
    }

    @Override
    protected void process(Collection<EntityReference> entityReferences)
    {
        // Wrap the work as a batch operation.
        batchOperationExecutor.execute(() -> super.process(entityReferences));
    }

    @Override
    protected void process(EntityReference entityReference)
    {
        // Dispatch the delete operation based on the entity type.

        switch (entityReference.getType()) {
            case DOCUMENT:
                try {
                    process(new DocumentReference(entityReference));
                } catch (Exception e) {
                    this.logger.error("Failed to delete document with reference [{}]", entityReference, e);
                }
                break;
            case SPACE:
                process(new SpaceReference(entityReference));
                break;
            default:
                this.logger.error("Unsupported entity type [{}].", entityReference.getType());
        }
    }

    private void process(DocumentReference documentReference) throws Exception
    {
        if (this.request.isDeep() && isSpaceHomeReference(documentReference)) {
            process(documentReference.getLastSpaceReference());
        } else {
            maybeDelete(documentReference);
        }
    }

    private void process(SpaceReference spaceReference)
    {
        visitDocuments(spaceReference, new Visitor<DocumentReference>()
        {
            @Override
            public void visit(DocumentReference documentReference)
            {
                try {
                    maybeDelete(documentReference);
                } catch (Exception e) {
                    logger.error("Failed to delete document [{}] from space [{}]", documentReference, spaceReference,
                        e);
                }
            }
        });
    }

    private void maybeDelete(DocumentReference documentReference) throws Exception
    {
        boolean skipRecycleBin = this.configuration.isRecycleBinSkippingActivated()
            && this.documentAccessBridge.isAdvancedUser() && getRequest().shouldSkipRecycleBin();
        EntitySelection entitySelection = this.getConcernedEntitiesEntitySelection(documentReference);
        if (entitySelection == null) {
            this.logger.info("Skipping [{}] because it does not match any entity selection.", documentReference);
        } else if (!entitySelection.isSelected()) {
            this.logger.info("Skipping [{}] because it has been unselected.", documentReference);
        } else if (!this.modelBridge.exists(documentReference)) {
            this.logger.warn("Skipping [{}] because it doesn't exist.", documentReference);
        } else if (!hasAccess(Right.DELETE, documentReference)) {
            this.logger.error("You are not allowed to delete [{}].", documentReference);
        } else if (!skipRecycleBin) {
            delete(documentReference, false, "[{}] has been successfully moved to the recycle bin.");
        } else {
            delete(documentReference, true, "[{}] has been successfully deleted.");
        }
    }

    private void delete(DocumentReference documentReference, boolean skipRecycleBin, String logMessage)
    {
        // Delete the document
        this.modelBridge.delete(documentReference, skipRecycleBin);
        this.logger.debug(logMessage, documentReference);

        DocumentReference backlinkDocumentReference = documentReference;
        // the NewBackLinkTargets map store the document reference without a locale specified so ensure to remove it
        // from the reference if it's the root locale.
        if (documentReference.getLocale() == Locale.ROOT) {
            backlinkDocumentReference = new DocumentReference(documentReference, (Locale) null);
        }

        // Create a redirect, if requested
        DocumentReference newTarget = getRequest().getNewBacklinkTargets().get(backlinkDocumentReference);

        if (getRequest().isAutoRedirect() && newTarget != null) {
            if (getRequest().isVerbose()) {
                this.logger.info("Creating automatic redirect from [{}] to [{}].", documentReference, newTarget);
            }
            this.modelBridge.createRedirect(documentReference, newTarget);
        }
    }
}
