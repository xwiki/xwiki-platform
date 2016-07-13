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
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * Parses an {@link ExtendedURL} written in the "standard" format and generate a
 * {@link org.xwiki.resource.ResourceReference} out of it.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("standard")
@Singleton
public class StandardExtendedURLResourceReferenceResolver implements ResourceReferenceResolver<ExtendedURL>
{
    @Inject
    @Named("generic")
    private ResourceReferenceResolver<ExtendedURL> genericResourceReferenceResolver;

    /**
     * {@inheritDoc}
     *
     * <p>
     * No specific parameter is supported.
     * </p>
     *
     * @see org.xwiki.resource.ResourceReferenceResolver#resolve(Object, ResourceType, Map)
     */
    @Override
    public ResourceReference resolve(ExtendedURL extendedURL, ResourceType type, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        return this.genericResourceReferenceResolver.resolve(extendedURL, type, parameters);
    }
}
