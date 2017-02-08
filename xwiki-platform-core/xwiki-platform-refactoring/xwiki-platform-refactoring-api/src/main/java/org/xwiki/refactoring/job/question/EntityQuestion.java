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
package org.xwiki.refactoring.job.question;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Question that a refactoring job could ask with a list of entities the user can select or un-select for the
 * refactoring.
 *
 * @version $Id$
 * @since 9.1RC1
 */
@Unstable
public class EntityQuestion
{
    /**
     * The map of entities concerned by the refactoring, where the entity reference if the key.
     */
    private Map<EntityReference, EntitySelection> entities = new HashMap<>();

    /**
     * @return the map of entities concerned by the refactoring, where the entity reference if the key.
     */
    public Map<EntityReference, EntitySelection> getEntities()
    {
        return entities;
    }

    /**
     * Add an entity reference concerned by the refactoring. Ensure the reference is stored only once.
     * @param entityReference the reference of the entity concerned by the refactoring
     * @return the unique entity selection corresponding to the entity
     */
    public EntitySelection addEntity(EntityReference entityReference)
    {
        EntitySelection entitySelection = entities.get(entityReference);
        if (entitySelection == null) {
            entitySelection = new EntitySelection(entityReference);
            entities.put(entitySelection.getEntityReference(), entitySelection);
        }
        return entitySelection;
    }

    /**
     * @param entityReference the reference of an entity
     * @return the EntitySelection corresponding to the entity, or null if the entity is not concerned
     *   by the refactoring
     */
    public EntitySelection get(EntityReference entityReference)
    {
        return entities.get(entityReference);
    }
}
