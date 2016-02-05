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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import com.google.common.base.Objects;

/**
 * Convert attachment resource reference into entity reference.
 *
 * @version $Id$
 * @since 7.4.1
 */
@Component
@Named("attach")
@Singleton
public class AttachmentResourceReferenceEntityReferenceResolver extends AbstractResourceReferenceEntityReferenceResolver
{
    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> currentAttachmentReferenceResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Default constructor.
     */
    public AttachmentResourceReferenceEntityReferenceResolver()
    {
        super(ResourceType.ATTACHMENT);
    }

    @Override
    protected EntityReference resolveTyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        AttachmentReference attachmentReference =
            this.currentAttachmentReferenceResolver.resolve(resourceReference.getReference(), baseReference);

        // See if the resolved (terminal or WebHome) document exists and, if so, use it.
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        if (!this.documentAccessBridge.exists(documentReference)) {
            // Also consider explicit "WebHome" references (i.e. the ones ending in "WebHome").
            String defaultDocumentName =
                this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

            // If already a space home page, no fallback
            // If same as current page, no fallback
            if (!documentReference.getName().equals(defaultDocumentName)
                && !Objects.equal(documentReference, baseReference)) {
                // It does not exist, make it an attachment located on a space home page
                SpaceReference spaceReference =
                    new SpaceReference(documentReference.getName(), (SpaceReference) documentReference.getParent());

                documentReference = new DocumentReference(defaultDocumentName, spaceReference);

                // Otherwise, handle it as a space reference for both cases when it exists or when it doesn't exist.
                attachmentReference = new AttachmentReference(attachmentReference.getName(), documentReference);
            }
        }

        return attachmentReference;
    }
}
