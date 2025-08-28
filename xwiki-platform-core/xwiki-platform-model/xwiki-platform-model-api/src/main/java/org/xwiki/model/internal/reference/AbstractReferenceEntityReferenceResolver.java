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
package org.xwiki.model.internal.reference;

import java.util.List;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.InvalidEntityReferenceException;

/**
 * Resolve an {@link EntityReference} into a valid and absolute reference (with all required parents filled in). Generic
 * implementation deferring default values for unspecified reference parts to extending classes.
 *
 * @see AbstractEntityReferenceResolver
 * @version $Id$
 * @since 2.2.3
 */
public abstract class AbstractReferenceEntityReferenceResolver extends AbstractEntityReferenceResolver
    implements EntityReferenceResolver<EntityReference>
{
    @Override
    public EntityReference resolve(EntityReference referenceToResolve, EntityType type, Object... parameters)
    {
        EntityReference normalizedReference = referenceToResolve;

        if (normalizedReference == null) {
            normalizedReference = resolveDefaultReference(type, parameters);
        } else if (normalizedReference.getType() != type) {
            // If the passed type is not compatible with the reference to resolve's type then we need to convert it
            if (!type.isAllowedAncestor(normalizedReference.getType())
                && !normalizedReference.getType().isAllowedAncestor(type)) {
                normalizedReference = normalizeReference(normalizedReference, parameters);

                normalizedReference = toCompatibleEntityReference(normalizedReference, type);
            }

            // If the passed type is a supertype of the reference to resolve's type then we need to insert a top
            // level reference.
            if (type != normalizedReference.getType() && type.isAllowedAncestor(normalizedReference.getType())) {
                normalizedReference = resolveDefaultReference(type, parameters).appendParent(normalizedReference);
            }
        }

        // Check all references and parent references which have a NULL name and replace them with default values.
        // In addition insert references where needed.
        try {
            normalizedReference = normalizeReference(normalizedReference, parameters);
        } catch (InvalidEntityReferenceException e) {
            throw new InvalidEntityReferenceException("Invalid reference [" + referenceToResolve + "]");
        }

        if (type != normalizedReference.getType()) {
            // If the passed type is a subtype of the reference to resolve's type then we extract the reference.
            normalizedReference = normalizedReference.extractReference(type);
        }

        return normalizedReference;
    }

    /**
     * Normalize the provided reference, filling missing names, and gaps in the parent chain.
     * 
     * @param referenceToResolve the reference to normalize, if the first parameter is an entity reference, it is used
     *            to compute default names.
     * @param parameters optional parameters,
     * @return a normalized reference chain
     */
    private EntityReference normalizeReference(EntityReference referenceToResolve, Object[] parameters)
    {
        EntityReference normalizedReference = referenceToResolve;
        EntityReference reference = normalizedReference;
        while (reference != null) {
            List<EntityType> types = reference.getType().getAllowedParents();
            if (reference.getParent() != null
                && isParentTypeAndAllowedTypeNotMatching(types, reference.getParentType())) {
                // The parent reference isn't the allowed parent: insert an allowed reference
                EntityReference newReference =
                    resolveDefaultReference(types.get(0), parameters).appendParent(reference.getParent());
                normalizedReference = normalizedReference.replaceParent(reference.getParent(), newReference);
                reference = newReference;
            } else if (reference.getParent() == null && reference.getParentType() != null) {
                // The top reference isn't the allowed top level reference, add a parent reference
                EntityReference newReference = resolveDefaultReference(reference.getParentType(), parameters);
                normalizedReference = normalizedReference.appendParent(newReference);
                reference = newReference;
            } else if (reference.getParent() != null && types.isEmpty()) {
                // There's a parent but no one is allowed
                throw new InvalidEntityReferenceException();
            } else {
                // Parent is ok, check next
                reference = reference.getParent();
            }
        }

        return normalizedReference;
    }

    private boolean isParentTypeAndAllowedTypeNotMatching(List<EntityType> allowedTypes, EntityType parentType)
    {
        return !allowedTypes.isEmpty() && parentType != null && !allowedTypes.contains(parentType);
    }
}
