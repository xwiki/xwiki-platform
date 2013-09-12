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
package org.xwiki.model.reference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A set of entities references.
 * <p>
 * Several "utility" reference can be used too like {@link PartialEntityReference} and {@link RegexEntityReference}
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class EntityReferenceSet
{
    private static class EntityReferenceEntry
    {
        public boolean or;

        public EntityReference reference;

        public boolean not;

        public EntityReferenceEntry(boolean or, EntityReference reference, boolean not)
        {
            this.or = or;
            this.reference = reference;
            this.not = not;
        }

        public boolean matches(EntityReference otherReference)
        {
            boolean result;

            if (otherReference == this.reference) {
                result = true;
            } else {
                result = matchesReference(otherReference);
            }

            return this.not ? !result : result;
        }

        /**
         * Try to match the provided reference.
         * 
         * @param otherReference the reference to match
         * @return true if the provided reference is matched
         */
        protected boolean matchesReference(EntityReference otherReference)
        {
            return this.reference == null || this.reference.equals(otherReference);
        }
    }

    private List<EntityReferenceEntry> entries = new ArrayList<EntityReferenceEntry>();

    /**
     * @param entityreference the reference
     * @return the entity reference set
     */
    public EntityReferenceSet and(EntityReference entityreference)
    {
        this.entries.add(new EntityReferenceEntry(false, entityreference, false));

        return this;
    }

    /**
     * @param entityReference the reference
     * @return the entity reference set
     */
    public EntityReferenceSet andnot(EntityReference entityReference)
    {
        this.entries.add(new EntityReferenceEntry(false, entityReference, true));

        return this;
    }

    /**
     * @param entityReference the reference
     * @return the entity reference set
     */
    public EntityReferenceSet or(EntityReference entityReference)
    {
        this.entries.add(new EntityReferenceEntry(true, entityReference, false));

        return this;
    }

    /**
     * @param entityReference the reference
     * @return the entity reference set
     */
    public EntityReferenceSet ornot(EntityReference entityReference)
    {
        this.entries.add(new EntityReferenceEntry(true, entityReference, true));

        return this;
    }

    /**
     * @param entityReference the reference
     * @return the entity reference set
     */
    public boolean matches(EntityReference entityReference)
    {
        if (this.entries.isEmpty()) {
            return true;
        }

        Iterator<EntityReferenceEntry> iterator = this.entries.iterator();

        boolean result = iterator.next().matches(entityReference);

        while (iterator.hasNext()) {
            EntityReferenceEntry entry = iterator.next();

            if (entry.or) {
                result |= entry.matches(entityReference);
            } else {
                result &= entry.matches(entityReference);

                if (!result) {
                    break;
                }
            }
        }

        return result;
    }
}
