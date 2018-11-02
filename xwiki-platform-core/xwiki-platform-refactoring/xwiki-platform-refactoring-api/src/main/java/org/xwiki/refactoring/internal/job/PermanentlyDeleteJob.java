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
import org.xwiki.refactoring.job.RefactoringJobs;

/**
 * A job that can permanently delete an entity already in the recycle bin.
 *
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Named(RefactoringJobs.PERMANENTLY_DELETE)
public class PermanentlyDeleteJob extends AbstractDeletedDocumentsJob
{
    @Override
    public String getType()
    {
        return RefactoringJobs.PERMANENTLY_DELETE;
    }

    @Override
    protected void handleDeletedDocuments(List<Long> idsDeletedDocuments, boolean checkRights)
    {
        this.progressManager.pushLevelProgress(idsDeletedDocuments.size(), this);

        for (Long idToDelete : idsDeletedDocuments) {
            if (this.status.isCanceled()) {
                break;
            } else {
                this.progressManager.startStep(this);
                modelBridge.permanentlyDeleteDocument(idToDelete, checkRights);
                this.progressManager.endStep(this);
            }
        }

        this.progressManager.popLevelProgress(this);
    }
}
