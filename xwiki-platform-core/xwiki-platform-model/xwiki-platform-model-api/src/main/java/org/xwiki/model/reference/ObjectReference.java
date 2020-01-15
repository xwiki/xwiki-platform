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
        super(reference);
    }

    /**
     * Clone an ObjectReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     * @since 3.3M2
     */
    protected ObjectReference(EntityReference reference, EntityReference oldReference, EntityReference newReference)
    {
        super(reference, oldReference, newReference);
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
     * Clone an ObjectReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public ObjectReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * Deprecated constructor.
     * 
     * @param wiki wiki where the parent document of the object is
     * @param space space where the parent document of the object is
     * @param document parent document of the object
     * @param objectName the name of the object
     */
    @Deprecated
    public ObjectReference(String wiki, String space, String document, String objectName)
    {
        this(objectName, new DocumentReference(wiki, space, document));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to check the type to be an object type.
     * </p>
     * 
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.OBJECT) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for an object reference");
        }

        super.setType(EntityType.OBJECT);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to ensure that the parent of an object is always a document.
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
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] in an object reference");
        }

        super.setParent(new DocumentReference(parent));
    }

    @Override
    public ObjectReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (newParent == oldParent) {
            return this;
        }

        return new ObjectReference(this, oldParent, newParent);
    }

    @Override
    public ObjectReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new ObjectReference(this, newParent);
    }

    /**
     * @return the reference of the document holding this object
     * @since 9.8RC1
     */
    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) getParent();
    }
}
