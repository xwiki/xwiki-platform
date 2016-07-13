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
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;

/**
 * Generic Resolver which first tries to find a Resource Reference Resolver specific to both the URL Scheme and to the
 * Resource Type (i.e. with a hint of the type {@code <URL scheme id>/<resource type id>}, e.g. {@code standard/entity})
 * and if it cannot find it then tries to find a Resource Reference Resolver specific only to the Resource Type
 * (i.e. with a hint of the type {@code <resource type id>}, e.g. {@code entity}, which means a Resolver registered
 * for all URL Schemes).
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("generic")
@Singleton
public class GenericResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    @Inject
    private URLConfiguration configuration;

    @Override
    public ResourceReference resolve(ExtendedURL extendedURL, ResourceType type, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ResourceReferenceResolver<ExtendedURL> resolver =
            findResourceResolver(this.configuration.getURLFormatId(), type);
        return resolver.resolve(extendedURL, type, parameters);
    }
}
