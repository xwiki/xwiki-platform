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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Generic entity reference resolver deferring resolution and default values to extending classes but resolving
 * default value from the first optional parameter when provided and is an instance of a entity reference. This
 * is use by most resolver to provide relative resolution to a provided reference.
 *
 * @version $Id$
 * @since 3.3M2
 */
public abstract class AbstractEntityReferenceResolver
{
    /**
     * @param type the entity type for which to return the default value to use (since the use has not specified it)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the default value to use
     */
    protected abstract String getDefaultValue(EntityType type, Object... parameters);

    /**
     * Resolve default name for a given reference type.
     * @param type the type for which a default name is requested
     * @param parameters optional parameters, if the first parameter is an entity reference which is of the given type
     * or contains the given types in its parent chain, use the name of the reference having the requested type in
     * place of the default value
     * @return a name for the given type
     */
    protected String resolveDefaultValue(EntityType type, Object... parameters)
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
