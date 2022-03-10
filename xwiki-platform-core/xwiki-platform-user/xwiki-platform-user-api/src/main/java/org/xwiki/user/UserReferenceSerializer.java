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
 * Converts a {@link UserReference} into a serialized form (e.g. into a String representation).
 *
 * @param <T> the type into which to serialize the user reference (e.g. String)
 * @version $Id$
 * @since 12.2
 */
@Role
public interface UserReferenceSerializer<T>
{
    /**
     * Type instance for {@code UserReferenceSerializer<String>}.
     * 
     * @since 14.1RC1
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, UserReferenceSerializer.class, String.class);

    /**
     * Type instance for {@code UserReferenceSerializer<DocumentReference>}.
     * 
     * @since 14.1RC1
     */
    ParameterizedType TYPE_DOCUMENT_REFERENCE =
        new DefaultParameterizedType(null, UserReferenceSerializer.class, DocumentReference.class);

    /**
     * @param userReference the user reference to serialize
     * @return the serialized representation
     */
    T serialize(UserReference userReference);

    /**
     * @param userReference the user reference to serialize
     * @param parameters optional parameters that have a meaning only for the specific serializer implementation used
     *            (for example a Document User Reference serializer might want to serialize the user reference relative
     *            to another entity reference passed as parameter)
     * @return the serialized representation
     * @since 14.1RC1
     */
    default T serialize(UserReference userReference, Object... parameters)
    {
        return serialize(userReference);
    }
}
