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
package org.xwiki.user;

import java.lang.reflect.ParameterizedType;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;

/**
 * Convert a raw representation of a user reference into a {@link UserReference} object (for example converts a String
 * containing a user name into a Document User Reference).
 *
 * @param <T> the type of the raw user reference
 * @version $Id$
 * @since 12.2
 */
@Role
public interface UserReferenceResolver<T>
{
    /**
     * Type instance for {@code UserReferenceResolver<String>}.
     * 
     * @since 14.1RC1
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, UserReferenceResolver.class, String.class);

    /**
     * Type instance for {@code UserReferenceResolver<DocumentReference>}.
     * 
     * @since 14.1RC1
     */
    ParameterizedType TYPE_DOCUMENT_REFERENCE =
        new DefaultParameterizedType(null, UserReferenceResolver.class, DocumentReference.class);

    /**
     * @param rawReference the raw representation of a user reference to convert. If null then resolves to the current
     *            user reference
     * @param parameters optional parameters that have a meaning only for the specific resolver implementation used (for
     *            example a Document User Reference resolver accepting a user name as a string will also take as
     *            parameter a Wiki Reference defining in which wiki the user belongs to)
     * @return the resulting User Reference object
     */
    UserReference resolve(T rawReference, Object... parameters);
}
