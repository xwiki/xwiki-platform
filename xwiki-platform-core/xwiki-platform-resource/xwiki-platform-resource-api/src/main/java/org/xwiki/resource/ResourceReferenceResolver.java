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

import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Transforms some representation of an XWiki Resource Reference (aka a URI) into a {@link ResourceReference} instance.
 *
 * @param <T> the object to transform into a XWiki Resource Reference
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface ResourceReferenceResolver<T>
{
    /**
     * Transforms some representation of a XWiki Resource Reference into a {@link ResourceReference} instance.
     *
     * @param representation the object to transform into a {@link ResourceReference} instance
     * @param resourceType the type of Resource represented by the passed representation parameter. To get this type
     *        you can use a {@link ResourceTypeResolver}
     * @param parameters generic parameters that depend on the underlying implementation. In order to know what to pass
     *        you need to check the documentation for the implementation you're using.
     * @return the {@link ResourceReference} instance
     * @throws CreateResourceReferenceException if there was an error while creating the XWiki Resource object
     * @throws UnsupportedResourceReferenceException if the passed representation points to an unsupported Resource
     *         Reference type that we don't know how to resolve
     * @since 7.1M1
     */
    ResourceReference resolve(T representation, ResourceType resourceType, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException;
}
