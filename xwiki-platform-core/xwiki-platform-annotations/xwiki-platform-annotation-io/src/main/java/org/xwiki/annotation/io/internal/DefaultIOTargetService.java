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
package org.xwiki.annotation.io.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.annotation.io.IOServiceException;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.annotation.reference.IndexedObjectReference;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Default {@link IOTargetService} implementation, based on resolving XWiki documents and object properties as
 * annotations targets. The references manipulated by this implementation are XWiki references, such as xwiki:Space.Page
 * for documents or with an object and property reference if the target is an object property. Use the reference module
 * to generate the references passed to this module, so that they can be resolved to XWiki content back by this
 * implementation.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultIOTargetService implements IOTargetService
{
    /**
     * Document access bridge to manipulate xwiki documents.
     */
    @Inject
    private DocumentAccessBridge dab;

    /**
     * Entity reference handler to resolve the reference.
     */
    @Inject
    private TypedStringEntityReferenceResolver referenceResolver;

    /**
     * Default entity reference serializer.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getSource(String reference) throws IOServiceException
    {
        try {
            EntityReference ref = referenceResolver.resolve(reference, EntityType.DOCUMENT);
            if (ref.getType() == EntityType.OBJECT_PROPERTY) {
                EntityReference docRef = ref.extractReference(EntityType.DOCUMENT);
                // handle this as a reference to an object, parse an indexed object out of this property's parent
                IndexedObjectReference objRef = new IndexedObjectReference(ref.getParent());
                if (objRef.getObjectNumber() != null) {
                    return dab.getProperty(serializer.serialize(docRef), objRef.getClassName(),
                        objRef.getObjectNumber(), ref.getName()).toString();
                } else {
                    return dab.getProperty(serializer.serialize(docRef), objRef.getClassName(), ref.getName())
                        .toString();
                }
            } else if (ref.getType() == EntityType.DOCUMENT) {
                return dab.getDocumentContent(serializer.serialize(ref));
            } else {
                // it was parsed as something else, just ignore the parsing and get the document content as its initial
                // name was
                return dab.getDocumentContent(reference);
            }
        } catch (Exception e) {
            throw new IOServiceException("An exception has occurred while getting the source for " + reference, e);
        }
    }

    @Override
    public String getSourceSyntax(String reference) throws IOServiceException
    {
        try {
            EntityReference ref = referenceResolver.resolve(reference, EntityType.DOCUMENT);
            EntityReference docRef = ref.extractReference(EntityType.DOCUMENT);
            if (docRef != null) {
                // return the syntax of the document in this reference, regardless of the type of reference, obj prop or
                // doc
                return dab.getDocumentSyntaxId(serializer.serialize(docRef));
            } else {
                return dab.getDocumentSyntaxId(reference);
            }
        } catch (Exception e) {
            throw new IOServiceException("An exception has occurred while getting the syntax of the source for "
                + reference, e);
        }
    }
}
