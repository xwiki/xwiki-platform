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
package org.xwiki.lesscss.internal.resources;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.WikiLESSResourceReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * A reference to a LESS resource containing in an XObject property in the wiki.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class LESSObjectPropertyResourceReference implements WikiLESSResourceReference
{
    private ObjectPropertyReference objectPropertyReference;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private DocumentAccessBridge bridge;

    private final UserReferenceSerializer<DocumentReference> userReferenceDocumentReferenceSerializer;

    /**
     * Constructor.
     *
     * @param objectPropertyReference reference to the property of an XObject storing some LESS code
     * @param entityReferenceSerializer object reference serializer
     * @param bridge bridge to access to documents
     * @param userReferenceDocumentReferenceSerializer a document reference serializer
     */
    public LESSObjectPropertyResourceReference(ObjectPropertyReference objectPropertyReference,
        EntityReferenceSerializer<String> entityReferenceSerializer, DocumentAccessBridge bridge,
        UserReferenceSerializer<DocumentReference> userReferenceDocumentReferenceSerializer)
    {
        this.objectPropertyReference = objectPropertyReference;
        this.entityReferenceSerializer = entityReferenceSerializer;
        this.bridge = bridge;
        this.userReferenceDocumentReferenceSerializer = userReferenceDocumentReferenceSerializer;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof LESSObjectPropertyResourceReference) {
            LESSObjectPropertyResourceReference lessObjectPropertyResourceReference =
                (LESSObjectPropertyResourceReference) o;
            return objectPropertyReference.equals(lessObjectPropertyResourceReference.objectPropertyReference);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return objectPropertyReference.hashCode();
    }

    @Override
    public String getContent(String skin) throws LESSCompilerException
    {
        return (String) bridge.getProperty(objectPropertyReference);
    }

    @Override
    public String serialize()
    {
        return String.format("LessXObjectProperty[%s]", entityReferenceSerializer.serialize(objectPropertyReference));
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.bridge.getCurrentDocumentReference();
    }

    @Override
    public DocumentReference getAuthorReference() throws LESSCompilerException
    {
        var documentReference = this.getDocumentReference();
        try {
            var documentInstance = this.bridge.getDocumentInstance(documentReference);
            var originalMetadataAuthor = documentInstance.getAuthors().getEffectiveMetadataAuthor();
            return this.userReferenceDocumentReferenceSerializer.serialize(originalMetadataAuthor);
        } catch (Exception e) {
            throw new LESSCompilerException(
                "Failed to get the document from document reference [" + documentReference + "]", e);
        }
    }
}
