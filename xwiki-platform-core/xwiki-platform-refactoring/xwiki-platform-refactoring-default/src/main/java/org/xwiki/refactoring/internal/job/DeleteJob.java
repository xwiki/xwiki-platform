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

import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.security.authorization.Right;

/**
 * A job that can delete entities.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(RefactoringJobs.DELETE)
public class DeleteJob extends AbstractOldCoreEntityJob<EntityRequest, EntityJobStatus<EntityRequest>>
{
    @Override
    public String getType()
    {
        return RefactoringJobs.DELETE;
    }

    @Override
    protected EntityJobStatus<EntityRequest> createNewStatus(EntityRequest request)
    {
        return new EntityJobStatus<EntityRequest>(request, this.observationManager, this.loggerManager, null);
    }

    @Override
    protected void process(EntityReference entityReference)
    {
        // Dispatch the delete operation based on the entity type.

        switch (entityReference.getType()) {
            case DOCUMENT:
                delete(new DocumentReference(entityReference), this.request.isDeep());
                break;
            default:
                this.logger.warn("Unsupported entity type [{}].", entityReference.getType());
        }
    }

    private void delete(DocumentReference documentReference, boolean deep)
    {
        this.progressManager.pushLevelProgress(2, this);

        try {
            // Step 1: Delete the document if possible.
            this.progressManager.startStep(this);
            if (!exists(documentReference)) {
                this.logger.warn("Skipping [{}] because it doesn't exist.", documentReference);
            } else if (!hasAccess(Right.DELETE, documentReference)) {
                this.logger.warn("You are not allowed to delete [{}].", documentReference);
            } else {
                delete(documentReference);
            }
            this.progressManager.endStep(this);

            // Note that we continue with the children even if deleting the parent failed.

            // Step 2: Delete the child documents.
            this.progressManager.startStep(this);
            if (deep && !isTerminal(documentReference)) {
                deleteChildren(documentReference);
            }
            this.progressManager.endStep(this);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void deleteChildren(DocumentReference documentReference)
    {
        List<DocumentReference> childReferences = getChildren(documentReference);

        this.progressManager.pushLevelProgress(childReferences.size(), this);

        try {
            for (DocumentReference childReference : childReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);

                    // We don't have to delete recursively because #getChildDocuments() returns all the descendants
                    // actually (because we don't have a way to retrieve the direct child documents at the moment).
                    delete(childReference, false);

                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }
}
