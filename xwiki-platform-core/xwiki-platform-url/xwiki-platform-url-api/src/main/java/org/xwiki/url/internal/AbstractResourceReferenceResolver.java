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
package org.xwiki.url.internal;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * Helper for implementers of {@link ResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 7.1M1
 */
public abstract class AbstractResourceReferenceResolver implements ResourceReferenceResolver<ExtendedURL>
{
    @Inject
    @Named("context")
    protected ComponentManager componentManager;

    /**
     * Find the right Resolver for the passed Resource type and call it.
     */
    protected ResourceReferenceResolver<ExtendedURL> findResourceResolver(String hintPrefix, ResourceType type)
        throws UnsupportedResourceReferenceException
    {
        ResourceReferenceResolver<ExtendedURL> resolver;

        Type roleType = new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class);
        String roleHint = computeResolverHint(hintPrefix, type.getId());

        // Step 1: Look for a Resolver specific to the scheme and specific to the Resource Type
        if (this.componentManager.hasComponent(roleType, roleHint)) {
            try {
                resolver = this.componentManager.getInstance(roleType, roleHint);
            } catch (ComponentLookupException cle) {
                // There's no Resolver registered for the passed Resource Type
                throw new UnsupportedResourceReferenceException(String.format(
                    "Failed to lookup Resource Reference Resolver for hint [%s]", roleHint), cle);
            }
        } else {
            // Step 2: If not found, look for a Resolver specific to the Resource Type but registered for all URL
            // schemes
            try {
                resolver = this.componentManager.getInstance(roleType, type.getId());
            } catch (ComponentLookupException cle) {
                // There's no Resolver registered for the passed Resource Type
                throw new UnsupportedResourceReferenceException(String.format(
                    "Failed to lookup Resource Reference Resolver for type [%s]", type), cle);
            }
        }

        return resolver;
    }

    protected String computeResolverHint(String hintPrefix, String type)
    {
        return String.format("%s/%s", hintPrefix, type);
    }

    /**
     * Copies query string parameters from the passed {@link org.xwiki.url.ExtendedURL} to the passed
     * {@link ResourceReference}.
     *
     * @param source the source URL from where to get the query string parameters
     * @param target the {@link ResourceReference} into which to copy the query string parameters
     */
    protected void copyParameters(ExtendedURL source, ResourceReference target)
    {
        for (Map.Entry<String, List<String>> entry : source.getParameters().entrySet()) {
            if (entry.getValue().isEmpty()) {
                target.addParameter(entry.getKey(), null);
            } else {
                target.addParameter(entry.getKey(), entry.getValue());
            }
        }
    }
}
