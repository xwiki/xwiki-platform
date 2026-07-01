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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Convert attachment resource reference into entity reference.
 *
 * @version $Id$
 * @since 17.0.0RC1
 * @since 16.10.2
 */
@Component
@Named("relative/attach")
@Singleton
public class RelativeAttachmentResourceReferenceEntityReferenceResolver
    extends AbstractRelativeResourceReferenceEntityReferenceResolver
{
    @Inject
    private AttachmentReferenceResolver<EntityReference> defaultReferenceAttachmentReferenceResolver;

    /**
     * Default constructor.
     */
    public RelativeAttachmentResourceReferenceEntityReferenceResolver()
    {
        super(ResourceType.ATTACHMENT);
    }

    @Override
    protected EntityType getEntityType()
    {
        return EntityType.ATTACHMENT;
    }

    @Override
    protected EntityReference resolveTyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        if (StringUtils.isEmpty(resourceReference.getReference())) {
            return null;
        }

        // Get relative reference
        EntityReference relativeReference =
            this.relativeReferenceResolver
                .resolve(resourceReference.getReference(), EntityType.ATTACHMENT, baseReference);

        EntityReference result = relativeReference;
        if (relativeReference.extractReference(EntityType.WIKI) != null) {
            // Resolve full reference
            AttachmentReference attachmentReference =
                this.defaultReferenceAttachmentReferenceResolver.resolve(relativeReference, baseReference);

            // See if the resolved (terminal or WebHome) document exists and, if so, use it.
            DocumentReference documentReference = attachmentReference.getDocumentReference();

            // Take care of fallback if needed
            DocumentReference finalDocumentReference =
                resolveDocumentReference(relativeReference.getParent(), documentReference, baseReference);
            // Also use that resolution if the relative reference doesn't contain any document, even if it doesn't
            // exists, to not produce incorrect references.
            if (finalDocumentReference != documentReference
                || relativeReference.extractReference(EntityType.DOCUMENT) == null) {
                result = new AttachmentReference(attachmentReference.getName(), finalDocumentReference);
            }
        }

        return result;
    }
}
