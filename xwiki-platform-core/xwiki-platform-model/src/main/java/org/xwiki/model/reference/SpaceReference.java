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

import java.beans.Transient;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.xwiki.component.util.DefaultParameterizedType;
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
     * The {@link Type} for a {@code Provider<SpaceReference>}.
     * 
     * @since 7.2M1
     */
    public static final Type TYPE_PROVIDER = new DefaultParameterizedType(null, Provider.class, SpaceReference.class);

    /**
     * Special constructor that transforms a generic entity reference into a {@link SpaceReference}. It checks the
     * validity of the passed reference (ie correct type).
     *
     * @param reference the entity reference to transforms
     * @exception IllegalArgumentException if the passed reference is not a valid space reference
     */
    public SpaceReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone an SpaceReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     * @since 3.3M2
     */
    protected SpaceReference(EntityReference reference, EntityReference oldReference, EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * Create a space reference based on a space name and a parent wiki reference.
     *
     * @param spaceName the name of the space
     * @param parent the wiki reference
     */
    public SpaceReference(String spaceName, WikiReference parent)
    {
        this(spaceName, (EntityReference) parent);
    }

    /**
     * Create a space reference based on a space name and a parent space reference.
     *
     * @param spaceName the name of the space
     * @param parent the space reference
     */
    public SpaceReference(String spaceName, SpaceReference parent)
    {
        this(spaceName, (EntityReference) parent);
    }

    /**
     * Create a space reference based on a space name and a parent entity reference. The entity reference may be either
     * a wiki or a space reference.
     *
     * @param spaceName the name of the space
     * @param parent the entity reference
     */
    public SpaceReference(String spaceName, EntityReference parent)
    {
        super(spaceName, EntityType.SPACE, parent);
    }

    /**
     * Create a space reference based on a space name and a parent space reference.
     *
     * @param wikiName the name of the wiki
     * @param spaceNames the spaces names
     * @since 7.4M1
     */
    public SpaceReference(String wikiName, String... spaceNames)
    {
        this(wikiName, Arrays.<String>asList(spaceNames));
    }

    /**
     * Create a space reference based on a space name and a parent space reference.
     *
     * @param wikiName the name of the wiki
     * @param spaceNames the spaces names
     * @since 7.4M1
     */
    public SpaceReference(String wikiName, List<String> spaceNames)
    {
        this(spaceNames.get(spaceNames.size() - 1), spaceNames.size() > 1
            ? new SpaceReference(wikiName, spaceNames.subList(0, spaceNames.size() - 1)) : new WikiReference(wikiName));
    }

    /**
     * Clone an SpaceReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public SpaceReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to verify the validity of the passed parent.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setParent(EntityReference)
     * @exception IllegalArgumentException if the passed parent is not a valid space reference parent (ie either a space
     *                reference or a wiki reference)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent instanceof SpaceReference || parent instanceof WikiReference) {
            super.setParent(parent);
            return;
        }

        if (parent == null || (parent.getType() != EntityType.SPACE && parent.getType() != EntityType.WIKI)) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] in a space reference");
        }

        if (parent.getType() == EntityType.SPACE) {
            super.setParent(new SpaceReference(parent));
        } else {
            super.setParent(new WikiReference(parent));
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to verify the validity of the passed type.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     * @exception IllegalArgumentException if the passed type is not a space type
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.SPACE) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a space reference");
        }

        super.setType(EntityType.SPACE);
    }

    @Override
    public SpaceReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (newParent == oldParent) {
            return this;
        }

        return new SpaceReference(this, oldParent, newParent);
    }

    @Override
    public SpaceReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new SpaceReference(this, newParent);
    }

    /**
     * @return the reference of the wiki containing this space
     * @since 7.1M2
     */
    @Transient
    public WikiReference getWikiReference()
    {
        return (WikiReference) extractReference(EntityType.WIKI);
    }
}
