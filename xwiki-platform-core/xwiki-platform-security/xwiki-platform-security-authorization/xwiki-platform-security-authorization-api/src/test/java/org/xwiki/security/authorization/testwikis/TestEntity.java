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

package org.xwiki.security.authorization.testwikis;

import java.util.Collection;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * An generic interface for all test entities.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface TestEntity
{
    /**
     * @return the reference of the entity represented by this test entity.
     */
    EntityReference getReference();

    /**
     * @return the type of reference used by this test entities.
     */
    EntityType getType();

    /**
     * Add a new child entity to this entity.
     * @param entity the child entity to be added.
     */
    void add(TestEntity entity);

    /**
     * Retrieve a direct child entity from this entity.
     * @param reference a reference to the child entity.
     * @return a test entity if found, null otherwise.
     */
    TestEntity getEntity(EntityReference reference);

    /**
     * @return a collection of all direct child entity of this entity.
     */
    Collection<TestEntity> getEntities();

    /**
     * Search for an entity in all descendant of this entity.
     * @param reference a reference to the search entity.
     * @return a test entity if found, null otherwise.
     */
    TestEntity searchEntity(EntityReference reference);

    /**
     * @return the parent entity of this entity.
     */
    TestEntity getParent();
}
