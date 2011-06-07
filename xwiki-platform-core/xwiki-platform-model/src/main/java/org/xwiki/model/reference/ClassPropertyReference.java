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
 * References a property in a class in a document (the description of the property).
 * 
 * @version $Id$
 * @since 3.2M1
 */
public class ClassPropertyReference extends EntityReference
{
    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     * 
     * @param reference the raw reference to build this object reference from
     */
    public ClassPropertyReference(EntityReference reference)
    {
        super(reference.getName(), reference.getType(), reference.getParent());
    }

    /**
     * Builds a property reference for the passed property in the passed object.
     * 
     * @param propertyName the name of the property to create reference for
     * @param classReference the reference to the class whose property is
     */
    public ClassPropertyReference(String propertyName, DocumentReference classReference)
    {
        super(propertyName, EntityType.CLASS_PROPERTY, classReference);
    }

    /**
     * @param wiki the wiki of the document where the parent class of this property is
     * @param space the space of the document where the parent class of this property is
     * @param page the document where the parent class of this property is
     * @param objectName the name of the parent class of this property
     * @param propertyName the name of the property to refer to
     */
    public ClassPropertyReference(String wiki, String space, String page, String propertyName)
    {
        this(propertyName, new DocumentReference(wiki, space, page));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to check the type to be a property type.
     * 
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     */
    @Override
    public void setType(EntityType type)
    {
        if (type != EntityType.CLASS_PROPERTY) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for an class property reference");
        }

        super.setType(EntityType.CLASS_PROPERTY);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to ensure that the parent of a property is always an object.
     * 
     * @see org.xwiki.model.reference.EntityReference#setParent(org.xwiki.model.reference.EntityReference)
     */
    @Override
    public void setParent(EntityReference parent)
    {
        if (parent == null || parent.getType() != EntityType.DOCUMENT) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] for an class property "
                + "reference");
        }

        super.setParent(new DocumentReference(parent));
    }
}
