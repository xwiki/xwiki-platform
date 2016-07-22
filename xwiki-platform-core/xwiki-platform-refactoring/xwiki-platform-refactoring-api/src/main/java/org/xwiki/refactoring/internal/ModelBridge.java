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
package org.xwiki.refactoring.internal;

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Interface used to access the XWiki model and to perform low level operations on it.
 * <p>
 * Keep this interface internal because it's not part of the public API exposed by this module.
 * 
 * @version $Id$
 * @since 7.4M2
 */
@Role
public interface ModelBridge
{
    /**
     * Create a new document with the specified reference.
     * 
     * @param documentReference the reference of the new document
     * @return {@code true} if the document was create successfully, {@code false} if the creation failed
     */
    boolean create(DocumentReference documentReference);

    /**
     * Copy a document with the specified reference.
     * 
     * @param source the document to copy
     * @param destination the reference of the document copy that is going to be created
     * @return {@code true} if the document was copied successfully, {@code false} if the copy failed
     */
    boolean copy(DocumentReference source, DocumentReference destination);

    /**
     * Delete the specified document.
     * 
     * @param documentReference the reference of the document to delete
     * @return {@code true} if the document was deleted successfully, {@code false} if the delete failed
     */
    boolean delete(DocumentReference documentReference);

    /**
     * Remove the edit lock from the specified document.
     * 
     * @param documentReference the document to unlock
     * @return {@code true} if the lock was removed successfully, {@code false} if the remove failed
     */
    boolean removeLock(DocumentReference documentReference);

    /**
     * Create a redirect from the old document reference to the new document reference.
     * 
     * @param oldReference the old document reference
     * @param newReference the new document reference
     */
    void createRedirect(DocumentReference oldReference, DocumentReference newReference);

    /**
     * @param reference a document reference
     * @return {@code true} if the specified document exists, {@code false} otherwise
     */
    boolean exists(DocumentReference reference);

    /**
     * @param reference a document reference
     * @return the list of documents that have links to the specified document
     */
    List<DocumentReference> getBackLinkedReferences(DocumentReference reference);

    /**
     * @param spaceReference a space reference
     * @return the list of all the documents from the specified space and its nested spaces
     */
    List<DocumentReference> getDocumentReferences(SpaceReference spaceReference);

    /**
     * @param oldParentReference the old document reference for which to update its children's parent fields
     * @param newParentReference the new value to set in the chidlren's parent field
     * @return {@code true} if the parent fields were successfully updated, {@code false} if the update failed
     * @since 8.0M2, 7.4.2
     */
    boolean updateParentField(DocumentReference oldParentReference, DocumentReference newParentReference);

    /**
     * Sets the current user reference on the execution context.
     * 
     * @param userReference the user reference to put on the execution context
     * @return the previous user reference that was on the execution context
     */
    DocumentReference setContextUserReference(DocumentReference userReference);

    /**
     * Modify the specified document based on the given parameters (usually the fields to update).
     * 
     * @param documentReference specifies the document to update
     * @param parameters specifies the updates to perform (e.g. which fields to update)
     */
    void update(DocumentReference documentReference, Map<String, String> parameters);
}
