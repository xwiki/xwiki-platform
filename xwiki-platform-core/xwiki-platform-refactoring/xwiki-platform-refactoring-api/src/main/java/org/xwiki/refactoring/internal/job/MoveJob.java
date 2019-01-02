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
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.RefactoringJobs;

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
    protected boolean isDeleteSources()
    {
        return this.request.isDeleteSource();
    }

    @Override
    protected void postMove(DocumentReference oldReference, DocumentReference newReference)
    {
        // Create an automatic redirect.
        this.progressManager.startStep(this);
        if (isDeleteSources() && this.request.isAutoRedirect()) {
            this.modelBridge.createRedirect(oldReference, newReference);
        }
    }

    @Override
    protected void postUpdateDocuments(DocumentReference oldReference, DocumentReference newReference)
    {
        // (legacy) Preserve existing parent-child relationships by updating the parent field of documents
        // having the moved document as parent.
        this.progressManager.startStep(this);
        if (this.request.isUpdateParentField()) {
            this.modelBridge.updateParentField(oldReference, newReference);
        }
        this.progressManager.endStep(this);
    }
}
