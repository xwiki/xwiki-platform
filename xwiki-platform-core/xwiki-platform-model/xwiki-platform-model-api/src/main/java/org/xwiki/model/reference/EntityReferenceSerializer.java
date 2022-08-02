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
 * Generate a different representation of an Entity Reference (eg as a String).
 * 
 * @param <T> the type of the new representation
 * @version $Id$
 * @since 2.2M1
 */
@Role
public interface EntityReferenceSerializer<T>
{
    /**
     * Type instance for {@code EntityReferenceSerializer<String>}.
     * 
     * @since 4.0M1
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class);

    /**
     * Type instance for {@code EntityReferenceSerializer<EntityReference>}.
     * 
     * @since 4.0M1
     */
    ParameterizedType TYPE_REFERENCE = new DefaultParameterizedType(null, EntityReferenceSerializer.class,
        EntityReference.class);

    /**
     * Serialize an entity reference into a new representation of type <code>T</code>.
     * 
     * @param reference the reference to serialize
     * @param parameters optional parameters. Their meaning depends on the serializer implementation
     * @return the new representation (eg as a String)
     */
    T serialize(EntityReference reference, Object... parameters);
}
