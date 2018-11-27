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
import org.xwiki.model.reference.SpaceReference;
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
    protected boolean isDeleteSources()
    {
        // rename always delete original sources
        return true;
    }

    @Override
    protected void process(Collection<EntityReference> entityReferences)
    {
        if (entityReferences.size() == 1) {
            process(entityReferences.iterator().next());
        }
    }

    @Override
    protected void process(EntityReference source)
    {
        // Perform generic checks that don't depend on the source/destination type.

        EntityReference destination = this.request.getDestination();
        if (source.getType() != destination.getType()) {
            this.logger.error("You cannot change the entity type (from [{}] to [{}]).", source.getType(),
                destination.getType());
            return;
        }

        super.process(source);
    }

    @Override
    protected void process(DocumentReference source, EntityReference destination)
    {
        // We know the destination is a document (see above).
        DocumentReference destinationDocumentReference = new DocumentReference(destination);
        if (this.request.isDeep() && isSpaceHomeReference(source)) {
            if (isSpaceHomeReference(destinationDocumentReference)) {
                // Rename an entire space.
                process(source.getLastSpaceReference(), destinationDocumentReference.getLastSpaceReference());
            } else {
                this.logger.error("You cannot transform a non-terminal document [{}] into a terminal document [{}]"
                    + " and preserve its child documents at the same time.", source, destinationDocumentReference);
            }
        } else {
            maybeMove(source, destinationDocumentReference);
        }
    }

    @Override
    protected void process(SpaceReference source, EntityReference destination)
    {
        // We know the destination is a space (see above).
        process(source, new SpaceReference(destination));
    }
}
