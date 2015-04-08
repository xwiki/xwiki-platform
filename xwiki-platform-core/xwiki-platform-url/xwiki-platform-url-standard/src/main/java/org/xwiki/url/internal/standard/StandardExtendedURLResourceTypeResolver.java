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
package org.xwiki.url.internal.standard;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

/**
 * Extracts the {@link ResourceType} from a passed {@link ExtendedURL}, using the {@code standard} URL scheme format.
 * In that format the Resource Type is the path segment in the URL just after the Context Path one (e.g.
 * {@code bin} in {@code http://<server>/xwiki/bin/view/Space/Page}.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("standard")
@Singleton
public class StandardExtendedURLResourceTypeResolver implements ResourceTypeResolver<ExtendedURL>
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public ResourceType resolve(ExtendedURL extendedURL, Map<String, Object> parameters)
    {
        ResourceType resourceType;

        // Find the Resource Type, which is the first segment in the ExtendedURL.
        //
        // Note that we need to remove it from the ExtendedURL instance since it's passed to the specific resolvers
        // and they shouldn't be aware of where it was located since they need to be able to resolve the rest of the
        // URL independently of the URL scheme, in case they wish to have a single URL syntax for all URL schemes.
        // Examples:
        // - scheme 1: /<type>/something
        // - scheme 2: /something?type=<type>
        // The specific resolver for type <type> needs to be passed an ExtendedURL independent of the type, in this
        // case, "/something" for both examples.
        //
        // However since we also want this code to work when short URLs are enabled, we only remove the segment part
        // if a Resource type has been identified (see below) and if not, we assume the URL is pointing to an Entity
        // Resource.
        String type = extendedURL.getSegments().get(0);

        // First, find out if an ExtendedURL Resource Resolver exists, only for this URL scheme.
        // Second, if not found, try to locate a URL Resolver registered for all URL schemes
        if (this.componentManager.hasComponent(new DefaultParameterizedType(null,
            ResourceReferenceResolver.class, ExtendedURL.class), computeHint(type)))
        {
            resourceType = new ResourceType(type);
            extendedURL.getSegments().remove(0);
        } else if (this.componentManager.hasComponent(new DefaultParameterizedType(null,
            ResourceReferenceResolver.class, ExtendedURL.class), type))
        {
            resourceType = new ResourceType(type);
            extendedURL.getSegments().remove(0);
        } else {
            // No specific Resource Type Resolver has been found. In order to support short URLs (ie. without the
            // "bin" or "wiki" part specified), we assume the URL is pointing to an Entity Resource Reference.
            // Since the "wiki" type was not selected, we're assuming that the we'll use the "bin" entity resolver.
            resourceType = EntityResourceReference.TYPE;
        }

        return resourceType;
    }

    private String computeHint(String type)
    {
        return String.format("standard/%s", type);
    }
}
