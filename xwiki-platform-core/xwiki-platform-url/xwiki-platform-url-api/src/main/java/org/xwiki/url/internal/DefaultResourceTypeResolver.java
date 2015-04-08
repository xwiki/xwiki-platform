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
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;

/**
 * Delegates the work of extracting the Resource Type to the Resource Type Resolver specified in the XWiki Configuration
 * (see {@link org.xwiki.url.URLConfiguration#getURLFormatId()}.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Singleton
public class DefaultResourceTypeResolver implements ResourceTypeResolver<ExtendedURL>
{
    /**
     * Used to get the hint of the {@link org.xwiki.resource.ResourceTypeResolver} to use.
     */
    @Inject
    private URLConfiguration configuration;

    /**
     * Used to lookup the correct {@link org.xwiki.resource.ResourceTypeResolver} component.
     */
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public ResourceType resolve(ExtendedURL extendedURL, Map<String, Object> parameters)
        throws CreateResourceTypeException
    {
        ResourceTypeResolver resolver;
        try {
            resolver = this.componentManager.getInstance(
                new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class),
                this.configuration.getURLFormatId());
        } catch (ComponentLookupException e) {
            throw new CreateResourceTypeException(
                String.format("Invalid configuration hint [%s]. Cannot create Resource Type for [%s].",
                    this.configuration.getURLFormatId(), extendedURL.getWrappedURL()), e);
        }
        return resolver.resolve(extendedURL, parameters);
    }
}
