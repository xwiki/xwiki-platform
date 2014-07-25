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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.InvalidEntityReferenceException;

/**
 * Resolve an {@link EntityReference} into a valid and absolute reference (with all required parents filled in).
 * Generic implementation deferring default values for unspecified reference parts to extending classes.
 *
 * @see AbstractEntityReferenceResolver
 * @version $Id$
 * @since 2.2.3
 */
public abstract class AbstractReferenceEntityReferenceResolver extends AbstractEntityReferenceResolver
    implements EntityReferenceResolver<EntityReference>
{
    /**
     * Map defining the parent relationship between reference types.
     */
    private Map<EntityType, List<EntityType>> nextAllowedEntityTypes = new HashMap<EntityType, List<EntityType>>()
    {
        {
            put(EntityType.ATTACHMENT, Arrays.asList(EntityType.DOCUMENT));
            put(EntityType.DOCUMENT, Arrays.asList(EntityType.SPACE));
            put(EntityType.SPACE, Arrays.asList(EntityType.WIKI, EntityType.SPACE));
            put(EntityType.WIKI, Collections.<EntityType> emptyList());
            put(EntityType.OBJECT, Arrays.asList(EntityType.DOCUMENT));
            put(EntityType.OBJECT_PROPERTY, Arrays.asList(EntityType.OBJECT));
        }
    };

    @Override
    public EntityReference resolve(EntityReference referenceToResolve, EntityType type, Object... parameters)
    {
        EntityReference normalizedReference;

        if (referenceToResolve == null) {
            normalizedReference = new EntityReference(resolveDefaultValue(type, parameters), type);
        } else {
            // If the passed type is a supertype of the reference to resolve's type then we need to insert a top level
            // reference.
            if (type.ordinal() > referenceToResolve.getType().ordinal()) {
                normalizedReference =
                    new EntityReference(resolveDefaultValue(type, parameters), type, referenceToResolve);
            } else {
                normalizedReference = referenceToResolve;
            }
        }

        // Check all references and parent references which have a NULL name and replace them with default values.
        // In addition insert references where needed.
        try {
            normalizedReference = normalizeReference(normalizedReference, parameters);
        } catch (InvalidEntityReferenceException e) {
            throw new InvalidEntityReferenceException("Invalid reference [" + referenceToResolve + "]");
        }

        if (referenceToResolve != null) {
            // If the passed type is a subtype of the reference to resolve's type then we extract the reference.
            if (type.ordinal() < referenceToResolve.getType().ordinal()) {
                normalizedReference = normalizedReference.extractReference(type);
            }
        }

        return normalizedReference;
    }

    /**
     * Normalize the provided reference, filling missing names, and gaps in the parent chain.
     * @param referenceToResolve the reference to normalize, if the first parameter is an entity reference, it is used
     * to compute default names.
     * @param parameters optional parameters,
     * @return a normalized reference chain
     */
    private EntityReference normalizeReference(EntityReference referenceToResolve,
        Object[] parameters)
    {
        EntityReference normalizedReference = referenceToResolve;
        EntityReference reference = normalizedReference;
        while (reference != null) {
            List<EntityType> types = this.nextAllowedEntityTypes.get(reference.getType());
            if (reference.getParent() != null && !types.isEmpty() && !types.contains(reference.getParent().getType())) {
                // The parent reference isn't the allowed parent: insert an allowed reference
                EntityReference newReference =
                    new EntityReference(resolveDefaultValue(types.get(0), parameters), types.get(0), reference
                        .getParent());
                normalizedReference = normalizedReference.replaceParent(reference.getParent(), newReference);
                reference = newReference;
            } else if (reference.getParent() == null && !types.isEmpty()) {
                // The top reference isn't the allowed top level reference, add a parent reference
                EntityReference newReference =
                    new EntityReference(resolveDefaultValue(types.get(0), parameters), types.get(0));
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
}
