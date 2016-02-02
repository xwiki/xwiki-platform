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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;

@Component
@Singleton
public class ResourceReferenceEntityReferenceResolver implements EntityReferenceResolver<ResourceReference>
{
    @Inject
    private EntityReferenceProvider defaultReferenceProvider;

    /**
     * Parser to parse link references pointing to documents.
     */
    @Inject
    @Named("doc")
    private ResourceReferenceTypeParser documentResourceReferenceTypeParser;

    /**
     * Parser to parse link references pointing to spaces.
     */
    @Inject
    @Named("space")
    private ResourceReferenceTypeParser spaceResourceReferenceTypeParser;

    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> currentAttachmentReferenceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentEntityDocumentReferenceResolver;

    @Inject
    @Named("current")
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeReferenceResolver;

    @Inject
    @Named("currentspace")
    private AttachmentReferenceResolver<String> currentSpaceAttachmentReferenceResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public EntityReference resolve(ResourceReference resourceReference, EntityType type, Object... parameters)
    {
        if (resourceReference == null) {
            return null;
        }

        EntityReference baseReference = null;
        if (!resourceReference.getBaseReferences().isEmpty()) {
            // If the passed reference has a base reference, resolve it first with a current resolver (it should
            // normally be absolute but who knows what the API caller has specified...)
            baseReference = resolveBaseReference(resourceReference.getBaseReferences(), resourceReference.getType());
        }

        EntityReference entityReference;

        if (resourceReference.isTyped()) {
            entityReference = resolveTyped(resourceReference, baseReference);
        } else {
            entityReference = resolveUntyped(resourceReference, baseReference);
        }

        return entityReference;
    }

    private EntityReference resolveTyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        return resolveReferenceWithBase(resourceReference.getReference(), resourceReference.getType(), baseReference);
    }

    private EntityReference resolveBaseReference(List<String> baseReferences, ResourceType resourceType)
    {
        EntityReference resolvedBaseReference = null;
        for (String baseReference : baseReferences) {
            resolvedBaseReference = resolveReferenceWithBase(baseReference, resourceType, resolvedBaseReference);
        }

        return resolvedBaseReference;
    }

    private EntityReference resolveReferenceWithBase(String resourceReference, ResourceType resourceType,
        EntityReference resolvedBaseReference)
    {
        // Use any previously resolved base reference when resolving the current one.
        Object[] resolveParameters = null;
        if (resolvedBaseReference != null) {
            resolveParameters = new Object[] {resolvedBaseReference};
        } else {
            resolveParameters = new Object[0];
        }

        EntityReference result = null;

        // Resolve the current reference.
        if (ResourceType.DOCUMENT.equals(resourceType)) {
            result = this.currentDocumentReferenceResolver.resolve(resourceReference, resolveParameters);
        } else if (ResourceType.SPACE.equals(resourceType)) {
            // Extract the space's homepage.
            result = this.currentSpaceReferenceResolver.resolve(resourceReference, resolveParameters);
        } else if (ResourceType.ATTACHMENT.equals(resourceType)) {
            result = resolveAttachmentReference(resourceReference, resourceType, resolvedBaseReference);
        } else {
            // No idea how to get an EntityReference from that
            return null;
        }

        return result;
    }

    private AttachmentReference resolveAttachmentReference(String resourceReference, ResourceType resourceType,
        EntityReference resolvedBaseReference)
    {
        AttachmentReference attachmentReference;
        if (resolvedBaseReference != null) {
            // If the passed reference has a base reference, resolve it first with a current resolver (it should
            // normally be absolute but who knows what the API caller has specified...)
            attachmentReference =
                this.currentAttachmentReferenceResolver.resolve(resourceReference, resolvedBaseReference);
        } else {
            attachmentReference = this.currentAttachmentReferenceResolver.resolve(resourceReference);
        }

        // See if the resolved (terminal or WebHome) document exists and, if so, use it.
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        if (!this.documentAccessBridge.exists(documentReference)) {
            // Also consider explicit "WebHome" references (i.e. the ones ending in "WebHome").
            String defaultDocumentName =
                this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
            if (!documentReference.getName().equals(defaultDocumentName)) {
                // Otherwise, handle it as a space reference for both cases when it exists or when it doesn't exist.
                attachmentReference = this.currentSpaceAttachmentReferenceResolver.resolve(resourceReference);
            }
        }

        return attachmentReference;
    }

    private EntityReference resolveUntyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        // If it's not a document or if the reference is empty fallback on typed logic
        if (!ResourceType.DOCUMENT.equals(resourceReference.getType())
            || StringUtils.isEmpty(resourceReference.getReference())) {
            return resolveReferenceWithBase(resourceReference.getReference(), resourceReference.getType(),
                baseReference);
        }

        // Use any previously resolved base reference when resolving the current one.
        Object[] baseParameters = null;
        if (baseReference != null) {
            baseParameters = new Object[] {baseReference};
        } else {
            baseParameters = new Object[0];
        }

        // Get the full document reference
        EntityReference reference =
            this.currentDocumentReferenceResolver.resolve(resourceReference.getReference(), baseParameters);

        // It can be a link to an existing terminal document
        if (!this.documentAccessBridge.exists((DocumentReference) reference)) {
            // It does not exist, make it a space home page. If the space does not exist, it will be
            // a wanted link.
            SpaceReference spaceReference =
                new SpaceReference(reference.getName(), (SpaceReference) reference.getParent());

            // Return a DocumentReference by default since a DOCUMENTresource reference was provided
            String defaultDocumentName =
                this.defaultReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
            reference = new DocumentReference(defaultDocumentName, spaceReference);
        }

        return reference;
    }
}
