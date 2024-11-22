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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Abstract class for all relative resource reference resolvers.
 *
 * @version $Id$
 * @since 17.0.0RC1
 */
public abstract class AbstractRelativeResourceReferenceEntityReferenceResolver
    extends AbstractResourceReferenceEntityReferenceResolver
{
    @Inject
    @Named("relative")
    protected EntityReferenceResolver<String> relativeReferenceResolver;

    /**
     * @param type the resource type that this resolver will support
     */
    protected AbstractRelativeResourceReferenceEntityReferenceResolver(ResourceType type)
    {
        super(type);
    }

    @Override
    public EntityReference resolve(ResourceReference resourceReference, EntityType entityType, Object... parameters)
    {
        if (resourceReference == null) {
            return null;
        }

        if (this.resourceType != null && !resourceReference.getType().equals(this.resourceType)) {
            throw new IllegalArgumentException(
                String.format("You must pass a resource reference of type [%s]. [%s] was passed", this.resourceType,
                    resourceReference));
        }

        EntityReference entityReference;
        EntityReference baseReference = getBaseReference(resourceReference, parameters);

        if (resourceReference.isTyped()) {
            entityReference = resolveTyped(resourceReference, baseReference);
        } else {
            entityReference = resolveUntyped(resourceReference, baseReference);
        }

        return entityReference;
    }

    @Override
    protected EntityReference getBaseReference(ResourceReference resourceReference, Object... parameters)
    {
        EntityReference baseReference =
            (parameters.length > 0 && parameters[0] instanceof EntityReference entityReference)
            ? entityReference : null;

        if (!resourceReference.getBaseReferences().isEmpty()) {
            // If the passed reference has a base reference, resolve it first with a relative resolver (it should
            // normally be absolute but who knows what the API caller has specified...)
            baseReference = resolveBaseReference(resourceReference.getBaseReferences(), baseReference);
        }

        return baseReference;
    }

    @Override
    protected EntityReference resolveBaseReference(List<String> baseReferences, EntityReference defaultBaseReference)
    {
        EntityReference resolvedBaseReference = defaultBaseReference;
        for (String baseReference : baseReferences) {
            resolvedBaseReference =
                this.relativeReferenceResolver.resolve(baseReference, EntityType.DOCUMENT, resolvedBaseReference);
        }

        return resolvedBaseReference;
    }

    @Override
    protected EntityReference resolveUntyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        return resolveTyped(resourceReference, baseReference);
    }

    @Override
    protected EntityReference resolveTyped(ResourceReference resourceReference, EntityReference baseReference)
    {
        return this.relativeReferenceResolver.resolve(resourceReference.getReference(), getEntityType(), baseReference);
    }

    /**
     *
     * @return the entity type of the {@link EntityReference} this resolver produces.
     */
    protected abstract EntityType getEntityType();
}
