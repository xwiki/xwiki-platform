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
 * References a property in an object in a document (the value of the property).
 * 
 * @since 2.3M1
 * @version $Id$
 */
public class ObjectPropertyReference extends EntityReference
{
    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     * 
     * @param reference the raw reference to build this object reference from
     */
    public ObjectPropertyReference(EntityReference reference)
    {
        super(reference.getName(), reference.getType(), reference.getParent());
    }

    /**
     * Builds a property reference for the passed property in the passed object.
     * 
     * @param propertyName the name of the property to create reference for
     * @param objectReference the reference to the object whose property is
     */
    public ObjectPropertyReference(String propertyName, ObjectReference objectReference)
    {
        super(propertyName, EntityType.OBJECT_PROPERTY, objectReference);
    }

    /**
     * @param wiki the wiki of the document where the parent object of this property is
     * @param space the space of the document where the parent object of this property is
     * @param page the document where the parent object of this property is
     * @param objectName the name of the parent object of this property
     * @param propertyName the name of the property to refer to
     */
    public ObjectPropertyReference(String wiki, String space, String page, String objectName, String propertyName)
    {
        this(propertyName, new ObjectReference(wiki, space, page, objectName));
    }

    /**
     * {@inheritDoc} <br />
     * Overridden to check the type to be a property type.
     * 
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     */
    @Override
    public void setType(EntityType type)
    {
        if (type != EntityType.OBJECT_PROPERTY) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for an object property reference");
        }

        super.setType(EntityType.OBJECT_PROPERTY);
    }

    /**
     * {@inheritDoc} <br />
     * Overridden to ensure that the parent of a property is always an object.
     * 
     * @see org.xwiki.model.reference.EntityReference#setParent(org.xwiki.model.reference.EntityReference)
     */
    @Override
    public void setParent(EntityReference parent)
    {
        if (parent == null || parent.getType() != EntityType.OBJECT) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] for an object property "
                + "reference");
        }

        super.setParent(new ObjectReference(parent));
    }
}
