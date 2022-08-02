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
 * Reference to a block, a structured part of the content of a document or an object property.
 *
 * While other references are generally unique, defined application wide, and usable to reach their target instance,
 * the meaning of block references depends on their usage, may not be unique and are not necessarily a way to reach
 * the referenced instance. We may have different kind of block references, for different purposes (for example
 * identifying a header in the content, linking signature to macro block, etc...).
 *
 * @version $Id$
 * @since 6.0M1
 */
public class BlockReference extends EntityReference
{
    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     *
     * @param reference the raw reference to build this block reference from
     */
    public BlockReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone an BlockReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     */
    protected BlockReference(EntityReference reference, EntityReference oldReference, EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * @param blockName the name of the block
     */
    public BlockReference(String blockName)
    {
        super(blockName, EntityType.BLOCK);
    }

    /**
     * @param blockName the name of the block
     * @param documentReference the reference of the parent document of the block
     */
    public BlockReference(String blockName, DocumentReference documentReference)
    {
        super(blockName, EntityType.BLOCK, documentReference);
    }

    /**
     * @param blockName the name of the block
     * @param objectPropertyReference the reference of the parent object property of the block
     */
    public BlockReference(String blockName, ObjectPropertyReference objectPropertyReference)
    {
        super(blockName, EntityType.BLOCK, objectPropertyReference);
    }

    /**
     * Clone an BlockReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public BlockReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to check the type to be an block type.
     * </p>
     * 
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.BLOCK) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a block reference");
        }

        super.setType(EntityType.BLOCK);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to ensure that the parent of a block is either a document or an object property.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setParent(org.xwiki.model.reference.EntityReference)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent == null || parent instanceof DocumentReference || parent instanceof ObjectPropertyReference) {
            super.setParent(parent);
            return;
        }

        if ((parent.getType() != EntityType.DOCUMENT && parent.getType() != EntityType.OBJECT_PROPERTY)) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] in a block reference");
        }

        if (parent.getType() == EntityType.DOCUMENT) {
            super.setParent(new DocumentReference(parent));
        } else {
            super.setParent(new ObjectPropertyReference(parent));
        }
    }

    @Override
    public BlockReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (newParent == oldParent) {
            return this;
        }

        return new BlockReference(this, oldParent, newParent);
    }

    @Override
    public BlockReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new BlockReference(this, newParent);
    }
}
