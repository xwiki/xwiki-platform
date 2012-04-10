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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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
     * The "default" component role hint.
     */
    private static final String DEFAULT_COMPONENT_HINT = "default";

    /**
     * Name of the target document referencee.
     */
    private DocumentReference documentReference;

    /**
     * Name of the parent document refernce.
     */
    private DocumentReference parentReference;

    /**
     * Component manager used to lookup for various name serializers.
     */
    private ComponentManager componentManager;

    /**
     * Creates a new {@link TargetDocumentDescriptor} instance.
     * 
     * @param documentReference reference of the target document
     * @param componentManager used to lookup for various name serializers.
     */
    public TargetDocumentDescriptor(DocumentReference documentReference, ComponentManager componentManager)
    {
        this.documentReference = documentReference;
        this.componentManager = componentManager;
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
        return serializeDocumentRefefence(getDocumentReference(), DEFAULT_COMPONENT_HINT);
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
        return (null != getParentReference()) ? serializeDocumentRefefence(
            getParentReference(), DEFAULT_COMPONENT_HINT) : null;
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

    /**
     * Utility method for serializing a {@link org.xwiki.model.reference.DocumentReference}.
     * 
     * @param documentReference document reference
     * @param serializerHint which serializer to use.
     * @return string representation of a document reference
     */
    private String serializeDocumentRefefence(DocumentReference documentReference, String serializerHint)
    {
        try {
            EntityReferenceSerializer<String> serializer =
                this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, serializerHint);
            return serializer.serialize(documentReference);
        } catch (ComponentLookupException ex) {
            // TODO: put a descriptive comment.
        }
        return null;
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
