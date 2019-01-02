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
import org.xwiki.stability.Unstable;

/**
 * Resolve an Object reference defined in a given representation into a validated {@link PageObjectReference} object,
 * i.e. with valid values and a valid hierarchy (e.g. an Object reference must have a parent which is a document
 * reference, reference values must not be null, etc).
 * 
 * @param <T> the type of the representation (e.g. a String)
 * @version $Id$
 * @since 10.6RC1
 */
@Role
@Unstable
public interface PageObjectReferenceResolver<T>
{
    /**
     * Type instance for {@code PageObjectReferenceResolver<String>}.
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, PageObjectReferenceResolver.class, String.class);

    /**
     * Type instance for {@code PageObjectReferenceResolver<EntityReference>}.
     */
    ParameterizedType TYPE_REFERENCE =
        new DefaultParameterizedType(null, PageObjectReferenceResolver.class, EntityReference.class);

    /**
     * @param objectReferenceRepresentation the representation of an object reference (e.g. as a String)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the valid resolved object reference as an object
     */
    PageObjectReference resolve(T objectReferenceRepresentation, Object... parameters);
}
