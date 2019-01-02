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
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.RefactoringJobs;

/**
 * Job used to copy entities without removing them afterwards.
 *
 * @since 10.11RC1
 * @version $Id$
 */
@Component
@Named(RefactoringJobs.COPY_AS)
public class CopyAsJob extends AbstractCopyOrMoveJob<CopyRequest>
{
    @Override
    public String getType()
    {
        return RefactoringJobs.COPY_AS;
    }

    @Override
    protected boolean isDeleteSources()
    {
        // copy never deletes sources
        return false;
    }

    @Override
    protected void postMove(DocumentReference oldReference, DocumentReference newReference)
    {
        // do nothing
    }

    @Override
    protected void postUpdateDocuments(DocumentReference oldReference, DocumentReference newReference)
    {
        // do nothing
    }

    @Override
    protected boolean processOnlySameSourceDestinationTypes()
    {
        // copyas only process same destination types
        return true;
    }
}
