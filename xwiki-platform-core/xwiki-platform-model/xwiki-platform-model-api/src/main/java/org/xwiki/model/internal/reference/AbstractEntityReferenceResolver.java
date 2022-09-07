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

import java.util.EnumMap;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.InvalidEntityReferenceException;

/**
 * Generic entity reference resolver deferring resolution and default values to extending classes but resolving default
 * value from the first optional parameter when provided and is an instance of a entity reference. This is use by most
 * resolver to provide relative resolution to a provided reference.
 *
 * @version $Id$
 * @since 3.3M2
 */
public abstract class AbstractEntityReferenceResolver
{
    /**
     * The mapping between the PAGE and the DOCUMENT based worlds.
     */
    public static final Map<EntityType, EntityType> TYPE_CONVERT_MAPPING = new EnumMap<>(EntityType.class);

    static {
        TYPE_CONVERT_MAPPING.put(EntityType.WIKI, EntityType.WIKI);

        TYPE_CONVERT_MAPPING.put(EntityType.PAGE, EntityType.SPACE);
        TYPE_CONVERT_MAPPING.put(EntityType.PAGE_ATTACHMENT, EntityType.ATTACHMENT);
        TYPE_CONVERT_MAPPING.put(EntityType.PAGE_CLASS_PROPERTY, EntityType.CLASS_PROPERTY);
        TYPE_CONVERT_MAPPING.put(EntityType.PAGE_OBJECT, EntityType.OBJECT);
        TYPE_CONVERT_MAPPING.put(EntityType.PAGE_OBJECT_PROPERTY, EntityType.OBJECT_PROPERTY);

        TYPE_CONVERT_MAPPING.put(EntityType.SPACE, EntityType.PAGE);
        TYPE_CONVERT_MAPPING.put(EntityType.ATTACHMENT, EntityType.PAGE_ATTACHMENT);
        TYPE_CONVERT_MAPPING.put(EntityType.CLASS_PROPERTY, EntityType.PAGE_CLASS_PROPERTY);
        TYPE_CONVERT_MAPPING.put(EntityType.OBJECT, EntityType.PAGE_OBJECT);
        TYPE_CONVERT_MAPPING.put(EntityType.OBJECT_PROPERTY, EntityType.PAGE_OBJECT_PROPERTY);
    }

    @Inject
    private EntityReferenceProvider defaultProvider;

    private String getDefaultDocumentName()
    {
        return this.defaultProvider.getDefaultReference(EntityType.DOCUMENT).getName();
    }

    /**
     * @param type the entity type for which to return the default value to use (since the use has not specified it)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the default value to use
     * @since 7.2M1
     */
    protected abstract EntityReference getDefaultReference(EntityType type, Object... parameters);

    /**
     * Resolve default reference for a given reference type.
     * 
     * @param type the type for which a default name is requested
     * @param parameters optional parameters, if the first parameter is an entity reference which is of the given type
     *            or contains the given types in its parent chain, use the name of the reference having the requested
     *            type in place of the default value
     * @return the reference for the given type
     * @since 7.2M1
     */
    protected EntityReference resolveDefaultReference(EntityType type, Object... parameters)
    {
        EntityReference resolvedDefaultValue = null;

        if (parameters != null && parameters.length > 0 && parameters[0] instanceof EntityReference) {
            // Try to extract the required type from the passed parameter.
            EntityReference referenceParameter = (EntityReference) parameters[0];
            // Make sure to use a compatible reference
            referenceParameter = toCompatibleEntityReference(referenceParameter, type);

            EntityReference extractedReference = referenceParameter.extractReference(type);
            if (extractedReference != null) {
                resolvedDefaultValue = extractedReference;

                // Get rid of parent if any
                EntityReference parent = extractedReference.getParent();
                while (parent != null && parent.getType() == type) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    resolvedDefaultValue = resolvedDefaultValue.removeParent(parent);
                }
            }
        }

        if (resolvedDefaultValue == null) {
            resolvedDefaultValue = getDefaultReference(type, parameters);
        }

        return resolvedDefaultValue;
    }

    /**
     * @param referenceToConvert the reference to convert (if needed)
     * @param toType the required type
     * @return the converted reference if conversion is needed, the passed one otherwise
     */
    protected EntityReference toCompatibleEntityReference(EntityReference referenceToConvert, EntityType toType)
    {
        return toCompatibleEntityReference(referenceToConvert, toType, true);
    }

    /**
     * Check if the passed types are compatible. This means that they are the same of that one can be the parent of the
     * other.
     * 
     * @param type1 the first type of test
     * @param type2 the second type of test
     * @return true of the passed types are compatible
     */
    protected boolean isCompatible(EntityType type1, EntityType type2)
    {
        return type1 == type2 || type1.isAllowedAncestor(type2) || type2.isAllowedAncestor(type1);
    }

    private EntityReference toCompatibleEntityReference(EntityReference referenceToConvert, EntityType toType,
        boolean firstPage)
    {
        if (referenceToConvert == null || isCompatible(referenceToConvert.getType(), toType)) {
            return referenceToConvert;
        }

        // Convert parent
        EntityReference convertedParent = toCompatibleEntityReference(referenceToConvert.getParent(), toType,
            firstPage && referenceToConvert.getType() != EntityType.PAGE);

        // Convert reference
        EntityReference convertedReference;
        if (referenceToConvert.getType() == EntityType.DOCUMENT) {
            if (getDefaultDocumentName().equals(referenceToConvert.getName())) {
                convertedReference = new EntityReference(convertedParent, referenceToConvert.getParameters());
            } else {
                convertedReference = new EntityReference(referenceToConvert.getName(), EntityType.PAGE, convertedParent,
                    referenceToConvert.getParameters());
            }
        } else if (referenceToConvert.getType() == EntityType.PAGE && firstPage
            && (toType == EntityType.DOCUMENT || toType.isAllowedAncestor(EntityType.DOCUMENT))) {
            convertedReference = toDOCUMENT(referenceToConvert, convertedParent);
        } else {
            convertedReference = convert(referenceToConvert, convertedParent);
        }

        return convertedReference;
    }

    private EntityReference toDOCUMENT(EntityReference referenceToConvert, EntityReference convertedParent)
    {
        return new EntityReference(getDefaultDocumentName(), EntityType.DOCUMENT,
            new EntityReference(referenceToConvert.getName(), EntityType.SPACE, convertedParent),
            referenceToConvert.getParameters());
    }

    private EntityReference convert(EntityReference referenceToConvert, EntityReference convertedParent)
    {
        EntityType convertedType = TYPE_CONVERT_MAPPING.get(referenceToConvert.getType());

        EntityReference convertedReference;
        if (convertedType == referenceToConvert.getType()) {
            convertedReference = referenceToConvert;
        } else if (convertedType != null) {
            convertedReference = new EntityReference(referenceToConvert.getName(), convertedType, convertedParent,
                referenceToConvert.getParameters());
        } else {
            throw new InvalidEntityReferenceException("Reference [" + referenceToConvert + "] cannot be converted");
        }

        return convertedReference;
    }
}
