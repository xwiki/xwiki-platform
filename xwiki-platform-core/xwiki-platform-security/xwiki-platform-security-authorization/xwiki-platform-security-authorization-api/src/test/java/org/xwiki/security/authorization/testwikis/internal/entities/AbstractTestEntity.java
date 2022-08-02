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

package org.xwiki.security.authorization.testwikis.internal.entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.testwikis.TestEntity;

/**
 * Base class for all test entities.
 *
 * @version $Id$
 * @since 5.0M2
 */
public abstract class AbstractTestEntity implements TestEntity
{
    /** Reference of this entity. */
    private final EntityReference reference;

    /** Parent entity of this entity. */
    private final TestEntity parent;

    /** Map of children entities. */
    private final Map<EntityReference, TestEntity> entities = new HashMap<EntityReference, TestEntity>();

    /** Create a new root entity (wikis). */
    AbstractTestEntity() {
        reference = null;
        parent = null;
    }

    /**
     * Create a new entity.
     * @param reference reference for this entity.
     * @param parent the parent entity of this entity.
     */
    AbstractTestEntity(EntityReference reference, TestEntity parent) {
        this.parent = parent;
        this.reference = reference;

        addToParent(parent);
    }

    /**
     * Add the current entity to his parent. Called by the constructor, overridden in subclasses to attach to
     * another collection of the parent then the default one.
     * @param parent the parent to attach to.
     */
    protected void addToParent(TestEntity parent) {
        parent.add(this);
    }

    @Override
    public EntityReference getReference()
    {
        return reference;
    }

    @Override
    public void add(TestEntity entity)
    {
        entities.put(entity.getReference(), entity);
    }

    @Override
    public TestEntity getEntity(EntityReference reference)
    {
        return entities.get(reference);
    }

    @Override
    public Collection<TestEntity> getEntities()
    {
        return entities.values();
    }

    @Override
    public TestEntity searchEntity(EntityReference reference)
    {
        EntityReference parentRef;
        EntityReference currentRef = reference;
        do {
            parentRef = currentRef.getParent();
            if (this.reference == parentRef || (this.reference != null && this.reference.equals(parentRef))) {
                if (currentRef == reference) {
                    return entities.get(reference);
                } else {
                    TestEntity child = entities.get(currentRef);
                    if (child == null) {
                        return null;
                    }
                    return child.searchEntity(reference);
                }
            }
            currentRef = parentRef;
        } while(currentRef != null);
        return null;
    }

    @Override
    public TestEntity getParent()
    {
        return parent;
    }
}
