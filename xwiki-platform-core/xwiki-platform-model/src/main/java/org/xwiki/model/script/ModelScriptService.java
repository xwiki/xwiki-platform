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
package org.xwiki.model.script;

import java.util.Locale;

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
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceTree;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
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
    private static final String DEFAULT_RESOLVER_HINT = "current";

    /**
     * The default hint used when serializing references.
     */
    private static final String DEFAULT_SERIALIZER_HINT = "compact";

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
     * Create a new reference with the passed {@link Locale}.
     * 
     * @param reference the reference (with or without locale)
     * @param locale the locale of the new reference
     * @return the typed Document Reference object or null if no Resolver with the passed hint could be found
     * @since 5.4RC1
     */
    public DocumentReference createDocumentReference(DocumentReference reference, Locale locale)
    {
        return new DocumentReference(reference, locale);
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
                DocumentReferenceResolver<EntityReference> drr =
                    this.componentManager.getInstance(DocumentReferenceResolver.class, hint);
                documentReference = drr.resolve(reference);
                this.logger.warn("Deprecated usage of DocumentReferenceResolver with hint [{}]. "
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
     * Creates a {@link WikiReference} from a string representing the wiki name.
     *
     * @param wikiName the wiki name (eg "xwiki")
     * @return the reference to the wiki
     * @since 5.0M1
     */
    public WikiReference createWikiReference(String wikiName)
    {
        return new WikiReference(wikiName);
    }

    /**
     * Creates a {@link SpaceReference} from a string representing the space name.
     *
     * @param spaceName the space name (eg "Main")
     * @param parent the wiki reference in which the space is located
     * @return the reference to the space
     * @since 5.0M1
     */
    public SpaceReference createSpaceReference(String spaceName, WikiReference parent)
    {
        return new SpaceReference(spaceName, parent);
    }

    /**
     * Creates any {@link EntityReference} from a string.
     *
     * @param name the entity reference name (eg "page")
     * @param type the entity type (eg "wiki", "space", "document", etc)
     * @return the created reference
     * @since 5.0M1
     */
    public EntityReference createEntityReference(String name, EntityType type)
    {
        return new EntityReference(name, type);
    }

    /**
     * Creates any {@link EntityReference} from a string.
     *
     * @param name the entity reference name (eg "page")
     * @param type the entity type (eg "wiki", "space", "document", etc)
     * @param parent the entity parent
     * @return the created reference
     * @since 5.0M1
     */
    public EntityReference createEntityReference(String name, EntityType type, EntityReference parent)
    {
        return new EntityReference(name, type, parent);
    }

    /**
     * @param stringRepresentation the space reference specified as a String (using the "wiki:space" format and with
     *            special characters escaped where required)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve a space
     *            reference relative to another entity reference
     * @return the typed Space Reference object (resolved using the {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 5.0M1
     */
    public SpaceReference resolveSpace(String stringRepresentation, Object... parameters)
    {
        return resolveSpace(stringRepresentation, DEFAULT_RESOLVER_HINT, parameters);
    }

    /**
     * @param stringRepresentation the space reference specified as a String (using the "wiki:space" format and with
     *            special characters escaped where required)
     * @param hint the hint of the Resolver to use in case any part of the reference is missing (no wiki or no space
     *            specified)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve a space
     *            reference relative to another entity reference
     * @return the typed Space Reference object or null if no Resolver with the passed hint could be found
     * @since 5.0M1
     */
    public SpaceReference resolveSpace(String stringRepresentation, String hint, Object... parameters)
    {
        try {
            EntityReferenceResolver<String> resolver =
                this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, hint);
            return new SpaceReference(resolver.resolve(stringRepresentation, EntityType.SPACE, parameters));
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    /**
     * @param stringRepresentation the document reference specified as a String (using the "wiki:space.page" format and
     *            with special characters escaped where required)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve a document
     *            reference relative to another entity reference
     * @return the typed Document Reference object (resolved using the {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 2.3M2
     */
    public DocumentReference resolveDocument(String stringRepresentation, Object... parameters)
    {
        return resolveDocument(stringRepresentation, DEFAULT_RESOLVER_HINT, parameters);
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
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve an attachment
     *            reference relative to another entity reference
     * @return the corresponding typed {@link AttachmentReference} object (resolved using the
     *         {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 2.5M2
     */
    public AttachmentReference resolveAttachment(String stringRepresentation, Object... parameters)
    {
        return resolveAttachment(stringRepresentation, DEFAULT_RESOLVER_HINT, parameters);
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
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve an object
     *            reference relative to another entity reference
     * @return the corresponding typed {@link ObjectReference} object (resolved using the
     *         {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 3.2M3
     */
    public ObjectReference resolveObject(String stringRepresentation, Object... parameters)
    {
        return resolveObject(stringRepresentation, DEFAULT_RESOLVER_HINT, parameters);
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
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve an object
     *            property reference relative to another entity reference
     * @return the corresponding typed {@link ObjectPropertyReference} object (resolved using the
     *         {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 3.2M3
     */
    public ObjectPropertyReference resolveObjectProperty(String stringRepresentation, Object... parameters)
    {
        return resolveObjectProperty(stringRepresentation, DEFAULT_RESOLVER_HINT, parameters);
    }

    /**
     * @param stringRepresentation an object property reference specified as {@link String} (using the
     *            "wiki:space.page^object.property" format and with special characters escaped where required)
     * @param hint the hint of the resolver to use in case any part of the reference is missing (no wiki specified, no
     *            space or no page)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve an object
     *            property reference relative to another entity reference
     * @return the corresponding typed {@link ObjectPropertyReference} object
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
     * @param stringRepresentation a class property reference specified as {@link String} (using the
     *            "wiki:Space.Class^property" format and with special characters escaped where required)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve a class
     *            property reference relative to another entity reference
     * @return the corresponding typed {@link ClassPropertyReference} object (resolved using the
     *         {@value #DEFAULT_RESOLVER_HINT} resolver)
     * @since 5.4.2, 6.0M1
     */
    public ClassPropertyReference resolveClassProperty(String stringRepresentation, Object... parameters)
    {
        return resolveClassProperty(stringRepresentation, DEFAULT_RESOLVER_HINT, parameters);
    }

    /**
     * @param stringRepresentation a class property reference specified as {@link String} (using the
     *            "wiki:Space.Class^property" format and with special characters escaped where required)
     * @param hint the hint of the resolver to use in case any part of the reference is missing (no wiki specified, no
     *            space or no page)
     * @param parameters extra parameters to pass to the resolver; you can use these parameters to resolve a class
     *            property reference relative to another entity reference
     * @return the corresponding typed {@link ClassPropertyReference} object
     * @since 5.4.2, 6.0M1
     */
    public ClassPropertyReference resolveClassProperty(String stringRepresentation, String hint, Object... parameters)
    {
        try {
            EntityReferenceResolver<String> resolver =
                this.componentManager.getInstance(EntityReferenceResolver.TYPE_STRING, hint);
            return new ClassPropertyReference(resolver.resolve(stringRepresentation, EntityType.CLASS_PROPERTY,
                parameters));
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    /**
     * @param reference the entity reference to transform into a String representation
     * @return the string representation of the passed entity reference (using the "compact" serializer)
     * @param parameters the optional extra parameters to pass to the Serializer; they are passed directly to
     *            {@link EntityReferenceSerializer#serialize(org.xwiki.model.reference.EntityReference, Object...)}
     * @since 2.3M2
     */
    public String serialize(EntityReference reference, Object... parameters)
    {
        return serialize(reference, DEFAULT_SERIALIZER_HINT, parameters);
    }

    /**
     * @param reference the entity reference to transform into a String representation
     * @param hint the hint of the Serializer to use (valid hints are for example "default", "compact", "local")
     * @param parameters the optional extra parameters to pass to the Serializer; they are passed directly to
     *            {@link EntityReferenceSerializer#serialize(org.xwiki.model.reference.EntityReference, Object...)}
     * @return the string representation of the passed entity reference
     */
    public String serialize(EntityReference reference, String hint, Object... parameters)
    {
        String result;
        try {
            EntityReferenceSerializer<String> serializer =
                this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, hint);
            result = serializer.serialize(reference, parameters);
        } catch (ComponentLookupException e) {
            result = null;
        }
        return result;
    }

    /**
     * Get the current value for a specific entity type, like the current space or wiki name. This doesn't return a
     * proper entity reference, but just the string value that should be used for that type of entity.
     * 
     * @param type the target entity type; from Velocity it's enough to use a string with the uppercase name of the
     *            entity, like {@code 'SPACE'}
     * @return the current value for the requested entity type
     * @since 4.3M1
     */
    public String getEntityReferenceValue(EntityType type)
    {
        return getEntityReferenceValue(type, DEFAULT_RESOLVER_HINT);
    }

    /**
     * Get the value configured for a specific entity type, like the space name or wiki name. This doesn't return a
     * proper entity reference, but just the string value that should be used for that type of entity.
     * 
     * @param type the target entity type; from Velocity it's enough to use a string with the uppercase name of the
     *            entity, like {@code 'SPACE'}
     * @param hint the hint of the value provider to use (valid hints are for example "default", "current" and
     *            "currentmixed")
     * @return the configured value for the requested entity type, for example "Main" for the default space or "WebHome"
     *         for the default space homepage
     * @since 4.3M1
     */
    public String getEntityReferenceValue(EntityType type, String hint)
    {
        if (type == null) {
            return null;
        }
        try {
            EntityReferenceValueProvider provider =
                this.componentManager.getInstance(EntityReferenceValueProvider.class, hint);
            return provider.getDefaultValue(type);
        } catch (ComponentLookupException ex) {
            return null;
        }
    }

    /**
     * Convert passed references to a tree of references.
     * 
     * @param references the references
     * @return the references as a tree
     * @since 5.4RC1
     */
    public EntityReferenceTree toTree(Iterable< ? extends EntityReference> references)
    {
        return new EntityReferenceTree(references);
    }

    /**
     * Convert passed references to a tree of references.
     * 
     * @param references the references
     * @return the references as a tree
     * @since 5.4RC1
     */
    public EntityReferenceTree toTree(EntityReference... references)
    {
        return new EntityReferenceTree(references);
    }
}
