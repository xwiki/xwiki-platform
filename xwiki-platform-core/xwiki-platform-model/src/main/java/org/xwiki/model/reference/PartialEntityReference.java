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

import org.xwiki.model.EntityType;

/**
 * An {@link EntityReference} used to match another one.
 * <p>
 * The {@link PartialEntityReference} can be partial or contain holes, for example if only the space regex reference is
 * provided it will tries to match only space part of the provided reference and consider other parts as matched.
 * <p>
 * It's of course possible to mix regex and "normal" {@link EntityReference} member. In that case normal member
 * {@link #equals(Object)} method behave as usual and then call following member {@link #equals(Object)} etc.
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class PartialEntityReference extends EntityReference
{
    /**
     * Create a new root EntityReference.
     * 
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     */
    public PartialEntityReference(String name, EntityType type)
    {
        super(name, type);
    }

    /**
     * Create a new EntityReference.
     * 
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     * @param parent parent reference for the newly created entity reference, may be null.
     */
    public PartialEntityReference(String name, EntityType type, EntityReference parent)
    {
        super(name, type, parent);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This equals method actually skip null elements.
     * </p>
     * 
     * @see org.xwiki.model.reference.EntityReference#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof EntityReference)) {
            return false;
        }

        EntityReference reference = (EntityReference) obj;

        for (EntityReference entity = reference; entity != null; entity = entity.getParent()) {
            if (getType().equals(entity.getType())) {
                if (!isNameEqual(entity.getName())) {
                    return false;
                } else {
                    return getParent() != null ? getParent().equals(entity.getParent()) : true;
                }
            }
        }

        return true;
    }

    /**
     * @param name the name to compare to this name
     * @return true of the passed name is equal to this name
     */
    protected boolean isNameEqual(String name)
    {
        return getName() != null && getName().equals(name);
    }
}
