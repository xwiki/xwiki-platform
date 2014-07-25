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
package org.xwiki.resource;

import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.stability.Unstable;

/**
 * Transforms some representation of an XWiki Resource into a {@link Resource} instance.
 *
 * @param <T> the object to transform into a XWiki Resource
 * @param <U> the return type (e.g. {@link Resource}, {@link EntityResource}, etc)
 * @version $Id$
 * @since 5.3M1
 */
@Role
@Unstable
public interface ResourceFactory<T, U extends Resource>
{
    /**
     * Type instance for {@code ResourceFactory<URL, Resource>}.
     */
    ParameterizedType TYPE_URL_RESOURCE =
        new DefaultParameterizedType(null, ResourceFactory.class, URL.class, Resource.class);

    /**
     * Transforms some representation of a XWiki Resource into a {@link Resource} instance.
     *
     * @param representation the object to transform into a {@link Resource} instance
     * @param parameters generic parameters that depend on the underlying implementation. In order to know what to pass
     * you need to check the documentation for the implementation you're using.
     * @return the {@link Resource} instance
     * @throws ResourceCreationException if there was an error while creating the XWiki Resource object
     * @throws UnsupportedResourceException if the passed representation points to an unsupported Resource type that we
     *         don't know how to parse
     */
    U createResource(T representation, Map<String, Object> parameters)
        throws ResourceCreationException, UnsupportedResourceException;
}
