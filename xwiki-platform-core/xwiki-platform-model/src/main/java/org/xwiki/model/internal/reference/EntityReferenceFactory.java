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
package org.xwiki.model.internal.reference;

import javax.inject.Singleton;

import org.xwiki.collection.SoftCache;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

/**
 * Store and return shared soft reference of EntityReference instances of equals reference. This is used to reduce the
 * memory footprint of caches containing entity references.
 * 
 * @version $Id$
 * @since 10.8RC1
 */
@Component(roles = EntityReferenceFactory.class)
@Singleton
public class EntityReferenceFactory
{
    private SoftCache<EntityReference, EntityReference> cache = new SoftCache<>();

    /**
     * Return a cached reference equals to the passed one and with the same or extending class. If none could be found
     * the passed reference is stored and returned.
     * 
     * @param <E> the type of the reference
     * @param reference the entity reference to find
     * @return the cached entity reference
     */
    public <E extends EntityReference> E getReference(E reference)
    {
        if (reference == null) {
            return null;
        }

        EntityReference entityReference = this.cache.get(reference);

        if (entityReference == null || !reference.getClass().isAssignableFrom(entityReference.getClass())) {
            entityReference = unique(reference);

            this.cache.put(entityReference, entityReference);
        }

        return (E) entityReference;
    }

    private <E extends EntityReference> E unique(E reference)
    {
        EntityReference parent = reference.getParent();

        EntityReference cachedParent = getReference(parent);

        if (cachedParent != parent) {
            EntityReference uniqueReference = reference.replaceParent(cachedParent);

            if (reference.getClass().isAssignableFrom(uniqueReference.getClass())) {
                return (E) uniqueReference;
            }
        }

        return reference;
    }
}
