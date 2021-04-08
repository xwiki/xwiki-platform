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
package org.xwiki.refactoring.script;

import java.util.Collection;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.refactoring.job.RestoreRequest;

/**
 * Factory dedicated to the creation of the requests.
 *
 * @version $Id$
 * @since 10.11RC1
 */
@Role
public interface RequestFactory
{
    /**
     * Creates a request to move the specified source entities to the specified destination entity (which becomes their
     * new parent).
     *
     * @param sources specifies the entities to be moved
     * @param destination specifies the place where to move the entities (their new parent entity)
     * @return the move request
     */
    MoveRequest createMoveRequest(Collection<EntityReference> sources, EntityReference destination);

    /**
     * Creates a request to move the specified source entity to the specified destination entity (which becomes its new
     * parent).
     *
     * @param source specifies the entity to be moved
     * @param destination specifies the place where to move the source entity (its new parent entity)
     * @return the move request
     */
    MoveRequest createMoveRequest(EntityReference source, EntityReference destination);

    /**
     * Creates a request to rename the entity specified by the given old reference.
     *
     * @param oldReference the entity to rename
     * @param newReference the new entity reference after the rename
     * @return the rename request
     */
    MoveRequest createRenameRequest(EntityReference oldReference, EntityReference newReference);

    /**
     * Creates a request to rename the specified entity.
     *
     * @param reference the entity to rename
     * @param newName the new entity name
     * @return the rename request
     */
    MoveRequest createRenameRequest(EntityReference reference, String newName);

    /**
     * Creates a request to copy the specified source entities to the specified destination entity.
     *
     * @param sources specifies the entities to be copied
     * @param destination specifies the place where to copy the entities (becomes the parent of the copies)
     * @return the copy request
     */
    CopyRequest createCopyRequest(Collection<EntityReference> sources, EntityReference destination);

    /**
     * Creates a request to copy the specified source entity to the specified destination entity.
     *
     * @param source specifies the entity to be copied
     * @param destination specifies the place where to copy the source entity (becomes the parent of the copy)
     * @return the copy request
     */
    CopyRequest createCopyRequest(EntityReference source, EntityReference destination);

    /**
     * Creates a request to copy the specified entity with a different reference.
     *
     * @param sourceReference the entity to copy
     * @param copyReference the reference to use for the copy
     * @return the copy-as request
     */
    CopyRequest createCopyAsRequest(EntityReference sourceReference, EntityReference copyReference);

    /**
     * Creates a request to copy the specified entity with a different name.
     *
     * @param reference the entity to copy
     * @param copyName the name of the entity copy
     * @return the copy-as request
     */
    CopyRequest createCopyAsRequest(EntityReference reference, String copyName);

    /**
     * Creates a request to delete the specified entities.
     *
     * @param entityReferences the entities to delete
     * @return the delete request
     */
    EntityRequest createDeleteRequest(Collection<EntityReference> entityReferences);

    /**
     * Creates a request to create the specified entities.
     *
     * @param entityReferences the entities to create
     * @return the create request
     * @since 7.4M2
     */
    CreateRequest createCreateRequest(Collection<EntityReference> entityReferences);

    /**
     * Creates a request to permanently delete a specified batch of deleted documents from the recycle bin.
     *
     * @param batchId the ID of the batch of deleted documents to permanently delete
     * @return the permanently delete request
     * @since 10.10RC1
     */
    PermanentlyDeleteRequest createPermanentlyDeleteRequest(String batchId);

    /**
     * Creates a request to permanently delete a specified list of deleted documents from the recycle bin.
     *
     * @param deletedDocumentIds the list of IDs of the deleted documents to permanently delete
     * @return the permanently delete request
     * @since 10.10RC1
     */
    PermanentlyDeleteRequest createPermanentlyDeleteRequest(List<Long> deletedDocumentIds);

    /**
     * Creates a request to restore a specified batch of deleted documents from the recycle bin.
     *
     * @param batchId the ID of the batch of deleted documents to restore
     * @return the restore request
     * @since 9.4RC1
     */
    RestoreRequest createRestoreRequest(String batchId);

    /**
     * Creates a request to restore a specified list of deleted documents from the recycle bin.
     *
     * @param deletedDocumentIds the list of IDs of the deleted documents to restore
     * @return the restore request
     * @since 9.4RC1
     */
    RestoreRequest createRestoreRequest(List<Long> deletedDocumentIds);

    /**
     * Creates a request to replace the occurrences of the old user reference with the new user reference.
     * 
     * @param oldUserReference the old user reference
     * @param newUserReference the new user reference
     * @return the request to replace the user reference
     * @since 11.8RC1
     */
    ReplaceUserRequest createReplaceUserRequest(DocumentReference oldUserReference, DocumentReference newUserReference);
}
