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
 * Reference to an object in a document (by classname and index, document, space, wiki).
 * 
 * @since 2.3M1
 * @version $Id$
 */
public class ObjectReference extends EntityReference
{
    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     * 
     * @param reference the raw reference to build this object reference from
     */
    public ObjectReference(EntityReference reference)
    {
        super(reference.getName(), reference.getType(), reference.getParent());
    }

    /**
     * @param wiki wiki where the parent document of the object is
     * @param space space where the parent document of the object is
     * @param document parent document of the object
     * @param objectName the name of the object
     */
    public ObjectReference(String wiki, String space, String document, String objectName)
    {
        this(objectName, new DocumentReference(wiki, space, document));
    }

    /**
     * @param objectName the name of the object
     * @param documentReference the reference of the parent document of the object
     */
    public ObjectReference(String objectName, DocumentReference documentReference)
    {
        super(objectName, EntityType.OBJECT, documentReference);
    }

    /**
     * {@inheritDoc} <br />
     * Overridden to check the type to be an object type.
     * 
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     */
    @Override
    public void setType(EntityType type)
    {
        if (type != EntityType.OBJECT) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for an object reference");
        }

        super.setType(EntityType.OBJECT);
    }

    /**
     * {@inheritDoc} <br />
     * Overridden to ensure that the parent of an object is always a document.
     * 
     * @see org.xwiki.model.reference.EntityReference#setParent(org.xwiki.model.reference.EntityReference)
     */
    @Override
    public void setParent(EntityReference parent)
    {
        if (parent == null || parent.getType() != EntityType.DOCUMENT) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] for an object reference");
        }

        super.setParent(new DocumentReference(parent));
    }
}
