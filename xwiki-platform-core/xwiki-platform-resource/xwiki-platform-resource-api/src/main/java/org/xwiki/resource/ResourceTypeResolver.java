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
import org.xwiki.stability.Unstable;

/**
 * Transforms some representation of an XWiki Resource Type into a {@link org.xwiki.resource.ResourceType} instance.
 * <p>
 * Note that you could wonder why this interface is required when the
 * {@link org.xwiki.resource.ResourceReferenceResolver} interface returns a {@link org.xwiki.resource.ResourceReference}
 * which itself provides that Type through {@link org.xwiki.resource.ResourceReference#getType()} method. The reason is
 * that some code needs to find out the Resource Type without parsing the full representation before taking
 * some decision. This is the case for example for the XWiki Routing Filter which needs to decide how to perform the
 * routing based on the Resource Type. That code is executed when there's no Execution Context yet and thus resolving
 * the full Resource Reference would fail since it requires some valid Execution Context to extract the wiki name
 * from the URL.
 * </p>
 *
 * @param <T> the object to transform into a Resource Type
 * @version $Id$
 * @since 7.1M1
 */
@Role
@Unstable
public interface ResourceTypeResolver<T>
{
    /**
     * Transforms some representation of a XWiki Resource Type into a {@link org.xwiki.resource.ResourceType} instance.
     *
     * @param representation the object to transform into a {@link org.xwiki.resource.ResourceType} instance
     * @param parameters generic parameters that depend on the underlying implementation. In order to know what to pass
     *        you need to check the documentation for the implementation you're using.
     * @return the {@link org.xwiki.resource.ResourceType} instance
     * @throws CreateResourceTypeException if there was an error while creating the Resource Type object (the
     *         representation doesn't contain a valid Resource Type for example)
     */
    ResourceType resolve(T representation, Map<String, Object> parameters) throws CreateResourceTypeException;
}
