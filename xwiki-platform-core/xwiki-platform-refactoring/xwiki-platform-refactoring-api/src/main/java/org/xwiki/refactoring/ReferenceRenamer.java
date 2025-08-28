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
package org.xwiki.refactoring;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.stability.Unstable;

/**
 * Allow to replace references during rename/move refactoring operations.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Role
public interface ReferenceRenamer
{
    /**
     * Change references of the given block so that the references pointing to the old target points to the new target.
     *
     * @param block the {@link Block} to modify
     * @param currentDocumentReference the current document reference
     * @param oldTarget the previous reference of the renamed entity (attachment or document)
     * @param newTarget the new reference of the renamed entity (attachment or document)
     * @param relative {@code true} if the link should be serialized relatively to the current document
     * @return {@code true} if the given {@link Block} was modified
     */
    boolean renameReferences(Block block, DocumentReference currentDocumentReference, DocumentReference oldTarget,
        DocumentReference newTarget, boolean relative);

    /**
     * Change references of the given block so that the references pointing to the old target points to the new target.
     *
     * @param block the {@link Block} to modify
     * @param currentDocumentReference the current document reference
     * @param oldTarget the previous reference of the renamed entity (attachment or document)
     * @param newTarget the new reference of the renamed entity (attachment or document)
     * @param relative {@code true} if the link should be serialized relatively to the current document
     * @param updatedEntities the map of entities that are or are going to be updated: the map contains the source
     *      and target destination.
     * @return {@code true} if the given {@link Block} was modified
     * @since 16.10.0RC1
     */
    @Unstable
    default boolean renameReferences(Block block, DocumentReference currentDocumentReference,
        DocumentReference oldTarget, DocumentReference newTarget, boolean relative,
        Map<EntityReference, EntityReference> updatedEntities)
    {
        return renameReferences(block, currentDocumentReference, oldTarget, newTarget, relative);
    }

    /**
     * Change references of the given block so that the references pointing to the old target points to the new target.
     *
     * @param block the {@link Block} to modify
     * @param currentDocumentReference the current document reference
     * @param oldTarget the previous reference of the renamed entity (attachment or document)
     * @param newTarget the new reference of the renamed entity (attachment or document)
     * @param relative {@code true} if the link should be serialized relatively to the current document
     * @return {@code true} if the given {@link Block} was modified
     * @since 14.2RC1
     */
    default boolean renameReferences(Block block, DocumentReference currentDocumentReference,
        AttachmentReference oldTarget, AttachmentReference newTarget, boolean relative)
    {
        return false;
    }

    /**
     * Change references of the given block so that the references pointing to the old target points to the new target.
     *
     * @param block the {@link Block} to modify
     * @param currentDocumentReference the current document reference
     * @param oldTarget the previous reference of the renamed entity (attachment or document)
     * @param newTarget the new reference of the renamed entity (attachment or document)
     * @param relative {@code true} if the link should be serialized relatively to the current document
     * @param updatedEntities the map of entities that are or are going to be updated: the map contains the source
     *      and target destination.
     * @return {@code true} if the given {@link Block} was modified
     * @since 16.10.0RC1
     */
    @Unstable
    default boolean renameReferences(Block block, DocumentReference currentDocumentReference,
        AttachmentReference oldTarget, AttachmentReference newTarget, boolean relative,
        Map<EntityReference, EntityReference> updatedEntities)
    {
        return renameReferences(block, currentDocumentReference, oldTarget, newTarget, relative);
    }

}
