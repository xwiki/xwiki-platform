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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.event.DocumentRenamingEvent;
import org.xwiki.refactoring.event.EntitiesRenamedEvent;
import org.xwiki.refactoring.event.EntitiesRenamingEvent;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.security.authorization.Right;

/**
 * A job that can move entities to a new parent within the hierarchy.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(RefactoringJobs.MOVE)
public class MoveJob extends AbstractCopyOrMoveJob<MoveRequest>
{
    @Override
    public String getType()
    {
        return RefactoringJobs.MOVE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        this.progressManager.pushLevelProgress(3, this);

        try {
            this.progressManager.startStep(this);
            EntitiesRenamingEvent entitiesRenamingEvent = new EntitiesRenamingEvent();
            this.observationManager.notify(entitiesRenamingEvent, this, this.getRequest());
            if (entitiesRenamingEvent.isCanceled()) {
                return;
            }
            this.progressManager.endStep(this);

            this.progressManager.startStep(this);
            super.runInternal();
            this.progressManager.endStep(this);

            this.progressManager.startStep(this);
            EntitiesRenamedEvent entitiesRenamedEvent = new EntitiesRenamedEvent();
            this.observationManager.notify(entitiesRenamedEvent, this, this.getRequest());
            this.progressManager.endStep(this);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
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
    protected boolean checkAllRights(DocumentReference oldReference, DocumentReference newReference)
    {
        if (!hasAccess(Right.DELETE, oldReference)) {
            // The move operation is implemented as Copy + Delete.
            this.logger.error("You are not allowed to delete [{}].", oldReference);
            return false;
        } else {
            return super.checkAllRights(oldReference, newReference);
        }
    }

    @Override
    protected void performRefactoring(DocumentReference oldReference, DocumentReference newReference)
    {
        DocumentRenamingEvent documentRenamingEvent = new DocumentRenamingEvent(oldReference, newReference);
        DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(oldReference, newReference);
        copyOrMove(oldReference, newReference, documentRenamingEvent, documentRenamedEvent);
    }

    @Override
    protected boolean atomicOperation(DocumentReference source, DocumentReference target)
    {
        return this.modelBridge.rename(source, target);
    }
}
