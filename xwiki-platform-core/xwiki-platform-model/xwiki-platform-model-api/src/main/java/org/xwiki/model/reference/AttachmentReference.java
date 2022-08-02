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

import org.xwiki.model.EntityType;

/**
 * Represents a reference to an Attachment (document reference and file name). Note that an attachment is always
 * attached to a document.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class AttachmentReference extends EntityReference
{
    /**
     * Special constructor that transforms a generic entity reference into an {@link AttachmentReference}. It checks the
     * validity of the passed reference (ie correct type and correct parent).
     *
     * @param reference the reference to be transformed
     * @exception IllegalArgumentException if the passed reference is not a valid attachment reference
     */
    public AttachmentReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone an AttachmentReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     * @since 3.3M2
     */
    protected AttachmentReference(EntityReference reference, EntityReference oldReference, EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * Clone an AttachmentReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public AttachmentReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * Create a new attachment reference based on the attachment name and the parent document reference.
     *
     * @param fileName the name of the attachment
     * @param parent the reference of the document
     */
    public AttachmentReference(String fileName, DocumentReference parent)
    {
        super(fileName, EntityType.ATTACHMENT, parent);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden in order to verify the validity of the passed parent.
     *
     * @exception IllegalArgumentException if the passed parent is not a valid attachment reference parent (ie an
     *            attachment reference)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent instanceof DocumentReference) {
            super.setParent(parent);
            return;
        }

        if (parent == null || parent.getType() != EntityType.DOCUMENT) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] in an attachment reference");
        }

        super.setParent(new DocumentReference(parent));
    }

    /**
     * {@inheritDoc}
     *
     * Overridden in order to verify the validity of the passed type.
     *
     * @exception IllegalArgumentException if the passed type is not an attachment type
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.ATTACHMENT) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for an attachment reference");
        }

        super.setType(EntityType.ATTACHMENT);
    }

    /**
     * @return the document reference contained in this attachment reference
     */
    @Transient
    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) extractReference(EntityType.DOCUMENT);
    }

    @Override
    public AttachmentReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (newParent == oldParent) {
            return this;
        }

        return new AttachmentReference(this, oldParent, newParent);
    }

    @Override
    public AttachmentReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new AttachmentReference(this, newParent);
    }
}
