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
import org.xwiki.stability.Unstable;

/**
 * References a property in an object in a document (the value of the property).
 * 
 * @version $Id$
 * @since 10.6RC1
 */
@Unstable
public class PageObjectPropertyReference extends EntityReference
{
    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     * 
     * @param reference the raw reference to build this object reference from
     */
    public PageObjectPropertyReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone an ObjectPropertyReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     */
    protected PageObjectPropertyReference(EntityReference reference, EntityReference oldReference,
        EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * Builds a property reference for the passed property in the passed object.
     * 
     * @param propertyName the name of the property to create reference for
     * @param objectReference the reference to the object whose property is
     */
    public PageObjectPropertyReference(String propertyName, PageObjectReference objectReference)
    {
        super(propertyName, EntityType.PAGE_OBJECT_PROPERTY, objectReference);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to check the type to be a property type.
     * </p>
     * 
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.PAGE_OBJECT_PROPERTY) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for an object property reference");
        }

        super.setType(type);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to ensure that the parent of a property is always an object.
     * </p>
     * 
     * @see org.xwiki.model.reference.EntityReference#setParent(org.xwiki.model.reference.EntityReference)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent instanceof ObjectReference) {
            super.setParent(parent);

            return;
        }

        if (parent == null || parent.getType() != EntityType.PAGE_OBJECT) {
            throw new IllegalArgumentException(
                "Invalid parent reference [" + parent + "] in an object property reference");
        }

        super.setParent(new PageObjectReference(parent));
    }

    @Override
    public PageObjectPropertyReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        return new PageObjectPropertyReference(this, oldParent, newParent);
    }
}
