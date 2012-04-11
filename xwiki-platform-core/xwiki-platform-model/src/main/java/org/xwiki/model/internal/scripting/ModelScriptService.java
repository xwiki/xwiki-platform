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
package org.xwiki.model.internal.scripting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.script.service.ScriptService;

/**
 * Provides Model-specific Scripting APIs.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("model")
@Singleton
public class ModelScriptService implements ScriptService
{
    /**
     * The default hint used when resolving references.
     */
    private static final String DEFAULT_RESOLVER_HINT = "currentmixed";
    
    /**
     * The object used to log messages.
     */
    @Inject
    private Logger logger;

    /**
     * Used to dynamically look up component implementations based on a given hint.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Create a Document Reference from a passed wiki, space and page names, which can be empty strings or {@code null}
     * in which case they are resolved using the {@value #DEFAULT_RESOLVER_HINT} resolver.
     * 
     * @param wiki the wiki reference name to use (can be empty or null)
     * @param space the space reference name to use (can be empty or null)
     * @param page the page reference name to use (can be empty or null)
     * @return the typed Document Reference object or null if no Resolver with the passed hint could be found
     * @since 2.3M2
     */
    public DocumentReference createDocumentReference(String wiki, String space, String page)
    {
        return createDocumentReference(wiki, space, page, DEFAULT_RESOLVER_HINT);
    }

    /**
     * Create a Document Reference from a passed wiki, space and page names, which can be empty strings or null in which
     * case they are resolved against the Resolver having the hint passed as parameter. Valid hints are for example
     * "default", "current", "currentmixed".
     * 
     * @param wiki the wiki reference name to use (can be empty or null)
     * @param space the space reference name to use (can be empty or null)
     * @param page the page reference name to use (can be empty or null)
     * @param hint the hint of the Resolver to use in case any parameter is empty or null
     * @return the typed Document Reference object or null if no Resolver with the passed hint could be found
     */
    public DocumentReference createDocumentReference(String wiki, String space, String page, String hint)
    {
        EntityReference reference = null;
        if (!StringUtils.isEmpty(wiki)) {
            reference = new EntityReference(wiki, EntityType.WIKI);
        }
        if (!StringUtils.isEmpty(space)) {
            reference = new EntityReference(space, EntityType.SPACE, reference);
        }
        if (!StringUtils.isEmpty(page)) {
            reference = new EntityReference(page, EntityType.DOCUMENT, reference);
        }

        DocumentReference documentReference;
        try {
            DocumentReferenceResolver<EntityReference> resolver =
                this.componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, hint);
            documentReference = resolver.resolve(reference);
        } catch (ComponentLookupException e) {
            try {
                // Ensure backward compatibility with older scripts that use hints like "default/reference" because at
                // the time they were written we didn't have support for generic types in component role.
                DocumentReferenceResolver drr =
                    this.componentManager.getInstance(DocumentReferenceResolver.class, hint);
                documentReference = drr.resolve(reference);
                logger.warn("Deprecated usage of DocumentReferenceResolver with hint [{}]. "
                    + "Please consider using a DocumentReferenceResolver that takes into account generic types.", hint);
            } catch (ComponentLookupException ex) {
                documentReference = null;
            }
        }
        return documentReference;
    }

    /**
     * Creates an {@link AttachmentReference} from a file name and a reference to the document holding that file.
     * 
     * @param documentReference a reference to the document the file is attached to
     * @param fileName the name of a file attached to a document
     * @return a reference to the specified attachment
     * @since 2.5M2
     */
    public AttachmentReference createAttachmentReference(DocumentReference documentReference, String fileName)
    {
        return new AttachmentReference(fileName, documentReference);
    }

    /**
     * @param stringRepresentation the document reference specified as a String (using the "wiki:space.page" format and
     *            with special characters escaped where required)
     * @return the typed Document Reference object (resolved using the {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 2.3M2
     */
    public DocumentReference resolveDocument(String stringRepresentation)
    {
        return resolveDocument(stringRepresentation, DEFAULT_RESOLVER_HINT);
    }

    /**
     * @param stringRepresentation the document reference specified as a String (using the "wiki:space.page" format and
     *            with special characters escaped where required)
     * @param hint the hint of the Resolver to use in case any part of the reference is missing (no wiki specified, no
     *            space or no page)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve a document
     *            reference relative to another entity reference
     * @return the typed Document Reference object or null if no Resolver with the passed hint could be found
     */
    public DocumentReference resolveDocument(String stringRepresentation, String hint, Object... parameters)
    {
        try {
            EntityReferenceResolver<String> resolver =
                this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, hint);
            return new DocumentReference(resolver.resolve(stringRepresentation, EntityType.DOCUMENT, parameters));
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    /**
     * @param stringRepresentation an attachment reference specified as {@link String} (using the "wiki:space.page@file"
     *            format and with special characters escaped where required)
     * @return the corresponding typed {@link AttachmentReference} object (resolved using the
     *         {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 2.5M2
     */
    public AttachmentReference resolveAttachment(String stringRepresentation)
    {
        return resolveAttachment(stringRepresentation, DEFAULT_RESOLVER_HINT);
    }

    /**
     * @param stringRepresentation an attachment reference specified as {@link String} (using the "wiki:space.page@file"
     *            format and with special characters escaped where required)
     * @param hint the hint of the resolver to use in case any part of the reference is missing (no wiki specified, no
     *            space or no page)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve an attachment
     *            reference relative to another entity reference
     * @return the corresponding typed {@link AttachmentReference} object
     * @since 2.5M2
     */
    public AttachmentReference resolveAttachment(String stringRepresentation, String hint, Object... parameters)
    {
        try {
            EntityReferenceResolver<String> resolver =
                this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, hint);
            return new AttachmentReference(resolver.resolve(stringRepresentation, EntityType.ATTACHMENT, parameters));
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    /**
     * @param stringRepresentation an object reference specified as {@link String} (using the "wiki:space.page^object"
     *            format and with special characters escaped where required)
     * @return the corresponding typed {@link ObjectReference} object (resolved using the
     *         {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 3.2M3
     */
    public ObjectReference resolveObject(String stringRepresentation)
    {
        return resolveObject(stringRepresentation, DEFAULT_RESOLVER_HINT);
    }
    
    /**
     * @param stringRepresentation an object reference specified as {@link String} (using the "wiki:space.page^object"
     *            format and with special characters escaped where required)
     * @param hint the hint of the resolver to use in case any part of the reference is missing (no wiki specified, no
     *            space or no page)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve an object
     *            reference relative to another entity reference
     * @return the corresponding typed {@link ObjectReference} object
     * @since 3.2M3
     */
    public ObjectReference resolveObject(String stringRepresentation, String hint, Object... parameters)
    {
        try {
            EntityReferenceResolver<String> resolver =
                this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, hint);
            return new ObjectReference(resolver.resolve(stringRepresentation, EntityType.OBJECT, parameters));
        } catch (ComponentLookupException e) {
            return null;
        }
    }
    
    /**
     * @param stringRepresentation an object property reference specified as {@link String} (using the 
     *            "wiki:space.page^object.property" format and with special characters escaped where required)
     * @return the corresponding typed {@link ObjectReference} object (resolved using the
     *         {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 3.2M3
     */
    public ObjectPropertyReference resolveObjectProperty(String stringRepresentation)
    {
        return resolveObjectProperty(stringRepresentation, DEFAULT_RESOLVER_HINT);
    }
    
    /**
     * @param stringRepresentation an object property reference specified as {@link String} (using the
     *            "wiki:space.page^object.property" format and with special characters escaped where required)
     * @param hint the hint of the resolver to use in case any part of the reference is missing (no wiki specified, no
     *            space or no page)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve an object
     *            property reference relative to another entity reference
     * @return the corresponding typed {@link ObjectReference} object
     * @since 3.2M3
     */
    public ObjectPropertyReference resolveObjectProperty(String stringRepresentation, String hint, Object... parameters)
    {
        try {
            EntityReferenceResolver<String> resolver =
                this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, hint);
            return new ObjectPropertyReference(resolver.resolve(stringRepresentation, EntityType.OBJECT_PROPERTY,
                parameters));
        } catch (ComponentLookupException e) {
            return null;
        }
    }
    
    /**
     * @param reference the entity reference to transform into a String representation
     * @return the string representation of the passed entity reference (using the "compact" serializer)
     * @since 2.3M2
     */
    public String serialize(EntityReference reference)
    {
        return serialize(reference, "compact");
    }

    /**
     * @param reference the entity reference to transform into a String representation
     * @param hint the hint of the Serializer to use (valid hints are for example "default", "compact", "local")
     * @return the string representation of the passed entity reference
     */
    public String serialize(EntityReference reference, String hint)
    {
        String result;
        try {
            EntityReferenceSerializer<String> serializer =
                this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, hint);
            result = serializer.serialize(reference);
        } catch (ComponentLookupException e) {
            result = null;
        }
        return result;
    }
}
