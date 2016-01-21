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
package com.xpn.xwiki.internal.model.reference;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;

/**
 * Resolve a String representing an Entity Reference into an {@link org.xwiki.model.reference.EntityReference} object.
 * The behavior is the one defined in {@link com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider},
 * except when resolving an {@link EntityType#ATTACHMENT}.
 * <p/>
 * In the {@link EntityType#ATTACHMENT} case, the resolved document will always be the default document. The space and
 * wiki parts will still be resolved with the
 * {@link com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider} (in case they are missing).
 *
 * @version $Id$
 * @since 7.4.1, 8.0M1
 */
@Component
@Named("currentspaceattachment")
@Singleton
public class CurrentSpaceAttachmentStringEntityReferenceResolver extends CurrentStringEntityReferenceResolver
{
    /**
     * Custom reference parsing order that skips the document part for attachments and treats what comes before the @ as
     * a space reference.
     * <p/>
     * FIXME: {@link org.xwiki.model.internal.reference.StringReferenceSeparators#CATTACHMENTSEP} is currently package
     * private. Find a solution that avoids hardcoding/duplicating it here.
     */
    private static final Map<Character, EntityType> ATTACHMENT_TYPE_SETUP = Collections.singletonMap('@',
        EntityType.SPACE);

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType type, Object... parameters)
    {
        EntityReference resolvedReference = super.resolve(entityReferenceRepresentation, type, parameters);
        EntityReference result = resolvedReference;

        // If an attachment was resolved, it will not have any document reference, because of the custom parsing order.
        // Make sure to assign it a default document reference, to be a complete attachment reference.
        if (EntityType.ATTACHMENT.equals(type)) {
            EntityReference attachmentReference = resolvedReference.extractReference(EntityType.ATTACHMENT);
            EntityReference spaceReference = resolvedReference.extractReference(EntityType.SPACE);
            EntityReference documentReference = defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT);

            // Set the resolved space as parent of the default document.
            documentReference = new EntityReference(documentReference, spaceReference);

            // Set the default document as parent of the resolved attachment.
            result = new EntityReference(attachmentReference, documentReference);
        }

        return result;
    }

    @Override
    protected Map<Character, EntityType> getTypeSetup(EntityType type)
    {
        // Use a custom type setup when resolving attachments.
        if (EntityType.ATTACHMENT.equals(type)) {
            return ATTACHMENT_TYPE_SETUP;
        } else {
            return super.getTypeSetup(type);
        }
    }
}
