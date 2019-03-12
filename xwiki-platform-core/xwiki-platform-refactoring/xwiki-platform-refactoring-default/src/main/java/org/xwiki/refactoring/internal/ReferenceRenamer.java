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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.internal.render.LinkedResourceHelper;

/**
 * Helper to rename references in various context (link, image, etc. ).
 * 
 * @version $Id$
 * @since 11.2RC1
 */
@Component(roles = ReferenceRenamer.class)
@Singleton
public class ReferenceRenamer
{
    private static final Set<ResourceType> SUPPORTED_RESOURCE_TYPES = new HashSet<>(
        Arrays.asList(ResourceType.DOCUMENT, ResourceType.SPACE, ResourceType.PAGE, ResourceType.ATTACHMENT));

    @Inject
    private EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private LinkedResourceHelper linkedResourceHelper;

    @Inject
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Inject
    private PageReferenceResolver<EntityReference> defaultReferencePageReferenceResolver;

    /**
     * @param xdom the {@link XDOM} to modify
     * @param currentDocumentReference the current document reference
     * @param oldTarget the previous reference of the renamed document
     * @param newTarget the new reference of the renamed document
     * @return true if the passed {@link XDOM} was modified
     */
    public boolean renameReferences(XDOM xdom, DocumentReference currentDocumentReference, DocumentReference oldTarget,
        DocumentReference newTarget)
    {
        List<Block> blocks = this.linkedResourceHelper.getBlocks(xdom);

        boolean modified = false;

        for (Block block : blocks) {
            modified |= renameReference(block, currentDocumentReference, oldTarget, newTarget);
        }

        return modified;
    }

    private boolean renameReference(Block block, DocumentReference currentDocumentReference,
        DocumentReference oldTarget, DocumentReference newTarget)
    {
        ResourceReference resourceReference = this.linkedResourceHelper.getResourceReference(block);
        if (resourceReference == null) {
            // Skip invalid blocks.
            throw new IllegalArgumentException();
        }

        ResourceType resourceType = resourceReference.getType();

        if (!SUPPORTED_RESOURCE_TYPES.contains(resourceType)) {
            // We are currently only interested in Document or Space references.
            return false;
        }

        // Resolve the resource reference.
        EntityReference linkEntityReference =
            this.resourceReferenceResolver.resolve(resourceReference, null, currentDocumentReference);
        // Resolve the document of the reference.
        DocumentReference linkTargetDocumentReference =
            this.defaultReferenceDocumentReferenceResolver.resolve(linkEntityReference);
        EntityReference newTargetReference = newTarget;
        ResourceType newResourceType = resourceType;

        // If the link targets the old (renamed) document reference, we must update it.
        if (linkTargetDocumentReference.equals(oldTarget)) {
            // If the link was resolved to a space...
            if (EntityType.SPACE.equals(linkEntityReference.getType())) {
                if (XWiki.DEFAULT_SPACE_HOMEPAGE.equals(newTarget.getName())) {
                    // If the new document reference is also a space (non-terminal doc), be careful to keep it
                    // serialized as a space still (i.e. without ".WebHome") and not serialize it as a doc by mistake
                    // (i.e. with ".WebHome").
                    newTargetReference = newTarget.getLastSpaceReference();
                } else {
                    // If the new target is a non-terminal document, we can not use a "space:" resource type to access
                    // it anymore. To fix it, we need to change the resource type of the link reference "doc:".
                    newResourceType = ResourceType.DOCUMENT;
                }
            }

            // If the link was resolved to a page...
            if (EntityType.PAGE.equals(linkEntityReference.getType())) {
                // Be careful to keep it serialized as a page still and not serialize it as a doc by mistake
                newTargetReference = this.defaultReferencePageReferenceResolver.resolve(newTarget);
            }

            // If the link was resolved as an attachment
            if (EntityType.ATTACHMENT.equals(linkEntityReference.getType())) {
                // Make sure to serialize an attachment reference and not just the document
                newTargetReference = new AttachmentReference(linkEntityReference.getName(), newTarget);
            }

            String newReferenceString =
                this.compactEntityReferenceSerializer.serialize(newTargetReference, currentDocumentReference);

            // Update the reference in the XDOM.
            this.linkedResourceHelper.setResourceReferenceString(block, newReferenceString);
            this.linkedResourceHelper.setResourceType(block, newResourceType);

            return true;
        }

        return false;
    }

    /**
     * @param xdom the {@link XDOM} to modify
     * @param oldDocumentReference the previous reference of the renamed document
     * @param newDocumentReference the new reference of the renamed document
     * @return true if the passed {@link XDOM} was modified
     */
    public boolean updateRelativeReferences(XDOM xdom, DocumentReference oldDocumentReference,
        DocumentReference newDocumentReference)
    {
        List<Block> blocks = this.linkedResourceHelper.getBlocks(xdom);

        boolean modified = false;

        for (Block block : blocks) {
            modified |= updateRelativeReference(block, oldDocumentReference, newDocumentReference);
        }

        return modified;
    }

    private boolean updateRelativeReference(Block block, DocumentReference oldDocumentReference,
        DocumentReference newDocumentReference)
    {
        ResourceReference resourceReference = this.linkedResourceHelper.getResourceReference(block);
        if (resourceReference == null || StringUtils.isEmpty(resourceReference.getReference())) {
            // Skip invalid blocks.
            return false;
        }

        ResourceType resourceType = resourceReference.getType();

        if (!SUPPORTED_RESOURCE_TYPES.contains(resourceType)) {
            // We are currently only interested in Document or Space references.
            return false;
        }

        // current link, use the old document's reference to fill in blanks.
        EntityReference oldLinkReference =
            this.resourceReferenceResolver.resolve(resourceReference, null, oldDocumentReference);
        // new link, use the new document's reference to fill in blanks.
        EntityReference newLinkReference =
            this.resourceReferenceResolver.resolve(resourceReference, null, newDocumentReference);

        // If the new and old link references don`t match, then we must update the relative link.
        if (!newLinkReference.equals(oldLinkReference)) {
            // Serialize the old (original) link relative to the new document's location, in compact form.
            String serializedLinkReference =
                this.compactEntityReferenceSerializer.serialize(oldLinkReference, newDocumentReference);

            // Update the reference in the XDOM.
            this.linkedResourceHelper.setResourceReferenceString(block, serializedLinkReference);

            return true;
        }

        return false;
    }
}
