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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.event.DocumentCopiedEvent;
import org.xwiki.refactoring.event.DocumentCopyingEvent;
import org.xwiki.refactoring.event.EntitiesCopiedEvent;
import org.xwiki.refactoring.event.EntitiesCopyingEvent;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.RefactoringJobs;

/**
 * Job used to copy entities without removing them afterwards.
 *
 * @since 10.11RC1
 * @version $Id$
 */
@Component
@Named(RefactoringJobs.COPY)
public class CopyJob extends AbstractCopyOrMoveJob<CopyRequest>
{
    @Override
    public String getType()
    {
        return RefactoringJobs.COPY;
    }

    @Override
    protected void runInternal() throws Exception
    {
        this.progressManager.pushLevelProgress(3, this);

        try {
            this.progressManager.startStep(this);
            EntitiesCopyingEvent entitiesCopyingEvent = new EntitiesCopyingEvent();
            this.observationManager.notify(entitiesCopyingEvent, this, this.getRequest());
            if (entitiesCopyingEvent.isCanceled()) {
                return;
            }
            this.progressManager.endStep(this);

            this.progressManager.startStep(this);
            super.runInternal();
            this.progressManager.endStep(this);

            this.progressManager.startStep(this);
            EntitiesCopiedEvent entitiesCopiedEvent = new EntitiesCopiedEvent();
            this.observationManager.notify(entitiesCopiedEvent, this, this.getRequest());
            this.progressManager.endStep(this);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    @Override
    protected void performRefactoring(DocumentReference sourceReference, DocumentReference targetReference)
    {
        DocumentCopyingEvent documentCopyingEvent = new DocumentCopyingEvent(sourceReference, targetReference);
        DocumentCopiedEvent documentCopiedEvent = new DocumentCopiedEvent(sourceReference, targetReference);
        try {
            copyOrMove(sourceReference, targetReference, documentCopyingEvent, documentCopiedEvent);
        } catch (Exception e) {
            this.logger.error("Failed to copy or move document from [{}] to [{}]", sourceReference, targetReference, e);
        }
    }

    @Override
    protected boolean atomicOperation(DocumentReference source, DocumentReference target)
    {
        return this.modelBridge.copy(source, target);
    }
}
