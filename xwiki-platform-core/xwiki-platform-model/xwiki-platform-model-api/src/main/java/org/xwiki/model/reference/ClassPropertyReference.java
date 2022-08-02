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
        super(reference);
    }

    /**
     * Clone an ClassPropertyReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     * @since 3.3M2
     */
    protected ClassPropertyReference(EntityReference reference, EntityReference oldReference,
        EntityReference newReference)
    {
        super(reference, oldReference, newReference);
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
     * Clone an ClassPropertyReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public ClassPropertyReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * Deprecated constructor.
     * 
     * @param wiki the wiki of the document where the parent class of this property is
     * @param space the space of the document where the parent class of this property is
     * @param page the document where the parent class of this property is
     * @param propertyName the name of the property to refer to
     */
    @Deprecated
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
    protected void setType(EntityType type)
    {
        if (type != EntityType.CLASS_PROPERTY) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a class property reference");
        }

        super.setType(EntityType.CLASS_PROPERTY);
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
        if (parent instanceof DocumentReference) {
            super.setParent(parent);
            return;
        }

        if (parent == null || parent.getType() != EntityType.DOCUMENT) {
            throw new IllegalArgumentException(
                "Invalid parent reference [" + parent + "] in a class property reference");
        }

        super.setParent(new DocumentReference(parent));
    }

    @Override
    public ClassPropertyReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (newParent == oldParent) {
            return this;
        }

        return new ClassPropertyReference(this, oldParent, newParent);
    }

    @Override
    public ClassPropertyReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new ClassPropertyReference(this, newParent);
    }
}
