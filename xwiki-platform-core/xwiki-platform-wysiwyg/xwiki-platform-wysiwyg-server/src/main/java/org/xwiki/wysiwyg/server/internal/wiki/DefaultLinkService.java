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
package org.xwiki.wysiwyg.server.internal.wiki;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.URIReference;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;
import org.xwiki.wysiwyg.server.wiki.EntityReferenceConverter;
import org.xwiki.wysiwyg.server.wiki.LinkService;

/**
 * The service used to create links.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLinkService implements LinkService
{
    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to serialize XWiki document references.
     */
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The component used to resolve an entity reference relative to another entity reference.
     */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<EntityReference> explicitReferenceEntityReferenceResolver;

    /**
     * The component used to resolve a string entity reference relative to another entity reference.
     */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> explicitStringEntityReferenceResolver;

    /**
     * The component used to serialize link references.
     * <p>
     * Note: The link reference syntax is independent of the syntax of the edited document. The current hint should be
     * replaced with a generic one to avoid confusion.
     */
    @Inject
    @Named("xhtmlmarker")
    private ResourceReferenceSerializer linkReferenceSerializer;

    /**
     * The component used to parser link references.
     * <p>
     * Note: The link reference syntax is independent of the syntax of the edited document. The current hint should be
     * replaced with a generic one to avoid confusion.
     */
    @Inject
    @Named("xhtmlmarker")
    private ResourceReferenceParser linkReferenceParser;

    /**
     * The object used to convert between client and server entity reference.
     */
    @Inject
    private EntityReferenceConverter entityReferenceConverter;

    /**
     * Used to resolve a DocumentReference from a SpaceReference, i.e. the space's homepage.
     */
    @Inject
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    /**
     * Convert an Attachment Reference from a String (specified using the space reference format) into an Attachment
     * object.
     */
    @Inject
    @Named("currentspaceattachment")
    private EntityReferenceResolver<String> currentSpaceAttachmentReferenceResolver;

    @Override
    public EntityConfig getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference origin,
        org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference destination)
    {
        String url;
        String destRelativeStrRef;

        if (org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.EXTERNAL == destination.getEntityReference()
            .getType()) {
            url = new URIReference(destination.getEntityReference()).getURI();
            destRelativeStrRef = url;
        } else {
            EntityReference originRef = entityReferenceConverter.convert(origin);
            EntityReference destRef = entityReferenceConverter.convert(destination.getEntityReference());
            destRef = explicitReferenceEntityReferenceResolver.resolve(destRef, destRef.getType(), originRef);
            destRelativeStrRef = entityReferenceSerializer.serialize(destRef, originRef);
            url = getEntityURL(destRef);
        }

        EntityConfig entityConfig = new EntityConfig();
        entityConfig.setUrl(url);
        entityConfig.setReference(getLinkReference(destination.getType(), destination.isTyped(), destRelativeStrRef));
        return entityConfig;
    }

    /**
     * @param entityReference an entity reference
     * @return the URL to access the specified entity
     */
    private String getEntityURL(EntityReference entityReference)
    {
        switch (entityReference.getType()) {
            case DOCUMENT:
                DocumentReference documentReference = new DocumentReference(entityReference);
                return documentAccessBridge.getDocumentURL(documentReference, "view", null, null);
            case ATTACHMENT:
                AttachmentReference attachmentReference = new AttachmentReference(entityReference);
                return documentAccessBridge.getAttachmentURL(attachmentReference, false);
            default:
                return null;
        }
    }

    /**
     * @param clientResourceType the type of linked resource
     * @param typed {@code true} to include the resource scheme in the link reference serialization, {@code false}
     *            otherwise
     * @param relativeStringEntityReference a relative string entity reference
     * @return a link reference that can be used to insert a link to the specified entity
     */
    private String getLinkReference(
        org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType clientResourceType, boolean typed,
        String relativeStringEntityReference)
    {
        ResourceType resourceType = new ResourceType(clientResourceType.getScheme());
        ResourceReference linkReference = new ResourceReference(relativeStringEntityReference, resourceType);
        linkReference.setTyped(typed);
        return linkReferenceSerializer.serialize(linkReference);
    }

    @Override
    public org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference parseLinkReference(String linkReferenceAsString,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        ResourceReference linkReference = linkReferenceParser.parse(linkReferenceAsString);

        ResourceType linkResourceType = linkReference.getType();
        if (ResourceType.SPACE.equals(linkResourceType)) {
            // Treat space resources the same as documents in order to reuse the UI.
            linkResourceType = ResourceType.DOCUMENT;
        }

        org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference clientLinkReference =
            new org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference();
        clientLinkReference.setType(org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType
            .forScheme(linkResourceType.getScheme()));
        clientLinkReference.setTyped(linkReference.isTyped());
        clientLinkReference.getParameters().putAll(linkReference.getParameters());
        clientLinkReference.setEntityReference(parseEntityReferenceFromResourceReference(linkReference,
            clientLinkReference.getType(), baseReference));

        return clientLinkReference;
    }

    /**
     * Parses a client entity reference from a link/resource reference.
     *
     * @param resourceReference the resource reference to parse
     * @param clientResourceType the previously resolved client-side type of the passed resource reference
     * @param baseReference the client entity reference that is used to resolve the parsed entity reference relative to
     * @return an untyped client entity reference
     */
    private org.xwiki.gwt.wysiwyg.client.wiki.EntityReference parseEntityReferenceFromResourceReference(
        ResourceReference resourceReference,
        org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType clientResourceType,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference result = null;

        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType clientEntityType;
        switch (clientResourceType) {
            case DOCUMENT:
                clientEntityType = org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.DOCUMENT;
                break;
            case ATTACHMENT:
                clientEntityType = org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.ATTACHMENT;
                break;
            default:
                clientEntityType = org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.EXTERNAL;
                break;
        }

        if (clientEntityType == org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.EXTERNAL) {
            result = new URIReference(resourceReference.getReference()).getEntityReference();
        } else {
            EntityReference serverEntityReference =
                parseServerEntityReferenceFromResourceReference(resourceReference, baseReference);

            result = entityReferenceConverter.convert(serverEntityReference);
        }

        return result;
    }

    /**
     * @param resourceReference the reference to resolve
     * @param baseReference the base reference to use when resolving
     * @return the resolved {@link EntityReference}
     */
    private EntityReference parseServerEntityReferenceFromResourceReference(ResourceReference resourceReference,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        EntityReference result = null;

        EntityReference baseServerEntityReference = entityReferenceConverter.convert(baseReference);

        String stringEntityReference = resourceReference.getReference();
        ResourceType resourceType = resourceReference.getType();

        // Resolve the resource reference based on its type. This might be enough in some cases.
        EntityType serverEntityType = null;
        if (ResourceType.DOCUMENT.equals(resourceType)) {
            serverEntityType = EntityType.DOCUMENT;
        } else if (ResourceType.SPACE.equals(resourceType)) {
            serverEntityType = EntityType.SPACE;
        } else if (ResourceType.ATTACHMENT.equals(resourceType)) {
            serverEntityType = EntityType.ATTACHMENT;
        } else {
            return null;
        }
        result =
            explicitStringEntityReferenceResolver.resolve(stringEntityReference, serverEntityType,
                baseServerEntityReference);

        // Depending on the resource type, additional work might be needed to resolve the reference.
        if (ResourceType.SPACE.equals(resourceType)) {
            // The rendering side already took care of identifying the type of resource, both in the case of typed
            // or untyped (when document existence needs to be checked) resources. We just need to make sure we
            // return a supported type (in this case, the space's home document).
            result = defaultReferenceDocumentReferenceResolver.resolve(result);
        } else if (ResourceType.ATTACHMENT.equals(resourceType)) {
            // In this case, the rendering produces the correct URL but does not expose the resource type ("space"
            // or document attachment). We need to resolve it ourselves.

            // See if the resolved (terminal or "WebHome") document exists and, if so, use it.
            DocumentReference documentReference = new DocumentReference(result.extractReference(EntityType.DOCUMENT));
            // Also consider explicit "WebHome" references (i.e. the ones ending in "WebHome").
            String defaultDocumentName =
                defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

            if (!documentReference.equals(baseServerEntityReference)
                && !documentReference.getName().equals(defaultDocumentName)
                && !documentAccessBridge.exists(documentReference)) {
                // Otherwise, handle it as a space reference for both cases when it exists or when it doesn't exist.
                result =
                    this.currentSpaceAttachmentReferenceResolver.resolve(stringEntityReference, EntityType.ATTACHMENT,
                        baseServerEntityReference);
            }
        }

        return result;
    }
}
