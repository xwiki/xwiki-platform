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
package org.xwiki.rendering.internal.macro.context;

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Finds all relative Resource References in an XDOM and make them absolute.
 *
 * @version $Id$
 * @since 3.0M1
 */
@Component
public class DefaultXDOMResourceReferenceResolver implements XDOMResourceReferenceResolver
{
    /**
     * Used to transform relative document links into absolute references relative to the included document.
     */
    @Requirement("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    /**
     * Used to transform relative attachment links into absolute references relative to the included document.
     */
    @Requirement("explicit")
    private AttachmentReferenceResolver<String> explicitAttachmentReferenceResolver;

    /**
     * Used to serialize resolved document links into a string again since the Rendering API only manipulates Strings
     * (done voluntarily to be independent of any wiki engine and not draw XWiki-specific dependencies).
     */
    @Requirement
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * {@inheritDoc}
     * @see XDOMResourceReferenceResolver#resolve(java.util.List, org.xwiki.model.reference.DocumentReference)
     */
    public void resolve(List<Block> blocks, DocumentReference docReference)
    {
        XDOM xdom = new XDOM(blocks);

        // Resolve links
        for (LinkBlock block : xdom.getChildrenByType(LinkBlock.class, true)) {
            ResourceReference resourceReference = block.getReference();
            // Make reference absolute for links to document and attachments.
            if (resourceReference.getType().equals(ResourceType.DOCUMENT)
                || resourceReference.getType().equals(ResourceType.ATTACHMENT))
            {
                EntityReference entityReference;
                if (resourceReference.getType().equals(ResourceType.DOCUMENT)) {
                    entityReference = this.explicitDocumentReferenceResolver.resolve(resourceReference.getReference(),
                        docReference);
                } else {
                    entityReference = this.explicitAttachmentReferenceResolver.resolve(resourceReference.getReference(),
                        docReference);
                }
                String resolvedReference = this.defaultEntityReferenceSerializer.serialize(entityReference);
                resourceReference.setReference(resolvedReference);
            }
        }

        // Resolve images
        for (ImageBlock block : xdom.getChildrenByType(ImageBlock.class, true)) {
            ResourceReference resourceReference = block.getReference();
            // Make reference absolute for images in documents
            if (resourceReference.getType().equals(ResourceType.ATTACHMENT)) {
                String resolvedReference = this.defaultEntityReferenceSerializer.serialize(
                    this.explicitAttachmentReferenceResolver.resolve(resourceReference.getReference(), docReference));
                resourceReference.setReference(resolvedReference);
            }
        }
    }
}
