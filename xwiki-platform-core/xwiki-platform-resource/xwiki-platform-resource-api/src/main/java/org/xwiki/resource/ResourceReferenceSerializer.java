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

import org.xwiki.component.annotation.Role;

/**
 * Transforms a Resource Reference into some other representation (eg URL).
 * 
 * @version $Id$
 * @since 6.1M2
 * @param <T> the type of the resource reference to serialize
 * @param <U> the return type (e.g. a URL, a String, etc)
 */
@Role
public interface ResourceReferenceSerializer<T extends ResourceReference, U>
{
    /**
     * Transforms a Resource Reference into some other representation.
     * 
     * @param resource the Resource Reference to transform
     * @return the new representation
     * @throws SerializeResourceReferenceException if there was an error while serializing the XWiki Resource object
     * @throws UnsupportedResourceReferenceException if the passed representation points to an unsupported Resource
     *         Reference type that we don't know how to serialize
     */
    U serialize(T resource) throws SerializeResourceReferenceException, UnsupportedResourceReferenceException;
}
