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
import org.xwiki.model.EntityType;

/**
 * Resolve an Entity reference defined in a given representation into a validated {@link EntityReference} object, ie
 * with valid values and a valid hierarchy (eg a Document reference must have a parent which is a space reference,
 * reference values must not be null, etc).
 * 
 * @param <T> the object to resolve into an Entity Reference
 * @version $Id$
 * @since 2.2M1
 */
@Role
public interface EntityReferenceResolver<T>
{
    /**
     * Type instance for {@code EntityReferenceResolver<String>}.
     * 
     * @since 4.0M1
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, EntityReferenceResolver.class, String.class);

    /**
     * Type instance for {@code EntityReferenceResolver<EntityReference>}.
     * 
     * @since 4.0M1
     */
    ParameterizedType TYPE_REFERENCE = new DefaultParameterizedType(null, EntityReferenceResolver.class,
        EntityReference.class);

    /**
     * @param entityReferenceRepresentation the representation of an entity reference (eg as a String)
     * @param type the type of the Entity (Document, Space, Attachment, Wiki, etc) to resolve out of the representation,
     *            since 14.8 it's possible to pass null if the resolver should extract the type from the representation
     *            itself
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the valid resolved reference as an Object
     */
    EntityReference resolve(T entityReferenceRepresentation, EntityType type, Object... parameters);
}
