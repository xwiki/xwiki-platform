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
package org.xwiki.rendering.internal.resolver;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Base class helper for EntityReferenceResolver<ResourceReference> implementations.
 *
 * @version $Id$
 * @since 7.4.1
 */
public abstract class AbstractResourceReferenceEntityReferenceResolver
    implements EntityReferenceResolver<ResourceReference>
{
    @Inject
    @Named("current")
    protected EntityReferenceResolver<String> currentEntityReferenceResolver;

    @Inject
    protected EntityReferenceResolver<EntityReference> defaultEntityReferenceResolver;

    @Inject
    @Named("current")
    protected Provider<DocumentReference> currentDocumentProvider;

    @Inject
    protected EntityReferenceProvider defaultReferenceProvider;

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    protected Logger logger;

    protected ResourceType resourceType;

    /**
     * Default constructor with an unset resourceType.
     */
    public AbstractResourceReferenceEntityReferenceResolver()
    {
    }

    /**
     * @param type the resource type that this resolver will support
     */
    public AbstractResourceReferenceEntityReferenceResolver(ResourceType type)
    {
        this.resourceType = type;
    }

    @Override
    public EntityReference resolve(ResourceReference resourceReference, EntityType entityType, Object... parameters)
    {
        if (resourceReference == null) {
            return null;
        }

        if (this.resourceType != null && !resourceReference.getType().equals(this.resourceType)) {
            throw new IllegalArgumentException(
                String.format("You must pass a resource reference of type [%s]. [%s] was passed", this.resourceType,
                    resourceReference));
        }

        EntityReference baseReference = getBaseReference(resourceReference, parameters);

        EntityReference entityReference;

        if (resourceReference.isTyped()) {
            entityReference = resolveTyped(resourceReference, baseReference);
        } else {
            entityReference = resolveUntyped(resourceReference, baseReference);
        }

        return convertReference(entityReference, entityType);
    }

    protected EntityReference resolveUntyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        return resolveTyped(resourceReference, baseReference);
    }

    protected abstract EntityReference resolveTyped(ResourceReference resourceReference, EntityReference baseReference);

    protected EntityReference getBaseReference(ResourceReference resourceReference, Object... parameters)
    {
        EntityReference baseReference = (parameters.length > 0 && parameters[0] instanceof EntityReference)
            ? (EntityReference) parameters[0] : null;

        if (!resourceReference.getBaseReferences().isEmpty()) {
            // If the passed reference has a base reference, resolve it first with a current resolver (it should
            // normally be absolute but who knows what the API caller has specified...)
            baseReference = resolveBaseReference(resourceReference.getBaseReferences(), baseReference);
        }

        if (baseReference == null) {
            baseReference = this.currentDocumentProvider.get();
        }

        return baseReference;
    }

    protected EntityReference resolveBaseReference(List<String> baseReferences, EntityReference defaultBaseReference)
    {
        EntityReference resolvedBaseReference = defaultBaseReference;
        for (String baseReference : baseReferences) {
            resolvedBaseReference =
                this.currentEntityReferenceResolver.resolve(baseReference, EntityType.DOCUMENT, resolvedBaseReference);
        }

        return resolvedBaseReference;
    }

    protected EntityReference convertReference(EntityReference entityReference, EntityType entityType)
    {
        // Convert the entity reference if needed
        if (entityReference != null && entityType != null && entityReference.getType() != entityType) {
            return this.defaultEntityReferenceResolver.resolve(entityReference, entityType);
        }

        return entityReference;
    }

    protected DocumentReference resolveDocumentReference(EntityReference sourceReference, DocumentReference reference,
        EntityReference baseReference)
    {
        DocumentReference finalReference = reference;

        // If same as base reference, no fallback
        if (!Objects.equals(reference, baseReference)) {
            // If already a space home page, no fallback
            String defaultDocumentName =
                this.defaultReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
            if (!reference.getName().equals(defaultDocumentName)) {
                finalReference =
                    resolveDocumentReference(sourceReference, finalReference, baseReference, true, defaultDocumentName);
            }
        }

        return finalReference;
    }

    protected DocumentReference resolveDocumentReference(EntityReference sourceReference, DocumentReference reference,
        EntityReference baseReference, boolean trySpaceSibling, String defaultDocumentName)
    {
        DocumentReference finalReference = reference;

        if (!exists(reference)) {
            // It does not exist, make it a space home page.
            SpaceReference spaceReference =
                new SpaceReference(reference.getName(), (SpaceReference) reference.getParent());
            finalReference = new DocumentReference(defaultDocumentName, spaceReference);

            if (trySpaceSibling
                && trySpaceSiblingFallback(sourceReference, finalReference, baseReference, defaultDocumentName)) {
                // Try as a space sibling.
                DocumentReference siblingReference =
                    resolveSiblingSpaceDocumentReference(sourceReference, reference, baseReference,
                    defaultDocumentName);

                // We accept the resolved sibling reference only if it is a terminal document (i.e. an exists check was
                // already performed for it) OR, in the case of a non-terminal document, if it actually exists (by
                // performing the exists check now). Otherwise, the default will remain the child non-terminal document.
                if (!siblingReference.getName().equals(defaultDocumentName)
                    || exists(siblingReference)) {
                    finalReference = siblingReference;
                }
            }
        }

        return finalReference;
    }

    protected DocumentReference resolveSiblingSpaceDocumentReference(EntityReference sourceReference,
        DocumentReference reference, EntityReference baseReference, String defaultDocumentName)
    {
        DocumentReference finalReference;

        // Create space sibling
        EntityReference parentReference = reference.getParent().getParent();
        if (parentReference instanceof SpaceReference) {
            finalReference = new DocumentReference(reference.getName(), (SpaceReference) parentReference);

            finalReference =
                resolveDocumentReference(sourceReference, finalReference, baseReference, false, defaultDocumentName);
        } else {
            SpaceReference spaceReference = new SpaceReference(reference.getName(), (WikiReference) parentReference);
            finalReference = new DocumentReference(defaultDocumentName, spaceReference);
        }

        return finalReference;
    }

    protected boolean trySpaceSiblingFallback(EntityReference sourceReference, DocumentReference finalReference,
        EntityReference baseReference, String defaultDocumentName)
    {
        // If not relative, no space sibling fallback
        // If base reference not a space home page, no space sibling fallback
        // If finalReference exist, no space sibling fallback
        return sourceReference.getParent() == null && baseReference != null
            && baseReference.getName().equals(defaultDocumentName) && !exists(finalReference);
    }

    private boolean exists(DocumentReference documentReference)
    {
        try {
            return this.documentAccessBridge.exists(documentReference);
        } catch (Exception e) {
            this.logger.error("Failed to check the existence of the document with reference [{}]", documentReference,
                e);
        }

        return false;
    }
}
