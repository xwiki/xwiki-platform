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
import org.xwiki.refactoring.job.RefactoringJobs;

/**
 * A job that can rename entities.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(RefactoringJobs.RENAME)
public class RenameJob extends MoveJob
{
    @Override
    public String getType()
    {
        return RefactoringJobs.RENAME;
    }

    @Override
    protected void runInternal() throws Exception
    {
        Collection<EntityReference> entityReferences = this.request.getEntityReferences();
        if (entityReferences != null && entityReferences.size() == 1 && this.request.getDestination() != null) {
            process(entityReferences.iterator().next());
        }
    }

    @Override
    protected void process(EntityReference source)
    {
        // Perform generic checks that don't depend on the source/destination type.

        EntityReference destination = this.request.getDestination();
        if (source.getType() != destination.getType()) {
            this.logger.warn("You cannot change the entity type (from [{}] to [{}]).", source.getType(),
                destination.getType());
            return;
        }

        super.process(source);
    }

    @Override
    protected void move(DocumentReference source, EntityReference destination)
    {
        // We know the destination is a document (see above).
        maybeMove(source, new DocumentReference(destination));
    }

    @Override
    protected void maybeMove(DocumentReference oldReference, DocumentReference newReference)
    {
        // Perform checks that are specific to the document source/destination type.

        if (isTerminal(newReference) && !isTerminal(oldReference) && this.request.isDeep()) {
            this.logger.warn("You cannot transform a non-terminal document [{}] into a terminal document [{}]"
                + " and preserve its child documents at the same time.", oldReference, newReference);
            return;
        }

        super.maybeMove(oldReference, newReference);
    }
}
