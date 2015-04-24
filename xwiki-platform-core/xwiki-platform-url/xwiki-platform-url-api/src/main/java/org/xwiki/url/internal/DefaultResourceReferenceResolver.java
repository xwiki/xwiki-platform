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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;

/**
 * Delegates the work to the Resource Reference Resolver matching the URL Scheme defined in the XWiki Configuration
 * (see {@link org.xwiki.url.URLConfiguration#getURLFormatId()}. If none is found, defaults to the Generic
 * Resource Reference Resolver.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultResourceReferenceResolver implements ResourceReferenceResolver<ExtendedURL>
{
    /**
     * Used to get the hint of the {@link org.xwiki.resource.ResourceReferenceResolver} to use.
     */
    @Inject
    private URLConfiguration configuration;

    @Inject
    @Named("generic")
    private ResourceReferenceResolver<ExtendedURL> genericResourceReferenceResolver;

    /**
     * Used to lookup the correct {@link org.xwiki.resource.ResourceReferenceResolver} component.
     */
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public ResourceReference resolve(ExtendedURL extendedURL, ResourceType type, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ResourceReferenceResolver resolver;

        // Step 1: Look for a URL-scheme-specific Resolver (a general one that is independent of the passed
        //         Resource Type). This allows URL-scheme implementation to completely override handling of any
        //         Resource Type if they wish.
        DefaultParameterizedType parameterizedType = new DefaultParameterizedType(null,
            ResourceReferenceResolver.class, ExtendedURL.class);
        String hint = this.configuration.getURLFormatId();
        if (this.componentManager.hasComponent(parameterizedType, hint)) {
            try {
                resolver = this.componentManager.getInstance(parameterizedType, hint);
            } catch (ComponentLookupException e) {
                throw new CreateResourceReferenceException(
                    String.format("Failed to create Resource Reference for [%s].", extendedURL.getWrappedURL()), e);
            }
        } else {
            // Step 2: If not found, use the Generic Resolver, which tries to find a Resolver registered for the
            //         specific Resource Type.
            resolver = this.genericResourceReferenceResolver;
        }
        return resolver.resolve(extendedURL, type, parameters);
    }
}
