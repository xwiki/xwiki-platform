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
package org.xwiki.model.reference;

import java.lang.reflect.ParameterizedType;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;

/**
 * Resolve a Page reference defined in a given representation into a validated {@link PageReference} object, ie with
 * valid values and a valid hierarchy (eg a Page reference must have a parent which is a space reference, reference
 * values must not be null, etc).
 * 
 * @version $Id$
 * @since 10.6RC1
 * @param <T> the type of the representation (eg a String)
 */
@Role
public interface PageReferenceResolver<T>
{
    /**
     * Type instance for {@code PageReferenceResolver<String>}.
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, PageReferenceResolver.class, String.class);

    /**
     * Type instance for {@code PageReferenceResolver<EntityReference>}.
     * 
     * @since 4.0M1
     */
    ParameterizedType TYPE_REFERENCE =
        new DefaultParameterizedType(null, PageReferenceResolver.class, EntityReference.class);

    /**
     * @param pageReferenceRepresentation the representation of a page reference (eg as a String)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the valid resolved page reference as an Object
     */
    PageReference resolve(T pageReferenceRepresentation, Object... parameters);
}
