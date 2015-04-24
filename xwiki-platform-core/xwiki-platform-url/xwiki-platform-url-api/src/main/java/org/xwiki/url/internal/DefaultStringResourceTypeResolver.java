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
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.url.URLConfiguration;

/**
 * Delegates the work to the Resource Type Resolver matching the URL Scheme defined in the XWiki Configuration
 * (see {@link org.xwiki.url.URLConfiguration#getURLFormatId()}. If none is found, defaults to the Generic
 * Resource Type Resolver.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Singleton
public class DefaultStringResourceTypeResolver implements ResourceTypeResolver<String>
{
    @Inject
    private URLConfiguration configuration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    @Named("generic")
    private ResourceTypeResolver<String> genericResourceTypeResolver;

    @Override
    public ResourceType resolve(String type, Map<String, Object> parameters)
        throws CreateResourceTypeException
    {
        ResourceTypeResolver resolver;

        DefaultParameterizedType parameterizedType =
            new DefaultParameterizedType(null, ResourceTypeResolver.class, String.class);
        String hint = this.configuration.getURLFormatId();
        if (this.componentManager.hasComponent(parameterizedType, hint)) {
            try {
                resolver = this.componentManager.getInstance(parameterizedType, hint);
            } catch (ComponentLookupException e) {
                throw new CreateResourceTypeException(
                    String.format("Failed to convert Resource Type from String [%s] to [%s]", type,
                        ResourceType.class.getSimpleName()), e);
            }
        } else {
            // No specific String Resource Type Resolver for the Scheme URL, use the generic one!
            resolver = this.genericResourceTypeResolver;
        }
        return resolver.resolve(type, parameters);
    }
}
