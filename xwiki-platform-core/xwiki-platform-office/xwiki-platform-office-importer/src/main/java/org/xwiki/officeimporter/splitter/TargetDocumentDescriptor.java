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
package org.xwiki.officeimporter.splitter;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Descriptor for specifying a reference to the document into which an office document is going to be saved.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class TargetDocumentDescriptor
{
    /**
     * Target document reference.
     */
    private final DocumentReference documentReference;

    /**
     * The object used to serialize entity references.
     */
    private EntityReferenceSerializer<String> serializer;

    /**
     * Parent document reference.
     */
    private DocumentReference parentReference;

    /**
     * Creates a new {@link TargetDocumentDescriptor} instance.
     * 
     * @param documentReference reference of the target document
     * @param componentManager used to lookup the entity reference serializer
     */
    public TargetDocumentDescriptor(DocumentReference documentReference, ComponentManager componentManager)
    {
        this.documentReference = documentReference;
        try {
            this.serializer = componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        } catch (ComponentLookupException e) {
            // Shouldn't happen.
            this.serializer = null;
        }
    }

    /**
     * @return target document reference name.
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     * @return target document reference as a string.
     */
    public String getDocumentReferenceAsString()
    {
        return serializer.serialize(getDocumentReference());
    }

    /**
     * @return target parent document reference
     */
    public DocumentReference getParentReference()
    {
        return this.parentReference;
    }

    /**
     * @return name of the parent document reference
     */
    public String getParentReferenceAsString()
    {
        return (null != getParentReference()) ? serializer.serialize(getParentReference()) : null;
    }

    /**
     * Sets the name of the parent document reference.
     * 
     * @param parentReference parent document reference
     */
    public void setParentReference(DocumentReference parentReference)
    {
        this.parentReference = parentReference;
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equals = false;
        if (obj instanceof TargetDocumentDescriptor) {
            TargetDocumentDescriptor other = (TargetDocumentDescriptor) obj;
            equals = other.getDocumentReference().equals(getDocumentReference());
        }
        return equals;
    }

    @Override
    public int hashCode()
    {
        return getDocumentReference().hashCode();
    }
}
