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
 * Represents a reference to a space (space name). Note that nested spaces are supported.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class SpaceReference extends EntityReference
{
    /**
     * Special constructor that transforms a generic entity reference into a {@link SpaceReference}. It checks the
     * validity of the passed reference (ie correct type and correct parent).
     *
     * @exception IllegalArgumentException if the passed reference is not a valid space reference
     */
    public SpaceReference(EntityReference reference)
    {
        super(reference.getName(), reference.getType(), reference.getParent());
    }

    public SpaceReference(String spaceName, SpaceReference parent)
    {
        super(spaceName, EntityType.SPACE, parent);
    }

    public SpaceReference(String spaceName, WikiReference parent)
    {
        super(spaceName, EntityType.SPACE, parent);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden in order to verify the validity of the passed parent
     *
     * @see org.xwiki.model.reference.EntityReference#setParent(EntityReference)
     * @exception IllegalArgumentException if the passed parent is not a valid space reference parent (ie either
     *            a space reference or a wiki reference)
     */
    @Override public void setParent(EntityReference parent)
    {
        if (parent == null || (parent.getType() != EntityType.SPACE && parent.getType() != EntityType.WIKI)) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] for a space reference");
        }

        if (parent.getType() == EntityType.SPACE) {
            super.setParent(new SpaceReference(parent));
        } else {
            super.setParent(new WikiReference(parent));
        }
    }

    /**
     * {@inheritDoc}
     *
     * Overridden in order to verify the validity of the passed type
     *
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     * @exception IllegalArgumentException if the passed type is not a space type
     */
    @Override public void setType(EntityType type)
    {
        if (type != EntityType.SPACE) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a space reference");
        }

        super.setType(EntityType.SPACE);
    }
}
