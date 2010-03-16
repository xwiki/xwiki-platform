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
 * 
 * @version $Id: DefaultReferenceEntityReferenceResolver.java 27580 2010-03-11 14:32:23Z vmassol $
 * @since 2.2.3
 */
public abstract class AbstractReferenceEntityReferenceResolver implements EntityReferenceResolver<EntityReference>
{
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

    /**
     * @param type the entity type for which to return the default value to use (since the use has not specified it)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the default value to use
     */
    protected abstract String getDefaultValue(EntityType type, Object... parameters);

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.model.reference.EntityReferenceResolver#resolve
     */
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
                    new EntityReference(resolveDefaultValue(type, parameters), type, referenceToResolve.clone());
            } else {
                normalizedReference = referenceToResolve.clone();
            }
        }

        // Check all references and parent references which have a NULL name and replace them with default values.
        // In addition insert references where needed.
        EntityReference reference = normalizedReference;
        while (reference != null) {
            List<EntityType> types = this.nextAllowedEntityTypes.get(reference.getType());
            if (reference.getParent() != null && !types.isEmpty() && !types.contains(reference.getParent().getType())) {
                // The parent reference isn't the allowed parent: insert an allowed reference
                EntityReference newReference =
                    new EntityReference(resolveDefaultValue(types.get(0), parameters), types.get(0), reference
                        .getParent());
                reference.setParent(newReference);
            } else if (reference.getParent() == null && !types.isEmpty()) {
                // The top reference isn't the allowed top level reference, add a parent reference
                EntityReference newReference =
                    new EntityReference(resolveDefaultValue(types.get(0), parameters), types.get(0));
                reference.setParent(newReference);
            } else if (reference.getParent() != null && types.isEmpty()) {
                // There's a parent but not of the correct type... it means the reference is invalid
                throw new InvalidEntityReferenceException("Invalid reference [" + referenceToResolve + "]");
            }
            reference = reference.getParent();
        }

        if (referenceToResolve != null) {
            // If the passed type is a subtype of the reference to resolve's type then we extract the reference.
            if (type.ordinal() < referenceToResolve.getType().ordinal()) {
                normalizedReference = normalizedReference.extractReference(type);
            }
        }

        return normalizedReference;
    }

    private String resolveDefaultValue(EntityType type, Object... parameters)
    {
        String resolvedDefaultValue = null;
        if (parameters.length > 0 && parameters[0] instanceof EntityReference) {
            // Try to extract the type from the passed parameter.
            EntityReference referenceParameter = (EntityReference) parameters[0];
            EntityReference extractedReference = referenceParameter.extractReference(type);
            if (extractedReference != null) {
                resolvedDefaultValue = extractedReference.getName();
            }
        }

        if (resolvedDefaultValue == null) {
            resolvedDefaultValue = getDefaultValue(type, parameters);
        }

        return resolvedDefaultValue;
    }
}
