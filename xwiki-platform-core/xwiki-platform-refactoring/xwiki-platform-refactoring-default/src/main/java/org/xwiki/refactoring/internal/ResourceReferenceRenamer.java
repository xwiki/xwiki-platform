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
package org.xwiki.refactoring.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import static com.xpn.xwiki.XWiki.DEFAULT_SPACE_HOMEPAGE;

/**
 * Utility class to rename resource reference.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Component(roles = ResourceReferenceRenamer.class)
@Singleton
public class ResourceReferenceRenamer
{
    @Inject
    private EntityReferenceResolver<ResourceReference> entityReferenceResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Inject
    private PageReferenceResolver<EntityReference> defaultReferencePageReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Update the given document reference so that if it targets the old document reference, the new document reference
     * is used instead.
     *
     * @param resourceReference the document reference to be checked and updated
     * @param oldReference the old document reference that needs to be replaced
     * @param newReference the new document reference target
     * @param currentDocumentReference the current document where the resource reference is located
     * @param relative {@code true} if the reference should be kept relative to the current document
     * @param updatedEntities the map of entities that are or are going to be updated: the map contains the source
     *      and target destination.
     * @return {@code true} if the resource reference has been updated
     */
    public boolean updateResourceReference(ResourceReference resourceReference,
        DocumentReference oldReference, DocumentReference newReference, DocumentReference currentDocumentReference,
        boolean relative, Map<EntityReference, EntityReference> updatedEntities)
    {
        return (relative)
            ? this.updateRelativeResourceReference(resourceReference, oldReference, newReference, updatedEntities)
            : this.updateAbsoluteResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, updatedEntities);
    }

    private boolean checkIfDocExists(DocumentReference documentReference)
    {
        XWikiContext context = contextProvider.get();
        boolean docExists = false;
        try {
            docExists = context.getWiki().exists(documentReference, context);
        } catch (XWikiException e) {
            this.logger.error("Error while checking if [{}] exists for link refactoring.", documentReference,
                e);
        }
        return docExists;
    }

    /**
     * Update the given document reference so that if it targets the old attachment reference, the new attachment
     * reference is used instead.
     *
     * @param resourceReference the document reference to be checked and updated
     * @param oldReference the old attachment reference that needs to be replaced
     * @param newReference the new attachment reference target
     * @param currentDocumentReference the current document where the resource reference is located
     * @param relative {@code true} if the reference should be kept relative to the current document
     * @param updatedEntities the map of entities that are or are going to be updated: the map contains the source
     *      and target destination.
     * @return {@code true} if the attachment reference has been updated
     */
    public boolean updateResourceReference(ResourceReference resourceReference,
        AttachmentReference oldReference, AttachmentReference newReference, DocumentReference currentDocumentReference,
        boolean relative, Map<EntityReference, EntityReference> updatedEntities)
    {
        return (relative)
            ? this.updateRelativeResourceReference(resourceReference, oldReference, newReference, updatedEntities)
            : this.updateAbsoluteResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, updatedEntities);
    }

    private boolean updateAbsoluteResourceReference(ResourceReference resourceReference,
        DocumentReference oldReference, DocumentReference newReference, DocumentReference currentDocumentReference,
        Map<EntityReference, EntityReference> updatedEntities)
    {
        boolean result = false;

        // FIXME: the root cause of XWIKI-18634 is related to this call.
        EntityReference linkEntityReference =
            this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference);

        DocumentReference linkTargetDocumentReference =
            this.defaultReferenceDocumentReferenceResolver.resolve(linkEntityReference);
        EntityReference newTargetReference = newReference;
        ResourceType newResourceType = resourceReference.getType();
        EntityReference absoluteResolvedReference = this.entityReferenceResolver.resolve(resourceReference, null);

        // If the link targets the old (renamed) document reference and it's an absolute reference
        // (i.e. its resolution without any given parameter gives same result than its resolution with the
        // currentDocument) then we must update it
        // We also update the link if it's not an absolute link but the current document is not part of the move job,
        // as in this case there won't be any other call to perform the link refactoring.
        boolean shouldBeUpdated = linkTargetDocumentReference.equals(oldReference)
            && (absoluteResolvedReference.equals(linkEntityReference)
            || !updatedEntities.containsKey(currentDocumentReference));

        if (shouldBeUpdated) {
            // If the link was resolved to a space...
            if (EntityType.SPACE.equals(linkEntityReference.getType())) {
                if (DEFAULT_SPACE_HOMEPAGE.equals(newReference.getName())) {
                    // If the new document reference is also a space (non-terminal doc), be careful to keep it
                    // serialized as a space still (i.e. without ".WebHome") and not serialize it as a doc by mistake
                    // (i.e. with ".WebHome").
                    newTargetReference = newReference.getLastSpaceReference();
                } else {
                    // If the new target is a non-terminal document, we can not use a "space:" resource type to access
                    // it anymore. To fix it, we need to change the resource type of the link reference "doc:".
                    newResourceType = ResourceType.DOCUMENT;
                }
            }

            // If the link was resolved to a page...
            if (EntityType.PAGE.equals(linkEntityReference.getType())) {
                // Be careful to keep it serialized as a page still and not serialize it as a doc by mistake
                newTargetReference = this.defaultReferencePageReferenceResolver.resolve(newReference);
            }

            // If the link was resolved as an attachment
            if (EntityType.ATTACHMENT.equals(linkEntityReference.getType())) {
                // Make sure to serialize an attachment reference and not just the document
                newTargetReference = new AttachmentReference(linkEntityReference.getName(), newReference);
            }

            String newReferenceString =
                this.compactEntityReferenceSerializer.serialize(newTargetReference, currentDocumentReference);

            resourceReference.setReference(newReferenceString);
            resourceReference.setType(newResourceType);
            result = true;
        }
        return result;
    }

    private <T extends EntityReference> boolean updateRelativeResourceReference(ResourceReference resourceReference,
        T oldReference, T newReference, Map<EntityReference, EntityReference> updatedEntities)
    {
        boolean result = false;

        EntityReference linkEntityReference = this.entityReferenceResolver.resolve(resourceReference, null,
            newReference);
        // current link, use the old document's reference to fill in blanks.
        EntityReference oldLinkReference =
            this.entityReferenceResolver.resolve(resourceReference, null, oldReference);
        EntityReference absoluteResolvedReference = this.entityReferenceResolver.resolve(resourceReference, null);

        boolean docExists = false;
        EntityType entityType = linkEntityReference.getType();
        DocumentReference documentReference = null;
        DocumentReference documentReferenceTarget = null;
        if (entityType == EntityType.DOCUMENT || entityType.getAllowedParents().contains(EntityType.DOCUMENT)) {
            documentReference =
                (DocumentReference) oldLinkReference.extractReference(EntityType.DOCUMENT);
            documentReferenceTarget =
                (DocumentReference) linkEntityReference.extractReference(EntityType.DOCUMENT);
            docExists = checkIfDocExists(documentReference);
        } else {
            docExists = true;
        }

        boolean anyMatchInMovedReferences =
            (oldLinkReference.hasParent(oldReference)
                || (documentReferenceTarget != null
                && documentReferenceTarget.equals(updatedEntities.get(documentReference))));

        // We should update the reference if:
        //  - it's relative: the resolution of the reference without any parameter, and the resolution of the reference
        //                   with the new reference should give different results
        //  - it doesn't match any reference moved in the same job, i.e. in the same hierarchy
        //  - the new and old link references don`t match
        //  - the link refers to a doc that exists
        boolean shouldBeUpdated = !absoluteResolvedReference.equals(linkEntityReference)
            && !anyMatchInMovedReferences
            && !linkEntityReference.equals(oldLinkReference)
            && docExists;

        if (shouldBeUpdated) {
            // Serialize the old (original) link relative to the new document's location, in compact form.
            String serializedLinkReference =
                this.compactEntityReferenceSerializer.serialize(oldLinkReference, newReference);
            resourceReference.setReference(serializedLinkReference);
            result = true;
        }
        return result;
    }

    // FIXME: Check if we shouldn't use the updatedEntities here.
    private boolean updateAbsoluteResourceReference(ResourceReference resourceReference,
        AttachmentReference oldReference, AttachmentReference newReference, DocumentReference currentDocumentReference,
        Map<EntityReference, EntityReference> updatedEntities)
    {
        boolean result = false;

        // FIXME: the root cause of XWIKI-18634 is related to this call.
        EntityReference linkEntityReference =
            this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference);

        // If the link targets the old (renamed) document reference, we must update it.
        if (linkEntityReference.equals(oldReference)) {
            
            String newReferenceString =
                this.compactEntityReferenceSerializer.serialize(newReference, currentDocumentReference);

            resourceReference.setReference(newReferenceString);
            result = true;
        }
        return result;
    }
}
