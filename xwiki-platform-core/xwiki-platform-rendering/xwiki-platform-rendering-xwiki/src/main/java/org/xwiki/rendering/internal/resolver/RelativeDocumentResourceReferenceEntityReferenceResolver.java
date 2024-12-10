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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Convert document resource reference into entity reference.
 *
 * @version $Id$
 * @since 17.0.0RC1
 * @since 16.10.2
 */
@Component
@Named("relative/doc")
@Singleton
public class RelativeDocumentResourceReferenceEntityReferenceResolver
    extends AbstractRelativeResourceReferenceEntityReferenceResolver
{
    @Inject
    private DocumentReferenceResolver<String> defaultStringDocumentReferenceResolver;

    @Inject
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    /**
     * Default constructor.
     */
    public RelativeDocumentResourceReferenceEntityReferenceResolver()
    {
        super(ResourceType.DOCUMENT);
    }

    @Override
    protected EntityType getEntityType()
    {
        return EntityType.DOCUMENT;
    }

    @Override
    protected EntityReference resolveUntyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        // If the reference is empty fallback on typed logic
        if (StringUtils.isEmpty(resourceReference.getReference())) {
            return resolveTyped(resourceReference, baseReference);
        }

        // Get relative reference
        EntityReference relativeReference =
            this.relativeReferenceResolver.resolve(resourceReference.getReference(), EntityType.DOCUMENT,
                baseReference);

        DocumentReference reference;
        if (relativeReference != null && (relativeReference.extractReference(EntityType.SPACE) == null
            || relativeReference.extractReference(EntityType.WIKI) == null)) {
            return setDefaultDocument(relativeReference);
        } else {
            // Resolve the full document reference
            // We don't start from the previously parsed relative reference to not loose "." prefixed reference meaning
            reference =
                this.defaultStringDocumentReferenceResolver.resolve(resourceReference.getReference(), baseReference);
            // Take care of fallback if needed
            return resolveDocumentReference(relativeReference, reference, baseReference);
        }
    }

    private EntityReference setDefaultDocument(EntityReference relativeReference)
    {
        String defaultDocumentName =
            this.defaultReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

        EntityReference result;
        if (!defaultDocumentName.equals(relativeReference.getName())) {
            EntityReference parentSpace = new EntityReference(relativeReference.getName(), EntityType.SPACE,
                relativeReference.getParent());
            result = new EntityReference(defaultDocumentName, EntityType.DOCUMENT, parentSpace);
        } else {
            result = relativeReference;
        }

        EntityReference oldSpaceReference = result.extractFirstReference(EntityType.SPACE);
        if (oldSpaceReference != null) {
            EntityReference newSpaceReference = new EntityReference(oldSpaceReference,
                Map.of(EntityReference.PARENT_TYPE_PARAMETER, EntityType.SPACE));
            result = result.replaceParent(oldSpaceReference, newSpaceReference);
        }
        return result;
    }
}
