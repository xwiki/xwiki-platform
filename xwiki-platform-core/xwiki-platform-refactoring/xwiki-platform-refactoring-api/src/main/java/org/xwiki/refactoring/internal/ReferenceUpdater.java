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

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Update all references (e.g. links in wiki content) in a given entity.
 * 
 * @version $Id$
 * @since 14.6RC1
 */
@Role
public interface ReferenceUpdater
{
    /**
     * @param documentReference the reference of the document in which to update the references
     * @param oldTargetReference the previous reference of the renamed entity
     * @param newTargetReference the new reference of the renamed entity
     * @param updatedEntities the map of entities that are or are going to be updated: the map contains the source
     * and target destination.
     * @since 16.10.0RC1
     */
    default void update(DocumentReference documentReference, EntityReference oldTargetReference,
        EntityReference newTargetReference, Map<EntityReference, EntityReference> updatedEntities)
    {
        update(documentReference, oldTargetReference, newTargetReference);
    }

    /**
     * @param documentReference the reference of the document in which to update the references
     * @param oldTargetReference the previous reference of the renamed entity
     * @param newTargetReference the new reference of the renamed entity
     */
    void update(DocumentReference documentReference, EntityReference oldTargetReference,
        EntityReference newTargetReference);
}
