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
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Convert document resource reference into entity reference.
 * 
 * @version $Id$
 * @since 7.4.1
 */
@Component
@Named("doc")
@Singleton
public class DocumentResourceReferenceEntityReferenceResolver extends AbstractResourceReferenceEntityReferenceResolver
{
    @Inject
    private EntityReferenceProvider defaultReferenceProvider;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    public DocumentResourceReferenceEntityReferenceResolver()
    {
        super(ResourceType.DOCUMENT);
    }

    @Override
    protected EntityReference resolveTyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        return this.currentDocumentReferenceResolver.resolve(resourceReference.getReference(), baseReference);
    }

    @Override
    protected EntityReference resolveUntyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        // If the reference is empty fallback on typed logic
        if (StringUtils.isEmpty(resourceReference.getReference())) {
            return resolveTyped(resourceReference, baseReference);
        }

        // Get the full document reference
        DocumentReference reference =
            this.currentDocumentReferenceResolver.resolve(resourceReference.getReference(), baseReference);

        // It can be a link to an existing terminal document
        if (!this.documentAccessBridge.exists(reference)) {
            String defaultDocumentName =
                this.defaultReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

            if (!reference.getName().equals(defaultDocumentName)) {
                // It does not exist, make it a space home page. If the space does not exist, it will be
                // a wanted link.
                SpaceReference spaceReference =
                    new SpaceReference(reference.getName(), (SpaceReference) reference.getParent());

                // Return a DocumentReference by default since a DOCUMENTresource reference was provided
                reference = new DocumentReference(defaultDocumentName, spaceReference);
            }
        }

        return reference;
    }
}
