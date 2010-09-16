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
package com.xpn.xwiki.wysiwyg.server.internal.wiki;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.renderer.LinkReferenceSerializer;

import com.xpn.xwiki.wysiwyg.server.wiki.EntityReferenceConverter;
import com.xpn.xwiki.wysiwyg.server.wiki.LinkService;

/**
 * The service used to create links.
 * 
 * @version $Id$
 */
public class DefaultLinkService implements LinkService
{
    /**
     * The attachment URI protocol.
     */
    private static final String ATTACHMENT_URI_PROTOCOL = "attach:";

    /**
     * The image URI protocol.
     */
    private static final String IMAGE_URI_PROTOCOL = "image:";

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to serialize XWiki document references.
     */
    @Requirement("compact")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The component used to resolve an entity reference relative to another entity reference.
     */
    @Requirement("explicit/reference")
    private EntityReferenceResolver<EntityReference> explicitReferenceEntityReferenceResolver;

    /**
     * The component used to resolve a string entity reference relative to another entity reference.
     */
    @Requirement("explicit")
    private EntityReferenceResolver<String> explicitStringEntityReferenceResolver;

    /**
     * The component used to serialize link references.
     */
    @Requirement
    private LinkReferenceSerializer linkReferenceSerializer;

    /**
     * The component used to parser link references.
     */
    @Requirement("xwiki/2.0")
    private LinkParser linkReferenceParser;

    /**
     * The object used to convert between client-side entity references and server-side entity references.
     */
    private final EntityReferenceConverter entityReferenceConverter = new EntityReferenceConverter();

    /**
     * {@inheritDoc}
     * 
     * @see LinkService#getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference,
     *      org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public EntityConfig getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference origin,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference destination)
    {
        EntityReference originReference = entityReferenceConverter.convert(origin);
        EntityReference destinationReference = entityReferenceConverter.convert(destination);
        destinationReference =
            explicitReferenceEntityReferenceResolver.resolve(destinationReference, destinationReference.getType(),
                originReference);
        String destRelativeStrRef = this.entityReferenceSerializer.serialize(destinationReference, originReference);

        EntityConfig entityConfig = new EntityConfig();
        entityConfig.setUrl(getEntityURL(destinationReference));
        entityConfig.setReference(getLinkReference(destination.getType(), destRelativeStrRef));
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
     * @param entityType the type of linked entity
     * @param relativeStringEntityReference a relative string entity reference
     * @return a link reference that can be used to insert a link to the specified entity
     */
    private String getLinkReference(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType entityType,
        String relativeStringEntityReference)
    {
        Link link = new Link();
        link.setType(entityType == org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.DOCUMENT
            ? LinkType.DOCUMENT : LinkType.URI);
        link.setReference(relativeStringEntityReference);
        String linkReference = linkReferenceSerializer.serialize(link);
        if (entityType == org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.ATTACHMENT) {
            linkReference = ATTACHMENT_URI_PROTOCOL + linkReference;
        }
        return linkReference;
    }

    /**
     * {@inheritDoc}
     * 
     * @see LinkService#parseLinkReference(String, org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType,
     *      org.xwiki.gwt.wysiwyg.client.wiki.EntityReference)
     */
    public org.xwiki.gwt.wysiwyg.client.wiki.EntityReference parseLinkReference(String linkReference,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType entityType,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        String fullLinkReference = linkReference;
        // Add the image protocol because the client doesn't provided it.
        if (entityType == org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType.IMAGE) {
            fullLinkReference = IMAGE_URI_PROTOCOL + linkReference;
        }
        Link link = linkReferenceParser.parse(fullLinkReference);
        String stringEntityReference = link.getReference();
        // Remove the URI protocol because the link reference parser doesn't do it.
        int uriSchemeDelimiter = stringEntityReference.indexOf(':');
        if (link.getType() == LinkType.URI && uriSchemeDelimiter > -1) {
            stringEntityReference = stringEntityReference.substring(uriSchemeDelimiter + 1);
        }
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference entityReference =
            entityReferenceConverter.convert(explicitStringEntityReferenceResolver.resolve(stringEntityReference,
                entityReferenceConverter.convert(entityType), entityReferenceConverter.convert(baseReference)));
        entityReference.setType(entityType);
        return entityReference;
    }
}
