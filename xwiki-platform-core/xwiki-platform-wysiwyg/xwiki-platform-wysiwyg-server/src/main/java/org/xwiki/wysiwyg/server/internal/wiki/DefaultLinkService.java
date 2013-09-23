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
import org.xwiki.model.reference.EntityReference;
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
    private final EntityReferenceConverter entityReferenceConverter = new EntityReferenceConverter();

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
        org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference clientLinkReference =
            new org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference();
        clientLinkReference.setType(org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType
            .forScheme(linkReference.getType().getScheme()));
        clientLinkReference.setTyped(linkReference.isTyped());
        clientLinkReference.getParameters().putAll(linkReference.getParameters());
        clientLinkReference.setEntityReference(parseEntityReferenceFromResourceReference(linkReference.getReference(),
            clientLinkReference.getType(), baseReference));
        return clientLinkReference;
    }

    /**
     * Parses a client entity reference from a link/resource reference.
     * 
     * @param stringEntityReference a string entity reference extracted from a link/resource reference
     * @param resourceType the type of resource the string entity reference was extracted from
     * @param baseReference the client entity reference that is used to resolve the parsed entity reference relative to
     * @return an untyped client entity reference
     */
    private org.xwiki.gwt.wysiwyg.client.wiki.EntityReference parseEntityReferenceFromResourceReference(
        String stringEntityReference, org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType resourceType,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType entityType;
        switch (resourceType) {
            case DOCUMENT:
                entityType = org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.DOCUMENT;
                break;
            case ATTACHMENT:
                entityType = org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.ATTACHMENT;
                break;
            default:
                entityType = org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.EXTERNAL;
                break;
        }
        if (entityType == org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.EXTERNAL) {
            return new URIReference(stringEntityReference).getEntityReference();
        } else {
            return entityReferenceConverter.convert(explicitStringEntityReferenceResolver.resolve(
                stringEntityReference, EntityType.valueOf(entityType.toString()), entityReferenceConverter
                    .convert(baseReference)));
        }
    }
}
